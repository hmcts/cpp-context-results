package uk.gov.moj.cpp.results.persist.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "hearing_resulted_document")
public class HearingResultedDocument {

    @Id
    @Column(name = "hearing_id", unique = true)
    private UUID hearingId;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "payload")
    private String payload;

    public HearingResultedDocument() {
        // for JPA
    }

    public HearingResultedDocument(final UUID hearingId, final LocalDate startDate, final LocalDate endDate, final String payload) {
        this.hearingId = hearingId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.payload = payload;
    }

    public UUID getHearingId() {
        return hearingId;
    }

    public void setHearingId(UUID hearingId) {
        this.hearingId = hearingId;
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

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }
}
