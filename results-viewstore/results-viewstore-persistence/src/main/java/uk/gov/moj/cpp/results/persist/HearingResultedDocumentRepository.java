package uk.gov.moj.cpp.results.persist;

import org.apache.deltaspike.data.api.AbstractEntityRepository;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.QueryParam;
import org.apache.deltaspike.data.api.Repository;
import uk.gov.moj.cpp.results.persist.entity.HearingResultedDocument;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository(forEntity = HearingResultedDocument.class)
public abstract class HearingResultedDocumentRepository extends AbstractEntityRepository<HearingResultedDocument, UUID> {

    @Query(value = "from HearingResultedDocument h where h.startDate <= :fromDate and h.endDate >= :fromDate")
    public abstract List<HearingResultedDocument> findByFromDate(@QueryParam("fromDate") final LocalDate fromDate);

}
