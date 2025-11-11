package uk.gov.moj.cpp.results.persist;

import static org.apache.deltaspike.data.api.SingleResultType.OPTIONAL;

import uk.gov.moj.cpp.results.persist.entity.HearingResultedDocument;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.apache.deltaspike.data.api.AbstractEntityRepository;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.QueryParam;
import org.apache.deltaspike.data.api.Repository;

@Repository(forEntity = HearingResultedDocument.class)
public abstract class HearingResultedDocumentRepository extends AbstractEntityRepository<HearingResultedDocument, UUID> {

    @Query(value = "from HearingResultedDocument h where h.endDate >= :fromDate")
    public abstract List<HearingResultedDocument> findByFromDate(@QueryParam("fromDate") final LocalDate fromDate);

    @Query(value = "from HearingResultedDocument h where h.id.hearingId = :hearingId")
    public abstract List<HearingResultedDocument> findByHearingId(@QueryParam("hearingId") final UUID hearingId);

    @Query(value = "from HearingResultedDocument h where h.id.hearingId = :hearingId and " +
            "h.id.hearingDay = (select max(hh.id.hearingDay) from HearingResultedDocument hh where hh.id.hearingId = :hearingId)", singleResult = OPTIONAL)
    public abstract HearingResultedDocument findByHearingIdAndLatestHearingDay(@QueryParam("hearingId") final UUID hearingId);

    @Query(value = "from HearingResultedDocument h where h.id.hearingId = :hearingId and h.id.hearingDay = :hearingDate")
    public abstract HearingResultedDocument findByHearingIdAndHearingDay(@QueryParam("hearingId") final UUID hearingId, @QueryParam("hearingDate") final LocalDate hearingDate);
}
