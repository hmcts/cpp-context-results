package uk.gov.moj.cpp.results.persist;

import org.apache.deltaspike.data.api.AbstractEntityRepository;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.QueryParam;
import org.apache.deltaspike.data.api.Repository;
import uk.gov.moj.cpp.results.persist.entity.Hearing;
import uk.gov.moj.cpp.results.persist.entity.HearingKey;
import uk.gov.moj.cpp.results.persist.entity.HearingResultSummary;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.apache.deltaspike.data.api.SingleResultType.OPTIONAL;

@Repository
public abstract class HearingRepository extends AbstractEntityRepository<Hearing, HearingKey> {

    @Query(value = "from Hearing h where h.id = :hearingId and h.personId = :personId", singleResult = OPTIONAL)
    public abstract Hearing findHearingByPersonIdAndHearingId(@QueryParam("personId") final UUID personId, @QueryParam("hearingId") final UUID hearingId);

    @Query(value = "select new uk.gov.moj.cpp.results.persist.entity.HearingResultSummary(h.id, h.personId, h.hearingType, h.startDate, p.firstName, p.lastName) from Hearing h, Defendant p where h.id = p.hearingId and h.personId = p.id and h.startDate >= :fromDate")
    public abstract List<HearingResultSummary> findHearingResultSummariesByFromDate(@QueryParam("fromDate") final LocalDate fromDate);
}
