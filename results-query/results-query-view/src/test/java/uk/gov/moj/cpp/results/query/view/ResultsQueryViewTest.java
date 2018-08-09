package uk.gov.moj.cpp.results.query.view;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.random.RandomGenerator;
import uk.gov.moj.cpp.domains.results.result.ResultLevel;
import uk.gov.moj.cpp.results.persist.DefendantRepository;
import uk.gov.moj.cpp.results.persist.HearingRepository;
import uk.gov.moj.cpp.results.persist.HearingResultRepository;
import uk.gov.moj.cpp.results.persist.VariantDirectoryRepository;
import uk.gov.moj.cpp.results.query.view.response.HearingResultSummariesView;
import uk.gov.moj.cpp.results.query.view.response.HearingResultSummaryView;
import uk.gov.moj.cpp.results.query.view.service.HearingService;
import uk.gov.moj.cpp.results.query.view.service.UserGroupsService;
import uk.gov.moj.cpp.results.persist.entity.CourtClerk;
import uk.gov.moj.cpp.results.persist.entity.Defendant;
import uk.gov.moj.cpp.results.persist.entity.Hearing;
import uk.gov.moj.cpp.results.persist.entity.HearingResult;
import uk.gov.moj.cpp.results.persist.entity.HearingResultSummary;
import uk.gov.moj.cpp.results.persist.entity.ResultPrompt;
import uk.gov.moj.cpp.results.persist.entity.VariantDirectory;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;

import static com.jayway.jsonassert.impl.matcher.IsCollectionWithSize.hasSize;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.matchers.JsonValueNullMatcher.isJsonValueNull;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.*;
import static uk.gov.moj.cpp.domains.results.result.ResultLevel.*;

@SuppressWarnings({"CdiInjectionPointsInspection", "unused", "unchecked"})
@RunWith(MockitoJUnitRunner.class)
public class ResultsQueryViewTest {

    private static final String REQUEST_NAME_GET_RESULTS_SUMMARY = "results.get-results-summary";

    private static final String RESPONSE_NAME_PERSON_DETAILS = "results.person-details";
    private static final String RESPONSE_NAME_HEARING_DETAILS = "results.hearing-details";
    private static final String RESPONSE_NAME_RESULTS_DETAILS = "results.results-details";
    private static final String RESPONSE_NAME_RESULTS_SUMMARY = "results.results-summary";

    private static final UUID PERSON_ID = randomUUID();
    private static final UUID DEFENDANT_ID = randomUUID();

    private static final String FIRST_NAME = STRING.next();
    private static final String LAST_NAME = STRING.next();
    private static final LocalDate DATE_OF_BIRTH = PAST_LOCAL_DATE.next();
    private static final String ADDRESS_1 = STRING.next();
    private static final UUID CLERK_OF_THE_COURT_ID = randomUUID();
    private static final String CLERK_OF_THE_COURT_FIRSTNAME = "Amon";
    private static final String CLERK_OF_THE_COURT_LASTNAME = STRING.next();
    private static final String ADDRESS_2 = STRING.next();
    private static final String ADDRESS_3 = STRING.next();
    private static final String ADDRESS_4 = STRING.next();
    private static final String POST_CODE = RandomGenerator.POST_CODE.next();

    private static final String FIELD_PERSON_ID = "personId";
    private static final String FIELD_HEARING_ID = "hearingId";
    private static final String FIELD_FROM_DATE = "fromDate";
    private static final String FIELD_RESULTS = "results";
    private static final String FIELD_HEARING_DATE = "hearingDate";

    private static final UUID HEARING_ID = randomUUID();
    private static final String[] HEARING_TYPES = {"PTPH", "SENTENCE", "TRIAL"};
    private static final String HEARING_TYPE =
                    HEARING_TYPES[new Random().nextInt(HEARING_TYPES.length)];
    private static final LocalDate HEARING_START_DATE = PAST_LOCAL_DATE.next();
    private static final LocalDate HEARING_END_DATE = HEARING_START_DATE.plusWeeks(2);
    private static final String COURT_CENTRE_NAME = STRING.next();
    private static final String COURT_ROOM = STRING.next();

