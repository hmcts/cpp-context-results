package uk.gov.moj.cpp.results.query.view;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.INTEGER;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.PAST_LOCAL_DATE;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.results.query.view.TestTemplates.templateApiProsecutionCase;
import static uk.gov.moj.cpp.results.query.view.TestTemplates.templateHearingResultsAdded;
import static uk.gov.moj.cpp.results.query.view.TestTemplates.templateProsecutionCase;

import uk.gov.justice.core.courts.HearingResultsAdded;
import uk.gov.justice.core.courts.external.ApiCourtCentre;
import uk.gov.justice.core.courts.external.ApiHearing;
import uk.gov.justice.core.courts.external.ApiHearingDay;
import uk.gov.justice.core.courts.external.ApiHearingType;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.random.RandomGenerator;
import uk.gov.moj.cpp.domains.HearingHelper;
import uk.gov.moj.cpp.domains.HearingTransformer;
import uk.gov.moj.cpp.results.persist.entity.HearingResultSummary;
import uk.gov.moj.cpp.results.query.view.service.HearingService;
import uk.gov.moj.cpp.results.query.view.service.UserGroupsService;

import java.time.LocalDate;
import java.util.Random;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

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

    @Spy
    private final Enveloper enveloper = createEnveloper();

    @Mock
    private HearingService hearingService;

    @Mock
    private HearingTransformer hearingTransformer;

    @Mock
    private UserGroupsService userGroupsService;

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;


    @InjectMocks
    private ResultsQueryView resultsQueryView;

    @Test
    public void shouldGetHearingDetails() {
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
    public void shouldGetHearingDetailsForHearingId() {
        final JsonEnvelope query = envelopeFrom(metadataWithRandomUUIDAndName(), createObjectBuilder()
                .add(FIELD_HEARING_ID, HEARING_ID.toString())
                .build());

        when(userGroupsService.findUserGroupsByUserId(query)).thenReturn(
                asList("Court Clerk", "Listing Officer")
        );

        HearingResultsAdded hearingResultsAdded = templateHearingResultsAdded();
        when(hearingService.findHearingForHearingId(HEARING_ID)).thenReturn(hearingResultsAdded);

        final ApiHearing.Builder apiHearingBuilder = ApiHearing.apiHearing()
                .withProsecutionCases(asList(templateApiProsecutionCase(templateProsecutionCase())))
                .withType(ApiHearingType.apiHearingType().withDescription(hearingResultsAdded.getHearing().getType().getDescription()).build())
                .withHearingDays(asList(ApiHearingDay.apiHearingDay().withSittingDay(hearingResultsAdded.getHearing().getHearingDays().get(0).getSittingDay()).build()))
                .withCourtCentre(ApiCourtCentre.apiCourtCentre().withId(hearingResultsAdded.getHearing().getCourtCentre().getId()).build());

        String dummyVal = randomUUID().toString();
        final JsonObject jsonResult = Json.createObjectBuilder().add("val", dummyVal).build();


        when(hearingTransformer.hearing(hearingResultsAdded.getHearing())).thenReturn(apiHearingBuilder);
        when(objectToJsonObjectConverter.convert(apiHearingBuilder.build())).thenReturn(jsonResult);

        final JsonEnvelope actualHearingResults = resultsQueryView.getHearingDetailsForHearingId(query);

        assertThat(actualHearingResults.payloadAsJsonObject().getString("val"), is(dummyVal));
    }

    private HearingResultSummary hearingResultWithDate(final LocalDate date) {
        return new HearingResultSummary(HEARING_ID, PERSON_ID, HEARING_TYPE, date, FIRST_NAME, LAST_NAME);
    }
}