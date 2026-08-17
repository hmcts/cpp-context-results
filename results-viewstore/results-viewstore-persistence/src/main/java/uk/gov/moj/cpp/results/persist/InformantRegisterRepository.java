package uk.gov.moj.cpp.results.persist;


import uk.gov.moj.cpp.domains.constant.RegisterStatus;
import uk.gov.moj.cpp.results.persist.entity.InformantRegisterEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@ApplicationScoped
public class InformantRegisterRepository {

    private static final String PROSECUTION_AUTHORITY_ID_PARAM = "prosecutionAuthorityId";
    private static final String REGISTER_DATE_PARAM = "registerDate";

    @PersistenceContext(unitName = "results-persistence-unit")
    EntityManager entityManager;

    public InformantRegisterEntity save(final InformantRegisterEntity informantRegisterEntity) {
        return entityManager.merge(informantRegisterEntity);
    }

    public List<InformantRegisterEntity> findAll() {
        return entityManager.createQuery("select informantRegister from InformantRegisterEntity informantRegister", InformantRegisterEntity.class).getResultList();
    }

    public void remove(final InformantRegisterEntity informantRegisterEntity) {
        entityManager.remove(entityManager.contains(informantRegisterEntity)
                ? informantRegisterEntity
                : entityManager.merge(informantRegisterEntity));
    }

    public List<InformantRegisterEntity> findByFileId(final UUID materialId) {
        return entityManager.createQuery(
                        "select informantRegister from InformantRegisterEntity informantRegister where informantRegister.fileId = :fileId", InformantRegisterEntity.class)
                .setParameter("fileId", materialId)
                .getResultList();
    }

    public List<InformantRegisterEntity> findByStatus(final RegisterStatus status) {
        return entityManager.createQuery(
                        "select informantRegister from InformantRegisterEntity informantRegister where informantRegister.status = :status", InformantRegisterEntity.class)
                .setParameter("status", status)
                .getResultList();
    }

    public List<InformantRegisterEntity> findByStatusRecorded() {
        return entityManager.createQuery(
                        "select informantRegister from InformantRegisterEntity informantRegister " +
                                "where informantRegister.status = 'RECORDED' and informantRegister.processedOn is null and (informantRegister.registerTime, informantRegister.hearingId) IN " +
                                "(select max(ir.registerTime), hearingId from InformantRegisterEntity ir where ir.status = 'RECORDED' AND ir.processedOn is null group by ir.hearingId, ir.status)", InformantRegisterEntity.class)
                .getResultList();
    }

    public List<InformantRegisterEntity> findByProsecutionAuthorityIdAndStatusRecorded(final UUID prosecutionAuthorityId) {
        return entityManager.createQuery(
                        "select informantRegister from InformantRegisterEntity informantRegister " +
                                " where informantRegister.prosecutionAuthorityId = :prosecutionAuthorityId " +
                                " and informantRegister.status = 'RECORDED' and informantRegister.processedOn is null and (informantRegister.registerTime, informantRegister.hearingId) IN " +
                                " (select max(ir.registerTime), ir.hearingId from InformantRegisterEntity ir where ir.prosecutionAuthorityId = :prosecutionAuthorityId " +
                                " and ir.status = 'RECORDED' AND ir.processedOn is null group by ir.hearingId, ir.status)", InformantRegisterEntity.class)
                .setParameter(PROSECUTION_AUTHORITY_ID_PARAM, prosecutionAuthorityId)
                .getResultList();
    }

    public List<InformantRegisterEntity> findByProsecutionAuthorityIdAndRegisterDateForStatusRecorded(final UUID prosecutionAuthorityId, final LocalDate registerDate) {
        return entityManager.createQuery(
                        "select informantRegister from InformantRegisterEntity informantRegister " +
                                " where informantRegister.prosecutionAuthorityId = :prosecutionAuthorityId " +
                                " and informantRegister.registerDate = :registerDate " +
                                " and informantRegister.status = 'RECORDED' and informantRegister.processedOn is null and (informantRegister.registerTime, informantRegister.hearingId) IN " +
                                " (select max(ir.registerTime), ir.hearingId from InformantRegisterEntity ir where ir.prosecutionAuthorityId = :prosecutionAuthorityId " +
                                " and ir.status = 'RECORDED' AND ir.processedOn is null group by ir.hearingId, ir.status)", InformantRegisterEntity.class)
                .setParameter(PROSECUTION_AUTHORITY_ID_PARAM, prosecutionAuthorityId)
                .setParameter(REGISTER_DATE_PARAM, registerDate)
                .getResultList();
    }