    private static final String COURT_CODE = INTEGER.next().toString();
    private static final String JUDGE_NAME = STRING.next();
    private static final String PROSECUTOR_NAME = STRING.next();
    private static final String DEFENCE_NAME = STRING.next();
    private static final UUID HEARING_RESULT_ID = randomUUID();
    private static final String GENERATED = "GENERATED";

    private static final HearingResult HEARING_RESULT = HearingResult.builder()
            .withId(HEARING_RESULT_ID)
            .withUrn(STRING.next())
            .withOffenceId(randomUUID())
            .withOffenceTitle(STRING.next())
            .withCaseId(randomUUID())
            .withHearingId(randomUUID())
            .withPersonId(PERSON_ID)
            .withPleaValue(STRING.next())
            .withPleaDate(PAST_LOCAL_DATE.next())
            .withResultLevel(randomEnum(ResultLevel.class).next())
            .withResultLabel(STRING.next())
            .withCourt(STRING.next())
            .withCourtRoom(STRING.next())
            .withOrderedDate(PAST_LOCAL_DATE.next())
            .withLastSharedDateTime(PAST_ZONED_DATE_TIME.next())
            .withClerkOfTheCourtId(randomUUID())
            .withClerkOfTheCourtFirstName(STRING.next())
            .withClerkOfTheCourtLastName(STRING.next())
            .withStartDate(PAST_LOCAL_DATE.next())
            .withEndDate(PAST_LOCAL_DATE.next())
            .withVerdictCategory(STRING.next())
            .withVerdictDate(PAST_LOCAL_DATE.next())
            .withVerdictDescription(STRING.next())
            .withConvictionDate(PAST_LOCAL_DATE.next())
            .withResultPrompts(asList(
                    ResultPrompt.builder()
                            .withId(randomUUID())
                            .withLabel(STRING.next())
                            .withValue(STRING.next())
                            .withHearingResultId(HEARING_RESULT_ID)
                            .build()

            ))
            .build();

    @Spy
    private final Enveloper enveloper = createEnveloper();

    @Mock
    private DefendantRepository defendantRepository;

    @Mock
    private HearingRepository hearingRepository;

    @Mock
    private HearingResultRepository hearingResultRepository;

    @Mock
    private HearingService hearingService;

    @Mock
    private UserGroupsService userGroupsService;

    @Mock
    private VariantDirectoryRepository variantDirectoryRepository;

    @InjectMocks
    private ResultsQueryView resultsQueryView;

    @Test
    public void shouldGetPersonDetails() {
        when(defendantRepository.findPersonByPersonIdAndHearingId(PERSON_ID, HEARING_ID)).thenReturn(defendant());

        final JsonEnvelope query = envelopeFrom(metadataWithRandomUUIDAndName(), createObjectBuilder()
                .add(FIELD_PERSON_ID, PERSON_ID.toString())
                .add(FIELD_HEARING_ID, HEARING_ID.toString())
                .build());

        final JsonEnvelope actualPersonResults = resultsQueryView.getPersonDetails(query);

        assertThat(actualPersonResults, is(jsonEnvelope(
                withMetadataEnvelopedFrom(query)
                        .withName(RESPONSE_NAME_PERSON_DETAILS),
                payloadIsJson(allOf(
                        withJsonPath("$.id", equalTo(PERSON_ID.toString())),
                        withJsonPath("$.firstName", equalTo(FIRST_NAME)),
                        withJsonPath("$.lastName", equalTo(LAST_NAME)),
                        withJsonPath("$.dateOfBirth", equalTo(LocalDates.to(DATE_OF_BIRTH))),
                        withJsonPath("$.address.address1", equalTo(ADDRESS_1)),
                        withJsonPath("$.address.address2", equalTo(ADDRESS_2)),
                        withJsonPath("$.address.address3", equalTo(ADDRESS_3)),
                        withJsonPath("$.address.address4", equalTo(ADDRESS_4)),
                        withJsonPath("$.address.postCode", equalTo(POST_CODE))
                ))).thatMatchesSchema()
        ));
    }

