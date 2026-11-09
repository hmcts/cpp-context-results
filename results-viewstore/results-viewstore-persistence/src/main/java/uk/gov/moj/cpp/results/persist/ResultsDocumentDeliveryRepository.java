package uk.gov.moj.cpp.results.persist;

import uk.gov.moj.cpp.results.persist.entity.ResultsDocumentDeliveryEntity;

import java.util.List;
import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface ResultsDocumentDeliveryRepository extends EntityRepository<ResultsDocumentDeliveryEntity, UUID> {

    List<ResultsDocumentDeliveryEntity> findByDocumentBlobUri(String documentBlobUri);
}
