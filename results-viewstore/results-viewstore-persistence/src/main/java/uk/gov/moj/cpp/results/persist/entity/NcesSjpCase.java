package uk.gov.moj.cpp.results.persist.entity;

import java.io.Serializable;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Embeddable;

/**
 * One SJP case an NCES document was sent to; a row of {@code results_nces_sjp_case}, owned by
 * {@link ResultsDocumentDeliveryEntity}.
 */
@Embeddable
public class NcesSjpCase implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "case_id", nullable = false)
    private UUID caseId;

    // PENDING, SUCCESS or FAILED (DocumentDeliveryStatus)
    @Column(name = "sjp_status")
    private String sjpStatus;

    public NcesSjpCase() {
    }

    public NcesSjpCase(final UUID caseId, final String sjpStatus) {
        this.caseId = caseId;
        this.sjpStatus = sjpStatus;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public void setCaseId(final UUID caseId) {
        this.caseId = caseId;
    }

    public String getSjpStatus() {
        return sjpStatus;
    }

    public void setSjpStatus(final String sjpStatus) {
        this.sjpStatus = sjpStatus;
    }
}
