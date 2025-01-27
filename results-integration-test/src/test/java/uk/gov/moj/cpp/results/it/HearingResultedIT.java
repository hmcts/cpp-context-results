package uk.gov.moj.cpp.results.it;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.nio.charset.Charset.defaultCharset;
import static java.time.LocalDate.now;
import static java.time.LocalDate.of;
import static java.util.Arrays.asList;
import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static javax.json.Json.createReader;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.gov.justice.core.courts.CourtApplication.courtApplication;
import static uk.gov.justice.core.courts.CourtApplicationCase.courtApplicationCase;
import static uk.gov.justice.core.courts.CourtCentre.courtCentre;
import static uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES;
import static uk.gov.justice.core.courts.ProsecutionCase.prosecutionCase;
import static uk.gov.justice.core.courts.ProsecutionCaseIdentifier.prosecutionCaseIdentifier;
import static uk.gov.justice.core.courts.external.ApiCourtCentre.apiCourtCentre;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.FUTURE_LOCAL_DATE;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.domains.results.shareresults.PublicHearingResulted.publicHearingResulted;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.closeMessageConsumers;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.createMessageConsumers;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.getHearingDetails;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.getHearingDetailsForHearingId;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.getHearingDetailsForHearingIdAndHearingDate;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.getInternalHearingDetailsForHearingId;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.getSummariesByDate;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.hearingResultsHaveBeenSharedV2;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.thenReturnsBadRequestForResultsSummaryWithoutFromDate;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.verifyPublicEventForPoliceResultsGenerated;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.verifyPublicEventPoliceResultGeneratedAndReturnPayload;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.verifyPublicEventPoliceResultGeneratedMessage;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.verifyPublicEventPoliceResultGeneratedNotRaised;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.verifyPublicEventPoliceResultsGenerated;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.whenPrisonAdminTriesToViewResultsForThePerson;
import static uk.gov.moj.cpp.results.it.steps.data.factory.HearingResultDataFactory.getUserId;
import static uk.gov.moj.cpp.results.it.stub.NotificationNotifyServiceStub.verifyEmailNotificationIsRaised;
import static uk.gov.moj.cpp.results.it.stub.ProgressionStub.stubGetProgressionCaseExistsByUrn;
import static uk.gov.moj.cpp.results.it.stub.ProgressionStub.stubGetProgressionProsecutionCases;
import static uk.gov.moj.cpp.results.it.utils.EventGridStub.stubEventGridEndpoint;
import static uk.gov.moj.cpp.results.it.utils.HttpClientUtil.sendGeneratePoliceResultsForADefendantCommand;
import static uk.gov.moj.cpp.results.it.utils.Queries.pollForMatch;
import static uk.gov.moj.cpp.results.it.utils.ReferenceDataServiceStub.PROSECUTOR_WITH_SPI_OUT_FALSE;
import static uk.gov.moj.cpp.results.it.utils.ReferenceDataServiceStub.stubBailStatuses;
import static uk.gov.moj.cpp.results.it.utils.ReferenceDataServiceStub.stubCountryNationalities;
import static uk.gov.moj.cpp.results.it.utils.ReferenceDataServiceStub.stubGetOrgainsationUnit;
import static uk.gov.moj.cpp.results.it.utils.ReferenceDataServiceStub.stubJudicialResults;
import static uk.gov.moj.cpp.results.it.utils.ReferenceDataServiceStub.stubModeOfTrialReasons;
import static uk.gov.moj.cpp.results.it.utils.ReferenceDataServiceStub.stubPoliceFlag;
import static uk.gov.moj.cpp.results.it.utils.ReferenceDataServiceStub.stubSpiOutFlag;
import static uk.gov.moj.cpp.results.it.utils.WireMockStubUtils.setupUserAsPrisonAdminGroup;
import static uk.gov.moj.cpp.results.it.utils.WireMockStubUtils.stubNotificationNotifyEndPoint;
import static uk.gov.moj.cpp.results.test.TestTemplates.basicShareHearingTemplateWithApplication;
import static uk.gov.moj.cpp.results.test.TestTemplates.basicShareHearingTemplateWithCustomApplication;
import static uk.gov.moj.cpp.results.test.TestTemplates.basicShareResultsV2Template;
import static uk.gov.moj.cpp.results.test.TestTemplates.basicShareResultsV2TemplateForIndicatedPlea;
import static uk.gov.moj.cpp.results.test.TestTemplates.basicShareResultsV2TemplateWithHearingDay;
import static uk.gov.moj.cpp.results.test.TestTemplates.basicShareResultsV2WithMagistratesAlongWithOffenceDateCodeTemplate;
import static uk.gov.moj.cpp.results.test.TestTemplates.basicShareResultsV2WithVerdictTemplate;
import static uk.gov.moj.cpp.results.test.TestTemplates.buildJudicialResultList;
import static uk.gov.moj.cpp.results.test.matchers.BeanMatcher.isBean;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingResultsAdded;
import uk.gov.justice.core.courts.IndicatedPleaValue;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.Verdict;
import uk.gov.justice.core.courts.VerdictType;
import uk.gov.justice.core.courts.external.ApiAddress;
import uk.gov.justice.core.courts.external.ApiCourtCentre;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.domains.results.shareresults.PublicHearingResulted;
import uk.gov.moj.cpp.results.query.view.response.HearingResultSummariesView;
import uk.gov.moj.cpp.results.query.view.response.HearingResultSummaryView;
import uk.gov.moj.cpp.results.test.TestTemplates;
import uk.gov.moj.cpp.results.test.matchers.BeanMatcher;

import java.io.InputStream;
import java.io.StringReader;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.jms.JMSException;
import javax.json.JsonObject;
import javax.json.JsonReader;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsNull;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings({"unchecked", "serial", "squid:S2925", "squid:S1607"})
public class HearingResultedIT {

