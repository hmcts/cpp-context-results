package uk.gov.moj.cpp.results.command.handler;

import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.groupingBy;
import static javax.json.Json.createObjectBuilder;
import static javax.json.JsonValue.NULL;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.domains.InformantRegisterHelper.getInformantRegisterStreamId;
import static uk.gov.moj.cpp.domains.constant.RegisterStatus.RECORDED;
import static uk.gov.moj.cpp.results.command.util.DefendantMapper.getDefendants;

import uk.gov.justice.core.courts.InformantRegisterRecorded;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.informantRegisterDocument.InformantRegisterDefendant;
import uk.gov.justice.core.courts.informantRegisterDocument.InformantRegisterDocumentRequest;
import uk.gov.justice.results.courts.GenerateInformantRegister;
import uk.gov.justice.results.courts.InformantRegisterGenerated;
import uk.gov.justice.results.courts.NotifyInformantRegister;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.results.command.GenerateInformantRegisterByDate;
import uk.gov.moj.cpp.results.command.service.ProgressionQueryService;
import uk.gov.moj.cpp.results.domain.aggregate.ProsecutionAuthorityAggregate;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(COMMAND_HANDLER)
public class InformantRegisterHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(InformantRegisterHandler.class.getName());
    private static final String INFORMANT_REGISTER_QUERY_BY_STATUS = "results.query.informant-register-document-request";
    private static final String INFORMANT_REGISTER_QUERY_BY_DATE = "results.query.informant-register-document-by-request-date";
    private static final String FIELD_INFORMANT_REGISTER_DOCUMENTS = "informantRegisterDocumentRequests";
    private static final String FIELD_PROSECUTION_AUTHORITY_ID = "prosecutionAuthorityId";
    private static final String FIELD_PAYLOAD = "payload";
    private static final String FIELD_REQUEST_STATUS = "requestStatus";
    private static final String FIELD_REGISTER_DATE = "registerDate";
    private static final String FIELD_PROSECUTION_AUTHORITY_CODE = "prosecutionAuthorityCode";

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private Requester requester;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Inject
    private ProgressionQueryService progressionQueryService;

    @Handles("results.command.add-informant-register")
    public void handleAddInformantRegisterToEventStream(final Envelope<InformantRegisterDocumentRequest> envelope) throws EventStreamException {
        LOGGER.debug("results.command.add-informant-register {}", envelope.metadata().asJsonObject());

        final InformantRegisterDocumentRequest informantRegisterDocumentRequest = envelope.payload();
        if (nonNull(informantRegisterDocumentRequest.getGroupId())) {
            final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
            populateMemberCasesForGroupCase(jsonEnvelope, informantRegisterDocumentRequest);
        }

        final UUID prosecutionAuthorityId = informantRegisterDocumentRequest.getProsecutionAuthorityId();
        final UUID informantRegisterId = getInformantRegisterStreamId(prosecutionAuthorityId.toString(), informantRegisterDocumentRequest.getRegisterDate().toLocalDate().toString());

        final EventStream eventStream = eventSource.getStreamById(informantRegisterId);
        final Stream<Object> events = Stream.of(new InformantRegisterRecorded(informantRegisterDocumentRequest, prosecutionAuthorityId));

        appendEventsToStream(envelope, eventStream, events);
    }

    private void populateMemberCasesForGroupCase(final JsonEnvelope envelope, final InformantRegisterDocumentRequest informantRegisterDocumentRequest) {
        final Optional<JsonObject> jsonObject = progressionQueryService.getGroupMemberCases(envelope, informantRegisterDocumentRequest.getGroupId().toString());

        if (!jsonObject.isPresent()) {
            throw new IllegalStateException(String.format("Unable to find member cases for the groupId %s", informantRegisterDocumentRequest.getGroupId()));
        }

        final List<ProsecutionCase> prosecutionCases = jsonObject.get().getJsonArray("prosecutionCases")
                .getValuesAs(JsonObject.class)
                .stream()
                .map(pc -> jsonObjectToObjectConverter.convert(pc, ProsecutionCase.class))
                .toList();

        inflateInformantRegister(informantRegisterDocumentRequest, prosecutionCases);
    }

    private void inflateInformantRegister(final InformantRegisterDocumentRequest informantRegisterDocumentRequest,
                                          final List<ProsecutionCase> prosecutionCases) {
        if (isNotEmpty(informantRegisterDocumentRequest.getHearingVenue().getCourtSessions())) {
            final InformantRegisterDefendant masterDefendant = informantRegisterDocumentRequest.getHearingVenue()
                    .getCourtSessions().get(0)
                    .getDefendants().get(0);
            informantRegisterDocumentRequest.getHearingVenue()
                    .getCourtSessions().get(0)
                    .getDefendants()
                    .addAll(getDefendants(masterDefendant, prosecutionCases));
        }
    }

    @Handles("results.command.generate-informant-register")
    public void handleGenerateInformantRegister(final Envelope<GenerateInformantRegister> jsonEnvelope) {
        final Map<UUID, List<JsonObject>> informantRegisterDocumentRequests = getInformantRegisterDocumentRequests(jsonEnvelope);
        informantRegisterDocumentRequests.forEach((informantRegisterId, informantRegisterRequest) -> processRequests(informantRegisterId, informantRegisterRequest, jsonEnvelope, true));
    }

    @Handles("results.command.generate-informant-register-by-date")
    public void handleGenerateInformantRegisterByDate(final Envelope<GenerateInformantRegisterByDate> jsonEnvelope) {
        final GenerateInformantRegisterByDate generateInformantRegisterByDate = jsonEnvelope.payload();
        final Map<UUID, List<JsonObject>> informantRegisterDocumentRequests = getInformantRegisterDocumentRequestsByDate(generateInformantRegisterByDate, jsonEnvelope);
        informantRegisterDocumentRequests.forEach((informantRegisterId, informantRegisterRequest) -> processRequests(informantRegisterId, informantRegisterRequest, jsonEnvelope, false));
    }


    @Handles("results.command.notify-informant-register")
    public void handleNotifyInformantRegister(final Envelope<NotifyInformantRegister> jsonEnvelope) throws
            EventStreamException {

        final NotifyInformantRegister notifyInformantRegister = jsonEnvelope.payload();
        final UUID informationRegisterId = getInformantRegisterStreamId(notifyInformantRegister.getProsecutionAuthorityId().toString(), notifyInformantRegister.getRegisterDate().toString());
        final EventStream eventStream = eventSource.getStreamById(informationRegisterId);
        final ProsecutionAuthorityAggregate prosecutionAuthorityAggregate = aggregateService.get(eventStream, ProsecutionAuthorityAggregate.class);
        final Stream<Object> events = prosecutionAuthorityAggregate.notifyProsecutingAuthority(notifyInformantRegister);
        appendEventsToStream(jsonEnvelope, eventStream, events);
    }


    private void processRequests(final UUID informantRegisterId, final List<JsonObject> informantRegisterRequest, final Envelope jsonEnvelope, final boolean systemGenerated) {
        try {
            final EventStream eventStream = eventSource.getStreamById(informantRegisterId);
            final List<InformantRegisterDocumentRequest> informantRegisterDocumentRequests = informantRegisterRequest.stream().map(informantRegister -> stringToJsonObjectConverter.convert(informantRegister.getString((FIELD_PAYLOAD))))
                    .map(informantRegister -> jsonObjectToObjectConverter.convert(informantRegister, InformantRegisterDocumentRequest.class))
                    .toList();

            final Stream<Object> events = Stream.of(InformantRegisterGenerated.informantRegisterGenerated()
                    .withInformantRegisterDocumentRequests(informantRegisterDocumentRequests)
                    .withSystemGenerated(systemGenerated)
                    .build());

            appendEventsToStream(jsonEnvelope, eventStream, events);
        } catch (EventStreamException e) {
            LOGGER.error("Generate informant register stream exception -->>", e);
        }
    }

    private Map<UUID, List<JsonObject>> getInformantRegisterDocumentRequests(final Envelope envelope) {
        final List<JsonObject> informantRegisterDocumentRequests = queryInformantRegistersByStatus(envelope);

        return informantRegisterDocumentRequests.stream()
                .collect(groupingBy(request -> getInformantRegisterStreamId(request.getString(FIELD_PROSECUTION_AUTHORITY_ID), request.getString(FIELD_REGISTER_DATE))));
    }

    private Map<UUID, List<JsonObject>> getInformantRegisterDocumentRequestsByDate(final GenerateInformantRegisterByDate generateInformantRegisterByDate, final Envelope envelope) {
        final List<JsonObject> informantRegisterDocumentRequests = queryInformantRegistersByDate(generateInformantRegisterByDate, envelope);

        return informantRegisterDocumentRequests.stream()
                .collect(groupingBy(request -> getInformantRegisterStreamId(request.getString(FIELD_PROSECUTION_AUTHORITY_ID), request.getString(FIELD_REGISTER_DATE))));
    }

    private List<JsonObject> queryInformantRegistersByStatus(final Envelope envelope) {
        final Metadata metadata = metadataFrom(envelope.metadata()).withName(INFORMANT_REGISTER_QUERY_BY_STATUS).build();
        final Envelope<JsonObject> requestEnvelope = Envelope.envelopeFrom(metadata, createObjectBuilder().add(FIELD_REQUEST_STATUS, RECORDED.name()).build());
        return requester.request(requestEnvelope)
                .payloadAsJsonObject()
                .getJsonArray(FIELD_INFORMANT_REGISTER_DOCUMENTS)
                .getValuesAs(JsonObject.class);
    }

    private List<JsonObject> queryInformantRegistersByDate(final GenerateInformantRegisterByDate generateInformantRegisterByDate, final Envelope envelope) {
        final Metadata metadata = metadataFrom(envelope.metadata()).withName(INFORMANT_REGISTER_QUERY_BY_DATE).build();

        final JsonObjectBuilder queryParameters = createObjectBuilder().add(FIELD_REGISTER_DATE, generateInformantRegisterByDate.getRegisterDate());
        if (isNotEmpty(generateInformantRegisterByDate.getProsecutionAuthorities())) {
            final String prosecutionAuthoritiesAsString = generateInformantRegisterByDate.getProsecutionAuthorities().stream().map(String::valueOf)
                    .collect(Collectors.joining(","));
            queryParameters.add(FIELD_PROSECUTION_AUTHORITY_CODE, prosecutionAuthoritiesAsString);
        }

        final Envelope<JsonObject> requestEnvelope = Envelope.envelopeFrom(metadata, queryParameters.build());
        return requester.request(requestEnvelope)
                .payloadAsJsonObject()
                .getJsonArray(FIELD_INFORMANT_REGISTER_DOCUMENTS)
                .getValuesAs(JsonObject.class);
    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = envelopeFrom(envelope.metadata(), NULL);
        eventStream.append(events.map(toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }
}
