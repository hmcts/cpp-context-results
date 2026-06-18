package uk.gov.moj.cpp.results.query.view;

import static java.util.UUID.fromString;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.of;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.getString;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.domains.constant.RegisterStatus;
import uk.gov.moj.cpp.results.persist.InformantRegisterRepository;
import uk.gov.moj.cpp.results.persist.entity.InformantRegisterEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import org.apache.commons.lang3.StringUtils;

@ServiceComponent(Component.QUERY_VIEW)
public class InformantRegisterDocumentRequestQueryView {
    private static final String FIELD_REQUEST_STATUS = "requestStatus";
    private static final String FIELD_INFORMANT_REGISTER_DOCUMENTS = "informantRegisterDocumentRequests";
    private static final String FIELD_FILE_ID = "fileId";
    private static final String FIELD_PROSECUTION_AUTHORITY_CODE = "prosecutionAuthorityCode";
    private static final String FIELD_REGISTER_DATE = "registerDate";
    private static final String FIELD_PAYLOAD = "payload";
    private static final String FIELD_VERDICT = "verdict";
    private static final String FIELD_VERDICT_CODE = "verdictCode";
    private static final String FIELD_VERDICT_TYPE = "verdictType";
    private static final String FIELD_VERDICT_DATE = "verdictDate";

    @Inject
    private InformantRegisterRepository informantRegisterRepository;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Handles("results.query.informant-register-document-request")
    public JsonEnvelope getInformantRegisterRequests(final JsonEnvelope envelope) {
        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();
        final JsonArrayBuilder jsonArrayBuilder = createArrayBuilder();
        final String requestStatus = envelope.payloadAsJsonObject().getString(FIELD_REQUEST_STATUS);
        if (isNotBlank(requestStatus)) {
            if(RegisterStatus.RECORDED.toString().equalsIgnoreCase(requestStatus)) {
                final List<InformantRegisterEntity> informantRegisterEntities = informantRegisterRepository.findByStatusRecorded();
                informantRegisterEntities.forEach(informantRegisterEntity -> jsonArrayBuilder.add(convertWithNormalisedVerdict(informantRegisterEntity)));
            } else {
                final List<InformantRegisterEntity> informantRegisterEntities = informantRegisterRepository.findByStatus(RegisterStatus.valueOf(requestStatus));
                informantRegisterEntities.forEach(informantRegisterEntity -> jsonArrayBuilder.add(convertWithNormalisedVerdict(informantRegisterEntity)));
            }
        }
        return envelopeFrom(envelope.metadata(),
                jsonObjectBuilder.add(FIELD_INFORMANT_REGISTER_DOCUMENTS, jsonArrayBuilder.build()).build());
    }

    /**
     * Converts the entity to JSON and normalises every offence in the stored payload so that the
     * verdict is always represented as a structured {@code verdict} object. Legacy payloads that
     * carry only a bare {@code verdictCode} string on the offence are rewritten to
     * {@code {"verdictCode": <code>, "verdictType": null, "verdictDate": null}}; payloads that
     * already hold a {@code verdict} object are left untouched.
     */
    private JsonObject convertWithNormalisedVerdict(final InformantRegisterEntity informantRegisterEntity) {
        final JsonObject converted = objectToJsonObjectConverter.convert(informantRegisterEntity);
        final String payload = converted.getString(FIELD_PAYLOAD, null);
        if (StringUtils.isBlank(payload)) {
            return converted;
        }
        final String normalisedPayload = deepTransformObject(stringToJsonObjectConverter.convert(payload)).toString();
        final JsonObjectBuilder builder = createObjectBuilder();
        converted.forEach((key, value) -> {
            if (FIELD_PAYLOAD.equals(key)) {
                builder.add(FIELD_PAYLOAD, normalisedPayload);
            } else {
                builder.add(key, value);
            }
        });
        return builder.build();
    }

