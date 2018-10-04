package uk.gov.moj.cpp.results.persist;

import org.apache.deltaspike.data.api.AbstractEntityRepository;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.QueryParam;
import org.apache.deltaspike.data.api.Repository;
import uk.gov.moj.cpp.results.persist.entity.HearingDefendant;
import uk.gov.moj.cpp.results.persist.entity.PersonKey;

import java.util.List;
import java.util.UUID;

import static org.apache.deltaspike.data.api.SingleResultType.OPTIONAL;

@Repository
public abstract class DefendantRepository extends AbstractEntityRepository<HearingDefendant, PersonKey> {

    @Query(value = "from HearingDefendant p where p.id = :personId and p.hearingId = :hearingId", singleResult = OPTIONAL)
    public abstract HearingDefendant findPersonByPersonIdAndHearingId(@QueryParam("personId") final UUID personId, @QueryParam("hearingId") final UUID hearingId);

    @Query(value = "from HearingDefendant p where p.hearingId = :hearingId")
    public abstract List<HearingDefendant> findByHearingId(@QueryParam("hearingId") final UUID hearingId);
}
