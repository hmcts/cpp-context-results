package uk.gov.moj.cpp.results.command.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.results.domain.aggregate.DocumentDeliveryAggregate;
import uk.gov.moj.cpp.results.domain.aggregate.DocumentDeliveryRequest;
import uk.gov.moj.cpp.results.domain.event.DocumentDeliveryRecorded;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DocumentDeliveryCommandHandlerTest {

    private static final String RECORD_DOCUMENT_DELIVERY = "results.command.record-document-delivery";
    private static final String DOCUMENT_DELIVERY_RECORDED = "results.event.document-delivery-recorded";
    private static final String DOCUMENT_BLOB_URI = "http://localhost/results/sdg/documents/ENFDocumentOrder_1.pdf";

    private final UUID materialId = randomUUID();
    private final UUID caseId = randomUUID();

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    // registers the event so the handler can envelope it
    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(DocumentDeliveryRecorded.class);

    @InjectMocks
    private DocumentDeliveryCommandHandler documentDeliveryCommandHandler;

    // converts the command payload as the framework does before calling the handler
    private final JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(new ObjectMapperProducer().objectMapper());

    private DocumentDeliveryAggregate aggregate;

    @BeforeEach
    void setUp() {
        aggregate = new DocumentDeliveryAggregate();
    }

    @Test
    void shouldHandleTheRecordDocumentDeliveryCommand() {
        assertThat(documentDeliveryCommandHandler, isHandler(COMMAND_HANDLER)
                .with(method("recordDocumentDelivery").thatHandles(RECORD_DOCUMENT_DELIVERY)));
    }

    @Test
    void shouldStartTrackingOnTheMaterialStream() throws Exception {
        givenTheAggregate();

        documentDeliveryCommandHandler.recordDocumentDelivery(command(createObjectBuilder()
                .add("materialId", materialId.toString())
                .add("documentType", "NCES")
                .add("materialStatus", "PENDING")
                .add("documentBlobUri", DOCUMENT_BLOB_URI)
                .add("sjpCases", createArrayBuilder().add(createObjectBuilder()
                        .add("caseId", caseId.toString())
                        .add("sjpCorrelationId", "document-id")
                        .add("sjpStatus", "PENDING")))
                .build()));

        final Stream<JsonEnvelope> events = verifyAppendAndGetArgumentFrom(eventStream);
        assertThat(events, streamContaining(jsonEnvelope(
                metadata().withName(DOCUMENT_DELIVERY_RECORDED),
                payload().isJson(allOf(
                        withJsonPath("$.materialId", is(materialId.toString())),
                        withJsonPath("$.documentType", is("NCES")),
                        withJsonPath("$.materialStatus", is("PENDING")),
                        withJsonPath("$.documentBlobUri", is(DOCUMENT_BLOB_URI)),
                        withJsonPath("$.sjpCases[0].caseId", is(caseId.toString())),
                        withJsonPath("$.sjpCases[0].sjpCorrelationId", is("document-id")),
                        withJsonPath("$.sjpCases[0].sjpStatus", is("PENDING")))))));
    }

    @Test
    void shouldRecordThatNoEmailIsRequired() throws Exception {
        aggregate.recordDocumentDelivery(new DocumentDeliveryRequest(materialId, "NCES", "PENDING", null, DOCUMENT_BLOB_URI, null, List.of())).toList();
        givenTheAggregate();

        documentDeliveryCommandHandler.recordDocumentDelivery(command(createObjectBuilder()
                .add("materialId", materialId.toString())
                .add("emailStatus", "NOT_REQUIRED")
                .build()));

        final Stream<JsonEnvelope> events = verifyAppendAndGetArgumentFrom(eventStream);
        assertThat(events, streamContaining(jsonEnvelope(
                metadata().withName(DOCUMENT_DELIVERY_RECORDED),
                payload().isJson(allOf(
                        withJsonPath("$.materialId", is(materialId.toString())),
                        withJsonPath("$.emailStatus", is("NOT_REQUIRED")))))));
    }

    private void givenTheAggregate() {
        when(eventSource.getStreamById(materialId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, DocumentDeliveryAggregate.class)).thenReturn(aggregate);
    }

    private Envelope<DocumentDeliveryRequest> command(final JsonObject payload) {
        return envelopeFrom(metadataWithRandomUUID(RECORD_DOCUMENT_DELIVERY),
                jsonObjectToObjectConverter.convert(payload, DocumentDeliveryRequest.class));
    }
}
