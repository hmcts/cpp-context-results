package uk.gov.moj.cpp.results.command.handler;

import static java.lang.Boolean.FALSE;
import static java.util.UUID.fromString;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;

import uk.gov.justice.core.courts.CaseDetails;
import uk.gov.justice.core.courts.CourtCentreWithLJA;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.SessionDay;
import uk.gov.justice.hearing.courts.HearingFinancialResultRequest;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.domains.results.shareresults.PublicHearingResulted;
import uk.gov.moj.cpp.results.domain.aggregate.HearingFinancialResultsAggregate;
import uk.gov.moj.cpp.results.domain.aggregate.ResultsAggregate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(COMMAND_HANDLER)
public class ResultsCommandHandler extends AbstractCommandHandler {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ResultsCommandHandler.class);
    private static final String HEARING_IDS = "hearingIds";
    private static final String POLICE_FLAG = "policeFlag";
    private static final String SPI_OUT_FLAG = "spiOutFlag";
    private static final String SOURCE_TYPE_SJP = "SJP";
    private static final String HEARING_DAY = "hearingDay";
    final JsonObjectToObjectConverter jsonObjectToObjectConverter;
    private final ReferenceDataService referenceDataService;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    public ResultsCommandHandler(final EventSource eventSource, final Enveloper enveloper,
                                 final AggregateService aggregateService, final JsonObjectToObjectConverter jsonObjectToObjectConverter, final ReferenceDataService referenceDataService, final ObjectToJsonObjectConverter objectToJsonObjectConverter) {
        super(eventSource, enveloper, aggregateService);
        this.jsonObjectToObjectConverter = jsonObjectToObjectConverter;
        this.referenceDataService = referenceDataService;
        this.objectToJsonObjectConverter = objectToJsonObjectConverter;
    }

    @Handles("results.command.add-hearing-result")
    public void addHearingResult(final JsonEnvelope envelope) throws EventStreamException {
        final JsonObject payload = envelope.payloadAsJsonObject();
        final PublicHearingResulted publicHearingResulted = jsonObjectToObjectConverter.convert(payload, PublicHearingResulted.class);

        aggregate(ResultsAggregate.class, fromString(payload.getJsonObject("hearing").getString("id")),
                envelope, a -> a.saveHearingResults(publicHearingResulted));
    }

    @Handles("results.command.add-hearing-result-for-day")
    public void addHearingResultForDay(final JsonEnvelope envelope) throws EventStreamException {
        final JsonObject payload = envelope.payloadAsJsonObject();
        final PublicHearingResulted publicHearingResulted = jsonObjectToObjectConverter.convert(payload, PublicHearingResulted.class);
        final LocalDate hearingDay = LocalDate.parse(payload.getString(HEARING_DAY), DateTimeFormatter.ISO_LOCAL_DATE);

        aggregate(ResultsAggregate.class, fromString(payload.getJsonObject("hearing").getString("id")),
                envelope, a -> a.saveHearingResultsForDay(publicHearingResulted, hearingDay));
    }

    @Handles("results.case-or-application-ejected")
    public void handleCaseOrApplicationEjected(final JsonEnvelope envelope) throws EventStreamException {
        final JsonObject payload = envelope.payloadAsJsonObject();
        final JsonArray hearingIds = payload.getJsonArray(HEARING_IDS);
        for (final JsonValue hearingId : hearingIds) {
            final UUID hearingUUID = fromString(((JsonString) hearingId).getString());
            aggregate(ResultsAggregate.class, hearingUUID,
                    envelope, a -> a.ejectCaseOrApplication(hearingUUID, payload));
        }
    }

    @Handles("results.command.create-results-for-day")
    public void createResultsForDay(final JsonEnvelope envelope) throws EventStreamException {
        final LocalDate hearingDay = LocalDate.parse(envelope.payloadAsJsonObject().getString(HEARING_DAY), DateTimeFormatter.ISO_LOCAL_DATE);
        createResults(envelope, Optional.of(hearingDay));
    }

    @Handles("results.create-results")
    public void createResult(final JsonEnvelope envelope) throws EventStreamException {
        createResults(envelope, Optional.empty());
    }

    @Handles("results.command.generate-police-results-for-a-defendant")
    public void generatePoliceResultsForDefendant(final JsonEnvelope envelope) throws EventStreamException {
        final JsonObject payload = envelope.payloadAsJsonObject();
        final String sessionId = payload.getString("sessionId");
        final String caseId = payload.getString("caseId");
        final String defendantId = payload.getString("defendantId");

        if (payload.containsKey(HEARING_DAY)) {
            final Optional<LocalDate> hearingDay = Optional.of(LocalDate.parse(envelope.payloadAsJsonObject().getString(HEARING_DAY), DateTimeFormatter.ISO_LOCAL_DATE));
            aggregate(ResultsAggregate.class, fromString(sessionId),
                    envelope, a -> a.generatePoliceResults(caseId, defendantId, hearingDay));
        } else {
            aggregate(ResultsAggregate.class, fromString(sessionId),
                    envelope, a -> a.generatePoliceResults(caseId, defendantId, Optional.empty()));
        }

    }

    private void createResults(final JsonEnvelope commandEnvelope, final Optional<LocalDate> hearingDay) throws EventStreamException {
        final JsonObject payload = commandEnvelope.payloadAsJsonObject();
        final JsonObject session = payload.getJsonObject("session");
        final Optional<JurisdictionType> jurisdictionType = JurisdictionType.valueFor(payload.containsKey("jurisdictionType") ? payload.getJsonString("jurisdictionType").getString() : "");
        final String id = session.getString("id");
        final String sourceType = session.getString("sourceType");
        final List<SessionDay> sessionDays = (List<SessionDay>) session.get("sessionDays");
        final List<JsonObject> cases = (List<JsonObject>) payload.get("cases");
        final CourtCentreWithLJA courtCentre = jsonObjectToObjectConverter.convert(session.getJsonObject("courtCentreWithLJA"), CourtCentreWithLJA.class);

        final EventStream eventStream = eventSource.getStreamById(fromString(id));
        final ResultsAggregate aggregate = aggregateService.get(eventStream, ResultsAggregate.class);

        aggregate(ResultsAggregate.class, fromString(id),
                commandEnvelope, a -> a.handleSession(fromString(id), courtCentre, sessionDays));

        final List<UUID> caseIdsFromAggregate = aggregate.getCaseIds();

        for (final JsonObject c : cases) {

            final CaseDetails caseDetails = jsonObjectToObjectConverter.convert(c, CaseDetails.class);

            if (SOURCE_TYPE_SJP.equalsIgnoreCase(sourceType) && caseIdsFromAggregate.contains(caseDetails.getCaseId())) {
                aggregate(ResultsAggregate.class, fromString(id),
                        commandEnvelope, a -> a.handleRejectedSjpCase(caseDetails.getCaseId()));
            } else {
                aggregate(ResultsAggregate.class, fromString(id),
                        commandEnvelope, a -> a.handleCase(caseDetails));

                final AtomicBoolean sendSpiOut = new AtomicBoolean(FALSE);
                final AtomicBoolean isPoliceProsecutor = new AtomicBoolean(FALSE);
                final AtomicReference<String> prosecutorEmailAddress = new AtomicReference("");

                final Optional<JsonObject> refDataProsecutorJson = referenceDataService.getSpiOutFlagForProsecutionAuthorityCode(caseDetails.getProsecutionAuthorityCode());
                refDataProsecutorJson.ifPresent(prosecutorJson -> {
                    sendSpiOut.set(getFlagValue(SPI_OUT_FLAG, prosecutorJson));
                    isPoliceProsecutor.set(getFlagValue(POLICE_FLAG, prosecutorJson));
                    prosecutorEmailAddress.set(getEmailAddress(prosecutorJson));
                });

                LOGGER.info("SPI OUT flag is '{}' and police prosecutor flag is '{}' for case with prosecution authority code '{}'", sendSpiOut.get(), isPoliceProsecutor.get(), caseDetails.getProsecutionAuthorityCode());

                aggregate(ResultsAggregate.class, fromString(id),
                        commandEnvelope, a -> a.handleDefendants(caseDetails, sendSpiOut.get(), jurisdictionType, prosecutorEmailAddress.get(), isPoliceProsecutor.get(), hearingDay));
            }

        }
    }

    @Handles("results.command.track-results")
    public void trackResult(final JsonEnvelope envelope) throws EventStreamException {
        final HearingFinancialResultRequest hearingFinancialResultRequest = jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), HearingFinancialResultRequest.class);
        LOGGER.info("masterDefandantId : {} HearingFinancialResultRequest:{}", hearingFinancialResultRequest.getMasterDefendantId(), objectToJsonObjectConverter.convert(hearingFinancialResultRequest));

        final EventStream eventStream = eventSource.getStreamById(hearingFinancialResultRequest.getMasterDefendantId());
        final HearingFinancialResultsAggregate aggregate = aggregateService.get(eventStream, HearingFinancialResultsAggregate.class);

        final Stream<Object> updateEvents = aggregate.updateFinancialResults(hearingFinancialResultRequest);

        eventStream.append(updateEvents.map(enveloper.withMetadataFrom(envelope)));

        LOGGER.info("masterDefandantId : {} HearingFinancialResultsAggregate:{}", hearingFinancialResultRequest.getMasterDefendantId(), objectToJsonObjectConverter.convert(aggregate));
    }

    private boolean getFlagValue(String key, JsonObject prosecutorJson) {
        return prosecutorJson.containsKey(key) ? prosecutorJson.getBoolean(key) : FALSE;
    }

    private String getEmailAddress(JsonObject prosecutorJson) {
        return prosecutorJson.containsKey("contactEmailAddress") ? prosecutorJson.getString("contactEmailAddress") : "";
    }

}