    @Test
    public void shouldGetPersonDetailsWithRequiredFieldsOnly() {
        when(defendantRepository.findPersonByPersonIdAndHearingId(PERSON_ID, HEARING_ID)).thenReturn(personWithRequiredFieldsOnly());

        final JsonEnvelope query = envelopeFrom(metadataWithRandomUUIDAndName(), createObjectBuilder()
                .add(FIELD_PERSON_ID, PERSON_ID.toString())
                .add(FIELD_HEARING_ID, HEARING_ID.toString())
                .build());

        final JsonEnvelope actualPersonResults = resultsQueryView.getPersonDetails(query);

        assertThat(actualPersonResults, is(jsonEnvelope(
                withMetadataEnvelopedFrom(query)
                        .withName(RESPONSE_NAME_PERSON_DETAILS),
                payloadIsJson(allOf(
                        withJsonPath("$.id", equalTo(PERSON_ID.toString())),
                        withJsonPath("$.firstName", equalTo(FIRST_NAME)),
                        withJsonPath("$.lastName", equalTo(LAST_NAME)),
                        withJsonPath("$.address.address1", equalTo(ADDRESS_1)),
                        withoutJsonPath("$.dateOfBirth"),
                        withoutJsonPath("$.address.address2"),
                        withoutJsonPath("$.address.address3"),
                        withoutJsonPath("$.address.address4"),
                        withoutJsonPath("$.address.postCode")
                ))).thatMatchesSchema()
        ));
    }

    @Test
    public void shouldNotReturnPersonDetailsWhenResultsHaveNotBeenShared() {
        when(defendantRepository.findPersonByPersonIdAndHearingId(PERSON_ID, HEARING_ID)).thenReturn(null);

        final JsonEnvelope query = envelopeFrom(metadataWithRandomUUIDAndName(), createObjectBuilder()
                .add(FIELD_PERSON_ID, PERSON_ID.toString())
                .add(FIELD_HEARING_ID, HEARING_ID.toString())
                .build());

        final JsonEnvelope actualPersonResults = resultsQueryView.getPersonDetails(query);

        assertThat(actualPersonResults, is(jsonEnvelope(
                withMetadataEnvelopedFrom(query)
                        .withName(RESPONSE_NAME_PERSON_DETAILS),
                payload(isJsonValueNull()))
//                .thatMatchesSchema() //TODO: uncomment when framework have fixed it
        ));

    }

    @Test
    public void shouldGetHearingDetails() {

        UUID materialId1 = randomUUID();
        UUID materialId2 = randomUUID();

        when(hearingRepository.findHearingByPersonIdAndHearingId(PERSON_ID, HEARING_ID)).thenReturn(hearing());

        when(hearingResultRepository.findCourtClerksForHearingIdAndPersonId(HEARING_ID, PERSON_ID)).thenReturn(
                asList(CourtClerk.builder()
                        .withClerkOfTheCourtId(CLERK_OF_THE_COURT_ID)
                        .withClerkOfTheCourtFirstName(CLERK_OF_THE_COURT_FIRSTNAME)
                        .withClerkOfTheCourtLastName(CLERK_OF_THE_COURT_LASTNAME)
                        .build())
        );

        when(variantDirectoryRepository.findByDefendantIdAndHearingId(PERSON_ID, HEARING_ID)).thenReturn(
                asList(new VariantDirectory(randomUUID(), HEARING_ID, PERSON_ID, DEFENDANT_ID, randomUUID(), Arrays.asList("ADMIN"), materialId1, "description", "templateName", GENERATED),
                        new VariantDirectory(randomUUID(), HEARING_ID, PERSON_ID, DEFENDANT_ID, randomUUID(), Arrays.asList("Court Clerk", "Listing Officer", "Police Officer"), materialId2, "description", "templateName", GENERATED))
        );

        final JsonEnvelope query = envelopeFrom(metadataWithRandomUUIDAndName(), createObjectBuilder()
                .add(FIELD_PERSON_ID, PERSON_ID.toString())
                .add(FIELD_HEARING_ID, HEARING_ID.toString())
                .build());

        when(userGroupsService.findUserGroupsByUserId(query)).thenReturn(
                asList("Court Clerk", "Listing Officer")
        );

        final JsonEnvelope actualHearingResults = resultsQueryView.getHearingDetails(query);

        assertThat(actualHearingResults, is(jsonEnvelope(
                withMetadataEnvelopedFrom(query)
                        .withName(RESPONSE_NAME_HEARING_DETAILS),
                payloadIsJson(allOf(
                        withJsonPath("$.id", equalTo(HEARING_ID.toString())),
                        withJsonPath("$.courtCentreName", equalTo(COURT_CENTRE_NAME)),
                        withJsonPath("$.courtCode", equalTo(COURT_CODE)),
                        withJsonPath("$.startDate", equalTo(LocalDates.to(HEARING_START_DATE))),
                        withJsonPath("$.judgeName", equalTo(JUDGE_NAME)),
                        withJsonPath("$.prosecutorName", equalTo(PROSECUTOR_NAME)),
                        withJsonPath("$.defenceName", equalTo(DEFENCE_NAME)),
                        withJsonPath("$.clerks[0].clerkOfTheCourtId", equalTo(CLERK_OF_THE_COURT_ID.toString())),
                        withJsonPath("$.clerks[0].clerkOfTheCourtFirstName", equalTo(CLERK_OF_THE_COURT_FIRSTNAME)),
                        withJsonPath("$.clerks[0].clerkOfTheCourtLastName", equalTo(CLERK_OF_THE_COURT_LASTNAME)),
                        withoutJsonPath("$.clerks[1]"),
                        withJsonPath("$.variants[0].materialId", equalTo(materialId2.toString())),
                        withJsonPath("$.variants[0].status", equalTo(GENERATED)),
                        withoutJsonPath("$.variants[1]")
                ))).thatMatchesSchema()
        ));
    }

