package uk.gov.moj.cpp.results.event.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.domains.HearingHelper;
import uk.gov.moj.cpp.domains.results.shareresults.PublicHearingResulted;
import uk.gov.moj.cpp.results.event.service.CacheService;
import uk.gov.moj.cpp.results.event.service.EventGridService;
import uk.gov.moj.cpp.results.test.TestTemplates;

import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

@RunWith(DataProviderRunner.class)
public class ResultsEventProcessorTest {

    @Spy
    private final Enveloper enveloper = createEnveloper();

    @Mock
    private Sender sender;

    @Mock
    private HearingHelper hearingHelper;

    @Mock
    private CacheService cacheService;

    @Mock
    private EventGridService eventGridService;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @InjectMocks
    private ResultsEventProcessor resultsEventProcessor;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor;

    @Before
    public void setUp() {
        initMocks(this);
        setField(jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void hearingResulted_shouldForwardAsIsAsPrivateEvent() {

        final PublicHearingResulted shareResultsMessage = TestTemplates.basicShareResultsTemplate();
        final String hearingId = UUID.randomUUID().toString();
        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("public.hearing.resulted"),
                objectToJsonObjectConverter.convert(shareResultsMessage));
        final JsonObject jsonResult = Json.createObjectBuilder().add("val", randomUUID().toString()).build();
        final JsonObject transformedHearing = envelope.asJsonObject() ;
        when(hearingHelper.transformedHearing(envelope.payloadAsJsonObject().getJsonObject("hearing"))).thenReturn(transformedHearing.getJsonObject("hearing"));
        when(cacheService.add(hearingId, transformedHearing.getJsonObject("hearing").toString())).thenReturn("");
        when(eventGridService.sendHearingResultedEvent(hearingId)).thenReturn(true);

        resultsEventProcessor.hearingResulted(envelope);

        verify(sender).sendAsAdmin(envelopeArgumentCaptor.capture());

        assertThat(
                envelopeArgumentCaptor.getValue(), jsonEnvelope(
                        metadata().withName("results.command.add-hearing-result"),
                        payloadIsJson(allOf(
                                withJsonPath("$.hearing.id", is(shareResultsMessage.getHearing().getId().toString()))))));

    }

    @Test
    public void testHandleCaseOrApplicationEjected_whenPayloadContainsCaseId_expectedCaseEjectedCommandRaisedWithCaseId() {
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

        assertThat(
                envelopeArgumentCaptor.getValue(), jsonEnvelope(
                        metadata().withName("results.case-or-application-ejected"),
                        payloadIsJson(allOf(
                                withJsonPath("$.caseId", is(caseId.toString()))))));


    }

    @Test
    public void testHandleCaseOrApplicationEjected_whenPayloadContainsApplicationId_expectedCaseEjectedCommandRaisedWithApplicationId() {
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

        assertThat(
                envelopeArgumentCaptor.getValue(), jsonEnvelope(
                        metadata().withName("results.case-or-application-ejected"),
                        payloadIsJson(allOf(
                                withJsonPath("$.applicationId", is(applicationId.toString()))))));


    }

    @Test
    public void hearingResulted_shouldContinueIfEventGridFails() {
        final PublicHearingResulted shareResultsMessage = TestTemplates.basicShareResultsTemplate();
        final String hearingId = shareResultsMessage.getHearing().getId().toString();

        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("public.hearing.resulted"),
                objectToJsonObjectConverter.convert(shareResultsMessage));
        final JsonObject transformedHearing = envelope.asJsonObject() ;

        when(hearingHelper.transformedHearing(envelope.payloadAsJsonObject().getJsonObject("hearing"))).thenReturn(transformedHearing.getJsonObject("hearing"));
        when(cacheService.add(hearingId, transformedHearing.getJsonObject("hearing").toString())).thenReturn("");
        when(eventGridService.sendHearingResultedEvent(hearingId)).thenThrow(new RuntimeException("Error"));

        resultsEventProcessor.hearingResulted(envelope);

        verify(sender).sendAsAdmin(envelopeArgumentCaptor.capture());

    }

    @Test
    public void hearingResulted_shouldUseCorrectHashKey() {
        final PublicHearingResulted shareResultsMessage = TestTemplates.basicShareResultsTemplate();
        final String hearingId = shareResultsMessage.getHearing().getId().toString();

        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("public.hearing.resulted"),
                objectToJsonObjectConverter.convert(shareResultsMessage));
        final JsonObject transformedHearing = envelope.asJsonObject() ;

        when(hearingHelper.transformedHearing(envelope.payloadAsJsonObject().getJsonObject("hearing"))).thenReturn(transformedHearing.getJsonObject("hearing"));

        final String expectedCacheKey = hearingId + "_result_";

        when(cacheService.add(expectedCacheKey, transformedHearing.getJsonObject("hearing").toString())).thenReturn("");

        when(eventGridService.sendHearingResultedEvent(hearingId)).thenReturn(true);

        resultsEventProcessor.hearingResulted(envelope);
        verify(cacheService, times(1)).add(expectedCacheKey, transformedHearing.getJsonObject("hearing").toString());

        verify(sender).sendAsAdmin(envelopeArgumentCaptor.capture());

    }
}