    private static final String TEMPLATE_PAYLOAD_CROWN = "json/public.events.hearing.hearing-resulted-crown.json";
    private static final String TEMPLATE_PAYLOAD_RESHARE_CROWN = "json/public.events.hearing.hearing-results-reshared-crown.json";
    private static final String TEMPLATE_PAYLOAD_GROUP_CASES = "json/public.events.hearing.hearing-resulted-group-cases.json";
    private static final String SESSION_ID = "sessionId";
    private static final String CASE_ID = "caseId";
    private static final String DEFENDANT_ID = "defendantId";
    private static final String CASE_ID_VALUE = "cccc1111-1e20-4c21-916a-81a6c90239e5";
    private static final String DEFENDANT_ID_VALUE = "dddd1111-1e20-4c21-916a-81a6c90239e5";

    private static final String FINDING_VALUE_DEFAULT = "G";
    private static final String FINDING_VALUE_NOT_GUILTY = "N";
    private static final String OU_CODE = "0300000";
    private static final String PROSECUTION_AUTHORITY = "bdc190e7-c939-37ca-be4b-9f615d6ef40e";

    private static final String HEARING_RESULT_APPLICATION_ONLY_JURISDICTION_CROWN = "json/public.hearing-resulted_with_crown_judicialresults_in_application_only.json";
    private static final String HEARING_RESULT_RESHARE_APPLICATION_ONLY_JURISDICTION_CROWN = "json/public.hearing-result_reshare_crown_judicialresults_in_application_only.json";
    private static final String HEARING_RESULT_APPLICATION_ONLY_JURISDICTION_MAGS = "json/public.hearing-resulted_with_mags_judicialresults_in_application_only.json";

    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
    private final JsonObjectToObjectConverter jsonToObjectConverter = new JsonObjectToObjectConverter(objectMapper);

    private final String policeEmailAddress = randomAlphabetic(10) + "@email.com";

