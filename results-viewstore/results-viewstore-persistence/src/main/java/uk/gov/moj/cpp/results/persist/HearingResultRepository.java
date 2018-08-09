package uk.gov.moj.cpp.results.persist;

import org.apache.deltaspike.data.api.AbstractEntityRepository;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.QueryParam;
import org.apache.deltaspike.data.api.Repository;
import uk.gov.moj.cpp.results.persist.entity.CourtClerk;
import uk.gov.moj.cpp.results.persist.entity.HearingResult;

import java.util.List;
import java.util.UUID;

@Repository(forEntity = HearingResult.class)
public abstract class HearingResultRepository extends AbstractEntityRepository<HearingResult, UUID> {

    @Query(value = "from HearingResult hr where hr.hearingId = :hearingId and hr.personId = :personId")
    public abstract List<HearingResult> findByHearingIdAndPersonId(@QueryParam("hearingId") final UUID hearingId,
                                                                   @QueryParam("personId") final UUID personId);

    @Query(value = "select distinct new uk.gov.moj.cpp.results.persist.entity.CourtClerk(hr.clerkOfTheCourtId, hr.clerkOfTheCourtFirstName, hr.clerkOfTheCourtLastName)" +
            " from HearingResult hr where hr.hearingId = :hearingId and hr.personId = :personId " +
            " order by hr.clerkOfTheCourtFirstName, hr.clerkOfTheCourtLastName")
    public abstract List<CourtClerk> findCourtClerksForHearingIdAndPersonId(@QueryParam("hearingId") final UUID hearingId, @QueryParam("personId") final UUID personId);

    @Query(value = "from HearingResult hr where hr.hearingId = :hearingId")
    public abstract List<HearingResult> findByHearingId(@QueryParam("hearingId") final UUID hearingId);
}
