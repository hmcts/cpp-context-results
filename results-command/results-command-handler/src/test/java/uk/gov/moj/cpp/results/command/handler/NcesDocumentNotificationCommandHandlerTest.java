package uk.gov.moj.cpp.results.command.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloperWithEvents;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.spi.DefaultJsonEnvelope;
import uk.gov.moj.cpp.results.domain.aggregate.HearingFinancialResultsAggregate;
import uk.gov.moj.cpp.results.domain.event.NcesEmailNotification;
import uk.gov.moj.cpp.results.domain.event.NcesEmailNotificationRequested;

import java.util.UUID;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class NcesDocumentNotificationCommandHandlerTest {

    private static final String MATERIAL_ID = "materialId";
    private static final String MATERIAL_URL = "materialUrl";
    private static final String RESULTS_QUERY_NCES_EMAIL_NOTIFICATION_DETAILS = "results.query.nces-email-notification-details";

    @Spy
    private final Enveloper enveloper = createEnveloperWithEvents(NcesEmailNotificationRequested.class);

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    private final JsonObjectToObjectConverter jsonToObjectConverter = new JsonObjectToObjectConverter(objectMapper);

    @Spy
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);

    @Mock
    private Requester requester;

    @InjectMocks
    private NcesDocumentNotificationCommandHandler ncesDocumentNotificationCommandHandler;

    @Captor
    private ArgumentCaptor<DefaultJsonEnvelope> envelopeArgumentCaptor;

    @BeforeEach
    public void setup() {
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingFinancialResultsAggregate.class)).thenReturn(new HearingFinancialResultsAggregate());
        setField(this.jsonToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.ncesDocumentNotificationCommandHandler, "ncesEmailNotificationTemplateId", randomUUID().toString());
    }

    @Test
    public void shouldProcessNcesEmailNotificationCommand() throws EventStreamException {
        final UUID id = randomUUID();
        final UUID materialId = randomUUID();
        final JsonEnvelope jsonEnvelope = createJsonEnvelope(id, materialId);

        final NcesEmailNotification ncesEmailNotification = createNcesEmailNotification(materialId);
        final JsonEnvelope ncesEmailNotificationJsonEnvelope = createNcesEmailNotificationJsonEnvelope(ncesEmailNotification);

        when(requester.requestAsAdmin(any(), any())).thenAnswer(mock -> ncesEmailNotificationJsonEnvelope);
        when(this.aggregateService.get(this.eventStream, HearingFinancialResultsAggregate.class)).thenReturn(new HearingFinancialResultsAggregate());

        ncesDocumentNotificationCommandHandler.processNcesEmailNotification(jsonEnvelope);

        verify(requester).requestAsAdmin(envelopeArgumentCaptor.capture(), any());
        assertThat(envelopeArgumentCaptor.getValue(), is(jsonEnvelope(
                metadata().withId(id).withName(RESULTS_QUERY_NCES_EMAIL_NOTIFICATION_DETAILS),
                payloadIsJson(
                        withJsonPath("$.materialId", equalTo(ncesEmailNotification.getMaterialId().toString()))
                )))
        );
        verify(eventSource).getStreamById(any());
        verify(aggregateService).get(eventStream, HearingFinancialResultsAggregate.class);
        verify(enveloper).withMetadataFrom(jsonEnvelope);

    }

    private JsonEnvelope createJsonEnvelope(final UUID id, final UUID materialId) {
        final Metadata metadata = metadataBuilder()
                .withId(id)
                .withName("results.command.nces-document-notification")
                .build();
        final JsonObject payload = createObjectBuilder()
                .add(MATERIAL_ID, materialId.toString())
                .add(MATERIAL_URL, "http://localhost:8080/")
                .build();

        return JsonEnvelope.envelopeFrom(metadata, payload);
    }

    private NcesEmailNotification createNcesEmailNotification(final UUID materialId) {
        return NcesEmailNotification.ncesEmailNotification()
                .withNotificationId(randomUUID())
                .withMasterDefendantId(randomUUID())
                .withMaterialId(materialId)
                .withMaterialUrl("http://localhost:8080/")
                .withSendToAddress("mail@email.com")
                .withSubject("subject")
                .withTemplateId(randomUUID())
                .build();
    }

    private JsonEnvelope createNcesEmailNotificationJsonEnvelope(NcesEmailNotification ncesEmailNotification) {
        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withName(RESULTS_QUERY_NCES_EMAIL_NOTIFICATION_DETAILS)
                .build();

        return JsonEnvelope.envelopeFrom(metadata, objectToJsonObjectConverter.convert(ncesEmailNotification));

    }
}
