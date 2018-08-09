package uk.gov.moj.cpp.results.it.steps;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.restassured.path.json.JsonPath;
import org.hamcrest.CoreMatchers;
import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder;
import uk.gov.justice.services.test.utils.core.http.ResponseData;
import uk.gov.justice.services.test.utils.core.messaging.MessageProducerClient;
import uk.gov.justice.services.test.utils.core.rest.RestClient;
import uk.gov.moj.cpp.domains.results.shareResults.*;
import uk.gov.moj.cpp.results.it.utils.WireMockStubUtils;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.lang.String.format;
import static java.time.LocalDate.parse;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.core.AllOf.allOf;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;

public class ResultsStepDefinitions extends AbstractStepDefinitions {

    private static final String CONTENT_TYPE_PERSON_DETAILS = "application/vnd.results.person-details+json";
    private static final String CONTENT_TYPE_HEARING_DETAILS = "application/vnd.results.hearing-details+json";
    private static final String CONTENT_TYPE_RESULTS_DETAILS = "application/vnd.results.results-details+json";
    private static final String CONTENT_TYPE_RESULTS_SUMMARY = "application/vnd.results.results-summary+json";

    private static final String PUBLIC_EVENT_HEARING_RESULTED = "public.hearing.resulted";

    private static final String GET_RESULTS_SUMMARY = "results.get-results-summary";

    public static void whenPrisonAdminTriesToViewResultsForThePerson(final UUID userId) {
        WireMockStubUtils.setupUserAsPrisonAdminGroup(userId);
        setLoggedInUser(userId);
    }

    public static void whenProbationAdminTriesToViewResultsForThePerson(final UUID userId) {
        WireMockStubUtils.setupUserAsProbationAdminGroup(userId);
        setLoggedInUser(userId);
    }

    public static void whenPoliceAdminTriesToViewResultsForThePerson(final UUID userId) {
        WireMockStubUtils.setupUserAsPoliceAdminGroup(userId);
        setLoggedInUser(userId);
    }

    public static void whenVictimsWitnessCareAdminTriesToViewResultsForThePerson(final UUID userId) {
        WireMockStubUtils.setupUserAsVictimsWitnessCareAdminGroup(userId);
        setLoggedInUser(userId);
    }

    public static void whenYouthOffendingServiceAdminTriesToViewResultsForThePerson(final UUID userId) {
        WireMockStubUtils.setupUserAsYouthOffendingServiceAdminGroup(userId);
        setLoggedInUser(userId);
    }

    public static void whenLegalAidAgencyAdminTriesToViewResultsForThePerson(final UUID userId) {
        WireMockStubUtils.setupUserAsLegalAidAgencyAdminGroup(userId);
        setLoggedInUser(userId);
    }

    public static void whenUnauthorisedUserTriesToViewResults(final UUID userId) {
        WireMockStubUtils.setupUserAsUnauthorisedGroup(userId);
        setLoggedInUser(userId);
    }

    public static void thenTheRequestIsForbidden(final UUID personId, final UUID hearingId) {
        final String personHearingResultDetailsUrl = format("%s%s", BASE_URI, getProperty("results.get-person-hearing-result-details", personId, hearingId));

        poll(requestParams(personHearingResultDetailsUrl, CONTENT_TYPE_HEARING_DETAILS).withHeader(USER_ID, getLoggedInUser()))
                .until(
                        status().is(FORBIDDEN)
                );
    }

