package uk.gov.moj.cpp.results.it;

import static java.time.LocalDate.of;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.closeMessageConsumers;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.createMessageConsumers;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.getHearingDetails;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.getSummariesByDate;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.hearingResultsHaveBeenShared;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.publicSjpResultedShared;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.thenReturnsBadRequestForResultsSummaryWithoutFromDate;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.verifyInPublicTopic;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.verifyNotInPublicTopic;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.verifyPrivateEvents;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.verifyPrivateEventsForAmendment;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.verifyPrivateEventsForCaseRejectedSjp;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.verifyPrivateEventsForPoliceGenerateResultsForDefendant;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.verifyPrivateEventsForRejected;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.whenPrisonAdminTriesToViewResultsForThePerson;
import static uk.gov.moj.cpp.results.it.steps.data.factory.HearingResultDataFactory.getUserId;
import static uk.gov.moj.cpp.results.it.utils.AuthorisationServiceStub.stubEnableAllCapabilities;
import static uk.gov.moj.cpp.results.it.utils.EventGridStub.stubEventGridEndpoint;
import static uk.gov.moj.cpp.results.it.utils.HttpClientUtil.sendGeneratePoliceResultsForADefendantCommand;
import static uk.gov.moj.cpp.results.it.utils.ReferenceDataServiceStub.stubBailStatuses;
import static uk.gov.moj.cpp.results.it.utils.ReferenceDataServiceStub.stubCountryNationalities;
import static uk.gov.moj.cpp.results.it.utils.ReferenceDataServiceStub.stubGetOrgainsationUnit;
import static uk.gov.moj.cpp.results.it.utils.ReferenceDataServiceStub.stubJudicialResults;
import static uk.gov.moj.cpp.results.it.utils.ReferenceDataServiceStub.stubModeOfTrialReasons;
import static uk.gov.moj.cpp.results.it.utils.ReferenceDataServiceStub.stubSpiOutFlag;
import static uk.gov.moj.cpp.results.it.utils.WireMockStubUtils.setupUserAsPrisonAdminGroup;
import static uk.gov.moj.cpp.results.test.TestTemplates.basicSJPCaseResulted;
import static uk.gov.moj.cpp.results.test.TestTemplates.basicShareResultsTemplate;
import static uk.gov.moj.cpp.results.test.TestTemplates.basicShareResultsTemplateWithoutResult;
import static uk.gov.moj.cpp.results.test.matchers.BeanMatcher.isBean;

import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingResultsAdded;
import uk.gov.justice.core.courts.external.ApiAddress;
import uk.gov.justice.core.courts.external.ApiCourtCentre;
import uk.gov.justice.core.courts.external.ApiHearing;
import uk.gov.justice.sjp.results.PublicSjpResulted;
import uk.gov.moj.cpp.domains.results.shareresults.PublicHearingResulted;
import uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions;
import uk.gov.moj.cpp.results.it.utils.Queries;
import uk.gov.moj.cpp.results.query.view.response.HearingResultSummariesView;
import uk.gov.moj.cpp.results.query.view.response.HearingResultSummaryView;
import uk.gov.moj.cpp.results.test.matchers.BeanMatcher;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.jms.JMSException;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import com.google.common.io.Resources;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings({"unchecked", "serial", "squid:S2925", "squid:S1607"})
public class ResultsIT {

    private static final String PROSECUTOR_WITH_SPI_OUT_FALSE = "prosecutorWithSpiOutFalse";
    private static final String TEMPLATE_PAYLOAD = "json/public.hearing-resulted.json";
    private static final String TEMPLATE_PAYLOAD_1 = "json/public.hearing-result-amended.json";
    private static final String SESSION_ID = "sessionId";
    private static final String CASE_ID = "caseId";
    private static final String DEFENDANT_ID = "defendantId";
    private static final String CASE_ID_VALUE = "cccc1111-1e20-4c21-916a-81a6c90239e5";
    private static final String DEFENDANT_ID_VALUE = "dddd1111-1e20-4c21-916a-81a6c90239e5";

    private static JsonObject getPayload(final String path, final UUID hearingId) {
        String request = null;
        try {
            request = Resources.toString(
                    Resources.getResource(path),
                    Charset.defaultCharset());
            request = request.replace("HEARING_ID", hearingId.toString());
        } catch (final Exception e) {
            fail("Error consuming file from location " + path);
        }
        final JsonReader reader = Json.createReader(new StringReader(request));
        return reader.readObject();
    }