    @Test
    public void shouldNotReturnHearingDetailsWhenResultsHaveNotBeenShared() {
        when(hearingRepository.findHearingByPersonIdAndHearingId(PERSON_ID, HEARING_ID)).thenReturn(null);

        final JsonEnvelope query = envelopeFrom(metadataWithRandomUUIDAndName(), createObjectBuilder()
                .add(FIELD_PERSON_ID, PERSON_ID.toString())
                .add(FIELD_HEARING_ID, HEARING_ID.toString())
                .build());

        final JsonEnvelope actualHearingResults = resultsQueryView.getHearingDetails(query);

        assertThat(actualHearingResults, is(jsonEnvelope(
                withMetadataEnvelopedFrom(query)
                        .withName(RESPONSE_NAME_HEARING_DETAILS),
                payload(isJsonValueNull()))
//                .thatMatchesSchema() //TODO: uncomment when framework have fixed it
        ));

    }

    @Test
    public void shouldGetHearingResultSummaries() {
        when(hearingService.findHearingResultSummariesFromDate(HEARING_RESULT.getStartDate()))
                .thenReturn(hearingResultsView(
                        hearingResultWithDate(HEARING_RESULT.getStartDate().plusDays(1)),
                        hearingResultWithDate(HEARING_RESULT.getStartDate().plusDays(2))));

        final JsonEnvelope query = envelopeFrom(metadataWithRandomUUID(REQUEST_NAME_GET_RESULTS_SUMMARY), createObjectBuilder()
                .add(FIELD_FROM_DATE, HEARING_RESULT.getStartDate().toString())
                .build());

        final JsonEnvelope actualHearingResults = resultsQueryView.getResultsSummary(query);

        assertThat(actualHearingResults, is(jsonEnvelope(
                withMetadataEnvelopedFrom(query)
                        .withName(RESPONSE_NAME_RESULTS_SUMMARY),
                payloadIsJson(allOf(
                        withJsonPath(format("$.%s", FIELD_RESULTS), hasSize(2)),
                        withJsonPath(format("$.%s[0].%s", FIELD_RESULTS, FIELD_HEARING_DATE), equalTo(HEARING_RESULT.getStartDate().plusDays(1).toString())),
                        withJsonPath(format("$.%s[1].%s", FIELD_RESULTS, FIELD_HEARING_DATE), equalTo(HEARING_RESULT.getStartDate().plusDays(2).toString()))
                ))).thatMatchesSchema()
        ));

    }

