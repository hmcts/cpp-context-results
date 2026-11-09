package uk.gov.moj.cpp.results.event.service;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.domains.DocumentDeliveryStatus.FAILED;
import static uk.gov.moj.cpp.domains.DocumentDeliveryStatus.PENDING;
import static uk.gov.moj.cpp.domains.DocumentDeliveryStatus.SUCCESS;
import static uk.gov.moj.cpp.domains.DocumentDeliveryType.NCES;
import static uk.gov.moj.cpp.domains.DocumentDeliveryType.POLICE_NOTIFICATION;
import static uk.gov.moj.cpp.results.event.service.DocumentDeliveryStatusService.RESULTS_COMMAND_RECORD_DOCUMENT_DELIVERY;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.domains.EmailDeliveryStatus;
import uk.gov.moj.cpp.results.event.service.DocumentDeliveryStatusService.SjpCaseDelivery;

import java.util.List;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DocumentDeliveryStatusServiceTest {

    private static final String PAYLOAD_BLOB_URI = "http://localhost/results/sdg/requests/POLICE_NOTIFICATION_HEARING_RESULTS1.html";
    private static final String DOCUMENT_BLOB_URI = "http://localhost/results/sdg/documents/ENFDocumentOrder_1.pdf";

    @Mock
    private Sender sender;

    @InjectMocks
    private DocumentDeliveryStatusService documentDeliveryStatusService;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeCaptor;

    private final Metadata originatingMetadata = metadataWithRandomUUID("results.event.nces-email-notification-requested").build();
    private final UUID materialId = randomUUID();
    private final UUID caseId = randomUUID();

    @Test
    void shouldStartTrackingAPoliceNotificationWithBothBlobUris() {
        documentDeliveryStatusService.startTracking(originatingMetadata, materialId, POLICE_NOTIFICATION, PAYLOAD_BLOB_URI, DOCUMENT_BLOB_URI, List.of(), null);

        final JsonObject payload = sentPayload();
        assertThat(payload.getString("materialId"), is(materialId.toString()));
        assertThat(payload.getString("documentType"), is("POLICE_NOTIFICATION"));
        assertThat(payload.getString("materialStatus"), is("PENDING"));
        assertThat(payload.getString("payloadBlobUri"), is(PAYLOAD_BLOB_URI));
        assertThat(payload.getString("documentBlobUri"), is(DOCUMENT_BLOB_URI));
        assertThat(payload.containsKey("sjpCases"), is(false));
        // the police email is decided later in the flow
        assertThat(payload.containsKey("emailStatus"), is(false));
    }

    @Test
    void shouldStartTrackingADocumentOfAFlowThatSendsNoEmail() {
        documentDeliveryStatusService.startTracking(originatingMetadata, materialId, NCES, null, DOCUMENT_BLOB_URI, List.of(),
                EmailDeliveryStatus.NOT_REQUIRED);

        assertThat(sentPayload().getString("emailStatus"), is("NOT_REQUIRED"));
    }

    @Test
    void shouldStartTrackingAnNcesDocumentWithItsSjpCasesPending() {
        documentDeliveryStatusService.startTracking(originatingMetadata, materialId, NCES, null, DOCUMENT_BLOB_URI,
                List.of(new SjpCaseDelivery(caseId, "document-id")), null);

        final JsonObject payload = sentPayload();
        assertThat(payload.containsKey("payloadBlobUri"), is(false));
        final JsonObject sjpCase = payload.getJsonArray("sjpCases").getJsonObject(0);
        assertThat(sjpCase.getString("caseId"), is(caseId.toString()));
        assertThat(sjpCase.getString("sjpCorrelationId"), is("document-id"));
        assertThat(sjpCase.getString("sjpStatus"), is(PENDING.name()));
    }

    @Test
    void shouldRecordTheMaterialStatus() {
        documentDeliveryStatusService.recordMaterialStatus(originatingMetadata, materialId, FAILED);

        final JsonObject payload = sentPayload();
        assertThat(payload.getString("materialStatus"), is("FAILED"));
        assertThat(payload.size(), is(2));
    }

    @Test
    void shouldRecordTheEmailStatus() {
        documentDeliveryStatusService.recordEmailStatus(originatingMetadata, materialId, EmailDeliveryStatus.SUCCESS);

        final JsonObject payload = sentPayload();
        assertThat(payload.getString("emailStatus"), is("SUCCESS"));
        assertThat(payload.size(), is(2));
    }

    @Test
    void shouldRecordThatNoEmailIsRequired() {
        documentDeliveryStatusService.recordEmailStatus(originatingMetadata, materialId, EmailDeliveryStatus.NOT_REQUIRED);

        assertThat(sentPayload().getString("emailStatus"), is("NOT_REQUIRED"));
    }

    @Test
    void shouldRecordTheSjpStatusOfACase() {
        documentDeliveryStatusService.recordSjpStatus(originatingMetadata, materialId, caseId, SUCCESS);

        final JsonObject sjpCase = sentPayload().getJsonArray("sjpCases").getJsonObject(0);
        assertThat(sjpCase.getString("caseId"), is(caseId.toString()));
        assertThat(sjpCase.getString("sjpStatus"), is("SUCCESS"));
    }

    private JsonObject sentPayload() {
        verify(sender).sendAsAdmin(envelopeCaptor.capture());
        final Envelope<JsonObject> envelope = envelopeCaptor.getValue();
        assertThat(envelope.metadata().name(), is(RESULTS_COMMAND_RECORD_DOCUMENT_DELIVERY));
        return envelope.payload();
    }
}
