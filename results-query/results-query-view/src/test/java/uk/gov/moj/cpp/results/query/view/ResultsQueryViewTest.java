package uk.gov.moj.cpp.results.query.view;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.verify;
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
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.random.RandomGenerator;
import uk.gov.moj.cpp.domains.HearingTransformer;
import uk.gov.moj.cpp.results.persist.entity.DefendantTrackingStatus;
import uk.gov.moj.cpp.results.persist.entity.HearingResultSummary;
import uk.gov.moj.cpp.results.query.view.service.DefendantTrackingStatusService;
import uk.gov.moj.cpp.results.query.view.service.HearingService;
import uk.gov.moj.cpp.results.query.view.service.UserGroupsService;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

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
    private static final String RESPONSE_NAME_DEFENDANTS_TRACKING_STATUS = "results.get-defendants-tracking-status";

    private static final UUID PERSON_ID = randomUUID();
    private static final UUID DEFENDANT_ID = randomUUID();

    private static final UUID DEFENDANT_ID1 = randomUUID();
    private static final UUID DEFENDANT_ID2 = randomUUID();

    private static final UUID OFFENCE_ID1 = randomUUID();
    private static final UUID OFFENCE_ID2 = randomUUID();

    private static final ZonedDateTime WOA_LAST_MODIFIED_TIME = new UtcClock().now();
    private static final ZonedDateTime EM_LAST_MODIFIED_TIME = new UtcClock().now().plusDays(1);

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
    private static final String FIELD_DEFENDANT_IDS = "defendantIds";
    private static final String FIELD_DEFENDANTS = "defendants";
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


    private static final String FIELD_TRACKING_STATUS = "trackingStatus";
    private static final String FIELD_OFFENCE_ID = "offenceId";
    private static final String FIELD_EM_STATUS = "emStatus";
    private static final String FIELD_WOA_STATUS = "woaStatus";

    private static final String FIELD_EM_LAST_MODIFIED_TIME = "emLastModifiedTime";
    private static final String FIELD_WOA_LAST_MODIFIED_TIME = "woaLastModifiedTime";
    private static final String COMMA = ",";


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

    @Mock
    private DefendantTrackingStatusService defendantTrackingStatusService;

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
        final JsonObject jsonObject = Json.createObjectBuilder().add("val", dummyVal).build();


        when(hearingTransformer.hearing(hearingResultsAdded.getHearing())).thenReturn(apiHearingBuilder);
        when(objectToJsonObjectConverter.convert(apiHearingBuilder.build())).thenReturn(jsonObject);

        final JsonEnvelope actualHearingResults = resultsQueryView.getHearingDetailsForHearingId(query);

        assertThat(actualHearingResults.payloadAsJsonObject().getJsonObject("hearing").getString("val"), is(dummyVal));
        assertThat(actualHearingResults.payloadAsJsonObject().getString("sharedTime"), notNullValue());
    }

    @Test
    public void shouldGetEmptyHearingDetailsForHearingIdDoesntExists() {
        final JsonEnvelope query = envelopeFrom(metadataWithRandomUUIDAndName(), createObjectBuilder()
                .add(FIELD_HEARING_ID, HEARING_ID.toString())
                .build());

        when(userGroupsService.findUserGroupsByUserId(query)).thenReturn(
                asList("Court Clerk", "Listing Officer")
        );

        HearingResultsAdded hearingResultsAdded = templateHearingResultsAdded();
        when(hearingService.findHearingForHearingId(HEARING_ID)).thenReturn(null);

        final JsonEnvelope actualHearingResults = resultsQueryView.getHearingDetailsForHearingId(query);

        assertThat(actualHearingResults.payloadAsJsonObject(), is(notNullValue()));
    }

    @Test
    public void shouldGetEmptyHearingDetailsForHearingIdDoesntExistsForHearingDetailsInternal() {
        final JsonEnvelope query = envelopeFrom(metadataWithRandomUUIDAndName(), createObjectBuilder()
                .add(FIELD_HEARING_ID, HEARING_ID.toString())
                .build());

        when(userGroupsService.findUserGroupsByUserId(query)).thenReturn(
                asList("Court Clerk", "Listing Officer")
        );

        HearingResultsAdded hearingResultsAdded = templateHearingResultsAdded();
        when(hearingService.findHearingForHearingId(HEARING_ID)).thenReturn(null);

        final JsonEnvelope actualHearingResults = resultsQueryView.getHearingDetailsInternal(query);

        assertThat(actualHearingResults.payloadAsJsonObject(), is(notNullValue()));
    }

    @Test
    public void shouldReturnDefendantTrackingStatus() {
        final List<UUID> defendantIds = new ArrayList<>();
        defendantIds.add(DEFENDANT_ID1);
        defendantIds.add(DEFENDANT_ID2);

        final List<DefendantTrackingStatus> defendantTrackingStatusList = new ArrayList<>();
        defendantTrackingStatusList.add(getDefendant1TrackingStatus());
        defendantTrackingStatusList.add(getDefendant2TrackingStatus());

        final JsonEnvelope query = envelopeFrom(metadataWithRandomUUIDAndName()
                        .withName(RESPONSE_NAME_DEFENDANTS_TRACKING_STATUS),
                createObjectBuilder()
                        .add(FIELD_DEFENDANT_IDS, DEFENDANT_ID1 + COMMA + DEFENDANT_ID2)
                        .build());

        when(defendantTrackingStatusService
                .findDefendantTrackingStatus(defendantIds))
                .thenReturn(asList(getDefendant1TrackingStatus(), getDefendant2TrackingStatus()));

        final JsonEnvelope defendantsTrackingStatus = resultsQueryView.getDefendantsTrackingStatus(query);

        verify(defendantTrackingStatusService).findDefendantTrackingStatus(defendantIds);

        final JsonObject payload = defendantsTrackingStatus.payloadAsJsonObject();
        assertThat(payload, is(notNullValue()));
        assertThat(payload.getJsonArray(FIELD_DEFENDANTS).size(), is(2));

        final JsonObject defendantJO1 = payload.getJsonArray(FIELD_DEFENDANTS).getJsonObject(0);
        final JsonArray trackingJOArray = defendantJO1.getJsonArray(FIELD_TRACKING_STATUS);
        assertThat(trackingJOArray.size(), is(1));

        final JsonObject trackingJO1 = trackingJOArray.getJsonObject(0);
        assertThat(defendantJO1.getString(FIELD_DEFENDANT_ID), is(DEFENDANT_ID1.toString()));
        assertThat(trackingJO1.getString(FIELD_OFFENCE_ID), is(OFFENCE_ID1.toString()));
        assertThat(trackingJO1.getString(FIELD_EM_LAST_MODIFIED_TIME), is(EM_LAST_MODIFIED_TIME.toString()));
        assertThat(trackingJO1.getBoolean(FIELD_EM_STATUS), is(true));

        final JsonObject defendantJO2 = payload.getJsonArray(FIELD_DEFENDANTS).getJsonObject(1);
        final JsonArray trackingJOArray2 = defendantJO2.getJsonArray(FIELD_TRACKING_STATUS);
        assertThat(trackingJOArray2.size(), is(1));

        final JsonObject trackingJO2 = trackingJOArray2.getJsonObject(0);
        assertThat(defendantJO2.getString(FIELD_DEFENDANT_ID), is(DEFENDANT_ID2.toString()));
        assertThat(trackingJO2.getString(FIELD_OFFENCE_ID), is(OFFENCE_ID2.toString()));
        assertThat(trackingJO2.getString(FIELD_EM_LAST_MODIFIED_TIME), is(EM_LAST_MODIFIED_TIME.toString()));
        assertThat(trackingJO2.getBoolean(FIELD_EM_STATUS), is(true));
        assertThat(trackingJO2.getString(FIELD_WOA_LAST_MODIFIED_TIME), is(WOA_LAST_MODIFIED_TIME.toString()));
        assertThat(trackingJO2.getBoolean(FIELD_WOA_STATUS), is(true));
    }
    
    @Test
    public void shouldReturnDefendantTrackingStatusWhenWoaStatusIsNull() {
        final List<UUID> defendantIds = new ArrayList<>();
        defendantIds.add(DEFENDANT_ID1);

        final List<DefendantTrackingStatus> defendantTrackingStatusList = new ArrayList<>();
        defendantTrackingStatusList.add(getDefendant1TrackingStatus());

        final JsonEnvelope query = envelopeFrom(metadataWithRandomUUIDAndName()
                        .withName(RESPONSE_NAME_DEFENDANTS_TRACKING_STATUS),
                createObjectBuilder()
                        .add(FIELD_DEFENDANT_IDS, DEFENDANT_ID1.toString())
                        .build());

        when(defendantTrackingStatusService
                .findDefendantTrackingStatus(defendantIds))
                .thenReturn(asList(getDefendant1TrackingStatusWithNullWoaStatusAndTS()));

        final JsonEnvelope defendantsTrackingStatus = resultsQueryView.getDefendantsTrackingStatus(query);

        verify(defendantTrackingStatusService).findDefendantTrackingStatus(defendantIds);

        final JsonObject payload = defendantsTrackingStatus.payloadAsJsonObject();
        assertThat(payload, is(notNullValue()));
        assertThat(payload.getJsonArray(FIELD_DEFENDANTS).size(), is(1));

        final JsonObject defendantJO1 = payload.getJsonArray(FIELD_DEFENDANTS).getJsonObject(0);
        final JsonArray trackingJOArray = defendantJO1.getJsonArray(FIELD_TRACKING_STATUS);
        assertThat(trackingJOArray.size(), is(1));

        final JsonObject trackingJO1 = trackingJOArray.getJsonObject(0);
        assertThat(defendantJO1.getString(FIELD_DEFENDANT_ID), is(DEFENDANT_ID1.toString()));
        assertThat(trackingJO1.getString(FIELD_OFFENCE_ID), is(OFFENCE_ID1.toString()));
        assertThat(trackingJO1.getBoolean(FIELD_EM_STATUS), is(true));
        assertThat(trackingJO1.getString(FIELD_EM_LAST_MODIFIED_TIME), is(EM_LAST_MODIFIED_TIME.toString()));
        assertThat(trackingJO1.getBoolean(FIELD_WOA_STATUS), is(false));
        assertThat(trackingJO1.get(FIELD_WOA_LAST_MODIFIED_TIME), is(nullValue()));

    }

    private HearingResultSummary hearingResultWithDate(final LocalDate date) {
        return new HearingResultSummary(HEARING_ID, PERSON_ID, HEARING_TYPE, date, FIRST_NAME, LAST_NAME);
    }

    private DefendantTrackingStatus getDefendant1TrackingStatusWithNullWoaStatusAndTS() {
        final DefendantTrackingStatus defendantTrackingStatusEntity = new DefendantTrackingStatus();
        defendantTrackingStatusEntity.setDefendantId(DEFENDANT_ID1);
        defendantTrackingStatusEntity.setOffenceId(OFFENCE_ID1);
        defendantTrackingStatusEntity.setEmLastModifiedTime(EM_LAST_MODIFIED_TIME);
        defendantTrackingStatusEntity.setEmStatus(true);
        defendantTrackingStatusEntity.setWoaLastModifiedTime(null);
        defendantTrackingStatusEntity.setWoaStatus(null);

        return defendantTrackingStatusEntity;
    }

    private DefendantTrackingStatus getDefendant1TrackingStatus() {
        final DefendantTrackingStatus defendantTrackingStatusEntity = new DefendantTrackingStatus();
        defendantTrackingStatusEntity.setDefendantId(DEFENDANT_ID1);
        defendantTrackingStatusEntity.setOffenceId(OFFENCE_ID1);
        defendantTrackingStatusEntity.setEmLastModifiedTime(EM_LAST_MODIFIED_TIME);
        defendantTrackingStatusEntity.setEmStatus(true);
        defendantTrackingStatusEntity.setWoaLastModifiedTime(WOA_LAST_MODIFIED_TIME);
        defendantTrackingStatusEntity.setWoaStatus(true);

        return defendantTrackingStatusEntity;
    }

    private DefendantTrackingStatus getDefendant2TrackingStatus() {
        final DefendantTrackingStatus defendantTrackingStatusEntity = new DefendantTrackingStatus();
        defendantTrackingStatusEntity.setDefendantId(DEFENDANT_ID2);
        defendantTrackingStatusEntity.setOffenceId(OFFENCE_ID2);
        defendantTrackingStatusEntity.setEmLastModifiedTime(EM_LAST_MODIFIED_TIME);
        defendantTrackingStatusEntity.setWoaLastModifiedTime(WOA_LAST_MODIFIED_TIME);
        defendantTrackingStatusEntity.setEmStatus(true);
        defendantTrackingStatusEntity.setWoaStatus(true);
        return defendantTrackingStatusEntity;
    }

}