    @Test
    public void shouldGetResultsDetailsWhenCaseLevelResultsAreAvailable() {
        final HearingResult caseLevelResult = hearingResultDetail(CASE);

        when(hearingResultRepository.findByHearingIdAndPersonId(HEARING_ID, PERSON_ID)).thenReturn(
                asList(caseLevelResult)
        );

        final JsonEnvelope query = envelopeFrom(metadataWithRandomUUIDAndName(), createObjectBuilder()
                .add(FIELD_PERSON_ID, PERSON_ID.toString())
                .add(FIELD_HEARING_ID, HEARING_ID.toString())
                .build());

        final JsonEnvelope actualResultDetails = resultsQueryView.getResultsDetails(query);

        assertThat(actualResultDetails, is(jsonEnvelope(
                withMetadataEnvelopedFrom(query)
                        .withName(RESPONSE_NAME_RESULTS_DETAILS),
                payloadIsJson(allOf(
                        withJsonPath("$.hearingId", equalTo(HEARING_ID.toString())),
                        withJsonPath("$.personId", equalTo(PERSON_ID.toString())),
                        withJsonPath("$.defendantLevelResults", hasSize(0)),
                        withJsonPath("$.cases[0].id", equalTo(caseLevelResult.getCaseId().toString())),
                        withJsonPath("$.cases[0].urn", equalTo(caseLevelResult.getUrn())),
                        withJsonPath("$.cases[0].caseLevelResults[0].label", equalTo(caseLevelResult.getResultLabel())),
                        withJsonPath("$.cases[0].caseLevelResults[0].prompts[0].label", equalTo(caseLevelResult.getResultPrompts().get(0).getLabel())),
                        withJsonPath("$.cases[0].caseLevelResults[0].prompts[0].value", equalTo(caseLevelResult.getResultPrompts().get(0).getValue())),
                        withJsonPath("$.cases[0].offences[0].id", equalTo(caseLevelResult.getOffenceId().toString())),
                        withJsonPath("$.cases[0].offences[0].offenceWording", equalTo(caseLevelResult.getOffenceTitle())),
                        withJsonPath("$.cases[0].offences[0].offenceLevelResults", hasSize(0))
                ))).thatMatchesSchema()
        ));
    }

    @Test
    public void shouldGetResultsDetailsWhenDefendantLevelResultsAreAvailable() {
        final HearingResult defendantLevelResults = hearingResultDetail(DEFENDANT);

        when(hearingResultRepository.findByHearingIdAndPersonId(HEARING_ID, PERSON_ID)).thenReturn(
                asList(defendantLevelResults)
        );

        final JsonEnvelope query = envelopeFrom(metadataWithRandomUUIDAndName(), createObjectBuilder()
                .add(FIELD_PERSON_ID, PERSON_ID.toString())
                .add(FIELD_HEARING_ID, HEARING_ID.toString())
                .build());

        final JsonEnvelope actualResultDetails = resultsQueryView.getResultsDetails(query);

        assertThat(actualResultDetails, is(jsonEnvelope(
                withMetadataEnvelopedFrom(query)
                        .withName(RESPONSE_NAME_RESULTS_DETAILS),
                payloadIsJson(allOf(
                        withJsonPath("$.hearingId", equalTo(HEARING_ID.toString())),
                        withJsonPath("$.personId", equalTo(PERSON_ID.toString())),
                        withJsonPath("$.defendantLevelResults[0].label", equalTo(defendantLevelResults.getResultLabel())),
                        withJsonPath("$.defendantLevelResults[0].prompts[0].label", equalTo(defendantLevelResults.getResultPrompts().get(0).getLabel())),
                        withJsonPath("$.defendantLevelResults[0].prompts[0].value", equalTo(defendantLevelResults.getResultPrompts().get(0).getValue())),
                        withJsonPath("$.cases[0].id", equalTo(defendantLevelResults.getCaseId().toString())),
                        withJsonPath("$.cases[0].urn", equalTo(defendantLevelResults.getUrn())),
                        withJsonPath("$.cases[0].caseLevelResults", hasSize(0)),
                        withJsonPath("$.cases[0].offences[0].offenceWording", equalTo(defendantLevelResults.getOffenceTitle())),
                        withJsonPath("$.cases[0].offences[0].id", equalTo(defendantLevelResults.getOffenceId().toString())),
                        withJsonPath("$.cases[0].offences[0].offenceLevelResults", hasSize(0))
                ))).thatMatchesSchema()
        ));
    }