    @BeforeAll
    public static void setUpClass() {
        setupUserAsPrisonAdminGroup(getUserId());
        stubEventGridEndpoint();
        stubCountryNationalities();
        stubGetOrgainsationUnit();
        stubJudicialResults();
        stubBailStatuses();
        stubModeOfTrialReasons();
        stubPoliceFlag(OU_CODE, PROSECUTION_AUTHORITY);
        stubNotificationNotifyEndPoint();
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());

    }

    @AfterEach
    public void teardown() throws JMSException {
        closeMessageConsumers();
    }

    @BeforeEach
    public void setUp() {
        stubSpiOutFlag(true, true);
        createMessageConsumers();
    }

    @Test
    public void testCCForSpiOutWithIndicatedPleaAndOtherBasicParameters() {
        final PublicHearingResulted resultsMessage = basicShareResultsV2TemplateForIndicatedPlea(MAGISTRATES);
        setOuCodeAndProsecutorAuthority(resultsMessage);

        hearingResultsHaveBeenSharedV2(resultsMessage);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());

        LocalDate startDate = resultsMessage.getHearing().getHearingDays().get(0).getSittingDay().toLocalDate();
        startDate = of(startDate.getYear(), startDate.getMonth(), startDate.getDayOfMonth() - 1);

        getSummariesByDate(startDate);

        Optional<String> response = verifyPublicEventPoliceResultGeneratedAndReturnPayload();
        final JSONObject firstOffence = new JSONObject(response.get()).getJSONObject("defendant").getJSONArray("offences").getJSONObject(0);
        assertThat(firstOffence.get("offenceSequenceNumber"), is(resultsMessage.getHearing().getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(0).getOrderIndex()));
        assertThat(firstOffence.getJSONObject("indicatedPlea").getString("indicatedPleaValue"), is(IndicatedPleaValue.INDICATED_GUILTY.name()));
        assertThat(firstOffence.get("offenceDateCode"), is(1));
        assertThat(firstOffence.getString("finding"), is(FINDING_VALUE_DEFAULT));
    }

    @Test
    public void testCCForSpiOutWithOffenceDateCode() {
        final Integer offenceDateCode = 4;
        final PublicHearingResulted resultsMessage = basicShareResultsV2WithMagistratesAlongWithOffenceDateCodeTemplate(offenceDateCode);
        setOuCodeAndProsecutorAuthority(resultsMessage);

        hearingResultsHaveBeenSharedV2(resultsMessage);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());

        LocalDate startDate = resultsMessage.getHearing().getHearingDays().get(0).getSittingDay().toLocalDate();
        startDate = of(startDate.getYear(), startDate.getMonth(), startDate.getDayOfMonth() - 1);

        getSummariesByDate(startDate);

        Optional<String> response = verifyPublicEventPoliceResultGeneratedAndReturnPayload();
        assertThat(resultsMessage.getHearing().getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(0).getOrderIndex(), is(new JSONObject(response.get()).getJSONObject("defendant").getJSONArray("offences").getJSONObject(0).get("offenceSequenceNumber")));
        assertThat(resultsMessage.getHearing().getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(0).getOffenceDateCode(), is(new JSONObject(response.get()).getJSONObject("defendant").getJSONArray("offences").getJSONObject(0).get("offenceDateCode")));
    }

    @Test
    public void whenVerdictPresentThenPoliceResultGeneratedHasFindingWithVerdictCode() throws JMSException {
        final PublicHearingResulted resultsMessage = basicShareResultsV2WithVerdictTemplate(MAGISTRATES, true, false);
        setOuCodeAndProsecutorAuthority(resultsMessage);

        hearingResultsHaveBeenSharedV2(resultsMessage);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());
        verifyPublicEventPoliceResultGeneratedMessage(FINDING_VALUE_NOT_GUILTY);
    }

    @Test
    public void testCCForEmailNotificationSuccess() {
        final PublicHearingResulted resultsMessage = basicShareResultsV2Template(JurisdictionType.CROWN);
        stubSpiOutFlag(true, true, policeEmailAddress);
        setOuCodeAndProsecutorAuthority(resultsMessage);

        hearingResultsHaveBeenSharedV2(resultsMessage);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());

        LocalDate startDate = resultsMessage.getHearing().getHearingDays().get(0).getSittingDay().toLocalDate();
        startDate = of(startDate.getYear(), startDate.getMonth(), startDate.getDayOfMonth() - 1);

        getSummariesByDate(startDate);
        verifyEmailNotificationIsRaised(List.of(policeEmailAddress));
    }

    @Test
    public void testCCForSpiOutFalse() throws JMSException {
        stubSpiOutFlag(false, true);
        final PublicHearingResulted resultsMessage = basicShareResultsV2Template(MAGISTRATES);
        setOuCodeAndProsecutorAuthority(resultsMessage);

        hearingResultsHaveBeenSharedV2(resultsMessage);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());

        LocalDate startDate = resultsMessage.getHearing().getHearingDays().get(0).getSittingDay().toLocalDate();
        startDate = of(startDate.getYear(), startDate.getMonth(), startDate.getDayOfMonth() - 1);

        getSummariesByDate(startDate);
        verifyPublicEventPoliceResultGeneratedNotRaised();

        final JsonObject payload = createObjectBuilder()
                .add(SESSION_ID, resultsMessage.getHearing().getId().toString())
                .add(CASE_ID, CASE_ID_VALUE)
                .add(DEFENDANT_ID, DEFENDANT_ID_VALUE)
                .build();

        sendGeneratePoliceResultsForADefendantCommand(payload);
        verifyPublicEventForPoliceResultsGenerated(true);
    }

    @Test
    public void shouldSendEmailForFirstShareAndAmendmentsForCrownCourt() {
        final UUID hearingId = randomUUID();
        final JsonObject payload = getPayload(TEMPLATE_PAYLOAD_CROWN, hearingId);
        stubSpiOutFlag(true, true, policeEmailAddress);
        hearingResultsHaveBeenSharedV2(payload);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());

        final LocalDate startDate = of(2019, 5, 25);

        getSummariesByDate(startDate);
        verifyEmailNotificationIsRaised(List.of(policeEmailAddress), List.of("Imprisonment"));

        final JsonObject amendedPayload = getPayload(TEMPLATE_PAYLOAD_RESHARE_CROWN, hearingId);
        hearingResultsHaveBeenSharedV2(amendedPayload);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());

        getSummariesByDate(startDate);
        verifyEmailNotificationIsRaised(List.of(policeEmailAddress, "Imprisonment"));
    }

    @Test
    public void testCCForUpdatedEventGroupCases() throws JMSException {
        final UUID hearingId = randomUUID();
        final UUID groupId = randomUUID();
        System.out.println(hearingId);
        final JsonObject payload = getPayload(TEMPLATE_PAYLOAD_GROUP_CASES, hearingId, groupId);

        hearingResultsHaveBeenSharedV2(payload);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());

        final LocalDate startDate = of(2019, 5, 25);

        getSummariesByDate(startDate);

        final List<Boolean> isGroupMember = asList(Boolean.TRUE, Boolean.TRUE, Boolean.FALSE);
        final List<Boolean> isGroupMaster = asList(Boolean.TRUE, Boolean.FALSE, null);

        verifyPublicEventPoliceResultsGenerated(Boolean.TRUE, groupId.toString(), isGroupMember, isGroupMaster);
    }

    @Test
    public void journeyHearingToResults() {
        final PublicHearingResulted resultsMessage = basicShareResultsV2Template(MAGISTRATES);
        setOuCodeAndProsecutorAuthority(resultsMessage);
        final Hearing hearingIn = resultsMessage.getHearing();
        final UUID defendantId0 =
                resultsMessage.getHearing().getProsecutionCases().get(0).getDefendants().get(0).getId();

        //share results
        hearingResultsHaveBeenSharedV2(resultsMessage);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());

        LocalDate startDate = resultsMessage.getHearing().getHearingDays().get(0).getSittingDay().toLocalDate();
        startDate = of(startDate.getYear(), startDate.getMonth(), startDate.getDayOfMonth() - 1);

        //search summaries
        HearingResultSummariesView summaries;

        final Function<HearingResultSummariesView, List<HearingResultSummaryView>> resultsFilter =
                summs -> summs.getResults().stream().filter(sum -> sum.getHearingId().equals(hearingIn.getId()))
                        .collect(Collectors.toList());


        final long defendantCount = hearingIn.getProsecutionCases().stream().mapToLong(c -> c.getDefendants().size()).sum();

        final BeanMatcher<HearingResultSummariesView> summaryCheck = isBean(HearingResultSummariesView.class)
                .withValue(summs -> resultsFilter.apply(summs).size(),
                        (int) hearingIn.getProsecutionCases().stream().mapToLong(pc -> pc.getDefendants().size()).sum())
                .with(resultsFilter::apply,
                        hasItem(isBean(HearingResultSummaryView.class)
                                .withValue(HearingResultSummaryView::getHearingId, hearingIn.getId())
                                .withValue(HearingResultSummaryView::getHearingType, hearingIn.getType().getDescription())
                        )
                );

        final LocalDate searchStartDate = startDate;
        pollForMatch(15, 500, () -> getSummariesByDate(searchStartDate), summaryCheck);

        final LocalDate earlierDate = hearingIn.getHearingDays().stream().map(hd -> hd.getSittingDay().toLocalDate()).min(Comparator.naturalOrder()).orElse(null).minusDays(1);
        final LocalDate laterDate = hearingIn.getHearingDays().stream().map(hd -> hd.getSittingDay().toLocalDate()).max(Comparator.naturalOrder()).orElse(null).plusDays(1);

        //check that date filters work
        summaries = getSummariesByDate(earlierDate);
        assertThat(resultsFilter.apply(summaries).size(), is((int) defendantCount));
        summaries = getSummariesByDate(laterDate);
        assertThat(resultsFilter.apply(summaries).size(), is(0));

        //matcher to check details results
        final Matcher<HearingResultsAdded> matcher = isBean(HearingResultsAdded.class)
                .with(HearingResultsAdded::getHearing, isBean(Hearing.class)
                        .withValue(Hearing::getId, hearingIn.getId())
                        .withValue(Hearing::getJurisdictionType, hearingIn.getJurisdictionType())
                        .withValue(Hearing::getCourtApplications, resultsMessage.getHearing().getCourtApplications())
                        .withValue(Hearing::getType, hearingIn.getType())
                );
        // check the details from query
        getHearingDetails(resultsMessage.getHearing().getId(), defendantId0, matcher);

        Matcher<HearingResultsAdded> matcherStatus = isBean(HearingResultsAdded.class)
                .with(HearingResultsAdded::getSharedTime, is(resultsMessage.getSharedTime()))
                .with(HearingResultsAdded::getHearing, isBean(Hearing.class)
                        .withValue(Hearing::getId, resultsMessage.getHearing().getId())
                        .withValue(Hearing::getCourtApplications, resultsMessage.getHearing().getCourtApplications())
                        .withValue(hearing -> hearing.getProsecutionCases().get(0).getDefendants().get(0).getPersonDefendant().getPersonDetails().getTitle(), "Baroness")
                );

        // get the details and check
        getHearingDetails(resultsMessage.getHearing().getId(), defendantId0, matcherStatus);
    }

    @Test
    public void outOfOrderJourney() {
        final PublicHearingResulted resultsMessage = basicShareResultsV2Template(MAGISTRATES);
        setOuCodeAndProsecutorAuthority(resultsMessage);
        final Hearing hearingIn = resultsMessage.getHearing();
        final UUID defendantId0 =
                resultsMessage.getHearing().getProsecutionCases().get(0).getDefendants().get(0).getId();


        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());
        hearingResultsHaveBeenSharedV2(resultsMessage);

        LocalDate startDate = resultsMessage.getHearing().getHearingDays().get(0).getSittingDay().toLocalDate();
        startDate = of(startDate.getYear(), startDate.getMonth(), startDate.getDayOfMonth() - 1);
        //search summaries
        HearingResultSummariesView summaries;

        final Function<HearingResultSummariesView, List<HearingResultSummaryView>> resultsFilter =
                summs -> summs.getResults().stream().filter(sum -> sum.getHearingId().equals(hearingIn.getId()))
                        .collect(Collectors.toList());

        final BeanMatcher<HearingResultSummariesView> summaryCheck = isBean(HearingResultSummariesView.class)
                .withValue(summs -> resultsFilter.apply(summs).size(),
                        (int) hearingIn.getProsecutionCases().stream().mapToLong(pc -> pc.getDefendants().size()).sum())
                .with(resultsFilter::apply,
                        hasItem(isBean(HearingResultSummaryView.class)
                                .withValue(HearingResultSummaryView::getHearingId, hearingIn.getId())
                                .withValue(HearingResultSummaryView::getHearingType, hearingIn.getType().getDescription())
                        )
                );
        final LocalDate searchStartDate = startDate;
        pollForMatch(15, 500, () -> getSummariesByDate(searchStartDate), summaryCheck);

        summaries = getSummariesByDate(startDate);
        assertThat(summaries, summaryCheck);

        //matcher to check details results
        final Matcher<HearingResultsAdded> matcher = isBean(HearingResultsAdded.class)
                .with(HearingResultsAdded::getHearing, isBean(Hearing.class)
                        .withValue(Hearing::getId, hearingIn.getId())
                        .withValue(Hearing::getJurisdictionType, hearingIn.getJurisdictionType())
                        .withValue(Hearing::getType, hearingIn.getType())
                );
        // check the details from query
        getHearingDetails(resultsMessage.getHearing().getId(), defendantId0, matcher);
    }

    @Test
    public void getHearingDetails_shouldReturnBadRequestForResultsSummaryWithoutFromDate() {
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());
        thenReturnsBadRequestForResultsSummaryWithoutFromDate();
    }

    @Test
    public void testJourneyHearingToDisplayAllDetailsInResults() {
        PublicHearingResulted resultsMessage = basicShareResultsV2Template(MAGISTRATES);
        setOuCodeAndProsecutorAuthority(resultsMessage);

        final Hearing hearingIn = resultsMessage.getHearing();
        hearingResultsHaveBeenSharedV2(resultsMessage);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());
        ApiCourtCentre expectedCourtCentre =
                apiCourtCentre()
                        .withAddress(ApiAddress.apiAddress()
                                .withAddress1(hearingIn.getCourtCentre().getAddress().getAddress1())
                                .withAddress2(hearingIn.getCourtCentre().getAddress().getAddress2())
                                .withAddress3(hearingIn.getCourtCentre().getAddress().getAddress3())
                                .withAddress4(hearingIn.getCourtCentre().getAddress().getAddress4())
                                .withAddress5(hearingIn.getCourtCentre().getAddress().getAddress5())
                                .withPostcode(hearingIn.getCourtCentre().getAddress().getPostcode()).build())
                        .withId(hearingIn.getCourtCentre().getId())
                        .withName(hearingIn.getCourtCentre().getName())
                        .withRoomId(hearingIn.getCourtCentre().getRoomId())
                        .withRoomName(hearingIn.getCourtCentre().getRoomName())
                        .withWelshName(hearingIn.getCourtCentre().getWelshName())
                        .withWelshRoomName(hearingIn.getCourtCentre().getWelshRoomName()).build();

        final Matcher[] matcher1 = {
                withJsonPath("$.hearing.id", equalTo(hearingIn.getId().toString())),
                withJsonPath("$.hearing", Matchers.notNullValue()),
                withJsonPath("$.hearing.courtCentre.name", equalTo(expectedCourtCentre.getName())),
                withJsonPath("$.hearing.courtCentre.id", equalTo(expectedCourtCentre.getId().toString())),
                withJsonPath("$.hearing.courtCentre.roomId", equalTo(expectedCourtCentre.getRoomId().toString())),
                withJsonPath("$.hearing.courtCentre.roomName", equalTo(expectedCourtCentre.getRoomName())),
                withJsonPath("$.hearing.courtCentre.welshName", equalTo(expectedCourtCentre.getWelshName())),
                withJsonPath("$.hearing.courtCentre.welshRoomName", equalTo(expectedCourtCentre.getWelshRoomName())),
                withJsonPath("$.hearing.courtCentre.address.address1", equalTo(expectedCourtCentre.getAddress().getAddress1())),
                withJsonPath("$.hearing.courtCentre.address.address2", equalTo(expectedCourtCentre.getAddress().getAddress2())),
                withJsonPath("$.hearing.courtCentre.address.postcode", equalTo(expectedCourtCentre.getAddress().getPostcode())),
                withJsonPath("$.sharedTime", notNullValue())
        };
        getHearingDetailsForHearingId(resultsMessage.getHearing().getId(), matcher1);

    }

    @Test
    public void testJourneyHearingToDisplayAllDetailsInResultsWithHearingDate() {
        PublicHearingResulted resultsMessage = basicShareResultsV2TemplateWithHearingDay(MAGISTRATES, LocalDate.of(2018, 5, 2));

        final Hearing hearingIn = resultsMessage.getHearing();
        hearingResultsHaveBeenSharedV2(resultsMessage);

        resultsMessage = basicShareResultsV2TemplateWithHearingDay(MAGISTRATES, LocalDate.of(2018, 5, 3));
        resultsMessage.setHearing(Hearing.hearing().withValuesFrom(resultsMessage.getHearing())
                .withId(hearingIn.getId()).build());

        hearingResultsHaveBeenSharedV2(resultsMessage);

        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());
        ApiCourtCentre expectedCourtCentre =
                apiCourtCentre()
                        .withAddress(ApiAddress.apiAddress()
                                .withAddress1(hearingIn.getCourtCentre().getAddress().getAddress1())
                                .withAddress2(hearingIn.getCourtCentre().getAddress().getAddress2())
                                .withAddress3(hearingIn.getCourtCentre().getAddress().getAddress3())
                                .withAddress4(hearingIn.getCourtCentre().getAddress().getAddress4())
                                .withAddress5(hearingIn.getCourtCentre().getAddress().getAddress5())
                                .withPostcode(hearingIn.getCourtCentre().getAddress().getPostcode()).build())
                        .withId(hearingIn.getCourtCentre().getId())
                        .withName(hearingIn.getCourtCentre().getName())
                        .withRoomId(hearingIn.getCourtCentre().getRoomId())
                        .withRoomName(hearingIn.getCourtCentre().getRoomName())
                        .withWelshName(hearingIn.getCourtCentre().getWelshName())
                        .withWelshRoomName(hearingIn.getCourtCentre().getWelshRoomName()).build();

        final Matcher[] matcher1 = {
                withJsonPath("$.hearing.id", equalTo(hearingIn.getId().toString())),
                withJsonPath("$.hearing", Matchers.notNullValue()),
                withJsonPath("$.hearing.courtCentre.name", equalTo(expectedCourtCentre.getName())),
                withJsonPath("$.hearing.courtCentre.id", equalTo(expectedCourtCentre.getId().toString())),
                withJsonPath("$.hearing.courtCentre.roomId", equalTo(expectedCourtCentre.getRoomId().toString())),
                withJsonPath("$.hearing.courtCentre.roomName", equalTo(expectedCourtCentre.getRoomName())),
                withJsonPath("$.hearing.courtCentre.welshName", equalTo(expectedCourtCentre.getWelshName())),
                withJsonPath("$.hearing.courtCentre.welshRoomName", equalTo(expectedCourtCentre.getWelshRoomName())),
                withJsonPath("$.hearing.courtCentre.address.address1", equalTo(expectedCourtCentre.getAddress().getAddress1())),
                withJsonPath("$.hearing.courtCentre.address.address2", equalTo(expectedCourtCentre.getAddress().getAddress2())),
                withJsonPath("$.hearing.courtCentre.address.postcode", equalTo(expectedCourtCentre.getAddress().getPostcode())),
                withJsonPath("$.sharedTime", notNullValue()),
                withJsonPath("$.hearing.hearingDays[3].sittingDay", is("2018-05-02T00:00:00.000Z"))
        };
        getHearingDetailsForHearingIdAndHearingDate(resultsMessage.getHearing().getId(), LocalDate.of(2018, 5, 2), matcher1);
    }

    @Test
    public void shouldDisplayAllInternalDetailsInHearingResults() {
        PublicHearingResulted resultsMessage = basicShareResultsV2Template(MAGISTRATES);
        setOuCodeAndProsecutorAuthority(resultsMessage);
        final Hearing hearingIn = resultsMessage.getHearing();

        hearingResultsHaveBeenSharedV2(resultsMessage);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());
        CourtCentre expectedCourtCentre =
                courtCentre()
                        .withAddress(Address.address()
                                .withAddress1(hearingIn.getCourtCentre().getAddress().getAddress1())
                                .withAddress2(hearingIn.getCourtCentre().getAddress().getAddress2())
                                .withAddress3(hearingIn.getCourtCentre().getAddress().getAddress3())
                                .withAddress4(hearingIn.getCourtCentre().getAddress().getAddress4())
                                .withAddress5(hearingIn.getCourtCentre().getAddress().getAddress5())
                                .withPostcode(hearingIn.getCourtCentre().getAddress().getPostcode()).build())
                        .withId(hearingIn.getCourtCentre().getId())
                        .withName(hearingIn.getCourtCentre().getName())
                        .withRoomId(hearingIn.getCourtCentre().getRoomId())
                        .withRoomName(hearingIn.getCourtCentre().getRoomName())
                        .withWelshName(hearingIn.getCourtCentre().getWelshName())
                        .withWelshRoomName(hearingIn.getCourtCentre().getWelshRoomName()).build();

        final Matcher[] matcher = {
                withJsonPath("$.hearing.id", equalTo(hearingIn.getId().toString())),
                withJsonPath("$.hearing", Matchers.notNullValue()),
                withJsonPath("$.hearing.courtCentre.name", equalTo(expectedCourtCentre.getName())),
                withJsonPath("$.hearing.courtCentre.id", equalTo(expectedCourtCentre.getId().toString())),
                withJsonPath("$.hearing.courtCentre.roomId", equalTo(expectedCourtCentre.getRoomId().toString())),
                withJsonPath("$.hearing.courtCentre.roomName", equalTo(expectedCourtCentre.getRoomName())),
                withJsonPath("$.hearing.courtCentre.welshName", equalTo(expectedCourtCentre.getWelshName())),
                withJsonPath("$.hearing.courtCentre.welshRoomName", equalTo(expectedCourtCentre.getWelshRoomName())),
                withJsonPath("$.hearing.courtCentre.address.address1", equalTo(expectedCourtCentre.getAddress().getAddress1())),
                withJsonPath("$.hearing.courtCentre.address.address2", equalTo(expectedCourtCentre.getAddress().getAddress2())),
                withJsonPath("$.hearing.courtCentre.address.postcode", equalTo(expectedCourtCentre.getAddress().getPostcode())),
                withJsonPath("$.sharedTime", notNullValue())
        };
        getInternalHearingDetailsForHearingId(resultsMessage.getHearing().getId(), matcher);
    }

    @Test
    public void shouldBeSentToSpiOutForApplicationWithNoJudicialResultsV2() throws JMSException {

        final PublicHearingResulted resultsMessage = publicHearingResulted()
                .setHearing(basicShareHearingTemplateWithApplication(randomUUID(), MAGISTRATES))
                .setSharedTime(ZonedDateTime.now(ZoneId.of("UTC")));
        resultsMessage.setIsReshare(Optional.of(false));
        resultsMessage.setHearingDay(Optional.of(LocalDate.of(2018, 5, 2)));

        final ProsecutionCase prosecutionCase = prosecutionCase().withValuesFrom(resultsMessage.getHearing().getProsecutionCases().get(0))
                .withOriginatingOrganisation(null)
                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier().withValuesFrom(resultsMessage.getHearing().getProsecutionCases().get(0).getProsecutionCaseIdentifier())
                        .withProsecutionAuthorityCode(PROSECUTOR_WITH_SPI_OUT_FALSE).build())
                .withOriginatingOrganisation(null)
                .build();
        final CourtApplicationCase courtApplicationCase = courtApplicationCase().
                withValuesFrom(resultsMessage.getHearing().getCourtApplications().get(0).getCourtApplicationCases().get(0))
                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier().withValuesFrom(resultsMessage.getHearing().getProsecutionCases().get(0).getProsecutionCaseIdentifier())
                        .withProsecutionAuthorityCode(PROSECUTOR_WITH_SPI_OUT_FALSE).build()).build();

        resultsMessage.getHearing().getProsecutionCases().set(0, prosecutionCase);
        resultsMessage.getHearing().getCourtApplications().get(0).getCourtApplicationCases().set(0, courtApplicationCase);

        setOuCodeAndProsecutorAuthority(resultsMessage);

        hearingResultsHaveBeenSharedV2(resultsMessage);

        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());

        LocalDate startDate = resultsMessage.getHearing().getHearingDays().get(0).getSittingDay().toLocalDate();
        startDate = of(startDate.getYear(), startDate.getMonth(), startDate.getDayOfMonth() - 1);

        getSummariesByDate(startDate);

        Optional<String> response = verifyPublicEventPoliceResultGeneratedAndReturnPayload();

        final JSONObject firstOffenceJson = new JSONObject(response.get()).getJSONObject("defendant").getJSONArray("offences").getJSONObject(1);
        assertThat(firstOffenceJson.get("offenceSequenceNumber"), is(65));
        assertThat(new JSONObject(response.get()).getJSONObject("defendant").get("prosecutorReference"), is("0800PP0100000000001H"));
        assertThat(firstOffenceJson.get("finding"), is("N"));
        assertThat(firstOffenceJson.getJSONObject("plea").get("pleaValue"), is("NOT_GUILTY"));
    }

    @Test
    public void shouldBeSentToSpiOutForApplicationWithJudicialResultsV2() throws JMSException {

        final PublicHearingResulted resultsMessage = publicHearingResulted()
                .setHearing(basicShareHearingTemplateWithCustomApplication(randomUUID(), MAGISTRATES,
                        asList(courtApplication()
                                .withId(fromString("f8254db1-1683-483e-afb3-b87fde5a0a26"))
                                .withApplicationReceivedDate(FUTURE_LOCAL_DATE.next())
                                .withApplicationReference("OFFENCE_CODE_REFERENCE")
                                .withType(TestTemplates.courtApplicationTypeTemplates())
                                .withApplicant(TestTemplates.courtApplicationPartyTemplates())
                                .withApplicationStatus(ApplicationStatus.DRAFT)
                                .withSubject(TestTemplates.courtApplicationPartyTemplates())
                                .withCourtApplicationCases(asList(TestTemplates.createCourtApplicationCaseWithOffences()))
                                .withApplicationParticulars("bail application")
                                .withJudicialResults(buildJudicialResultList())
                                .withAllegationOrComplaintStartDate(LocalDate.now())
                                .withPlea(uk.gov.justice.core.courts.Plea.plea().withOffenceId(UUID.randomUUID()).withPleaDate(LocalDate.now()).withPleaValue("NOT_GUILTY").build())
                                .withVerdict(Verdict.verdict()
                                        .withVerdictType(VerdictType.verdictType()
                                                .withId(fromString("3f0d69d0-2fda-3472-8d4c-a6248f661825"))
                                                .withCategory(STRING.next())
                                                .withCategoryType(STRING.next())
                                                .withCjsVerdictCode("N")
                                                .build())
                                        .withOriginatingHearingId(randomUUID())
                                        .withOffenceId(UUID.randomUUID())
                                        .withVerdictDate(now())
                                        .build())
                                .build())))
                .setSharedTime(ZonedDateTime.now(ZoneId.of("UTC")));

        final ProsecutionCase prosecutionCase = prosecutionCase().withValuesFrom(resultsMessage.getHearing().getProsecutionCases().get(0))
                .withOriginatingOrganisation(null)
                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier().withValuesFrom(resultsMessage.getHearing().getProsecutionCases().get(0).getProsecutionCaseIdentifier())
                        .withProsecutionAuthorityCode(PROSECUTOR_WITH_SPI_OUT_FALSE).build())
                .withOriginatingOrganisation(null)
                .build();
        final CourtApplicationCase courtApplicationCase = courtApplicationCase().
                withValuesFrom(resultsMessage.getHearing().getCourtApplications().get(0).getCourtApplicationCases().get(0))
                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier().withValuesFrom(resultsMessage.getHearing().getProsecutionCases().get(0).getProsecutionCaseIdentifier())
                        .withProsecutionAuthorityCode(PROSECUTOR_WITH_SPI_OUT_FALSE).build()).build();

        resultsMessage.getHearing().getProsecutionCases().set(0, prosecutionCase);
        resultsMessage.getHearing().getCourtApplications().get(0).getCourtApplicationCases().set(0, courtApplicationCase);


        resultsMessage.setIsReshare(Optional.of(false));
        resultsMessage.setHearingDay(Optional.of(LocalDate.of(2018, 5, 2)));
        setOuCodeAndProsecutorAuthority(resultsMessage);
        hearingResultsHaveBeenSharedV2(resultsMessage);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());

        LocalDate startDate = resultsMessage.getHearing().getHearingDays().get(0).getSittingDay().toLocalDate();
        startDate = of(startDate.getYear(), startDate.getMonth(), startDate.getDayOfMonth() - 1);

        getSummariesByDate(startDate);

        Optional<String> response = verifyPublicEventPoliceResultGeneratedAndReturnPayload();
        final JSONObject firstOffenceJson = new JSONObject(response.get()).getJSONObject("defendant").getJSONArray("offences").getJSONObject(1);
        assertThat(firstOffenceJson.get("offenceSequenceNumber"), is(0));
        assertThat(new JSONObject(response.get()).getJSONObject("defendant").get("prosecutorReference"), is("0800PP0100000000001H"));
        assertThat(firstOffenceJson.get("finding"), is("N"));
        assertThat(firstOffenceJson.getJSONObject("plea").get("pleaValue"), is("NOT_GUILTY"));
    }

    @Test
    public void shouldSentSpiOutForApplicationWithCourtOrderOnlyV2() throws JMSException {
        final UUID caseId = randomUUID();
        final String caseUrn = "32DN1212262";
        final JsonObject payload = getPayload("json/public.events.hearing.hearing-resulted-court-order.json", randomUUID(), caseId, null);
        final PublicHearingResulted publicHearingResulted = jsonToObjectConverter.convert(payload, PublicHearingResulted.class);

        stubGetProgressionProsecutionCases(caseId);
        stubGetProgressionCaseExistsByUrn(caseUrn, caseId);
        hearingResultsHaveBeenSharedV2(publicHearingResulted);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());

        LocalDate startDate = publicHearingResulted.getHearing().getHearingDays().get(0).getSittingDay().toLocalDate();
        startDate = of(startDate.getYear(), startDate.getMonth(), startDate.getDayOfMonth() - 1);

        getSummariesByDate(startDate);

        Optional<String> response = verifyPublicEventPoliceResultGeneratedAndReturnPayload();
        JSONObject jsonObject = new JSONObject(response.get());
        if (!jsonObject.getString("caseId").equalsIgnoreCase(caseId.toString())) {
            response = verifyPublicEventPoliceResultGeneratedAndReturnPayload();
            jsonObject = new JSONObject(response.get());
        }
        assertThat(jsonObject.getString("caseId"), is(caseId.toString()));
        assertThat(jsonObject.getJSONObject("defendant").getJSONArray("offences").length(), is(2));
        assertThat(jsonObject.getJSONObject("defendant").getJSONArray("offences").getJSONObject(0).getString("id"), is("b729153d-50e3-4ce4-811c-f16799043d4f"));
    }

    @Test
    public void testCCForEmailNotificationSuccess_WhenApplicationIsResultedOnly() throws JMSException {
        final UUID hearingId = randomUUID();
        final UUID caseId = randomUUID();
        final String caseUrn = "31DI1504926";
        JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(new ObjectMapperProducer().objectMapper());
        final PublicHearingResulted resultsMessage = jsonObjectToObjectConverter.convert(getPayload(HEARING_RESULT_APPLICATION_ONLY_JURISDICTION_CROWN, hearingId, caseId, null), PublicHearingResulted.class);

        stubGetProgressionProsecutionCases(caseId);
        stubGetProgressionCaseExistsByUrn(caseUrn, caseId);
        stubSpiOutFlag(true, true, policeEmailAddress);
        setOuCodeAndProsecutorAuthority(resultsMessage);

        hearingResultsHaveBeenSharedV2(resultsMessage);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());

        LocalDate startDate = resultsMessage.getHearing().getHearingDays().get(0).getSittingDay().toLocalDate();
        startDate = of(startDate.getYear(), startDate.getMonth(), startDate.getDayOfMonth() - 1);

        getSummariesByDate(startDate);
        verifyEmailNotificationIsRaised(List.of(policeEmailAddress));
    }

    @Test
    public void testCCForEmailNotificationSuccess_WhenApplicationIsResultedAndResharedOnly() throws JMSException {
        final UUID hearingId = randomUUID();
        final UUID caseId = fromString("cfb28f37-5159-4297-ab6a-653a63627d0b");
        final String caseUrn = "31DI1504926";
        JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(new ObjectMapperProducer().objectMapper());
        final PublicHearingResulted resultsMessage = jsonObjectToObjectConverter.convert(getPayload(HEARING_RESULT_APPLICATION_ONLY_JURISDICTION_CROWN, hearingId, caseId, null), PublicHearingResulted.class);

        stubGetProgressionProsecutionCases(caseId);
        stubGetProgressionCaseExistsByUrn(caseUrn, caseId);
        stubSpiOutFlag(true, true, policeEmailAddress);
        setOuCodeAndProsecutorAuthority(resultsMessage);

        hearingResultsHaveBeenSharedV2(resultsMessage);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());

        LocalDate startDate = resultsMessage.getHearing().getHearingDays().get(0).getSittingDay().toLocalDate();
        startDate = of(startDate.getYear(), startDate.getMonth(), startDate.getDayOfMonth() - 1);

        getSummariesByDate(startDate);
        verifyEmailNotificationIsRaised(List.of(policeEmailAddress, "TestArmand TestKrajcik"));

        final PublicHearingResulted resultsMessage1 = jsonObjectToObjectConverter.convert(getPayload(HEARING_RESULT_RESHARE_APPLICATION_ONLY_JURISDICTION_CROWN, hearingId), PublicHearingResulted.class);
        hearingResultsHaveBeenSharedV2(resultsMessage1);
        verifyEmailNotificationIsRaised(List.of(policeEmailAddress, "TestArmand1 TestKrajcik1"));
    }

    @Test
    public void shouldSentSpiOutForResultApplicationOnlyForMags() throws JMSException {
        final UUID hearingId = randomUUID();
        final UUID caseId = randomUUID();
        final String caseUrn = "31DI1504926";
        JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(new ObjectMapperProducer().objectMapper());
        final PublicHearingResulted publicHearingResulted = jsonObjectToObjectConverter.convert(getPayload(HEARING_RESULT_APPLICATION_ONLY_JURISDICTION_MAGS, hearingId, caseId, null), PublicHearingResulted.class);

        stubGetProgressionProsecutionCases(caseId);
        stubGetProgressionCaseExistsByUrn(caseUrn, caseId);
        hearingResultsHaveBeenSharedV2(publicHearingResulted);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());

        LocalDate startDate = publicHearingResulted.getHearing().getHearingDays().get(0).getSittingDay().toLocalDate();
        startDate = of(startDate.getYear(), startDate.getMonth(), startDate.getDayOfMonth() - 1);

        getSummariesByDate(startDate);

        Optional<String> response = verifyPublicEventPoliceResultGeneratedAndReturnPayload();
        final JSONObject jsonObject = new JSONObject(response.get());
        assertThat(jsonObject.getString("caseId"), is(caseId.toString()));
    }

    private void setOuCodeAndProsecutorAuthority(final PublicHearingResulted resultsMessage) {
        if (null != resultsMessage.getHearing().getProsecutionCases() && !resultsMessage.getHearing().getProsecutionCases().isEmpty()) {
            int size = resultsMessage.getHearing().getProsecutionCases().size();
            for (int i = 0; i < size; i++) {
                final ProsecutionCase prosecutionCase = prosecutionCase().withValuesFrom(resultsMessage.getHearing().getProsecutionCases().get(i))
                        .withOriginatingOrganisation(OU_CODE)
                        .withProsecutionCaseIdentifier(prosecutionCaseIdentifier().withValuesFrom(resultsMessage.getHearing().getProsecutionCases().get(i).getProsecutionCaseIdentifier())
                                .withProsecutionAuthorityCode(PROSECUTION_AUTHORITY)
                                .withProsecutionAuthorityOUCode(OU_CODE).build())
                        .build();

                resultsMessage.getHearing().getProsecutionCases().set(i, prosecutionCase);

            }
        }
    }

    private JsonObject getPayload(final String path, final UUID hearingId) {
        return getPayload(path, hearingId, null);
    }

    private JsonObject getPayload(final String path, final UUID hearingId, final UUID groupId) {
        String request = null;
        try {
            final InputStream inputStream = HearingResultedIT.class.getClassLoader().getResourceAsStream(path);
            assertThat(inputStream, IsNull.notNullValue());
            final String groupIdStr = nonNull(groupId) ? groupId.toString() : EMPTY;
            request = IOUtils.toString(inputStream, defaultCharset())
                    .replace("HEARING_ID", hearingId.toString())
                    .replace("GROUP_ID", groupIdStr);
        } catch (final Exception e) {
            fail("Error consuming file from location " + path);
        }
        final JsonReader reader = createReader(new StringReader(request));
        return reader.readObject();
    }

    private JsonObject getPayload(final String path, final UUID hearingId, final UUID caseId, final UUID groupId) {
        String request = null;
        try {
            final InputStream inputStream = HearingResultedIT.class.getClassLoader().getResourceAsStream(path);
            assertThat(inputStream, IsNull.notNullValue());
            final String groupIdStr = nonNull(groupId) ? groupId.toString() : EMPTY;
            request = IOUtils.toString(inputStream, defaultCharset())
                    .replace("HEARING_ID", hearingId.toString())
                    .replace("CASE_ID", caseId.toString())
                    .replace("GROUP_ID", groupIdStr);
        } catch (final Exception e) {
            fail("Error consuming file from location " + path);
        }
        final JsonReader reader = createReader(new StringReader(request));
        return reader.readObject();
    }
}