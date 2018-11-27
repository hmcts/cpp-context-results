package uk.gov.moj.cpp.results.it;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static uk.gov.moj.cpp.results.it.TestUtilities.makeCommand;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.hearingResultsHaveBeenShared;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.thenReturnsBadRequestForResultsSummaryWithoutFromDate;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.whenPrisonAdminTriesToViewResultsForThePerson;
import static uk.gov.moj.cpp.results.it.steps.data.factory.HearingResultDataFactory.getUserId;
import static uk.gov.moj.cpp.results.it.utils.AuthorisationServiceStub.stubEnableAllCapabilities;
import static uk.gov.moj.cpp.results.it.utils.WireMockStubUtils.setupUserAsPrisonAdminGroup;
import static uk.gov.moj.cpp.results.test.TestTemplates.basicShareResultsTemplate;
import static uk.gov.moj.cpp.results.test.matchers.BeanMatcher.isBean;
import static uk.gov.moj.cpp.results.test.matchers.ElementAtListMatcher.first;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import uk.gov.justice.json.schemas.core.SharedHearing;
import uk.gov.justice.json.schemas.core.SharedVariant;
import uk.gov.moj.cpp.domains.results.shareresults.PublicHearingResulted;
import uk.gov.moj.cpp.domains.results.shareresults.ShareResultsMessage;
import uk.gov.moj.cpp.domains.results.shareresults.SharedResultLine;
import uk.gov.moj.cpp.results.domain.event.HearingResultsAdded;
import uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions;
import uk.gov.moj.cpp.results.query.view.response.HearingResultSummariesView;
import uk.gov.moj.cpp.results.query.view.response.HearingResultSummaryView;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@SuppressWarnings({"unchecked", "serial"})
public class ResultsIT {

    @Before
    public void setUp() {
        setupUserAsPrisonAdminGroup(getUserId());
        stubEnableAllCapabilities();
    }

    @Test
    public void journey() {
        PublicHearingResulted shareResultsMessage = basicShareResultsTemplate();
        final SharedHearing hearingIn = shareResultsMessage.getHearing();
        UUID defendantId0 =
                shareResultsMessage.getHearing().getProsecutionCases().get(0).getDefendants().get(0).getId();

        final String initialStatus0 = "processing";
        final SharedVariant variant0 = shareResultsMessage.getVariants().stream()
                .filter(v -> v.getKey().getDefendantId().equals(defendantId0)).findFirst().orElseThrow(
                        () -> new RuntimeException("invalid test data - no variant for chosen defendant")
                );
        variant0.setStatus(initialStatus0);
        final UUID materialId0 = variant0.getMaterialId();

        //share results
        hearingResultsHaveBeenShared(shareResultsMessage);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());

        LocalDate startDate = shareResultsMessage.getHearing().getHearingDays().get(0).getSittingDay().toLocalDate();
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

        final LocalDate earlierDate = hearingIn.getHearingDays().stream().map(hd -> hd.getSittingDay().toLocalDate()).min((a, b) -> a.compareTo(b)).orElse(null).minusDays(1);
        final LocalDate laterDate = hearingIn.getHearingDays().stream().map(hd -> hd.getSittingDay().toLocalDate()).max((a, b) -> a.compareTo(b)).orElse(null).plusDays(1);

        //check that date filters work
        summaries = ResultsStepDefinitions.getSummariesByDate(earlierDate);
        assertThat(resultsFilter.apply(summaries).size(), is(0));
        summaries = ResultsStepDefinitions.getSummariesByDate(laterDate);
        assertThat(resultsFilter.apply(summaries).size(), is(0));

        //matcher to check details results
        Matcher<HearingResultsAdded> matcher = isBean(HearingResultsAdded.class)
                .with(HearingResultsAdded::getHearing, isBean(SharedHearing.class)
                        .withValue(SharedHearing::getId, hearingIn.getId())
                        .withValue(SharedHearing::getJurisdictionType, hearingIn.getJurisdictionType())
                        .withValue(SharedHearing::getType, hearingIn.getType())
                );
        // check the details from query
        ResultsStepDefinitions.getHearingDetails(shareResultsMessage.getHearing().getId(), defendantId0, matcher);

        final String newStatusValue0 = "generated";
        makeCommand("results.update-nows-material-status")
                .ofType("application/vnd.results.update-nows-material-status+json")
                .withArgs(hearingIn.getId(), materialId0).withPayload(new HashMap<String, String>() {{
            put("status", newStatusValue0);
        }}).executeSuccessfully();

