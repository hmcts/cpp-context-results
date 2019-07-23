package uk.gov.moj.cpp.results.command.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloperWithEvents;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataOf;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

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
import uk.gov.moj.cpp.domains.results.shareresults.PublicHearingResulted;
import uk.gov.moj.cpp.results.domain.aggregate.ResultsAggregate;
import uk.gov.moj.cpp.results.test.TestTemplates;

import java.util.UUID;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

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

    private static UUID metadataId;

    private static PublicHearingResulted shareResultsMessage;

    @Spy
    private final Enveloper enveloper = createEnveloperWithEvents(
            HearingResultsAdded.class);

    @InjectMocks
    private ResultsCommandHandler resultsCommandHandler;

    @BeforeClass
    public static void init() {
        shareResultsMessage = TestTemplates.basicShareResultsTemplate();
        metadataId = UUID.randomUUID();
    }

    @Before
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
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
}