package uk.gov.moj.cpp.results.event.service;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.enveloper.Enveloper;

import javax.inject.Inject;

public class UploadMaterialService {

    public static final String RESULTS_COMMAND_RECORD_MATERIAL_REQUEST = "results.events.record-material-request";

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    MaterialService materialService;

    @Inject
    private Enveloper enveloper;

    public void uploadMaterial(final UploadMaterialContext uploadMaterialContext, final String ncesOriginatorValue) {
        materialService.uploadMaterial(uploadMaterialContext.getFileId(), uploadMaterialContext.getMaterialId(), uploadMaterialContext.getOriginatingEnvelope(), ncesOriginatorValue);
    }

}
