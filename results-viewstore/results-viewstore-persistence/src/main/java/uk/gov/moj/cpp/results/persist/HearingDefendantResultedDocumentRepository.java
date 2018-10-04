package uk.gov.moj.cpp.results.persist;

import org.apache.deltaspike.data.api.AbstractEntityRepository;
import org.apache.deltaspike.data.api.Repository;
import uk.gov.moj.cpp.results.persist.entity.HearingDefendantResultedDocument;
import java.util.UUID;

@Repository(forEntity = HearingDefendantResultedDocument.class)
public abstract class HearingDefendantResultedDocumentRepository extends AbstractEntityRepository<HearingDefendantResultedDocument, UUID> {

}
