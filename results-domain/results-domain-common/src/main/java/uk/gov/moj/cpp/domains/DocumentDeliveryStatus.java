package uk.gov.moj.cpp.domains;

/**
 * Status of the material upload or an SJP filing of a tracked document, as kept in
 * results_document_delivery.material_status and results_nces_sjp_case.sjp_status; the email has its
 * own {@link EmailDeliveryStatus}.
 */
public enum DocumentDeliveryStatus {
    PENDING,
    SUCCESS,
    FAILED
}
