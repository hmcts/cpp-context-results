package uk.gov.moj.cpp.results.command.handler;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.Captor;
import org.mockito.ArgumentCaptor;


import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.core.courts.HearingApplicationEjected;
import uk.gov.justice.core.courts.HearingCaseEjected;
import uk.gov.justice.core.courts.HearingResultsAdded;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.domains.results.shareresults.PublicHearingResulted;
import uk.gov.moj.cpp.results.domain.aggregate.ResultsAggregate;
import uk.gov.moj.cpp.results.test.TestTemplates;

import javax.json.Json;
import javax.json.JsonObject;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloperWithEvents;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataOf;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

@RunWith(MockitoJUnitRunner.class)
public class ResultsCommandHandlerTest {

    @Mock
    private EventStream eventStream;

    @Mock
    private EventSource eventSource;

    @Mock
    private AggregateService aggregateService;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Captor
    private ArgumentCaptor<Stream<JsonEnvelope>> streamArgumentCaptor;


    private static UUID metadataId;

    private static PublicHearingResulted shareResultsMessage;

    @Spy
    private final Enveloper enveloper = createEnveloperWithEvents(
            HearingResultsAdded.class, HearingCaseEjected.class, HearingApplicationEjected.class);

    @InjectMocks
    private ResultsCommandHandler resultsCommandHandler;

    @BeforeClass
    public static void init() {
        shareResultsMessage = TestTemplates.basicShareResultsTemplate();
        metadataId = randomUUID();
    }

