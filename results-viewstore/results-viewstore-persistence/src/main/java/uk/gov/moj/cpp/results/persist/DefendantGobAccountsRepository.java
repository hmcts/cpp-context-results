package uk.gov.moj.cpp.results.persist;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@ApplicationScoped
public class DefendantGobAccountsRepository {

    @PersistenceContext(unitName = "results-persistence-unit")
    EntityManager entityManager;

    public DefendantGobAccountsEntity save(final DefendantGobAccountsEntity defendantGobAccountsEntity) {
        return entityManager.merge(defendantGobAccountsEntity);
    }

    public DefendantGobAccountsEntity findBy(final DefendantGobAccountsId id) {
        return entityManager.find(DefendantGobAccountsEntity.class, id);
    }

    public List<DefendantGobAccountsEntity> findAll() {
        return entityManager.createQuery("select d from DefendantGobAccountsEntity d", DefendantGobAccountsEntity.class).getResultList();
    }

    public void remove(final DefendantGobAccountsEntity defendantGobAccountsEntity) {
        entityManager.remove(entityManager.contains(defendantGobAccountsEntity)
                ? defendantGobAccountsEntity
                : entityManager.merge(defendantGobAccountsEntity));
    }

    @SuppressWarnings("unchecked")
    public Optional<DefendantGobAccountsEntity> findAccountNumberByMasterDefendantIdAndHearingId(final UUID masterDefendantId,
                                                                                                 final UUID hearingId) {
        final List<DefendantGobAccountsEntity> results = entityManager.createNativeQuery("""
                        SELECT *
                        FROM defendant_gob_accounts dga
                        WHERE dga.master_defendant_id = :masterDefendantId
                          AND dga.hearing_id = :hearingId
                        ORDER BY dga.account_request_time DESC
                        LIMIT 1
                        """, DefendantGobAccountsEntity.class)
                .setParameter("masterDefendantId", masterDefendantId)
                .setParameter("hearingId", hearingId)
                .getResultList();
        return results.stream().findFirst();
    }
}