    @Test
    public void shouldGetResultsDetailsWhenOffenceLevelResultsAreAvailable() {
        final HearingResult offenceLevelResults = hearingResultDetail(OFFENCE);

        when(hearingResultRepository.findByHearingIdAndPersonId(HEARING_ID, PERSON_ID)).thenReturn(
                asList(offenceLevelResults)
        );

        final JsonEnvelope query = envelopeFrom(metadataWithRandomUUIDAndName(), createObjectBuilder()
                .add(FIELD_PERSON_ID, PERSON_ID.toString())
                .add(FIELD_HEARING_ID, HEARING_ID.toString())
                .build());

        final JsonEnvelope actualResultDetails = resultsQueryView.getResultsDetails(query);

        assertThat(actualResultDetails, is(jsonEnvelope(
                withMetadataEnvelopedFrom(query)
                        .withName(RESPONSE_NAME_RESULTS_DETAILS),
                payloadIsJson(allOf(
                        withJsonPath("$.hearingId", equalTo(HEARING_ID.toString())),
                        withJsonPath("$.personId", equalTo(PERSON_ID.toString())),
                        withJsonPath("$.defendantLevelResults", hasSize(0)),
                        withJsonPath("$.cases[0].id", equalTo(offenceLevelResults.getCaseId().toString())),
                        withJsonPath("$.cases[0].urn", equalTo(offenceLevelResults.getUrn())),
                        withJsonPath("$.cases[0].caseLevelResults", hasSize(0)),
                        withJsonPath("$.cases[0].offences[0].offenceWording", equalTo(offenceLevelResults.getOffenceTitle())),
                        withJsonPath("$.cases[0].offences[0].id", equalTo(offenceLevelResults.getOffenceId().toString())),
                        withJsonPath("$.cases[0].offences[0].startDate", equalTo(offenceLevelResults.getStartDate().toString())),
                        withJsonPath("$.cases[0].offences[0].endDate", equalTo(offenceLevelResults.getEndDate().toString())),
                        withJsonPath("$.cases[0].offences[0].offenceLevelResults[0].label", equalTo(offenceLevelResults.getResultLabel())),
                        withJsonPath("$.cases[0].offences[0].offenceLevelResults[0].court", equalTo(offenceLevelResults.getCourt())),
                        withJsonPath("$.cases[0].offences[0].offenceLevelResults[0].courtRoom", equalTo(offenceLevelResults.getCourtRoom())),
                        withJsonPath("$.cases[0].offences[0].offenceLevelResults[0].orderedDate", equalTo(offenceLevelResults.getOrderedDate().toString())),
                        withJsonPath("$.cases[0].offences[0].offenceLevelResults[0].lastSharedDate", equalTo(offenceLevelResults.getLastSharedDateTime().toLocalDate().toString())),
                        withJsonPath("$.cases[0].offences[0].offenceLevelResults[0].prompts[0].label", equalTo(offenceLevelResults.getResultPrompts().get(0).getLabel())),
                        withJsonPath("$.cases[0].offences[0].offenceLevelResults[0].prompts[0].value", equalTo(offenceLevelResults.getResultPrompts().get(0).getValue()))
                ))).thatMatchesSchema()
        ));
    }

