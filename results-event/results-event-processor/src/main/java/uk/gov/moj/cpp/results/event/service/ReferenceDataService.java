package uk.gov.moj.cpp.results.event.service;

import static java.lang.Boolean.FALSE;
import static java.lang.Integer.valueOf;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static uk.gov.justice.core.courts.AllocationDecision.allocationDecision;
import static uk.gov.justice.core.courts.BailStatus.bailStatus;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.core.courts.AllocationDecision;
import uk.gov.justice.core.courts.BailStatus;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.results.event.helper.resultdefinition.AllResultDefinitions;
import uk.gov.moj.cpp.results.event.helper.resultdefinition.ResultDefinition;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.apache.commons.collections.CollectionUtils;

public class ReferenceDataService {

    private static final String OU_CODE = "oucode";
    private static final String PROSECUTOR_CODE = "prosecutorCode";
    private static final String CONTACT_EMAIL_ADDRESS = "contactEmailAddress";
    private static final String REFERENCE_DATA_QUERY_GET_PROSECUTOR_BY_OUCODE = "referencedata.query.get.prosecutor.by.oucode";
    private static final String REFERENCE_DATA_QUERY_GET_PROSECUTORS = "referencedata.query.prosecutors";
    private static final String REFERENCE_DATA_QUERY_GET_POLICE_COURT_ROOM_CODE = "referencedata.query.get.police-opt-courtroom-ou-courtroom-code";
    private static final String POLICE_FLAG = "policeFlag";
    private static final String SPI_OUT_FLAG = "spiOutFlag";

    @Inject
    private Enveloper enveloper;
    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;
    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Requester requester;

    public Envelope<JsonObject> getOffenceByCjsCode(final String cjsOffenceCode, final JsonEnvelope event) {
        final JsonObject payload = createObjectBuilder().add("cjsoffencecode", cjsOffenceCode).build();
        final JsonEnvelope request = envelopeFrom(metadataFrom(event.metadata()).withName("referencedata.query.offences"), payload);

        return requester.requestAsAdmin(request, JsonObject.class);
    }

    public JsonObject getAllNationality(final JsonEnvelope envelope) {
        final JsonEnvelope request = envelopeFrom(metadataFrom(envelope.metadata()).withName("referencedata.query.country-nationality"), createObjectBuilder().build());
        return requester.requestAsAdmin(request, JsonObject.class).payload();
    }

    public Optional<JsonObject> getNationalityById(final UUID nationalityId, final JsonEnvelope envelope) {
        final JsonObject response = getAllNationality(envelope);
        return response.getJsonArray("countryNationality")
                .getValuesAs(JsonObject.class).stream()
                .filter(nationality -> nonNull(nationality.getString("id", null)))
                .filter(nationality -> nationality.getString("id").equals(nationalityId.toString()))
                .findFirst();
    }

    public Envelope<JsonObject> fetchResultDefinitionById(final JsonEnvelope envelope, final LocalDate on, final UUID id) {
        final JsonEnvelope request = envelopeFrom(metadataFrom(envelope.metadata()).withName("referencedata.get-result-definition"), createObjectBuilder().add("on", on.toString()).add("resultDefinitionId", id.toString()).build());
        return requester.requestAsAdmin(request, JsonObject.class);
    }

    public AllResultDefinitions loadAllResultDefinitions(final JsonEnvelope context, final LocalDate localDate) {
        final String strLocalDate = localDate.toString();
        final JsonEnvelope requestEnvelope = envelopeFrom(metadataFrom(context.metadata()).withName("referencedata.get-all-result-definitions"), createObjectBuilder().add("on", strLocalDate).build());
        final JsonObject jsonResultEnvelope = requester.requestAsAdmin(requestEnvelope, JsonObject.class).payload();
        final AllResultDefinitions allResultDefinitions = jsonObjectToObjectConverter.convert(jsonResultEnvelope, AllResultDefinitions.class);
        allResultDefinitions.getResultDefinitions().forEach(this::trimUserGroups);
        return allResultDefinitions;
    }

    public JsonObject getOrgainsationUnit(final String courtId, final JsonEnvelope envelope) {
        final JsonObject payload = createObjectBuilder().add("id", courtId).build();
        final JsonEnvelope requestEnvelope = envelopeFrom(metadataFrom(envelope.metadata()).withName("referencedata.query.organisation-unit.v2"), payload);
        return requester.requestAsAdmin(requestEnvelope, JsonObject.class).payload();
    }

