package uk.gov.moj.cpp.results.persist;

import java.util.List;
import java.util.UUID;

import org.apache.deltaspike.data.api.AbstractEntityRepository;
import org.apache.deltaspike.data.api.Modifying;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.QueryParam;
import org.apache.deltaspike.data.api.Repository;

import uk.gov.moj.cpp.results.persist.entity.VariantDirectory;

@Repository
public abstract class VariantDirectoryRepository extends AbstractEntityRepository<VariantDirectory, UUID> {

    @Modifying
    @Query("update VariantDirectory vd set vd.status = :status where vd.materialId = :materialId")
    public abstract int updateStatus(@QueryParam("materialId") final UUID materialId, @QueryParam("status") final String status);

    @Query("from VariantDirectory vd where vd.hearingId = :hearingId")
    public abstract List<VariantDirectory> findByHearingId(@QueryParam("hearingId") UUID hearingId);
}