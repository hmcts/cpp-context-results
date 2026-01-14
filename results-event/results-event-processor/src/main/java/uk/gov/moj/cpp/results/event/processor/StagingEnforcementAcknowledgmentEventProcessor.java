package uk.gov.moj.cpp.results.event.processor;

import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.JsonObjects.getJsonObject;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.moj.cpp.results.event.service.ProgressionService;
import uk.gov.moj.cpp.results.event.service.ReferenceDataService;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class StagingEnforcementAcknowledgmentEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(StagingEnforcementAcknowledgmentEventProcessor.class);
    private static final String ORIGINATOR = "originator";
    private static final String COURTS = "Courts";
    private static final String ATCM = "ATCM";
    private static final String ACKNOWLEDGEMENT = "acknowledgement";
    private static final String REQUEST_ID = "requestId";
    private static final String ERROR_CODE = "errorCode";
    private static final String ACCOUNT_NUMBER = "accountNumber";
    private static final String CORRELATION_ID ="correlationId";
    private static final String MASTER_DEFENDANT_ID ="masterDefendantId";
    private static final String ACCOUNT_CORRELATION_ID ="accountCorrelationId";
    private static final String HEARING_FINANCIAL_RESULT_REQUEST= "hearingFinancialResultRequest";
    private static final String CASE_IDS = "caseIds";
    private static final String INACTIVE_MIGRATED_CASE_SUMMARIES = "inactiveMigratedCaseSummaries";
    private static final String INACTIVE_CASE_SUMMARY = "inactiveCaseSummary";
    private static final String MIGRATION_SOURCE_SYSTEM = "migrationSourceSystem";
    private static final String DEFENDANT_FINE_ACCOUNT_NUMBERS = "defendantFineAccountNumbers";
    private static final String FINE_ACCOUNT_NUMBER = "fineAccountNumber";
    private static final String COURT_EMAIL = "courtEmail";
    private static final String DEFENDANTS = "defendants";
    private static final String DEFENDANT_ID = "defendantId";


    @Inject
    private Sender sender;

    @Inject
    private ProgressionService progressionService;

    @Inject
    private ReferenceDataService referenceDataService;

    @Handles("public.stagingenforcement.enforce-financial-imposition-acknowledgement")
    public void processAcknowledgement(final JsonEnvelope event) {
        final JsonObject enforcementResponsePayload = event.payloadAsJsonObject();
        final Optional<String> originator = JsonObjects.getString(enforcementResponsePayload, ORIGINATOR);

        if (originator.isPresent()
                && (COURTS.equalsIgnoreCase(originator.get()) || ATCM.equalsIgnoreCase(originator.get()))) {
            final Optional<JsonObject> acknowledgement = getJsonObject(enforcementResponsePayload, ACKNOWLEDGEMENT);
            final Optional<String> optionalRequestId = JsonObjects.getString(enforcementResponsePayload, REQUEST_ID);
            final String requestId = optionalRequestId.orElseThrow(() -> new IllegalArgumentException("RequestId is mandatory from enforcement"));

            acknowledgement.map(ack -> JsonObjects.getString(ack, ERROR_CODE))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .ifPresent(error -> LOGGER.error("Acknowledgement has an error {} ", acknowledgement.get()));

            acknowledgement.map(ack -> JsonObjects.getString(ack, ACCOUNT_NUMBER))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .ifPresent(accountNumber -> updateGobAccount(event, accountNumber, requestId));

        }
    }

    @Handles("results.event.hearing-financial-results-tracked")
    public void processHearingFinancialResultsTracked(final JsonEnvelope event){
        final JsonObject enforcementResponsePayload = event.payloadAsJsonObject();
        final JsonObject hearingFinancialResultRequest = enforcementResponsePayload.getJsonObject(HEARING_FINANCIAL_RESULT_REQUEST);
        final String masterDefendantId = hearingFinancialResultRequest.getString(MASTER_DEFENDANT_ID);

        if(hearingFinancialResultRequest.containsKey(ACCOUNT_CORRELATION_ID)){
            final String correlationId = hearingFinancialResultRequest.getString(ACCOUNT_CORRELATION_ID);
            updaterCorrelationId(event, correlationId, masterDefendantId);
        }

    }

    @Handles("public.hearing.nces-email-notification-for-application")
    public void processSendNcesMailForNewApplication(final JsonEnvelope event){ //Analyse for CCT-2266
        final JsonObject payload = event.payloadAsJsonObject();

        Envelope<JsonObject> envelope = null;

        final String masterDefendantId = payload.getString(MASTER_DEFENDANT_ID);

        final String hearingCourtCentreId = payload.getString("hearingCourtCentreId");

        final JsonObject organisationUnitPayload = referenceDataService.getOrganisationUnit(hearingCourtCentreId, event);

        final JsonObject enforcementArea = of("enforcementArea")
                .filter(organisationUnitPayload::containsKey)
                .map(organisationUnitPayload::getJsonObject).orElse(createObjectBuilder().build());

        final Optional<String> ncesNotificationEmail = of("ncesNotificationEmail")
                .filter(enforcementArea::containsKey)
                .map(enforcementArea::getString);

        final String ncesNotificationEmailAddress = ncesNotificationEmail.orElse(null);

        final List<String> caseIds = ofNullable(payload.getJsonArray(CASE_IDS))
                .map(jsonArray -> jsonArray.stream()
                        .map(i -> ((JsonString) i).getString())
                        .collect(Collectors.toList()))
                .orElse(Collections.emptyList()); // Returns empty list if key is missing

        if(CollectionUtils.isNotEmpty(caseIds)) {
            final Optional<JsonObject> inactiveMigratedCases = progressionService.getInactiveMigratedCasesByCaseIds(caseIds);
            final List<String> fineAccountNumbers = extractFineAccountNumbers(inactiveMigratedCases, masterDefendantId);

            final JsonObjectBuilder enrichedPayload = createObjectBuilder(payload);

            if (!fineAccountNumbers.isEmpty() && ncesNotificationEmailAddress != null) {

                final JsonObject courtEmailAndFineAccount = createObjectBuilder()
                        .add(COURT_EMAIL, ncesNotificationEmailAddress)
                        .add(FINE_ACCOUNT_NUMBER, fineAccountNumbers.get(0))
                        .build();

                enrichedPayload.add("migratedMasterDefendantCourtEmailAndFineAccount", courtEmailAndFineAccount);

                envelope = envelop(enrichedPayload.build())
                        .withName("result.command.send-nces-email-for-application")
                        .withMetadataFrom(event);
            }
        } else {
            envelope = envelop(event.payloadAsJsonObject()).withName("result.command.send-nces-email-for-application").withMetadataFrom(event);
        }
            this.sender.sendAsAdmin(envelope);
    }

    private List<String> extractFineAccountNumbers(final Optional<JsonObject> inactiveMigratedCases, final String masterDefendantId) {
        return inactiveMigratedCases
                .map(response -> response.getJsonArray(INACTIVE_MIGRATED_CASE_SUMMARIES))
                .map(caseSummaries -> caseSummaries.getValuesAs(JsonObject.class).stream())
                .orElse(Stream.empty())
                .map(cs -> cs.getJsonObject(INACTIVE_CASE_SUMMARY))
                .filter(summary -> summary.containsKey(DEFENDANTS) && summary.containsKey(MIGRATION_SOURCE_SYSTEM))
                .flatMap(summary -> {
                    final Set<String> matchingDefendantIds = summary.getJsonArray(DEFENDANTS)
                            .getValuesAs(JsonObject.class).stream()
                            .filter(def -> masterDefendantId.equals(def.getString(MASTER_DEFENDANT_ID)))
                            .map(def -> def.getString(DEFENDANT_ID))
                            .collect(Collectors.toSet());
                    
                    return summary.getJsonObject(MIGRATION_SOURCE_SYSTEM)
                            .getJsonArray(DEFENDANT_FINE_ACCOUNT_NUMBERS)
                            .getValuesAs(JsonObject.class).stream()
                            .filter(fa -> matchingDefendantIds.contains(fa.getString(DEFENDANT_ID)))
                            .map(fa -> fa.getString(FINE_ACCOUNT_NUMBER));
                })
                .collect(Collectors.toList());
    }

    @Handles("public.progression.defendant-address-changed")
    public void updateDefendantAddressInAggregateForNewApplication(final JsonEnvelope event){
        final Envelope<JsonObject> envelope = envelop(event.payloadAsJsonObject()).withName("result.command.update-defendant-address-for-application").withMetadataFrom(event);
        this.sender.sendAsAdmin(envelope);
    }

    private void updateGobAccount(JsonEnvelope event, String accountNumber, String correlationId) {
            final JsonObject commandPayload = createObjectBuilder().add(ACCOUNT_NUMBER, accountNumber).add(CORRELATION_ID, correlationId).build();
            final Envelope<JsonObject> envelope = envelop(commandPayload).withName("result.command.update-gob-account").withMetadataFrom(event);
            this.sender.sendAsAdmin(envelope);
    }

    private void updaterCorrelationId(JsonEnvelope event, String correlationId, String masterDefendantId) {
        final JsonObject commandPayload = createObjectBuilder().add(CORRELATION_ID, correlationId).add(MASTER_DEFENDANT_ID, masterDefendantId).build();
        final Envelope<JsonObject> envelope = envelop(commandPayload).withName("result.command.add-correlation-id").withMetadataFrom(event);
        this.sender.sendAsAdmin(envelope);
    }


}