    @Test
    public void shouldGetResultsDetailsWhenAllLevelsHaveAvailableResults() {
        final HearingResult caseLevelResults = hearingResultDetail(CASE);
        final HearingResult defendantLevelResults = hearingResultDetail(DEFENDANT);
        final HearingResult offenceLevelResults = hearingResultDetail(OFFENCE);

        when(hearingResultRepository.findByHearingIdAndPersonId(HEARING_ID, PERSON_ID)).thenReturn(
                asList(offenceLevelResults, caseLevelResults, defendantLevelResults)
        );

        final JsonEnvelope query = envelopeFrom(metadataWithRandomUUIDAndName(), createObjectBuilder()
                .add(FIELD_PERSON_ID, PERSON_ID.toString())
                .add(FIELD_HEARING_ID, HEARING_ID.toString())
                .build());

        final JsonEnvelope actualResultDetails = resultsQueryView.getResultsDetails(query);

        assertThat(actualResultDetails, is(jsonEnvelope(
                withMetadataEnvelopedFrom(query)
                        .withName(RESPONSE_NAME_RESULTS_DETAILS),
                payloadIsJson(allOf(
                        withJsonPath("$.hearingId", equalTo(HEARING_ID.toString())),
                        withJsonPath("$.personId", equalTo(PERSON_ID.toString())),
                        withJsonPath("$.defendantLevelResults[0].label", equalTo(defendantLevelResults.getResultLabel())),
                        withJsonPath("$.defendantLevelResults[0].court", equalTo(defendantLevelResults.getCourt())),
                        withJsonPath("$.defendantLevelResults[0].courtRoom", equalTo(defendantLevelResults.getCourtRoom())),
                        withJsonPath("$.defendantLevelResults[0].orderedDate", equalTo(defendantLevelResults.getOrderedDate().toString())),
                        withJsonPath("$.defendantLevelResults[0].lastSharedDate", equalTo(defendantLevelResults.getLastSharedDateTime().toLocalDate().toString())),                        
                        withJsonPath("$.defendantLevelResults[0].prompts[0].label", equalTo(defendantLevelResults.getResultPrompts().get(0).getLabel())),
                        withJsonPath("$.defendantLevelResults[0].prompts[0].value", equalTo(defendantLevelResults.getResultPrompts().get(0).getValue())),
                        withJsonPath("$.cases[0].id", equalTo(offenceLevelResults.getCaseId().toString())),
                        withJsonPath("$.cases[0].urn", equalTo(offenceLevelResults.getUrn())),
                        withJsonPath("$.cases[0].caseLevelResults[0].label", equalTo(caseLevelResults.getResultLabel())),
                        withJsonPath("$.cases[0].caseLevelResults[0].court", equalTo(caseLevelResults.getCourt())),
                        withJsonPath("$.cases[0].caseLevelResults[0].courtRoom", equalTo(caseLevelResults.getCourtRoom())),
                        withJsonPath("$.cases[0].caseLevelResults[0].orderedDate", equalTo(caseLevelResults.getOrderedDate().toString())),
                        withJsonPath("$.cases[0].caseLevelResults[0].lastSharedDate", equalTo(caseLevelResults.getLastSharedDateTime().toLocalDate().toString())),                        
                        withJsonPath("$.cases[0].caseLevelResults[0].prompts[0].label", equalTo(caseLevelResults.getResultPrompts().get(0).getLabel())),
                        withJsonPath("$.cases[0].caseLevelResults[0].prompts[0].value", equalTo(caseLevelResults.getResultPrompts().get(0).getValue())),
                        withJsonPath("$.cases[0].offences[0].plea.pleaValue", equalTo(offenceLevelResults.getPleaValue())),
                        withJsonPath("$.cases[0].offences[0].plea.pleaDate", equalTo(offenceLevelResults.getPleaDate().toString())),
                        withJsonPath("$.cases[0].offences[0].offenceWording", equalTo(offenceLevelResults.getOffenceTitle())),
                        withJsonPath("$.cases[0].offences[0].id", equalTo(offenceLevelResults.getOffenceId().toString())),
                        withJsonPath("$.cases[0].offences[0].startDate", equalTo(offenceLevelResults.getStartDate().toString())),
                        withJsonPath("$.cases[0].offences[0].endDate", equalTo(offenceLevelResults.getEndDate().toString())),
                        withJsonPath("$.cases[0].offences[0].offenceLevelResults[0].label", equalTo(offenceLevelResults.getResultLabel())),
                        withJsonPath("$.cases[0].offences[0].offenceLevelResults[0].court", equalTo(offenceLevelResults.getCourt())),
                        withJsonPath("$.cases[0].offences[0].offenceLevelResults[0].courtRoom", equalTo(offenceLevelResults.getCourtRoom())),
                        withJsonPath("$.cases[0].offences[0].offenceLevelResults[0].orderedDate", equalTo(offenceLevelResults.getOrderedDate().toString())),
                        withJsonPath("$.cases[0].offences[0].offenceLevelResults[0].lastSharedDate", equalTo(offenceLevelResults.getLastSharedDateTime().toLocalDate().toString())),                        
                        withJsonPath("$.cases[0].offences[0].offenceLevelResults[0].prompts[0].label", equalTo(offenceLevelResults.getResultPrompts().get(0).getLabel())),
                        withJsonPath("$.cases[0].offences[0].offenceLevelResults[0].prompts[0].value", equalTo(offenceLevelResults.getResultPrompts().get(0).getValue())),
                        withJsonPath("$.cases[0].offences[0].verdict.verdictCategory", equalTo(offenceLevelResults.getVerdictCategory())),
                        withJsonPath("$.cases[0].offences[0].verdict.verdictDate", equalTo(offenceLevelResults.getVerdictDate().toString())),
                        withJsonPath("$.cases[0].offences[0].verdict.verdictDescription", equalTo(offenceLevelResults.getVerdictDescription())),
                        withJsonPath("$.cases[0].offences[0].convictionDate", equalTo(offenceLevelResults.getConvictionDate().toString()))

                ))).thatMatchesSchema()
        ));
    }

