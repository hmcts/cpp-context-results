package uk.gov.moj.cpp.results.event.service;

import static java.util.Objects.nonNull;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.moj.cpp.domains.DocumentDeliveryStatus.PENDING;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.domains.DocumentDeliveryStatus;
import uk.gov.moj.cpp.domains.DocumentDeliveryType;
import uk.gov.moj.cpp.domains.EmailDeliveryStatus;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

/**
 * Records the delivery tracking of a document results stores in its blob container (file uri path
 * only) by sending results.command.record-document-delivery:
 * <ul>
 *     <li>{@link #startTracking} - material PENDING, when the document is sent to system document
 *     generator, or to material where there is no generation step, together with the SJP cases it is
 *     sent to (PENDING);</li>
 *     <li>{@link #recordMaterialStatus} - SUCCESS / FAILED on material's public events;</li>
 *     <li>{@link #recordSjpStatus} - SUCCESS / FAILED on SJP's public events;</li>
 *     <li>{@link #recordEmailStatus} - PENDING when the email is sent, SUCCESS / FAILED on its outcome,
 *     NOT_REQUIRED where no email is sent (no recipient address). A flow that never sends an email
 *     starts with NOT_REQUIRED instead.</li>
 * </ul>
 * A status for a material that is not tracked, or for an SJP case the document was not sent to, is
 * ignored by the aggregate, so callers need not filter the file service path out of shared events.
 */
public class DocumentDeliveryStatusService {

    public static final String RESULTS_COMMAND_RECORD_DOCUMENT_DELIVERY = "results.command.record-document-delivery";

    private static final String MATERIAL_ID = "materialId";
    private static final String SJP_CASES = "sjpCases";
    private static final String CASE_ID = "caseId";
    private static final String SJP_CORRELATION_ID = "sjpCorrelationId";
    private static final String SJP_STATUS = "sjpStatus";

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Sender sender;

    /**
     * @param payloadBlobUri the payload sent to system document generator; null where there is none
     * @param sjpCases       the SJP cases the document is sent to; empty where there are none
     * @param emailStatus    NOT_REQUIRED for a flow that sends no email; null for a flow that does,
     *                       which records NOT_REQUIRED or PENDING once it has decided whether to send it
     */
    public void startTracking(final Metadata originatingMetadata, final UUID materialId, final DocumentDeliveryType documentType,
                              final String payloadBlobUri, final String documentBlobUri, final List<SjpCaseDelivery> sjpCases,
                              final EmailDeliveryStatus emailStatus) {
        final JsonObjectBuilder payload = materialPayload(materialId)
                .add("documentType", documentType.name())
                .add("materialStatus", PENDING.name());
        addIfPresent(payload, "payloadBlobUri", payloadBlobUri);
        addIfPresent(payload, "documentBlobUri", documentBlobUri);
        if (nonNull(emailStatus)) {
            payload.add("emailStatus", emailStatus.name());
        }

        if (!sjpCases.isEmpty()) {
            final JsonArrayBuilder sjpCaseArray = createArrayBuilder();
            sjpCases.forEach(sjpCase -> {
                final JsonObjectBuilder sjpCasePayload = createObjectBuilder()
                        .add(CASE_ID, sjpCase.caseId().toString())
                        .add(SJP_STATUS, PENDING.name());
                addIfPresent(sjpCasePayload, SJP_CORRELATION_ID, sjpCase.sjpCorrelationId());
                sjpCaseArray.add(sjpCasePayload);
            });
            payload.add(SJP_CASES, sjpCaseArray);
        }

        send(originatingMetadata, payload.build());
    }

    public void recordMaterialStatus(final Metadata originatingMetadata, final UUID materialId, final DocumentDeliveryStatus materialStatus) {
        send(originatingMetadata, materialPayload(materialId).add("materialStatus", materialStatus.name()).build());
    }

    public void recordEmailStatus(final Metadata originatingMetadata, final UUID materialId, final EmailDeliveryStatus emailStatus) {
        send(originatingMetadata, materialPayload(materialId).add("emailStatus", emailStatus.name()).build());
    }

    public void recordSjpStatus(final Metadata originatingMetadata, final UUID materialId, final UUID caseId, final DocumentDeliveryStatus sjpStatus) {
        send(originatingMetadata, materialPayload(materialId)
                .add(SJP_CASES, createArrayBuilder().add(createObjectBuilder()
                        .add(CASE_ID, caseId.toString())
                        .add(SJP_STATUS, sjpStatus.name())))
                .build());
    }

    private static JsonObjectBuilder materialPayload(final UUID materialId) {
        return createObjectBuilder().add(MATERIAL_ID, materialId.toString());
    }

    private static void addIfPresent(final JsonObjectBuilder builder, final String key, final String value) {
        if (nonNull(value)) {
            builder.add(key, value);
        }
    }

    private void send(final Metadata originatingMetadata, final JsonObject payload) {
        sender.sendAsAdmin(Envelope.envelopeFrom(
                metadataFrom(originatingMetadata).withName(RESULTS_COMMAND_RECORD_DOCUMENT_DELIVERY),
                payload));
    }

    /**
     * An SJP case a document is sent to, with the correlation id it is filed with (the caseDocument
     * sent on sjp.upload-case-document).
     */
    public record SjpCaseDelivery(UUID caseId, String sjpCorrelationId) {
    }
}
