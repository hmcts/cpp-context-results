package uk.gov.moj.cpp.results.persist;

import uk.gov.moj.cpp.results.persist.entity.NcesEmailNotificationDetailsEntity;

import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface NcesEmailNotificationDetailsRepository extends EntityRepository<NcesEmailNotificationDetailsEntity, UUID> {
    NcesEmailNotificationDetailsEntity findByMaterialId(final UUID materialId);
}
