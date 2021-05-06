package uk.gov.moj.cpp.results.event.service;

import static java.lang.String.format;
import static javax.json.Json.createObjectBuilder;
import static javax.transaction.Transactional.TxType.REQUIRES_NEW;

import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.api.FileStorer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.results.event.service.exception.FileUploadException;
import uk.gov.moj.cpp.results.event.service.exception.TemplateNameNotFoundException;
import uk.gov.moj.cpp.system.documentgenerator.client.DocumentGeneratorClient;
import uk.gov.moj.cpp.system.documentgenerator.client.DocumentGeneratorClientProducer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S00107"})
public class DocumentGeneratorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentGeneratorService.class);
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String ERROR_MESSAGE = "Error while uploading document generation or upload ";
    public static final String NCES_DOCUMENT_TEMPLATE_NAME = "NCESNotification";
    public static final String NCES_DOCUMENT_ORDER = "NCESDocumentOrder";

    private final DocumentGeneratorClientProducer documentGeneratorClientProducer;

    private final FileStorer fileStorer;

    private final UploadMaterialService uploadMaterialService;

    private final SystemUserProvider systemUserProvider;


    @Inject
    public DocumentGeneratorService(final SystemUserProvider systemUserProvider,
                                    final DocumentGeneratorClientProducer documentGeneratorClientProducer,
                                    final FileStorer fileStorer,
                                    final UploadMaterialService uploadMaterialService
    ) {
        this.systemUserProvider = systemUserProvider;
        this.documentGeneratorClientProducer = documentGeneratorClientProducer;
        this.fileStorer = fileStorer;
        this.uploadMaterialService = uploadMaterialService;
    }


    @Transactional(REQUIRES_NEW)
    public void generateNcesDocument(final Sender sender, final JsonEnvelope originatingEnvelope,
                                     final UUID userId, UUID materialId ) {
        try {
            final JsonObject ncesDocumentJson = originatingEnvelope.payloadAsJsonObject();
            final DocumentGeneratorClient documentGeneratorClient = documentGeneratorClientProducer.documentGeneratorClient();
            final byte[] resultOrderAsByteArray = documentGeneratorClient.generatePdfDocument(ncesDocumentJson, NCES_DOCUMENT_TEMPLATE_NAME, getSystemUserUuid());
            addDocumentToMaterial(sender, originatingEnvelope, getTimeStampAmendedFileName(NCES_DOCUMENT_ORDER),
                    new ByteArrayInputStream(resultOrderAsByteArray), userId,  materialId);

        } catch (IOException | RuntimeException e) {
            LOGGER.error(ERROR_MESSAGE, e);
        }
    }

    private void addDocumentToMaterial(Sender sender, JsonEnvelope originatingEnvelope, final String filename, final InputStream fileContent,
                                       final UUID userId,
                                       final UUID materialId) {

        try {
            //Uploading the file
            final UUID fileId = storeFile(fileContent, filename);
            LOGGER.info("Stored material {} in file store {}", materialId, fileId);
            final UploadMaterialContextBuilder uploadMaterialContextBuilder = new UploadMaterialContextBuilder();
            uploadMaterialService.uploadMaterial(uploadMaterialContextBuilder
                    .setSender(sender)
                    .setOriginatingEnvelope(originatingEnvelope)
                    .setUserId(userId)
                    .setMaterialId(materialId)
                    .setFileId(fileId)
                    .build());

        } catch (final FileServiceException e) {
            LOGGER.error("Error while uploading file {}", filename);

            throw new FileUploadException(e);
        }
    }



    private UUID storeFile(final InputStream fileContent, final String fileName) throws FileServiceException {
        final JsonObject metadata = createObjectBuilder().add("fileName", fileName).build();
        return fileStorer.store(metadata, fileContent);
    }


    private String getTimeStampAmendedFileName(final String fileName) {
        return format("%s_%s.pdf", fileName, ZonedDateTime.now().format(TIMESTAMP_FORMATTER));
    }

    private UUID getSystemUserUuid() {
        return systemUserProvider.getContextSystemUserId().orElseThrow(() -> new TemplateNameNotFoundException("Could not find systemId "));
    }
}
