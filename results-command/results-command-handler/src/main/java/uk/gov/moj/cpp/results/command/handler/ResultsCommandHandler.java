package uk.gov.moj.cpp.results.command.handler;

import static java.lang.Boolean.FALSE;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static javax.json.JsonValue.NULL;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.core.courts.CaseDetails;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtCentreWithLJA;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.Offence;
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
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.domains.results.defendanttracking.UpdateDefendantTracking;
import uk.gov.moj.cpp.domains.results.shareresults.PublicHearingResulted;
import uk.gov.moj.cpp.results.domain.aggregate.DefendantAggregate;
import uk.gov.moj.cpp.results.domain.aggregate.HearingFinancialResultsAggregate;
import uk.gov.moj.cpp.results.domain.aggregate.ResultsAggregate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
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
    public static final String SURREY_POLICE_CPS_ORGANISATION = "A45AA00";
    public static final String SUSSEX_POLICE_CPS_ORGANISATION = "A47AA00";
    private static final String A_4 = "A4";
    private static final String ZERO_FOUR = "04";

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
        createResults(envelope, of(hearingDay));
    }

    @Handles("results.create-results")
    public void createResult(final JsonEnvelope envelope) throws EventStreamException {
        createResults(envelope, empty());
    }

    @Handles("results.command.generate-police-results-for-a-defendant")
    public void generatePoliceResultsForDefendant(final JsonEnvelope envelope) throws EventStreamException {
        final JsonObject payload = envelope.payloadAsJsonObject();
        final String sessionId = payload.getString("sessionId");
        final String caseId = payload.getString("caseId");
        final String defendantId = payload.getString("defendantId");

        if (payload.containsKey(HEARING_DAY)) {
            final Optional<LocalDate> hearingDay = of(LocalDate.parse(envelope.payloadAsJsonObject().getString(HEARING_DAY), DateTimeFormatter.ISO_LOCAL_DATE));
            aggregate(ResultsAggregate.class, fromString(sessionId),
                    envelope, a -> a.generatePoliceResults(caseId, defendantId, hearingDay));
        } else {
            aggregate(ResultsAggregate.class, fromString(sessionId),
                    envelope, a -> a.generatePoliceResults(caseId, defendantId, empty()));
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
        final List<JsonObject> courtApplications = ofNullable((List<JsonObject>) payload.get("courtApplications"))
                .orElse(Collections.emptyList());


        final CourtCentreWithLJA courtCentre = jsonObjectToObjectConverter.convert(session.getJsonObject("courtCentreWithLJA"), CourtCentreWithLJA.class);

        final EventStream eventStream = eventSource.getStreamById(fromString(id));
        final ResultsAggregate aggregate = aggregateService.get(eventStream, ResultsAggregate.class);

        aggregate(ResultsAggregate.class, fromString(id),
                commandEnvelope, a -> a.handleSession(fromString(id), courtCentre, sessionDays));

        final List<UUID> caseIdsFromAggregate = aggregate.getCaseIds();
        final List<CourtApplication> courtApplicationList = new ArrayList<>();
        for (final JsonObject jsonObject : courtApplications) {
            final CourtApplication courtApplication = jsonObjectToObjectConverter.convert(jsonObject, CourtApplication.class);
            courtApplicationList.add(courtApplication);
        }

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
                final AtomicReference<String> prosecutorEmailAddress = new AtomicReference<>("");

                final String originatingOrganisation = getOriginatingOrganisation(caseDetails.getOriginatingOrganisation());
                if (isNotEmpty(originatingOrganisation)) {
                    final Optional<JsonObject> refDataProsecutorJson = referenceDataService.getSpiOutFlagForOriginatingOrganisation(originatingOrganisation);


                    refDataProsecutorJson.ifPresent(prosecutorJson -> {
                        sendSpiOut.set(getFlagValue(SPI_OUT_FLAG, prosecutorJson));
                        isPoliceProsecutor.set(getFlagValue(POLICE_FLAG, prosecutorJson));
                        prosecutorEmailAddress.set(getEmailAddress(prosecutorJson));
                    });
                } else {
                    final Optional<JsonObject> refDataProsecutorJson = referenceDataService.getSpiOutFlagForProsecutionAuthorityCode(caseDetails.getProsecutionAuthorityCode());
                    refDataProsecutorJson.ifPresent(prosecutorJson -> {
                        sendSpiOut.set(getFlagValue(SPI_OUT_FLAG, prosecutorJson));
                        isPoliceProsecutor.set(getFlagValue(POLICE_FLAG, prosecutorJson));
                        prosecutorEmailAddress.set(getEmailAddress(prosecutorJson));
                    });
                }

                LOGGER.info("SPI OUT flag is '{}' and police prosecutor flag is '{}' for case with prosecution authority code '{}'", sendSpiOut.get(), isPoliceProsecutor.get(), caseDetails.getProsecutionAuthorityCode());
                final String applicationTypeForCase = getApplicationTypeForCase(caseDetails.getCaseId(), courtApplicationList);
                aggregate(ResultsAggregate.class, fromString(id),
                        commandEnvelope, a -> a.handleDefendants(caseDetails, sendSpiOut.get(), jurisdictionType, prosecutorEmailAddress.get(), isPoliceProsecutor.get(), hearingDay, applicationTypeForCase));
            }
        }
    }


    private String getApplicationTypeForCase(UUID caseId, List<CourtApplication> courtApplications) {
        final List<CourtApplication> courtApplicationList = courtApplications.stream()
                .filter(courtApplication -> courtApplication.getCourtApplicationCases() != null)
                .filter(courtApplication -> courtApplication.getCourtApplicationCases().stream()
                        .anyMatch(courtApplicationCase -> courtApplicationCase.getProsecutionCaseId().equals(caseId))).collect(toList());

        return courtApplicationList.stream()
                .map(courtApplication -> courtApplication.getType() != null ? courtApplication.getType().getType() : "")
                .filter(type -> type != null && !type.isEmpty())
                .distinct()
                .collect(joining(", "));
    }


    private String getOriginatingOrganisation(final String originatingOrganisation) {
        if (SURREY_POLICE_CPS_ORGANISATION.equalsIgnoreCase(originatingOrganisation) || SUSSEX_POLICE_CPS_ORGANISATION.equalsIgnoreCase(originatingOrganisation)) {
            return originatingOrganisation.replace(A_4, ZERO_FOUR);
        }
        return originatingOrganisation;
    }

    @Handles("results.command.update-defendant-tracking-status")
    public void updateDefendantTrackingStatus(final JsonEnvelope envelope) throws EventStreamException {

        final JsonObject defendantTrackingPayload = envelope.payloadAsJsonObject();
        final UpdateDefendantTracking updateDefendantTracking = jsonObjectToObjectConverter.convert(defendantTrackingPayload, UpdateDefendantTracking.class);
        final UUID defendantId = updateDefendantTracking.getDefendantId();

        LOGGER.info("results.command.update-defendant-tracking-status received for defendant: {}", defendantId);

        final List<Offence> offenceList = updateDefendantTracking.getOffences();

        final EventStream eventStream = eventSource.getStreamById(defendantId);
        final DefendantAggregate aggregate = aggregateService.get(eventStream, DefendantAggregate.class);

        final Stream<Object> event = aggregate.updateDefendantTrackingStatus(defendantId, offenceList);

        appendEventsToStream(envelope, eventStream, event);


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

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = envelopeFrom(envelope.metadata(), NULL);
        eventStream.append(events.map(toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }

}
