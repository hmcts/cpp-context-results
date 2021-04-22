package uk.gov.moj.cpp.results.event;

import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;
import static uk.gov.moj.cpp.domains.constant.RegisterStatus.GENERATED;
import static uk.gov.moj.cpp.domains.constant.RegisterStatus.NOTIFIED;
import static uk.gov.moj.cpp.domains.constant.RegisterStatus.RECORDED;

import uk.gov.justice.core.courts.informantRegisterDocument.InformantRegisterDocumentRequest;
import uk.gov.justice.results.courts.InformantRegisterGenerated;
import uk.gov.justice.results.courts.InformantRegisterNotified;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.results.persist.InformantRegisterRepository;
import uk.gov.moj.cpp.results.persist.entity.InformantRegisterEntity;

import java.time.ZonedDateTime;
import java.util.List;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.transaction.Transactional;

import org.apache.commons.lang3.BooleanUtils;

@ServiceComponent(EVENT_LISTENER)
public class InformantRegisterEventListener {

    private static final String INFORMANT_REGISTER_REQUEST_PARAM = "informantRegister";

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private InformantRegisterRepository informantRegisterRepository;

    @Transactional
    @Handles("results.event.informant-register-recorded")
    public void saveInformantRegister(final JsonEnvelope event) {

        final JsonObject informantRegisterDocumentRequestJson = event.payloadAsJsonObject().getJsonObject(INFORMANT_REGISTER_REQUEST_PARAM);

        final InformantRegisterDocumentRequest informantRegisterDocumentRequest = jsonObjectToObjectConverter.convert(informantRegisterDocumentRequestJson, InformantRegisterDocumentRequest.class);

        final InformantRegisterEntity informantRegisterEntity = new InformantRegisterEntity();
        informantRegisterEntity.setId(randomUUID());
        informantRegisterEntity.setRegisterDate(informantRegisterDocumentRequest.getRegisterDate().toLocalDate());
        informantRegisterEntity.setRegisterTime(informantRegisterDocumentRequest.getRegisterDate());
        informantRegisterEntity.setHearingId(informantRegisterDocumentRequest.getHearingId());
        informantRegisterEntity.setProsecutionAuthorityId(informantRegisterDocumentRequest.getProsecutionAuthorityId());
        informantRegisterEntity.setProsecutionAuthorityCode(informantRegisterDocumentRequest.getProsecutionAuthorityCode());
        informantRegisterEntity.setProsecutionAuthorityOuCode(informantRegisterDocumentRequest.getProsecutionAuthorityOuCode());
        informantRegisterEntity.setPayload(informantRegisterDocumentRequestJson.toString());
        informantRegisterEntity.setStatus(RECORDED);
        informantRegisterRepository.save(informantRegisterEntity);
    }

    @Handles("results.event.informant-register-generated")
    public void generateInformantRegister(final JsonEnvelope event) {
        final JsonObject payload = event.payloadAsJsonObject();
        final InformantRegisterGenerated informantRegisterGenerated = jsonObjectToObjectConverter.convert(payload, InformantRegisterGenerated.class);
        final ZonedDateTime currentDateTime = now();

        final List<InformantRegisterEntity> informantRegisters = informantRegisterRepository.findByProsecutionAuthorityIdAndStatusRecorded(informantRegisterGenerated.getInformantRegisterDocumentRequests().get(0).getProsecutionAuthorityId());
        informantRegisters.forEach(informantRegisterEntity -> {
            informantRegisterEntity.setStatus(GENERATED);
            informantRegisterEntity.setProcessedOn(currentDateTime);
            if(BooleanUtils.isTrue(informantRegisterGenerated.getSystemGenerated())) {
                informantRegisterEntity.setGeneratedDate(currentDateTime.toLocalDate());
                informantRegisterEntity.setGeneratedTime(currentDateTime);
            }
        });

        informantRegisterGenerated.getInformantRegisterDocumentRequests().stream().map(InformantRegisterDocumentRequest::getHearingId).forEach(hearingId -> {
            final List<InformantRegisterEntity> informantRegistersList = informantRegisterRepository.findByHearingIdAndStatusRecorded(hearingId);
            informantRegistersList.forEach(informantRegisterEntity -> informantRegisterEntity.setProcessedOn(currentDateTime));
        });

    }

    @Handles("results.event.informant-register-notified")
    public void notifyInformantRegister(final JsonEnvelope event) {
        final JsonObject payload = event.payloadAsJsonObject();
        final InformantRegisterNotified informantRegisterNotified = jsonObjectToObjectConverter.convert(payload, InformantRegisterNotified.class);
        final List<InformantRegisterEntity> informantRegisters = informantRegisterRepository.findByProsecutionAuthorityIdAndStatusGenerated(informantRegisterNotified.getProsecutionAuthorityId());
        informantRegisters.forEach(informantRegisterEntity -> {
                    informantRegisterEntity.setStatus(NOTIFIED);
                    informantRegisterEntity.setFileId(informantRegisterNotified.getFileId());
                    informantRegisterEntity.setProcessedOn(now());
                }
        );
    }
}