    @Test
    public void shouldNotReturnResultDetailsWhenResultsHaveNotBeenShared() {
        when(hearingRepository.findHearingByPersonIdAndHearingId(PERSON_ID, HEARING_ID)).thenReturn(null);

        final JsonEnvelope query = envelopeFrom(metadataWithRandomUUIDAndName(), createObjectBuilder()
                .add(FIELD_PERSON_ID, PERSON_ID.toString())
                .add(FIELD_HEARING_ID, HEARING_ID.toString())
                .build());

        final JsonEnvelope actualHearingResults = resultsQueryView.getResultsDetails(query);

        assertThat(actualHearingResults, is(jsonEnvelope(
                withMetadataEnvelopedFrom(query)
                        .withName(RESPONSE_NAME_RESULTS_DETAILS),
                payload(isJsonValueNull()))
//                .thatMatchesSchema() //TODO: uncomment when framework have fixed it
        ));

    }


    private HearingResultSummariesView hearingResultsView(final HearingResultSummary... hearingResults) {
        return new HearingResultSummariesView(Arrays.stream(hearingResults)
                .map(hr -> new HearingResultSummaryView.Builder(hr).build()).collect(toList()));
    }

    private Defendant defendant() {
        return Defendant.builder().withId(PERSON_ID).withHearingId(HEARING_ID).withFirstName(FIRST_NAME)
                .withLastName(LAST_NAME).withDateOfBirth(DATE_OF_BIRTH).withAddress1(ADDRESS_1).withAddress2(ADDRESS_2)
                .withAddress3(ADDRESS_3).withAddress4(ADDRESS_4).withPostCode(POST_CODE).build();
    }

    private Defendant personWithRequiredFieldsOnly() {
        return Defendant.builder().withId(PERSON_ID).withHearingId(HEARING_ID).withFirstName(FIRST_NAME)
                .withLastName(LAST_NAME).withDateOfBirth(null).withAddress1(ADDRESS_1).withAddress2(null)
                .withAddress3(null).withAddress4(null).withPostCode(null).build();
    }

    private Hearing hearing() {
        return hearingWithStartDate(HEARING_START_DATE);
    }

    private Hearing hearingWithStartDate(final LocalDate date) {
        return new Hearing(HEARING_ID, PERSON_ID, HEARING_TYPE, date, COURT_CENTRE_NAME, COURT_CODE, JUDGE_NAME, PROSECUTOR_NAME, DEFENCE_NAME);
    }

    private HearingResult hearingResultDetail(final ResultLevel resultLevel) {
        final UUID HEARING_RESULT_ID = randomUUID();
        return HearingResult.of(HEARING_RESULT)
                .withId(HEARING_RESULT_ID)
                .withResultLevel(resultLevel)
                .withResultPrompts(HEARING_RESULT.getResultPrompts()
                        .stream()
                        .map(prompt -> ResultPrompt.of(prompt).withHearingResultId(HEARING_RESULT_ID).build())
                        .collect(toList()))
                .build();
    }

    private HearingResult hearingResultDetailWithNoOffenceDetails(final ResultLevel resultLevel) {
        final UUID HEARING_RESULT_ID = randomUUID();
        return HearingResult.of(HEARING_RESULT)
                .withId(randomUUID())
                .withResultLevel(resultLevel)
                .withOffenceTitle(null)
                .withOffenceId(null)
                .withResultPrompts(HEARING_RESULT.getResultPrompts()
                        .stream()
                        .map(prompt -> ResultPrompt.of(prompt).withHearingResultId(HEARING_RESULT_ID).build())
                        .collect(toList()))
                .build();
    }

    private HearingResultSummary hearingResultWithDate(final LocalDate date) {
        return new HearingResultSummary(HEARING_ID, PERSON_ID, HEARING_TYPE, date, FIRST_NAME, LAST_NAME);
    }

}