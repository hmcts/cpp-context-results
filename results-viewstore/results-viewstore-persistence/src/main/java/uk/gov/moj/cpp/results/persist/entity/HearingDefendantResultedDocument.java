package uk.gov.moj.cpp.results.persist.entity;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "hearing_defendant_document")
public class HearingDefendantResultedDocument {

    @EmbeddedId
    private HearingDefendantKey id;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "details_payload")
    private String detailsPayload;

    @Column(name = "summary_payload")
    private String summaryPayload;

    public HearingDefendantResultedDocument() {
        // for JPA
    }

    public HearingDefendantResultedDocument(final HearingDefendantKey id, final LocalDate startDate, final LocalDate endDate, final String detailsPayload, final String summaryPayload) {
        this.id = id;
        this.startDate = startDate;
        this.endDate = endDate;
        this.detailsPayload = detailsPayload;
        this.summaryPayload = summaryPayload;
    }

    public HearingDefendantKey getId() {
        return id;
    }

    public void setId(HearingDefendantKey id) {
        this.id = id;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getDetailsPayload() {
        return detailsPayload;
    }

    public void setDetailsPayload(String detailsPayload) {
        this.detailsPayload = detailsPayload;
    }

    public String getSummaryPayload() {
        return summaryPayload;
    }

    public void setSummaryPayload(String summaryPayload) {
        this.summaryPayload = summaryPayload;
    }
}
