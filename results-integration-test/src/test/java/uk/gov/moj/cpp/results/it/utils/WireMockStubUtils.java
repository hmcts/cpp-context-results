package uk.gov.moj.cpp.results.it.utils;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.reset;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.google.common.io.Resources.getResource;
import static java.nio.charset.Charset.defaultCharset;
import static java.text.MessageFormat.format;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static javax.ws.rs.core.Response.Status.OK;
import static uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils.stubPingFor;
import static uk.gov.justice.services.common.http.HeaderConstants.ID;
import static uk.gov.justice.services.test.utils.core.http.BaseUriProvider.getBaseUri;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.values;

import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.moj.cpp.results.it.steps.data.hearing.DefenceCounsel;
import uk.gov.moj.cpp.results.it.steps.data.hearing.Hearing;
import uk.gov.moj.cpp.results.it.steps.data.hearing.Offence;
import uk.gov.moj.cpp.results.it.steps.data.hearing.ProgressionCase;
import uk.gov.moj.cpp.results.it.steps.data.hearing.ProsecutionCounsel;
import uk.gov.moj.cpp.results.it.steps.data.people.Person;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.core.Response.Status;

import com.google.common.io.Resources;
import org.apache.http.HttpHeaders;

/**
 * Utility class for setting stubs.
 */
public class WireMockStubUtils {

    private static final String HOST = System.getProperty("INTEGRATION_HOST_KEY", "localhost");
    private static final String APPLICATION_JSON = "application/json";
    private static final String CONTENT_TYPE_QUERY_GROUPS = "application/vnd.usersgroups.groups+json";
    private static final String CONTENT_TYPE_QUERY_PERSON = "application/vnd.people.query.person+json";
    private static final String CONTENT_TYPE_QUERY_HEARING = "application/vnd.hearing.get.hearing+json";
    private static final String CONTENT_TYPE_QUERY_PLEAS = "application/vnd.hearing.get.pleas+json";
    private static final String CONTENT_TYPE_QUERY_PROGRESSION_DEFENDANTS = "application/vnd.progression.query.defendants+json";
    private static final String CONTENT_TYPE_QUERY_PROSECUTION_COUNSELS = "application/vnd.hearing.get.prosecution-counsels+json";
    private static final String CONTENT_TYPE_QUERY_DEFENCE_COUNSELS = "application/vnd.hearing.get.defence-counsels+json";
    private static final String CONTENT_TYPE_QUERY_CASE_DEFENDANTS = "application/vnd.progression.query.defendants+json";

    static {
        configureFor(HOST, 8080);
        reset();
    }

    public static void setupAsAuthorisedUser(final UUID userId) {
        stubPingFor("usersgroups-service");

        stubFor(get(urlPathEqualTo(format("/usersgroups-service/query/api/rest/usersgroups/users/{0}/groups", userId)))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(FileUtil.getPayload("stub-data/usersgroups.get-groups-by-user.json"))));

