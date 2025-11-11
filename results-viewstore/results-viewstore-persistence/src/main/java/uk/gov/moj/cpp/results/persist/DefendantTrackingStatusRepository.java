package uk.gov.moj.cpp.results.persist;

import uk.gov.moj.cpp.results.persist.entity.DefendantTrackingStatus;

import java.util.List;
import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.QueryParam;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface DefendantTrackingStatusRepository extends EntityRepository<DefendantTrackingStatus, UUID> {

    @Query(value = "FROM DefendantTrackingStatus defendantTrackingStatus where defendantTrackingStatus.defendantId in (:defendantIds) and (defendantTrackingStatus.emStatus = true or defendantTrackingStatus.woaStatus = true) ")
    List<DefendantTrackingStatus> findActiveDefendantTrackingStatusByDefendantIds(@QueryParam("defendantIds") final List<UUID> defendantIds);
}
