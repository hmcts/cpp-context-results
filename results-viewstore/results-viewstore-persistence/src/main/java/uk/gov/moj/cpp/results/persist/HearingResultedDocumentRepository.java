package uk.gov.moj.cpp.results.persist;

import uk.gov.moj.cpp.results.persist.entity.HearingResultedDocument;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@ApplicationScoped
public class HearingResultedDocumentRepository {

    private static final String HEARING_ID_PARAM = "hearingId";

    @PersistenceContext(unitName = "results-persistence-unit")
    EntityManager entityManager;

    public HearingResultedDocument save(final HearingResultedDocument hearingResultedDocument) {
        return entityManager.merge(hearingResultedDocument);
    }

    public List<HearingResultedDocument> findByFromDate(final LocalDate fromDate) {
        return entityManager.createQuery(
                        "select h from HearingResultedDocument h where h.endDate >= :fromDate", HearingResultedDocument.class)
                .setParameter("fromDate", fromDate)
                .getResultList();
    }

    public List<HearingResultedDocument> findByHearingId(final UUID hearingId) {
        return entityManager.createQuery(
                        "select h from HearingResultedDocument h where h.id.hearingId = :hearingId", HearingResultedDocument.class)
                .setParameter(HEARING_ID_PARAM, hearingId)
                .getResultList();
    }

    public Optional<HearingResultedDocument> findByHearingIdAndLatestHearingDay(final UUID hearingId) {
        return entityManager.createQuery(
                        "select h from HearingResultedDocument h where h.id.hearingId = :hearingId and " +
                                "h.id.hearingDay = (select max(hh.id.hearingDay) from HearingResultedDocument hh where hh.id.hearingId = :hearingId)", HearingResultedDocument.class)
                .setParameter(HEARING_ID_PARAM, hearingId)
                .getResultStream()
                .findFirst();
    }

    public Optional<HearingResultedDocument> findByHearingIdAndHearingDay(final UUID hearingId, final LocalDate hearingDate) {
        return entityManager.createQuery(
                        "select h from HearingResultedDocument h where h.id.hearingId = :hearingId and h.id.hearingDay = :hearingDate", HearingResultedDocument.class)
                .setParameter(HEARING_ID_PARAM, hearingId)
                .setParameter("hearingDate", hearingDate)
                .getResultStream()
                .findFirst();
    }
}
