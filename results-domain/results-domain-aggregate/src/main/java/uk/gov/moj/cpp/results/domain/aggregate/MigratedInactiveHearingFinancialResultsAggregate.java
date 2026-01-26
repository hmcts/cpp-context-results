package uk.gov.moj.cpp.results.domain.aggregate;

import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.stream.Stream.empty;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.APPLICATION_TYPES;
import static uk.gov.moj.cpp.results.domain.event.MigratedInactiveNcesEmailNotificationRequested.migratedInactiveNcesEmailNotificationRequested;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.moj.cpp.domains.results.MigratedMasterDefendantCaseDetails;
import uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants;
import uk.gov.moj.cpp.results.domain.event.MigratedInactiveNcesEmailNotification;
import uk.gov.moj.cpp.results.domain.event.MigratedInactiveNcesEmailNotificationRequested;
import uk.gov.moj.cpp.results.domain.event.NcesEmailNotification;

import java.io.Serial;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@SuppressWarnings({"PMD.BeanMembersShouldSerialize"})
public class MigratedInactiveHearingFinancialResultsAggregate implements Aggregate {

    private UUID masterDefendantId;
    private UUID notificationId;
    private UUID materialId;
    private String sendToAddress;
    private String subject;

    @Serial
    private static final long serialVersionUID = 1L;


    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(MigratedInactiveNcesEmailNotificationRequested.class).apply(this::handleMigratedInactiveNcesEmailNotificationRequested),
                otherwiseDoNothing());
    }

    private void handleMigratedInactiveNcesEmailNotificationRequested(MigratedInactiveNcesEmailNotificationRequested migratedInactiveNcesEmailNotificationRequested) {


        this.masterDefendantId = migratedInactiveNcesEmailNotificationRequested.getMasterDefendantId();
        this.notificationId = migratedInactiveNcesEmailNotificationRequested.getNotificationId();
        this.materialId = migratedInactiveNcesEmailNotificationRequested.getMaterialId();
        this.sendToAddress = migratedInactiveNcesEmailNotificationRequested.getSendTo();
        this.subject = migratedInactiveNcesEmailNotificationRequested.getSubject();

    }

    public Stream<Object> sendNcesEmailForMigratedApplication(
            final String applicationType,
            final String listingDate,
            final List<String> caseUrns,
            final String hearingCourtCentreName,
            final MigratedMasterDefendantCaseDetails migratedCaseDetails) {

        if (migratedCaseDetails == null) {
            return Stream.empty();
        }

        final String subject = APPLICATION_TYPES.get(applicationType);
        final MigratedInactiveNcesEmailNotificationRequested requested = migratedInactiveNcesEmailNotificationRequested()
                .withNotificationId(randomUUID())
                .withMaterialId(randomUUID())
                .withSendTo(migratedCaseDetails.courtEmail())
                .withSubject(subject)
                .withHearingCourtCentreName(hearingCourtCentreName)
                .withCaseReferences(String.join(NCESDecisionConstants.COMMA, caseUrns))
                .withMasterDefendantId(fromString(migratedCaseDetails.masterDefendantId()))
                .withListedDate(listingDate)
                .withIsWriteOff(Boolean.FALSE)
                .withFinAccountNumber(migratedCaseDetails.courtEmail())
                .withCaseId(fromString(migratedCaseDetails.caseId()))
                .withDivisionCode(migratedCaseDetails.division())
                .withDefendantName(migratedCaseDetails.defendantName())
                .withDefendantAddress(migratedCaseDetails.defendantAddress())
                .withApplicationHearingCourtEmail(migratedCaseDetails.courtEmail())
                .withOriginalDateOfConviction(migratedCaseDetails.originalDateOfConviction())
                .withDefendantEmail(migratedCaseDetails.defendantEmail())
                .withDefendantDateOfBirth(migratedCaseDetails.defendantDateOfBirth())
                .withDefendantContactNumber(migratedCaseDetails.defendantContactNumber())
                .withLegacyCaseReference(migratedCaseDetails.migrationSourceSystemCaseIdentifier())
                .build();

         return apply(Stream.of(requested));
    }

    public Stream<Object> saveMigratedInactiveNcesEmailNotificationDetails(final String materialUrl, final  String ncesEmailNotificationTemplateId) {
        final MigratedInactiveNcesEmailNotification migratedInactiveNcesEmailNotification = MigratedInactiveNcesEmailNotification.migratedInactiveNcesEmailNotification()
                .withMasterDefendantId(this.masterDefendantId)
                .withNotificationId(this.notificationId)
                .withMaterialId(this.materialId)
                .withTemplateId(fromString(ncesEmailNotificationTemplateId))
                .withSendToAddress(this.sendToAddress)
                .withSubject(this.subject)
                .withMaterialUrl(materialUrl)
                .build();

        return apply(Stream.of(migratedInactiveNcesEmailNotification));

    }

    UUID getMasterDefendantId() {
        return masterDefendantId;
    }

    UUID getNotificationId() {
        return notificationId;
    }

    UUID getMaterialId() {
        return materialId;
    }

    String getSendToAddress() {
        return sendToAddress;
    }

    String getSubject() {
        return subject;
    }
}
