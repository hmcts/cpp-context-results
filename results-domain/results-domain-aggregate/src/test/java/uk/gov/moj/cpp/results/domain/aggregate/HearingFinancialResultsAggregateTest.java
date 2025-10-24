package uk.gov.moj.cpp.results.domain.aggregate;

import static java.nio.charset.Charset.defaultCharset;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static javax.json.Json.createReader;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.gov.justice.hearing.courts.OffenceResults.offenceResults;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.APPLICATION_UPDATED_SUBJECT;

import uk.gov.justice.core.courts.UnmarkedAggregateSendEmailWhenAccountReceived;
import uk.gov.justice.hearing.courts.HearingFinancialResultRequest;
import uk.gov.justice.hearing.courts.HearingFinancialResultsTracked;
import uk.gov.justice.hearing.courts.OffenceResults;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.moj.cpp.results.domain.event.MarkedAggregateSendEmailWhenAccountReceived;
import uk.gov.moj.cpp.results.domain.event.NcesEmailNotificationRequested;
import uk.gov.moj.cpp.results.domain.event.NewOffenceByResult;
import uk.gov.moj.cpp.results.domain.event.SendNcesEmailNotFound;

import java.io.InputStream;
import java.io.StringReader;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.json.JsonObject;
import javax.json.JsonReader;

import org.apache.commons.io.IOUtils;
import org.hamcrest.core.IsNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

@SuppressWarnings({"squid:S2925"})
public class HearingFinancialResultsAggregateTest {
    public static final String STAT_DEC = "STAT_DEC";
    public static final String REOPEN = "REOPEN";
    public static final String APPEAL = "APPEAL";
    public static final String RFSD = "RFSD";
    public static final String WDRN = "WDRN";
    public static final String APPEAL_DISMISSED = "APPEAL DISMISSED";
    public static final String APPEAL_DISMISSED_SENTENCE_VARIED = "APPEAL DISMISSED SENTENCE VARIED";
    public static final String STATUTORY_DECLARATION_WITHDRAWN = "STATUTORY DECLARATION WITHDRAWN";
    public static final String WRITE_OFF_ONE_DAY_DEEMED_SERVED = "WRITE OFF ONE DAY DEEMED SERVED";
    public static final String AMEND_AND_RESHARE = "AMEND AND RESHARE- DUPLICATE ACCOUNT: WRITE OFF REQUIRED";
    public static final String APPEAL_ALLOWED = "APPEAL ALLOWED";
    public static final String STATUTORY_DECLARATION_GRANTED = "STATUTORY DECLARATION GRANTED";
    public static final String APPLICATION_TO_REOPEN_GRANTED = "APPLICATION TO REOPEN GRANTED";
    public static final String G = "G";
    public static final String ASV = "ASV";
    public static final String ACON = "ACON";
    public static final String FIDI = "FIDI";
    public static final String FIDICI = "FIDICI";
    public static final String BRITISH_DATE_FORMAT = "dd/MM/yyyy";
    private static final String ACON_EMAIL_SUBJECT = "ACCOUNTS TO BE CONSOLIDATED";
    private static final String HEARING_SITTING_DAY_PATTERN = "yyyy-MM-dd";
    private static final String STATUTORY_DECLARATION_UPDATED = "STATUTORY DECLARATION UPDATED";
    private static final String APPLICATION_TO_REOPEN_UPDATED = "APPLICATION TO REOPEN UPDATED";
    private static final String APPEAL_APPLICATION_UPDATED = "APPEAL APPLICATION UPDATED";
    private static final String APPLICATION_RESULT = "NEXH --";
    private final UUID MASTER_DEFENDANT_ID = randomUUID();
    private final UUID OFFENCE_ID_1 = randomUUID();
    private final UUID OFFENCE_ID_2 = randomUUID();
    private final UUID CORRELATION_ID_1 = randomUUID();
    private final UUID CORRELATION_ID_2 = randomUUID();
    private final UUID HEARING_ID = randomUUID();
    private final List<String> applicationSubjects = asList("STATUTORY DECLARATION REFUSED", STATUTORY_DECLARATION_WITHDRAWN, STATUTORY_DECLARATION_GRANTED,
            "APPLICATION TO REOPEN REFUSED", "APPLICATION TO REOPEN WITHDRAWN", APPLICATION_TO_REOPEN_GRANTED, APPEAL_DISMISSED, "APPEAL DISMISSED SENTENCE VARIED",
            "APPEAL ABANDONED", "APPEAL WITHDRAWN", APPEAL_ALLOWED, STATUTORY_DECLARATION_UPDATED, APPLICATION_TO_REOPEN_UPDATED, APPEAL_APPLICATION_UPDATED);
    private final List<String> applicationGrantedSubjects = Arrays.asList("STATUTORY DECLARATION GRANTED",
            "APPLICATION TO REOPEN GRANTED",
            "APPLICATION TO REOPEN GRANTED",
            "APPEAL GRANTED");
    private String ncesEMail;
    private ZonedDateTime hearingSittingDay = ZonedDateTimes.fromString("2020-03-07T14:22:00.000Z");
    private String hearingCourtCentreName = "Croydon Crown Court";
    private String defendantDateOfBirth = "1988-07-07";
    private String defendantAddress = "address";
    private String defendantEmail = "aa@aa.com";
    private String defendantContactNumber = "02074561234";

    private UUID hearingId = randomUUID();
    private final HearingFinancialResultRequest input = HearingFinancialResultRequest.hearingFinancialResultRequest()
            .withAccountCorrelationId(CORRELATION_ID_1)
            .withHearingId(HEARING_ID)
            .withAccountDivisionCode("adc")
            .withMasterDefendantId(MASTER_DEFENDANT_ID)
            .withHearingSittingDay(hearingSittingDay)
            .withHearingCourtCentreName(hearingCourtCentreName)
            .withDefendantName("name")
            .withDefendantDateOfBirth(defendantDateOfBirth)
            .withDefendantAddress(defendantAddress)
            .withDefendantEmail(defendantEmail)
            .withDefendantContactNumber(defendantContactNumber)
            .withIsSJPHearing(false)
            .withNcesEmail("email")
            .withAccountNumber("accNo")
            .withOffenceResults(asList(offenceResults()
                            .withOffenceId(OFFENCE_ID_1)
                            .withResultCode("rc1")
                            .withDateOfResult("24/05/2024")
                            .withIsFinancial(true)
                            .withIsDeemedServed(false)
                            .build(),
                    offenceResults()
                            .withOffenceId(OFFENCE_ID_2)
                            .withResultCode("rc2")
                            .withDateOfResult("24/05/2024")
                            .withIsFinancial(true)
                            .withIsDeemedServed(false)
                            .build()))
            .build();

    @InjectMocks
    private HearingFinancialResultsAggregate aggregate;

    public static Object[][] applicationTypes() {
        return new String[][]{
                {STAT_DEC, "APPLICATION FOR A STATUTORY DECLARATION RECEIVED"},
                {REOPEN, "APPLICATION TO REOPEN RECEIVED"},
                {APPEAL, "APPEAL APPLICATION RECEIVED"}
        };
    }

    public static Object[][] subjects() {
        return new String[][]{
                {STAT_DEC, RFSD, "STATUTORY DECLARATION REFUSED"},
                {STAT_DEC, WDRN, STATUTORY_DECLARATION_WITHDRAWN},
                {STAT_DEC, G, STATUTORY_DECLARATION_GRANTED},
                {STAT_DEC, "STDEC", STATUTORY_DECLARATION_GRANTED},
                {REOPEN, RFSD, "APPLICATION TO REOPEN REFUSED"},
                {REOPEN, WDRN, "APPLICATION TO REOPEN WITHDRAWN"},
                {REOPEN, G, APPLICATION_TO_REOPEN_GRANTED},
                {REOPEN, "ROPENED", APPLICATION_TO_REOPEN_GRANTED},
                {APPEAL, "AACD", APPEAL_DISMISSED},
                {APPEAL, "AASD", APPEAL_DISMISSED},
                {APPEAL, "ACSD", APPEAL_DISMISSED},
                {APPEAL, "ASV", "APPEAL DISMISSED SENTENCE VARIED"},
                {APPEAL, "APA", "APPEAL ABANDONED"},
                {APPEAL, WDRN, "APPEAL WITHDRAWN"},
                {APPEAL, "AACA", APPEAL_ALLOWED},
                {APPEAL, "AASA", APPEAL_ALLOWED}
        };
    }

    public static Object[][] applicationUpdateSubjects() {
        return new String[][]{
                {STAT_DEC, "ABC", STATUTORY_DECLARATION_UPDATED},
                {REOPEN, "ROPENED1", APPLICATION_TO_REOPEN_UPDATED},
                {APPEAL, "APPEAL", APPEAL_APPLICATION_UPDATED}
        };
    }

    private static JsonObject stringToObject(final String request) {
        final JsonReader reader = createReader(new StringReader(request));
        return reader.readObject();
    }

    public static String getPayloadAsString(final String path) {
        String request = null;
        try {
            final InputStream inputStream = HearingFinancialResultsAggregateTest.class.getClassLoader().getResourceAsStream(path);
            assertThat(inputStream, IsNull.notNullValue());
            request = IOUtils.toString(inputStream, defaultCharset());
        } catch (final Exception e) {
            fail("Error consuming file from location " + path);
        }
        return request;
    }

    public static JsonObject convertStringToJson(String str) {
        final JsonReader reader = createReader(new StringReader(str));
        return reader.readObject();
    }

