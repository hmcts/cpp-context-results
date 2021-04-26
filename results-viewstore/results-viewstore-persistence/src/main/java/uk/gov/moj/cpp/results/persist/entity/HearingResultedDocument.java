package uk.gov.moj.cpp.results.persist.entity;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "hearing_resulted_document")
public class HearingResultedDocument {

    @EmbeddedId
    private HearingResultedDocumentKey id;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "payload")
    private String payload;

    public HearingResultedDocument() {
        // for JPA
    }

    public HearingResultedDocument(final HearingResultedDocumentKey id, final LocalDate startDate, final LocalDate endDate, final String payload) {
        this.id = id;
        this.startDate = startDate;
        this.endDate = endDate;
        this.payload = payload;
    }

    public HearingResultedDocumentKey getId() {
        return id;
    }

    public void setId(final HearingResultedDocumentKey id) {
        this.id = id;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(final LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }
}
