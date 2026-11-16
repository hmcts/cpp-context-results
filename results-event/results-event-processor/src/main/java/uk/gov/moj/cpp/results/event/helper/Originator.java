package uk.gov.moj.cpp.results.event.helper;

import static java.util.Objects.nonNull;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonMetadata.ID;
import static uk.gov.justice.services.messaging.JsonMetadata.NAME;
import static uk.gov.justice.services.messaging.JsonMetadata.USER_ID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import java.util.Locale;
import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public class Originator {

    public static final String CONTEXT = "context";
    public static final String SOURCE = "originator";
    public static final String ORIGINATOR_VALUE = "court";
    public static final String SOURCE_NCES = "originator-nces";
    public static final String ORIGINATOR_VALUE_NCES = "nces";
    public static final String ORIGINATOR_VALUE_NCES_CASEID = "nces:";
    /**
     * Put on the metadata of a material results uploads by its blob uri (the file uri path); material
     * echoes it back on its events, as it does the originator.
     */
    public static final String ORIGINATOR_FILE_URI = "originator-file-uri";

    private Originator() {
    }

    /**
     * An NCES document results uploaded to material - the originator results puts on the upload's
     * metadata, which material echoes back on its events.
     */
    public static boolean isNcesMaterial(final Metadata metadata) {
        return ORIGINATOR_VALUE_NCES.equalsIgnoreCase(ncesOriginatorOf(metadata));
    }

    /**
     * A migrated inactive NCES document results uploaded to material: its originator carries the
     * master defendant and case ids after the prefix.
     */
    public static boolean isMigratedInactiveNcesMaterial(final Metadata metadata) {
        final String ncesOriginator = ncesOriginatorOf(metadata);
        return nonNull(ncesOriginator) && ncesOriginator.toLowerCase(Locale.ROOT).startsWith(ORIGINATOR_VALUE_NCES_CASEID);
    }

    /**
     * A material results uploaded - an NCES document, a migrated inactive one or a police
     * notification - as opposed to one of another context.
     */
    public static boolean isResultsMaterial(final Metadata metadata) {
        return isNcesMaterial(metadata) || isMigratedInactiveNcesMaterial(metadata);
    }

    /**
     * A material results uploaded by its blob uri - the file uri path, the only one whose delivery is
     * tracked.
     */
    public static boolean isFileUriMaterial(final Metadata metadata) {
        return metadata.asJsonObject().getBoolean(ORIGINATOR_FILE_URI, false);
    }

    private static String ncesOriginatorOf(final Metadata metadata) {
        return metadata.asJsonObject().getString(SOURCE_NCES, null);
    }


    public static Metadata createMetadataWithProcessIdAndUserId(final String id, final String name, final String userId, final String ncesOriginatorValue) {
        return metadataFrom(metadataBuilder(id, name, userId, ncesOriginatorValue).build()).build();
    }

    /**
     * As {@link #createMetadataWithProcessIdAndUserId} for a material uploaded by its blob uri (the
     * file uri path), which the metadata marks.
     */
    public static Metadata createFileUriMetadataWithProcessIdAndUserId(final String id, final String name, final String userId, final String ncesOriginatorValue) {
        return metadataFrom(metadataBuilder(id, name, userId, ncesOriginatorValue).add(ORIGINATOR_FILE_URI, true).build()).build();
    }

    private static JsonObjectBuilder metadataBuilder(final String id, final String name, final String userId, final String ncesOriginatorValue) {
        final JsonObjectBuilder builder = createObjectBuilder()
                .add(ID, id)
                .add(NAME, name)
                .add(SOURCE, ORIGINATOR_VALUE)
                .add(SOURCE_NCES, ncesOriginatorValue);

        if (nonNull(userId)) {
            builder.add(CONTEXT, createObjectBuilder()
                   .add(USER_ID, userId));
        }

        return builder;
    }

    public static JsonEnvelope assembleEnvelopeWithPayloadAndMetaDetails(final JsonObject payload, final String contentType, final String userId, final String ncesOriginatorValue) {
        final Metadata metadata = createMetadataWithProcessIdAndUserId(UUID.randomUUID().toString(), contentType, userId, ncesOriginatorValue);
        return envelopeFrom(metadata, addMetadataToPayload(payload, metadata));
    }

    /**
     * As {@link #assembleEnvelopeWithPayloadAndMetaDetails} for a material uploaded by its blob uri
     * (the file uri path), which the metadata marks.
     */
    public static JsonEnvelope assembleFileUriEnvelopeWithPayloadAndMetaDetails(final JsonObject payload, final String contentType, final String userId, final String ncesOriginatorValue) {
        final Metadata metadata = createFileUriMetadataWithProcessIdAndUserId(UUID.randomUUID().toString(), contentType, userId, ncesOriginatorValue);
        return envelopeFrom(metadata, addMetadataToPayload(payload, metadata));
    }

    private static JsonObject addMetadataToPayload(final JsonObject load, final Metadata metadata) {
        final JsonObjectBuilder job = createObjectBuilder();
        load.entrySet().forEach(entry -> job.add(entry.getKey(), entry.getValue()));
        job.add(JsonEnvelope.METADATA, metadata.asJsonObject());
        return job.build();
    }
}