    @BeforeEach
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
        ncesEMail = "John.Doe@xxx.com";
    }

    @Test
    public void shouldTrackFinancialResults() {
        aggregate.updateFinancialResults(input, "false", "2021-21-21", "2021-21-21", null, APPLICATION_RESULT);
        assertThat(aggregate.getDefendantName(), is("name"));
        assertThat(aggregate.getCaseOffenceResultsDetails().size(), is(2));
        assertThat(aggregate.getCaseOffenceResultsDetails().get(OFFENCE_ID_1), is(notNullValue()));
        assertThat(aggregate.getCaseOffenceResultsDetails().get(OFFENCE_ID_2), is(notNullValue()));
        assertAdditionalData(aggregate);
    }

    private List<OffenceResults> getOffenceResults(final UUID offenceId) {
        List<OffenceResults> offenceResults = new ArrayList<>();
        offenceResults.add(OffenceResults.offenceResults().withIsFinancial(true)
                .withDateOfResult("24/05/2024")
                .withOffenceId(offenceId)
                .withOffenceTitle("Title")
                .withImpositionOffenceDetails("impositionOffenceDetails")
                .build());
        return offenceResults;
    }

    public void assertAdditionalData(HearingFinancialResultsAggregate aggregate) {
        assertThat(aggregate.getHearingSittingDay(), is(hearingSittingDay));
        assertThat(aggregate.getHearingCourtCentreName(), is(hearingCourtCentreName));
        assertThat(aggregate.getDefendantDateOfBirth(), is(defendantDateOfBirth));
        assertThat(aggregate.getDefendantAddress(), is(defendantAddress));
        assertThat(aggregate.getDefendantEmail(), is(defendantEmail));
        assertThat(aggregate.getDefendantContactNumber(), is(defendantContactNumber));
    }

    private void assertOffenceResultsDetails(HearingFinancialResultsAggregate aggregate, UUID resultId, UUID offenceId3) {
        assertThat(aggregate.getCaseOffenceResultsDetails().size(), is(3));
        assertThat(aggregate.getCaseOffenceResultsDetails().get(OFFENCE_ID_1), is(notNullValue()));
        assertThat(aggregate.getCaseOffenceResultsDetails().get(OFFENCE_ID_1).getResultCode(), is("rc1"));
        assertThat(aggregate.getCaseOffenceResultsDetails().get(OFFENCE_ID_1).getAmendmentReason(), is("AmendmentReason"));
        assertThat(aggregate.getCaseOffenceResultsDetails().get(OFFENCE_ID_1).getAmendmentDate(), is("AmendmentDate"));
        assertThat(aggregate.getCaseOffenceResultsDetails().get(OFFENCE_ID_1).getDateOfResult(), is("24/05/2024"));
        assertThat(aggregate.getCaseOffenceResultsDetails().get(OFFENCE_ID_1).getImpositionOffenceDetails(), is("impositionDetails"));
        assertThat(aggregate.getCaseOffenceResultsDetails().get(OFFENCE_ID_1).getOffenceTitle(), is("offenceTitle"));
        assertThat(aggregate.getCaseOffenceResultsDetails().get(OFFENCE_ID_1).getIsDeemedServed(), is(true));
        assertThat(aggregate.getCaseOffenceResultsDetails().get(OFFENCE_ID_1).getResultId(), is(resultId));


        assertThat(aggregate.getCaseOffenceResultsDetails().get(OFFENCE_ID_2).getOffenceId(), is(notNullValue()));
        assertThat(aggregate.getCaseOffenceResultsDetails().get(OFFENCE_ID_2).getDateOfResult(), is("24/05/2024"));
        assertThat(aggregate.getCaseOffenceResultsDetails().get(offenceId3).getOffenceId(), is(notNullValue()));
        assertThat(aggregate.getCaseOffenceResultsDetails().get(offenceId3).getDateOfResult(), is("24/05/2024"));
    }

    @Test
    public void testNcesEmailNotAvailable() {
        final String caseReference = "caseReference";
        final String oldDivisionCode = "oldDivisionCode";
        final UUID masterDefendantId = randomUUID();
        final MarkedAggregateSendEmailWhenAccountReceived accountReceived = MarkedAggregateSendEmailWhenAccountReceived.markedAggregateSendEmailWhenAccountReceived()
                .withCaseReferences(caseReference)
                .withHearingSittingDay(hearingSittingDay.format(ofPattern(HEARING_SITTING_DAY_PATTERN)))
                .withHearingCourtCentreName(hearingCourtCentreName)
                .withDefendantName("name")
                .withDefendantDateOfBirth(defendantDateOfBirth)
                .withDefendantAddress(defendantAddress)
                .withDefendantEmail(defendantEmail)
                .withDefendantContactNumber(defendantContactNumber)
                .withIsSJPHearing(false)
                .withOldDivisionCode(oldDivisionCode)
                .withMasterDefendantId(masterDefendantId)
                .build();
        final List<Object> eventStream = aggregate.ncesEmailNotFound(accountReceived).collect(toList());
        assertThat(eventStream.size(), is(1));
        final SendNcesEmailNotFound requestToSendNcesEmailRejected = (SendNcesEmailNotFound) eventStream.get(0);
        assertNotNull(requestToSendNcesEmailRejected);
        assertThat(requestToSendNcesEmailRejected.getMasterDefendantId(), is(masterDefendantId));
        assertThat(requestToSendNcesEmailRejected.getCaseReference(), is(caseReference));
        assertThat(requestToSendNcesEmailRejected.getOldDivisionCode(), is(oldDivisionCode));
        assertNull(requestToSendNcesEmailRejected.getSendTo());
        assertNull(requestToSendNcesEmailRejected.getSubject());
        assertThat(requestToSendNcesEmailRejected.getHearingSittingDay(), is(hearingSittingDay.format(ofPattern(HEARING_SITTING_DAY_PATTERN))));
        assertThat(requestToSendNcesEmailRejected.getHearingCourtCentreName(), is(hearingCourtCentreName));
        assertThat(requestToSendNcesEmailRejected.getDefendantDateOfBirth(), is(defendantDateOfBirth));
        assertThat(requestToSendNcesEmailRejected.getDefendantAddress(), is(defendantAddress));
        assertThat(requestToSendNcesEmailRejected.getDefendantEmail(), is(defendantEmail));
        assertThat(requestToSendNcesEmailRejected.getDefendantContactNumber(), is(defendantContactNumber));
    }

    @Test
    @SuppressWarnings("java:S5961")
    public void shouldUpdateTrackedFinancialResults() {
        aggregate.updateFinancialResults(input, "false", "2021-21-21", "2021-21-21", null, APPLICATION_RESULT);
        assertThat(aggregate.getDefendantName(), is("name"));
        assertThat(aggregate.getCaseOffenceResultsDetails().size(), is(2));
        assertThat(aggregate.getCaseOffenceResultsDetails().get(OFFENCE_ID_1), is(notNullValue()));
        assertThat(aggregate.getCaseOffenceResultsDetails().get(OFFENCE_ID_2), is(notNullValue()));

        assertThat(aggregate.getCaseOffenceResultsDetails().get(OFFENCE_ID_1).getDateOfResult(), is("24/05/2024"));
        assertThat(aggregate.getCaseOffenceResultsDetails().get(OFFENCE_ID_2).getDateOfResult(), is("24/05/2024"));

        assertThat(aggregate.getCorrelationItemList().get(0).getAccountDivisionCode(), is("adc"));
        assertThat(aggregate.getCorrelationItemList().get(0).getAccountDivisionCode(), is("adc"));
        assertAdditionalData(aggregate);

        final UUID offenceId3 = randomUUID();
        final UUID resultId = randomUUID();
        HearingFinancialResultRequest newInput = HearingFinancialResultRequest.hearingFinancialResultRequest()
                .withAccountCorrelationId(CORRELATION_ID_2)
                .withHearingId(HEARING_ID)
                .withHearingSittingDay(hearingSittingDay)
                .withHearingCourtCentreName(hearingCourtCentreName)
                .withDefendantName("name")
                .withDefendantDateOfBirth(defendantDateOfBirth)
                .withDefendantAddress(defendantAddress)
                .withDefendantEmail(defendantEmail)
                .withDefendantContactNumber(defendantContactNumber)
                .withIsSJPHearing(false)
                .withAccountDivisionCode("new_division_code")
                .withMasterDefendantId(MASTER_DEFENDANT_ID)
                .withProsecutionCaseReferences(singletonList(""))
                .withOffenceResults(asList(offenceResults()
                                .withOffenceId(OFFENCE_ID_1)
                                .withResultCode("rc1")
                                .withAmendmentReason("AmendmentReason")
                                .withAmendmentDate("AmendmentDate")
                                .withDateOfResult("24/05/2024")
                                .withResultId(resultId)
                                .withImpositionOffenceDetails("impositionDetails")
                                .withOffenceTitle("offenceTitle")
                                .withIsDeemedServed(true)
                                .withIsFinancial(true)
                                .withIsDeemedServed(true)
                                .build(),
                        offenceResults()
                                .withOffenceId(OFFENCE_ID_2)
                                .withResultCode("rc2")
                                .withDateOfResult("24/05/2024")
                                .withIsFinancial(true)
                                .withIsDeemedServed(false)
                                .build(),
                        offenceResults()
                                .withOffenceId(offenceId3)
                                .withResultCode("rc3")
                                .withDateOfResult("24/05/2024")
                                .withIsFinancial(true)
                                .withIsDeemedServed(false)
                                .build()))
                .build();
        aggregate.updateFinancialResults(newInput, "false", "2021-21-21", "2021-21-21", null, APPLICATION_RESULT);
        assertThat(aggregate.getDefendantName(), is("name"));
        assertAdditionalData(aggregate);
        assertOffenceResultsDetails(aggregate, resultId, offenceId3);

        verifyHistory();
    }

    private void verifyHistory() {
        assertThat(aggregate.getCorrelationItemList().size(), is(2));
        assertThat(aggregate.getCorrelationItemList().get(0).getAccountCorrelationId(), is(CORRELATION_ID_1));
        assertThat(aggregate.getCorrelationItemList().get(1).getAccountCorrelationId(), is(CORRELATION_ID_2));
        assertThat(aggregate.getCorrelationItemList().get(0).getAccountDivisionCode(), is("adc"));
        assertThat(aggregate.getCorrelationItemList().get(1).getAccountDivisionCode(), is("new_division_code"));
    }

    @Test
    public void shouldMatchAccountNumberWhenMultipleCorrelationIdReceived() {
        final List<UUID> accountCorrelationIds = asList(randomUUID(), randomUUID(), randomUUID());
        final UUID offenceId = randomUUID();
        final UUID masterDefendantId = randomUUID();

        accountCorrelationIds.forEach(accountCorrelationId -> {
            HearingFinancialResultRequest request1 = HearingFinancialResultRequest.hearingFinancialResultRequest()
                    .withMasterDefendantId(masterDefendantId)
                    .withAccountCorrelationId(accountCorrelationId)
                    .withIsSJPHearing(false)
                    .withOffenceResults(asList(OffenceResults.offenceResults()
                            .withOffenceId(offenceId)
                            .withIsFinancial(true)
                            .withIsDeemedServed(false).build()))
                    .withAccountDivisionCode(accountCorrelationId.toString() + "DIVCODE")
                    .build();
            aggregate.updateFinancialResults(request1, "false", "2021-21-21", "2021-21-21", null, APPLICATION_RESULT);
        });


        Collections.reverse(accountCorrelationIds);
        accountCorrelationIds.forEach(accountCorrelationId ->
                aggregate.updateAccountNumber(accountCorrelationId.toString() + "ACCOUNT",
                        accountCorrelationId)
        );

        assertThat(aggregate.getCorrelationItemList().size(), is(3));
        assertThat(aggregate.getCorrelationItemList().get(0).getAccountCorrelationId(), is(accountCorrelationIds.get(2)));
        assertThat(aggregate.getCorrelationItemList().get(0).getAccountDivisionCode(), is(accountCorrelationIds.get(2) + "DIVCODE"));
        assertThat(aggregate.getCorrelationItemList().get(0).getAccountNumber(), is(accountCorrelationIds.get(2) + "ACCOUNT"));

        assertThat(aggregate.getCorrelationItemList().get(1).getAccountCorrelationId(), is(accountCorrelationIds.get(1)));
        assertThat(aggregate.getCorrelationItemList().get(1).getAccountDivisionCode(), is(accountCorrelationIds.get(1) + "DIVCODE"));
        assertThat(aggregate.getCorrelationItemList().get(1).getAccountNumber(), is(accountCorrelationIds.get(1) + "ACCOUNT"));

        assertThat(aggregate.getCorrelationItemList().get(2).getAccountCorrelationId(), is(accountCorrelationIds.get(0)));
        assertThat(aggregate.getCorrelationItemList().get(2).getAccountDivisionCode(), is(accountCorrelationIds.get(0) + "DIVCODE"));
        assertThat(aggregate.getCorrelationItemList().get(2).getAccountNumber(), is(accountCorrelationIds.get(0) + "ACCOUNT"));

    }

    @Test
    public void shouldMatchAccountNumberWhenMultipleCorrelationIdReceivedWithDifferentCaseReference() {
        final List<UUID> accountCorrelationIds = asList(randomUUID(), randomUUID(), randomUUID());
        final UUID offenceId = randomUUID();
        final UUID masterDefendantId = randomUUID();


        HearingFinancialResultRequest request1 = HearingFinancialResultRequest.hearingFinancialResultRequest()
                .withMasterDefendantId(masterDefendantId)
                .withAccountCorrelationId(accountCorrelationIds.get(0))
                .withIsSJPHearing(false)
                .withProsecutionCaseReferences(asList("RANDOM_CASE_1"))
                .withOffenceResults(asList(OffenceResults.offenceResults()
                        .withOffenceId(offenceId)
                        .withIsFinancial(true)
                        .withIsDeemedServed(false).build()))
                .withAccountDivisionCode(accountCorrelationIds.get(0).toString() + "DIVCODE")
                .build();
        aggregate.updateFinancialResults(request1, "false", "2021-21-21", "2021-21-21", null, APPLICATION_RESULT);

        HearingFinancialResultRequest request2 = HearingFinancialResultRequest.hearingFinancialResultRequest()
                .withMasterDefendantId(masterDefendantId)
                .withAccountCorrelationId(accountCorrelationIds.get(1))
                .withProsecutionCaseReferences(asList("RANDOM_CASE_2"))
                .withIsSJPHearing(false)
                .withOffenceResults(asList(OffenceResults.offenceResults()
                        .withOffenceId(offenceId)
                        .withIsFinancial(true)
                        .withIsDeemedServed(false).build()))
                .withAccountDivisionCode(accountCorrelationIds.get(1).toString() + "DIVCODE")
                .build();
        aggregate.updateFinancialResults(request2, "false", "2021-21-21", "2021-21-21", null, APPLICATION_RESULT);

        HearingFinancialResultRequest request3 = HearingFinancialResultRequest.hearingFinancialResultRequest()
                .withMasterDefendantId(masterDefendantId)
                .withAccountCorrelationId(accountCorrelationIds.get(2))
                .withProsecutionCaseReferences(asList("RANDOM_CASE_1"))
                .withIsSJPHearing(false)
                .withOffenceResults(asList(OffenceResults.offenceResults()
                        .withOffenceId(offenceId)
                        .withIsFinancial(true)
                        .withIsDeemedServed(false).build()))
                .withAccountDivisionCode(accountCorrelationIds.get(2).toString() + "DIVCODE")
                .build();
        aggregate.updateFinancialResults(request3, "false", "2021-21-21", "2021-21-21", null, APPLICATION_RESULT);
        Collections.reverse(accountCorrelationIds);
        accountCorrelationIds.forEach(accountCorrelationId ->
                aggregate.updateAccountNumber(accountCorrelationId.toString() + "ACCOUNT",
                        accountCorrelationId)
        );


        assertThat(aggregate.getCorrelationItemList().size(), is(3));
        assertThat(aggregate.getCorrelationItemList().get(0).getAccountCorrelationId(), is(accountCorrelationIds.get(2)));
        assertThat(aggregate.getCorrelationItemList().get(0).getAccountDivisionCode(), is(accountCorrelationIds.get(2) + "DIVCODE"));
        assertThat(aggregate.getCorrelationItemList().get(0).getAccountNumber(), is(accountCorrelationIds.get(2) + "ACCOUNT"));
        assertThat(aggregate.getCorrelationItemList().get(0).getProsecutionCaseReferences(), is(asList("RANDOM_CASE_1")));

        assertThat(aggregate.getCorrelationItemList().get(1).getAccountCorrelationId(), is(accountCorrelationIds.get(1)));
        assertThat(aggregate.getCorrelationItemList().get(1).getAccountDivisionCode(), is(accountCorrelationIds.get(1) + "DIVCODE"));
        assertThat(aggregate.getCorrelationItemList().get(1).getAccountNumber(), is(accountCorrelationIds.get(1) + "ACCOUNT"));
        assertThat(aggregate.getCorrelationItemList().get(1).getProsecutionCaseReferences(), is(asList("RANDOM_CASE_2")));

        assertThat(aggregate.getCorrelationItemList().get(2).getAccountCorrelationId(), is(accountCorrelationIds.get(0)));
        assertThat(aggregate.getCorrelationItemList().get(2).getAccountDivisionCode(), is(accountCorrelationIds.get(0) + "DIVCODE"));
        assertThat(aggregate.getCorrelationItemList().get(2).getAccountNumber(), is(accountCorrelationIds.get(0) + "ACCOUNT"));
        assertThat(aggregate.getCorrelationItemList().get(2).getProsecutionCaseReferences(), is(asList("RANDOM_CASE_1")));

    }

    @Test
    public void shouldMatchAccountNumberWhenMultipleCorrelationIdReceivedWithNullValue() {
        final List<UUID> accountCorrelationIds = asList(randomUUID(), null, randomUUID());
        final UUID offenceId = randomUUID();

        updateFinancialResult(accountCorrelationIds, singletonList(offenceId));


        Collections.reverse(accountCorrelationIds);
        updateGobAccounts(accountCorrelationIds);

        assertThat(aggregate.getCorrelationItemList().size(), is(2));
        assertThat(aggregate.getCorrelationItemList().get(0).getAccountCorrelationId(), is(accountCorrelationIds.get(2)));
        assertThat(aggregate.getCorrelationItemList().get(0).getAccountDivisionCode(), is(accountCorrelationIds.get(2) + "DIVCODE"));
        assertThat(aggregate.getCorrelationItemList().get(0).getAccountNumber(), is(accountCorrelationIds.get(2) + "ACCOUNT"));

        assertThat(aggregate.getCorrelationItemList().get(1).getAccountCorrelationId(), is(accountCorrelationIds.get(0)));
        assertThat(aggregate.getCorrelationItemList().get(1).getAccountDivisionCode(), is(accountCorrelationIds.get(0) + "DIVCODE"));
        assertThat(aggregate.getCorrelationItemList().get(1).getAccountNumber(), is(accountCorrelationIds.get(0) + "ACCOUNT"));

    }

    @Test
    public void shouldMatchAccountNumberWhenRejectedResultCodePassed() {
        final List<UUID> accountCorrelationIds = asList(randomUUID(), randomUUID(), randomUUID());
        final UUID offenceId = randomUUID();

        updateFinancialResult(accountCorrelationIds, singletonList(offenceId));

        raiseEventsForApplicationResult(singletonList(null), singletonList(offenceId), singletonList(true), singletonList("caseUrn1"), STAT_DEC, singletonList(WDRN));

        assertThat(aggregate.getCorrelationItemList().size(), is(3));
        assertThat(aggregate.getCorrelationItemList().get(0).getAccountCorrelationId(), is(accountCorrelationIds.get(0)));
        assertThat(aggregate.getCorrelationItemList().get(0).getAccountDivisionCode(), is(accountCorrelationIds.get(0) + "DIVCODE"));

        assertThat(aggregate.getCorrelationItemList().get(1).getAccountCorrelationId(), is(accountCorrelationIds.get(1)));
        assertThat(aggregate.getCorrelationItemList().get(1).getAccountDivisionCode(), is(accountCorrelationIds.get(1) + "DIVCODE"));

        assertThat(aggregate.getCorrelationItemList().get(2).getAccountCorrelationId(), is(accountCorrelationIds.get(2)));
        assertThat(aggregate.getCorrelationItemList().get(2).getAccountDivisionCode(), is(accountCorrelationIds.get(2) + "DIVCODE"));
    }

    @ParameterizedTest
    @MethodSource("applicationTypes")
    public void shouldRaiseEmailEventWithCorrectSubject(final String applicationType, final String subject) {
        final List<UUID> accountCorrelationIds = asList(randomUUID(), randomUUID(), randomUUID());
        final UUID offenceId1 = randomUUID();

        updateFinancialResult(accountCorrelationIds, singletonList(offenceId1));

        Collections.reverse(accountCorrelationIds);
        updateGobAccounts(accountCorrelationIds);

        final List<Object> events = aggregate.sendNcesEmailForNewApplication(applicationType, "01/01/2020", singletonList("caseUrn"), hearingCourtCentreName, List.of(offenceId1.toString())).collect(toList());

        assertThat(events.size(), is(1));
        final NcesEmailNotificationRequested ncesEmailNotificationRequested = Optional.of(events.get(0)).map(o -> (NcesEmailNotificationRequested) o).get();
        verifyEmailForNewApplication(subject, accountCorrelationIds.get(0), ncesEmailNotificationRequested, "caseUrn");
        assertThat(ncesEmailNotificationRequested.getHearingSittingDay(), is(hearingSittingDay.format(ofPattern(HEARING_SITTING_DAY_PATTERN))));
        assertThat(ncesEmailNotificationRequested.getHearingCourtCentreName(), is(hearingCourtCentreName));
        assertThat(ncesEmailNotificationRequested.getDefendantDateOfBirth(), is(defendantDateOfBirth));
        assertThat(ncesEmailNotificationRequested.getDefendantAddress(), is(defendantAddress));
        assertThat(ncesEmailNotificationRequested.getDefendantEmail(), is(defendantEmail));
        assertThat(ncesEmailNotificationRequested.getHearingCourtCentreName(), is(hearingCourtCentreName));
        assertThat(ncesEmailNotificationRequested.getDefendantContactNumber(), is(defendantContactNumber));
    }

    @Test
    public void shouldNotRaiseEmailEventWhenNonFinancialResults() {
        final String applicationType = "f3a6e917-7cc8-3c66-83dd-d958abd6a6e4";
        final List<UUID> accountCorrelationIds = asList(null, null, null);
        final UUID offenceId1 = randomUUID();

        updateFinancialResult(accountCorrelationIds, singletonList(offenceId1));

        final Optional<NcesEmailNotificationRequested> ncesEmailNotificationRequested = aggregate.sendNcesEmailForNewApplication(applicationType, "listingDate", singletonList("caseUrn"), hearingCourtCentreName, emptyList())
                .map(o -> (NcesEmailNotificationRequested) o)
                .findFirst();

        assertThat(ncesEmailNotificationRequested.isPresent(), is(false));
    }

    @Test
    public void shouldRaisedEmailWhenCreateApplicationInTheSameDay() {
        final String subject = "APPLICATION FOR A STATUTORY DECLARATION RECEIVED";
        final List<UUID> accountCorrelationIds = singletonList(randomUUID());
        final UUID offenceId1 = randomUUID();

        updateFinancialResult(accountCorrelationIds, singletonList(offenceId1));

        List<Object> events = aggregate.sendNcesEmailForNewApplication(STAT_DEC, "01/01/2020", singletonList("caseUrn"), hearingCourtCentreName, emptyList()).collect(toList());
        assertThat(events.size(), is(1));
        Optional.of(events.get(0)).map(o -> (MarkedAggregateSendEmailWhenAccountReceived) o).ifPresent(event ->
                verifyMarkedAggregateSendEmailWhenAccountReceivedForNewApplication(accountCorrelationIds.get(0), subject, event, "caseUrn"));

        events = updateGobAccounts(accountCorrelationIds);
        assertThat(events.size(), is(3));

        final NcesEmailNotificationRequested ncesEmailNotificationRequested = Optional.of(events.get(1)).map(o -> (NcesEmailNotificationRequested) o).get();
        verifyEmailForNewApplication(subject, accountCorrelationIds.get(0), ncesEmailNotificationRequested, "caseUrn");

        final UnmarkedAggregateSendEmailWhenAccountReceived unmarkedAggregateSendEmailWhenAccountReceived = Optional.of(events.get(2)).map(o -> (UnmarkedAggregateSendEmailWhenAccountReceived) o).get();
        assertThat(unmarkedAggregateSendEmailWhenAccountReceived.getId(), is(notNullValue()));
    }

    @ParameterizedTest
    @MethodSource("applicationUpdateSubjects")
    public void shouldRaiseEmailWhenSingleCaseSingleDefendantSingleOffenceForUpdateSubjects(final String applicationType, final String applicationResultCode, final String subject) {
        final String createAppSubject = Arrays.stream(applicationTypes()).filter(s -> s[0].equals(applicationType)).map(s -> (String) s[1]).findFirst().orElse("subject not found");
        final UUID accountCorrelationId = randomUUID();
        final UUID offenceIdA = randomUUID();

        updateFinancialResult(singletonList(accountCorrelationId), singletonList(offenceIdA));

        updateGobAccounts(singletonList(accountCorrelationId));

        final List<Object> eventsCreateApp1 = aggregate.sendNcesEmailForNewApplication(applicationType, "01/01/2020", singletonList("caseUrn1"), hearingCourtCentreName, List.of(offenceIdA.toString())).toList();
        assertThat(eventsCreateApp1.size(), is(1));
        Optional.of(eventsCreateApp1.get(0)).map(o -> (NcesEmailNotificationRequested) o).ifPresent(event ->
                verifyEmailForNewApplication(createAppSubject, accountCorrelationId, event, "caseUrn1"));


        final List<Object> eventsApp1 = raiseEventsForApplicationResult(singletonList(null), singletonList(offenceIdA), singletonList(false), singletonList("caseUrn1"), applicationType, singletonList(applicationResultCode), true);
        assertThat(eventsApp1.size(), is(1));
        Optional.of(eventsApp1.get(0)).map(o -> (NcesEmailNotificationRequested) o).ifPresent(event -> {
                    assertThat(event.getSendTo(), is("John.Doe@xxx.com"));
                    assertThat(event.getSubject(), is(subject));
                    assertThat(event.getHearingCourtCentreName(), is(hearingCourtCentreName));
                    assertThat(event.getApplicationResult(), is(APPLICATION_RESULT));
                }
        );
    }

    @Test
    public void shouldRaiseEmailWhenOneCaseMultipleOffencesAmendedThenRejectApplication() {
        final String applicationType = STAT_DEC;
        final String createAppSubject = Arrays.stream(applicationTypes()).filter(s -> s[0].equals(applicationType)).map(s -> (String) s[1]).findFirst().orElse("subject not found");
        final UUID accountCorrelationId = randomUUID();
        final UUID offenceIdA = randomUUID();
        final UUID offenceIdB = randomUUID();

        updateFinancialResult(singletonList(accountCorrelationId), asList(offenceIdA, offenceIdB));

        updateFinancialResult(singletonList(null), singletonList(offenceIdA), singletonList(false));

        List<Object> events = updateGobAccounts(singletonList(accountCorrelationId));
        assertThat(events.size(), is(1));

        final List<Object> eventsCreateApp1 = aggregate.sendNcesEmailForNewApplication(applicationType, "01/01/2020", singletonList("caseUrn1"), hearingCourtCentreName, List.of(offenceIdA.toString(), offenceIdB.toString())).collect(toList());
        assertThat(eventsCreateApp1.size(), is(1));
        Optional.of(eventsCreateApp1.get(0)).map(o -> (NcesEmailNotificationRequested) o).ifPresent(event ->
                verifyEmailForNewApplication(createAppSubject, accountCorrelationId, event, "caseUrn1"));

        final List<Object> eventsApp1 = raiseEventsForApplicationResult(singletonList(null), asList(offenceIdB, offenceIdA), asList(true, false), singletonList("caseUrn1"), applicationType, asList(WDRN, WDRN), true);
        assertThat(eventsApp1.size(), is(1));
        Optional.of(eventsApp1.get(0)).map(o -> (NcesEmailNotificationRequested) o).ifPresent(event ->
                verifyEmailWithoutOlds(STATUTORY_DECLARATION_WITHDRAWN, accountCorrelationId, singletonList(offenceIdB), event, "caseUrn1"));

    }

    private void assertApplicationProcess(final String applicationType, final List<UUID> accountCorrelationIds,
                                          final List<UUID> offenceIds) {
        final String createAppSubject = Arrays.stream(applicationTypes())
                .filter(s -> s[0].equals(applicationType))
                .map(s -> (String) s[1])
                .findFirst()
                .orElse("subject not found");

        updateFinancialResult(accountCorrelationIds, offenceIds);
        List<Object> events = updateGobAccounts(accountCorrelationIds);
        assertThat(events.size(), is(1));

        final List<Object> eventsCreateApp1 = aggregate.sendNcesEmailForNewApplication(applicationType, "01/01/2020", singletonList("caseUrn1"), hearingCourtCentreName, offenceIds.stream().map(UUID::toString).toList()).collect(toList());
        assertThat(eventsCreateApp1.size(), is(1));
        Optional.of(eventsCreateApp1.get(0)).map(o -> (NcesEmailNotificationRequested) o).ifPresent(event ->
                verifyEmailForNewApplication(createAppSubject, accountCorrelationIds.get(0), event, "caseUrn1"));
    }
    @Test
    public void shouldNotRaiseEmailWhenApplicationResultedWithoutTracked() {
        final UUID offenceIdA = randomUUID();
        final String applicationType = STAT_DEC;
        final List<Object> eventsApp1 = raiseEventsForApplicationResult(singletonList(null), singletonList(offenceIdA), singletonList(false), singletonList("caseUrn1"), applicationType, singletonList(G));
        assertThat(eventsApp1.size(), is(1));
        assertThat(eventsApp1.get(0).getClass(), is(HearingFinancialResultsTracked.class));
    }

    @Test
    public void shouldSendAccountConsolidationEmailToNCESWithMultipleOffencesAndAconResults() {
        final UUID accountCorrelationId = randomUUID();
        final UUID offenceIdA = randomUUID();
        final UUID offenceIdB = randomUUID();

        List<Object> events = raiseEventsForApplicationResult(singletonList(accountCorrelationId), asList(offenceIdA, offenceIdB), asList(true, true), singletonList("caseUrn1"), null, asList(ACON, ACON));
        assertThat(events.size(), is(2));

        events = updateGobAccounts(singletonList(accountCorrelationId));
        assertThat(events.size(), is(3));
        Optional.of(events.get(1)).map(o -> (NcesEmailNotificationRequested) o).ifPresent(event ->
                verifyEmailWithoutOldsForDeemed(ACON_EMAIL_SUBJECT, accountCorrelationId, asList(offenceIdA, offenceIdB), event, "caseUrn1", asList(" Application", " Application")));
    }

    @Test
    public void shouldSendAccountConsolidationEmailToNCESWithOneAconResult() {

        final UUID accountCorrelationId = randomUUID();
        final UUID offenceIdA = randomUUID();
        final UUID offenceIdB = randomUUID();

        List<Object> events = raiseEventsForApplicationResult(asList(accountCorrelationId), asList(offenceIdA, offenceIdB), asList(true, true), singletonList("caseUrn1"), null, asList(ACON, null));
        assertThat(events.size(), is(2));

        events = updateGobAccounts(singletonList(accountCorrelationId));
        assertThat(events.size(), is(3));
        Optional.of(events.get(1)).map(o -> (NcesEmailNotificationRequested) o).ifPresent(event ->
                verifyEmailWithoutOldsForDeemed(ACON_EMAIL_SUBJECT, accountCorrelationId, asList(offenceIdA, offenceIdB), event, "caseUrn1", asList(" Application", " Application")));
    }

    @Test
    public void shouldRaiseEmailForDeemedWhenOneCaseOneDefendantOneOffence() {
        final UUID accountCorrelationId = randomUUID();
        final UUID offenceIdA = randomUUID();

        updateFinancialResultWithDeemedServed(singletonList(accountCorrelationId), singletonList(offenceIdA), singletonList(true), singletonList(true));

        List<Object> events = updateGobAccounts(singletonList(accountCorrelationId));
        assertThat(events.size(), is(3));

        Optional.of(events.get(1)).map(o -> (NcesEmailNotificationRequested) o).ifPresent(event ->
                verifyEmailWithoutOlds(WRITE_OFF_ONE_DAY_DEEMED_SERVED, accountCorrelationId, singletonList(offenceIdA), event, "Ref1,Ref2"));

    }

    @Test
    public void shouldRaiseEmailForDeemedWhenMultipleOffencesWithFinancialOnlyOneDeemed() {
        final UUID accountCorrelationId = randomUUID();
        final UUID offenceIdA = randomUUID();
        final UUID offenceIdB = randomUUID();

        updateFinancialResultWithDeemedServed(singletonList(accountCorrelationId), asList(offenceIdA, offenceIdB), asList(true, true), asList(false, true));

        List<Object> events = updateGobAccounts(singletonList(accountCorrelationId));
        assertThat(events.size(), is(3));

        Optional.of(events.get(1)).map(o -> (NcesEmailNotificationRequested) o).ifPresent(event ->
                verifyEmailWithoutOlds(WRITE_OFF_ONE_DAY_DEEMED_SERVED, accountCorrelationId, asList(offenceIdA,offenceIdB), event, "Ref1,Ref2"));

    }

    @Test
    public void shouldRaiseEmailForDeemedWhenAmendedDeemedToNonFinancial() {
        final UUID accountCorrelationId = randomUUID();
        final UUID accountCorrelationId2 = randomUUID();
        final UUID offenceIdA = randomUUID();

        updateFinancialResultWithDeemedServed(singletonList(accountCorrelationId), singletonList(offenceIdA), singletonList(true), singletonList(true));

        updateGobAccounts(singletonList(accountCorrelationId));

        List<Object> events = updateFinancialResultWithDeemedServed(singletonList(null), singletonList(offenceIdA), singletonList(false), singletonList(false), singletonList("changed"));
        assertThat(events.size(), is(3));

        Optional.of(events.get(1)).map(o -> (NcesEmailNotificationRequested) o).ifPresent(event ->
                verifyEmailWithoutOlds(AMEND_AND_RESHARE, accountCorrelationId, singletonList(offenceIdA), event, "Ref1,Ref2", singletonList("changed")));

        updateFinancialResultWithDeemedServed(singletonList(accountCorrelationId2), singletonList(offenceIdA), singletonList(true), singletonList(true), singletonList("changed"));
        events = updateGobAccounts(singletonList(accountCorrelationId2));
        assertThat(events.size(), is(3));

        Optional.of(events.get(1)).map(o -> (NcesEmailNotificationRequested) o).ifPresent(event ->
                verifyEmailWithoutOldsForDeemed(WRITE_OFF_ONE_DAY_DEEMED_SERVED, accountCorrelationId2, singletonList(offenceIdA), event, "Ref1,Ref2", singletonList("changed")));
    }


    @Test
    public void shouldRaiseEmailForAmendmentWhenOneCaseOneOffence() {
        final UUID accountCorrelationId1 = randomUUID();
        final UUID accountCorrelationId2 = randomUUID();
        final UUID offenceIdA = randomUUID();

        updateFinancialResult(singletonList(accountCorrelationId1), singletonList(offenceIdA));
        List<Object> events = updateGobAccounts(singletonList(accountCorrelationId1));
        assertThat(events.size(), is(1));

        updateFinancialResultWithDeemedServed(singletonList(accountCorrelationId2), singletonList(offenceIdA), singletonList(true), singletonList(false), singletonList("changed"));
        events = updateGobAccounts(singletonList(accountCorrelationId2));

        assertThat(events.size(), is(3));
        Optional.of(events.get(1)).map(o -> (NcesEmailNotificationRequested) o).ifPresent(event ->
                verifyEmailWithOlds(asList(accountCorrelationId1, accountCorrelationId2), singletonList(offenceIdA), event, singletonList("changed")));


    }

    @Test
    public void shouldRaiseEmailForAmendmentWhenOneCaseTwoOffences() {
        final UUID accountCorrelationId1 = randomUUID();
        final UUID accountCorrelationId2 = randomUUID();
        final UUID offenceIdA = randomUUID();
        final UUID offenceIdB = randomUUID();

        updateFinancialResult(singletonList(accountCorrelationId1), asList(offenceIdA, offenceIdB));
        List<Object> events = updateGobAccounts(singletonList(accountCorrelationId1));
        assertThat(events.size(), is(1));

        updateFinancialResultWithDeemedServed(singletonList(accountCorrelationId2), asList(offenceIdA, offenceIdB), asList(true, true), asList(false, false), asList("changed", "changed2"));
        events = updateGobAccounts(singletonList(accountCorrelationId2));

        assertThat(events.size(), is(3));
        Optional.of(events.get(1)).map(o -> (NcesEmailNotificationRequested) o).ifPresent(event ->
                verifyEmailWithOlds(asList(accountCorrelationId1, accountCorrelationId2), asList(offenceIdA, offenceIdB), event, asList("changed", "changed2")));

    }

    @Test
    public void shouldRaiseEmailForAmendmentWhenOneCaseTwoOffencesMultipleTimes() {
        final UUID accountCorrelationId1 = randomUUID();
        final UUID accountCorrelationId2 = randomUUID();
        final UUID accountCorrelationId3 = randomUUID();
        final UUID offenceIdA = randomUUID();
        final UUID offenceIdB = randomUUID();

        updateFinancialResult(singletonList(accountCorrelationId1), asList(offenceIdA, offenceIdB));
        List<Object> events = updateGobAccounts(singletonList(accountCorrelationId1));
        assertThat(events.size(), is(1));

        updateFinancialResultWithDeemedServed(singletonList(accountCorrelationId2), asList(offenceIdA, offenceIdB), asList(true, true), asList(false, false), asList("changed", ""));
        events = updateGobAccounts(singletonList(accountCorrelationId2));
        assertThat(events.size(), is(3));
        Optional.of(events.get(1)).map(o -> (NcesEmailNotificationRequested) o).ifPresent(event ->
                verifyEmailWithOlds(asList(accountCorrelationId1, accountCorrelationId2), asList(offenceIdA, offenceIdB), event, Arrays.asList("changed", "")));

        updateFinancialResultWithDeemedServed(singletonList(accountCorrelationId3), asList(offenceIdA, offenceIdB), asList(true, true), asList(false, false), asList("changed", "changed"));
        events = updateGobAccounts(singletonList(accountCorrelationId3));
        assertThat(events.size(), is(3));
        Optional.of(events.get(1)).map(o -> (NcesEmailNotificationRequested) o).ifPresent(event ->
                verifyEmailWithOldsMultiAmendement(asList(accountCorrelationId2, accountCorrelationId3), asList(offenceIdA, offenceIdB), event, Arrays.asList("changed", "changed")));

    }

    @Test
    public void shouldRaiseEmailForAmendmentWhenAmendedDeemedToFinancial() {
        final UUID accountCorrelationId1 = randomUUID();
        final UUID accountCorrelationId2 = randomUUID();
        final UUID offenceIdA = randomUUID();

        updateFinancialResultWithDeemedServed(singletonList(accountCorrelationId1), singletonList(offenceIdA), singletonList(true), singletonList(true));

        List<Object> events = updateGobAccounts(singletonList(accountCorrelationId1));
        assertThat(events.size(), is(3));
        Optional.of(events.get(1)).map(o -> (NcesEmailNotificationRequested) o).ifPresent(event ->
                verifyEmailWithoutOlds(WRITE_OFF_ONE_DAY_DEEMED_SERVED, accountCorrelationId1, singletonList(offenceIdA), event, "Ref1,Ref2"));


        updateFinancialResultWithDeemedServed(singletonList(accountCorrelationId2), singletonList(offenceIdA), singletonList(true), singletonList(false), singletonList("changed"));
        events = updateGobAccounts(singletonList(accountCorrelationId2));

        Optional.of(events.get(1)).map(o -> (NcesEmailNotificationRequested) o).ifPresent(event ->
                verifyEmailWithOlds(asList(accountCorrelationId1, accountCorrelationId2), singletonList(offenceIdA), event, singletonList("changed")));

    }

    @Test
    public void shouldRaiseEmailForAmendmentWhenDeemedToFinancialMultipleOffences() {
        final UUID accountCorrelationId1 = randomUUID();
        final UUID accountCorrelationId2 = randomUUID();
        final UUID offenceIdA = randomUUID();
        final UUID offenceIdB = randomUUID();

        updateFinancialResultWithDeemedServed(singletonList(accountCorrelationId1), asList(offenceIdA, offenceIdB), asList(true, true), asList(true, true));

        List<Object> events = updateGobAccounts(singletonList(accountCorrelationId1));
        assertThat(events.size(), is(3));
        Optional.of(events.get(1)).map(o -> (NcesEmailNotificationRequested) o).ifPresent(event ->
                verifyEmailWithoutOlds(WRITE_OFF_ONE_DAY_DEEMED_SERVED, accountCorrelationId1, asList(offenceIdA, offenceIdB), event, "Ref1,Ref2"));


        updateFinancialResultWithDeemedServed(singletonList(accountCorrelationId2), asList(offenceIdA, offenceIdB), asList(true, true), asList(false, false), asList("amend", "amend"));
        events = updateGobAccounts(singletonList(accountCorrelationId2));

        Optional.of(events.get(1)).map(o -> (NcesEmailNotificationRequested) o).ifPresent(event ->
                verifyEmailWithOldsAndAmendmentDate(asList(accountCorrelationId1, accountCorrelationId2), asList(offenceIdA, offenceIdB), event, asList("amend", "amend"), "01/02/2021", "amendmentReason"));

    }

    @Test
    public void shouldRaiseEmailForAmendmentWhenDeemedToFinancialMultipleOffencesOnlyOneOffenceToNotDeemed() {
        final UUID accountCorrelationId1 = randomUUID();
        final UUID accountCorrelationId2 = randomUUID();
        final UUID offenceIdA = randomUUID();
        final UUID offenceIdB = randomUUID();

        updateFinancialResultWithDeemedServed(singletonList(accountCorrelationId1), asList(offenceIdA, offenceIdB), asList(true, true), asList(true, true));

        List<Object> events = updateGobAccounts(singletonList(accountCorrelationId1));
        assertThat(events.size(), is(3));
        Optional.of(events.get(1)).map(o -> (NcesEmailNotificationRequested) o).ifPresent(event ->
                verifyEmailWithoutOlds(WRITE_OFF_ONE_DAY_DEEMED_SERVED, accountCorrelationId1, asList(offenceIdA, offenceIdB), event, "Ref1,Ref2"));


        updateFinancialResultWithDeemedServed(singletonList(accountCorrelationId2), asList(offenceIdA, offenceIdB), asList(true, true), asList(true, false), asList("changed", ""));
        events = updateGobAccounts(singletonList(accountCorrelationId2));

        Optional.of(events.get(1)).map(o -> (NcesEmailNotificationRequested) o).ifPresent(event ->
                verifyEmailWithOlds(asList(accountCorrelationId1, accountCorrelationId2), asList(offenceIdA, offenceIdB), event, asList("changed", "")));

    }

    @Test
    public void shouldRaiseEmailForAmendmentWhenFinancialToDeemedMultipleOffences() {
        final UUID accountCorrelationId1 = randomUUID();
        final UUID accountCorrelationId2 = randomUUID();
        final UUID offenceIdA = randomUUID();
        final UUID offenceIdB = randomUUID();
        final UUID offenceIdC = randomUUID();

        updateFinancialResultWithDeemedServed(singletonList(accountCorrelationId1), asList(offenceIdA, offenceIdB, offenceIdC), asList(true, true, true), asList(false, false, false));

        List<Object> events = updateGobAccounts(singletonList(accountCorrelationId1));
        assertThat(events.size(), is(1));

        updateFinancialResultWithDeemedServed(singletonList(accountCorrelationId2), asList(offenceIdA, offenceIdB, offenceIdC), asList(true, true, true), asList(true, false, false), asList("changed", "", ""));
        events = updateGobAccounts(singletonList(accountCorrelationId2));
        assertThat(events.size(), is(5));
        Optional.of(events.get(1)).map(o -> (NcesEmailNotificationRequested) o).ifPresent(event ->
                verifyEmailWithOlds(asList(accountCorrelationId1, accountCorrelationId2), asList(offenceIdA, offenceIdB, offenceIdC), event, asList("changed", "", "")));
        Optional.of(events.get(2)).map(o -> (NcesEmailNotificationRequested) o).ifPresent(event ->
                verifyEmailWithoutOldsForDeemed(WRITE_OFF_ONE_DAY_DEEMED_SERVED, accountCorrelationId2, asList(offenceIdA, offenceIdB, offenceIdC), event, "Ref1,Ref2", asList("changed", "", "")));
    }

    @Disabled //TBD
    public void shouldRaiseEmailForAmendmentWhenFinancialToNonFinancial() {
        final UUID accountCorrelationId1 = randomUUID();
        final UUID accountCorrelationId2 = randomUUID();
        final UUID offenceIdA = randomUUID();

        updateFinancialResult(singletonList(accountCorrelationId1), singletonList(offenceIdA));
        List<Object> events = updateGobAccounts(singletonList(accountCorrelationId1));
        assertThat(events.size(), is(1));

        events = updateFinancialResultWithDeemedServed(singletonList(accountCorrelationId1), singletonList(offenceIdA), singletonList(false), singletonList(false), singletonList("changed"));

        assertThat(events.size(), is(2));
        Optional.of(events.get(1)).map(o -> (NcesEmailNotificationRequested) o).ifPresent(event ->
                verifyEmailWithoutOlds(AMEND_AND_RESHARE, accountCorrelationId1, singletonList(offenceIdA), event, "Ref1,Ref2", singletonList("changed")));
    }

    @Disabled //TBD
    public void shouldRaiseEmailForAmendmentWhenFinancialDeemedToNonFinancialMultipleOffences() {
        final UUID accountCorrelationId1 = randomUUID();
        final UUID accountCorrelationId2 = randomUUID();
        final UUID offenceIdA = randomUUID();
        final UUID offenceIdB = randomUUID();
        final UUID offenceIdC = randomUUID();

        updateFinancialResultWithDeemedServed(singletonList(accountCorrelationId1), asList(offenceIdA, offenceIdB, offenceIdC), asList(true, true, true), asList(true, true, true));
        List<Object> events = updateGobAccounts(singletonList(accountCorrelationId1));
        assertThat(events.size(), is(3));
        Optional.of(events.get(1)).map(o -> (NcesEmailNotificationRequested) o).ifPresent(event ->
                verifyEmailWithoutOlds(WRITE_OFF_ONE_DAY_DEEMED_SERVED, accountCorrelationId1, asList(offenceIdA, offenceIdB, offenceIdC), event, "Ref1,Ref2"));

        events = updateFinancialResultWithDeemedServed(singletonList(accountCorrelationId2), asList(offenceIdA, offenceIdB, offenceIdC), asList(false, true, true), asList(false, true, true), asList("changed", "", ""));
        assertThat(events.size(), is(3));

        Optional.of(events.get(1)).map(o -> (NcesEmailNotificationRequested) o).ifPresent(event ->
                verifyEmailWithoutOlds(AMEND_AND_RESHARE, accountCorrelationId1, asList(offenceIdA, offenceIdB, offenceIdC), event, "Ref1,Ref2"));


    }

    @Test
    public void shouldRaiseEmailForAmendmentWhenFinancialDeemedToFinancialDeemedMultipleOffences() {
        final UUID accountCorrelationId1 = randomUUID();
        final UUID accountCorrelationId2 = randomUUID();
        final UUID offenceIdA = randomUUID();
        final UUID offenceIdB = randomUUID();
        final UUID offenceIdC = randomUUID();

        updateFinancialResultWithDeemedServed(singletonList(accountCorrelationId1), asList(offenceIdA, offenceIdB, offenceIdC), asList(true, true, true), asList(true, true, true));
        List<Object> events = updateGobAccounts(singletonList(accountCorrelationId1));
        assertThat(events.size(), is(3));
        Optional.of(events.get(1)).map(o -> (NcesEmailNotificationRequested) o).ifPresent(event ->
                verifyEmailWithoutOlds(WRITE_OFF_ONE_DAY_DEEMED_SERVED, accountCorrelationId1, asList(offenceIdA, offenceIdB, offenceIdC), event, "Ref1,Ref2"));

        events = updateFinancialResultWithDeemedServed(singletonList(accountCorrelationId2), asList(offenceIdA, offenceIdB, offenceIdC), asList(true, true, true), asList(true, true, true), asList("changed", "", ""));
        assertThat(events.size(), is(3));

        events = updateGobAccounts(singletonList(accountCorrelationId2));
        assertThat(events.size(), is(5));
        Optional.of(events.get(1)).map(o -> (NcesEmailNotificationRequested) o).ifPresent(event ->
                verifyEmailWithOlds(asList(accountCorrelationId1, accountCorrelationId2), asList(offenceIdA, offenceIdB, offenceIdC), event, asList("changed", "", "")));
        Optional.of(events.get(2)).map(o -> (NcesEmailNotificationRequested) o).ifPresent(event ->
                verifyEmailWithoutOldsForDeemed(WRITE_OFF_ONE_DAY_DEEMED_SERVED, accountCorrelationId2, asList(offenceIdA, offenceIdB, offenceIdC), event, "Ref1,Ref2", asList("changed", "", "")));
    }

    @Test
    public void shouldNotRaiseEmailForAmendmentWhenNonFinancialToNonFinancial() {
        final UUID accountCorrelationId1 = randomUUID();
        final UUID offenceIdA = randomUUID();

        updateFinancialResult(singletonList(accountCorrelationId1), singletonList(offenceIdA));
        updateGobAccounts(singletonList(accountCorrelationId1));

        List<Object> events = updateFinancialResultWithDeemedServed(singletonList(null), singletonList(offenceIdA), singletonList(false), singletonList(false), singletonList("changed"));
        assertThat(events.size(), is(2));
        Optional.of(events.get(1)).map(o -> (NcesEmailNotificationRequested) o).ifPresent(event ->
                verifyEmailWithoutOlds(AMEND_AND_RESHARE, accountCorrelationId1, singletonList(offenceIdA), event, "Ref1,Ref2", singletonList("changed")));

        events = updateFinancialResultWithDeemedServed(singletonList(null), singletonList(offenceIdA), singletonList(false), singletonList(false), singletonList("changed"));
        assertThat(events.size(), is(1));
    }

    @Test
    public void shouldNotRaiseEventForAmendmentWhenNonFinancialToNonFinancialFirstResult() {
        final UUID offenceIdA = randomUUID();
        final List<Object> events = updateFinancialResultWithDeemedServed(singletonList(null), singletonList(offenceIdA), singletonList(false), singletonList(false), singletonList("changed"));
        assertThat(events.size(), is(1));
        assertThat(events.get(0).getClass(), is(HearingFinancialResultsTracked.class));
    }

    @Test
    public void shouldNotRaiseEmailWhenOneCaseMultipleOffencesThenGrantApplicationWithNoFineOffence() {
        final UUID accountCorrelationId = randomUUID();
        final UUID offenceIdA = randomUUID();
        final UUID offenceIdB = randomUUID();

        updateFinancialResult(singletonList(accountCorrelationId), asList(offenceIdA, offenceIdB), asList(true, false));

        List<Object> events = updateGobAccounts(List.of(accountCorrelationId));
        assertThat(events.size(), is(1));

        final List<Object> eventsCreateApp1 = aggregate.sendNcesEmailForNewApplication(STAT_DEC, "01/01/2020", singletonList("caseUrn1"), hearingCourtCentreName, List.of(offenceIdB.toString())).toList();

        assertThat(eventsCreateApp1.size(), is(0));
    }

    @Test
    public void shouldNotRaiseEmailWhenOneCaseMultipleOffencesAmendedThenGrantApplicationWithNoFineOffence() {
        final UUID accountCorrelationId = randomUUID();
        final UUID accountCorrelationId2 = randomUUID();
        final UUID offenceIdA = randomUUID();
        final UUID offenceIdB = randomUUID();

        updateFinancialResult(singletonList(accountCorrelationId), asList(offenceIdA, offenceIdB));

        updateFinancialResult(singletonList(accountCorrelationId2), asList(offenceIdA, offenceIdB), asList(true, false));

        List<Object> events = updateGobAccounts(List.of(accountCorrelationId, accountCorrelationId2));
        assertThat(events.size(), is(2));

        final List<Object> eventsCreateApp1 = aggregate.sendNcesEmailForNewApplication(STAT_DEC, "01/01/2020", singletonList("caseUrn1"), hearingCourtCentreName, List.of(offenceIdB.toString())).toList();

        assertThat(eventsCreateApp1.size(), is(0));
    }

    @Test
    public void shouldRaiseEmailWhenOneCaseMultipleOffencesThenGrantApplicationWithFineOffence() {
        final UUID accountCorrelationId = randomUUID();
        final UUID offenceIdA = randomUUID();
        final UUID offenceIdB = randomUUID();
        final String createAppSubject = Arrays.stream(applicationTypes()).filter(s -> s[0].equals(STAT_DEC)).map(s -> (String) s[1]).findFirst().orElse("subject not found");

        updateFinancialResult(singletonList(accountCorrelationId), asList(offenceIdA, offenceIdB), asList(true, false));

        List<Object> events = updateGobAccounts(List.of(accountCorrelationId));
        assertThat(events.size(), is(1));

        final List<Object> eventsCreateApp1 = aggregate.sendNcesEmailForNewApplication(STAT_DEC, "01/01/2020", singletonList("caseUrn1"), hearingCourtCentreName, List.of(offenceIdA.toString(), offenceIdB.toString())).toList();

        assertThat(eventsCreateApp1.size(), is(1));
        Optional.of(eventsCreateApp1.get(0)).map(o -> (NcesEmailNotificationRequested) o).ifPresent(event ->
                verifyEmailForNewApplication(createAppSubject, accountCorrelationId, event, "caseUrn1"));
    }

    private List<Object> updateGobAccounts(final List<UUID> accountCorrelationIds) {
        List<Object> events = new ArrayList<>();
        accountCorrelationIds.stream().filter(Objects::nonNull).forEach(accountCorrelationId -> {
                    events.addAll(aggregate.updateAccountNumber(accountCorrelationId + "ACCOUNT", accountCorrelationId).toList());
                    events.addAll(aggregate.checkApplicationEmailAndSend().toList());
                }
        );
        return events;
    }

    private List<Object> updateFinancialResult(final List<UUID> accountCorrelationIds, final List<UUID> offenceIds) {
        return updateFinancialResultWithDeemedServed(accountCorrelationIds, offenceIds, offenceIds.stream().map(i -> true).collect(toList()), offenceIds.stream().map(i -> false).collect(toList()));
    }

    private List<Object> updateFinancialResult(final List<UUID> accountCorrelationIds, final List<UUID> offenceIds, final List<Boolean> isFinancial) {
        return updateFinancialResultWithDeemedServed(accountCorrelationIds, offenceIds, isFinancial, offenceIds.stream().map(i -> false).collect(toList()));
    }

    private List<Object> updateFinancialResultWithDeemedServed(final List<UUID> accountCorrelationIds, final List<UUID> offenceIds, final List<Boolean> isFinancial, final List<Boolean> isDeemedServers) {
        return updateFinancialResultWithDeemedServed(accountCorrelationIds, offenceIds, isFinancial, isDeemedServers, offenceIds.stream().map(o -> "").collect(toList()));
    }

    private List<Object> updateFinancialResultWithDeemedServed(final List<UUID> accountCorrelationIds, final List<UUID> offenceIds, final List<Boolean> isFinancial, final List<Boolean> isDeemedServers, final List<String> suffixes) {
        List<Object> events = new ArrayList<>();
        accountCorrelationIds.forEach(accountCorrelationId -> {
            final AtomicInteger index = new AtomicInteger(0);
            HearingFinancialResultRequest request1 = HearingFinancialResultRequest.hearingFinancialResultRequest()
                    .withAccountCorrelationId(accountCorrelationId)
                    .withMasterDefendantId(MASTER_DEFENDANT_ID)
                    .withProsecutionCaseReferences(asList("Ref1", "Ref2"))
                    .withHearingSittingDay(hearingSittingDay)
                    .withHearingCourtCentreName(hearingCourtCentreName)
                    .withDefendantName("John Doe")
                    .withDefendantDateOfBirth(defendantDateOfBirth)
                    .withDefendantAddress(defendantAddress)
                    .withDefendantEmail(defendantEmail)
                    .withDefendantContactNumber(defendantContactNumber)
                    .withHearingId(hearingId)
                    .withIsSJPHearing(false)
                    .withNcesEmail(ncesEMail)
                    .withOffenceResults(offenceIds.stream().map(id -> OffenceResults.offenceResults()
                            .withOffenceId(id)
                            .withImpositionOffenceDetails(id.toString() + " ImpositionOffenceDetails" + suffixes.get(index.get()))
                            .withOffenceTitle(id + " offenceTitle" + suffixes.get(index.get()))
                            .withIsFinancial(isFinancial.get(index.get()))
                            .withDateOfResult("01/02/2024")
                            .withAmendmentDate(suffixes.get(index.get()).equals("") ? null : "01/02/2021")
                            .withAmendmentReason(suffixes.get(index.get()).equals("") ? null : "amendmentReason")
                            .withIsDeemedServed(isDeemedServers.get(index.getAndIncrement()))
                            .build()).collect(toList()))
                    .withAccountDivisionCode(accountCorrelationId == null ? null : accountCorrelationId + "DIVCODE")
                    .build();

            List<NewOffenceByResult> newResultByOffenceList = new ArrayList<NewOffenceByResult>();
            final AtomicInteger newOffenceIndex = new AtomicInteger(0);
            offenceIds.forEach(offenceId -> {
                int currentIndex = newOffenceIndex.getAndIncrement();
                String suffix = currentIndex < suffixes.size() ? suffixes.get(currentIndex) : "";
                NewOffenceByResult result = NewOffenceByResult.newOffenceByResult()
                        .withDetails(offenceId + " ImpositionOffenceDetails" + suffix)
                        .withOffenceDate("01/02/2024")
                        .withTitle(offenceId + " offenceTitle" + suffix)
                        .build();
                newResultByOffenceList.add(result);
            });

            events.addAll(aggregate.updateFinancialResults(request1, "false", "2021-21-21", "2021-21-21", newResultByOffenceList, APPLICATION_RESULT).collect(toList()));
        });
        return events;
    }

    private List<Object> raiseEventsForApplicationResult(final List<UUID> accountCorrelationIds,
                                                         final List<UUID> offenceIds,
                                                         final List<Boolean> isFinancial,
                                                         final List<String> caseUrns,
                                                         final String applicationType,
                                                         final List<String> applicationResultCode) {
        return raiseEventsForApplicationResult(accountCorrelationIds, offenceIds, isFinancial, caseUrns, applicationType, applicationResultCode, null, null);
    }

    private List<Object> raiseEventsForApplicationResult(final List<UUID> accountCorrelationIds,
                                                         final List<UUID> offenceIds,
                                                         final List<Boolean> isFinancial,
                                                         final List<String> caseUrns,
                                                         final String applicationType,
                                                         final List<String> applicationResultCode,
                                                         final boolean removeTrackedEvent) {
        return raiseEventsForApplicationResult(accountCorrelationIds, offenceIds, isFinancial, caseUrns, applicationType, applicationResultCode, null, null, removeTrackedEvent);
    }

    private List<Object> raiseEventsForApplicationResult(final List<UUID> accountCorrelationIds,
                                                         final List<UUID> offenceIds,
                                                         final List<Boolean> isFinancial,
                                                         final List<String> caseUrns,
                                                         final String applicationType,
                                                         final List<String> applicationResultCode,
                                                         final List<Boolean> isDeemedServed,
                                                         final List<String> amendmentDates) {
        return raiseEventsForApplicationResult(accountCorrelationIds, offenceIds, isFinancial, caseUrns, applicationType, applicationResultCode, isDeemedServed, amendmentDates, false);

    }

    private List<Object> raiseEventsForApplicationResult(final List<UUID> accountCorrelationIds,
                                                         final List<UUID> offenceIds,
                                                         final List<Boolean> isFinancial,
                                                         final List<String> caseUrns,
                                                         final String applicationType,
                                                         final List<String> applicationResultCode,
                                                         final List<Boolean> isDeemedServed,
                                                         final List<String> amendmentDates,
                                                         final boolean removeTrackedEvent
    ) {
        final List<Object> events = new ArrayList<>();
        final int offenceCount = offenceIds.size();
        accountCorrelationIds.forEach(accountCorrelationId -> {
            HearingFinancialResultRequest request1 = HearingFinancialResultRequest.hearingFinancialResultRequest()
                    .withAccountCorrelationId(accountCorrelationId)
                    .withMasterDefendantId(MASTER_DEFENDANT_ID)
                    .withProsecutionCaseReferences(caseUrns)
                    .withDefendantName("John Doe")
                    .withHearingSittingDay(hearingSittingDay)
                    .withHearingCourtCentreName(hearingCourtCentreName)
                    .withDefendantDateOfBirth(defendantDateOfBirth)
                    .withDefendantAddress(defendantAddress)
                    .withDefendantEmail(defendantEmail)
                    .withDefendantContactNumber(defendantContactNumber)
                    .withIsSJPHearing(false)
                    .withNcesEmail(ncesEMail)
                    .withOffenceResults(range(0, offenceCount)
                            .mapToObj(i -> OffenceResults.offenceResults()
                                    .withOffenceId(offenceIds.size() == offenceCount ? offenceIds.get(i) : offenceIds.get(0))
                                    .withApplicationType(applicationResultCode.get(i) == null ||
                                            applicationResultCode.get(i).equalsIgnoreCase(EMPTY) ||
                                            applicationResultCode.get(i).equalsIgnoreCase(ACON) ||
                                            applicationResultCode.get(i).startsWith(FIDI) ? null : applicationType)
                                    .withResultCode(applicationResultCode.get(i))
                                    .withDateOfResult("01/02/2024")
                                    .withImpositionOffenceDetails((offenceIds.size() == offenceCount ? offenceIds.get(i) : offenceIds.get(0))
                                            .toString() + " ImpositionOffenceDetails Application")
                                    .withOffenceTitle((offenceIds.size() == offenceCount ? offenceIds.get(i) : offenceIds.get(0))
                                            .toString() + " offenceTitle Application")
                                    .withIsFinancial(isFinancial.get(i))
                                    .withIsParentFlag(isFinancial.get(i))
                                    .withIsDeemedServed(isNotEmpty(isDeemedServed) ? isDeemedServed.get(i) : Boolean.FALSE)
                                    .withAmendmentDate(isNotEmpty(amendmentDates) ? amendmentDates.get(i) : null)
                                    .withApplicationId(randomUUID())
                                    .build())
                            .collect(toList()))
                    .withAccountDivisionCode(accountCorrelationId == null ? null : accountCorrelationId.toString() + "DIVCODE")
                    .build();
            events.addAll(aggregate.updateFinancialResults(request1, "false", "2021-21-21", "2021-21-21", null, APPLICATION_RESULT).collect(toList()));
        });
        if (removeTrackedEvent) {
            return events.stream().filter(e -> !HearingFinancialResultsTracked.class.isAssignableFrom(e.getClass())).toList();
        } else {
            return events;
        }
    }

    private void verifyMarkedAggregateSendEmailWhenAccountReceivedForNewApplication(final UUID accountCorrelationId, final String createAppSubject, final MarkedAggregateSendEmailWhenAccountReceived event, final String caseUrn) {
        assertThat(event.getAccountCorrelationId(), is(accountCorrelationId));
        verifyEmailForNewApplicationWithoutAccountNumber(createAppSubject, accountCorrelationId, convert(event), caseUrn);
    }

    private void verifyMarkedAggregate(final String subject, final UUID accountCorrelationId,
                                       final List<UUID> offenceIds, final MarkedAggregateSendEmailWhenAccountReceived event,
                                       final String caseUrn, final boolean isApplication) {
        assertThat(event.getAccountCorrelationId(), is(accountCorrelationId));
        assertThat(event.getSubject(), is(subject));
        if(nonNull(event.getHearingCourtCentreName())) {
            assertThat(event.getHearingCourtCentreName(), is(hearingCourtCentreName));
        }
        assertThat(event.getDivisionCode(), is(accountCorrelationId.toString() + "DIVCODE"));
        assertThat(event.getCaseReferences(), is(caseUrn));
        assertThat(nonNull(event.getDateDecisionMade()) ? event.getDateDecisionMade() : event.getAmendmentDate(), is("01/02/2024"));
        assertThat(event.getDefendantName(), is("John Doe"));
        assertThat(event.getSendTo(), is(ncesEMail));
        assertThat(event.getImpositionOffenceDetails().size(), is(offenceIds.size()));
        for (int i = 0; i < offenceIds.size(); i++) {
            assertThat(event.getImpositionOffenceDetails().get(i).getDetails(),
                    is(offenceIds.get(i).toString() + " ImpositionOffenceDetails".concat(isApplication ? " Application" : "")));
            assertThat(event.getImpositionOffenceDetails().get(i).getTitle(),
                    is(offenceIds.get(i).toString() + " offenceTitle".concat(isApplication ? " Application" : "")));
        }
    }

    private NcesEmailNotificationRequested convert(MarkedAggregateSendEmailWhenAccountReceived marked) {
        return NcesEmailNotificationRequested.ncesEmailNotificationRequested()
                .withDivisionCode(marked.getDivisionCode())
                .withListedDate(marked.getListedDate())
                .withImpositionOffenceDetails(marked.getImpositionOffenceDetails())
                .withDateDecisionMade(marked.getDateDecisionMade())
                .withGobAccountNumber(marked.getGobAccountNumber())
                .withMasterDefendantId(marked.getMasterDefendantId())
                .withCaseReferences(marked.getCaseReferences())
                .withSubject(marked.getSubject())
                .withSendTo(marked.getSendTo())
                .withDivisionCode(marked.getDivisionCode())
                .withDefendantName(marked.getDefendantName())
                .build();
    }

    private void verifyEmailForNewApplication(final String createAppSubject, final UUID accountCorrelationId, final NcesEmailNotificationRequested event, final String caseUrn) {
        assertThat(event.getGobAccountNumber(), is(accountCorrelationId.toString() + "ACCOUNT"));
        assertThat(event.getMaterialId(), is(notNullValue()));
        assertThat(event.getHearingCourtCentreName(), is(hearingCourtCentreName));
        verifyEmailForNewApplicationWithoutAccountNumber(createAppSubject, accountCorrelationId, event, caseUrn);
    }

    private void verifyEmailForNewApplicationWithoutAccountNumber(final String createAppSubject, final UUID accountCorrelationId, final NcesEmailNotificationRequested event, final String caseUrn) {
        assertThat(event.getSubject(), is(createAppSubject));
        assertThat(event.getDivisionCode(), is(accountCorrelationId.toString() + "DIVCODE"));
        assertThat(event.getCaseReferences(), is(caseUrn));
        assertThat(event.getListedDate(), is("01/01/2020"));
        assertThat(event.getDefendantName(), is("John Doe"));
        assertThat(event.getSendTo(), is("John.Doe@xxx.com"));
    }

    private void verifyEmailWithoutOlds(final String subject, final UUID accountCorrelationId, final List<UUID> offenceIds, final NcesEmailNotificationRequested ncesEmailNotificationRequestedApp1, final String caseUrn) {
        verifyEmailWithoutOlds(subject, accountCorrelationId, offenceIds, ncesEmailNotificationRequestedApp1, caseUrn, offenceIds.stream().map(o -> "").collect(toList()));
    }

    private void verifyEmailWithoutOlds(final String subject, final UUID accountCorrelationId, final List<UUID> offenceIds, final NcesEmailNotificationRequested ncesEmailNotificationRequestedApp1, final String caseUrn, final List<String> suffixes) {
        String CASE_REOPENED = "Case reopened";

        assertThat(ncesEmailNotificationRequestedApp1.getMaterialId(), is(notNullValue()));
        assertThat(ncesEmailNotificationRequestedApp1.getGobAccountNumber(), is(accountCorrelationId.toString() + "ACCOUNT"));
        assertThat(ncesEmailNotificationRequestedApp1.getSubject(), is(subject));
        assertThat(ncesEmailNotificationRequestedApp1.getDivisionCode(), is(accountCorrelationId.toString() + "DIVCODE"));
        assertThat(ncesEmailNotificationRequestedApp1.getCaseReferences(), is(caseUrn));
        assertThat(ncesEmailNotificationRequestedApp1.getDefendantName(), is("John Doe"));
        assertThat(ncesEmailNotificationRequestedApp1.getSendTo(), is("John.Doe@xxx.com"));
        assertThat(ncesEmailNotificationRequestedApp1.getImpositionOffenceDetails().size(), is(offenceIds.size()));

        offenceIds.forEach(oId -> {
            assertThat(ncesEmailNotificationRequestedApp1.getImpositionOffenceDetails().stream().filter(o -> o.getOffenceId().equals(oId)).findFirst().get().getDetails(), is(oId + " ImpositionOffenceDetails"));
            assertThat(ncesEmailNotificationRequestedApp1.getImpositionOffenceDetails().stream().filter(o -> o.getOffenceId().equals(oId)).findFirst().get().getTitle(), is(oId + " offenceTitle"));
        });

        if (subject.equals(WRITE_OFF_ONE_DAY_DEEMED_SERVED)) {
            assertThat(ncesEmailNotificationRequestedApp1.getOriginalDateOfSentence(), is(hearingSittingDay.format(ofPattern(BRITISH_DATE_FORMAT))));
            assertThat(ncesEmailNotificationRequestedApp1.getAmendmentReason(), is(nullValue()));
        } else if (applicationSubjects.contains(subject) || ACON_EMAIL_SUBJECT.equals(subject)) {
            assertThat(ncesEmailNotificationRequestedApp1.getAmendmentReason(), is(nullValue()));
        } else {
            assertThat(ncesEmailNotificationRequestedApp1.getAmendmentDate(), is("01/02/2021"));
            assertThat(ncesEmailNotificationRequestedApp1.getAmendmentReason(), is("amendmentReason"));
            assertThat(ncesEmailNotificationRequestedApp1.getDefendantDateOfBirth(), is(defendantDateOfBirth));
            assertThat(ncesEmailNotificationRequestedApp1.getDefendantEmail(), is(nullValue()));
            assertThat(ncesEmailNotificationRequestedApp1.getDefendantContactNumber(), is(nullValue()));
            assertThat(ncesEmailNotificationRequestedApp1.getHearingSittingDay(), is(nullValue()));
        }
        if (applicationGrantedSubjects.contains(subject)) {
            assertThat(ncesEmailNotificationRequestedApp1.getIsFinancialPenaltiesWrittenOff(), is("false"));
            assertThat(ncesEmailNotificationRequestedApp1.getOriginalDateOfSentence(), is("2021-21-21"));
        }
        if (APPLICATION_UPDATED_SUBJECT.values().stream().anyMatch(e -> e.equals(subject))) {
            assertThat(ncesEmailNotificationRequestedApp1.getApplicationResult().split(",")[0], is(CASE_REOPENED));
        }

        if (Arrays.stream(applicationTypes()).map(a -> a[1]).collect(Collectors.toList()).contains(subject)) {
            assertThat(ncesEmailNotificationRequestedApp1.getDefendantDateOfBirth(), is(defendantDateOfBirth));
            assertThat(ncesEmailNotificationRequestedApp1.getDefendantEmail(), is(defendantEmail));
            assertThat(ncesEmailNotificationRequestedApp1.getDefendantContactNumber(), is(defendantContactNumber));
            assertThat(ncesEmailNotificationRequestedApp1.getHearingSittingDay(), is(hearingSittingDay.format(ofPattern(HEARING_SITTING_DAY_PATTERN))));
            assertThat(ncesEmailNotificationRequestedApp1.getHearingCourtCentreName(), is(hearingCourtCentreName));
        }

        assertThat(ncesEmailNotificationRequestedApp1.getOldGobAccountNumber(), is(nullValue()));
        assertThat(ncesEmailNotificationRequestedApp1.getOldDivisionCode(), is(nullValue()));

    }

    private void verifyEmailWithoutOldsForDeemed(final String subject, final UUID accountCorrelationId, final List<UUID> offenceIds, final NcesEmailNotificationRequested ncesEmailNotificationRequestedApp1, final String caseUrn, final List<String> suffixes) {
        String CASE_REOPENED = "Case reopened";

        assertThat(ncesEmailNotificationRequestedApp1.getMaterialId(), is(notNullValue()));
        assertThat(ncesEmailNotificationRequestedApp1.getGobAccountNumber(), is(accountCorrelationId.toString() + "ACCOUNT"));
        assertThat(ncesEmailNotificationRequestedApp1.getSubject(), is(subject));
        assertThat(ncesEmailNotificationRequestedApp1.getDivisionCode(), is(accountCorrelationId.toString() + "DIVCODE"));
        assertThat(ncesEmailNotificationRequestedApp1.getCaseReferences(), is(caseUrn));
        assertThat(ncesEmailNotificationRequestedApp1.getDefendantName(), is("John Doe"));
        assertThat(ncesEmailNotificationRequestedApp1.getSendTo(), is("John.Doe@xxx.com"));
        assertThat(ncesEmailNotificationRequestedApp1.getImpositionOffenceDetails().size(), is(offenceIds.size()));
        for (int i = 0; i < offenceIds.size(); i++) {
            assertThat(ncesEmailNotificationRequestedApp1.getImpositionOffenceDetails().get(i).getDetails(), is(offenceIds.get(i).toString() + " ImpositionOffenceDetails" + suffixes.get(i)));
            assertThat(ncesEmailNotificationRequestedApp1.getImpositionOffenceDetails().get(i).getTitle(), is(offenceIds.get(i).toString() + " offenceTitle" + suffixes.get(i)));
        }

        if (subject.equals(WRITE_OFF_ONE_DAY_DEEMED_SERVED)) {
            assertThat(ncesEmailNotificationRequestedApp1.getOriginalDateOfSentence(), is(hearingSittingDay.format(ofPattern(BRITISH_DATE_FORMAT))));
            assertThat(ncesEmailNotificationRequestedApp1.getAmendmentReason(), is(nullValue()));
        } else if (applicationSubjects.contains(subject) || ACON_EMAIL_SUBJECT.equals(subject)) {
            assertThat(ncesEmailNotificationRequestedApp1.getAmendmentReason(), is(nullValue()));
        } else {
            assertThat(ncesEmailNotificationRequestedApp1.getAmendmentDate(), is("01/02/2021"));
            assertThat(ncesEmailNotificationRequestedApp1.getAmendmentReason(), is("amendmentReason"));
            assertThat(ncesEmailNotificationRequestedApp1.getDefendantDateOfBirth(), is(defendantDateOfBirth));
            assertThat(ncesEmailNotificationRequestedApp1.getDefendantEmail(), is(nullValue()));
            assertThat(ncesEmailNotificationRequestedApp1.getDefendantContactNumber(), is(nullValue()));
            assertThat(ncesEmailNotificationRequestedApp1.getHearingSittingDay(), is(nullValue()));
        }
        if (applicationGrantedSubjects.contains(subject)) {
            assertThat(ncesEmailNotificationRequestedApp1.getIsFinancialPenaltiesWrittenOff(), is("false"));
            assertThat(ncesEmailNotificationRequestedApp1.getOriginalDateOfSentence(), is("2021-21-21"));
        }
        if (APPLICATION_UPDATED_SUBJECT.values().stream().anyMatch(e -> e.equals(subject))) {
            assertThat(ncesEmailNotificationRequestedApp1.getApplicationResult().split(",")[0], is(CASE_REOPENED));
        }

        if (Arrays.stream(applicationTypes()).map(a -> a[1]).collect(Collectors.toList()).contains(subject)) {
            assertThat(ncesEmailNotificationRequestedApp1.getDefendantDateOfBirth(), is(defendantDateOfBirth));
            assertThat(ncesEmailNotificationRequestedApp1.getDefendantEmail(), is(defendantEmail));
            assertThat(ncesEmailNotificationRequestedApp1.getDefendantContactNumber(), is(defendantContactNumber));
            assertThat(ncesEmailNotificationRequestedApp1.getHearingSittingDay(), is(hearingSittingDay.format(ofPattern(HEARING_SITTING_DAY_PATTERN))));
            assertThat(ncesEmailNotificationRequestedApp1.getHearingCourtCentreName(), is(hearingCourtCentreName));
        }

        assertThat(ncesEmailNotificationRequestedApp1.getOldGobAccountNumber(), is(nullValue()));
        assertThat(ncesEmailNotificationRequestedApp1.getOldDivisionCode(), is(nullValue()));

    }

    private void verifyEmailWithOlds(final List<UUID> accountCorrelationIds, final List<UUID> offenceIds, final NcesEmailNotificationRequested ncesEmailNotificationRequestedApp1, final List<String> suffixes) {
        verifyEmailWithOldsAndAmendmentDate(accountCorrelationIds, offenceIds, ncesEmailNotificationRequestedApp1, suffixes, "01/02/2021", "amendmentReason");
    }

    private void verifyEmailWithOldsMultiAmendement(final List<UUID> accountCorrelationIds, final List<UUID> offenceIds, final NcesEmailNotificationRequested ncesEmailNotificationRequestedApp1, final List<String> suffixes) {
        assertThat(ncesEmailNotificationRequestedApp1.getGobAccountNumber(), is(accountCorrelationIds.get(1).toString() + "ACCOUNT"));
        assertThat(ncesEmailNotificationRequestedApp1.getDivisionCode(), is(accountCorrelationIds.get(1).toString() + "DIVCODE"));
        assertThat(ncesEmailNotificationRequestedApp1.getOldGobAccountNumber(), is(accountCorrelationIds.get(0).toString() + "ACCOUNT"));
        assertThat(ncesEmailNotificationRequestedApp1.getOldDivisionCode(), is(accountCorrelationIds.get(0).toString() + "DIVCODE"));
        assertThat(ncesEmailNotificationRequestedApp1.getSubject(), is(HearingFinancialResultsAggregateTest.AMEND_AND_RESHARE));
        assertThat(ncesEmailNotificationRequestedApp1.getCaseReferences(), is("Ref1,Ref2"));
        assertThat(ncesEmailNotificationRequestedApp1.getDefendantName(), is("John Doe"));
        assertThat(ncesEmailNotificationRequestedApp1.getSendTo(), is("John.Doe@xxx.com"));
        assertThat(ncesEmailNotificationRequestedApp1.getImpositionOffenceDetails().size(), is(offenceIds.size()));
        assertThat(ncesEmailNotificationRequestedApp1.getImpositionOffenceDetails().stream().filter(imp -> imp.getOffenceId().equals(offenceIds.get(0))).findFirst().get().getDetails(), is(offenceIds.get(0).toString() + " ImpositionOffenceDetailschanged"));
        assertThat(ncesEmailNotificationRequestedApp1.getImpositionOffenceDetails().stream().filter(imp -> imp.getOffenceId().equals(offenceIds.get(0))).findFirst().get().getTitle(), is(offenceIds.get(0).toString() + " offenceTitlechanged"));
        assertThat(ncesEmailNotificationRequestedApp1.getImpositionOffenceDetails().stream().filter(imp -> imp.getOffenceId().equals(offenceIds.get(1))).findFirst().get().getDetails(), is(offenceIds.get(1).toString() + " ImpositionOffenceDetails"));
        assertThat(ncesEmailNotificationRequestedApp1.getImpositionOffenceDetails().stream().filter(imp -> imp.getOffenceId().equals(offenceIds.get(1))).findFirst().get().getTitle(), is(offenceIds.get(1).toString() + " offenceTitle"));
        assertThat(ncesEmailNotificationRequestedApp1.getAmendmentDate(), is("01/02/2021"));
        assertThat(ncesEmailNotificationRequestedApp1.getAmendmentReason(), is("amendmentReason"));
        assertThat(ncesEmailNotificationRequestedApp1.getNewOffenceByResult().size(), is(offenceIds.size()));
        for (int i = 0; i < offenceIds.size(); i++) {
            assertThat(ncesEmailNotificationRequestedApp1.getNewOffenceByResult().get(i).getDetails(), is(offenceIds.get(i).toString() + " ImpositionOffenceDetails" + suffixes.get(i)));
            assertThat(ncesEmailNotificationRequestedApp1.getNewOffenceByResult().get(i).getTitle(), is(offenceIds.get(i).toString() + " offenceTitle" + suffixes.get(i)));
        }
    }

    private void verifyEmailWithOldsAndAmendmentDate(final List<UUID> accountCorrelationIds, final List<UUID> offenceIds, final NcesEmailNotificationRequested ncesEmailNotificationRequestedApp1, final List<String> suffixes, final String amendmentDate, final String amendmentReason) {
        assertThat(ncesEmailNotificationRequestedApp1.getGobAccountNumber(), is(accountCorrelationIds.get(1).toString() + "ACCOUNT"));
        assertThat(ncesEmailNotificationRequestedApp1.getDivisionCode(), is(accountCorrelationIds.get(1).toString() + "DIVCODE"));
        assertThat(ncesEmailNotificationRequestedApp1.getOldGobAccountNumber(), is(accountCorrelationIds.get(0).toString() + "ACCOUNT"));
        assertThat(ncesEmailNotificationRequestedApp1.getOldDivisionCode(), is(accountCorrelationIds.get(0).toString() + "DIVCODE"));
        assertThat(ncesEmailNotificationRequestedApp1.getSubject(), is(HearingFinancialResultsAggregateTest.AMEND_AND_RESHARE));
        assertThat(ncesEmailNotificationRequestedApp1.getCaseReferences(), is("Ref1,Ref2"));
        assertThat(ncesEmailNotificationRequestedApp1.getDefendantName(), is("John Doe"));
        assertThat(ncesEmailNotificationRequestedApp1.getSendTo(), is("John.Doe@xxx.com"));
        assertThat(ncesEmailNotificationRequestedApp1.getImpositionOffenceDetails().size(), is(offenceIds.size()));

        offenceIds.forEach(offenceId -> assertThat(ncesEmailNotificationRequestedApp1.getImpositionOffenceDetails().stream().filter(imp -> imp.getOffenceId().equals(offenceId)).findFirst().get().getDetails(), is(offenceId + " ImpositionOffenceDetails")));
        assertThat(ncesEmailNotificationRequestedApp1.getAmendmentDate(), is(amendmentDate));
        assertThat(ncesEmailNotificationRequestedApp1.getAmendmentReason(), is(amendmentReason));
        assertThat(ncesEmailNotificationRequestedApp1.getNewOffenceByResult().size(), is(offenceIds.size()));
        for (int i = 0; i < offenceIds.size(); i++) {
            assertThat(ncesEmailNotificationRequestedApp1.getNewOffenceByResult().get(i).getDetails(), is(offenceIds.get(i).toString() + " ImpositionOffenceDetails" + suffixes.get(i)));
            assertThat(ncesEmailNotificationRequestedApp1.getNewOffenceByResult().get(i).getTitle(), is(offenceIds.get(i).toString() + " offenceTitle" + suffixes.get(i)));
        }
    }
}