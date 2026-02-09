package uk.gov.moj.cpp.results.domain.aggregate;

import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
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
import uk.gov.moj.cpp.results.domain.event.MigratedInactiveNcesEmailNotificationRequestedExists;

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
    private boolean isEventRaisedEarlier = false;

    @Serial
    private static final long serialVersionUID = 1L;
    private UUID caseId;


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
        this.caseId = migratedInactiveNcesEmailNotificationRequested.getCaseId();
        this.isEventRaisedEarlier = true;
    }

    public Stream<Object> sendNcesEmailForMigratedApplication(
            final String applicationType,
            final String listingDate,
            final List<String> caseUrns,
            final String hearingCourtCentreName,
            final MigratedMasterDefendantCaseDetails migratedCaseDetails) {

        if (migratedCaseDetails == null || isEventRaisedEarlier) {
            MigratedInactiveNcesEmailNotificationRequestedExists event = MigratedInactiveNcesEmailNotificationRequestedExists
                    .migratedInactiveNcesEmailNotificationRequestedExists()
                    .withMasterDefendantId(this.masterDefendantId)
                    .withCaseId(this.caseId)
                    .withDescription("Event earlier or migratedCaseDetails is null")
                    .build();
            return apply(Stream.of(event));
        }

        final String subject = APPLICATION_TYPES.get(applicationType);
        final MigratedInactiveNcesEmailNotificationRequested requested = migratedInactiveNcesEmailNotificationRequested()
                .withNotificationId(randomUUID())
                .withMaterialId(randomUUID())
                .withSendTo(migratedCaseDetails.courtEmail())
                .withSubject(subject)
                .withHearingCourtCentreName(hearingCourtCentreName)
                .withCaseReferences(String.join(NCESDecisionConstants.COMMA, migratedCaseDetails.caseURN()))
                .withMasterDefendantId(fromString(migratedCaseDetails.masterDefendantId()))
                .withListedDate(listingDate)
                .withIsWriteOff(Boolean.FALSE)
                .withFineAccountNumber(migratedCaseDetails.fineAccountNumber())
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


    boolean isEventRaisedEarlier() {
        return isEventRaisedEarlier;
    }
}
