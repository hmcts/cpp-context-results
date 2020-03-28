package uk.gov.moj.cpp.results.event.service;

import static java.lang.Integer.valueOf;
import static java.time.LocalDate.parse;
import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.core.courts.AllocationDecision.allocationDecision;
import static uk.gov.justice.core.courts.BailStatus.bailStatus;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.core.courts.AllocationDecision;
import uk.gov.justice.core.courts.BailStatus;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.results.event.helper.resultdefinition.AllResultDefinitions;
import uk.gov.moj.cpp.results.event.helper.resultdefinition.ResultDefinition;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonObject;

public class ReferenceDataService {

    @Inject
    private Enveloper enveloper;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Requester requester;

    // TODO: cjsOffenceCode is a queryParameter not a uriParameter so check this actually works..
    public JsonEnvelope getOffenceByCjsCode(final String cjsOffenceCode, final JsonEnvelope event) {
        final JsonObject payload = createObjectBuilder().add("cjsoffencecode", cjsOffenceCode).build();
        final JsonEnvelope request = enveloper.withMetadataFrom(event, "referencedata.query.offences").apply(payload);
        return requester.requestAsAdmin(request);
    }

    public JsonObject getAllNationality(final JsonEnvelope envelope) {
        final JsonEnvelope request = enveloper.withMetadataFrom(envelope, "referencedata.query.country-nationality").apply(createObjectBuilder().build());
        return requester.requestAsAdmin(request).payloadAsJsonObject();
    }

    public Optional<JsonObject> getNationalityById(final UUID nationalityId, final JsonEnvelope envelope) {
        final JsonObject response = getAllNationality(envelope);
        return response.getJsonArray("countryNationality")
                .getValuesAs(JsonObject.class).stream()
                .filter(nationality -> nonNull(nationality.getString("id", null)))
                .filter(nationality -> nationality.getString("id").equals(nationalityId.toString()))
                .findFirst();
    }

    public JsonEnvelope getResultDefinitionById(final JsonEnvelope envelope, final LocalDate on, final UUID id) {
        return requester.requestAsAdmin(enveloper.withMetadataFrom(envelope, "referencedata.get-result-definition")
                .apply(createObjectBuilder().add("on", on.toString()).add("resultDefinitionId", id.toString()).build()));
    }

    public AllResultDefinitions loadAllResultDefinitions(final JsonEnvelope context, final LocalDate localDate) {
        final String strLocalDate = localDate.toString();
        final JsonEnvelope requestEnvelope = enveloper.withMetadataFrom(context, "referencedata.get-all-result-definitions")
                .apply(createObjectBuilder().add("on", strLocalDate).build());

        final JsonEnvelope jsonResultEnvelope = requester.requestAsAdmin(requestEnvelope);
        final AllResultDefinitions allResultDefinitions = jsonObjectToObjectConverter.convert(jsonResultEnvelope.payloadAsJsonObject(), AllResultDefinitions.class);
        allResultDefinitions.getResultDefinitions().forEach(rd -> trimUserGroups(rd));
        return allResultDefinitions;
    }

    public JsonEnvelope getOrgainsationUnit(final String courtId, final JsonEnvelope envelope) {
        final JsonObject payload = createObjectBuilder().add("id", courtId).build();
        final JsonEnvelope request = enveloper.withMetadataFrom(envelope, "referencedata.query.organisation-unit.v2").apply(payload);
        return requester.requestAsAdmin(request);
    }

    public boolean getSpiOutFlagForProsecutorOucode(final String oucode) {
        final JsonObject payload = createObjectBuilder().add("oucode", oucode).build();
        final JsonEnvelope request = envelopeFrom(metadataBuilder().withId(randomUUID()).withName("referencedata.query.get.prosecutor.by.oucode").build(), payload);
        final JsonObject response = requester.requestAsAdmin(request).payloadAsJsonObject();
        return response.getBoolean("spiOutFlag");
    }

    private void trimUserGroups(final ResultDefinition resultDefinition) {
        resultDefinition.setUserGroups(trim(resultDefinition.getUserGroups()));
        resultDefinition.getPrompts().forEach(p -> p.setUserGroups(trim(p.getUserGroups())));
    }

    private List<String> trim(final List<String> strs) {
        return strs == null ? null : strs.stream().map(s -> {
            if (s == null) {
                return null;
            } else {
                return s.trim();
            }
        }).collect(toList());
    }

    public List<BailStatus> getAllBailStatuses(final JsonEnvelope context) {
        final JsonObject payload = createObjectBuilder().build();
        final Metadata metadata = envelop(payload).withName("referencedata.query.bail-statuses").withMetadataFrom(context).metadata();
        final JsonEnvelope request = envelopeFrom(metadata, payload);
        final JsonEnvelope response = requester.requestAsAdmin(request);

        return Optional.ofNullable(response)
                .map(JsonEnvelope::payloadAsJsonObject)
                .map(o -> o.getJsonArray("bailStatuses"))
                .map(a -> a.getValuesAs(JsonObject.class))
                .map(l -> l.stream()
                        .map(this::convertBailStatus))
                .orElseGet(Stream::empty)
                .collect(toList());

    }

    private BailStatus convertBailStatus(final JsonObject source) {
        return bailStatus()
                .withId(fromString(source.getString("id", null)))
                .withCode(source.getString("statusCode", null))
                .withDescription(source.getString("statusDescription", null))
                .build();

    }


    public List<AllocationDecision> getAllModeOfTrialReasons(final JsonEnvelope context) {
        final JsonObject payload = createObjectBuilder().build();
        final Metadata metadata = envelop(payload).withName("referencedata.query.mode-of-trial-reasons").withMetadataFrom(context).metadata();
        final JsonEnvelope request = envelopeFrom(metadata, payload);
        final JsonEnvelope response = requester.requestAsAdmin(request);

        return Optional.ofNullable(response)
                .map(JsonEnvelope::payloadAsJsonObject)
                .map(o -> o.getJsonArray("modeOfTrialReasons"))
                .map(a -> a.getValuesAs(JsonObject.class))
                .map(l -> l.stream()
                        .map(this::convertToAllocationDecision))
                .orElseGet(Stream::empty)
                .collect(toList());
    }


    private AllocationDecision convertToAllocationDecision(final JsonObject jsonObject) {
        return allocationDecision()
                .withMotReasonId(fromString(jsonObject.getString("id")))
                .withSequenceNumber(valueOf(jsonObject.getInt("seqNum")))
                .withMotReasonCode(jsonObject.getString("code"))
                .withMotReasonDescription(jsonObject.getString("description"))
                .build();
    }
}