    public static void thenPersonDetailsAreAsExpected(final UUID hearingId, final uk.gov.moj.cpp.domains.results.shareResults.Person person) {

        final String personHearingResultDetailsUrl = format("%s%s", BASE_URI,
                getProperty("results.get-person-hearing-result-details", person.getId(),
                        hearingId));

        poll(requestParams(personHearingResultDetailsUrl, CONTENT_TYPE_PERSON_DETAILS).withHeader(USER_ID, getLoggedInUser()))
                .until(
                        print(),
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.id", equalTo(person.getId().toString())),
                                withJsonPath("$.firstName", equalTo(person.getFirstName())),
                                withJsonPath("$.lastName", equalTo(person.getLastName())),
                                withJsonPath("$.dateOfBirth", equalTo(LocalDates.to(person.getDateOfBirth()))),
                                withJsonPath("$.address.address1", equalTo(person.getAddress().getAddress1())),
                                withJsonPath("$.address.address2", equalTo(person.getAddress().getAddress2())),
                                withJsonPath("$.address.address3", equalTo(person.getAddress().getAddress3())),
                                withJsonPath("$.address.address4", equalTo(person.getAddress().getAddress4())),
                                withJsonPath("$.address.postCode", equalTo(person.getAddress().getPostCode()))

                        )));
    }

    public static void thenPersonDetailsAreAsExpected_withLimitedData(final UUID hearingId, final uk.gov.moj.cpp.domains.results.shareResults.Person person) {


        final String personHearingResultDetailsUrl = format("%s%s", BASE_URI,
                getProperty("results.get-person-hearing-result-details", person.getId(),
                        hearingId));

        poll(requestParams(personHearingResultDetailsUrl, CONTENT_TYPE_PERSON_DETAILS).withHeader(USER_ID, getLoggedInUser()))
                .until(
                        print(),
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.id", equalTo(person.getId().toString())),
                                withJsonPath("$.firstName", equalTo(person.getFirstName())),
                                withJsonPath("$.lastName", equalTo(person.getLastName())),
                                withoutJsonPath("$.dateOfBirth"),
                                withJsonPath("$.address.address1", equalTo(person.getAddress().getAddress1())),
                                withoutJsonPath("$.address.address2"),
                                withoutJsonPath("$.address.address3"),
                                withoutJsonPath("$.address.address4"),
                                withoutJsonPath("$.address.postCode")

                        )));
    }

    public static void thenHearingDetailsAreAsExpected(final uk.gov.moj.cpp.domains.results.shareResults.Hearing hearing, final UUID personId, final UUID defendantId) {

        final String personHearingResultDetailsUrl = format("%s%s", BASE_URI,
                getProperty("results.get-person-hearing-result-details", personId,
                        hearing.getId()));

        final String judgeName = hearing.getAttendees().stream()
                .filter(a -> a.getType().equals("JUDGE"))
                .map(a -> format("%s %s",
                        a.getFirstName(),
                        a.getLastName()
                ))
                .findFirst()
                .get();

        final String prosecutionCounsel = hearing.getAttendees().stream()
                .filter(a -> a.getType().equals("PROSECUTIONADVOCATE"))
                .map(ProsecutionAdvocate.class::cast)
                .map(a -> format("%s %s %s",
                        a.getFirstName(),
                        a.getLastName(),
                        a.getStatus()
                ))
                .findFirst()
                .get();

        final String defenceCounsel = hearing.getAttendees().stream()
                .filter(a -> a.getType().equals("DEFENCEADVOCATE"))
                .map(DefenceAdvocate.class::cast)
                .filter(da -> da.getDefendantIds().contains(defendantId))
                .map(a -> format("%s %s %s",
                        a.getFirstName(),
                        a.getLastName(),
                        a.getStatus()
                ))
                .findFirst()
                .get();

        final List<SharedResultLine> sharedResultLines = hearing.getSharedResultLines();

        final List<String> courtClerkIds = sharedResultLines.stream().filter(s -> nonNull(s.getCourtClerk()) && s.getDefendantId().equals(defendantId)).map(s -> s.getCourtClerk().getId().toString()).collect(toList());

        final List<String> courtClerkFirstNames = sharedResultLines.stream().filter(s -> nonNull(s.getCourtClerk()) && s.getDefendantId().equals(defendantId)).map(s -> s.getCourtClerk().getFirstName()).collect(toList());

        final List<String> courtClerkLastNames = sharedResultLines.stream().filter(s -> nonNull(s.getCourtClerk()) && s.getDefendantId().equals(defendantId)).map(s -> s.getCourtClerk().getLastName()).collect(toList());

        poll(requestParams(personHearingResultDetailsUrl, CONTENT_TYPE_HEARING_DETAILS).withHeader(USER_ID, getLoggedInUser()))
                .until(
                        print(),
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.id", equalTo(hearing.getId().toString())),
                                withJsonPath("$.startDate", equalTo(LocalDates.to(hearing.getStartDateTime().toLocalDate()))),
                                withJsonPath("$.prosecutorName", equalTo(prosecutionCounsel)),
                                withJsonPath("$.defenceName", equalTo(defenceCounsel)),
                                withJsonPath("$.courtCentreName", equalTo(hearing.getCourtCentre().getCourtCentreName())),
                                withJsonPath("$.courtCode", equalTo("433")),
                                withJsonPath("$.judgeName", equalTo(judgeName)),
                                withJsonPath("$.clerks.[*].clerkOfTheCourtId", containsInAnyOrder(courtClerkIds.toArray(new String[courtClerkIds.size()]))),
                                withJsonPath("$.clerks.[*].clerkOfTheCourtFirstName", containsInAnyOrder(courtClerkFirstNames.toArray(new String[courtClerkFirstNames.size()]))),
                                withJsonPath("$.clerks.[*].clerkOfTheCourtLastName", containsInAnyOrder(courtClerkLastNames.toArray(new String[courtClerkLastNames.size()])))
                        )));
    }

    public static void thenHearingDetailsWithVariantsAreAsExpected(uk.gov.moj.cpp.domains.results.shareResults.Hearing hearing, UUID personId, UUID defendantId, List<Variant> variants) {

        final String personHearingResultDetailsUrl = format("%s%s", BASE_URI,
                getProperty("results.get-person-hearing-result-details", personId,
                        hearing.getId()));

        final String judgeName = hearing.getAttendees().stream()
                .filter(a -> a.getType().equals("JUDGE"))
                .map(a -> format("%s %s",
                        a.getFirstName(),
                        a.getLastName()
                ))
                .findFirst()
                .get();

        final String prosecutionCounsel = hearing.getAttendees().stream()
                .filter(a -> a.getType().equals("PROSECUTIONADVOCATE"))
                .map(ProsecutionAdvocate.class::cast)
                .map(a -> format("%s %s %s",
                        a.getFirstName(),
                        a.getLastName(),
                        a.getStatus()
                ))
                .findFirst()
                .get();

        final String defenceCounsel = hearing.getAttendees().stream()
                .filter(a -> a.getType().equals("DEFENCEADVOCATE"))
                .map(DefenceAdvocate.class::cast)
                .filter(da -> da.getDefendantIds().contains(defendantId))
                .map(a -> format("%s %s %s",
                        a.getFirstName(),
                        a.getLastName(),
                        a.getStatus()
                ))
                .findFirst()
                .get();


        final List<SharedResultLine> sharedResultLines = hearing.getSharedResultLines();

        final List<String> courtClerkIds = sharedResultLines.stream().filter(s -> nonNull(s.getCourtClerk()) && s.getDefendantId().equals(defendantId)).map(s -> s.getCourtClerk().getId().toString()).collect(toList());

        final List<String> courtClerkFirstNames = sharedResultLines.stream().filter(s -> nonNull(s.getCourtClerk()) && s.getDefendantId().equals(defendantId)).map(s -> s.getCourtClerk().getFirstName()).collect(toList());

        final List<String> courtClerkLastNames = sharedResultLines.stream().filter(s -> nonNull(s.getCourtClerk()) && s.getDefendantId().equals(defendantId)).map(s -> s.getCourtClerk().getLastName()).collect(toList());

        final List<String> materialIds = variants.stream().map(variant -> variant.getMaterialId().toString()).collect(toList());

        final List<String> statuses = variants.stream().map(variant -> variant.getStatus()).collect(toList());

        poll(requestParams(personHearingResultDetailsUrl, CONTENT_TYPE_HEARING_DETAILS).withHeader(USER_ID, getLoggedInUser()))
                .until(
                        print(),
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.id", equalTo(hearing.getId().toString())),
                                withJsonPath("$.startDate", equalTo(LocalDates.to(hearing.getStartDateTime().toLocalDate()))),
                                withJsonPath("$.prosecutorName", equalTo(prosecutionCounsel)),
                                withJsonPath("$.defenceName", equalTo(defenceCounsel)),
                                withJsonPath("$.courtCentreName", equalTo(hearing.getCourtCentre().getCourtCentreName())),
                                withJsonPath("$.courtCode", equalTo("433")),
                                withJsonPath("$.judgeName", equalTo(judgeName)),
                                withJsonPath("$.clerks.[*].clerkOfTheCourtId", containsInAnyOrder(courtClerkIds.toArray(new String[courtClerkIds.size()]))),
                                withJsonPath("$.clerks.[*].clerkOfTheCourtFirstName", containsInAnyOrder(courtClerkFirstNames.toArray(new String[courtClerkFirstNames.size()]))),
                                withJsonPath("$.clerks.[*].clerkOfTheCourtLastName", containsInAnyOrder(courtClerkLastNames.toArray(new String[courtClerkLastNames.size()]))),
                                withJsonPath("$.variants.[*].materialId", containsInAnyOrder(materialIds.toArray(new String[materialIds.size()]))),
                                withJsonPath("$.variants.[*].status", containsInAnyOrder(statuses.toArray(new String[statuses.size()])))
                        )));
    }

    public static void thenHearingDetailsAreAsExpected_givenNoAdvocates(final ShareResultsMessage shareResultsMessage) {

        final uk.gov.moj.cpp.domains.results.shareResults.Person person = shareResultsMessage.getHearing().getDefendants().get(0).getPerson();

        final String personHearingResultDetailsUrl = format("%s%s", BASE_URI,
                getProperty("results.get-person-hearing-result-details", person.getId(),
                        shareResultsMessage.getHearing().getId()));


        final String judgeName = shareResultsMessage.getHearing().getAttendees().stream()
                .filter(a -> a.getType().equals("JUDGE"))
                .map(a -> format("%s %s",
                        a.getFirstName(),
                        a.getLastName()
                ))
                .findFirst()
                .get();

        final List<SharedResultLine> sharedResultLines = shareResultsMessage.getHearing().getSharedResultLines();

        final List<String> courtClerkIds = sharedResultLines.stream().map(s -> s.getCourtClerk().getId().toString()).collect(toList());

        final List<String> courtClerkFirstNames = sharedResultLines.stream().map(s -> s.getCourtClerk().getFirstName()).collect(toList());

        final List<String> courtClerkLastNames = sharedResultLines.stream().map(s -> s.getCourtClerk().getLastName()).collect(toList());

        poll(requestParams(personHearingResultDetailsUrl, CONTENT_TYPE_HEARING_DETAILS).withHeader(USER_ID, getLoggedInUser()))
                .until(
                        print(),
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.id", equalTo(shareResultsMessage.getHearing().getId().toString())),
                                withJsonPath("$.startDate", equalTo(LocalDates.to(shareResultsMessage.getHearing().getStartDateTime().toLocalDate()))),
                                withJsonPath("$.prosecutorName", equalTo("N/A")),
                                withJsonPath("$.defenceName", equalTo("N/A")),
                                withJsonPath("$.courtCentreName", equalTo(shareResultsMessage.getHearing().getCourtCentre().getCourtCentreName())),
                                withJsonPath("$.courtCode", equalTo("433")),
                                withJsonPath("$.judgeName", equalTo(judgeName)),
                                withJsonPath("$.clerks.[*].clerkOfTheCourtId", containsInAnyOrder(courtClerkIds.toArray(new String[courtClerkIds.size()]))),
                                withJsonPath("$.clerks.[*].clerkOfTheCourtFirstName", containsInAnyOrder(courtClerkFirstNames.toArray(new String[courtClerkFirstNames.size()]))),
                                withJsonPath("$.clerks.[*].clerkOfTheCourtLastName", containsInAnyOrder(courtClerkLastNames.toArray(new String[courtClerkLastNames.size()])))
                        )));
    }

    public static void thenResultDetailsAreAsExpected(final ShareResultsMessage shareResultsMessage, final Defendant defendant) {

        final Case legalCase = defendant.getCases().get(0);

        final uk.gov.moj.cpp.domains.results.shareResults.Offence offence = legalCase.getOffences().get(0);

        final String personHearingResultDetailsUrl = format("%s%s", BASE_URI,
                getProperty("results.get-person-hearing-result-details", defendant.getPerson().getId(),
                        shareResultsMessage.getHearing().getId()));

        final SharedResultLine caseLevelResultLine = shareResultsMessage.getHearing().getSharedResultLines().stream()
                .filter(rl -> rl.getLevel().equals("CASE"))
                .filter(rl -> rl.getCaseId().equals(legalCase.getId()))
                .findFirst()
                .get();

        final SharedResultLine defendantLevelResultLine = shareResultsMessage.getHearing().getSharedResultLines().stream()
                .filter(rl -> rl.getLevel().equals("DEFENDANT"))
                .filter(rl -> rl.getDefendantId().equals(defendant.getId()))
                .findFirst()
                .get();


        final SharedResultLine offenceLevelResultLine = shareResultsMessage.getHearing().getSharedResultLines().stream()
                .filter(rl -> rl.getLevel().equals("OFFENCE"))
                .filter(rl -> rl.getOffenceId().equals(offence.getId()))
                .findFirst()
                .get();

        poll(requestParams(personHearingResultDetailsUrl, CONTENT_TYPE_RESULTS_DETAILS).withHeader(USER_ID, getLoggedInUser()))
                .until(
                        print(),
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.personId", equalTo(defendant.getPerson().getId().toString())),
                                withJsonPath("$.hearingId", equalTo(shareResultsMessage.getHearing().getId().toString())),

                                withJsonPath("$.defendantLevelResults.[0].label", equalTo(defendantLevelResultLine.getLabel())),
                                withJsonPath("$.defendantLevelResults.[0].prompts.[0].label", equalTo(defendantLevelResultLine.getPrompts().get(0).getLabel())),
                                withJsonPath("$.defendantLevelResults.[0].prompts.[0].value", equalTo(defendantLevelResultLine.getPrompts().get(0).getValue())),
                                withJsonPath("$.defendantLevelResults.[0].court", equalTo(shareResultsMessage.getHearing().getCourtCentre().getCourtCentreName())),
                                withJsonPath("$.defendantLevelResults.[0].courtRoom", equalTo(shareResultsMessage.getHearing().getCourtCentre().getCourtRoomName())),
                                withJsonPath("$.defendantLevelResults.[0].lastSharedDate", equalTo(defendantLevelResultLine.getLastSharedDateTime().toLocalDate().toString())),
                                withJsonPath("$.defendantLevelResults.[0].orderedDate", equalTo(defendantLevelResultLine.getOrderedDate().toString())),
                                withJsonPath("$.cases.[0].id", equalTo(legalCase.getId().toString())),
                                withJsonPath("$.cases.[0].urn", equalTo(legalCase.getUrn())),

                                withJsonPath("$.cases.[0].caseLevelResults.[0].label", equalTo(caseLevelResultLine.getLabel())),
                                withJsonPath("$.cases.[0].caseLevelResults.[0].prompts.[0].label", equalTo(caseLevelResultLine.getPrompts().get(0).getLabel())),
                                withJsonPath("$.cases.[0].caseLevelResults.[0].prompts.[0].value", equalTo(caseLevelResultLine.getPrompts().get(0).getValue())),
                                withJsonPath("$.cases.[0].caseLevelResults.[0].court", equalTo(shareResultsMessage.getHearing().getCourtCentre().getCourtCentreName())),
                                withJsonPath("$.cases.[0].caseLevelResults.[0].courtRoom", equalTo(shareResultsMessage.getHearing().getCourtCentre().getCourtRoomName())),
                                withJsonPath("$.cases.[0].caseLevelResults.[0].lastSharedDate", equalTo(caseLevelResultLine.getLastSharedDateTime().toLocalDate().toString())),
                                withJsonPath("$.cases.[0].caseLevelResults.[0].orderedDate", equalTo(caseLevelResultLine.getOrderedDate().toString())),
                                withJsonPath("$.cases.[0].offences.[0].id", equalTo(offence.getId().toString())),

                                withJsonPath("$.cases.[0].offences.[0].offenceLevelResults.[0].label", equalTo(offenceLevelResultLine.getLabel())),
                                withJsonPath("$.cases.[0].offences.[0].offenceLevelResults.[0].prompts.[0].label", equalTo(offenceLevelResultLine.getPrompts().get(0).getLabel())),
                                withJsonPath("$.cases.[0].offences.[0].offenceLevelResults.[0].prompts.[0].value", equalTo(offenceLevelResultLine.getPrompts().get(0).getValue())),
                                withJsonPath("$.cases.[0].offences.[0].offenceLevelResults.[0].court", equalTo(shareResultsMessage.getHearing().getCourtCentre().getCourtCentreName())),
                                withJsonPath("$.cases.[0].offences.[0].offenceLevelResults.[0].courtRoom", equalTo(shareResultsMessage.getHearing().getCourtCentre().getCourtRoomName())),
                                withJsonPath("$.cases.[0].offences.[0].offenceLevelResults.[0].lastSharedDate", equalTo(offenceLevelResultLine.getLastSharedDateTime().toLocalDate().toString())),
                                withJsonPath("$.cases.[0].offences.[0].offenceLevelResults.[0].orderedDate", equalTo(offenceLevelResultLine.getOrderedDate().toString())),
                                
                                withJsonPath("$.cases.[0].offences.[0].plea.pleaValue", equalTo(offence.getPlea().getValue())),
                                withJsonPath("$.cases.[0].offences.[0].plea.pleaDate", equalTo(LocalDates.to(offence.getPlea().getDate()))),
                                withJsonPath("$.cases.[0].offences.[0].offenceWording", equalTo(offence.getWording())),
                                withJsonPath("$.cases.[0].offences.[0].endDate", equalTo(LocalDates.to(offence.getEndDate()))),
                                withJsonPath("$.cases.[0].offences.[0].startDate", equalTo(LocalDates.to(offence.getStartDate()))),
                                withJsonPath("$.cases.[0].offences.[0].convictionDate", equalTo(LocalDates.to(offence.getConvictionDate()))),
                                withJsonPath("$.cases.[0].offences.[0].verdict.verdictDate", equalTo(LocalDates.to(offence.getVerdict().getVerdictDate()))),
                                withJsonPath("$.cases.[0].offences.[0].verdict.verdictCategory", equalTo(offence.getVerdict().getVerdictCategory())),
                                withJsonPath("$.cases.[0].offences.[0].verdict.verdictDescription", equalTo(offence.getVerdict().getVerdictDescription()))
                        )));
    }

    public static void thenResultDetailsAreAsExpected_givenNoPlea(final ShareResultsMessage shareResultsMessage) {

        final uk.gov.moj.cpp.domains.results.shareResults.Person person = shareResultsMessage.getHearing().getDefendants().get(0).getPerson();

        final Case legalCase = shareResultsMessage.getHearing().getDefendants().get(0).getCases().get(0);

        final uk.gov.moj.cpp.domains.results.shareResults.Offence offence = legalCase.getOffences().get(0);

        final String personHearingResultDetailsUrl = format("%s%s", BASE_URI,
                getProperty("results.get-person-hearing-result-details", person.getId(),
                        shareResultsMessage.getHearing().getId()));

        final SharedResultLine caseLevelResultLine = findResultLine(shareResultsMessage, legalCase);

        final SharedResultLine defendantLevelResultLine = shareResultsMessage.getHearing().getSharedResultLines().stream()
                .filter(rl -> rl.getLevel().equals("DEFENDANT"))
                .filter(rl -> rl.getDefendantId().equals(shareResultsMessage.getHearing().getDefendants().get(0).getId()))
                .findFirst()
                .get();


        final SharedResultLine offenceLevelResultLine = shareResultsMessage.getHearing().getSharedResultLines().stream()
                .filter(rl -> rl.getLevel().equals("OFFENCE"))
                .filter(rl -> rl.getOffenceId().equals(offence.getId()))
                .findFirst()
                .get();

        poll(requestParams(personHearingResultDetailsUrl, CONTENT_TYPE_RESULTS_DETAILS).withHeader(USER_ID, getLoggedInUser()))
                .until(
                        print(),
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.personId", equalTo(person.getId().toString())),
                                withJsonPath("$.hearingId", equalTo(shareResultsMessage.getHearing().getId().toString())),

                                withJsonPath("$.defendantLevelResults.[0].label", equalTo(defendantLevelResultLine.getLabel())),
                                withJsonPath("$.defendantLevelResults.[0].prompts.[0].label", equalTo(defendantLevelResultLine.getPrompts().get(0).getLabel())),
                                withJsonPath("$.defendantLevelResults.[0].prompts.[0].value", equalTo(defendantLevelResultLine.getPrompts().get(0).getValue())),
                                withJsonPath("$.defendantLevelResults.[0].court", equalTo(shareResultsMessage.getHearing().getCourtCentre().getCourtCentreName())),
                                withJsonPath("$.defendantLevelResults.[0].courtRoom", equalTo(shareResultsMessage.getHearing().getCourtCentre().getCourtRoomName())),
                                withJsonPath("$.defendantLevelResults.[0].lastSharedDate", equalTo(defendantLevelResultLine.getLastSharedDateTime().toLocalDate().toString())),
                                withJsonPath("$.defendantLevelResults.[0].orderedDate", equalTo(defendantLevelResultLine.getOrderedDate().toString())),
                                
                                withJsonPath("$.cases.[0].id", equalTo(legalCase.getId().toString())),
                                withJsonPath("$.cases.[0].urn", equalTo(legalCase.getUrn())),

                                withJsonPath("$.cases.[0].caseLevelResults.[0].label", equalTo(caseLevelResultLine.getLabel())),
                                withJsonPath("$.cases.[0].caseLevelResults.[0].prompts.[0].label", equalTo(caseLevelResultLine.getPrompts().get(0).getLabel())),
                                withJsonPath("$.cases.[0].caseLevelResults.[0].prompts.[0].value", equalTo(caseLevelResultLine.getPrompts().get(0).getValue())),
                                withJsonPath("$.cases.[0].caseLevelResults.[0].court", equalTo(shareResultsMessage.getHearing().getCourtCentre().getCourtCentreName())),
                                withJsonPath("$.cases.[0].caseLevelResults.[0].courtRoom", equalTo(shareResultsMessage.getHearing().getCourtCentre().getCourtRoomName())),
                                withJsonPath("$.cases.[0].caseLevelResults.[0].lastSharedDate", equalTo(caseLevelResultLine.getLastSharedDateTime().toLocalDate().toString())),
                                withJsonPath("$.cases.[0].caseLevelResults.[0].orderedDate", equalTo(caseLevelResultLine.getOrderedDate().toString())),
                                
                                withJsonPath("$.cases.[0].offences.[0].id", equalTo(offence.getId().toString())),

                                withJsonPath("$.cases.[0].offences.[0].offenceLevelResults.[0].label", equalTo(offenceLevelResultLine.getLabel())),
                                withJsonPath("$.cases.[0].offences.[0].offenceLevelResults.[0].prompts.[0].label", equalTo(offenceLevelResultLine.getPrompts().get(0).getLabel())),
                                withJsonPath("$.cases.[0].offences.[0].offenceLevelResults.[0].prompts.[0].value", equalTo(offenceLevelResultLine.getPrompts().get(0).getValue())),
                                withJsonPath("$.cases.[0].offences.[0].offenceLevelResults.[0].court", equalTo(shareResultsMessage.getHearing().getCourtCentre().getCourtCentreName())),
                                withJsonPath("$.cases.[0].offences.[0].offenceLevelResults.[0].courtRoom", equalTo(shareResultsMessage.getHearing().getCourtCentre().getCourtRoomName())),
                                withJsonPath("$.cases.[0].offences.[0].offenceLevelResults.[0].lastSharedDate", equalTo(offenceLevelResultLine.getLastSharedDateTime().toLocalDate().toString())),
                                withJsonPath("$.cases.[0].offences.[0].offenceLevelResults.[0].orderedDate", equalTo(offenceLevelResultLine.getOrderedDate().toString())),
                                
                                withoutJsonPath("$.cases.[0].offences.[0].plea.pleaValue"),
                                withoutJsonPath("$.cases.[0].offences.[0].plea.pleaDate"),
                                withJsonPath("$.cases.[0].offences.[0].offenceWording", equalTo(offence.getWording())),
                                withJsonPath("$.cases.[0].offences.[0].endDate", equalTo(LocalDates.to(offence.getEndDate()))),
                                withJsonPath("$.cases.[0].offences.[0].startDate", equalTo(LocalDates.to(offence.getStartDate()))),
                                withJsonPath("$.cases.[0].offences.[0].convictionDate", equalTo(LocalDates.to(offence.getConvictionDate()))),
                                withJsonPath("$.cases.[0].offences.[0].verdict.verdictDate", equalTo(LocalDates.to(offence.getVerdict().getVerdictDate()))),
                                withJsonPath("$.cases.[0].offences.[0].verdict.verdictCategory", equalTo(offence.getVerdict().getVerdictCategory())),
                                withJsonPath("$.cases.[0].offences.[0].verdict.verdictDescription", equalTo(offence.getVerdict().getVerdictDescription()))
                        )));
    }

    private static SharedResultLine findResultLine(final ShareResultsMessage shareResultsMessage, final Case legalCase) {
        return shareResultsMessage.getHearing().getSharedResultLines().stream()
                .filter(rl -> rl.getLevel().equals("CASE"))
                .filter(rl -> rl.getCaseId().equals(legalCase.getId()))
                .findFirst()
                .get();
    }

    public static void thenResultsSummaryShowsHearingWithinDateRange(final LocalDate fromDate) {
        final String resultSummaryUrl = format("%s%s?fromDate=%s", BASE_URI, getProperty(GET_RESULTS_SUMMARY), fromDate);
        final RequestParamsBuilder resultsSummaryRequest = requestParams(resultSummaryUrl,
                CONTENT_TYPE_RESULTS_SUMMARY)
                .withHeader(USER_ID, getLoggedInUser());

        final ResponseData resultsSummaryResponse = poll(resultsSummaryRequest)
                .until(
                        status().is(OK)
                );

        final JsonPath jsonResponse = new JsonPath(resultsSummaryResponse.getPayload());
        final List<String> hearingDates = jsonResponse.getList("results.hearingDate");

        assertThatHearingDatesAreNotBeforeFromDate(hearingDates, fromDate);
    }

    public static void andResultsSummaryShowsExpectedDetails(final LocalDate fromDate,
                                                             final ShareResultsMessage shareResultsMessage,
                                                             final JsonObject preExistingResultSummary) {
        final JsonObject resultSummaryPayload = getResultSummaryJson(fromDate);

        final JsonArray totalResults = resultSummaryPayload.getJsonArray("results");
        final JsonArray preExistingResults = preExistingResultSummary.getJsonArray("results");

        final List<String> preExistingPersonIds = preExistingResults.stream()
                .map(JsonObject.class::cast)
                .map(existingResult -> existingResult.getJsonObject("defendant").getString("personId"))
                .distinct().collect(toList());

        final List<JsonObject> actualResults = totalResults.stream().map(JsonObject.class::cast)
                .filter(actualResult -> !preExistingPersonIds.contains(actualResult.getJsonObject("defendant").getString("personId")))
                .collect(toList());

        final List<String> actualPersonIds = actualResults.stream()
                .map(jsonObject -> jsonObject.getJsonObject("defendant").getString("personId"))
                .distinct().collect(toList());

        final List<String> expectedPersonIds = shareResultsMessage.getHearing().getDefendants().stream()
                .map(Defendant::getPerson)
                .map(person -> person.getId().toString())
                .distinct()
                .collect(toList());

        assertThat(actualPersonIds, hasSize(expectedPersonIds.size()));
        assertThat(actualPersonIds, containsInAnyOrder(expectedPersonIds.toArray()));

        final uk.gov.moj.cpp.domains.results.shareResults.Person person = shareResultsMessage.getHearing().getDefendants().get(0).getPerson();

        actualResults.forEach(resultSummary -> {
            assertThat(resultSummary.getString("hearingId"), equalTo(shareResultsMessage.getHearing().getId().toString()));
            assertThat(resultSummary.getString("hearingType"), equalTo(shareResultsMessage.getHearing().getHearingType()));
            assertThat(resultSummary.getString("hearingDate"), equalTo(shareResultsMessage.getHearing().getStartDateTime().toLocalDate().toString()));
            final JsonObject defendant = resultSummary.getJsonObject("defendant");
            assertThat(defendant.getString("personId"), equalTo(person.getId().toString()));
            assertThat(defendant.getString("firstName"), equalTo(person.getFirstName()));
            assertThat(defendant.getString("lastName"), equalTo(person.getLastName()));

            final List<String> actualUrns = resultSummary.getJsonArray("urns").getValuesAs(JsonString.class).stream().map(JsonString::getString).collect(toList());
            final Object[] expectedUrns = shareResultsMessage.getHearing().getDefendants().get(0).getCases().stream()
                    .map(Case::getUrn)
                    .distinct()
                    .toArray();

            assertThat(actualUrns, containsInAnyOrder(expectedUrns));
        });
    }

    public static JsonObject getExistingResultSummary(final LocalDate localDate, final UUID userId) {
        setLoggedInUser(userId);
        return getResultSummaryJson(localDate);
    }

    private static JsonObject getResultSummaryJson(final LocalDate fromDate) {
        final String resultSummaryUrl = format("%s%s?fromDate=%s", BASE_URI, getProperty(GET_RESULTS_SUMMARY), fromDate);
        final RequestParamsBuilder resultsSummaryRequest = requestParams(resultSummaryUrl,
                CONTENT_TYPE_RESULTS_SUMMARY)
                .withHeader(USER_ID, getLoggedInUser());

        final ResponseData resultsSummaryResponse = poll(resultsSummaryRequest)
                .until(
                        status().is(OK)
                );
        return new StringToJsonObjectConverter().convert(resultsSummaryResponse.getPayload());
    }

    public static void thenReturnsBadRequestForResultsSummaryWithoutFromDate() {
        final String resultSummaryUrlWithoutFromDateParameter = format("%s%s", BASE_URI, getProperty(GET_RESULTS_SUMMARY));
        final Response resultsSummaryResponse = new RestClient()
                .query(resultSummaryUrlWithoutFromDateParameter,
                        CONTENT_TYPE_RESULTS_SUMMARY,
                        getUserHeader(getLoggedInUser()));

        assertThatResponseIndicatesBadRequest(resultsSummaryResponse);
    }


    public static void givenPersonDetailsAreAvailable(final uk.gov.moj.cpp.results.it.steps.data.people.Person person) {
        WireMockStubUtils.mockPersonDetails(person);
    }

    public static void hearingResultsHaveBeenShared(final ShareResultsMessage shareResultsMessage) {
        try (final MessageProducerClient messageProducer = new MessageProducerClient()) {
            messageProducer.startProducer(PUBLIC_EVENT_TOPIC);

            final JsonObject payload = convertToJsonObject(shareResultsMessage);

            messageProducer.sendMessage(PUBLIC_EVENT_HEARING_RESULTED, payload);
        }
    }

    private static void assertThatHearingDatesAreNotBeforeFromDate(final List<String> hearingDates, final LocalDate fromDate) {
        assertThat(hearingDates.stream().filter(hearingDate -> parse(hearingDate).isBefore(fromDate)).collect(toList()), hasSize(0));
    }

    private static void assertThatResponseIndicatesBadRequest(final Response response) {
        assertResponseStatusCode(SC_BAD_REQUEST, response);
    }

    private static void assertResponseStatusCode(final int statusCode, final Response response) {
        assertThat(response.getStatus(), CoreMatchers.equalTo(statusCode));
    }

    private static <T> Predicate<T> distinct(final Function<? super T, ?> func) {
        final Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(func.apply(t));
    }

    private static JsonObject convertToJsonObject(final Object input) {
        final ObjectMapper mapper = new ObjectMapperProducer().objectMapper();
        try {
            return mapper.readValue(mapper.writeValueAsString(input), JsonObject.class);
        } catch (final IOException e) {
            throw new IllegalArgumentException("Error while trying to convert.");
        }
    }
}
