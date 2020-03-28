package uk.gov.moj.cpp.results.event.service;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.Integer.valueOf;
import static java.time.LocalDate.now;
import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataOf;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.AllocationDecision;
import uk.gov.justice.core.courts.BailStatus;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;
import uk.gov.moj.cpp.results.event.helper.resultdefinition.AllResultDefinitions;
import uk.gov.moj.cpp.results.event.helper.resultdefinition.ResultDefinition;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@SuppressWarnings("unused")
@RunWith(MockitoJUnitRunner.class)
public class ReferenceDataServiceTest {

    protected static final String GAFTL_00 = "GAFTL00";
    private static final String FIELD_CJS_OFFENCE_CODE = "cjsoffencecode";
    private static final String NATIONALITY_CODE = "USA";
    private static final UUID NATIONALITY_ID = randomUUID();
    private static final String FIELD_COURT_ID = "id";
    private static final String FIELD_MOT_ID = "id";
    private static final String FIELD_MOT_REASON_ID = "motReasonId";
    private static final String FIELD_MOT_REASONO_CODE = "code";
    private static final String FIELD_SEQ_NUM = "seqNum";
    private static final String FIELD_MOT_DESCRIPTION = "description";
    private static final String FIELD_VALID_FROM = "validFrom";
    private static final String FIELD_OFFENCE_ID = "offenceId";
    private final JsonEnvelope envelope = envelopeFrom(MetadataBuilderFactory.metadataWithRandomUUIDAndName(), createObjectBuilder().build());
    @Spy
    private final Enveloper enveloper = createEnveloper();
    @Mock
    private Requester requester;
    @InjectMocks
    private ReferenceDataService referenceDataService;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Before
    public void setUp() {
        initMocks(this);
        setField(jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    // TODO: What about queryParameter usage rather than uriParameter?
    @Test
    public void shouldRequestForOffenceByCjsOffenceCode() {
        final String cjsOffenceCode = "OF61131";
        final JsonEnvelope event = envelope()
                .with(metadataWithRandomUUIDAndName())
                .withPayloadOf(cjsOffenceCode, FIELD_CJS_OFFENCE_CODE)
                .build();
        referenceDataService.getOffenceByCjsCode(cjsOffenceCode, event);

        verify(requester).requestAsAdmin(envelopeArgumentCaptor.capture());
        assertThat(envelopeArgumentCaptor.getValue(), is(jsonEnvelope(
                withMetadataEnvelopedFrom(event)
                        .withName("referencedata.query.offences"),
                payloadIsJson(
                        withJsonPath("$.cjsoffencecode", equalTo(cjsOffenceCode))
                ))
        ));
        verifyNoMoreInteractions(requester);
    }

    @Test
    public void shouldReturnNationalityForCodeWhenFound() {
        final JsonObject nationality = createObjectBuilder()
                .add("isoCode", NATIONALITY_CODE)
                .add("id", NATIONALITY_ID.toString())
                .build();
        mockCountryNationalityResponseAndAssertOnResult(
                nationality,
                CoreMatchers.is(of(nationality)),
                NATIONALITY_ID);
    }

    @Test
    public void shouldHandleNationalitiesWithEmptyIsoCode() {
        mockCountryNationalityResponseAndAssertOnResult(
                createObjectBuilder()
                        .add("id", "22ef7a73-df50-4349-8c72-ca3b9ace6363")
                        .add("cjsCode", 0)
                        .build(),
                CoreMatchers.is(empty()),
                NATIONALITY_ID);
    }

    @Test
    public void shouldReturnEmptyOptionalWhenNationalityNotFoundForCode() {
        mockCountryNationalityResponseAndAssertOnResult(
                createObjectBuilder()
                        .add("isoCode", "foo")
                        .build(),
                CoreMatchers.is(empty()),
                NATIONALITY_ID);
    }

    @Test
    public void shouldReturnEmptyOptionalWhenNationalityIsNull() {
        mockCountryNationalityResponseAndAssertOnResult(
                createObjectBuilder()
                        .add("isoCode", "foo")
                        .build(),
                CoreMatchers.is(empty()),
                null);
    }

    @Test
    public void getAllResultDefinitions() {
        final UUID id = randomUUID();
        final LocalDate now = now();
        final AllResultDefinitions allResultDefinitions = new AllResultDefinitions()
                .setResultDefinitions(asList(
                        ResultDefinition.resultDefinition()
                                .setId(id)
                ));
        final JsonEnvelope event = envelopeFrom(metadataOf(randomUUID(), "public.hearing.resulted").build(), createObjectBuilder().add("on", now.toString()).build());
        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("referencedata.get-all-result-definitions"),
                objectToJsonObjectConverter.convert(allResultDefinitions));

        when(requester.requestAsAdmin(any())).thenReturn(envelope);
        referenceDataService.loadAllResultDefinitions(event, now);
        verify(requester).requestAsAdmin(envelopeArgumentCaptor.capture());

        assertThat(envelopeArgumentCaptor.getValue(), is(jsonEnvelope(
                withMetadataEnvelopedFrom(event)
                        .withName("referencedata.get-all-result-definitions"),
                payloadIsJson(allOf(
                        withJsonPath("$.on", equalTo(now.toString()))
                )))
        ));
        verifyNoMoreInteractions(requester);
    }

    @Test
    public void getResultDefinitionsById() {
        final LocalDate now = now();
        final UUID id = randomUUID();
        final JsonEnvelope event = envelopeFrom(metadataOf(randomUUID(), "public.hearing.resulted").build(), createObjectBuilder().add("on", now.toString()).build());
        referenceDataService.getResultDefinitionById(event, now, id);
        verify(requester).requestAsAdmin(envelopeArgumentCaptor.capture());

        assertThat(envelopeArgumentCaptor.getValue(), is(jsonEnvelope(
                withMetadataEnvelopedFrom(event)
                        .withName("referencedata.get-result-definition"),
                payloadIsJson(allOf(
                        withJsonPath("$.on", equalTo(now.toString())),
                        withJsonPath("$.resultDefinitionId", equalTo(id.toString()))
                )))
        ));
        verifyNoMoreInteractions(requester);
    }


    @Test
    public void shouldRequestForNationalCourtCodeAndOucodeByCourtId() {

        final String courtId = randomUUID().toString();
        final JsonEnvelope event = envelope()
                .with(metadataWithRandomUUIDAndName())
                .withPayloadOf(courtId, FIELD_COURT_ID)
                .build();
        referenceDataService.getOrgainsationUnit(courtId, event);

        verify(requester).requestAsAdmin(envelopeArgumentCaptor.capture());

        assertThat(envelopeArgumentCaptor.getValue(), is(jsonEnvelope(
                withMetadataEnvelopedFrom(event)
                        .withName("referencedata.query.organisation-unit.v2"),
                payloadIsJson(
                        withJsonPath("$.id", equalTo(courtId))
                ))
        ));
        verifyNoMoreInteractions(requester);
    }

    private void mockCountryNationalityResponseAndAssertOnResult(final JsonObject nationality, final Matcher nationalityMatcher, final UUID nationalityId) {

        final JsonArray nationalities = createArrayBuilder()
                .add(nationality)
                .build();
        final JsonObject responsePayload = createObjectBuilder().add("countryNationality", nationalities).build();
        final JsonEnvelope queryResponse = envelopeFrom(MetadataBuilderFactory.metadataWithRandomUUIDAndName(), responsePayload);

        when(requestCountryNationalities()).thenReturn(queryResponse);

        final Optional<JsonObject> nationalityResult = referenceDataService.getNationalityById(nationalityId, envelope);
        MatcherAssert.assertThat(nationalityResult, nationalityMatcher);
    }

    private Object requestCountryNationalities() {
        return requester.requestAsAdmin(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(envelope).withName("referencedata.query.country-nationality"),
                payloadIsJson(notNullValue()))));
    }

    @Test
    public void shouldReturnTrueWhenSpiOutFlagTrue() {

        final JsonObject responsePayload = createObjectBuilder().add("spiOutFlag", true).build();
        final JsonEnvelope queryResponse = envelopeFrom(MetadataBuilderFactory.metadataWithRandomUUIDAndName(), responsePayload);


        final String originatingOrganisation = "GAFTL00";

        when(requester.requestAsAdmin(any())).thenReturn(queryResponse);

        assertTrue(referenceDataService.getSpiOutFlagForProsecutorOucode(originatingOrganisation));

        verify(requester).requestAsAdmin(envelopeArgumentCaptor.capture());

        assertThat(envelopeArgumentCaptor.getValue(), is(jsonEnvelope(
                metadata()
                        .withName("referencedata.query.get.prosecutor.by.oucode"),
                payloadIsJson(allOf(
                        withJsonPath("$.oucode", equalTo(GAFTL_00)))
                )))
        );
    }

    @Test
    public void shouldReturnFalseWhenSpiOutFlagFalse() {

        final JsonObject responsePayload = createObjectBuilder().add("spiOutFlag", false).build();
        final JsonEnvelope queryResponse = envelopeFrom(MetadataBuilderFactory.metadataWithRandomUUIDAndName(), responsePayload);


        final String originatingOrganisation = "GAFTL00";

        when(requester.requestAsAdmin(any())).thenReturn(queryResponse);

        assertFalse(referenceDataService.getSpiOutFlagForProsecutorOucode(originatingOrganisation));

        verify(requester).requestAsAdmin(envelopeArgumentCaptor.capture());

        assertThat(envelopeArgumentCaptor.getValue(), is(jsonEnvelope(
                metadata()
                        .withName("referencedata.query.get.prosecutor.by.oucode"),
                payloadIsJson(allOf(
                        withJsonPath("$.oucode", equalTo(GAFTL_00)))
                )))
        );
    }

    @Test(expected = NullPointerException.class)
    public void shouldFailWhenSpiOutFlagNotFound() {

        final JsonObject responsePayload = createObjectBuilder().build();
        final JsonEnvelope queryResponse = envelopeFrom(MetadataBuilderFactory.metadataWithRandomUUIDAndName(), responsePayload);


        final String originatingOrganisation = "GAFTL00";

        when(requester.requestAsAdmin(any())).thenReturn(queryResponse);

        referenceDataService.getSpiOutFlagForProsecutorOucode(originatingOrganisation);
    }

    @Test
    public void shouldGetBailsStatusFromReferenceData() {
        final JsonObject bailStatus = createObjectBuilder()
                .add("statusCode", "A")
                .add("statusDescription", "Not applicable")
                .add("id", randomUUID().toString())
                .build();

        final JsonArray bailStatusArray = createArrayBuilder()
                .add(bailStatus)
                .build();

        final JsonObject responsePayload = createObjectBuilder().add("bailStatuses", bailStatusArray).build();

        final JsonEnvelope queryResponse = envelopeFrom(MetadataBuilderFactory.metadataWithRandomUUIDAndName(), responsePayload);
        when(requester.requestAsAdmin(any())).thenReturn(queryResponse);

        final JsonEnvelope context = envelopeFrom(MetadataBuilderFactory.metadataWithRandomUUIDAndName(), createObjectBuilder().build());
        final List<BailStatus> bailStatuses = referenceDataService.getAllBailStatuses(context);

        verify(requester).requestAsAdmin(envelopeArgumentCaptor.capture());
        assertThat(bailStatuses.size(), is(1));
        assertThat(bailStatuses.get(0).getId().toString(), is(bailStatus.getString("id")));
        assertThat(bailStatuses.get(0).getCode(), is(bailStatus.getString("statusCode")));
        assertThat(bailStatuses.get(0).getDescription(), is(bailStatus.getString("statusDescription")));
    }

    @Test
    public void shouldGetModeOfTrailReasonsFromReferenceData() {
        final JsonObject modeOfTrial = createObjectBuilder()
                .add(FIELD_MOT_ID, randomUUID().toString())
                .add(FIELD_MOT_REASON_ID, randomUUID().toString())
                .add(FIELD_MOT_REASONO_CODE, "B")
                .add(FIELD_SEQ_NUM, 10)
                .add(FIELD_MOT_DESCRIPTION, "motReason Desc  1")
                .add(FIELD_OFFENCE_ID, randomUUID().toString())
                .add(FIELD_VALID_FROM, "2019-06-05")
                .build();

        final JsonArray modeOfTrialArray = createArrayBuilder()
                .add(modeOfTrial)
                .build();

        final JsonObject responsePayload = createObjectBuilder().add("modeOfTrialReasons", modeOfTrialArray).build();

        final JsonEnvelope queryResponse = envelopeFrom(MetadataBuilderFactory.metadataWithRandomUUIDAndName(), responsePayload);
        when(requester.requestAsAdmin(any())).thenReturn(queryResponse);

        final JsonEnvelope context = envelopeFrom(MetadataBuilderFactory.metadataWithRandomUUIDAndName(), createObjectBuilder().build());
        final List<AllocationDecision> allocationDecisions = referenceDataService.getAllModeOfTrialReasons(context);

        verify(requester).requestAsAdmin(envelopeArgumentCaptor.capture());
        assertThat(allocationDecisions.size(), is(1));
        final AllocationDecision allocationDecision = allocationDecisions.get(0);
        assertThat(allocationDecision.getMotReasonId().toString(), is(modeOfTrial.getString(FIELD_MOT_ID)));
        assertThat(allocationDecision.getSequenceNumber(), is((valueOf(modeOfTrial.getInt(FIELD_SEQ_NUM)))));
        assertThat(allocationDecision.getMotReasonCode(), is(modeOfTrial.getString(FIELD_MOT_REASONO_CODE)));
        assertThat(allocationDecision.getMotReasonDescription(), is(modeOfTrial.getString(FIELD_MOT_DESCRIPTION)));
        assertThat(allocationDecision.getAllocationDecisionDate(), is(nullValue()));

    }
}