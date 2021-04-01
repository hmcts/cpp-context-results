package uk.gov.moj.cpp.results.event.processor;

import static com.google.common.collect.ImmutableList.of;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataOf;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.results.event.helper.TestTemplate.buildAllocationDecision;
import static uk.gov.moj.cpp.results.test.TestTemplates.basicShareResultsWithMagistratesTemplate;

import uk.gov.justice.core.courts.BailStatus;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.sjp.results.PublicSjpResulted;
import uk.gov.moj.cpp.domains.HearingHelper;
import uk.gov.moj.cpp.domains.results.shareresults.PublicHearingResulted;
import uk.gov.moj.cpp.results.event.helper.BaseSessionStructureConverterForSjp;
import uk.gov.moj.cpp.results.event.helper.BaseStructureConverter;
import uk.gov.moj.cpp.results.event.helper.CaseDetailsConverterForSjp;
import uk.gov.moj.cpp.results.event.helper.CasesConverter;
import uk.gov.moj.cpp.results.event.helper.ReferenceCache;
import uk.gov.moj.cpp.results.event.helper.resultdefinition.Prompt;
import uk.gov.moj.cpp.results.event.helper.resultdefinition.ResultDefinition;
import uk.gov.moj.cpp.results.event.service.ApplicationParameters;
import uk.gov.moj.cpp.results.event.service.CacheService;
import uk.gov.moj.cpp.results.event.service.EventGridService;
import uk.gov.moj.cpp.results.event.service.NotificationNotifyService;
import uk.gov.moj.cpp.results.event.service.ReferenceDataService;
import uk.gov.moj.cpp.results.test.TestTemplates;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;

@RunWith(DataProviderRunner.class)
public class ResultsEventProcessorTest {

    private static final UUID PROMPT_ID = fromString("e4003b92-419b-4e47-b3f9-89a4bbd6741d");
    private static final UUID PROMPT_ID_1 = fromString("e4003b92-419b-4e47-b3f9-89a4bbd6742d");
    private static final UUID RESULT_ID = fromString("e4003b92-419b-4e47-b3f9-89a4bbd6743d");
    private static final UUID NATIONALITY_ID = fromString("dddd4444-1e20-4c21-916a-81a6c90239e5");
    private static final String OU_CODE = "GFL123";
    private static final String URN = "urn123";
    private static final String EMAIL_ADDRESS = "test@hmcts.net";
    private static final String COMMON_PLATFORM_URL = "http://xxx.xx.com";
    private static final String POLICE_TEMPLATE_ID = "781b970d-a13e-4440-97c3-ecf22a4540d5";
    private static final String FIELD_SEND_TO_ADDRESS = "sendToAddress";
    private static final String FIELD_PERSONALISATION = "personalisation";
    private static final String FIELD_URN = "URN";
    private static final String FIELD_COMMON_PLATFORM_URL = "common_platform_url";
    private static final String FIELD_TEMPLATE_ID = "templateId";
    private static final String FIELD_NOTIFICATION_ID = "notificationId";
    @Spy
    private final Enveloper enveloper = createEnveloper();

    @Mock
    ReferenceDataService referenceDataService;

    @Mock
    ReferenceCache referenceCache;

    @Mock
    private Sender sender;

    @Mock
    private HearingHelper hearingHelper;

    @Mock
    private CacheService cacheService;

    @Mock
    private EventGridService eventGridService;

    @Mock
    private Requester requester;

    @Mock
    private CasesConverter casesConverter;

    @Mock
    private BaseStructureConverter baseStructureConverter;

    @Mock
    private BaseSessionStructureConverterForSjp baseSessionStructureConverterForSjp;

    @Mock
    private CaseDetailsConverterForSjp caseDetailsConverterForSjp;

    @Mock
    private ApplicationParameters applicationParameters;

    @Mock
    private NotificationNotifyService notificationNotifyService;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @InjectMocks
    private ResultsEventProcessor resultsEventProcessor;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeArgumentCaptor;

    @Captor
    private ArgumentCaptor<JsonObject> jsonObjectArgumentCaptor;

