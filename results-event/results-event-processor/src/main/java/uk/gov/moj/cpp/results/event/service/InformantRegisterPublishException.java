package uk.gov.moj.cpp.results.event.service;

/**
 * Raised when the informant register distribution command could not be published to the Service Bus
 * queue. Deliberately unchecked and deliberately allowed to escape the event processor: the
 * framework rolls back the delivery, redelivers, and dead-letters on exhaustion, so the owed publish
 * is recoverable from the DLQ rather than lost in a log line.
 */
public class InformantRegisterPublishException extends RuntimeException {

    private static final long serialVersionUID = 8154379125271563432L;

    public InformantRegisterPublishException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
