package uk.gov.moj.cpp.results.persist;

import uk.gov.moj.cpp.results.persist.entity.DefendantTrackingStatus;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@ApplicationScoped
public class DefendantTrackingStatusRepository {

    @PersistenceContext(unitName = "results-persistence-unit")
    EntityManager entityManager;

    public DefendantTrackingStatus save(final DefendantTrackingStatus defendantTrackingStatus) {
        return entityManager.merge(defendantTrackingStatus);
    }

    public DefendantTrackingStatus findBy(final UUID id) {
        return entityManager.find(DefendantTrackingStatus.class, id);
    }

    public List<DefendantTrackingStatus> findAll() {
        return entityManager.createQuery("select defendantTrackingStatus from DefendantTrackingStatus defendantTrackingStatus", DefendantTrackingStatus.class).getResultList();
    }

    public void remove(final DefendantTrackingStatus defendantTrackingStatus) {
        entityManager.remove(entityManager.contains(defendantTrackingStatus)
                ? defendantTrackingStatus
                : entityManager.merge(defendantTrackingStatus));
    }

    public List<DefendantTrackingStatus> findActiveDefendantTrackingStatusByDefendantIds(final List<UUID> defendantIds) {
        return entityManager.createQuery(
                        "select defendantTrackingStatus FROM DefendantTrackingStatus defendantTrackingStatus where defendantTrackingStatus.defendantId in (:defendantIds) and (defendantTrackingStatus.emStatus = true or defendantTrackingStatus.woaStatus = true) ", DefendantTrackingStatus.class)
                .setParameter("defendantIds", defendantIds)
                .getResultList();
    }
}