    @Before
    public void setUp() {
        initMocks(this);
        setField(jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        final JsonObject result = createObjectBuilder()
                .add("isoCode", "USA")
                .add("id", NATIONALITY_ID.toString())
                .build();
        final JsonObject resultPayload = createObjectBuilder().add("countryNationality", createArrayBuilder()
                .add(
                        result)
                .build())
                .build();
        final JsonEnvelope envelope = envelopeFrom(metadataOf(randomUUID(), "results.hearing-results-added").build(), resultPayload);

        final JsonEnvelope envelopeForCourt = envelopeFrom(metadataOf(randomUUID(), "referencedata.query.local-justice-area-national-court-code-and-oucode-mapping").build(), createObjectBuilder().
                add("nationalCourtCode", "1234")
                .add("oucode", "B22KS00")
                .build());
        when(referenceCache.getNationalityById(any())).thenReturn(Optional.of(result));
        when(referenceCache.getResultDefinitionById(any(), any(), any())).thenReturn(buildResultDefinition());
        when(referenceDataService.getOrgainsationUnit(any(), any())).thenReturn(envelopeForCourt.payloadAsJsonObject());

    }

    @Test
    public void shouldForwardAsIsAsPrivateEventWhenHearingResulted() {

        final PublicHearingResulted shareResultsMessage = TestTemplates.basicShareResultsWithShadowListedOffencesTemplate();
        final String hearingId = UUID.randomUUID().toString();
        final UUID userId = UUID.randomUUID();
        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("public.hearing.resulted"),
                objectToJsonObjectConverter.convert(shareResultsMessage));
        final JsonObject jsonResult = Json.createObjectBuilder().add("val", randomUUID().toString()).build();
        final JsonObject transformedHearing = envelope.asJsonObject();
        when(hearingHelper.transformedHearing(envelope.payloadAsJsonObject().getJsonObject("hearing"))).thenReturn(transformedHearing.getJsonObject("hearing"));
        when(cacheService.add(hearingId, transformedHearing.getJsonObject("hearing").toString())).thenReturn("");
        when(eventGridService.sendHearingResultedEvent(userId, hearingId, "Hearing_Resulted")).thenReturn(true);

        resultsEventProcessor.hearingResulted(envelope);

        verify(sender).sendAsAdmin(envelopeArgumentCaptor.capture());

        final List<Envelope<JsonObject>> argumentCaptor = envelopeArgumentCaptor.getAllValues();

        final JsonEnvelope allValues = envelopeFrom(argumentCaptor.get(0).metadata(), argumentCaptor.get(0).payload());


        assertThat(allValues,
                jsonEnvelope(
                        metadata().withName("results.command.add-hearing-result"),
                        payloadIsJson(allOf(
                                withJsonPath("$.hearing.id", is(shareResultsMessage.getHearing().getId().toString())),
                                withJsonPath("$.hearing.prosecutionCases[0].originatingOrganisation", is(shareResultsMessage.getHearing().getProsecutionCases().get(0).getOriginatingOrganisation())),
                                withJsonPath("$.shadowListedOffences[0]", is(shareResultsMessage.getShadowListedOffences().get(0).toString())))
                        )));

    }

    @Test
    public void shouldIssueCreateResultCommandWhenHearingResulted() {

        final PublicHearingResulted shareResultsMessage = basicShareResultsWithMagistratesTemplate();

        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("results.hearing-results-added"),
                objectToJsonObjectConverter.convert(shareResultsMessage));

        resultsEventProcessor.hearingResultAdded(envelope);

        verify(sender).sendAsAdmin(envelopeArgumentCaptor.capture());

        final List<Envelope<JsonObject>> argumentCaptor = envelopeArgumentCaptor.getAllValues();

        final JsonEnvelope requestPayload = envelopeFrom(argumentCaptor.get(0).metadata(), argumentCaptor.get(0).payload());