        Matcher<HearingResultsAdded> matcherStatus = isBean(HearingResultsAdded.class)
                .with(HearingResultsAdded::getVariants, hasItem(isBean(SharedVariant.class)
                        .withValue(SharedVariant::getMaterialId, materialId0)
                        .withValue(SharedVariant::getStatus, newStatusValue0)
                ));
        // get the details and check
        ResultsStepDefinitions.getHearingDetails(shareResultsMessage.getHearing().getId(), defendantId0, matcherStatus);

        //make a check
        shareResultsMessage.setVariants(asList(variant0));

        hearingResultsHaveBeenShared(shareResultsMessage);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());

        //checking modification
        matcher = isBean(HearingResultsAdded.class)
                .withValue(hrs -> hrs.getVariants().size(), 1)
                .with(HearingResultsAdded::getVariants, first(isBean(SharedVariant.class)
                        .withValue(SharedVariant::getMaterialId, variant0.getMaterialId())
                ))
                .with(HearingResultsAdded::getHearing, isBean(SharedHearing.class)
                        .withValue(SharedHearing::getId, hearingIn.getId())
                        .withValue(SharedHearing::getJurisdictionType, hearingIn.getJurisdictionType())
                        .withValue(SharedHearing::getType, hearingIn.getType())
                );
        ResultsStepDefinitions.getHearingDetails(shareResultsMessage.getHearing().getId(), defendantId0, matcher);


    }


    @Test
    public void shouldReplacePreviousHearingResults() throws InterruptedException {

        //TODO GPE-6220
        // 1st hearing results sharing message
        /*final ShareResultsMessage firstShareResultsMessage = basicShareResultsTemplate();

        hearingResultsHaveBeenShared(firstShareResultsMessage);

        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());

        with(firstShareResultsMessage.getHearing().getDefendants().get(0), defendant -> {
            thenPersonDetailsAreAsExpected(firstShareResultsMessage.getHearing().getId(), defendant.getPerson());
            thenHearingDetailsWithVariantsAreAsExpected(firstShareResultsMessage.getHearing(), defendant.getPerson().getId(), defendant.getId(), firstShareResultsMessage.getVariants());
            thenResultDetailsAreAsExpected(firstShareResultsMessage, defendant);
        });

        TimeUnit.SECONDS.sleep(5);

        // 2nd hearing results sharing message
        final ShareResultsMessage secondShareResultsMessage = with(firstShareResultsMessage, template -> {

            final UUID caseId = randomUUID();
            final UUID defendantId = randomUUID();
            final UUID offenceId = randomUUID();
            final UUID personId = randomUUID();

            template.setHearing(firstShareResultsMessage.getHearing());
            template.getHearing().getDefendants().add(defendantTemplate(caseId, defendantId, offenceId, personId));
            template.getHearing().getAttendees().add(defenceAdvocateTemplate(defendantId));

            template.getHearing().getSharedResultLines().addAll(asList(
                    sharedResultLineTemplate(caseId, defendantId, offenceId, "OFFENCE"),
                    sharedResultLineTemplate(caseId, defendantId, offenceId, "DEFENDANT"),
                    sharedResultLineTemplate(caseId, defendantId, offenceId, "CASE")
            ));

            template.setSharedTime(ZonedDateTime.now(ZoneId.of("UTC")));
        });

        hearingResultsHaveBeenShared(secondShareResultsMessage);

        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());

        with(secondShareResultsMessage.getHearing().getDefendants().get(0), defendant -> {
            thenPersonDetailsAreAsExpected(secondShareResultsMessage.getHearing().getId(), defendant.getPerson());
            thenHearingDetailsAreAsExpected(secondShareResultsMessage.getHearing(), defendant.getPerson().getId(), defendant.getId());
            thenResultDetailsAreAsExpected(secondShareResultsMessage, defendant);
        });

        with(secondShareResultsMessage.getHearing().getDefendants().get(1), defendant -> {
            thenPersonDetailsAreAsExpected(secondShareResultsMessage.getHearing().getId(), defendant.getPerson());
            thenHearingDetailsAreAsExpected(secondShareResultsMessage.getHearing(), defendant.getPerson().getId(), defendant.getId());
            thenResultDetailsAreAsExpected(secondShareResultsMessage, defendant);
        });*/
    }

    @Test
    public void storeHearingResults_shouldAmend() {
        //TODO GPE-6220
        /*ShareResultsMessage shareResultsMessage = basicShareResultsTemplate();

        hearingResultsHaveBeenShared(shareResultsMessage);

        Defendant defendant = shareResultsMessage.getHearing().getDefendants().get(0);

        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());
        thenPersonDetailsAreAsExpected(shareResultsMessage.getHearing().getId(), defendant.getPerson());
        thenHearingDetailsAreAsExpected(shareResultsMessage.getHearing(), defendant.getPerson().getId(), defendant.getId());
        thenResultDetailsAreAsExpected(shareResultsMessage, defendant);

        ShareResultsMessage alteredResultsMessage = with(basicShareResultsTemplate(), template -> {
            UUID hearingId = shareResultsMessage.getHearing().getId();
            UUID caseId = shareResultsMessage.getHearing().getDefendants().get(0).getCases().get(0).getId();
            UUID defendantId = shareResultsMessage.getHearing().getDefendants().get(0).getId();
            UUID personId = shareResultsMessage.getHearing().getDefendants().get(0).getPerson().getId();
            UUID offenceId = shareResultsMessage.getHearing().getDefendants().get(0).getCases().get(0).getOffences().get(0).getId();

            template.getHearing().setId(hearingId);
            template.getHearing().getDefendants().get(0).setId(defendantId);
            template.getHearing().getDefendants().get(0).getPerson().setId(personId);
            template.getHearing().getDefendants().get(0).getCases().get(0).setId(caseId);
            template.getHearing().getDefendants().get(0).getCases().get(0).getOffences().get(0).setId(offenceId);

            template.getHearing().getSharedResultLines().forEach(sharedResultLine -> sharedResultLine
                    .setCaseId(caseId)
                    .setDefendantId(defendantId)
                    .setOffenceId(offenceId));

            template.getHearing().getAttendees().stream()
                    .filter(a -> a.getType().equals("DEFENCEADVOCATE"))
                    .map(DefenceAdvocate.class::cast)
                    .forEach(da -> da.setDefendantIds(Collections.singletonList(defendantId)));

            template.setVariants(shareResultsMessage.getVariants());
            findSharedResultOfLevel(template, "CASE");
            findSharedResultOfLevel(template, "DEFENDANT");
            findSharedResultOfLevel(template, "OFFENCE");
        });


        hearingResultsHaveBeenShared(alteredResultsMessage);

        defendant = alteredResultsMessage.getHearing().getDefendants().get(0);

        //HearingDefendant details and hearing details do not update - they should have the old values from the first publish.
        thenPersonDetailsAreAsExpected(shareResultsMessage.getHearing().getId(), defendant.getPerson());

        thenHearingDetailsAreAsExpected(alteredResultsMessage.getHearing(), defendant.getPerson().getId(), defendant.getId());

        thenResultDetailsAreAsExpected(alteredResultsMessage, alteredResultsMessage.getHearing().getDefendants().get(0));*/
    }

    @Test
    public void getHearingDetails_shouldDisplayForAuthorisedUserGroups() {
        //TODO GPE-6220
        /*ShareResultsMessage shareResultsMessage = basicShareResultsTemplate();

        hearingResultsHaveBeenShared(shareResultsMessage);

        Defendant defendant = shareResultsMessage.getHearing().getDefendants().get(0);

        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());
        thenPersonDetailsAreAsExpected(shareResultsMessage.getHearing().getId(), defendant.getPerson());
        thenHearingDetailsAreAsExpected(shareResultsMessage.getHearing(), defendant.getPerson().getId(), defendant.getId());
        thenResultDetailsAreAsExpected(shareResultsMessage, defendant);

        whenProbationAdminTriesToViewResultsForThePerson(getUserId());
        thenPersonDetailsAreAsExpected(shareResultsMessage.getHearing().getId(), defendant.getPerson());
        thenHearingDetailsAreAsExpected(shareResultsMessage.getHearing(), defendant.getPerson().getId(), defendant.getId());
        thenResultDetailsAreAsExpected(shareResultsMessage, defendant);

        whenPoliceAdminTriesToViewResultsForThePerson(getUserId());
        thenPersonDetailsAreAsExpected(shareResultsMessage.getHearing().getId(), defendant.getPerson());
        thenHearingDetailsAreAsExpected(shareResultsMessage.getHearing(), defendant.getPerson().getId(), defendant.getId());
        thenResultDetailsAreAsExpected(shareResultsMessage, defendant);

        whenVictimsWitnessCareAdminTriesToViewResultsForThePerson(getUserId());
        thenPersonDetailsAreAsExpected(shareResultsMessage.getHearing().getId(), defendant.getPerson());
        thenHearingDetailsAreAsExpected(shareResultsMessage.getHearing(), defendant.getPerson().getId(), defendant.getId());
        thenResultDetailsAreAsExpected(shareResultsMessage, defendant);

        whenYouthOffendingServiceAdminTriesToViewResultsForThePerson(getUserId());
        thenPersonDetailsAreAsExpected(shareResultsMessage.getHearing().getId(), defendant.getPerson());
        thenHearingDetailsAreAsExpected(shareResultsMessage.getHearing(), defendant.getPerson().getId(), defendant.getId());
        thenResultDetailsAreAsExpected(shareResultsMessage, defendant);

        whenLegalAidAgencyAdminTriesToViewResultsForThePerson(getUserId());
        thenPersonDetailsAreAsExpected(shareResultsMessage.getHearing().getId(), defendant.getPerson());
        thenHearingDetailsAreAsExpected(shareResultsMessage.getHearing(), defendant.getPerson().getId(), defendant.getId());
        thenResultDetailsAreAsExpected(shareResultsMessage, defendant);

        whenUnauthorisedUserTriesToViewResults(getUserId());
        thenTheRequestIsForbidden(defendant.getPerson().getId(), shareResultsMessage.getHearing().getId());*/
    }

    @Test
    public void getHearingDetails_shouldReturnBadRequestForResultsSummaryWithoutFromDate() {
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());
        thenReturnsBadRequestForResultsSummaryWithoutFromDate();
    }

    @Test
    public void shouldUpdateNowsMaterialStatusToGenerated() throws InterruptedException {

        //TODO GPE-6220
        final PublicHearingResulted shareResultsMessage = basicShareResultsTemplate();

        hearingResultsHaveBeenShared(shareResultsMessage);

        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());

        final UUID hearingId = shareResultsMessage.getHearing().getId();
        final UUID materialId = shareResultsMessage.getVariants().get(0).getMaterialId();

        /*poll(requestParams(getUrl("results.get-person-hearing-result-details", personId, hearingId),
                "application/vnd.results.hearing-details+json").withHeader(USER_ID, getUserId()))
        .until(
                print(),
                status().is(OK), 
                payload().isJson(allOf(
                        withJsonPath("$.id", is(hearingId.toString())),
                        withJsonPath("$.variants.[0].materialId", is(materialId.toString())),
                        withJsonPath("$.variants.[0].status", equalToIgnoringCase("building"))))
        );*/

