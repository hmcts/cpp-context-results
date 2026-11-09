package uk.gov.moj.cpp.results.domain.aggregate;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;
import static uk.gov.moj.cpp.domains.DocumentDeliveryStatus.PENDING;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.moj.cpp.results.domain.event.DocumentDeliveryRecorded;
import uk.gov.moj.cpp.results.domain.event.SjpCases;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Delivery tracking of a document results stores in its blob container (NCES document, police
 * notification), one stream per material id.
 *
 * <p>Tracking starts with the PENDING record written when the document is sent to system document
 * generator or material; every other record for a material that is not tracked - a material event of
 * the file service path, a material of another context - is ignored. Once tracked, a record is
 * reduced to what it changes:
 * <ul>
 *     <li>an outcome (SUCCESS / FAILED, or NOT_REQUIRED for the email) is never taken back to PENDING - it may be
 *     recorded before a late PENDING;</li>
 *     <li>the email of a flow that sends none starts NOT_REQUIRED; in a flow that sends one it starts
 *     empty and becomes NOT_REQUIRED or PENDING once the flow decides whether to send it;</li>
 *     <li>only the SJP cases the document was sent to are updated; a case that is not tracked is
 *     ignored, never added;</li>
 *     <li>a blob uri, once known, is not replaced.</li>
 * </ul>
 * A record that changes nothing raises no event, so redelivered records are harmless.
 */
public class DocumentDeliveryAggregate implements Aggregate {

    private static final long serialVersionUID = 4127734559210384175L;

    private boolean tracked;
    private String materialStatus;
    private String emailStatus;
    private String payloadBlobUri;
    private String documentBlobUri;
    private final Map<UUID, String> sjpStatusByCaseId = new HashMap<>();

    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(DocumentDeliveryRecorded.class).apply(this::onDocumentDeliveryRecorded),
                otherwiseDoNothing()
        );
    }

    public Stream<Object> recordDocumentDelivery(final DocumentDeliveryRequest request) {
        final DocumentDeliveryRecorded recorded = tracked ? changesOf(request) : start(request);
        return isNull(recorded) ? Stream.empty() : apply(Stream.of(recorded));
    }

    private static DocumentDeliveryRecorded start(final DocumentDeliveryRequest request) {
        if (isNull(request.documentType()) || !PENDING.name().equals(request.materialStatus())) {
            return null;
        }

        // a case listed twice is tracked once
        final Map<UUID, SjpCases> sjpCases = new LinkedHashMap<>();
        request.sjpCases()
                .forEach(sjpCase -> sjpCases.putIfAbsent(sjpCase.caseId(),
                        new SjpCases(sjpCase.caseId(), sjpCase.sjpCorrelationId(), sjpCase.sjpStatus())));

        return DocumentDeliveryRecorded.documentDeliveryRecorded()
                .withMaterialId(request.materialId())
                .withDocumentType(request.documentType())
                .withMaterialStatus(request.materialStatus())
                .withPayloadBlobUri(request.payloadBlobUri())
                .withDocumentBlobUri(request.documentBlobUri())
                .withEmailStatus(request.emailStatus())
                .withSjpCases(sjpCases.isEmpty() ? null : new ArrayList<>(sjpCases.values()))
                .build();
    }

    private DocumentDeliveryRecorded changesOf(final DocumentDeliveryRequest request) {
        final String newMaterialStatus = statusChange(materialStatus, request.materialStatus());
        final String newEmailStatus = statusChange(emailStatus, request.emailStatus());
        final String newPayloadBlobUri = isNull(payloadBlobUri) ? request.payloadBlobUri() : null;
        final String newDocumentBlobUri = isNull(documentBlobUri) ? request.documentBlobUri() : null;
        final List<SjpCases> sjpCaseChanges = sjpCaseChanges(request.sjpCases());

        if (isNull(newMaterialStatus) && isNull(newEmailStatus) && isNull(newPayloadBlobUri)
                && isNull(newDocumentBlobUri) && sjpCaseChanges.isEmpty()) {
            return null;
        }

        return DocumentDeliveryRecorded.documentDeliveryRecorded()
                .withMaterialId(request.materialId())
                .withMaterialStatus(newMaterialStatus)
                .withEmailStatus(newEmailStatus)
                .withPayloadBlobUri(newPayloadBlobUri)
                .withDocumentBlobUri(newDocumentBlobUri)
                .withSjpCases(sjpCaseChanges.isEmpty() ? null : sjpCaseChanges)
                .build();
    }

    private List<SjpCases> sjpCaseChanges(final List<DocumentDeliveryRequest.SjpCase> sjpCases) {
        final Map<UUID, SjpCases> changes = new LinkedHashMap<>();
        for (final DocumentDeliveryRequest.SjpCase sjpCase : sjpCases) {
            final UUID caseId = sjpCase.caseId();
            // a case the document was not sent to is left alone
            if (!sjpStatusByCaseId.containsKey(caseId)) {
                continue;
            }
            final String newSjpStatus = statusChange(sjpStatusByCaseId.get(caseId), sjpCase.sjpStatus());
            if (nonNull(newSjpStatus)) {
                changes.put(caseId, new SjpCases(caseId, null, newSjpStatus));
            }
        }
        return new ArrayList<>(changes.values());
    }

    /**
     * @return the status to record, or null when the incoming status changes nothing: absent, the
     * same as the current one, or a PENDING arriving after the outcome. Any status but PENDING is an
     * outcome - SUCCESS, FAILED and, for the email, NOT_REQUIRED.
     */
    private static String statusChange(final String current, final String incoming) {
        if (isNull(incoming) || incoming.equals(current)) {
            return null;
        }
        if (PENDING.name().equals(incoming) && nonNull(current)) {
            return null;
        }
        return incoming;
    }

    private void onDocumentDeliveryRecorded(final DocumentDeliveryRecorded recorded) {
        tracked = true;
        if (nonNull(recorded.getMaterialStatus())) {
            materialStatus = recorded.getMaterialStatus();
        }
        if (nonNull(recorded.getEmailStatus())) {
            emailStatus = recorded.getEmailStatus();
        }
        if (nonNull(recorded.getPayloadBlobUri())) {
            payloadBlobUri = recorded.getPayloadBlobUri();
        }
        if (nonNull(recorded.getDocumentBlobUri())) {
            documentBlobUri = recorded.getDocumentBlobUri();
        }
        if (nonNull(recorded.getSjpCases())) {
            recorded.getSjpCases().forEach(sjpCase -> sjpStatusByCaseId.put(sjpCase.getCaseId(), sjpCase.getSjpStatus()));
        }
    }
}
