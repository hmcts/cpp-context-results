package uk.gov.moj.cpp.results.event.service;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.domains.EmailDeliveryStatus;
import uk.gov.moj.cpp.results.event.service.exception.MalformedResultMessageException;

import java.util.UUID;

import com.azure.core.util.BinaryData;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotifyGatewayResultServiceTest {

    @Mock
    private DocumentDeliveryStatusService documentDeliveryStatusService;

    @Mock
    private ServiceBusReceivedMessage message;

    @InjectMocks
    private NotifyGatewayResultService notifyGatewayResultService;

    @Test
    void shouldRecordTheEmailOfASentNotificationAsSuccess() {
        final UUID materialId = randomUUID();
        receive("notification-sent", createObjectBuilder()
                .add("notificationId", randomUUID().toString())
                .add("sentTime", "2026-11-17T10:00:00Z")
                .add("clientContext", "RESULTS_" + materialId)
                .build().toString());

        notifyGatewayResultService.handle(message);

        verify(documentDeliveryStatusService).recordEmailStatus(any(Metadata.class), eq(materialId), eq(EmailDeliveryStatus.SUCCESS));
    }

    @Test
    void shouldRecordTheEmailOfAFailedNotificationAsFailed() {
        final UUID materialId = randomUUID();
        receive("notification-failed", createObjectBuilder()
                .add("notificationId", randomUUID().toString())
                .add("failedTime", "2026-11-17T10:00:00Z")
                .add("errorMessage", "permanent-failure")
                .add("clientContext", "RESULTS_" + materialId)
                .build().toString());

        notifyGatewayResultService.handle(message);

        verify(documentDeliveryStatusService).recordEmailStatus(any(Metadata.class), eq(materialId), eq(EmailDeliveryStatus.FAILED));
    }

    @Test
    void shouldRecordTheEmailOfARejectedCommandAsValidationFailedByTheClientContextOfTheOriginalPayload() {
        final UUID materialId = randomUUID();
        final String originalEventPayload = createObjectBuilder()
                .add("notificationId", randomUUID().toString())
                .add("clientContext", "RESULTS_" + materialId)
                .build().toString();
        receive("notification-validation-failed", createObjectBuilder()
                .add("reason", "PAYLOAD_VALIDATION_FAILED")
                .add("details", "sendToAddress: must not be blank")
                .add("originalEventPayload", originalEventPayload)
                .build().toString());

        notifyGatewayResultService.handle(message);

        verify(documentDeliveryStatusService).recordEmailStatus(any(Metadata.class), eq(materialId), eq(EmailDeliveryStatus.VALIDATION_FAILED));
    }

    @Test
    void shouldIgnoreTheResultOfAnEmailWithoutTheResultsClientContext() {
        receive("notification-sent", createObjectBuilder()
                .add("notificationId", randomUUID().toString())
                .add("sentTime", "2026-11-17T10:00:00Z")
                .add("clientContext", "STAGINGDVLA_" + randomUUID())
                .build().toString());

        notifyGatewayResultService.handle(message);

        verifyNoInteractions(documentDeliveryStatusService);
    }

    @Test
    void shouldIgnoreTheResultOfAnEmailWithoutAClientContext() {
        receive("notification-failed", createObjectBuilder()
                .add("notificationId", randomUUID().toString())
                .add("failedTime", "2026-11-17T10:00:00Z")
                .add("errorMessage", "permanent-failure")
                .build().toString());

        notifyGatewayResultService.handle(message);

        verifyNoInteractions(documentDeliveryStatusService);
    }

    @Test
    void shouldRejectAnUnrecognisedSubject() {
        receive("notification-queued", createObjectBuilder()
                .add("clientContext", "RESULTS_" + randomUUID())
                .build().toString());

        assertThrows(MalformedResultMessageException.class, () -> notifyGatewayResultService.handle(message));
        verifyNoInteractions(documentDeliveryStatusService);
    }

    @Test
    void shouldRejectAnUnparseableBody() {
        when(message.getBody()).thenReturn(BinaryData.fromString("not json"));

        assertThrows(MalformedResultMessageException.class, () -> notifyGatewayResultService.handle(message));
        verifyNoInteractions(documentDeliveryStatusService);
    }

    @Test
    void shouldRejectAValidationFailureWhoseOriginalPayloadIsNotJson() {
        receive("notification-validation-failed", createObjectBuilder()
                .add("reason", "PAYLOAD_VALIDATION_FAILED")
                .add("details", "Unexpected character")
                .add("originalEventPayload", "not json")
                .build().toString());

        assertThrows(MalformedResultMessageException.class, () -> notifyGatewayResultService.handle(message));
        verifyNoInteractions(documentDeliveryStatusService);
    }

    private void receive(final String subject, final String body) {
        when(message.getSubject()).thenReturn(subject);
        when(message.getBody()).thenReturn(BinaryData.fromString(body));
    }
}
