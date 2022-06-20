package uk.gov.moj.cpp.results.event.processor;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.domains.HearingHelper;
import uk.gov.moj.cpp.results.event.service.CacheService;
import uk.gov.moj.cpp.results.event.service.EventGridService;
import uk.gov.moj.cpp.results.event.service.ReferenceDataService;

import javax.json.*;
import java.io.StringReader;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataOf;

@RunWith(MockitoJUnitRunner.class)
public class HearingResultedEventProcessorTest {

    @Mock
    private Sender sender;

    @Mock
    private HearingHelper hearingHelper;

    @Mock
    private CacheService cacheService;

    @Mock
    private EventGridService eventGridService;

    @InjectMocks
    private HearingResultedEventProcessor eventProcessor;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeArgumentCaptor;

    @Mock
    ReferenceDataService referenceDataService;

    private static final UtcClock clock = new UtcClock();

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

        eventProcessor.handleHearingResultedPublicEvent(event);

        verify(cacheService).add(eq("EXT_" + hearingId + "_2021-03-15_result_"), anyString());
        verify(cacheService).add(eq("INT_" + hearingId + "_2021-03-15_result_"), anyString());

        verify(sender).sendAsAdmin(envelopeArgumentCaptor.capture());

        verify(eventGridService).sendHearingResultedForDayEvent(userId, hearingId.toString(), hearingDay, "Hearing_Resulted");

        final List<Envelope<JsonObject>> argumentCaptor = envelopeArgumentCaptor.getAllValues();
        final JsonEnvelope allValues = envelopeFrom(argumentCaptor.get(0).metadata(), argumentCaptor.get(0).payload());
        assertThat(allValues,
                jsonEnvelope(
                        metadata().withName("results.command.add-hearing-result-for-day"),
                        payloadIsJson(allOf(
                                withJsonPath("$.hearing.id", is(hearingId.toString())),
                                withJsonPath("$.hearingDay", is(hearingDay)),
                                withJsonPath("$.sharedTime", is(ZonedDateTimes.toString(sharedTime))))
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

        eventProcessor.handleHearingResultedPublicEvent(event);

        verify(cacheService).add(eq("SJP_" + hearingId + "_2021-03-15_result_"), anyString());

        verify(sender).sendAsAdmin(envelopeArgumentCaptor.capture());

        verify(eventGridService).sendHearingResultedForDayEvent(userId, hearingId.toString(), hearingDay, "SJP_Hearing_Resulted");
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
        when(eventGridService.sendHearingResultedEvent(userId, hearingId.toString(), "Hearing_Resulted")).thenThrow(new RuntimeException("Error"));

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

        when(hearingHelper.transformedHearing(hearing)).thenReturn(createObjectBuilder().add("id", hearingId.toString()).build());
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

        when(hearingHelper.transformedHearing(hearing)).thenReturn(createObjectBuilder().add("id", hearingId.toString()).build());
        when(referenceDataService.getPoliceFlag(Mockito.anyString(), Mockito.anyString())).thenReturn(true);
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

        when(hearingHelper.transformedHearing(hearing)).thenReturn(createObjectBuilder().add("id", hearingId.toString()).build());
        when(referenceDataService.getPoliceFlag(null, null)).thenReturn(false);
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

        when(hearingHelper.transformedHearing(hearing)).thenReturn(createObjectBuilder().add("id", hearingId.toString()).build());
        when(referenceDataService.getPoliceFlag(null, null)).thenReturn(false);
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

        when(hearingHelper.transformedHearing(hearing)).thenReturn(createObjectBuilder().add("id", hearingId.toString()).build());
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
        when(eventGridService.sendHearingResultedEvent(userId, hearingId.toString(), "Hearing_Resulted")).thenThrow(new RuntimeException("Error"));

        eventProcessor.handleHearingResultedPublicEvent(event);

        verify(cacheService).add(eq("EXT_" + hearingId + "_2021-03-15_result_"), externalPayloadCaptor.capture());
        verify(cacheService).add(eq("INT_" + hearingId + "_2021-03-15_result_"), anyString());

        verify(eventGridService).sendHearingResultedForDayEvent(userId, hearingId.toString(), hearingDay, "Hearing_Resulted");

        verify(sender).sendAsAdmin(envelopeArgumentCaptor.capture());

        final String externalCaptor = externalPayloadCaptor.getValue();
        JsonReader jsonReader = Json.createReader(new StringReader(externalCaptor));
        JsonObject externalPayload = jsonReader.readObject();
        jsonReader.close();

        JsonArray policeProsecutionCases = externalPayload.getJsonArray("policeCases");
        if (null != policeProsecutionCases && !policeProsecutionCases.isEmpty()) {
            for (int i = 0; i < policeProsecutionCases.size(); i++) {
                assertThat(prosecutionId, is(policeProsecutionCases.getString(i)));
            }
        }
    }

    private JsonEnvelope createPublicEvent(final UUID userId, final JsonObject hearing, final ZonedDateTime sharedTime, final String hearingDay, final boolean reshare) {
        final JsonObject resultPayload = createObjectBuilder()
                .add("isReshare", reshare)
                .add("hearingDay", hearingDay)
                .add("sharedTime", ZonedDateTimes.toString(sharedTime))
                .add("hearing", hearing)
                .build();

        return envelopeFrom(metadataOf(randomUUID(), "public.events.hearing.hearing-resulted")
                        .withUserId(userId.toString())
                        .build(),
                resultPayload);
    }
}