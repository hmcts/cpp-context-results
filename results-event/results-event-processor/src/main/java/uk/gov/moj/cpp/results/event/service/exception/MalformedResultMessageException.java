package uk.gov.moj.cpp.results.event.service.exception;

/**
 * A notify-gateway result message on results' own result queue that cannot be processed as-is: an
 * unparseable body, or a {@code subject} other than the three recognised event names
 * ({@code notification-sent}, {@code notification-failed}, {@code notification-validation-failed}).
 * Distinct from a transient failure (e.g. sending the record-document-delivery command failing in
 * {@code DocumentDeliveryStatusService}) - a transient failure is retried by leaving the message
 * uncompleted; this one is poison and is dead-lettered instead, since retrying it will never
 * succeed.
 */
public class MalformedResultMessageException extends RuntimeException {

    public MalformedResultMessageException(final String message) {
        super(message);
    }
}
