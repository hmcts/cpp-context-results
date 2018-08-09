package uk.gov.moj.cpp.results.it;

import static uk.gov.moj.cpp.results.it.TestUtilities.getUrl;
import static uk.gov.moj.cpp.results.it.TestUtilities.print;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.text.IsEqualIgnoringCase.equalToIgnoringCase;
import static org.hamcrest.Matchers.is;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.results.it.TestUtilities.USER_ID_VALUE;
import static uk.gov.moj.cpp.results.it.TestUtilities.listenFor;
import static uk.gov.moj.cpp.results.it.TestUtilities.makeCommand;
import static uk.gov.moj.cpp.results.it.TestUtilities.with;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.andResultsSummaryShowsExpectedDetails;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.getExistingResultSummary;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.hearingResultsHaveBeenShared;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.thenHearingDetailsAreAsExpected;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.thenHearingDetailsAreAsExpected_givenNoAdvocates;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.thenHearingDetailsWithVariantsAreAsExpected;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.thenPersonDetailsAreAsExpected;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.thenPersonDetailsAreAsExpected_withLimitedData;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.thenResultDetailsAreAsExpected;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.thenResultDetailsAreAsExpected_givenNoPlea;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.thenResultsSummaryShowsHearingWithinDateRange;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.thenReturnsBadRequestForResultsSummaryWithoutFromDate;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.thenTheRequestIsForbidden;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.whenLegalAidAgencyAdminTriesToViewResultsForThePerson;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.whenPoliceAdminTriesToViewResultsForThePerson;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.whenPrisonAdminTriesToViewResultsForThePerson;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.whenProbationAdminTriesToViewResultsForThePerson;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.whenUnauthorisedUserTriesToViewResults;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.whenVictimsWitnessCareAdminTriesToViewResultsForThePerson;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.whenYouthOffendingServiceAdminTriesToViewResultsForThePerson;
import static uk.gov.moj.cpp.results.it.steps.data.factory.HearingResultDataFactory.defenceAdvocateTemplate;
import static uk.gov.moj.cpp.results.it.steps.data.factory.HearingResultDataFactory.defendantTemplate;
import static uk.gov.moj.cpp.results.it.steps.data.factory.HearingResultDataFactory.getUserId;
import static uk.gov.moj.cpp.results.it.steps.data.factory.HearingResultDataFactory.sharedResultLineTemplate;
import static uk.gov.moj.cpp.results.it.utils.AuthorisationServiceStub.stubEnableAllCapabilities;
import static uk.gov.moj.cpp.results.it.utils.WireMockStubUtils.setupUserAsPrisonAdminGroup;
import static uk.gov.moj.cpp.results.test.TestTemplates.basicShareResultsTemplate;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Test;

import uk.gov.moj.cpp.domains.results.shareResults.Attendee;
import uk.gov.moj.cpp.domains.results.shareResults.DefenceAdvocate;
import uk.gov.moj.cpp.domains.results.shareResults.Defendant;
import uk.gov.moj.cpp.domains.results.shareResults.ShareResultsMessage;
import uk.gov.moj.cpp.domains.results.shareResults.SharedResultLine;
import uk.gov.moj.cpp.results.it.TestUtilities.EventListener;

@SuppressWarnings({"unchecked", "serial"})
public class ResultsIT {

    @Before
    public void setUp() {
        setupUserAsPrisonAdminGroup(getUserId());
        stubEnableAllCapabilities();
    }

    @Test
    public void shouldStoreHearingResults() {

        ShareResultsMessage shareResultsMessage = basicShareResultsTemplate();

        hearingResultsHaveBeenShared(shareResultsMessage);

        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());

        Defendant defendant = shareResultsMessage.getHearing().getDefendants().get(0);

        thenPersonDetailsAreAsExpected(shareResultsMessage.getHearing().getId(), defendant.getPerson());

        thenHearingDetailsWithVariantsAreAsExpected(shareResultsMessage.getHearing(), defendant.getPerson().getId(), defendant.getId(), shareResultsMessage.getVariants());

