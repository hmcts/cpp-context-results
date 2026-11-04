package uk.gov.moj.cpp.results.persist;

import uk.gov.moj.cpp.results.persist.entity.NcesEmailNotificationDetailsEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@ApplicationScoped
public class NcesEmailNotificationDetailsRepository {

    @PersistenceContext(unitName = "results-persistence-unit")
    EntityManager entityManager;

    public NcesEmailNotificationDetailsEntity save(final NcesEmailNotificationDetailsEntity ncesEmailNotificationDetailsEntity) {
        return entityManager.merge(ncesEmailNotificationDetailsEntity);
    }

    public List<NcesEmailNotificationDetailsEntity> findAll() {
        return entityManager.createQuery("select n from NcesEmailNotificationDetailsEntity n", NcesEmailNotificationDetailsEntity.class).getResultList();
    }

    public void remove(final NcesEmailNotificationDetailsEntity ncesEmailNotificationDetailsEntity) {
        entityManager.remove(entityManager.contains(ncesEmailNotificationDetailsEntity)
                ? ncesEmailNotificationDetailsEntity
                : entityManager.merge(ncesEmailNotificationDetailsEntity));
    }

    public Optional<NcesEmailNotificationDetailsEntity> findByMaterialId(final UUID materialId) {
        return entityManager.createQuery(
                        "select n from NcesEmailNotificationDetailsEntity n where n.materialId = :materialId", NcesEmailNotificationDetailsEntity.class)
                .setParameter("materialId", materialId)
                .getResultStream()
                .findFirst();
    }
}
