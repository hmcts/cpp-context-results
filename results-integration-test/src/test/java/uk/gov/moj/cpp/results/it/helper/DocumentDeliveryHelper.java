package uk.gov.moj.cpp.results.it.helper;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.messaging.MessageProducerClient;
import uk.gov.justice.services.test.utils.persistence.TestJdbcConnectionProvider;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

/**
 * Reads the delivery tracking of a document from the results view store, and raises the public
 * events other contexts report its outcome with.
 */
public class DocumentDeliveryHelper {

    private static final String PUBLIC_EVENT_TOPIC = "public.event";
    private static final String RESULTS_CONTEXT = "results";
    private static final int TIMEOUT_IN_SECONDS = 30;

    private static final String SELECT_DELIVERY = "SELECT document_type, material_status, payload_blob_uri, document_blob_uri, email_status "
            + "FROM results_document_delivery WHERE material_id = ?";
    private static final String SELECT_SJP_CASE = "SELECT sjp_status FROM results_nces_sjp_case WHERE material_id = ? AND case_id = ?";

    private final TestJdbcConnectionProvider connectionProvider = new TestJdbcConnectionProvider();

    public record DocumentDelivery(String documentType, String materialStatus, String payloadBlobUri, String documentBlobUri, String emailStatus) {
    }

    public DocumentDelivery awaitDelivery(final UUID materialId) {
        await().atMost(TIMEOUT_IN_SECONDS, SECONDS).pollInterval(1, SECONDS).until(() -> findDelivery(materialId).isPresent());
        return findDelivery(materialId).orElseThrow();
    }

    public void awaitMaterialStatus(final UUID materialId, final String materialStatus) {
        await().atMost(TIMEOUT_IN_SECONDS, SECONDS).pollInterval(1, SECONDS)
                .until(() -> findDelivery(materialId).map(DocumentDelivery::materialStatus).orElse(null), is(materialStatus));
    }

    public void awaitEmailStatus(final UUID materialId, final String emailStatus) {
        await().atMost(TIMEOUT_IN_SECONDS, SECONDS).pollInterval(1, SECONDS)
                .until(() -> findDelivery(materialId).map(DocumentDelivery::emailStatus).orElse(null), is(emailStatus));
    }

    public void awaitSjpStatus(final UUID materialId, final UUID caseId, final String sjpStatus) {
        await().atMost(TIMEOUT_IN_SECONDS, SECONDS).pollInterval(1, SECONDS)
                .until(() -> findSjpStatus(materialId, caseId).orElse(null), is(sjpStatus));
    }

    public Optional<DocumentDelivery> findDelivery(final UUID materialId) {
        try (final Connection connection = connectionProvider.getViewStoreConnection(RESULTS_CONTEXT);
             final PreparedStatement statement = connection.prepareStatement(SELECT_DELIVERY)) {
            statement.setObject(1, materialId);
            try (final ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next()
                        ? Optional.of(new DocumentDelivery(resultSet.getString(1), resultSet.getString(2), resultSet.getString(3),
                        resultSet.getString(4), resultSet.getString(5)))
                        : Optional.empty();
            }
        } catch (final SQLException e) {
            throw new IllegalStateException("Failed to read the delivery of material " + materialId, e);
        }
    }

    public Optional<String> findSjpStatus(final UUID materialId, final UUID caseId) {
        try (final Connection connection = connectionProvider.getViewStoreConnection(RESULTS_CONTEXT);
             final PreparedStatement statement = connection.prepareStatement(SELECT_SJP_CASE)) {
            statement.setObject(1, materialId);
            statement.setObject(2, caseId);
            try (final ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(resultSet.getString(1)) : Optional.empty();
            }
        } catch (final SQLException e) {
            throw new IllegalStateException("Failed to read SJP case " + caseId + " of material " + materialId, e);
        }
    }

    public void raisePublicEvent(final String eventName, final JsonObject payload) {
        raisePublicEvent(metadataWithRandomUUID(eventName).build(), payload);
    }

    /**
     * material.material-added as material raises it for an NCES document results uploaded by its blob
     * uri: material echoes the originator results sent with the upload back on the event's metadata.
     */
    public void raiseMaterialAddedForNcesFileUriMaterial(final UUID materialId, final UUID userId) {
        raiseMaterialAddedForFileUriMaterial(materialId, userId, "nces");
    }

    /**
     * As {@link #raiseMaterialAddedForNcesFileUriMaterial} for the NCES document of a migrated
     * inactive case, whose originator also names the master defendant and the case.
     */
    public void raiseMaterialAddedForMigratedInactiveNcesFileUriMaterial(final UUID materialId, final UUID userId,
                                                                         final String masterDefendantId, final String caseId) {
        raiseMaterialAddedForFileUriMaterial(materialId, userId, "nces:" + masterDefendantId + ":" + caseId);
    }

    private static void raiseMaterialAddedForFileUriMaterial(final UUID materialId, final UUID userId, final String ncesOriginator) {
        final String eventName = "material.material-added";
        final JsonObjectBuilder metadata = createObjectBuilder(metadataWithRandomUUID(eventName).withUserId(userId.toString()).build().asJsonObject())
                .add("originator-nces", ncesOriginator)
                .add("originator-file-uri", true);

        raisePublicEvent(metadataFrom(metadata.build()).build(), createObjectBuilder()
                .add("materialId", materialId.toString())
                .add("fileDetails", createObjectBuilder()
                        .add("alfrescoAssetId", "aGVsbG8=")
                        .add("mimeType", "application/pdf")
                        .add("fileName", "ENFDocumentOrder.pdf"))
                .add("materialAddedDate", "2026-11-12T10:00:00.000Z")
                .build());
    }

    private static void raisePublicEvent(final Metadata metadata, final JsonObject payload) {
        try (final MessageProducerClient messageProducer = new MessageProducerClient()) {
            messageProducer.startProducer(PUBLIC_EVENT_TOPIC);
            messageProducer.sendMessage(metadata.name(), envelopeFrom(metadata, payload));
        }
    }
}
