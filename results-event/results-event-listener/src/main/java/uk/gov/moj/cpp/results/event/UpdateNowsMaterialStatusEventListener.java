package uk.gov.moj.cpp.results.event;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.results.domain.event.NowsMaterialStatusUpdated;
import uk.gov.moj.cpp.results.persist.VariantDirectoryRepository;

@ServiceComponent(EVENT_LISTENER)
public class UpdateNowsMaterialStatusEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateNowsMaterialStatusEventListener.class);

    private final VariantDirectoryRepository variantDirectoryRepository;
    private final JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    public UpdateNowsMaterialStatusEventListener(final VariantDirectoryRepository nowsMaterialRepository,
            final JsonObjectToObjectConverter jsonObjectToObjectConverter) {
        this.variantDirectoryRepository = nowsMaterialRepository;
        this.jsonObjectToObjectConverter = jsonObjectToObjectConverter;
    }

    @Transactional
    @Handles("results.event.nows-material-status-updated")
    public void onNowsMaterialStatusUpdated(final JsonEnvelope event) {
        final NowsMaterialStatusUpdated nowsMaterialStatusUpdated = jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), NowsMaterialStatusUpdated.class);
        final int result = variantDirectoryRepository.updateStatus(nowsMaterialStatusUpdated.getMaterialId(), nowsMaterialStatusUpdated.getStatus().trim().toUpperCase());
        if (result == 0) {
            throw new IllegalStateException("Failure to update the nows material status materialId: " + nowsMaterialStatusUpdated.getMaterialId());
        }
        LOGGER.info("Status successfully updated to: {} for material id: {}", nowsMaterialStatusUpdated.getStatus(), nowsMaterialStatusUpdated.getMaterialId());
    }
}