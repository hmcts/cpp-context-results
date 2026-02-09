package uk.gov.moj.cpp.results.domain.aggregate;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.APPLICATION_TYPES;
import static uk.gov.moj.cpp.results.domain.event.MigratedInactiveNcesEmailNotificationRequested.migratedInactiveNcesEmailNotificationRequested;

import uk.gov.moj.cpp.domains.results.MigratedMasterDefendantCaseDetails;
import uk.gov.moj.cpp.results.domain.event.MigratedInactiveNcesEmailNotificationRequested;
import uk.gov.moj.cpp.results.domain.event.MigratedInactiveNcesEmailNotificationRequestedExists;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class MigratedInactiveHearingFinancialResultsAggregateTest {

    private static final String STAT_DEC = "STAT_DEC";
    private static final String LISTING_DATE = "01/01/2020";
    private static final String HEARING_COURT_CENTRE_NAME = "Croydon Crown Court";
    private static final String CASE_ID = randomUUID().toString();
    private static final String MASTER_DEFENDANT_ID = randomUUID().toString();
    private static final String FINE_ACCOUNT_NUMBER = "FINE123";
    private static final String COURT_EMAIL = "court@example.com";
    private static final String DIVISION = "6";
    private static final String DEFENDANT_ID = "defendant-id-1";
    private static final String DEFENDANT_NAME = "defendantName";
    private static final String DEFENDANT_ADDRESS = "defendantAddress";
    private static final String ORIGINAL_DATE_OF_CONVICTION = "OriginalDateOfConviction";
    private static final String DEFENDANT_EMAIL = "defendant@email.com";
    private static final String DEFENDANT_DATE_OF_BIRTH = "21/10/1978";
    private static final String DEFENDANT_CONTACT_NUMBER = "089776687";
    public static final String EVENT_EARLIER_OR_MIGRATED_CASE_DETAILS_IS_NULL = "Event earlier or migratedCaseDetails is null";

    @InjectMocks
    private MigratedInactiveHearingFinancialResultsAggregate aggregate;

    @BeforeEach
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldReturnEmptyStreamWhenMigratedCaseDetailsIsNull() {
        final Stream<Object> result = aggregate.sendNcesEmailForMigratedApplication(
                STAT_DEC,
                LISTING_DATE,
                singletonList("caseUrn"),
                HEARING_COURT_CENTRE_NAME,
                null);

        final List<Object> events = result.toList();
        assertThat(events.size(), is(1));
        assertThat(events.get(0).getClass(), is(MigratedInactiveNcesEmailNotificationRequestedExists.class));
        assertThat(((MigratedInactiveNcesEmailNotificationRequestedExists) events.get(0)).getDescription(),
                is(EVENT_EARLIER_OR_MIGRATED_CASE_DETAILS_IS_NULL));
    }

    @Test
    public void shouldSendNcesEmailForMigratedApplication() {
        final MigratedMasterDefendantCaseDetails migratedCaseDetails = MigratedMasterDefendantCaseDetails.builder()
                .withMasterDefendantId(MASTER_DEFENDANT_ID)
                .withCaseId(CASE_ID)
                .withFineAccountNumber(FINE_ACCOUNT_NUMBER)
                .withCourtEmail(COURT_EMAIL)
                .withDivision(DIVISION)
                .withDefendantId(DEFENDANT_ID)
                .withDefendantName(DEFENDANT_NAME)
                .withDefendantAddress(DEFENDANT_ADDRESS)
                .withOriginalDateOfConviction(ORIGINAL_DATE_OF_CONVICTION)
                .withDefendantEmail(DEFENDANT_EMAIL)
                .withDefendantDateOfBirth(DEFENDANT_DATE_OF_BIRTH)
                .withDefendantContactNumber(DEFENDANT_CONTACT_NUMBER)
                .withMigrationSourceSystemCaseIdentifier("CASE123")
                .withCaseURN("caseUrn1")
                .build();

        final Stream<Object> result = aggregate.sendNcesEmailForMigratedApplication(
                STAT_DEC,
                LISTING_DATE,
                singletonList("caseUrn"),
                HEARING_COURT_CENTRE_NAME,
                migratedCaseDetails);

        final List<Object> events = result.collect(toList());
        assertThat(events.size(), is(1));
        assertThat(events.get(0), is(notNullValue()));
        assertThat(events.get(0).getClass(), is(MigratedInactiveNcesEmailNotificationRequested.class));

        final MigratedInactiveNcesEmailNotificationRequested event = (MigratedInactiveNcesEmailNotificationRequested) events.get(0);
        assertThat(event.getMasterDefendantId(), is(notNullValue()));
        assertThat(event.getNotificationId(), is(notNullValue()));
        assertThat(event.getMaterialId(), is(notNullValue()));
        assertThat(event.getSendTo(), is(COURT_EMAIL));
        assertThat(event.getSubject(), is(APPLICATION_TYPES.get(STAT_DEC)));
        assertThat(event.getHearingCourtCentreName(), is(HEARING_COURT_CENTRE_NAME));
        assertThat(event.getCaseReferences(), is("caseUrn1"));
        assertThat(event.getListedDate(), is(LISTING_DATE));
        assertThat(event.getIsWriteOff(), is(Boolean.FALSE));
    }

    @Test
    public void shouldReturnEmptyStreamWhenEventRaisedEarlier() {
        final UUID masterDefendantId = randomUUID();
        final UUID notificationId = randomUUID();
        final UUID materialId = randomUUID();
        final String sendToAddress = "test@example.com";
        final String subject = "TEST SUBJECT";

        final MigratedInactiveNcesEmailNotificationRequested event = migratedInactiveNcesEmailNotificationRequested()
                .withMasterDefendantId(masterDefendantId)
                .withNotificationId(notificationId)
                .withMaterialId(materialId)
                .withSendTo(sendToAddress)
                .withSubject(subject)
                .build();

        aggregate.apply(event);
        assertThat(aggregate.isEventRaisedEarlier(), is(true));

        final MigratedMasterDefendantCaseDetails migratedCaseDetails = MigratedMasterDefendantCaseDetails.builder()
                .withMasterDefendantId(MASTER_DEFENDANT_ID)
                .withCaseId(CASE_ID)
                .withFineAccountNumber(FINE_ACCOUNT_NUMBER)
                .withCourtEmail(COURT_EMAIL)
                .withDivision(DIVISION)
                .withDefendantId(DEFENDANT_ID)
                .withDefendantName(DEFENDANT_NAME)
                .withDefendantAddress(DEFENDANT_ADDRESS)
                .withOriginalDateOfConviction(ORIGINAL_DATE_OF_CONVICTION)
                .withDefendantEmail(DEFENDANT_EMAIL)
                .withDefendantDateOfBirth(DEFENDANT_DATE_OF_BIRTH)
                .withDefendantContactNumber(DEFENDANT_CONTACT_NUMBER)
                .withMigrationSourceSystemCaseIdentifier("CASE123")
                .withCaseURN("caseUrn1")
                .build();

        final Stream<Object> result = aggregate.sendNcesEmailForMigratedApplication(
                STAT_DEC,
                LISTING_DATE,
                singletonList("caseUrn"),
                HEARING_COURT_CENTRE_NAME,
                migratedCaseDetails);

        final List<Object> events = result.toList();
        assertThat(events.size(), is(1));
        assertThat(events.get(0).getClass(), is(MigratedInactiveNcesEmailNotificationRequestedExists.class));
        assertThat(((MigratedInactiveNcesEmailNotificationRequestedExists) events.get(0)).getDescription(),
                is(EVENT_EARLIER_OR_MIGRATED_CASE_DETAILS_IS_NULL));
    }

    @Test
    public void shouldSaveMigratedInactiveNcesEmailNotificationDetails() {
        final UUID masterDefendantId = randomUUID();
        final UUID notificationId = randomUUID();
        final UUID materialId = randomUUID();
        final String sendToAddress = "test@example.com";
        final String subject = "TEST SUBJECT";

        final MigratedInactiveNcesEmailNotificationRequested event = migratedInactiveNcesEmailNotificationRequested()
                .withMasterDefendantId(masterDefendantId)
                .withNotificationId(notificationId)
                .withMaterialId(materialId)
                .withSendTo(sendToAddress)
                .withSubject(subject)
                .build();

        aggregate.apply(event);

        final String materialUrl = "https://example.com/material";
        final String templateId = randomUUID().toString();

        final Stream<Object> result = aggregate.saveMigratedInactiveNcesEmailNotificationDetails(materialUrl, templateId);

        final List<Object> events = result.collect(toList());
        assertThat(events.size(), is(1));
    }
}