    public List<InformantRegisterEntity> findByProsecutionAuthorityIdAndStatusGenerated(final UUID prosecutionAuthorityId) {
        return entityManager.createQuery(
                        "select informantRegister FROM InformantRegisterEntity informantRegister where prosecutionAuthorityId=:prosecutionAuthorityId and status='GENERATED'", InformantRegisterEntity.class)
                .setParameter(PROSECUTION_AUTHORITY_ID_PARAM, prosecutionAuthorityId)
                .getResultList();
    }

    public List<InformantRegisterEntity> findByProsecutionAuthorityIdAndRegisterDateAndStatusGenerated(final UUID prosecutionAuthorityId, final LocalDate registerDate) {
        return entityManager.createQuery(
                        "select informantRegister FROM InformantRegisterEntity informantRegister where prosecutionAuthorityId=:prosecutionAuthorityId and registerDate=:registerDate and status='GENERATED'", InformantRegisterEntity.class)
                .setParameter(PROSECUTION_AUTHORITY_ID_PARAM, prosecutionAuthorityId)
                .setParameter(REGISTER_DATE_PARAM, registerDate)
                .getResultList();
    }

    public List<InformantRegisterEntity> findByRegisterDate(final LocalDate registerDate) {
        return entityManager.createQuery(
                        "select informantRegister from InformantRegisterEntity informantRegister " +
                                "where informantRegister.generatedDate = :registerDate " +
                                "and informantRegister.registerTime IN " +
                                "(select max(ir.registerTime) from InformantRegisterEntity ir " +
                                "where ir.generatedDate = :registerDate group by ir.hearingId)", InformantRegisterEntity.class)
                .setParameter(REGISTER_DATE_PARAM, registerDate)
                .getResultList();
    }

    public List<InformantRegisterEntity> findByRegisterDateAndProsecutionAuthorityCode(final LocalDate registerDate, final String prosecutionAuthorityCode) {
        return entityManager.createQuery(
                        "select informantRegister from InformantRegisterEntity informantRegister " +
                                "where informantRegister.prosecutionAuthorityCode = :prosecutionAuthorityCode" +
                                " and informantRegister.generatedDate = :registerDate and informantRegister.registerTime IN " +
                                "(select max(ir.registerTime) from InformantRegisterEntity ir " +
                                "where ir.prosecutionAuthorityCode = :prosecutionAuthorityCode " +
                                "and ir.generatedDate = :registerDate group by ir.hearingId)", InformantRegisterEntity.class)
                .setParameter(REGISTER_DATE_PARAM, registerDate)
                .setParameter("prosecutionAuthorityCode", prosecutionAuthorityCode)
                .getResultList();
    }

    public List<InformantRegisterEntity> findByHearingIdAndStatusRecorded(final UUID hearingId) {
        return entityManager.createQuery(
                        "select informantRegister from InformantRegisterEntity informantRegister " +
                                " where informantRegister.hearingId = :hearingId and informantRegister.status = 'RECORDED'", InformantRegisterEntity.class)
                .setParameter("hearingId", hearingId)
                .getResultList();
    }

    public List<InformantRegisterEntity> findByProsecutionAuthorityOuCodeAndRegisterDateRange(final String prosecutionAuthorityOuCode,
                                                                                              final LocalDate startDate,
                                                                                              final LocalDate endDate) {
        return entityManager.createQuery(
                        "select informantRegister FROM InformantRegisterEntity informantRegister where prosecutionAuthorityOuCode=:prosecutionAuthorityOuCode and informantRegister.registerDate >= :startDate and informantRegister.registerDate <= :endDate", InformantRegisterEntity.class)
                .setParameter("prosecutionAuthorityOuCode", prosecutionAuthorityOuCode)
                .setParameter("startDate", startDate)
                .setParameter("endDate", endDate)
                .getResultList();
    }
}