    private JsonObject deepTransformObject(final JsonObject object) {
        final boolean wrapVerdictCode = object.containsKey(FIELD_VERDICT_CODE) && !object.containsKey(FIELD_VERDICT);
        final JsonObjectBuilder builder = createObjectBuilder();
        object.forEach((key, value) -> {
            if (wrapVerdictCode && FIELD_VERDICT_CODE.equals(key)) {
                return;
            }
            if (!FIELD_VERDICT.equals(key) && value.getValueType() == JsonValue.ValueType.OBJECT) {
                builder.add(key, deepTransformObject(value.asJsonObject()));
            } else if (value.getValueType() == JsonValue.ValueType.ARRAY) {
                builder.add(key, deepTransformArray(value.asJsonArray()));
            } else {
                builder.add(key, value);
            }
        });
        if (wrapVerdictCode) {
            builder.add(FIELD_VERDICT, createObjectBuilder()
                    .add(FIELD_VERDICT_CODE, object.getString(FIELD_VERDICT_CODE))
                    .addNull(FIELD_VERDICT_TYPE)
                    .addNull(FIELD_VERDICT_DATE)
                    .build());
        }
        return builder.build();
    }

    private JsonArray deepTransformArray(final JsonArray array) {
        final JsonArrayBuilder builder = createArrayBuilder();
        array.forEach(item -> {
            if (item.getValueType() == JsonValue.ValueType.OBJECT) {
                builder.add(deepTransformObject(item.asJsonObject()));
            } else if (item.getValueType() == JsonValue.ValueType.ARRAY) {
                builder.add(deepTransformArray(item.asJsonArray()));
            } else {
                builder.add(item);
            }
        });
        return builder.build();
    }

    @Handles("results.query.informant-register-document-by-material")
    public JsonEnvelope getInformantRegistersByMaterial(final JsonEnvelope envelope) {
        final UUID fileId = fromString(envelope.payloadAsJsonObject().getString(FIELD_FILE_ID));

        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();
        final JsonArrayBuilder jsonArrayBuilder = createArrayBuilder();

        final List<InformantRegisterEntity> informantRegisterEntities = informantRegisterRepository.findByFileId(fileId);
        informantRegisterEntities.forEach(informantRegisterEntity -> jsonArrayBuilder.add(objectToJsonObjectConverter.convert(informantRegisterEntity)));

        return envelopeFrom(envelope.metadata(),
                jsonObjectBuilder.add(FIELD_INFORMANT_REGISTER_DOCUMENTS, jsonArrayBuilder.build()).build());
    }

    @Handles("results.query.informant-register-document-by-request-date")
    public JsonEnvelope getInformantRegistersByRequestDate(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();
        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();
        final JsonArrayBuilder jsonArrayBuilder = createArrayBuilder();

        final Optional<LocalDate> registerDate = getString(payload, FIELD_REGISTER_DATE).map(LocalDate::parse);

        registerDate.ifPresent(regDate -> {
            List<InformantRegisterEntity> informantRegisterEntities;
            final String prosecutionAuthoritiesAsString = payload.getString(FIELD_PROSECUTION_AUTHORITY_CODE, "");
            final Set<String> prosecutionAuthorities = of(prosecutionAuthoritiesAsString.split(","))
                    .map(String::trim)
                    .filter(StringUtils::isNotEmpty)
                    .collect(toSet());

            if (prosecutionAuthorities.isEmpty()) {
                informantRegisterEntities = informantRegisterRepository.findByRegisterDate(regDate);
            } else {
                informantRegisterEntities = prosecutionAuthorities.stream()
                        .map(prosecutionAuthority -> informantRegisterRepository.findByRegisterDateAndProsecutionAuthorityCode(regDate, prosecutionAuthority))
                        .flatMap(List::stream)
                        .collect(Collectors.toList());
            }
            informantRegisterEntities.forEach(i -> jsonArrayBuilder.add(objectToJsonObjectConverter.convert(i)));
        });

        return envelopeFrom(envelope.metadata(),
                jsonObjectBuilder.add(FIELD_INFORMANT_REGISTER_DOCUMENTS, jsonArrayBuilder.build()).build());
    }
}