    @After
    public void teardown() throws JMSException {
        closeMessageConsumers();
    }

    @Before
    public void setUp() throws IOException {

        setupUserAsPrisonAdminGroup(getUserId());
        stubEnableAllCapabilities();
        stubEventGridEndpoint();
        stubCountryNationalities();
        stubGetOrgainsationUnit();
        stubJudicialResults();
        stubSpiOutFlag();
        stubBailStatuses();
        stubModeOfTrialReasons();
        createMessageConsumers();
    }

    @Test
    public void shouldProcessPublicSjpResulted() throws JMSException {
        final PublicSjpResulted sjpResulted = basicSJPCaseResulted();
        publicSjpResultedShared(sjpResulted);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());
        verifyPrivateEvents();
        verifyInPublicTopic();
    }

    @Test
    public void shouldProcessPublicSjpResultedSpiOutFalse() throws JMSException {
        final PublicSjpResulted sjpResulted = basicSJPCaseResulted();
        sjpResulted.getCases().get(0).setProsecutionAuthorityCode(PROSECUTOR_WITH_SPI_OUT_FALSE);
        publicSjpResultedShared(sjpResulted);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());
        verifyPrivateEvents(false);
        verifyNotInPublicTopic();
    }

    @Test
    public void testCCForSpiOut() throws JMSException {
        final PublicHearingResulted resultsMessage = basicShareResultsTemplate();

        hearingResultsHaveBeenShared(resultsMessage);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());

        LocalDate startDate = resultsMessage.getHearing().getHearingDays().get(0).getSittingDay().toLocalDate();
        startDate = of(startDate.getYear(), startDate.getMonth(), startDate.getDayOfMonth() - 1);

        getSummariesByDate(startDate);
        verifyPrivateEvents();
        verifyInPublicTopic();
    }

    @Test
    public void testCCForSpiOutFalse() throws JMSException {
        final PublicHearingResulted resultsMessage = basicShareResultsTemplate();
        resultsMessage.getHearing().getProsecutionCases().get(0).getProsecutionCaseIdentifier().setProsecutionAuthorityCode(PROSECUTOR_WITH_SPI_OUT_FALSE);
        resultsMessage.getHearing().getProsecutionCases().get(1).getProsecutionCaseIdentifier().setProsecutionAuthorityCode(PROSECUTOR_WITH_SPI_OUT_FALSE);
        hearingResultsHaveBeenShared(resultsMessage);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());

        LocalDate startDate = resultsMessage.getHearing().getHearingDays().get(0).getSittingDay().toLocalDate();
        startDate = of(startDate.getYear(), startDate.getMonth(), startDate.getDayOfMonth() - 1);

        getSummariesByDate(startDate);
        verifyPrivateEvents(false);
        verifyNotInPublicTopic();
    }

    @Test
    public void testCCForRejectedEvent() throws JMSException {
        final PublicHearingResulted resultsMessage = basicShareResultsTemplateWithoutResult();

        hearingResultsHaveBeenShared(resultsMessage);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());

        LocalDate startDate = resultsMessage.getHearing().getHearingDays().get(0).getSittingDay().toLocalDate();
        startDate = of(startDate.getYear(), startDate.getMonth(), startDate.getDayOfMonth() - 1);

        getSummariesByDate(startDate);
        verifyPrivateEventsForRejected();
    }

    @Test
    public void testCCForUpdatedEvent() throws JMSException {
        final UUID hearingId = randomUUID();
        final JsonObject payload = getPayload(TEMPLATE_PAYLOAD, hearingId);

        hearingResultsHaveBeenShared(payload);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());

        final LocalDate startDate = of(2019, 5, 25);

        getSummariesByDate(startDate);
        verifyPrivateEvents();
        verifyInPublicTopic();

        final JsonObject amendedPayload = getPayload(TEMPLATE_PAYLOAD_1, hearingId);
        hearingResultsHaveBeenShared(amendedPayload);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());

        getSummariesByDate(startDate);
        verifyPrivateEventsForAmendment();
        verifyInPublicTopic();

    }

    @Test
    public void testSjpForRejectedCaseEvent() throws JMSException {

        final PublicSjpResulted sjpResulted = basicSJPCaseResulted();
        publicSjpResultedShared(sjpResulted);

        verifyPrivateEvents();
        verifyInPublicTopic();

        publicSjpResultedShared(sjpResulted);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());

        verifyPrivateEventsForCaseRejectedSjp();
    }

    @Test
    public void testGeneratePoliceResultsForDefendantCC() throws JMSException {
        final PublicHearingResulted resultsMessage = basicShareResultsTemplate();

        hearingResultsHaveBeenShared(resultsMessage);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());
        verifyPrivateEvents();
        verifyInPublicTopic();

        final JsonObject payload = Json.createObjectBuilder()
                .add(SESSION_ID, resultsMessage.getHearing().getId().toString())
                .add(CASE_ID, CASE_ID_VALUE)
                .add(DEFENDANT_ID, DEFENDANT_ID_VALUE)
                .build();

        sendGeneratePoliceResultsForADefendantCommand(payload);

        verifyPrivateEventsForPoliceGenerateResultsForDefendant();
        verifyInPublicTopic();
    }

    @Test
    public void testGeneratePoliceResultsForDefendantCCWhenSpiOutFalse() throws JMSException {
        final PublicHearingResulted resultsMessage = basicShareResultsTemplate();
        resultsMessage.getHearing().getProsecutionCases().get(0).getProsecutionCaseIdentifier().setProsecutionAuthorityCode(PROSECUTOR_WITH_SPI_OUT_FALSE);
        resultsMessage.getHearing().getProsecutionCases().get(1).getProsecutionCaseIdentifier().setProsecutionAuthorityCode(PROSECUTOR_WITH_SPI_OUT_FALSE);
        hearingResultsHaveBeenShared(resultsMessage);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());
        verifyPrivateEvents(false);
        verifyNotInPublicTopic();

        final JsonObject payload = Json.createObjectBuilder()
                .add(SESSION_ID, resultsMessage.getHearing().getId().toString())
                .add(CASE_ID, CASE_ID_VALUE)
                .add(DEFENDANT_ID, DEFENDANT_ID_VALUE)
                .build();

        sendGeneratePoliceResultsForADefendantCommand(payload);

        verifyPrivateEventsForPoliceGenerateResultsForDefendant(false);
        verifyNotInPublicTopic();
    }

    @Test
    public void testGeneratePoliceResultsForDefendantSJP() throws JMSException {
        final PublicSjpResulted sjpResulted = basicSJPCaseResulted();

        publicSjpResultedShared(sjpResulted);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());
        verifyPrivateEvents();
        verifyInPublicTopic();

        final JsonObject payload = Json.createObjectBuilder()
                .add(SESSION_ID, sjpResulted.getSession().getSessionId().toString())
                .add(CASE_ID, CASE_ID_VALUE)
                .add(DEFENDANT_ID, DEFENDANT_ID_VALUE)
                .build();

        sendGeneratePoliceResultsForADefendantCommand(payload);

        verifyPrivateEventsForPoliceGenerateResultsForDefendant();
        verifyInPublicTopic();
    }

    @Test
    public void testGeneratePoliceResultsForDefendantSJPWhenSpiOutFalse() throws JMSException {
        final PublicSjpResulted sjpResulted = basicSJPCaseResulted();
        sjpResulted.getCases().get(0).setProsecutionAuthorityCode(PROSECUTOR_WITH_SPI_OUT_FALSE);
        publicSjpResultedShared(sjpResulted);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());
        verifyPrivateEvents(false);
        verifyNotInPublicTopic();

        final JsonObject payload = Json.createObjectBuilder()
                .add(SESSION_ID, sjpResulted.getSession().getSessionId().toString())
                .add(CASE_ID, CASE_ID_VALUE)
                .add(DEFENDANT_ID, DEFENDANT_ID_VALUE)
                .build();

        sendGeneratePoliceResultsForADefendantCommand(payload);

        verifyPrivateEventsForPoliceGenerateResultsForDefendant(false);
        verifyNotInPublicTopic();
    }

    @Test
    public void journeyHearingToResults() throws JMSException {
        final PublicHearingResulted resultsMessage = basicShareResultsTemplate();

        final Hearing hearingIn = resultsMessage.getHearing();
        final UUID defendantId0 =
                resultsMessage.getHearing().getProsecutionCases().get(0).getDefendants().get(0).getId();

        //share results
        hearingResultsHaveBeenShared(resultsMessage);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());


        LocalDate startDate = resultsMessage.getHearing().getHearingDays().get(0).getSittingDay().toLocalDate();
        startDate = of(startDate.getYear(), startDate.getMonth(), startDate.getDayOfMonth() - 1);

        //search summaries
        HearingResultSummariesView summaries;

        final Function<HearingResultSummariesView, List<HearingResultSummaryView>> resultsFilter =
                summs -> summs.getResults().stream().filter(sum -> sum.getHearingId().equals(hearingIn.getId()))
                        .collect(Collectors.toList());


        final long defendantCount = hearingIn.getProsecutionCases().stream().flatMap(c -> c.getDefendants().stream()).count();

        final BeanMatcher<HearingResultSummariesView> summaryCheck = isBean(HearingResultSummariesView.class)
                .withValue(summs -> resultsFilter.apply(summs).size(),
                        (int) hearingIn.getProsecutionCases().stream().flatMap(pc -> pc.getDefendants().stream()).count())
                .with(summs -> resultsFilter.apply(summs),
                        hasItem(isBean(HearingResultSummaryView.class)
                                .withValue(HearingResultSummaryView::getHearingId, hearingIn.getId())
                                .withValue(HearingResultSummaryView::getHearingType, hearingIn.getType().getDescription())
                        )
                );

        final LocalDate searchStartDate = startDate;
        Queries.pollForMatch(15, 500, () -> getSummariesByDate(searchStartDate), summaryCheck);

        final LocalDate earlierDate = hearingIn.getHearingDays().stream().map(hd -> hd.getSittingDay().toLocalDate()).min((a, b) -> a.compareTo(b)).orElse(null).minusDays(1);
        final LocalDate laterDate = hearingIn.getHearingDays().stream().map(hd -> hd.getSittingDay().toLocalDate()).max((a, b) -> a.compareTo(b)).orElse(null).plusDays(1);

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

        Matcher<HearingResultsAdded> matcherStatus = null;

        matcherStatus = isBean(HearingResultsAdded.class)
                .with(HearingResultsAdded::getHearing, isBean(Hearing.class)
                        .withValue(Hearing::getId, resultsMessage.getHearing().getId())
                        .withValue(Hearing::getCourtApplications, resultsMessage.getHearing().getCourtApplications())
                        .withValue(hearing -> hearing.getProsecutionCases().get(0).getDefendants().get(0).getPersonDefendant().getPersonDetails().getTitle(),"Baroness")
                );

        // get the details and check
        getHearingDetails(resultsMessage.getHearing().getId(), defendantId0, matcherStatus);
    }

    @Test
    public void outOfOrderJourney() {
        final PublicHearingResulted resultsMessage = basicShareResultsTemplate();

        final Hearing hearingIn = resultsMessage.getHearing();
        final UUID defendantId0 =
                resultsMessage.getHearing().getProsecutionCases().get(0).getDefendants().get(0).getId();


        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());
        hearingResultsHaveBeenShared(resultsMessage);


        LocalDate startDate = resultsMessage.getHearing().getHearingDays().get(0).getSittingDay().toLocalDate();
        startDate = of(startDate.getYear(), startDate.getMonth(), startDate.getDayOfMonth() - 1);
        //search summaries
        final HearingResultSummariesView summaries = getSummariesByDate(startDate);

        final Function<HearingResultSummariesView, List<HearingResultSummaryView>> resultsFilter =
                summs -> summs.getResults().stream().filter(sum -> sum.getHearingId().equals(hearingIn.getId()))
                        .collect(Collectors.toList());

        final Matcher<HearingResultSummariesView> summaryCheck = isBean(HearingResultSummariesView.class)
                .withValue(summs -> resultsFilter.apply(summs).size(),
                        (int) hearingIn.getProsecutionCases().stream().flatMap(pc -> pc.getDefendants().stream()).count())
                .with(summs -> resultsFilter.apply(summs),
                        hasItem(isBean(HearingResultSummaryView.class)
                                .withValue(HearingResultSummaryView::getHearingId, hearingIn.getId())
                                .withValue(HearingResultSummaryView::getHearingType, hearingIn.getType().getDescription())
                        )
                );
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
        PublicHearingResulted resultsMessage = basicShareResultsTemplate();

        final Hearing hearingIn = resultsMessage.getHearing();

        hearingResultsHaveBeenShared(resultsMessage);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());
        ApiCourtCentre expectedCourtCentre =
                ApiCourtCentre.apiCourtCentre()
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

        Matcher<ApiHearing> matcher = isBean(ApiHearing.class)
                .withValue(ApiHearing::getId, hearingIn.getId())
                .withValue(ApiHearing::getCourtCentre, expectedCourtCentre);

        ResultsStepDefinitions.getHearingDetailsForHearingId(resultsMessage.getHearing().getId(), matcher);

    }
}