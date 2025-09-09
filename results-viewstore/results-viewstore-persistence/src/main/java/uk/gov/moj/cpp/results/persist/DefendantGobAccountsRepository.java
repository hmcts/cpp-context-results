package uk.gov.moj.cpp.results.persist;

import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.QueryParam;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface DefendantGobAccountsRepository extends EntityRepository<DefendantGobAccountsEntity, UUID> {

    @Query("SELECT d FROM DefendantGobAccountsEntity d WHERE d.masterDefendantId = :masterDefendantId AND (d.caseReferences = :caseReferences OR d.caseReferences LIKE CONCAT('%', :caseReferences, '%') OR :caseReferences LIKE CONCAT('%', d.caseReferences, '%')) AND d.createdDateTime = (SELECT MAX(d2.createdDateTime) FROM DefendantGobAccountsEntity d2 WHERE d2.masterDefendantId = :masterDefendantId AND (d2.caseReferences = :caseReferences OR d2.caseReferences LIKE CONCAT('%', :caseReferences, '%') OR :caseReferences LIKE CONCAT('%', d2.caseReferences, '%')))")
    DefendantGobAccountsEntity findByAccountNumber(@QueryParam("masterDefendantId") final UUID masterDefendantId, @QueryParam("caseReferences") final String caseReferences);
}