    @Before
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        when(this.eventSource.getStreamById(shareResultsMessage.getHearing().getId())).thenReturn(this.eventStream);
    }

    @Test
    public void testSaveShareResults_shouldRaiseHearingResultsAdded() throws EventStreamException {

        when(this.aggregateService.get(this.eventStream, ResultsAggregate.class)).thenReturn(new ResultsAggregate());

        final JsonEnvelope envelope = envelopeFrom(metadataOf(metadataId, "results.add-hearing-result"),
                objectToJsonObjectConverter.convert(shareResultsMessage));

        this.resultsCommandHandler.addHearingResult(envelope);

        assertThat(verifyAppendAndGetArgumentFrom(this.eventStream), streamContaining(
                jsonEnvelope(
                        withMetadataEnvelopedFrom(envelope).withName("results.hearing-results-added"),
                        payloadIsJson(allOf(
                                withJsonPath("$.hearing.id", is(shareResultsMessage.getHearing().getId().toString()))
                                )
                        ))));

    }

    @Test
    public void testHandleCaseOrApplicationEjected_whenPayloadContainsCaseId_expectHearingCaseEjectedEvent() throws EventStreamException {

        when(this.aggregateService.get(this.eventStream, ResultsAggregate.class)).thenReturn(new ResultsAggregate());
        final UUID hearingId1 = randomUUID();
        final UUID hearingId2 = randomUUID();
        final UUID caseId = randomUUID();

        shareResultsMessage.getHearing().setId(hearingId1);

        final JsonEnvelope envelop1 = envelopeFrom(metadataOf(metadataId, "results.add-hearing-result"),
                objectToJsonObjectConverter.convert(shareResultsMessage));
        when(this.eventSource.getStreamById(shareResultsMessage.getHearing().getId())).thenReturn(this.eventStream);

        this.resultsCommandHandler.addHearingResult(envelop1);

        shareResultsMessage.getHearing().setId(hearingId2);

        final JsonEnvelope envelop2 = envelopeFrom(metadataOf(metadataId, "results.add-hearing-result"),
                objectToJsonObjectConverter.convert(shareResultsMessage));
        when(this.eventSource.getStreamById(shareResultsMessage.getHearing().getId())).thenReturn(this.eventStream);

        this.resultsCommandHandler.addHearingResult(envelop2);

        final String CASE_ID = "caseId";
        final String HEARING_IDS = "hearingIds";
        final JsonObject payload = Json.createObjectBuilder()
                .add(HEARING_IDS,
                        Json.createArrayBuilder().add(hearingId1.toString()).add(hearingId2.toString()).build())
                .add(CASE_ID, caseId.toString())
                .build();

        final JsonEnvelope envelope = envelopeFrom(metadataOf(metadataId, "results.case-or-application-ejected"),
                payload);
        when(this.eventSource.getStreamById(any())).thenReturn(this.eventStream);

        this.resultsCommandHandler.handleCaseOrApplicationEjected(envelope);

        verify(eventStream, times(4)).append(streamArgumentCaptor.capture());
        final List<JsonEnvelope> jsonEnvelopeList = convertStreamToEventList(streamArgumentCaptor.getAllValues());
        final List<JsonEnvelope> filterdJsonEnvelopList = jsonEnvelopeList.stream().filter(jsonEnvelope -> jsonEnvelope.metadata().name().equals("results.hearing-case-ejected")).collect(toList());

        final JsonObject hearingCaseEjectedPayload = filterdJsonEnvelopList.get(1).payloadAsJsonObject();
        final Metadata metadata = filterdJsonEnvelopList.get(1).metadata();
        Assert.assertThat(metadata.name(), is("results.hearing-case-ejected"));
        Assert.assertThat(hearingCaseEjectedPayload.getString(CASE_ID), is(caseId.toString()));

    }

    @Test
    public void testHandleCaseOrApplicationEjected_whenPayloadContainsApplicationId_expectHearingApplicatioNEjectedEvent() throws EventStreamException {

        when(this.aggregateService.get(this.eventStream, ResultsAggregate.class)).thenReturn(new ResultsAggregate());
        final UUID hearingId1 = randomUUID();
        final UUID hearingId2 = randomUUID();
        final UUID applicationId = randomUUID();

        shareResultsMessage.getHearing().setId(hearingId1);

        final JsonEnvelope envelop1 = envelopeFrom(metadataOf(metadataId, "results.add-hearing-result"),
                objectToJsonObjectConverter.convert(shareResultsMessage));
        when(this.eventSource.getStreamById(shareResultsMessage.getHearing().getId())).thenReturn(this.eventStream);

        this.resultsCommandHandler.addHearingResult(envelop1);

        shareResultsMessage.getHearing().setId(hearingId2);

        final JsonEnvelope envelop2 = envelopeFrom(metadataOf(metadataId, "results.add-hearing-result"),
                objectToJsonObjectConverter.convert(shareResultsMessage));
        when(this.eventSource.getStreamById(shareResultsMessage.getHearing().getId())).thenReturn(this.eventStream);

        this.resultsCommandHandler.addHearingResult(envelop2);

        final String APPLICATION_ID = "applicationId";
        final String HEARING_IDS = "hearingIds";
        final JsonObject payload = Json.createObjectBuilder()
                .add(HEARING_IDS,
                        Json.createArrayBuilder().add(hearingId1.toString()).add(hearingId2.toString()).build())
                .add(APPLICATION_ID, applicationId.toString())
                .build();

        final JsonEnvelope envelope = envelopeFrom(metadataOf(metadataId, "results.case-or-application-ejected"),
                payload);
        when(this.eventSource.getStreamById(any())).thenReturn(this.eventStream);

        this.resultsCommandHandler.handleCaseOrApplicationEjected(envelope);

        verify(eventStream, times(4)).append(streamArgumentCaptor.capture());
        final List<JsonEnvelope> jsonEnvelopeList = convertStreamToEventList(streamArgumentCaptor.getAllValues());
        final List<JsonEnvelope> filterdJsonEnvelopList = jsonEnvelopeList.stream().filter(jsonEnvelope -> jsonEnvelope.metadata().name().equals("results.hearing-application-ejected")).collect(toList());


        final JsonObject hearingCaseEjectedPayload = filterdJsonEnvelopList.get(1).payloadAsJsonObject();
        final Metadata metadata = filterdJsonEnvelopList.get(1).metadata();
        Assert.assertThat(metadata.name(), is("results.hearing-application-ejected"));
        Assert.assertThat(hearingCaseEjectedPayload.getString(APPLICATION_ID), is(applicationId.toString()));

    }

    private List<JsonEnvelope> convertStreamToEventList(final List<Stream<JsonEnvelope>> listOfStreams) {
        return listOfStreams.stream()
                .flatMap(jsonEnvelopeStream -> jsonEnvelopeStream).collect(toList());
    }

}

