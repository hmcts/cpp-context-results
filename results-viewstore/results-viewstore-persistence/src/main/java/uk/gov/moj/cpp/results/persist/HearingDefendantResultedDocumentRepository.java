package uk.gov.moj.cpp.results.persist;

import uk.gov.moj.cpp.results.persist.entity.HearingDefendantResultedDocument;

import java.util.UUID;

import org.apache.deltaspike.data.api.AbstractEntityRepository;
import org.apache.deltaspike.data.api.Repository;

@Repository(forEntity = HearingDefendantResultedDocument.class)
public abstract class HearingDefendantResultedDocumentRepository extends AbstractEntityRepository<HearingDefendantResultedDocument, UUID> {

}
