package uk.gov.moj.cpp.results.event.processor;

import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.JsonObjects.getJsonObject;
import static uk.gov.moj.cpp.results.event.processor.MigrationConstants.Case;
import static uk.gov.moj.cpp.results.event.processor.MigrationConstants.Defendant;
import static uk.gov.moj.cpp.results.event.processor.MigrationConstants.InactiveMigratedCase;
import static uk.gov.moj.cpp.results.event.processor.MigrationConstants.Offence;
import static uk.gov.moj.cpp.results.event.processor.MigrationConstants.PersonDetails;
import static uk.gov.moj.cpp.results.event.processor.MigrationConstants.ReferenceData;

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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import javax.json.JsonValue;

import org.checkerframework.checker.nullness.qual.Nullable;
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

    private record NcesNotificationDetails(String email, String division) {}
    public record FineAccount(String caseId, String fineAccountNumber, String caseIdentifier, String caseURN) {}
    public record EnrichedFineDetail(FineAccount fineAccount, DefendantDetails defendant) {}
    public record DefendantDetails(String defendantId, String defendantName, String defendantAddress, String originalDateOfConviction,
                                   String defendantEmail, String defendantDateOfBirth, String defendantContactNumber) {}

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
    public void processSendNcesMailForNewApplication(final JsonEnvelope event) { //Analyse for CCT-2266
        final JsonObject payload = event.payloadAsJsonObject();

        final Envelope<JsonObject> requestEnvelope = envelop(event.payloadAsJsonObject()).withName("result.command.send-nces-email-for-application").withMetadataFrom(event);

        final List<String> caseIds = ofNullable(payload.getJsonArray(Case.CASE_IDS))
                .map(jsonArray -> jsonArray.stream()
                        .map(i -> ((JsonString) i).getString())
                        .collect(toList()))
                .orElse(emptyList());

        if (isNotEmpty(caseIds)) {
            final Optional<JsonObject> inActiveMigratedCases = progressionService.getInactiveMigratedCasesByCaseIds(caseIds);
            final String masterDefendantId = payload.getString(MASTER_DEFENDANT_ID);

            List<EnrichedFineDetail> enrichedDetails = inActiveMigratedCases
                    .map(json -> extractAllEnrichedData(json, masterDefendantId))
                    .orElse(Collections.emptyList());

            NcesNotificationDetails ncesNotificationDetails = null;
            if (isNotEmpty(enrichedDetails)) {
                ncesNotificationDetails = extractNcesNotificationEmail(event, payload);
            }

            if (isNotEmpty(enrichedDetails) && nonNull(ncesNotificationDetails) && nonNull(ncesNotificationDetails.email())) {
                for (EnrichedFineDetail item : enrichedDetails) {

                    final JsonObjectBuilder builder = createObjectBuilder()
                            .add(MASTER_DEFENDANT_ID, masterDefendantId)
                            .add(Case.ID, item.fineAccount().caseId())
                            .add(MigrationConstants.FineAccount.FINE_ACCOUNT_NUMBER, item.fineAccount().fineAccountNumber())
                            .add(InactiveMigratedCase.MIGRATION_SOURCE_SYSTEM_CASE_IDENTIFIER, item.fineAccount().caseIdentifier())
                            .add(Case.COURT_EMAIL, ncesNotificationDetails.email())
                            .add(Case.DIVISION, ncesNotificationDetails.division())
                            .add(Defendant.ID, item.defendant().defendantId())
                            .add(Defendant.NAME, item.defendant().defendantName())
                            .add(Defendant.ADDRESS, item.defendant().defendantAddress())
                            .add(Defendant.ORIGINAL_DATE_OF_CONVICTION, item.defendant().originalDateOfConviction())
                            .add(Case.URN, item.fineAccount().caseURN());

                    addIfNotNull(builder, Defendant.EMAIL, item.defendant().defendantEmail());
                    addIfNotNull(builder, Defendant.DATE_OF_BIRTH, item.defendant().defendantDateOfBirth());
                    addIfNotNull(builder, Defendant.CONTACT_NUMBER, item.defendant().defendantContactNumber());

                    final JsonObject fineAccountInfo = builder.build();

                    final JsonObjectBuilder migratedInactivePayload = createObjectBuilder(payload);
                    migratedInactivePayload.add(MigrationConstants.MIGRATED_MASTER_DEFENDANT_COURT_EMAIL_AND_FINE_ACCOUNT, fineAccountInfo);

                    this.sender.sendAsAdmin(envelop(migratedInactivePayload.build())
                            .withName(MigrationConstants.RESULT_COMMAND_SEND_MIGRATED_INACTIVE_NCES_EMAIL_FOR_APPLICATION)
                            .withMetadataFrom(event));
                }
            }
            this.sender.sendAsAdmin(requestEnvelope);
        } else {
            this.sender.sendAsAdmin(requestEnvelope);

        }
    }
    private void addIfNotNull(final JsonObjectBuilder builder, final String key, final String value) {
        if (value != null) {
            builder.add(key, value);
        }
    }

    private @Nullable NcesNotificationDetails extractNcesNotificationEmail(final JsonEnvelope event, final JsonObject payload) {

        if (!payload.containsKey(ReferenceData.HEARING_COURT_CENTRE_ID) || payload.isNull(ReferenceData.HEARING_COURT_CENTRE_ID)) {
            return null;
        }
        final String hearingCourtCentreId = payload.getString(ReferenceData.HEARING_COURT_CENTRE_ID);

        final JsonObject organisationUnitPayload = referenceDataService.getOrganisationUnit(hearingCourtCentreId, event);

        final JsonObject enforcementArea = of(ReferenceData.ENFORCEMENT_AREA)
                .filter(organisationUnitPayload::containsKey)
                .map(organisationUnitPayload::getJsonObject).orElse(createObjectBuilder().build());

        final Optional<String> ncesNotificationEmail = of(ReferenceData.NCES_NOTIFICATION_EMAIL)
                .filter(enforcementArea::containsKey)
                .map(enforcementArea::getString);

        final Optional<String> divisionCode = of(ReferenceData.DIVISION_CODE)
                .filter(organisationUnitPayload::containsKey)
                .map(organisationUnitPayload::getString);

        if (ncesNotificationEmail.isPresent() && divisionCode.isPresent()) {
            return new NcesNotificationDetails(ncesNotificationEmail.get(), divisionCode.get());
        }

        return null;
    }

    private List<EnrichedFineDetail> extractAllEnrichedData(JsonObject json, String masterId) {
        return json.getJsonArray(InactiveMigratedCase.INACTIVE_MIGRATED_CASE_SUMMARIES).stream()
                .map(JsonValue::asJsonObject)
                .filter(obj -> obj.containsKey(InactiveMigratedCase.INACTIVE_CASE_SUMMARY))
                .map(obj -> obj.getJsonObject(InactiveMigratedCase.INACTIVE_CASE_SUMMARY))
                .flatMap(caseSummary -> {
                    String caseId = caseSummary.getString(InactiveMigratedCase.ID);
                    String caseURN = caseSummary.getString(Case.URN);

                    // Navigate into migrationSourceSystem
                    JsonObject sourceSystem = caseSummary.getJsonObject(InactiveMigratedCase.MIGRATION_SOURCE_SYSTEM);
                    String caseIdentifier = sourceSystem.getString(InactiveMigratedCase.MIGRATION_SOURCE_SYSTEM_CASE_IDENTIFIER);

                    return caseSummary.getJsonArray(Defendant.DEFENDANTS).stream()
                            .map(JsonValue::asJsonObject)
                            .filter(def -> masterId.equals(def.getString(MASTER_DEFENDANT_ID)))
                            .flatMap(def -> {
                                String currentDefId = def.getString(Defendant.ID);
                                DefendantDetails details = mapToDefendantDetails(def);

                                return sourceSystem.getJsonArray(InactiveMigratedCase.DEFENDANT_FINE_ACCOUNT_NUMBERS).stream()
                                        .map(JsonValue::asJsonObject)
                                        // This matches the defendantId from the account to the defendant in the loop
                                        .filter(fa -> currentDefId.equals(fa.getString(Defendant.ID)))
                                        .map(fa -> new EnrichedFineDetail(
                                                new FineAccount(caseId, fa.getString(MigrationConstants.FineAccount.FINE_ACCOUNT_NUMBER), caseIdentifier, caseURN),
                                                details)
                                        );
                            });
                })
                .collect(Collectors.toList());
    }

    private DefendantDetails mapToDefendantDetails(JsonObject defendantJson) {
        if (defendantJson == null) {
            return new DefendantDetails("", "", "", "", "", "", "");
        }

        final String defendantId = defendantJson.getString(Defendant.ID, "");

        final JsonObject details = Optional.of(defendantJson)
                .map(d -> d.getJsonObject(PersonDetails.PERSON_DEFENDANT))
                .map(pd -> pd.getJsonObject(PersonDetails.PERSON_DETAILS))
                .orElse(JsonValue.EMPTY_JSON_OBJECT);

        final String defendantName = (details.getString(PersonDetails.FIRST_NAME, "") + " " + details.getString(PersonDetails.LAST_NAME, "")).trim();

        final String defendantAddress = Optional.ofNullable(details.getJsonObject(PersonDetails.ADDRESS))
                .map(addr -> Stream.of(PersonDetails.ADDRESS_1, PersonDetails.ADDRESS_2, PersonDetails.ADDRESS_3, PersonDetails.ADDRESS_4, PersonDetails.ADDRESS_5, PersonDetails.POSTCODE)
                        .map(key -> addr.getString(key, ""))
                        .filter(val -> !val.isEmpty())
                        .collect(Collectors.joining(" ")))
                .orElse("");

        final String originalDateOfConviction = Optional.ofNullable(defendantJson.getJsonArray(Offence.OFFENCES))
                .map(array -> array.getValuesAs(JsonObject.class).stream())
                .orElse(Stream.empty())
                .map(offence -> offence.getString(Offence.CONVICTION_DATE, ""))
                .filter(date -> !date.isEmpty())
                .collect(Collectors.joining(" "));

        final JsonObject contact = details.getJsonObject(PersonDetails.CONTACT);

        final String email = Optional.ofNullable(contact)
                .map(c -> c.getString(PersonDetails.PRIMARY_EMAIL, null))
                .orElse(null);

        final String dob = details.getString(PersonDetails.DATE_OF_BIRTH, null);

        final String phone = Optional.ofNullable(contact)
                .map(c -> c.getString(PersonDetails.WORK,
                        c.getString(PersonDetails.MOBILE,
                                c.getString(PersonDetails.HOME, null))))
                .orElse(null);

        return new DefendantDetails(defendantId, defendantName, defendantAddress, originalDateOfConviction, email, dob, phone);
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
