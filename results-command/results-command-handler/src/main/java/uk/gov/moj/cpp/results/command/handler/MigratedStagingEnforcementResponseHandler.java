package uk.gov.moj.cpp.results.command.handler;

import static java.util.UUID.nameUUIDFromBytes;

import uk.gov.justice.services.common.configuration.Value;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.domains.results.MigratedMasterDefendantCaseDetails;
import uk.gov.moj.cpp.results.domain.aggregate.MigratedInactiveHearingFinancialResultsAggregate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonString;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.COMMAND_HANDLER)
public class MigratedStagingEnforcementResponseHandler extends AbstractCommandHandler {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(MigratedStagingEnforcementResponseHandler.class.getName());
    public static final String MASTER_DEFENDANT_ID = "masterDefendantId";
    public static final String APPLICATION_TYPE = "applicationType";
    public static final String HEARING_COURT_CENTRE_NAME = "hearingCourtCentreName";
    public static final String LISTING_DATE = "listingDate";
    public static final String CASE_URNS = "caseUrns";
    public static final String IN_FORMAT = "dd/MM/yyyy";
    public static final String EMPTY_STRING = "";
    public static final String MIGRATED_MASTER_DEFENDANT_COURT_EMAIL_AND_FINE_ACCOUNT = "migratedMasterDefendantCourtEmailAndFineAccount";
    public static final String CASE_ID = "caseId";
    public static final String FINE_ACCOUNT_NUMBER = "fineAccountNumber";
    public static final String COURT_EMAIL = "courtEmail";
    public static final String DIVISION = "division";
    public static final String DEFENDANT_ID = "defendantId";
    public static final String DEFENDANT_NAME = "defendantName";
    public static final String DEFENDANT_ADDRESS = "defendantAddress";
    public static final String ORIGINAL_DATE_OF_CONVICTION = "originalDateOfConviction";
    public static final String DEFENDANT_EMAIL = "defendantEmail";
    public static final String DEFENDANT_DATE_OF_BIRTH = "defendantDateOfBirth";
    public static final String DEFENDANT_CONTACT_NUMBER = "defendantContactNumber";
    public static final String MIGRATION_SOURCE_SYSTEM_CASE_IDENTIFIER = "migrationSourceSystemCaseIdentifier";
    public static final String CASE_URN = "caseURN";
    private static final String MATERIAL_URL = "materialUrl";

    @Inject
    @Value(key = "ncesEmailNotificationTemplateId")
    private String ncesEmailNotificationTemplateId;

    @Inject
    public MigratedStagingEnforcementResponseHandler(final EventSource eventSource, final Enveloper enveloper, final AggregateService aggregateService) {
        super(eventSource, enveloper, aggregateService);
    }