        thenResultDetailsAreAsExpected(shareResultsMessage, defendant);
    }

    @Test
    public void shouldReplacePreviousHearingResults() throws InterruptedException {

        // 1st hearing results sharing message
        final ShareResultsMessage firstShareResultsMessage = basicShareResultsTemplate();

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
        });
    }

    @Test
    public void shouldStoreHearingResults_givenNoPlea() {

        ShareResultsMessage shareResultsMessage = with(basicShareResultsTemplate(), template -> template.getHearing()
                .getDefendants().get(0)
                .getCases().get(0)
                .getOffences().get(0)
                .setPlea(null));

        hearingResultsHaveBeenShared(shareResultsMessage);

        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());

        Defendant defendant = shareResultsMessage.getHearing().getDefendants().get(0);

        thenPersonDetailsAreAsExpected(shareResultsMessage.getHearing().getId(), defendant.getPerson());

        thenHearingDetailsAreAsExpected(shareResultsMessage.getHearing(), defendant.getPerson().getId(), defendant.getId());

        thenResultDetailsAreAsExpected_givenNoPlea(shareResultsMessage);
    }

    @Test
    public void shouldStoreHearingResults_givenNoAdvocates() {

        ShareResultsMessage shareResultsMessage = with(basicShareResultsTemplate(), template -> {
            List<Attendee> attendees = template.getHearing().getAttendees();

            attendees.removeIf(attendee -> attendee.getType().equals("DEFENCEADVOCATE"));
            attendees.removeIf(attendee -> attendee.getType().equals("PROSECUTIONADVOCATE"));
        });

        hearingResultsHaveBeenShared(shareResultsMessage);

        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());

        Defendant defendant = shareResultsMessage.getHearing().getDefendants().get(0);

        thenPersonDetailsAreAsExpected(shareResultsMessage.getHearing().getId(), defendant.getPerson());

        thenHearingDetailsAreAsExpected_givenNoAdvocates(shareResultsMessage);

        thenResultDetailsAreAsExpected(shareResultsMessage, defendant);
    }

    @Test
    public void shouldStoreHearingResults_givenTwoDefendants_defenceAdvocatesShouldAssociateCorrectly() {

        ShareResultsMessage shareResultsMessage = with(basicShareResultsTemplate(), template -> {
            UUID caseId = randomUUID();
            UUID defendantId = randomUUID();
            UUID offenceId = randomUUID();
            UUID personId = randomUUID();

            template.getHearing().getDefendants().add(defendantTemplate(caseId, defendantId, offenceId, personId));
            template.getHearing().getAttendees().add(defenceAdvocateTemplate(defendantId));

            template.getHearing().getSharedResultLines().addAll(asList(
                    sharedResultLineTemplate(caseId, defendantId, offenceId, "OFFENCE"),
                    sharedResultLineTemplate(caseId, defendantId, offenceId, "DEFENDANT"),
                    sharedResultLineTemplate(caseId, defendantId, offenceId, "CASE")
            ));
        });

        hearingResultsHaveBeenShared(shareResultsMessage);

        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());

        with(shareResultsMessage.getHearing().getDefendants().get(0), defendant -> {
            thenPersonDetailsAreAsExpected(shareResultsMessage.getHearing().getId(), defendant.getPerson());

            thenHearingDetailsWithVariantsAreAsExpected(shareResultsMessage.getHearing(), defendant.getPerson().getId(), defendant.getId(), shareResultsMessage.getVariants());

            thenResultDetailsAreAsExpected(shareResultsMessage, defendant);
        });

        with(shareResultsMessage.getHearing().getDefendants().get(1), defendant -> {
            thenPersonDetailsAreAsExpected(shareResultsMessage.getHearing().getId(), defendant.getPerson());

            thenHearingDetailsAreAsExpected(shareResultsMessage.getHearing(), defendant.getPerson().getId(), defendant.getId());

            thenResultDetailsAreAsExpected(shareResultsMessage, defendant);
        });

    }

    @Test
    public void shouldStoreHearingResults_givenLimitedDefendantData() {

        ShareResultsMessage shareResultsMessage = with(basicShareResultsTemplate(), template ->
                with(template.getHearing().getDefendants().get(0).getPerson(),
                person -> {
                    person.setDateOfBirth(null);
                    with(person.getAddress(), address ->
                            address.setAddress2(null)
                                    .setAddress3(null)
                                    .setAddress4(null)
                                    .setPostCode(null));
                }));

        hearingResultsHaveBeenShared(shareResultsMessage);

        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());

        Defendant defendant = shareResultsMessage.getHearing().getDefendants().get(0);

        thenPersonDetailsAreAsExpected_withLimitedData(shareResultsMessage.getHearing().getId(), defendant.getPerson());
    }

    @Test
    public void storeHearingResults_shouldAmend() {
        ShareResultsMessage shareResultsMessage = basicShareResultsTemplate();

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

        //Defendant details and hearing details do not update - they should have the old values from the first publish.
        thenPersonDetailsAreAsExpected(shareResultsMessage.getHearing().getId(), defendant.getPerson());

        thenHearingDetailsAreAsExpected(alteredResultsMessage.getHearing(), defendant.getPerson().getId(), defendant.getId());

        thenResultDetailsAreAsExpected(alteredResultsMessage, alteredResultsMessage.getHearing().getDefendants().get(0));
    }

    @Test
    public void getSummaryResults_shouldReturnSummaryResults() {

        ZonedDateTime referenceDate = ZonedDateTime.now();

        ShareResultsMessage shareResultsMessage = with(basicShareResultsTemplate(), template -> template.getHearing().setStartDateTime(referenceDate));

        final JsonObject existingResultSummary = getExistingResultSummary(referenceDate.toLocalDate(), getUserId());

        hearingResultsHaveBeenShared(shareResultsMessage);

        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());

        thenResultsSummaryShowsHearingWithinDateRange(referenceDate.toLocalDate());

        andResultsSummaryShowsExpectedDetails(referenceDate.toLocalDate(), shareResultsMessage, existingResultSummary);
    }

    @Test
    public void getHearingDetails_shouldDisplayForAuthorisedUserGroups() {
        ShareResultsMessage shareResultsMessage = basicShareResultsTemplate();

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
        thenTheRequestIsForbidden(defendant.getPerson().getId(), shareResultsMessage.getHearing().getId());
    }

    @Test
    public void getHearingDetails_shouldReturnBadRequestForResultsSummaryWithoutFromDate() {
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());
        thenReturnsBadRequestForResultsSummaryWithoutFromDate();
    }

    @Test
    public void shouldUpdateNowsMaterialStatusToGenerated() {

        final ShareResultsMessage shareResultsMessage = basicShareResultsTemplate();

        hearingResultsHaveBeenShared(shareResultsMessage);

        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());

        final UUID hearingId = shareResultsMessage.getHearing().getId();
        final UUID materialId = shareResultsMessage.getVariants().get(0).getMaterialId();
        final UUID personId = shareResultsMessage.getHearing().getDefendants().get(0).getPerson().getId();

        poll(requestParams(getUrl("results.get-person-hearing-result-details", personId, hearingId),
                "application/vnd.results.hearing-details+json").withHeader(USER_ID, getUserId()))
        .until(
                print(),
                status().is(OK), 
                payload().isJson(allOf(
                        withJsonPath("$.id", is(hearingId.toString())),
                        withJsonPath("$.variants.[0].materialId", is(materialId.toString())),
                        withJsonPath("$.variants.[0].status", equalToIgnoringCase("building"))))
        );

        final EventListener nowsMaterialStatusUpdatedEventListener = listenFor("public.results.event.nows-material-status-updated")
                .withFilter(isJson(allOf(
                        withJsonPath("$._metadata.name", is("public.results.event.nows-material-status-updated")),
                        withJsonPath("$._metadata.context.user", is(USER_ID_VALUE.toString())),
                        withJsonPath("$.hearingId", is(hearingId.toString())),
                        withJsonPath("$.materialId", is(materialId.toString())),
                        withJsonPath("$.status", is("generated"))
                )));
        
        makeCommand("results.update-nows-material-status")
                .ofType("application/vnd.results.update-nows-material-status+json")
                .withArgs(hearingId, materialId).withPayload(new HashMap<String, String>() {{
                        put("status", "generated");
                 }}).executeSuccessfully();

        nowsMaterialStatusUpdatedEventListener.waitFor();

        poll(requestParams(getUrl("results.get-person-hearing-result-details", personId, hearingId),
                "application/vnd.results.hearing-details+json").withHeader(USER_ID, getUserId()))
        .until(
                print(),
                status().is(OK), 
                payload().isJson(allOf(
                        withJsonPath("$.id", is(hearingId.toString())),
                        withJsonPath("$.variants.[0].materialId", is(materialId.toString())),
                        withJsonPath("$.variants.[0].status", equalToIgnoringCase("generated"))))
        );
    }

    private SharedResultLine findSharedResultOfLevel(ShareResultsMessage template, String level) {
        return template.getHearing().getSharedResultLines().stream()
                .filter(rl -> rl.getLevel().equals(level))
                .findFirst().orElse(null);
    }
}