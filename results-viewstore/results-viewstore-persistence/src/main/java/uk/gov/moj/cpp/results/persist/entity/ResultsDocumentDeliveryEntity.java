package uk.gov.moj.cpp.results.persist.entity;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;

/**
 * Completion tracking for a document results stores in its blob container, one row per
 * materialId. An NCES document can be sent to several SJP cases, each tracked in
 * {@code results_nces_sjp_case}.
 */
@Entity
@Table(name = "results_document_delivery")
public class ResultsDocumentDeliveryEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "material_id", nullable = false)
    private UUID materialId;

    @Column(name = "document_type", nullable = false)
    private String documentType;

    @Column(name = "created_at", nullable = false)
    private ZonedDateTime createdAt;

    @Column(name = "material_status")
    private String materialStatus;

    @Column(name = "payload_blob_uri")
    private String payloadBlobUri;

    @Column(name = "document_blob_uri")
    private String documentBlobUri;

    @Column(name = "email_status")
    private String emailStatus;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "results_nces_sjp_case", joinColumns = @JoinColumn(name = "material_id"))
    private List<NcesSjpCase> sjpCases = new ArrayList<>();

    public ResultsDocumentDeliveryEntity() {
    }

    public ResultsDocumentDeliveryEntity(final UUID materialId, final String documentType, final ZonedDateTime createdAt,
                                         final String payloadBlobUri, final String documentBlobUri) {
        this.materialId = materialId;
        this.documentType = documentType;
        this.createdAt = createdAt;
        this.payloadBlobUri = payloadBlobUri;
        this.documentBlobUri = documentBlobUri;
    }

    /**
     * Sets the status of the SJP case, if it is one this document was sent to; a case that is not
     * tracked is left alone.
     *
     * @return whether the case was tracked and its status updated
     */
    public boolean updateSjpStatus(final UUID caseId, final String sjpStatus) {
        final Optional<NcesSjpCase> sjpCase = findSjpCase(caseId);
        sjpCase.ifPresent(c -> c.setSjpStatus(sjpStatus));
        return sjpCase.isPresent();
    }

    public Optional<NcesSjpCase> findSjpCase(final UUID caseId) {
        return sjpCases.stream().filter(c -> c.getCaseId().equals(caseId)).findFirst();
    }

    public UUID getMaterialId() {
        return materialId;
    }

    public void setMaterialId(final UUID materialId) {
        this.materialId = materialId;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(final String documentType) {
        this.documentType = documentType;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getMaterialStatus() {
        return materialStatus;
    }

    public void setMaterialStatus(final String materialStatus) {
        this.materialStatus = materialStatus;
    }

    public String getPayloadBlobUri() {
        return payloadBlobUri;
    }

    public void setPayloadBlobUri(final String payloadBlobUri) {
        this.payloadBlobUri = payloadBlobUri;
    }

    public String getDocumentBlobUri() {
        return documentBlobUri;
    }

    public void setDocumentBlobUri(final String documentBlobUri) {
        this.documentBlobUri = documentBlobUri;
    }

    public String getEmailStatus() {
        return emailStatus;
    }

    public void setEmailStatus(final String emailStatus) {
        this.emailStatus = emailStatus;
    }

    public List<NcesSjpCase> getSjpCases() {
        return sjpCases;
    }

    public void setSjpCases(final List<NcesSjpCase> sjpCases) {
        this.sjpCases = sjpCases;
    }
}
