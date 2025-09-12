package uk.gov.moj.cpp.results.persist;

import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.QueryParam;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface DefendantGobAccountsRepository extends EntityRepository<DefendantGobAccountsEntity, UUID> {

    @Query(value = """
            SELECT * 
            FROM defendant_gob_accounts dga 
            WHERE dga.master_defendant_id = :masterDefendantId 
              AND dga.hearing_id = :hearingId
            ORDER BY dga.account_request_time DESC
            LIMIT 1
            """, isNative = true)
    DefendantGobAccountsEntity findAccountNumberByMasterDefendantIdAndHearingId(@QueryParam("masterDefendantId") final UUID masterDefendantId,
                                                                                 @QueryParam("hearingId") final UUID hearingId);

    @Query(value = """
            SELECT * 
            FROM defendant_gob_accounts dga 
            WHERE dga.master_defendant_id = :masterDefendantId 
              AND dga.hearing_id = :hearingId
              AND dga.correlation_id = :correlationId
            """, isNative = true)
    DefendantGobAccountsEntity findByMasterDefendantIdAndHearingIdAndCorrelationId(@QueryParam("masterDefendantId") final UUID masterDefendantId,
                                                                                   @QueryParam("hearingId") final UUID hearingId,
                                                                                   @QueryParam("correlationId") final UUID correlationId);

}