    public JsonObject getCourtRoomOuCode(final String courtRoomUuid) {
        final JsonObject payload = createObjectBuilder().add("courtRoomUuid", courtRoomUuid).build();
        final JsonEnvelope requestEnvelope = envelopeFrom(metadataBuilder().withName(REFERENCE_DATA_QUERY_GET_POLICE_COURT_ROOM_CODE).withId(randomUUID()).build(), payload);
        return requester.requestAsAdmin(requestEnvelope, JsonObject.class).payload();
    }

    public boolean getSpiOutFlag(final String originatingOrganisation) {
        final JsonObject payload = createObjectBuilder().add(OU_CODE, originatingOrganisation).build();
        final JsonEnvelope request = envelopeFrom(metadataBuilder().withId(randomUUID()).withName(REFERENCE_DATA_QUERY_GET_PROSECUTOR_BY_OUCODE).build(), payload);
        final JsonObject response = requester.requestAsAdmin(request, JsonObject.class).payload();
        return response.getBoolean(SPI_OUT_FLAG);
    }

    public boolean getPoliceFlag(final String originatingOrganisation, final String prosecutionAuthority) {
        Optional<JsonObject> optionalJsonObject;

        if (isNotEmpty(originatingOrganisation)) {
            optionalJsonObject = getProsecutorByOriginatingOrganisation(originatingOrganisation);
        } else if (isNotEmpty(prosecutionAuthority)) {
            optionalJsonObject = getProsecutorByProsecutionAuthority(prosecutionAuthority);
        } else {
            return FALSE;
        }

        if (optionalJsonObject.isPresent()) {
            final JsonObject jsonObject = optionalJsonObject.get();
            return jsonObject.containsKey(POLICE_FLAG) ? jsonObject.getBoolean(POLICE_FLAG) : FALSE;
        }

        return FALSE;
    }

    public Optional<JsonObject> getProsecutorByOriginatingOrganisation(final String originatingOrganisation) {
        final JsonObject payload = createObjectBuilder().add(OU_CODE, originatingOrganisation).build();
        final Metadata metadata = JsonEnvelope.metadataBuilder()
                .withId(randomUUID())
                .withName(REFERENCE_DATA_QUERY_GET_PROSECUTOR_BY_OUCODE)
                .build();

        final JsonEnvelope jsonEnvelope = envelopeFrom(metadata, payload);
        final Envelope<JsonObject> response = requester.requestAsAdmin(jsonEnvelope, JsonObject.class);
        return ofNullable(nonNull(response) ? response.payload() : null);
    }

    public Optional<JsonObject> getProsecutorByProsecutionAuthority(final String prosecutionAuthority) {
        final JsonObject payload = createObjectBuilder().add(PROSECUTOR_CODE, prosecutionAuthority).build();
        final Metadata metadata = JsonEnvelope.metadataBuilder()
                .withId(randomUUID())
                .withName(REFERENCE_DATA_QUERY_GET_PROSECUTORS)
                .build();

        final Envelope<JsonObject> response = requester.requestAsAdmin(envelopeFrom(metadata, payload), JsonObject.class);
        if(CollectionUtils.isEmpty(response.payload().getJsonArray("prosecutors"))) {
            return Optional.empty();
        }
        return ofNullable(response.payload().getJsonArray("prosecutors").getJsonObject(0));
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
        final JsonObject response = requester.requestAsAdmin(request, JsonObject.class).payload();

        return ofNullable(response)
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
        final JsonObject response = requester.requestAsAdmin(request, JsonObject.class).payload();

        return ofNullable(response)
                .map(o -> o.getJsonArray("modeOfTrialReasons"))
                .map(a -> a.getValuesAs(JsonObject.class))
                .map(l -> l.stream()
                        .map(this::convertToAllocationDecision))
                .orElseGet(Stream::empty)
                .collect(toList());
    }


    public String fetchPoliceEmailAddressForProsecutorOuCode(final String ouCode) {
        final JsonObject payload = createObjectBuilder().add(OU_CODE, ouCode).build();
        final JsonEnvelope request = envelopeFrom(metadataBuilder().withId(randomUUID()).withName(REFERENCE_DATA_QUERY_GET_PROSECUTOR_BY_OUCODE).build(), payload);
        final JsonObject response = requester.requestAsAdmin(request, JsonObject.class).payload();
        return response.getString(CONTACT_EMAIL_ADDRESS);
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
