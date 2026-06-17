package uk.gov.moj.cpp.results.command.handler;

import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.groupingBy;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static javax.json.JsonValue.NULL;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.domains.InformantRegisterHelper.getInformantRegisterStreamId;
import static uk.gov.moj.cpp.domains.constant.RegisterStatus.RECORDED;
import static uk.gov.moj.cpp.results.command.util.DefendantMapper.getDefendants;

import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.results.courts.InformantRegisterGenerated;
import uk.gov.justice.results.courts.InformantRegisterGeneratedV2;
import uk.gov.justice.results.courts.InformantRegisterRecordedV2;
import uk.gov.justice.results.courts.informantRegisterDocument.InformantRegisterDefendant;
import uk.gov.justice.results.courts.informantRegisterDocument.InformantRegisterDocumentRequest;
import uk.gov.justice.results.courts.GenerateInformantRegister;
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

import java.util.ArrayList;
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
    private static final String FIELD_HEARING_VENUE = "hearingVenue";
    private static final String FIELD_COURT_SESSIONS = "courtSessions";
    private static final String FIELD_DEFENDANTS = "defendants";
    private static final String FIELD_CASES_OR_APPLICATIONS = "prosecutionCasesOrApplications";
    private static final String FIELD_OFFENCES = "offences";
    private static final String FIELD_VERDICT_CODE = "verdictCode";

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
        final Stream<Object> events = Stream.of(InformantRegisterRecordedV2.informantRegisterRecordedV2()
                .withProsecutionAuthorityId(prosecutionAuthorityId)
                .withInformantRegister(informantRegisterDocumentRequest)
                .build());

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

            final List<JsonObject> payloads = informantRegisterRequest.stream()
                    .map(informantRegister -> stringToJsonObjectConverter.convert(informantRegister.getString(FIELD_PAYLOAD)))
                    .toList();

            final List<uk.gov.justice.core.courts.informantRegisterDocument.InformantRegisterDocumentRequest> v1DocumentRequests = payloads.stream()
                    .filter(this::isV1InformantRegisterPayload)
                    .map(payload -> jsonObjectToObjectConverter.convert(payload, uk.gov.justice.core.courts.informantRegisterDocument.InformantRegisterDocumentRequest.class))
                    .toList();

            final List<InformantRegisterDocumentRequest> v2DocumentRequests = payloads.stream()
                    .filter(payload -> !isV1InformantRegisterPayload(payload))
                    .map(payload -> jsonObjectToObjectConverter.convert(payload, InformantRegisterDocumentRequest.class))
                    .toList();

            LOGGER.debug("Generating informant register for stream {}: {} V1 request(s), {} V2 request(s)",
                    informantRegisterId, v1DocumentRequests.size(), v2DocumentRequests.size());

            final List<Object> events = new ArrayList<>();
            if (isNotEmpty(v1DocumentRequests)) {
                events.add(InformantRegisterGenerated.informantRegisterGenerated()
                        .withInformantRegisterDocumentRequests(v1DocumentRequests)
                        .withSystemGenerated(systemGenerated)
                        .build());
            }
            if (isNotEmpty(v2DocumentRequests)) {
                events.add(InformantRegisterGeneratedV2.informantRegisterGeneratedV2()
                        .withInformantRegisterDocumentRequests(v2DocumentRequests)
                        .withSystemGenerated(systemGenerated)
                        .build());
            }

            appendEventsToStream(jsonEnvelope, eventStream, events.stream());
        } catch (EventStreamException e) {
            LOGGER.error("Generate informant register stream exception -->>", e);
        }
    }

    /**
     * Classifies a queried register payload as V1 or V2 so it can be converted to the matching document-request
     * type and emitted as the matching generated event.
     *
     * <p>This relies on an invariant of the record side: each stored payload is a whole document request recorded
     * via either {@code results.event.informant-register-recorded} (V1, flat {@code verdictCode} string on the
     * offence) or {@code results.event.informant-register-recorded-v2} (V2, structured {@code verdict} object), and
     * is validated against a single schema version at record time. A single stored payload therefore never mixes
     * offence shapes — it is uniformly V1 or uniformly V2.
     *
     * <p>Given that invariant, a payload is treated as V1 only when it positively carries the legacy top-level
     * {@code verdictCode} on an offence; structured-verdict payloads and payloads with no offences are treated as
     * V2, so a structured verdict is never silently dropped.
     */
    private boolean isV1InformantRegisterPayload(final JsonObject payload) {
        return offences(payload).anyMatch(offence -> offence.containsKey(FIELD_VERDICT_CODE));
    }

    private Stream<JsonObject> offences(final JsonObject payload) {
        final JsonObject hearingVenue = objectOrNull(payload, FIELD_HEARING_VENUE);
        if (hearingVenue == null) {
            return Stream.empty();
        }
        return arrayObjects(hearingVenue, FIELD_COURT_SESSIONS)
                .flatMap(courtSession -> arrayObjects(courtSession, FIELD_DEFENDANTS))
                .flatMap(defendant -> arrayObjects(defendant, FIELD_CASES_OR_APPLICATIONS))
                .flatMap(caseOrApplication -> arrayObjects(caseOrApplication, FIELD_OFFENCES));
    }

    private JsonObject objectOrNull(final JsonObject parent, final String field) {
        if (!parent.containsKey(field) || parent.get(field).getValueType() != JsonValue.ValueType.OBJECT) {
            return null;
        }
        return parent.getJsonObject(field);
    }

    private Stream<JsonObject> arrayObjects(final JsonObject parent, final String field) {
        if (!parent.containsKey(field) || parent.get(field).getValueType() != JsonValue.ValueType.ARRAY) {
            return Stream.empty();
        }
        return parent.getJsonArray(field).getValuesAs(JsonObject.class).stream();
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
