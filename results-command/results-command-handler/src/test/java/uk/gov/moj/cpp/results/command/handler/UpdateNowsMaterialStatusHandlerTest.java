package uk.gov.moj.cpp.results.command.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloperWithEvents;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.fasterxml.jackson.databind.ObjectMapper;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.results.domain.aggregate.HearingResultAggregate;
import uk.gov.moj.cpp.results.domain.event.NowsMaterialStatusUpdated;

@RunWith(MockitoJUnitRunner.class)
public class UpdateNowsMaterialStatusHandlerTest {

    @Spy
    private final Enveloper enveloper = createEnveloperWithEvents(NowsMaterialStatusUpdated.class);
    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
    @InjectMocks
    private UpdateNowsMaterialStatusHandler updateNowsMaterialStatusHandler;
    @Mock
    private EventStream hearingAggregateEventStream;
    @Mock
    private EventStream hearingResultEventStream;
    @Mock
    private EventSource eventSource;
    @Mock
    private AggregateService aggregateService;
    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;
    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @BeforeClass
    public static void init() {
    }

    @Before
    public void setup() {
    }

    @Test
    public void testUpdateNowsMaterialStatus() throws Throwable {
        UUID hearingId = randomUUID();
        String materialId = randomUUID().toString();
        String status = "generated";
        final JsonObject requestPayload = createObjectBuilder()
                .add("hearingId", hearingId.toString())
                .add("materialId", materialId)
                .add("status", status)
                .build();

        final JsonEnvelope command = envelopeFrom(metadataWithRandomUUID("results.handler.update-nows-material-status"), requestPayload);

        when(this.eventSource.getStreamById(hearingId)).thenReturn(this.hearingResultEventStream);
        when(this.aggregateService.get(this.hearingResultEventStream, HearingResultAggregate.class)).thenReturn(new HearingResultAggregate());


        this.updateNowsMaterialStatusHandler.updateNowsMaterialStatus(command);

        assertThat(verifyAppendAndGetArgumentFrom(this.hearingResultEventStream), streamContaining(
                jsonEnvelope(
                        withMetadataEnvelopedFrom(command)
                                .withName("results.event.nows-material-status-updated"),
                        payloadIsJson(allOf(withJsonPath(format("$.%s", "hearingId"), equalTo(hearingId.toString())),
                                withJsonPath(format("$.%s", "materialId"), equalTo(materialId)),
                                withJsonPath(format("$.%s", "status"), equalTo(status)))
                        )))
        );
    }
}