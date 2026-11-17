package uk.gov.moj.cpp.results.event.notifygateway;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.results.event.azure.AzureResourceResolver;
import uk.gov.moj.cpp.results.event.service.NotifyGatewayResultService;
import uk.gov.moj.cpp.results.event.service.exception.MalformedResultMessageException;

import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotifyGatewayResultConsumerTest {

    @Mock
    private AzureResourceResolver azureResourceResolver;

    @Mock
    private NotifyGatewayServiceBusConfiguration configuration;

    @Mock
    private NotifyGatewayResultService notifyGatewayResultService;

    @Mock
    private ServiceBusProcessorClient processorClient;

    @Mock
    private ServiceBusReceivedMessageContext context;

    @Mock
    private ServiceBusReceivedMessage message;

    @InjectMocks
    private NotifyGatewayResultConsumer consumer;

    @Test
    void shouldStartTheProcessorClientOnTheReplyQueueAndCloseItOnShutdown() {
        when(azureResourceResolver.serviceBusProcessorClient(eq(configuration), any(), any())).thenReturn(processorClient);

        consumer.init();
        consumer.cleanup();

        verify(processorClient).start();
        verify(processorClient).close();
    }

    @Test
    void shouldNotFailOnShutdownWhenTheProcessorClientWasNeverStarted() {
        // a fresh instance: @InjectMocks would inject the processor client mock into the client field
        final NotifyGatewayResultConsumer neverStarted = new NotifyGatewayResultConsumer();

        assertDoesNotThrow(neverStarted::cleanup);
    }

    @Test
    void shouldReportAProcessorErrorWithoutFailing(@Mock final ServiceBusErrorContext errorContext) {
        when(errorContext.getEntityPath()).thenReturn("results-notification-result");
        when(errorContext.getException()).thenReturn(new IllegalStateException("connection lost"));

        assertDoesNotThrow(() -> consumer.processError(errorContext));

        verify(errorContext).getEntityPath();
        verify(errorContext).getException();
    }

    @Test
    void shouldCompleteAMessageThatIsRecorded() {
        when(context.getMessage()).thenReturn(message);

        consumer.processMessage(context);

        verify(notifyGatewayResultService).handle(message);
        verify(context).complete();
        verify(context, never()).deadLetter();
    }

    @Test
    void shouldDeadLetterAMalformedMessage() {
        when(context.getMessage()).thenReturn(message);
        doThrow(new MalformedResultMessageException("unparseable")).when(notifyGatewayResultService).handle(message);

        consumer.processMessage(context);

        verify(context).deadLetter();
        verify(context, never()).complete();
    }

    @Test
    void shouldLeaveAMessageThatFailsToBeRecordedForRedelivery() {
        when(context.getMessage()).thenReturn(message);
        doThrow(new IllegalStateException("transient")).when(notifyGatewayResultService).handle(message);

        assertThrows(IllegalStateException.class, () -> consumer.processMessage(context));

        verify(context, never()).complete();
        verify(context, never()).deadLetter();
    }
}
