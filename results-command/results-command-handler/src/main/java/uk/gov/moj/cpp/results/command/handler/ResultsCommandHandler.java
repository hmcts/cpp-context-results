package uk.gov.moj.cpp.results.command.handler;

import static java.lang.Boolean.FALSE;
import static java.util.UUID.fromString;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;

import uk.gov.justice.core.courts.CaseDetails;
import uk.gov.justice.core.courts.CourtCentreWithLJA;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.SessionDay;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.domains.results.shareresults.PublicHearingResulted;
import uk.gov.moj.cpp.results.domain.aggregate.ResultsAggregate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;

@ServiceComponent(COMMAND_HANDLER)
public class ResultsCommandHandler extends AbstractCommandHandler {

    private static final String HEARING_IDS = "hearingIds";
    private static final String POLICE_FLAG = "policeFlag";
    private static final String SPI_OUT_FLAG = "spiOutFlag";
    private static final String SOURCE_TYPE_SJP = "SJP";
    final JsonObjectToObjectConverter jsonObjectToObjectConverter;
    private final ReferenceDataService referenceDataService;


    @Inject
    public ResultsCommandHandler(final EventSource eventSource, final Enveloper enveloper,
                                 final AggregateService aggregateService, final JsonObjectToObjectConverter jsonObjectToObjectConverter, final ReferenceDataService referenceDataService) {
        super(eventSource, enveloper, aggregateService);
        this.jsonObjectToObjectConverter = jsonObjectToObjectConverter;
        this.referenceDataService = referenceDataService;
    }

    @Handles("results.command.add-hearing-result")
    public void addHearingResult(final JsonEnvelope envelope) throws EventStreamException {
        final JsonObject payload = envelope.payloadAsJsonObject();
        final PublicHearingResulted publicHearingResulted = jsonObjectToObjectConverter.convert(payload, PublicHearingResulted.class);

        aggregate(ResultsAggregate.class, fromString(payload.getJsonObject("hearing").getString("id")),
                envelope, a -> a.saveHearingResults(publicHearingResulted));
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

    @Handles("results.create-results")
    public void createResult(final JsonEnvelope envelope) throws EventStreamException {
        final JsonObject payload = envelope.payloadAsJsonObject();
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
                envelope, a -> a.handleSession(fromString(id), courtCentre, sessionDays));

        final List<UUID> caseIdsFromAggregate = aggregate.getCaseIds();

        for (final JsonObject c : cases) {

            final CaseDetails caseDetails = jsonObjectToObjectConverter.convert(c, CaseDetails.class);

            if (SOURCE_TYPE_SJP.equalsIgnoreCase(sourceType) && caseIdsFromAggregate.contains(caseDetails.getCaseId())) {
                aggregate(ResultsAggregate.class, fromString(id),
                        envelope, a -> a.handleRejectedSjpCase(caseDetails.getCaseId()));
            } else {
                aggregate(ResultsAggregate.class, fromString(id),
                        envelope, a -> a.handleCase(caseDetails));

                final AtomicBoolean sendSpiOut = new AtomicBoolean(FALSE);
                final AtomicBoolean isPoliceProsecutor = new AtomicBoolean(FALSE);
                final AtomicReference<String> prosecutorEmailAddress = new AtomicReference("");

                final Optional<JsonObject> refDataProsecutorJson = referenceDataService.getSpiOutFlagForOriginatingOrganisation(caseDetails.getOriginatingOrganisation());
                refDataProsecutorJson.ifPresent(prosecutorJson -> {
                    sendSpiOut.set(getFlagValue(SPI_OUT_FLAG, prosecutorJson));
                    isPoliceProsecutor.set(getFlagValue(POLICE_FLAG, prosecutorJson));
                    prosecutorEmailAddress.set(getEmailAddress(prosecutorJson));
                });


                aggregate(ResultsAggregate.class, fromString(id),
                        envelope, a -> a.handleDefendants(caseDetails, sendSpiOut.get(), jurisdictionType, prosecutorEmailAddress.get(), isPoliceProsecutor.get()));
            }

        }
    }

    @Handles("results.command.generate-police-results-for-a-defendant")
    public void generatePoliceResultsForDefendant(final JsonEnvelope envelope) throws
            EventStreamException {
        final JsonObject payload = envelope.payloadAsJsonObject();
        final String sessionId = payload.getString("sessionId");
        final String caseId = payload.getString("caseId");
        final String defendantId = payload.getString("defendantId");

        final EventStream eventStream = eventSource.getStreamById(fromString(sessionId));
        final ResultsAggregate aggregate = aggregateService.get(eventStream, ResultsAggregate.class);

        final String originatingOrg = aggregate.getOriginatingOrganisation();
        boolean sendSpiOut = false;
        if(null != originatingOrg) {
            sendSpiOut = getSpiOutFlag(originatingOrg);
        }

        if (sendSpiOut) {
            aggregate(ResultsAggregate.class, fromString(sessionId),
                    envelope, a -> a.generatePoliceResults(caseId, defendantId));
        }
    }

    private boolean getSpiOutFlag(final String originatingOrg) {
        final Optional<JsonObject> refDataProsecutorJson = referenceDataService.getSpiOutFlagForOriginatingOrganisation(originatingOrg);
        return refDataProsecutorJson.filter(jsonObject -> jsonObject.containsKey(SPI_OUT_FLAG) ? jsonObject.getBoolean(SPI_OUT_FLAG) : FALSE).isPresent();
    }

    private boolean getFlagValue(String key, JsonObject prosecutorJson) {
        return prosecutorJson.containsKey(key) ? prosecutorJson.getBoolean(key) : FALSE;
    }

    private String getEmailAddress(JsonObject prosecutorJson) {
        return prosecutorJson.containsKey("contactEmailAddress") ? prosecutorJson.getString("contactEmailAddress") : "";
    }

}
