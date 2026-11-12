package uk.gov.moj.cpp.results.event.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createReader;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataOf;

import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.featurecontrol.FeatureControlGuard;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.domains.HearingHelper;
import uk.gov.moj.cpp.results.event.helper.ApplicationFinalResultsEnricher;
import uk.gov.moj.cpp.results.event.service.CacheService;
import uk.gov.moj.cpp.results.event.service.EventGridService;
import uk.gov.moj.cpp.results.event.service.ReferenceDataService;

import java.io.StringReader;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonString;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class HearingResultedEventProcessorTest {

    @Mock
    private Sender sender;

    @Mock
    private HearingHelper hearingHelper;

    @Mock
    private CacheService cacheService;

    @Mock
    private EventGridService eventGridService;

    @Mock
    private FeatureControlGuard featureControlGuard;

    @InjectMocks
    private HearingResultedEventProcessor eventProcessor;

    @Mock
    private ReferenceDataService referenceDataService;

    @Mock
    private ApplicationFinalResultsEnricher applicationResultsEnricher;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeArgumentCaptor;


    // Referenced from the processor rather than restated, so a rename of the flag cannot leave the
    // production guard and these tests pointing at two different feature names.
    private static final String INFORMANT_REGISTER_SERVICE_FEATURE =
            HearingResultedEventProcessor.INFORMANT_REGISTER_SERVICE_FEATURE;
    private static final String ADD_HEARING_RESULT_FOR_DAY = "results.command.add-hearing-result-for-day";
    private static final String REQUEST_INFORMANT_REGISTER_PUBLISH = "results.command.request-informant-register-publish";

    private static final UtcClock clock = new UtcClock();

    private void givenTheInformantRegisterServiceFeatureIs(final boolean enabled) {
        when(featureControlGuard.isFeatureEnabled(INFORMANT_REGISTER_SERVICE_FEATURE)).thenReturn(enabled);
    }

    @Test
    public void shouldHandlePublicHearingResultedEvent() {
        final UUID userId = randomUUID();
        final UUID hearingId = randomUUID();
        final ZonedDateTime sharedTime = clock.now();
        final String hearingDay = "2021-03-15";
        final boolean reshare = false;

        final JsonObject hearing = createObjectBuilder()
                .add("id", hearingId.toString())
                .build();

        final JsonEnvelope event = createPublicEvent(userId, hearing, sharedTime, hearingDay, reshare);

        when(hearingHelper.transformedHearing(hearing)).thenReturn(createObjectBuilder().add("id", hearingId.toString()).build());
        when(applicationResultsEnricher.enrichIfApplicationResultsMissing(any(JsonObject.class))).thenAnswer(invocation -> invocation.getArgument(0));
        givenTheInformantRegisterServiceFeatureIs(true);

        eventProcessor.handleHearingResultedPublicEvent(event);

        verify(cacheService).add(eq("EXT_" + hearingId + "_2021-03-15_result_"), anyString());
        verify(cacheService).add(eq("INT_" + hearingId + "_2021-03-15_result_"), anyString());

        verify(sender, times(2)).sendAsAdmin(envelopeArgumentCaptor.capture());

        verify(eventGridService).sendHearingResultedForDayEvent(userId, hearingId.toString(), hearingDay, "Hearing_Resulted");

        final List<Envelope<JsonObject>> argumentCaptor = envelopeArgumentCaptor.getAllValues();
        final JsonEnvelope resultingCommand = envelopeFrom(argumentCaptor.get(0).metadata(), argumentCaptor.get(0).payload());
        assertThat(resultingCommand,
                jsonEnvelope(
                        metadata().withName(ADD_HEARING_RESULT_FOR_DAY),
                        payloadIsJson(allOf(
                                withJsonPath("$.hearing.id", is(hearingId.toString())),
                                withJsonPath("$.hearingDay", is(hearingDay)),
                                withJsonPath("$.sharedTime", is(ZonedDateTimes.toString(sharedTime))))
                        )));

        // The publish request goes last, after the resulting command. Both sends share this
        // delivery's transaction, so this is ordering, not isolation - a broker failure on either
        // leg rolls back both.
        final JsonEnvelope publishRequest = envelopeFrom(argumentCaptor.get(1).metadata(), argumentCaptor.get(1).payload());
        assertThat(publishRequest,
                jsonEnvelope(
                        metadata().withName(REQUEST_INFORMANT_REGISTER_PUBLISH),
                        payloadIsJson(allOf(
                                withJsonPath("$.hearingId", is(hearingId.toString())),
                                withJsonPath("$.hearingDay", is(hearingDay)),
                                withJsonPath("$.sharedTime", is(ZonedDateTimes.toString(sharedTime))),
                                withJsonPath("$.userId", is(userId.toString())))
                        )));
    }

    @Test
    public void shouldUseSJPEventTypeWhenSJPHearingResulted() {
        final UUID userId = randomUUID();
        final UUID hearingId = randomUUID();
        final ZonedDateTime sharedTime = clock.now();
        final String hearingDay = "2021-03-15";
        final boolean reshare = false;

        final JsonObject hearing = createObjectBuilder()
                .add("id", hearingId.toString())
                .add("isSJPHearing", true)
                .build();

        final JsonEnvelope event = createPublicEvent(userId, hearing, sharedTime, hearingDay, reshare);

        when(hearingHelper.transformedHearing(hearing)).thenReturn(createObjectBuilder()
                .add("id", hearingId.toString())
                .add("hearing", createObjectBuilder()
                        .add("isSJPHearing", true)
                        .build())
                .build());
        List<String> prosecutionIdsForCPS = new ArrayList<>();
        prosecutionIdsForCPS.add("mock1");
        prosecutionIdsForCPS.add("mock2");
        when(referenceDataService.getProsecutorIdForCPSFlagTrue()).thenReturn(prosecutionIdsForCPS);
        when(applicationResultsEnricher.enrichIfApplicationResultsMissing(any(JsonObject.class))).thenAnswer(invocation -> invocation.getArgument(0));
        eventProcessor.handleHearingResultedPublicEvent(event);

        verify(cacheService).add(eq("SJP_" + hearingId + "_2021-03-15_result_"), anyString());

        verify(sender).sendAsAdmin(envelopeArgumentCaptor.capture());

        verify(eventGridService).sendHearingResultedForDayEvent(userId, hearingId.toString(), hearingDay, "SJP_Hearing_Resulted");

        // Only the resulting command - SJP hearings are out of scope for the informant register, and
        // the branch short-circuits before the feature flag is even consulted.
        assertThat(envelopeArgumentCaptor.getValue().metadata().name(), is(ADD_HEARING_RESULT_FOR_DAY));
        verifyNoInteractions(featureControlGuard);
    }

    /**
     * Toggle off is the default and must be a complete no-op: no command sent, and the Event Grid and
     * Redis legs behave exactly as they did before the publish-request chain existed.
     */
    @Test
    public void shouldNotRequestInformantRegisterPublishWhenTheFeatureIsDisabled() {
        final UUID userId = randomUUID();
        final UUID hearingId = randomUUID();
        final ZonedDateTime sharedTime = clock.now();
        final String hearingDay = "2021-03-15";

        final JsonObject hearing = createObjectBuilder()
                .add("id", hearingId.toString())
                .build();

        final JsonEnvelope event = createPublicEvent(userId, hearing, sharedTime, hearingDay, false);

        when(hearingHelper.transformedHearing(hearing)).thenReturn(createObjectBuilder().add("id", hearingId.toString()).build());
        when(applicationResultsEnricher.enrichIfApplicationResultsMissing(any(JsonObject.class))).thenAnswer(invocation -> invocation.getArgument(0));
        givenTheInformantRegisterServiceFeatureIs(false);

        eventProcessor.handleHearingResultedPublicEvent(event);

        verify(sender, times(1)).sendAsAdmin(envelopeArgumentCaptor.capture());
        assertThat(envelopeArgumentCaptor.getValue().metadata().name(), is(ADD_HEARING_RESULT_FOR_DAY));

        verify(cacheService).add(eq("EXT_" + hearingId + "_2021-03-15_result_"), anyString());
        verify(cacheService).add(eq("INT_" + hearingId + "_2021-03-15_result_"), anyString());
        verify(eventGridService).sendHearingResultedForDayEvent(userId, hearingId.toString(), hearingDay, "Hearing_Resulted");
    }

    /**
     * The flag read is not worth a resulting. With the non-caching feature provider the lookup is a
     * remote fetch and the framework does not catch its failures, so it has to sit inside the catch:
     * outside it, a feature-store blip would propagate and roll back a delivery whose resulting
     * command has already been sent.
     */
    @Test
    public void shouldNotFailTheResultingWhenTheFeatureLookupThrows() {
        final UUID userId = randomUUID();
        final UUID hearingId = randomUUID();
        final ZonedDateTime sharedTime = clock.now();
        final String hearingDay = "2021-03-15";

        final JsonObject hearing = createObjectBuilder()
                .add("id", hearingId.toString())
                .build();

        final JsonEnvelope event = createPublicEvent(userId, hearing, sharedTime, hearingDay, false);

        when(hearingHelper.transformedHearing(hearing)).thenReturn(createObjectBuilder().add("id", hearingId.toString()).build());
        when(applicationResultsEnricher.enrichIfApplicationResultsMissing(any(JsonObject.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(featureControlGuard.isFeatureEnabled(INFORMANT_REGISTER_SERVICE_FEATURE))
                .thenThrow(new RuntimeException("feature store unavailable"));

        eventProcessor.handleHearingResultedPublicEvent(event);

        verify(sender, times(1)).sendAsAdmin(envelopeArgumentCaptor.capture());
        assertThat(envelopeArgumentCaptor.getValue().metadata().name(), is(ADD_HEARING_RESULT_FOR_DAY));

        verify(cacheService).add(eq("EXT_" + hearingId + "_2021-03-15_result_"), anyString());
        verify(cacheService).add(eq("INT_" + hearingId + "_2021-03-15_result_"), anyString());
        verify(eventGridService).sendHearingResultedForDayEvent(userId, hearingId.toString(), hearingDay, "Hearing_Resulted");
    }

    @Test
    public void shouldNotPublishToInformantRegisterQueueWithoutAUserId() {
        final UUID hearingId = randomUUID();
        final ZonedDateTime sharedTime = clock.now();
        final String hearingDay = "2021-03-15";

        final JsonObject hearing = createObjectBuilder()
                .add("id", hearingId.toString())
                .build();

        final JsonObjectBuilder resultPayload = createObjectBuilder()
                .add("isReshare", false)
                .add("hearingDay", hearingDay)
                .add("sharedTime", ZonedDateTimes.toString(sharedTime))
                .add("hearing", hearing);

        final JsonEnvelope event = envelopeFrom(metadataOf(randomUUID(), "public.events.hearing.hearing-resulted").build(),
                resultPayload.build());

        when(hearingHelper.transformedHearing(hearing)).thenReturn(createObjectBuilder().add("id", hearingId.toString()).build());
        when(applicationResultsEnricher.enrichIfApplicationResultsMissing(any(JsonObject.class))).thenAnswer(invocation -> invocation.getArgument(0));
        givenTheInformantRegisterServiceFeatureIs(true);

        eventProcessor.handleHearingResultedPublicEvent(event);

        verify(sender, times(1)).sendAsAdmin(envelopeArgumentCaptor.capture());
        assertThat(envelopeArgumentCaptor.getValue().metadata().name(), is(ADD_HEARING_RESULT_FOR_DAY));
    }

    /**
     * The consumer's contract types userId as a canonical uuid, so a metadata value that is not one
     * can only be dead-lettered on arrival - in another team's queue, for a defect that originated
     * here. It is rejected on this side instead, exactly as the Event Grid leg rejects it, and the
     * hearing-resulted command still goes out: a malformed identity must not cost the resulting.
     */
    @Test
    public void shouldNotPublishToInformantRegisterQueueWhenTheUserIdIsNotAUuid() {
        final UUID hearingId = randomUUID();
        final ZonedDateTime sharedTime = clock.now();
        final String hearingDay = "2021-03-15";

        final JsonObject hearing = createObjectBuilder()
                .add("id", hearingId.toString())
                .build();

        final JsonObjectBuilder resultPayload = createObjectBuilder()
                .add("isReshare", false)
                .add("hearingDay", hearingDay)
                .add("sharedTime", ZonedDateTimes.toString(sharedTime))
                .add("hearing", hearing);

        final JsonEnvelope event = envelopeFrom(
                metadataOf(randomUUID(), "public.events.hearing.hearing-resulted")
                        .withUserId("not-a-uuid")
                        .build(),
                resultPayload.build());

        when(hearingHelper.transformedHearing(hearing)).thenReturn(createObjectBuilder().add("id", hearingId.toString()).build());
        when(applicationResultsEnricher.enrichIfApplicationResultsMissing(any(JsonObject.class))).thenAnswer(invocation -> invocation.getArgument(0));
        givenTheInformantRegisterServiceFeatureIs(true);

        eventProcessor.handleHearingResultedPublicEvent(event);

        verify(sender, times(1)).sendAsAdmin(envelopeArgumentCaptor.capture());
        assertThat(envelopeArgumentCaptor.getValue().metadata().name(), is(ADD_HEARING_RESULT_FOR_DAY));
    }

    /**
     * Scope of this test, stated honestly: there is no transaction in scope here, so it proves only
     * that the processor returns normally and that both sends were attempted in order. It does NOT
     * prove the resulting survives - a broker-side send failure marks the real transaction
     * rollback-only and the whole hearing-resulted event is redelivered regardless of this catch.
     * What the catch does cover is the caller-side permanent failures (malformed userId, a payload
     * the command schema rejects), which throw before any JMS work; those are covered by the two
     * userId tests above.
     */
    @Test
    public void shouldContinueIfTheInformantRegisterPublishRequestFailsWhenHearingResulted() {
        final UUID userId = randomUUID();
        final UUID hearingId = randomUUID();
        final ZonedDateTime sharedTime = clock.now();
        final String hearingDay = "2021-03-15";

        final JsonObject hearing = createObjectBuilder()
                .add("id", hearingId.toString())
                .build();

        final JsonEnvelope event = createPublicEvent(userId, hearing, sharedTime, hearingDay, false);

        when(hearingHelper.transformedHearing(hearing)).thenReturn(createObjectBuilder().add("id", hearingId.toString()).build());
        when(applicationResultsEnricher.enrichIfApplicationResultsMissing(any(JsonObject.class))).thenAnswer(invocation -> invocation.getArgument(0));
        givenTheInformantRegisterServiceFeatureIs(true);

        // The resulting command goes first and succeeds; the publish request is the second send.
        // A plain RuntimeException stands in for anything the send can raise.
        doNothing().doThrow(new RuntimeException("command queue unavailable")).when(sender).sendAsAdmin(any(Envelope.class));

        eventProcessor.handleHearingResultedPublicEvent(event);

        verify(eventGridService).sendHearingResultedForDayEvent(userId, hearingId.toString(), hearingDay, "Hearing_Resulted");

        verify(sender, times(2)).sendAsAdmin(envelopeArgumentCaptor.capture());
        assertThat(envelopeArgumentCaptor.getAllValues().get(0).metadata().name(), is(ADD_HEARING_RESULT_FOR_DAY));
        assertThat(envelopeArgumentCaptor.getAllValues().get(1).metadata().name(), is(REQUEST_INFORMANT_REGISTER_PUBLISH));
    }

    @Test
    public void shouldContinueIfCacheServiceFailsWhenHearingResulted() {
        final UUID userId = randomUUID();
        final UUID hearingId = randomUUID();
        final ZonedDateTime sharedTime = clock.now();
        final String hearingDay = "2021-03-15";
        final boolean reshare = false;

        final JsonObject hearing = createObjectBuilder()
                .add("id", hearingId.toString())
                .build();

        final JsonEnvelope event = createPublicEvent(userId, hearing, sharedTime, hearingDay, reshare);

        when(hearingHelper.transformedHearing(hearing)).thenReturn(createObjectBuilder().add("id", hearingId.toString()).build());
        when(cacheService.add(eq("EXT_" + hearingId + "_2021-03-15_result_"), anyString())).thenThrow(new RuntimeException("Error"));
        when(applicationResultsEnricher.enrichIfApplicationResultsMissing(any(JsonObject.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Explicit: these tests assert a single sendAsAdmin, which only holds with the feature off.
        // Left implicit they would be toggle-off tests by accident, and an inverted default would
        // fail here rather than in the test that is actually about the toggle.
        givenTheInformantRegisterServiceFeatureIs(false);

        eventProcessor.handleHearingResultedPublicEvent(event);

        verify(cacheService).add(eq("EXT_" + hearingId + "_2021-03-15_result_"), anyString());
        verify(cacheService, never()).add(eq("INT_" + hearingId + "_2021-03-15_result_"), anyString());

        verify(eventGridService).sendHearingResultedForDayEvent(userId, hearingId.toString(), hearingDay, "Hearing_Resulted");

        verify(sender).sendAsAdmin(envelopeArgumentCaptor.capture());
    }

    @Test
    public void shouldContinueIfEventGridFailsWhenHearingResulted() {
        final UUID userId = randomUUID();
        final UUID hearingId = randomUUID();
        final ZonedDateTime sharedTime = clock.now();
        final String hearingDay = "2021-03-15";
        final boolean reshare = false;

        final JsonObject hearing = createObjectBuilder()
                .add("id", hearingId.toString())
                .build();

        final JsonEnvelope event = createPublicEvent(userId, hearing, sharedTime, hearingDay, reshare);

        when(hearingHelper.transformedHearing(hearing)).thenReturn(createObjectBuilder().add("id", hearingId.toString()).build());
        when(applicationResultsEnricher.enrichIfApplicationResultsMissing(any(JsonObject.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Explicit: these tests assert a single sendAsAdmin, which only holds with the feature off.
        // Left implicit they would be toggle-off tests by accident, and an inverted default would
        // fail here rather than in the test that is actually about the toggle.
        givenTheInformantRegisterServiceFeatureIs(false);

        eventProcessor.handleHearingResultedPublicEvent(event);

        verify(cacheService).add(eq("EXT_" + hearingId + "_2021-03-15_result_"), anyString());
        verify(cacheService).add(eq("INT_" + hearingId + "_2021-03-15_result_"), anyString());

        verify(eventGridService).sendHearingResultedForDayEvent(userId, hearingId.toString(), hearingDay, "Hearing_Resulted");

        verify(sender).sendAsAdmin(envelopeArgumentCaptor.capture());
    }

    @Test
    public void shouldExtractPoliceCases() {
        final UUID userId = randomUUID();
        final UUID hearingId = randomUUID();
        final ZonedDateTime sharedTime = clock.now();
        final String hearingDay = "2021-03-15";
        final boolean reshare = false;
        final String prosecutionId = "764bff92-a135-34cb-b858-8bb6b4b66301";

        final JsonObject caseIdentifier = createObjectBuilder()
                .add("id", prosecutionId)
                .add("prosecutionAuthorityId", "764bff92-a135-34cb-b858-8bb6b4b66301")
                .add("prosecutionAuthorityOUCode", "0450000")
                .build();
        final JsonObject prosecutionCase = createObjectBuilder()
                .add("id", prosecutionId)
                .add("prosecutionCaseIdentifier", caseIdentifier)
                .build();
        final JsonArrayBuilder prosecutionCases = createArrayBuilder()
                .add(prosecutionCase);

        final JsonObject hearing = createObjectBuilder()
                .add("id", hearingId.toString())
                .add("prosecutionCases", prosecutionCases.build())
                .build();

        when(referenceDataService.getPoliceFlag(Mockito.anyString(), Mockito.anyString())).thenReturn(true);
        final JsonArray policeProsecutionCases = eventProcessor.extractPoliceCases(hearing);

        assertThat(policeProsecutionCases.size(), is(1));
        assertThat(((JsonString) policeProsecutionCases.get(0)).getString(), is("764bff92-a135-34cb-b858-8bb6b4b66301"));
    }

    @Test
    public void shouldExtractPoliceCasesReturnSizeZero() {
        final UUID hearingId = randomUUID();
        final ZonedDateTime sharedTime = clock.now();
        final String hearingDay = "2021-03-15";
        final boolean reshare = false;

        final JsonObject hearing = createObjectBuilder()
                .add("id", hearingId.toString())
                .build();

        final JsonArray policeProsecutionCases = eventProcessor.extractPoliceCases(hearing);
        assertThat(policeProsecutionCases.size(), is(0));
    }

    @Test
    public void shouldExtractPoliceCasesNullProsecutionCase() {
        final UUID hearingId = randomUUID();
        final ZonedDateTime sharedTime = clock.now();
        final String hearingDay = "2021-03-15";
        final boolean reshare = false;
        final String prosecutionId = "764bff92-a135-34cb-b858-8bb6b4b66301";

        final JsonArrayBuilder prosecutionCases = createArrayBuilder();
        final JsonObject hearing = createObjectBuilder()
                .add("id", hearingId.toString())
                .add("prosecutionCases", prosecutionCases.build())
                .build();

        final JsonArray policeProsecutionCases = eventProcessor.extractPoliceCases(hearing);
        assertThat(policeProsecutionCases.size(), is(0));
    }

    @Test
    public void shouldExtractPoliceCasesNullProsecutionCaseIdentifier() {
        final UUID hearingId = randomUUID();
        final ZonedDateTime sharedTime = clock.now();
        final String hearingDay = "2021-03-15";
        final boolean reshare = false;
        final String prosecutionId = "764bff92-a135-34cb-b858-8bb6b4b66301";

        final JsonArrayBuilder prosecutionCases = createArrayBuilder();

        final JsonObject hearing = createObjectBuilder()
                .add("id", hearingId.toString())
                .add("prosecutionCases", prosecutionCases.build())
                .build();

        final JsonArray policeProsecutionCases = eventProcessor.extractPoliceCases(hearing);
        assertThat(policeProsecutionCases.size(), is(0));
    }

    @Test
    public void shouldExtractPoliceCasesSendNullToReferenceData() {
        final UUID hearingId = randomUUID();
        final ZonedDateTime sharedTime = clock.now();
        final String hearingDay = "2021-03-15";
        final boolean reshare = false;
        final String prosecutionId = "764bff92-a135-34cb-b858-8bb6b4b66301";

        final JsonObject caseIdentifier = createObjectBuilder()
                .add("id", prosecutionId)
                .build();
        final JsonObject prosecutionCase = createObjectBuilder()
                .add("id", prosecutionId)
                .add("prosecutionCaseIdentifier", caseIdentifier)
                .build();
        final JsonArrayBuilder prosecutionCases = createArrayBuilder()
                .add(prosecutionCase);

        final JsonObject hearing = createObjectBuilder()
                .add("id", hearingId.toString())
                .add("prosecutionCases", prosecutionCases.build())
                .build();

        when(referenceDataService.getPoliceFlag(null, null)).thenReturn(false);
        final JsonArray policeProsecutionCases = eventProcessor.extractPoliceCases(hearing);
        assertThat(policeProsecutionCases.size(), is(0));
    }

    @Test
    public void shouldHandlePublicHearingResultedEventForExtractPoliceCases() {
        final UUID userId = randomUUID();
        final UUID hearingId = randomUUID();
        final ZonedDateTime sharedTime = clock.now();
        final String hearingDay = "2021-03-15";
        final boolean reshare = false;

        final String prosecutionId = "764bff92-a135-34cb-b858-8bb6b4b66301";

        final JsonObject caseIdentifier = createObjectBuilder()
                .add("id", prosecutionId)
                .add("prosecutionAuthorityId", "764bff92-a135-34cb-b858-8bb6b4b66301")
                .add("prosecutionAuthorityOUCode", "0450000")
                .build();
        final JsonObject prosecutionCase = createObjectBuilder()
                .add("id", prosecutionId)
                .add("prosecutionCaseIdentifier", caseIdentifier)
                .build();
        final JsonArrayBuilder prosecutionCases = createArrayBuilder()
                .add(prosecutionCase);

        final JsonObject hearing = createObjectBuilder()
                .add("id", hearingId.toString())
                .add("prosecutionCases", prosecutionCases.build())
                .build();

        ArgumentCaptor<String> externalPayloadCaptor = ArgumentCaptor.forClass(String.class);

        final JsonEnvelope event = createPublicEvent(userId, hearing, sharedTime, hearingDay, reshare);

        when(hearingHelper.transformedHearing(hearing)).thenReturn(hearing);
        when(referenceDataService.getPoliceFlag(Mockito.anyString(), Mockito.anyString())).thenReturn(true);
        when(applicationResultsEnricher.enrichIfApplicationResultsMissing(any(JsonObject.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Explicit: these tests assert a single sendAsAdmin, which only holds with the feature off.
        // Left implicit they would be toggle-off tests by accident, and an inverted default would
        // fail here rather than in the test that is actually about the toggle.
        givenTheInformantRegisterServiceFeatureIs(false);

        eventProcessor.handleHearingResultedPublicEvent(event);

        verify(cacheService).add(eq("EXT_" + hearingId + "_2021-03-15_result_"), externalPayloadCaptor.capture());
        verify(cacheService).add(eq("INT_" + hearingId + "_2021-03-15_result_"), anyString());

        verify(eventGridService).sendHearingResultedForDayEvent(userId, hearingId.toString(), hearingDay, "Hearing_Resulted");

        verify(sender).sendAsAdmin(envelopeArgumentCaptor.capture());

        final String externalCaptor = externalPayloadCaptor.getValue();
        JsonReader jsonReader = createReader(new StringReader(externalCaptor));
        JsonObject externalPayload = jsonReader.readObject();
        jsonReader.close();

        JsonArray policeProsecutionCases = externalPayload.getJsonArray("policeCases");
        if (null != policeProsecutionCases && !policeProsecutionCases.isEmpty()) {
            for (int i = 0; i < policeProsecutionCases.size(); i++) {
                assertThat(prosecutionId, is(policeProsecutionCases.getString(i)));
            }
        }
    }

    private JsonEnvelope createPublicEvent(final UUID userId, final JsonObject hearing, final ZonedDateTime sharedTime, final String hearingDay,
                                           final boolean reshare) {
        final JsonObjectBuilder resultPayload = createObjectBuilder()
                .add("isReshare", reshare)
                .add("hearingDay", hearingDay)
                .add("sharedTime", ZonedDateTimes.toString(sharedTime))
                .add("hearing", hearing);

        return envelopeFrom(metadataOf(randomUUID(), "public.events.hearing.hearing-resulted")
                        .withUserId(userId.toString())
                        .build(),
                resultPayload.build());
    }
}