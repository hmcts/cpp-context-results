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
import uk.gov.justice.results.courts.InformantRegisterNotifiedV2;
import uk.gov.justice.results.courts.InformantRegisterGeneratedV2;
import uk.gov.justice.results.courts.InformantRegisterRecordedV2;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.results.persist.InformantRegisterRepository;
import uk.gov.moj.cpp.results.persist.entity.InformantRegisterEntity;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.transaction.Transactional;

import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_LISTENER)
public class InformantRegisterEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(InformantRegisterEventListener.class);

    private static final String INFORMANT_REGISTER_REQUEST_PARAM = "informantRegister";

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private InformantRegisterRepository informantRegisterRepository;

    @Transactional
    @Handles("results.event.informant-register-recorded")
    public void saveInformantRegister(final JsonEnvelope event) {
        final JsonObject informantRegisterDocumentRequestJson = event.payloadAsJsonObject().getJsonObject(INFORMANT_REGISTER_REQUEST_PARAM);
        final InformantRegisterDocumentRequest documentRequest = jsonObjectToObjectConverter.convert(informantRegisterDocumentRequestJson, InformantRegisterDocumentRequest.class);

        saveRecordedInformantRegister(informantRegisterDocumentRequestJson, documentRequest.getRegisterDate(),
                documentRequest.getHearingId(), documentRequest.getProsecutionAuthorityId(),
                documentRequest.getProsecutionAuthorityCode(), documentRequest.getProsecutionAuthorityOuCode());
    }

    @Transactional
    @Handles("results.event.informant-register-recorded-v2")
    public void saveInformantRegisterV2(final JsonEnvelope event) {
        final JsonObject informantRegisterDocumentRequestJson = event.payloadAsJsonObject().getJsonObject(INFORMANT_REGISTER_REQUEST_PARAM);
        final uk.gov.justice.results.courts.informantRegisterDocument.InformantRegisterDocumentRequest documentRequest =
                jsonObjectToObjectConverter.convert(informantRegisterDocumentRequestJson, uk.gov.justice.results.courts.informantRegisterDocument.InformantRegisterDocumentRequest.class);

        saveRecordedInformantRegister(informantRegisterDocumentRequestJson, documentRequest.getRegisterDate(),
                documentRequest.getHearingId(), documentRequest.getProsecutionAuthorityId(),
                documentRequest.getProsecutionAuthorityCode(), documentRequest.getProsecutionAuthorityOuCode());
    }

    private void saveRecordedInformantRegister(final JsonObject informantRegisterDocumentRequestJson,
                                               final ZonedDateTime registerDate,
                                               final UUID hearingId,
                                               final UUID prosecutionAuthorityId,
                                               final String prosecutionAuthorityCode,
                                               final String prosecutionAuthorityOuCode) {
        final InformantRegisterEntity informantRegisterEntity = new InformantRegisterEntity();
        informantRegisterEntity.setId(randomUUID());
        informantRegisterEntity.setRegisterDate(registerDate.toLocalDate());
        informantRegisterEntity.setRegisterTime(registerDate);
        informantRegisterEntity.setHearingId(hearingId);
        informantRegisterEntity.setProsecutionAuthorityId(prosecutionAuthorityId);
        informantRegisterEntity.setProsecutionAuthorityCode(prosecutionAuthorityCode);
        informantRegisterEntity.setProsecutionAuthorityOuCode(prosecutionAuthorityOuCode);
        informantRegisterEntity.setPayload(informantRegisterDocumentRequestJson.toString());
        informantRegisterEntity.setStatus(RECORDED);
        informantRegisterRepository.save(informantRegisterEntity);
    }

    @Handles("results.event.informant-register-generated")
    public void generateInformantRegister(final JsonEnvelope event) {
        final JsonObject payload = event.payloadAsJsonObject();
        final InformantRegisterGenerated informantRegisterGenerated = jsonObjectToObjectConverter.convert(payload, InformantRegisterGenerated.class);
        final ZonedDateTime currentDateTime = now();

        final List<InformantRegisterDocumentRequest> documentRequests = informantRegisterGenerated.getInformantRegisterDocumentRequests();
        if (documentRequests == null || documentRequests.isEmpty()) {
            LOGGER.warn("Skipping results.event.informant-register-generated: no informantRegisterDocumentRequests present");
            return;
        }

        final UUID prosecutionAuthorityId = documentRequests.get(0).getProsecutionAuthorityId();
        final LocalDate registerDate = documentRequests.get(0).getRegisterDate().toLocalDate();

        final List<InformantRegisterEntity> informantRegisters = informantRegisterRepository.findByProsecutionAuthorityIdAndRegisterDateForStatusRecorded(prosecutionAuthorityId, registerDate);
        informantRegisters.forEach(informantRegisterEntity -> {
            informantRegisterEntity.setStatus(GENERATED);
            informantRegisterEntity.setProcessedOn(currentDateTime);
            if(BooleanUtils.isTrue(informantRegisterGenerated.getSystemGenerated())) {
                informantRegisterEntity.setGeneratedDate(currentDateTime.toLocalDate());
                informantRegisterEntity.setGeneratedTime(currentDateTime);
            }
        });

        documentRequests.stream().map(InformantRegisterDocumentRequest::getHearingId).forEach(hearingId -> {
            final List<InformantRegisterEntity> informantRegistersList = informantRegisterRepository.findByHearingIdAndStatusRecorded(hearingId);
            informantRegistersList.forEach(informantRegisterEntity -> informantRegisterEntity.setProcessedOn(currentDateTime));
        });

    }

    @Handles("results.event.informant-register-generated-v2")
    public void generateInformantRegisterV2(final JsonEnvelope event) {
        final JsonObject payload = event.payloadAsJsonObject();
        final InformantRegisterGeneratedV2 informantRegisterGenerated = jsonObjectToObjectConverter.convert(payload, InformantRegisterGeneratedV2.class);
        final ZonedDateTime currentDateTime = now();

        final List<uk.gov.justice.results.courts.informantRegisterDocument.InformantRegisterDocumentRequest> documentRequests = informantRegisterGenerated.getInformantRegisterDocumentRequests();
        if (documentRequests == null || documentRequests.isEmpty()) {
            LOGGER.warn("Skipping results.event.informant-register-generated-v2: no informantRegisterDocumentRequests present");
            return;
        }

        final UUID prosecutionAuthorityId = documentRequests.get(0).getProsecutionAuthorityId();
        final LocalDate registerDate = documentRequests.get(0).getRegisterDate().toLocalDate();

        final List<InformantRegisterEntity> informantRegisters = informantRegisterRepository.findByProsecutionAuthorityIdAndRegisterDateForStatusRecorded(prosecutionAuthorityId, registerDate);
        informantRegisters.forEach(informantRegisterEntity -> {
            informantRegisterEntity.setStatus(GENERATED);
            informantRegisterEntity.setProcessedOn(currentDateTime);
            if (BooleanUtils.isTrue(informantRegisterGenerated.getSystemGenerated())) {
                informantRegisterEntity.setGeneratedDate(currentDateTime.toLocalDate());
                informantRegisterEntity.setGeneratedTime(currentDateTime);
            }
        });

        documentRequests.stream()
                .map(uk.gov.justice.results.courts.informantRegisterDocument.InformantRegisterDocumentRequest::getHearingId)
                .forEach(hearingId -> {
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

    @Handles("results.event.informant-register-notified-v2")
    public void notifyInformantRegisterV2(final JsonEnvelope event) {
        final JsonObject payload = event.payloadAsJsonObject();
        final InformantRegisterNotifiedV2 informantRegisterNotified = jsonObjectToObjectConverter.convert(payload, InformantRegisterNotifiedV2.class);
        final List<InformantRegisterEntity> informantRegisters = informantRegisterRepository.findByProsecutionAuthorityIdAndRegisterDateAndStatusGenerated(informantRegisterNotified.getProsecutionAuthorityId(), informantRegisterNotified.getRegisterDate());
        informantRegisters.forEach(informantRegisterEntity -> {
                    informantRegisterEntity.setStatus(NOTIFIED);
                    informantRegisterEntity.setFileId(informantRegisterNotified.getFileId());
                    informantRegisterEntity.setProcessedOn(now());
                }
        );
    }
}