/*        final EventListener nowsMaterialStatusUpdatedEventListener = listenFor("public.results.event.nows-material-status-updated")
                .withFilter(isJson(allOf(
                        withJsonPath("$._metadata.name", is("public.results.event.nows-material-status-updated")),
                        withJsonPath("$._metadata.context.user", is(USER_ID_VALUE.toString())),
                        withJsonPath("$.hearingId", is(hearingId.toString())),
                        withJsonPath("$.materialId", is(materialId.toString())),

                        withJsonPath("$.status", is("generated"))
                )));*/

        makeCommand("results.update-nows-material-status")
                .ofType("application/vnd.results.update-nows-material-status+json")
                .withArgs(hearingId, materialId).withPayload(new HashMap<String, String>() {{
            put("status", "generated");
        }}).executeSuccessfully();

        //nowsMaterialStatusUpdatedEventListener.waitFor();

        /*poll(requestParams(getUrl("results.get-person-hearing-result-details", personId, hearingId),
                "application/vnd.results.hearing-details+json").withHeader(USER_ID, getUserId()))
        .until(
                print(),
                status().is(OK), 
                payload().isJson(allOf(
                        withJsonPath("$.id", is(hearingId.toString())),
                        withJsonPath("$.variants.[0].materialId", is(materialId.toString())),
                        withJsonPath("$.variants.[0].status", equalToIgnoringCase("generated"))))
        );*/
    }

    private SharedResultLine findSharedResultOfLevel(ShareResultsMessage template, String level) {
        return template.getHearing().getSharedResultLines().stream()
                .filter(rl -> rl.getLevel().equals(level))
                .findFirst().orElse(null);
    }
}