    @Handles("result.command.send-migrated-inactive-nces-email-for-application")
    public void sendNcesEmailForMigratedApplication(final JsonEnvelope envelope) throws EventStreamException {
        final String masterDefendantId = envelope.payloadAsJsonObject().getString(MASTER_DEFENDANT_ID);
        LOGGER.info("masterDefendantId : {} - sendNcesEmailForMigratedApplication: {}", masterDefendantId, envelope.toObfuscatedDebugString());
        final String applicationType = envelope.payloadAsJsonObject().getString(APPLICATION_TYPE);
        final String listingDate = LocalDate.parse(envelope.payloadAsJsonObject().getString(LISTING_DATE), DateTimeFormatter.ofPattern(IN_FORMAT)).toString();
        final List<String> caseUrns = envelope.payloadAsJsonObject().getJsonArray(CASE_URNS).stream().map(i -> ((JsonString) i).getString()).collect(Collectors.toList());
        final String hearingCourtCentreName = envelope.payloadAsJsonObject().containsKey(HEARING_COURT_CENTRE_NAME)
                ? envelope.payloadAsJsonObject().getString(HEARING_COURT_CENTRE_NAME)
                : EMPTY_STRING;
        
        final JsonObject fineAccountInfo = envelope.payloadAsJsonObject().getJsonObject(MIGRATED_MASTER_DEFENDANT_COURT_EMAIL_AND_FINE_ACCOUNT);
        final MigratedMasterDefendantCaseDetails migratedCaseDetails = MigratedMasterDefendantCaseDetails.builder()
                .withMasterDefendantId(fineAccountInfo.getString(MASTER_DEFENDANT_ID))
                .withCaseId(fineAccountInfo.getString(CASE_ID))
                .withFineAccountNumber(fineAccountInfo.getString(FINE_ACCOUNT_NUMBER))
                .withCourtEmail(fineAccountInfo.getString(COURT_EMAIL))
                .withDivision(fineAccountInfo.getString(DIVISION))
                .withDefendantId(fineAccountInfo.containsKey(DEFENDANT_ID) ? fineAccountInfo.getString(DEFENDANT_ID) : EMPTY_STRING)
                .withDefendantName(fineAccountInfo.containsKey(DEFENDANT_NAME) ? fineAccountInfo.getString(DEFENDANT_NAME) : EMPTY_STRING)
                .withDefendantAddress(fineAccountInfo.containsKey(DEFENDANT_ADDRESS) ? fineAccountInfo.getString(DEFENDANT_ADDRESS) : EMPTY_STRING)
                .withOriginalDateOfConviction(fineAccountInfo.containsKey(ORIGINAL_DATE_OF_CONVICTION) ? fineAccountInfo.getString(ORIGINAL_DATE_OF_CONVICTION) : EMPTY_STRING)
                .withDefendantEmail(fineAccountInfo.containsKey(DEFENDANT_EMAIL) ? fineAccountInfo.getString(DEFENDANT_EMAIL) : null)
                .withDefendantDateOfBirth(fineAccountInfo.containsKey(DEFENDANT_DATE_OF_BIRTH) ? fineAccountInfo.getString(DEFENDANT_DATE_OF_BIRTH) : null)
                .withDefendantContactNumber(fineAccountInfo.containsKey(DEFENDANT_CONTACT_NUMBER) ? fineAccountInfo.getString(DEFENDANT_CONTACT_NUMBER) : null)
                .withMigrationSourceSystemCaseIdentifier(fineAccountInfo.containsKey(MIGRATION_SOURCE_SYSTEM_CASE_IDENTIFIER) ? fineAccountInfo.getString(MIGRATION_SOURCE_SYSTEM_CASE_IDENTIFIER) : EMPTY_STRING)
                .withCaseURN(fineAccountInfo.containsKey(CASE_URN) ? fineAccountInfo.getString(CASE_URN) : EMPTY_STRING)
                .build();


        final String rootAggregateId = masterDefendantId + "-" + migratedCaseDetails.caseId();
        final UUID rootAggregateUUID = nameUUIDFromBytes(rootAggregateId.getBytes(StandardCharsets.UTF_8));
        aggregate(MigratedInactiveHearingFinancialResultsAggregate.class, rootAggregateUUID,
                envelope,
                migratedInactiveHearingFinancialResultsAggregate ->
                        migratedInactiveHearingFinancialResultsAggregate.sendNcesEmailForMigratedApplication(applicationType, listingDate, caseUrns, hearingCourtCentreName, migratedCaseDetails));

        LOGGER.info("MigratedInactiveHearingFinancialResultsAggregate updated for masterDefendantId : {}", masterDefendantId);
    }

    @Handles("result.command.migrated-inactive-nces-document-notification")
    public void processMigratedInactiveNcesEmailNotification(final JsonEnvelope envelope) throws EventStreamException {
        LOGGER.info("Received MigratedInactiveNcesEmailNotification {}", envelope.toObfuscatedDebugString());

        final JsonObject payload = envelope.payloadAsJsonObject();
        final String materialUrl = payload.getString(MATERIAL_URL);

        final String masterDefendantId = payload.getString(MASTER_DEFENDANT_ID);
        final String caseId = payload.getString(CASE_ID);


        final String rootAggregateId = masterDefendantId + "-" + caseId;
        final UUID rootAggregateUUID = nameUUIDFromBytes(rootAggregateId.getBytes(StandardCharsets.UTF_8));

        aggregate(MigratedInactiveHearingFinancialResultsAggregate.class, rootAggregateUUID,
                envelope,
                migratedInactiveHearingFinancialResultsAggregate ->
                        migratedInactiveHearingFinancialResultsAggregate.saveMigratedInactiveNcesEmailNotificationDetails(materialUrl, ncesEmailNotificationTemplateId));

    }
}