        assertThat(requestPayload,
                jsonEnvelope(
                        metadata().withName("results.create-results"),
                        payloadIsJson(allOf(
                                withJsonPath("$.session.id", is(shareResultsMessage.getHearing().getId().toString())),
                                withJsonPath("$.session.sourceType", is("CC")),
                                withJsonPath("$.session.sessionDays.[0].sittingDay", is("2018-05-02T12:01:01.000Z")))

                        )));

    }

    @Test
    public void shouldRaiseAPublicEventPoliceResultGenerated() {
        final String id = "d4a8d314-9cf0-411f-84fc-af603b46a388";
        final JsonEnvelope jsonEnvelope = envelope()
                .with(metadataBuilder().withId(randomUUID()).withName("dummy"))
                .withPayloadOf(id, "id")
                .withPayloadOf(createObjectBuilder().add("id", id).build(), "courtCentre")
                .withPayloadOf(createObjectBuilder().add("id", id).build(), "sessionDays")
                .withPayloadOf("urn123", "urn")
                .withPayloadOf("d4a8d314-9cf0-411f-84fc-af603b46a38813", "caseId")
                .withPayloadOf(createObjectBuilder().add("id", id).build(), "defendant")
                .build();

        resultsEventProcessor.createResult(jsonEnvelope);

        verify(sender).sendAsAdmin(envelopeArgumentCaptor.capture());

        final Envelope<JsonObject> argumentCaptor = envelopeArgumentCaptor.getValue();

        final JsonEnvelope allValues = envelopeFrom(argumentCaptor.metadata(), argumentCaptor.payload());


        assertThat(
                allValues, jsonEnvelope(
                        metadata().withName("public.results.police-result-generated"),
                        payloadIsJson(allOf(
                                withJsonPath("$.id", is(id))))));

    }

    @Test
    public void shouldHandleTheEventPoliceNotificationRequested() {
        final UUID notificationId = randomUUID();
        final JsonEnvelope jsonEnvelope = envelope()
                .with(metadataBuilder().withId(randomUUID()).withName("dummy"))
                .withPayloadOf(URN, "urn")
                .withPayloadOf(EMAIL_ADDRESS, "policeEmailAddress")
                .withPayloadOf(notificationId, "notificationId")
                .build();

        when(referenceDataService.fetchPoliceEmailAddressForProsecutorOuCode(OU_CODE)).thenReturn(EMAIL_ADDRESS);
        when(applicationParameters.getCommonPlatformUrl()).thenReturn(COMMON_PLATFORM_URL);
        when(applicationParameters.getEmailTemplateId()).thenReturn(POLICE_TEMPLATE_ID);

        resultsEventProcessor.handlePoliceNotificationRequested(jsonEnvelope);

        verify(notificationNotifyService, times(1)).sendEmailNotification(
                Mockito.eq(jsonEnvelope), jsonObjectArgumentCaptor.capture());
        assertThat(jsonObjectArgumentCaptor.getValue().getString(FIELD_NOTIFICATION_ID), is(notificationId.toString()));
        assertThat(jsonObjectArgumentCaptor.getValue().getString(FIELD_TEMPLATE_ID), is(POLICE_TEMPLATE_ID));
        assertThat(jsonObjectArgumentCaptor.getValue().getString(FIELD_SEND_TO_ADDRESS), is(EMAIL_ADDRESS));
        assertThat(jsonObjectArgumentCaptor.getValue().getJsonObject(FIELD_PERSONALISATION).getString(FIELD_URN), is(URN));
        assertThat(jsonObjectArgumentCaptor.getValue().getJsonObject(FIELD_PERSONALISATION).getString(FIELD_COMMON_PLATFORM_URL), is(COMMON_PLATFORM_URL));

    }

    @Test
    public void shouldHandleCaseOrApplicationEjectedWhenPayloadContainsCaseIdExpectedCaseEjectedCommandRaisedWithCaseId() {
        final UUID hearingId1 = randomUUID();
        final UUID hearingId2 = randomUUID();
        final UUID caseId = randomUUID();
        final String PROSECUTION_CASE_ID = "prosecutionCaseId";
        final String HEARING_IDS = "hearingIds";
        final JsonObject payload = Json.createObjectBuilder()
                .add(HEARING_IDS,
                        Json.createArrayBuilder().add(hearingId1.toString()).add(hearingId2.toString()).build())
                .add(PROSECUTION_CASE_ID, caseId.toString())
                .build();
        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("public.progression.events.case-or-application-ejected"), payload);
        resultsEventProcessor.handleCaseOrApplicationEjected(envelope);

        verify(sender).sendAsAdmin(envelopeArgumentCaptor.capture());
        final Envelope<JsonObject> argumentCaptor = envelopeArgumentCaptor.getValue();

        final JsonEnvelope allValues = envelopeFrom(argumentCaptor.metadata(), argumentCaptor.payload());


        assertThat(
                allValues, jsonEnvelope(
                        metadata().withName("results.case-or-application-ejected"),
                        payloadIsJson(allOf(
                                withJsonPath("$.caseId", is(caseId.toString()))))));


    }

    @Test
    public void shouldHandleCaseOrApplicationEjectedWhenPayloadContainsApplicationIdExpectedCaseEjectedCommandRaisedWithApplicationId() {
        final UUID hearingId1 = randomUUID();
        final UUID hearingId2 = randomUUID();
        final UUID applicationId = randomUUID();
        final String APPLICATION_ID = "applicationId";
        final String HEARING_IDS = "hearingIds";
        final JsonObject payload = Json.createObjectBuilder()
                .add(HEARING_IDS,
                        Json.createArrayBuilder().add(hearingId1.toString()).add(hearingId2.toString()).build())
                .add(APPLICATION_ID, applicationId.toString())
                .build();
        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("public.progression.events.case-or-application-ejected"), payload);
        resultsEventProcessor.handleCaseOrApplicationEjected(envelope);

        verify(sender).sendAsAdmin(envelopeArgumentCaptor.capture());
        final Envelope<JsonObject> argumentCaptor = envelopeArgumentCaptor.getValue();

        final JsonEnvelope allValues = envelopeFrom(argumentCaptor.metadata(), argumentCaptor.payload());

        assertThat(
                allValues, jsonEnvelope(
                        metadata().withName("results.case-or-application-ejected"),
                        payloadIsJson(allOf(
                                withJsonPath("$.applicationId", is(applicationId.toString()))))));


    }

    @Test
    public void shouldContinueIfEventGridFailsWhenHearingResulted() {
        final PublicHearingResulted shareResultsMessage = basicShareResultsWithMagistratesTemplate();
        final String hearingId = shareResultsMessage.getHearing().getId().toString();
        final UUID userId = randomUUID();
        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("public.hearing.resulted"),
                objectToJsonObjectConverter.convert(shareResultsMessage));
        final JsonObject transformedHearing = envelope.asJsonObject();

        when(hearingHelper.transformedHearing(envelope.payloadAsJsonObject().getJsonObject("hearing"))).thenReturn(transformedHearing.getJsonObject("hearing"));
        when(cacheService.add(hearingId, transformedHearing.getJsonObject("hearing").toString())).thenReturn("");
        when(eventGridService.sendHearingResultedEvent(userId, hearingId, "Hearing_Resulted")).thenThrow(new RuntimeException("Error"));

        resultsEventProcessor.hearingResulted(envelope);

        verify(sender).sendAsAdmin(envelopeArgumentCaptor.capture());

    }

    @Test
    public void shouldUseLAAEventTypeWhenHearingResulted() {
        final PublicHearingResulted shareResultsMessage = basicShareResultsWithMagistratesTemplate(false, false);
        final String hearingId = shareResultsMessage.getHearing().getId().toString();
        final UUID userId = randomUUID();
        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("public.hearing.resulted"),
                objectToJsonObjectConverter.convert(shareResultsMessage));

        final JsonObject payload = envelope.asJsonObject();
        final JsonObject internalPayload = Json.createObjectBuilder()
                .add("hearing", payload.getJsonObject("hearing"))
                .add("sharedTime", payload.getJsonString("sharedTime"))
                .build();

        final JsonObject transformedHearing = payload.getJsonObject("hearing");

        when(hearingHelper.transformedHearing(envelope.payloadAsJsonObject().getJsonObject("hearing")))
                .thenReturn(transformedHearing);

        final JsonObject externalPayload = Json.createObjectBuilder()
                .add("hearing", transformedHearing)
                .add("sharedTime", payload.getJsonString("sharedTime"))
                .build();

        final String expectedCacheKeyExternal = "EXT_" + hearingId + "_result_";
        final String expectedCacheKeyInternal = "INT_" + hearingId + "_result_";

        when(cacheService.add(expectedCacheKeyExternal, externalPayload.toString())).thenReturn("");
        when(cacheService.add(expectedCacheKeyInternal, internalPayload.toString())).thenReturn("");

        when(eventGridService.sendHearingResultedEvent(userId, hearingId, "Hearing_Resulted")).thenReturn(true);

        resultsEventProcessor.hearingResulted(envelope);

        verify(cacheService, times(1)).add(expectedCacheKeyExternal, externalPayload.toString());
        verify(cacheService, times(1)).add(expectedCacheKeyInternal, internalPayload.toString());

        verify(sender).sendAsAdmin(envelopeArgumentCaptor.capture());
    }

    @Test
    public void shouldUseSJPEventTypeWhenHearingResulted() {
        final PublicHearingResulted shareResultsMessage = basicShareResultsWithMagistratesTemplate(false, true);
        final String hearingId = shareResultsMessage.getHearing().getId().toString();
        final UUID userId = randomUUID();
        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("public.hearing.resulted"),
                objectToJsonObjectConverter.convert(shareResultsMessage));

        final JsonObject payload = envelope.asJsonObject();

        JsonObject transformedHearing = payload.getJsonObject("hearing");
        transformedHearing = jsonObjectToBuilder(transformedHearing).add("isSJPHearing", true).build();

        when(hearingHelper.transformedHearing(envelope.payloadAsJsonObject().getJsonObject("hearing")))
                .thenReturn(transformedHearing);

        final JsonObject externalPayload = Json.createObjectBuilder()
                .add("hearing", transformedHearing)
                .add("sharedTime", payload.getJsonString("sharedTime"))
                .build();

        final String expectedCacheKeyExternal = "SJP_" + hearingId + "_result_";

        when(cacheService.add(expectedCacheKeyExternal, externalPayload.toString())).thenReturn("");

        when(eventGridService.sendHearingResultedEvent(userId, hearingId, "SJP_Hearing_Resulted")).thenReturn(true);

        resultsEventProcessor.hearingResulted(envelope);

        verify(cacheService, times(1)).add(expectedCacheKeyExternal, externalPayload.toString());

        verify(sender).sendAsAdmin(envelopeArgumentCaptor.capture());
    }

    @Test
    public void shouldForwardAsEventWhenSjpCaseResulted() {
        final BailStatus bailStatus = BailStatus.bailStatus().build();
        when(referenceCache.getBailStatusObjectByCode(any(), any())).thenReturn(Optional.of(bailStatus));
        when(referenceCache.getAllocationDecision(any(), anyString())).thenReturn(Optional.of(buildAllocationDecision()));

        final PublicSjpResulted sjpCaseResultedMessage = TestTemplates.basicSJPCaseResulted();

        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("public.sjp.case-resulted"),
                objectToJsonObjectConverter.convert(sjpCaseResultedMessage));

        resultsEventProcessor.sjpCaseResulted(envelope);

        verify(sender).sendAsAdmin(envelopeArgumentCaptor.capture());
        final String id = sjpCaseResultedMessage.getSession().getSessionId().toString();
        final Envelope<JsonObject> argumentCaptor = envelopeArgumentCaptor.getValue();

        final JsonEnvelope allValues = envelopeFrom(argumentCaptor.metadata(), argumentCaptor.payload());

        assertThat(
                allValues, jsonEnvelope(
                        metadata().withName("results.create-results"),
                        payloadIsJson(allOf(
                                withJsonPath("$.session.id", is(id)),
                                withJsonPath("$.session.sourceType", is("SJP"))
                        ))
                ));
    }

    private JsonObjectBuilder jsonObjectToBuilder(JsonObject jo) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        for (Map.Entry<String, JsonValue> entry : jo.entrySet()) {
            builder.add(entry.getKey(), entry.getValue());
        }
        return builder;
    }

    private ResultDefinition buildResultDefinition() {
        final ResultDefinition resultDefinition = new ResultDefinition();
        resultDefinition.setId(RESULT_ID);
        resultDefinition.setAdjournment("Y");
        resultDefinition.setLabel("label");
        resultDefinition.setShortCode("shortCode");
        resultDefinition.setLevel("level");
        resultDefinition.setRank(1);
        resultDefinition.setStartDate(new Date());
        resultDefinition.setEndDate(new Date());
        resultDefinition.setWelshLabel("welshLabel");
        resultDefinition.setIsAvailableForCourtExtract(true);
        resultDefinition.setFinancial("financial");
        resultDefinition.setCategory("A");
        resultDefinition.setCjsCode("cjsCode");
        resultDefinition.setConvicted("Y");
        resultDefinition.setVersion("1.0");
        resultDefinition.setUserGroups(of("1", "2"));
        resultDefinition.setPrompts(of(buildPrompt(PROMPT_ID), buildPrompt(PROMPT_ID_1)));
        return resultDefinition;
    }

    private Prompt buildPrompt(final UUID promptId) {
        final Prompt prompt = new Prompt();
        prompt.setDuration("duration");
        prompt.setLabel("label");
        prompt.setWelshLabel("welshLabel");
        prompt.setMandatory(false);
        prompt.setType("type");
        prompt.setSequence(2);
        prompt.setFixedListId(randomUUID());
        prompt.setReference("reference");
        prompt.setId(promptId);
        return prompt;
    }

}
