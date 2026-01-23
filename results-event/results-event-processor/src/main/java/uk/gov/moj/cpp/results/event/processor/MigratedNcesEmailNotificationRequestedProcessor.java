package uk.gov.moj.cpp.results.event.processor;

import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;

import uk.gov.justice.core.courts.CaseDocument;
import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.DocumentCategory;
import uk.gov.justice.core.courts.Material;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.results.event.helper.Originator;
import uk.gov.moj.cpp.results.event.helper.ReferenceCache;
import uk.gov.moj.cpp.results.event.service.DocumentGeneratorService;
import uk.gov.moj.cpp.results.event.service.FileParams;
import uk.gov.moj.cpp.results.event.service.ReferenceDataService;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class MigratedNcesEmailNotificationRequestedProcessor {
    private static final String MATERIAL_ID = "materialId";
    private static final String COURT_DOCUMENT = "courtDocument";
    private static final String PROGRESSION_ADD_COURT_DOCUMENT = "progression.add-court-document";
    private static final String DOCUMENT_TYPE_DESCRIPTION = "Electronic Notifications";
    private static final UUID CASE_DOCUMENT_TYPE_ID = fromString("f471eb51-614c-4447-bd8d-28f9c2815c9e");
    private static final String CASE_ID = "caseId";
    private static final String APPLICATION_PDF = "application/pdf";
    private static final String MASTER_DEFENDANT_ID = "masterDefendantId";
    private static final Logger LOGGER = LoggerFactory.getLogger(MigratedNcesEmailNotificationRequestedProcessor.class);

    @Inject
    ReferenceDataService referenceDataService;

    @Inject
    ReferenceCache referenceCache;
    @Inject
    private Sender sender;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private DocumentGeneratorService documentGeneratorService;

    @Handles("results.event.migrated-inactive-nces-email-notification-requested")
    public void handleMigratedNcesEmailNotificationRequested(final JsonEnvelope envelope) {
        final UUID userId = fromString(envelope.metadata().userId().orElseThrow(() -> new RuntimeException("UserId missing from event.")));
        final UUID caseUUID = fromString(envelope.payloadAsJsonObject().getString(CASE_ID));
        final UUID masterDefendsntUUID = fromString(envelope.payloadAsJsonObject().getString(MASTER_DEFENDANT_ID));
        final String rootAggregateId = masterDefendsntUUID.toString() + "-" + caseUUID.toString();


        final UUID materialId = UUID.fromString(envelope.payloadAsJsonObject().getString(MATERIAL_ID));
        final FileParams fileParams = documentGeneratorService.generateNcesDocument(sender, envelope, userId, materialId, Originator.ORIGINATOR_VALUE_NCES_CASEID.concat(rootAggregateId));

        addCourtDocumentForCCCase(envelope, caseUUID, materialId, fileParams.getFilename());
        LOGGER.info("In CC case Nces notification requested payload for add court document- fileid {} case UUID {}", fileParams.getFileId(), caseUUID);

    }

    private void addCourtDocumentForCCCase(final JsonEnvelope envelope, final UUID caseUUID, final UUID materialId, final String fileName) {
        LOGGER.info("addCourtDocumentForCCCase caseUUID {} , fileName {} , materialId {}", caseUUID, fileName, materialId);
        final CourtDocument courtDocument = buildCourtDocument(caseUUID, materialId, fileName);
        final JsonObject jsonObject = createObjectBuilder()
                .add(MATERIAL_ID, materialId.toString())
                .add(COURT_DOCUMENT, objectToJsonObjectConverter.convert(courtDocument))
                .build();
        final Envelope<JsonObject> data = envelopeFrom(JsonEnvelope.metadataFrom(envelope.metadata())
                .withName(PROGRESSION_ADD_COURT_DOCUMENT), jsonObject);
        sender.send(data);
    }

    private CourtDocument buildCourtDocument(UUID caseUUID, UUID materialId, String fileName) {
        final DocumentCategory documentCategory = DocumentCategory.documentCategory()
                .withCaseDocument(CaseDocument.caseDocument()
                        .withProsecutionCaseId(caseUUID)
                        .build())
                .build();

        final Material material = Material.material().withId(materialId)
                .withReceivedDateTime(ZonedDateTime.now())
                .build();

        return CourtDocument.courtDocument()
                .withCourtDocumentId(randomUUID())
                .withDocumentCategory(documentCategory)
                .withDocumentTypeDescription(DOCUMENT_TYPE_DESCRIPTION)
                .withDocumentTypeId(CASE_DOCUMENT_TYPE_ID)
                .withMimeType(APPLICATION_PDF)
                .withName(fileName)
                .withMaterials(Collections.singletonList(material))
                .withSendToCps(false)
                .withContainsFinancialMeans(false)
                .build();
    }
}
