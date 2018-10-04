package uk.gov.moj.cpp.results.query.view;

import static com.jayway.jsonassert.impl.matcher.IsCollectionWithSize.hasSize;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Matchers.eq;
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
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.INTEGER;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.PAST_LOCAL_DATE;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.PAST_ZONED_DATE_TIME;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.UUID;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.randomEnum;
import static uk.gov.moj.cpp.domains.results.result.ResultLevel.CASE;
import static uk.gov.moj.cpp.domains.results.result.ResultLevel.DEFENDANT;
import static uk.gov.moj.cpp.domains.results.result.ResultLevel.OFFENCE;
import static uk.gov.moj.cpp.results.query.view.TestTemplates.templateHearingResultsAdded;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.random.RandomGenerator;
import uk.gov.moj.cpp.domains.results.result.ResultLevel;
import uk.gov.moj.cpp.results.domain.event.HearingResultsAdded;
import uk.gov.moj.cpp.results.persist.DefendantRepository;
import uk.gov.moj.cpp.results.persist.HearingRepository;
import uk.gov.moj.cpp.results.persist.HearingResultRepository;
import uk.gov.moj.cpp.results.persist.VariantDirectoryRepository;
import uk.gov.moj.cpp.results.persist.entity.Hearing;
import uk.gov.moj.cpp.results.persist.entity.HearingResult;
import uk.gov.moj.cpp.results.persist.entity.HearingResultSummary;
import uk.gov.moj.cpp.results.persist.entity.ResultPrompt;
import uk.gov.moj.cpp.results.query.view.response.DefendantView;
import uk.gov.moj.cpp.results.query.view.response.HearingResultSummariesView;
import uk.gov.moj.cpp.results.query.view.response.HearingResultSummaryView;
import uk.gov.moj.cpp.results.query.view.service.HearingService;
import uk.gov.moj.cpp.results.query.view.service.UserGroupsService;

import javax.json.Json;
import javax.json.JsonObject;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;

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
    private static final String FIELD_DEFENDANT_ID = "defendantId";
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

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @InjectMocks
    private ResultsQueryView resultsQueryView;

    @Test
    public void shouldGetHearingDetails() {

        UUID materialId1 = randomUUID();
        UUID materialId2 = randomUUID();

        final JsonEnvelope query = envelopeFrom(metadataWithRandomUUIDAndName(), createObjectBuilder()
                .add(FIELD_DEFENDANT_ID, DEFENDANT_ID.toString())
                .add(FIELD_HEARING_ID, HEARING_ID.toString())
                .build());

        when(userGroupsService.findUserGroupsByUserId(query)).thenReturn(
                asList("Court Clerk", "Listing Officer")
        );

        HearingResultsAdded hearingResultsAdded = templateHearingResultsAdded();
        when(hearingService.findHearingDetailsByHearingIdDefendantId(HEARING_ID, DEFENDANT_ID)).thenReturn(hearingResultsAdded);
        String dummyVal = randomUUID().toString();
        final JsonObject jsonResult = Json.createObjectBuilder().add("val", dummyVal).build();

        when(objectToJsonObjectConverter.convert(hearingResultsAdded)).thenReturn(jsonResult);

        final JsonEnvelope actualHearingResults = resultsQueryView.getHearingDetails(query);

        assertThat(actualHearingResults.payloadAsJsonObject().getString("val"), is(dummyVal));
    }

    @Test
    public void shouldGetHearingResultSummaries() {
        final LocalDate startDate = HEARING_RESULT.getStartDate();
        HearingResultSummariesView result = new HearingResultSummariesView(
                Arrays.asList(
                        new HearingResultSummaryView(UUID.next(), STRING.next(), LocalDate.now(), asList("ABC"),
                                new DefendantView(UUID.next(), STRING.next(), STRING.next()))
                )
        );
        when(hearingService.findHearingResultSummariesFromDate(eq(startDate)))
                .thenReturn(result);

        final String strFromDate = startDate.toString();
        final JsonEnvelope query = envelopeFrom(metadataWithRandomUUID(REQUEST_NAME_GET_RESULTS_SUMMARY), createObjectBuilder()
                .add(FIELD_FROM_DATE, strFromDate)
                .build());

        final JsonEnvelope actualHearingResults = resultsQueryView.getResultsSummary(query);

        assertThat(actualHearingResults, is(jsonEnvelope(
                withMetadataEnvelopedFrom(query)
                        .withName(RESPONSE_NAME_RESULTS_SUMMARY),
                payloadIsJson(allOf(
                        withJsonPath(format("$.%s", FIELD_RESULTS), hasSize(result.getResults().size())),
                        withJsonPath(format("$.%s[0].%s", FIELD_RESULTS, FIELD_HEARING_ID), equalTo(result.getResults().get(0).getHearingId().toString()))
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