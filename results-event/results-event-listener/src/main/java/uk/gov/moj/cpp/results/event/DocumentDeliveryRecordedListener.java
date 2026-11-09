package uk.gov.moj.cpp.results.event;

import static java.time.ZonedDateTime.now;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.results.domain.event.DocumentDeliveryRecorded;
import uk.gov.moj.cpp.results.domain.event.SjpCases;
import uk.gov.moj.cpp.results.persist.ResultsDocumentDeliveryRepository;
import uk.gov.moj.cpp.results.persist.entity.NcesSjpCase;
import uk.gov.moj.cpp.results.persist.entity.ResultsDocumentDeliveryEntity;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Keeps results_document_delivery and results_nces_sjp_case in step with the document delivery
 * aggregate. The event carries only what changed: the first event of a material creates the record
 * with the SJP cases the document was sent to, a later one updates the fields it carries and the
 * status of the SJP cases it names - an SJP case that is not on the record is neither added nor
 * updated, and no SJP case is ever removed.
 */
@ServiceComponent(EVENT_LISTENER)
public class DocumentDeliveryRecordedListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentDeliveryRecordedListener.class);

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ResultsDocumentDeliveryRepository resultsDocumentDeliveryRepository;

    @Handles("results.event.document-delivery-recorded")
    public void handleDocumentDeliveryRecorded(final JsonEnvelope event) {
        final DocumentDeliveryRecorded recorded = jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), DocumentDeliveryRecorded.class);
        final ResultsDocumentDeliveryEntity delivery = resultsDocumentDeliveryRepository.findBy(recorded.getMaterialId());

        if (nonNull(delivery)) {
            update(delivery, recorded);
        } else if (nonNull(recorded.getDocumentType())) {
            resultsDocumentDeliveryRepository.save(create(recorded));
        }
    }

    private static ResultsDocumentDeliveryEntity create(final DocumentDeliveryRecorded recorded) {
        final ResultsDocumentDeliveryEntity delivery = new ResultsDocumentDeliveryEntity(recorded.getMaterialId(), recorded.getDocumentType(),
                now(), recorded.getPayloadBlobUri(), recorded.getDocumentBlobUri());
        delivery.setMaterialStatus(recorded.getMaterialStatus());
        delivery.setEmailStatus(recorded.getEmailStatus());
        sjpCasesOf(recorded).forEach(sjpCase ->
                delivery.getSjpCases().add(new NcesSjpCase(sjpCase.getCaseId(), sjpCase.getSjpCorrelationId(), sjpCase.getSjpStatus())));
        return delivery;
    }

    private void update(final ResultsDocumentDeliveryEntity delivery, final DocumentDeliveryRecorded recorded) {
        if (nonNull(recorded.getMaterialStatus())) {
            delivery.setMaterialStatus(recorded.getMaterialStatus());
        }
        if (nonNull(recorded.getEmailStatus())) {
            delivery.setEmailStatus(recorded.getEmailStatus());
        }
        if (nonNull(recorded.getPayloadBlobUri())) {
            delivery.setPayloadBlobUri(recorded.getPayloadBlobUri());
        }
        if (nonNull(recorded.getDocumentBlobUri())) {
            delivery.setDocumentBlobUri(recorded.getDocumentBlobUri());
        }
        sjpCasesOf(recorded).forEach(sjpCase -> {
            if (!delivery.updateSjpStatus(sjpCase.getCaseId(), sjpCase.getSjpStatus())) {
                LOGGER.warn("Material {} was not sent to SJP case {}; its status is not recorded", recorded.getMaterialId(), sjpCase.getCaseId());
            }
        });
        resultsDocumentDeliveryRepository.save(delivery);
    }

    private static List<SjpCases> sjpCasesOf(final DocumentDeliveryRecorded recorded) {
        return isNull(recorded.getSjpCases()) ? List.of() : recorded.getSjpCases();
    }
}
