package uk.gov.moj.cpp.results.persist;

import uk.gov.moj.cpp.results.persist.entity.HearingDefendantResultedDocument;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@ApplicationScoped
public class HearingDefendantResultedDocumentRepository {

    @PersistenceContext(unitName = "results-persistence-unit")
    EntityManager entityManager;

    public HearingDefendantResultedDocument save(final HearingDefendantResultedDocument hearingDefendantResultedDocument) {
        return entityManager.merge(hearingDefendantResultedDocument);
    }
}
