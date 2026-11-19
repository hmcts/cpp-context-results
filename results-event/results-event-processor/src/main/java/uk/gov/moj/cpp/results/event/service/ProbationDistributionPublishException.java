package uk.gov.moj.cpp.results.event.service;

/**
 * Raised when the probation results distribution command could not be published to the
 * probation.requests Service Bus queue. Deliberately unchecked and deliberately allowed to escape
 * the event processor: the framework rolls back the delivery, redelivers, and dead-letters on
 * exhaustion, so the owed publish is recoverable from the DLQ rather than lost in a log line.
 */
public class ProbationDistributionPublishException extends RuntimeException {

    private static final long serialVersionUID = 4127703815920386471L;

    public ProbationDistributionPublishException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
