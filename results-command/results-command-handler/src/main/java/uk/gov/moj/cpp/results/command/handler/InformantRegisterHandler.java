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
import uk.gov.moj.cpp.results.command.RequestInformantRegisterPublish;
import uk.gov.moj.cpp.results.command.service.ProgressionQueryService;
import uk.gov.moj.cpp.results.domain.aggregate.ProsecutionAuthorityAggregate;
import uk.gov.moj.cpp.results.domain.event.InformantRegisterPublishRequested;

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

    /**
     * Records that a resulted regular hearing owes an informant register publish, so the publish
     * survives a Service Bus failure. Before this event existed the publish happened inline on the
     * hearing-resulted event processor and a broker blip lost the register silently, with no retry
     * and no record that a publish was ever owed.
     *
     * <p>The stream id is the hearingId, so every share and re-share of one hearing lands on one
     * stream. That is deliberate and load-bearing: the framework serialises dispatch per stream, so
     * a re-share published moments after the original cannot overtake it on the way to the queue. A
     * stream per share - the requestId, say - would put the two on unrelated streams, let them
     * dispatch concurrently, and allow the consuming service to apply the older share last. It also
     * keeps stream cardinality to one per hearing rather than one per share, each of which is a
     * permanent row in the event store's stream_status and in the processor's event buffer, and each
     * of which a CATCHUP walks.
     *
     * <p>The stream is an audit log, not an idempotency key: a redelivered command appends a second
     * event rather than failing, and the framework then publishes a second, byte-identical Service
     * Bus message. So does a processor CATCHUP or stream replay, for every share ever recorded. That
     * is survivable because the requestId minted downstream is deterministic and the consuming
     * service keeps a (source, requestId) processed-log - that log is the authoritative guard, not
     * the broker, whose duplicate detection is optional and time-windowed. A genuine re-share
     * carries a new sharedTime, mints a new requestId, and must be published again.
     *
     * <p>No aggregate is loaded: nothing about this event mutates case or financial state, and no
     * aggregate reads it back. Same shape as {@link #handleAddInformantRegisterToEventStream}.
     */
    @Handles("results.command.request-informant-register-publish")
    public void handleRequestInformantRegisterPublish(final Envelope<RequestInformantRegisterPublish> envelope)
            throws EventStreamException {

        final RequestInformantRegisterPublish command = envelope.payload();

        // hearingDay and sharedTime are copied through as the raw strings from the hearing-resulted
        // event and are not re-rendered here: the requestId the publisher mints is a hash of them,
        // so reformatting either would mint a different id for the same share and defeat both dedupes.
        LOGGER.info("results.command.request-informant-register-publish for hearing {}, hearingDay {}, sharedTime {}",
                command.getHearingId(), command.getHearingDay(), command.getSharedTime());

        final EventStream eventStream = eventSource.getStreamById(command.getHearingId());
        final Stream<Object> events = Stream.of(InformantRegisterPublishRequested.informantRegisterPublishRequested()
                .withHearingId(command.getHearingId())
                .withHearingDay(command.getHearingDay())
                .withSharedTime(command.getSharedTime())
                .withUserId(command.getUserId())
                .build());

        appendEventsToStream(envelope, eventStream, events);
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

            final Stream<Object> events = Stream.of(InformantRegisterGeneratedV2.informantRegisterGeneratedV2()
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
