package uk.gov.moj.cpp.results.domain.aggregate;

import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;

import java.util.List;
import java.util.UUID;

/**
 * What a caller of results.command.record-document-delivery reports about a document's delivery;
 * every field but the material id is optional and only the fields present are applied.
 *
 * @param sjpCases the SJP cases the document was sent to (with PENDING), or the outcome of an SJP
 *                 filing (SUCCESS / FAILED)
 */
public record DocumentDeliveryRequest(UUID materialId,
                                      String documentType,
                                      String materialStatus,
                                      String payloadBlobUri,
                                      String documentBlobUri,
                                      String emailStatus,
                                      List<SjpCase> sjpCases) {

    public DocumentDeliveryRequest {
        sjpCases = isNull(sjpCases) ? emptyList() : List.copyOf(sjpCases);
    }

    /**
     * An SJP case, identified by its case id; the correlation id the document was filed with is
     * recorded when tracking starts.
     */
    public record SjpCase(UUID caseId, String sjpCorrelationId, String sjpStatus) {
    }
}