        waitForStubToBeReady(format("/usersgroups-service/query/api/rest/usersgroups/users/{0}/groups", userId), CONTENT_TYPE_QUERY_GROUPS);
    }

    public static void setupAsSystemUser(final UUID userId) {
        stubPingFor("usersgroups-service");

        stubFor(get(urlPathEqualTo(format("/usersgroups-service/query/api/rest/usersgroups/users/{0}/groups", userId)))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(FileUtil.getPayload("stub-data/usersgroups.get-systemuser-groups-by-user.json"))));

        waitForStubToBeReady(format("/usersgroups-service/query/api/rest/usersgroups/users/{0}/groups", userId), CONTENT_TYPE_QUERY_GROUPS);
    }

    public static void setupUserAsPrisonAdminGroup(final UUID userId) {
        setupUserWithGroup(userId, "stub-data/get-groups-prison-admin.json");
    }

    public static void setupUserAsProbationAdminGroup(final UUID userId) {
        setupUserWithGroup(userId, "stub-data/get-groups-probation-admin.json");
    }

    public static void setupUserAsPoliceAdminGroup(final UUID userId) {
        setupUserWithGroup(userId, "stub-data/get-groups-police-admin.json");
    }

    public static void setupUserAsVictimsWitnessCareAdminGroup(final UUID userId) {
        setupUserWithGroup(userId, "stub-data/get-groups-victims-witness-care-admin.json");
    }

    public static void setupUserAsYouthOffendingServiceAdminGroup(final UUID userId) {
        setupUserWithGroup(userId, "stub-data/get-groups-youth-offending-service-admin.json");
    }

    public static void setupUserAsLegalAidAgencyAdminGroup(final UUID userId) {
        setupUserWithGroup(userId, "stub-data/get-groups-legal-aid-agency-admin.json");
    }

    public static void setupUserAsUnauthorisedGroup(final UUID userId) {
        setupUserWithGroup(userId, "stub-data/get-groups-unauthorised-user.json");
    }

    private static void setupUserWithGroup(final UUID userId, String userGroupFile) {
        stubPingFor("usersgroups-service");

        stubFor(get(urlPathEqualTo(format("/usersgroups-service/query/api/rest/usersgroups/users/{0}/groups", userId)))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(getJsonResponse(userGroupFile))));

        waitForStubToBeReady(format("/usersgroups-service/query/api/rest/usersgroups/users/{0}/groups", userId), CONTENT_TYPE_QUERY_GROUPS);
    }

    public static void mockPersonDetails(final Person person) {
        stubPingFor("people-service");

        stubFor(get(urlPathEqualTo(format("/people-service/query/api/rest/people/people/{0}", person.getPersonId())))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE_QUERY_PERSON)
                        .withBody(getPersonJsonBuilder(person).build().toString())));

        waitForStubToBeReady(format("/people-service/query/api/rest/people/people/{0}", person.getPersonId()), CONTENT_TYPE_QUERY_PERSON);
    }

    public static void mockProgressionCaseDetails(final ProgressionCase progressionCase) {
        stubPingFor("progression-service");

        stubFor(get(urlPathEqualTo(format("/progression-service/query/api/rest/progression/cases/{0}", progressionCase.getCaseId())))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE_QUERY_PROGRESSION_DEFENDANTS)
                        .withBody(getProgressionCaseJsonBuilder(progressionCase).build().toString())));

        waitForStubToBeReady(format("/progression-service/query/api/rest/progression/cases/{0}", progressionCase.getCaseId()), CONTENT_TYPE_QUERY_PROGRESSION_DEFENDANTS);
    }

    public static void mockHearingDetails(final Hearing hearing) {
        stubPingFor("hearing-service");

        stubFor(get(urlPathEqualTo(format("/hearing-service/query/api/rest/hearing/hearings/{0}", hearing.getHearingId())))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE_QUERY_HEARING)
                        .withBody(getHearingJsonBuilder(hearing).build().toString())));

        waitForStubToBeReady(format("/hearing-service/query/api/rest/hearing/hearings/{0}", hearing.getHearingId()), CONTENT_TYPE_QUERY_HEARING);
    }

    public static void mockPleasDetails(final ProgressionCase progressionCase) {
        stubPingFor("hearing-service");

        stubFor(get(urlPathEqualTo(format("/hearing-service/query/api/rest/hearing/cases/{0}/pleas", progressionCase.getCaseId())))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE_QUERY_PLEAS)
                        .withBody(pleasJsonBuilder(progressionCase).build().toString())));

        waitForStubToBeReady(format("/hearing-service/query/api/rest/hearing/cases/{0}/pleas", progressionCase.getCaseId()), CONTENT_TYPE_QUERY_PLEAS);
    }

    public static void mockEmptyPleaDetails(final ProgressionCase progressionCase) {
        stubPingFor("hearing-service");

        stubFor(get(urlPathEqualTo(format("/hearing-service/query/api/rest/hearing/cases/{0}/pleas", progressionCase.getCaseId())))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE_QUERY_PLEAS)
                        .withBody(emptyPleasJsonBuilder().build().toString())));

        waitForStubToBeReady(format("/hearing-service/query/api/rest/hearing/cases/{0}/pleas", progressionCase.getCaseId()), CONTENT_TYPE_QUERY_PLEAS);
    }

    public static void mockProsecutionCounsels(final UUID hearingId, final List<ProsecutionCounsel> prosecutionCounsels) {
        prosecutionCounsels.forEach(prosecutionCounsel -> mockPersonDetails(prosecutionCounsel.getPerson()));

        stubFor(get(urlPathEqualTo(format("/hearing-service/query/api/rest/hearing/hearings/{0}/prosecution-counsels", hearingId)))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE_QUERY_PROSECUTION_COUNSELS)
                        .withBody(getProsecutionCounselsJsonBuilder(prosecutionCounsels).build().toString())));

        waitForStubToBeReady(format("/hearing-service/query/api/rest/hearing/hearings/{0}/prosecution-counsels", hearingId), CONTENT_TYPE_QUERY_PROSECUTION_COUNSELS);
    }

    public static void mockDefenceCounsels(final UUID hearingId, final List<DefenceCounsel> defenceCounsels) {
        defenceCounsels.forEach(defenceCounsel -> mockPersonDetails(defenceCounsel.getPerson()));

        stubFor(get(urlPathEqualTo(format("/hearing-service/query/api/rest/hearing/hearings/{0}/defence-counsels", hearingId)))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE_QUERY_DEFENCE_COUNSELS)
                        .withBody(getDefenceCounselsJsonBuilder(defenceCounsels).build().toString())));

        waitForStubToBeReady(format("/hearing-service/query/api/rest/hearing/hearings/{0}/defence-counsels", hearingId), CONTENT_TYPE_QUERY_DEFENCE_COUNSELS);
    }

    public static void mockCaseDefendants(final ProgressionCase progressionCase) {
        stubPingFor("progression-service");

        stubFor(get(urlPathEqualTo(format("/progression-service/query/api/rest/progression/cases/{0}/defendants", progressionCase.getCaseId())))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE_QUERY_CASE_DEFENDANTS)
                        .withBody(getCaseDefendantsJsonBuilder(progressionCase.getCaseId(), progressionCase.getPersonIds(), progressionCase.getDefendantIds(), progressionCase.getOffences()).build().toString())));

        waitForStubToBeReady(format("/progression-service/query/api/rest/progression/cases/{0}/defendants", progressionCase.getCaseId()), CONTENT_TYPE_QUERY_CASE_DEFENDANTS);
    }

    private static JsonObjectBuilder getProsecutionCounselsJsonBuilder(final List<ProsecutionCounsel> prosecutionCounsels) {
        final JsonArrayBuilder prosecutionCounselsBuilder = createArrayBuilder();

        prosecutionCounsels.forEach(prosecutionCounsel ->
                prosecutionCounselsBuilder.add(createObjectBuilder()
                        .add("attendeeId", prosecutionCounsel.getAttendeeId().toString())
                        .add("personId", prosecutionCounsel.getPersonId().toString())
                        .add("status", prosecutionCounsel.getStatus())
                )
        );

        return createObjectBuilder().add("prosecution-counsels", prosecutionCounselsBuilder);
    }

    private static JsonObjectBuilder getDefenceCounselsJsonBuilder(final List<DefenceCounsel> defenceCounsels) {
        final JsonArrayBuilder defenceCounselsBuilder = createArrayBuilder();

        defenceCounsels.forEach(defenceCounsel -> {
                    final JsonArrayBuilder defendantIdsBuilder = createArrayBuilder();

                    defenceCounsel.getPersonIds().forEach(defendantId ->
                            defendantIdsBuilder.add(createObjectBuilder().add("defendantId", defendantId.toString()))
                    );

                    defenceCounselsBuilder.add(createObjectBuilder()
                            .add("attendeeId", defenceCounsel.getAttendeeId().toString())
                            .add("personId", defenceCounsel.getPersonId().toString())
                            .add("status", defenceCounsel.getStatus())
                            .add("defendantIds", defendantIdsBuilder)
                    );
                }
        );

        return createObjectBuilder().add("defence-counsels", defenceCounselsBuilder);
    }

    private static JsonObjectBuilder getHearingJsonBuilder(final Hearing hearing) {

        return createObjectBuilder()
                .add("hearingId", hearing.getHearingId().toString())
                .add("startDate", LocalDates.to(hearing.getStartDate()))
                .add("startTime", hearing.getStartTime().toString())
                .add("hearingType", hearing.getHearingType())
                .add("courtCentreName", hearing.getCourtCenterName())
                .add("duration", hearing.getDuration())
                .add("judge", createObjectBuilder()
                        .add("id", hearing.getJudge().getId().toString())
                        .add("title", hearing.getJudge().getTitle())
                        .add("firstName", hearing.getJudge().getFirstName())
                        .add("lastName", hearing.getJudge().getLastName())
                );
    }

    private static JsonObjectBuilder pleasJsonBuilder(final ProgressionCase progressionCase){
        return createObjectBuilder()
                .add("pleas", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("pleaId", progressionCase.getOffences().get(0).getPlea().getPleaId().toString())
                                .add("offenceId", progressionCase.getOffences().get(0).getId().toString())
                                .add("pleaDate", progressionCase.getOffences().get(0).getPlea().getPleaDate().toString())
                                .add("value", progressionCase.getOffences().get(0).getPlea().getPleaValue())
                        )
                        .add(createObjectBuilder()
                                .add("pleaId", randomUUID().toString())
                                .add("offenceId", randomUUID().toString())
                                .add("pleaDate", progressionCase.getOffences().get(0).getPlea().getPleaDate().toString())
                                .add("value", progressionCase.getOffences().get(0).getPlea().getPleaValue())
                        )
                );
    }


    private static JsonObjectBuilder emptyPleasJsonBuilder() {
        return createObjectBuilder()
                .add("pleas", createArrayBuilder());

    }

    private static JsonObjectBuilder getProgressionCaseJsonBuilder(final ProgressionCase progressionCase) {
        return createObjectBuilder()
                .add("caseId", progressionCase.getCaseId().toString())
                .add("caseUrn", progressionCase.getUrn());
    }

    private static JsonObjectBuilder getPersonJsonBuilder(final Person person) {
        final JsonObjectBuilder personJson = createObjectBuilder();
        final JsonObjectBuilder addressJson = createObjectBuilder().add("address1", person.getAddress1());

        if (person.getDateOfBirth() != null) {
            personJson.add("dateOfBirth", LocalDates.to(person.getDateOfBirth()));
        }
        if (person.getAddress2() != null) {
            addressJson.add("address2", person.getAddress2());
        }
        if (person.getAddress3() != null) {
            addressJson.add("address3", person.getAddress3());
        }
        if (person.getAddress4() != null) {
            addressJson.add("address4", person.getAddress4());
        }
        if (person.getPostCode() != null) {
            addressJson.add("postCode", person.getPostCode());
        }
        return personJson
                .add("id", person.getPersonId().toString())
                .add("title", RandomPersonNameGenerator.title())
                .add("firstName", person.getFirstName())
                .add("lastName", person.getLastName())
                .add("nationality", STRING.next())
                .add("disability", STRING.next())
                .add("ethnicity", STRING.next())
                .add("gender", values(asList("Male", "Female", "Not Specified")).next())
                .add("address", addressJson);
    }

    private static JsonObjectBuilder getCaseDefendantsJsonBuilder(final UUID caseId, final List<UUID> peopleIds, final List<UUID> defendantIds, final List<Offence> offences) {
        final JsonArrayBuilder caseDefendantsBuilder = createArrayBuilder();


        IntStream.range(0, peopleIds.size())
                .forEach(i -> caseDefendantsBuilder.add(createObjectBuilder()
                        .add("id", defendantIds.get(i).toString())
                        .add("personId", peopleIds.get(i).toString())
                        .add("caseId", caseId.toString())
                        .add("offences", progressionDefendantsByCaseIdOffences(offences.get(i)))));

        return createObjectBuilder().add("defendants", caseDefendantsBuilder);
    }

    private static JsonArray progressionDefendantsByCaseIdOffences(final Offence offence) {

        final JsonObjectBuilder offenceBuilder = createObjectBuilder()
                .add("id", offence.getId().toString())
                .add("description", offence.getOffenceTitle())
                .add("startDate", offence.getStartDate().toString())
                .add("endDate", offence.getEndDate().toString());

        if (offence.getPlea() != null) {
            offenceBuilder.add("plea", getPlea(offence));
        }

        return createArrayBuilder().add(offenceBuilder).build();
    }

    private static JsonObjectBuilder getPlea(final Offence offence) {
        return createObjectBuilder().add("pleaValue", offence.getPlea().getPleaValue())
                .add("pleaDate", LocalDates.to(offence.getPlea().getPleaDate()));
    }

    private static String getJsonResponse(final String filename) {
        try {
            return Resources.toString(getResource(filename), defaultCharset());
        } catch (final IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    static void waitForStubToBeReady(final String resource, final String mediaType) {
        waitForStubToBeReady(resource, mediaType, Status.OK);
    }

    private static void waitForStubToBeReady(final String resource, final String mediaType, final Status expectedStatus) {
        poll(requestParams(format("{0}/{1}", getBaseUri(), resource), mediaType)).until(status().is(expectedStatus));
    }

}
