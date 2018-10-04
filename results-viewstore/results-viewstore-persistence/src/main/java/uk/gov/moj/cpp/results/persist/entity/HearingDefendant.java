package uk.gov.moj.cpp.results.persist.entity;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "hearing_defendant")
public class HearingDefendant {

    @EmbeddedId
    private HearingDefendantKey id;

    @ManyToOne
    @JoinColumn(name = "hearing_id", insertable = false, updatable = false)
    private HearingResultedDocument hearingResultedDocument;

    public HearingDefendant() {
        // for JPA
    }

    public HearingDefendantKey getId() {
        return id;
    }

    public void setId(HearingDefendantKey id) {
        this.id = id;
    }

    public HearingResultedDocument getHearingResultedDocument() {
        return hearingResultedDocument;
    }

    public void setHearingResultedDocument(HearingResultedDocument hearingResultedDocument) {
        this.hearingResultedDocument = hearingResultedDocument;
    }
}