package uk.gov.moj.cpp.results.it;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.hearingResultsHaveBeenShared;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.thenReturnsBadRequestForResultsSummaryWithoutFromDate;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.whenPrisonAdminTriesToViewResultsForThePerson;
import static uk.gov.moj.cpp.results.it.steps.data.factory.HearingResultDataFactory.getUserId;
import static uk.gov.moj.cpp.results.it.utils.AuthorisationServiceStub.stubEnableAllCapabilities;
import static uk.gov.moj.cpp.results.it.utils.WireMockStubUtils.setupUserAsPrisonAdminGroup;
import static uk.gov.moj.cpp.results.test.TestTemplates.basicShareResultsTemplate;
import static uk.gov.moj.cpp.results.test.matchers.BeanMatcher.isBean;

import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingResultsAdded;
import uk.gov.moj.cpp.domains.results.shareresults.PublicHearingResulted;
import uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions;
import uk.gov.moj.cpp.results.it.utils.Queries;
import uk.gov.moj.cpp.results.query.view.response.HearingResultSummariesView;
import uk.gov.moj.cpp.results.query.view.response.HearingResultSummaryView;
import uk.gov.moj.cpp.results.test.matchers.BeanMatcher;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

@SuppressWarnings({"unchecked", "serial", "squid:S2925"})
public class ResultsIT {

    @Before
    public void setUp() {
        setupUserAsPrisonAdminGroup(getUserId());
        stubEnableAllCapabilities();
    }

    @Test
    public void journeyHearingToResults() {
        PublicHearingResulted resultsMessage = basicShareResultsTemplate();

        final Hearing hearingIn = resultsMessage.getHearing();
        final UUID defendantId0 =
                resultsMessage.getHearing().getProsecutionCases().get(0).getDefendants().get(0).getId();

        //share results
        hearingResultsHaveBeenShared(resultsMessage);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());

        LocalDate startDate = resultsMessage.getHearing().getHearingDays().get(0).getSittingDay().toLocalDate();
        startDate = LocalDate.of(startDate.getYear(), startDate.getMonth(), startDate.getDayOfMonth() - 1);


        //search summaries
        HearingResultSummariesView summaries = ResultsStepDefinitions.getSummariesByDate(startDate);

        Function<HearingResultSummariesView, List<HearingResultSummaryView>> resultsFilter =
                summs -> summs.getResults().stream().filter(sum -> sum.getHearingId().equals(hearingIn.getId()))
                        .collect(Collectors.toList());

        long defendantCount = hearingIn.getProsecutionCases().stream().flatMap(c->c.getDefendants().stream()).count();

        BeanMatcher<HearingResultSummariesView> summaryCheck = isBean(HearingResultSummariesView.class)
                .withValue(summs -> resultsFilter.apply(summs).size(),
                        (int) hearingIn.getProsecutionCases().stream().flatMap(pc -> pc.getDefendants().stream()).count())
                .with(summs -> resultsFilter.apply(summs),
                        hasItem(isBean(HearingResultSummaryView.class)
                                .withValue(HearingResultSummaryView::getHearingId, hearingIn.getId())
                                .withValue(HearingResultSummaryView::getHearingType, hearingIn.getType().getDescription())
                        )
                );

        final LocalDate searchStartDate = startDate;
        Queries.pollForMatch(15, 500, () -> ResultsStepDefinitions.getSummariesByDate(searchStartDate), summaryCheck);

        final LocalDate earlierDate = hearingIn.getHearingDays().stream().map(hd -> hd.getSittingDay().toLocalDate()).min((a, b) -> a.compareTo(b)).orElse(null).minusDays(1);
        final LocalDate laterDate = hearingIn.getHearingDays().stream().map(hd -> hd.getSittingDay().toLocalDate()).max((a, b) -> a.compareTo(b)).orElse(null).plusDays(1);

        //check that date filters work
        summaries = ResultsStepDefinitions.getSummariesByDate(earlierDate);
        assertThat(resultsFilter.apply(summaries).size(), is((int)defendantCount));
        summaries = ResultsStepDefinitions.getSummariesByDate(laterDate);
        assertThat(resultsFilter.apply(summaries).size(), is(0));

        //matcher to check details results
        Matcher<HearingResultsAdded> matcher = isBean(HearingResultsAdded.class)
                .with(HearingResultsAdded::getHearing, isBean(Hearing.class)
                        .withValue(Hearing::getId, hearingIn.getId())
                        .withValue(Hearing::getJurisdictionType, hearingIn.getJurisdictionType())
                        .withValue(Hearing::getType, hearingIn.getType())
                );
        // check the details from query
        ResultsStepDefinitions.getHearingDetails(resultsMessage.getHearing().getId(), defendantId0, matcher);

        Matcher<HearingResultsAdded> matcherStatus = null;

        matcherStatus = isBean(HearingResultsAdded.class)
                .with(HearingResultsAdded::getHearing, isBean(Hearing.class)
                        .withValue(Hearing::getId, resultsMessage.getHearing().getId())
                        .withValue(Hearing::getCourtApplications, resultsMessage.getHearing().getCourtApplications())
                        .withValue(hearing -> hearing.getProsecutionCases().get(0).getDefendants().get(0).getPersonDefendant().getPersonDetails().getTitle(),"Baroness")
                );

        // get the details and check
        ResultsStepDefinitions.getHearingDetails(resultsMessage.getHearing().getId(), defendantId0, matcherStatus);
    }

    @Test
    @Ignore("GPE-9197 - JIRA issue raised to fix this test case on pipeline")
    public void outOfOrderJourney() {
        PublicHearingResulted resultsMessage = basicShareResultsTemplate();

        final Hearing hearingIn = resultsMessage.getHearing();
        final UUID defendantId0 =
                resultsMessage.getHearing().getProsecutionCases().get(0).getDefendants().get(0).getId();


        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());
        hearingResultsHaveBeenShared(resultsMessage);

        LocalDate startDate = resultsMessage.getHearing().getHearingDays().get(0).getSittingDay().toLocalDate();
        startDate = LocalDate.of(startDate.getYear(), startDate.getMonth(), startDate.getDayOfMonth() - 1);

        //search summaries
        HearingResultSummariesView summaries = ResultsStepDefinitions.getSummariesByDate(startDate);

        Function<HearingResultSummariesView, List<HearingResultSummaryView>> resultsFilter =
                summs -> summs.getResults().stream().filter(sum -> sum.getHearingId().equals(hearingIn.getId()))
                        .collect(Collectors.toList());

        Matcher<HearingResultSummariesView> summaryCheck = isBean(HearingResultSummariesView.class)
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
        Matcher<HearingResultsAdded> matcher = isBean(HearingResultsAdded.class)
                .with(HearingResultsAdded::getHearing, isBean(Hearing.class)
                        .withValue(Hearing::getId, hearingIn.getId())
                        .withValue(Hearing::getJurisdictionType, hearingIn.getJurisdictionType())
                        .withValue(Hearing::getType, hearingIn.getType())
                );
        // check the details from query
        ResultsStepDefinitions.getHearingDetails(resultsMessage.getHearing().getId(), defendantId0, matcher);
    }

    @Test
    public void getHearingDetails_shouldReturnBadRequestForResultsSummaryWithoutFromDate() {
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());
        thenReturnsBadRequestForResultsSummaryWithoutFromDate();
    }
}