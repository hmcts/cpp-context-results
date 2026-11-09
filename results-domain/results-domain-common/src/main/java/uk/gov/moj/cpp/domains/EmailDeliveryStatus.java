package uk.gov.moj.cpp.domains;

/**
 * Status of the email that carries a tracked document's link, as kept in
 * results_document_delivery.email_status. NOT_REQUIRED is the outcome where no email is sent (no
 * recipient address).
 */
public enum EmailDeliveryStatus {
    NOT_REQUIRED,
    PENDING,
    SUCCESS,
    FAILED
}
