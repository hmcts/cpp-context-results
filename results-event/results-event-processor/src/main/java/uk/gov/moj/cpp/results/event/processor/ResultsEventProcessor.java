package uk.gov.moj.cpp.results.event.processor;

import static java.util.Comparator.comparing;
import static javax.json.Json.createArrayBuilder;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;

import uk.gov.justice.core.courts.BaseStructure;
import uk.gov.justice.core.courts.CaseDetails;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.sjp.results.PublicSjpResulted;
import uk.gov.moj.cpp.domains.HearingHelper;
import uk.gov.moj.cpp.domains.results.shareresults.PublicHearingResulted;
import uk.gov.moj.cpp.results.event.helper.BaseSessionStructureConverterForSjp;
import uk.gov.moj.cpp.results.event.helper.BaseStructureConverter;
import uk.gov.moj.cpp.results.event.helper.CaseDetailsConverterForSjp;
import uk.gov.moj.cpp.results.event.helper.CasesConverter;
import uk.gov.moj.cpp.results.event.helper.ReferenceCache;
import uk.gov.moj.cpp.results.event.service.CacheService;
import uk.gov.moj.cpp.results.event.service.EventGridService;
import uk.gov.moj.cpp.results.event.service.ReferenceDataService;

import java.util.List;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class ResultsEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResultsEventProcessor.class);
    private static final String PROSECUTION_CASE_ID = "prosecutionCaseId";
    private static final String HEARING_IDS = "hearingIds";
    private static final String CASE_ID = "caseId";
    private static final String APPLICATION_ID = "applicationId";
    private static final String HEARING_ID = "id";
    private static final String HEARING = "hearing";
    private static final String RESULTS_COMMAND_HANDLER_CASE_OR_APPLICATION_EJECTED = "results.case-or-application-ejected";
    private static final String CACHE_KEY_SUFFIX = "_result_";

    @Inject
    ReferenceDataService referenceDataService;
    @Inject
    ReferenceCache referenceCache;

    @Inject
    private Enveloper enveloper;

    @Inject
    private Sender sender;

    @Inject
    private HearingHelper hearingHelper;

    @Inject
    private CacheService cacheService;

    @Inject
    private EventGridService eventGridService;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Handles("public.hearing.resulted")
    @SuppressWarnings({"squid:S2221"})
    public void hearingResulted(final JsonEnvelope envelope) {

        LOGGER.info("Hearing Resulted Event Received");

        final JsonObject hearingResultPayload = envelope.payloadAsJsonObject();

        final JsonObject transformedHearing = hearingHelper.transformedHearing(hearingResultPayload.getJsonObject(HEARING));

        final String hearingId = transformedHearing.getString(HEARING_ID);

        final String cacheKey = hearingId + CACHE_KEY_SUFFIX;

        try {
            LOGGER.info("Adding hearing {} to Redis Cache", hearingId);
            cacheService.add(cacheKey, transformedHearing.toString());
        } catch (Exception e) {
            LOGGER.error("Exception caught while attempting to connect to cache service: {}", e);
        }

        try {
            LOGGER.info("Adding Hearing Resulted for hearing {} to EventGrid", hearingId);
            eventGridService.sendHearingResultedEvent(hearingId);
        } catch (Exception e) {
            LOGGER.error("Exception caught while attempting to connect to EventGrid: {}", e);
        }

        sender.sendAsAdmin(enveloper.withMetadataFrom(envelope, "results.command.add-hearing-result").apply(hearingResultPayload));
    }


    @Handles("results.hearing-results-added")
    public void hearingResultAdded(final JsonEnvelope envelope) {

        final JsonObject hearingResultPayload = envelope.payloadAsJsonObject();

        final PublicHearingResulted publicHearingResulted = jsonObjectToObjectConverter.convert(hearingResultPayload, PublicHearingResulted.class);

        if(isNotEmpty(publicHearingResulted.getHearing().getProsecutionCases())) {
            publicHearingResulted.getHearing().getHearingDays().sort(comparing(HearingDay::getSittingDay).reversed());
            final BaseStructure baseStructure = new BaseStructureConverter(referenceDataService).convert(publicHearingResulted);
            final List<CaseDetails> caseDetails = new CasesConverter(referenceCache).convert(publicHearingResulted);
            final JsonArrayBuilder caseDetailsJsonArrayBuilder = createArrayBuilder();
            caseDetails.stream().forEach(c -> caseDetailsJsonArrayBuilder.add(objectToJsonObjectConverter.convert(c)));
            final JsonObjectBuilder resultJsonPayload = Json.createObjectBuilder();
            baseStructure.setSourceType("CC");
            resultJsonPayload.add("session", objectToJsonObjectConverter.convert(baseStructure));
            resultJsonPayload.add("cases", caseDetailsJsonArrayBuilder.build());
            sender.sendAsAdmin(enveloper.withMetadataFrom(envelope, "results.create-results").apply(resultJsonPayload.build()));
        } else if(LOGGER.isDebugEnabled()){
            LOGGER.debug("No Prosecution Cases present for hearing id : {} ", publicHearingResulted.getHearing().getId());
        }
    }


    @Handles("results.event.police-result-generated")
    public void createResult(final JsonEnvelope envelope) {
        LOGGER.debug("results.event.police-result-generated {}", envelope.payload());
        sender.sendAsAdmin(enveloper.withMetadataFrom(envelope, "public.results.police-result-generated").apply(envelope.payload()));
    }

    @Handles("public.sjp.case-resulted")
    public void sjpCaseResulted(final JsonEnvelope envelope) {

        final JsonObject sjpResultedPayload = envelope.payloadAsJsonObject();

        LOGGER.debug("public.sjp.case-resulted event received {}", sjpResultedPayload);
        final PublicSjpResulted publicSjpCaseResulted = jsonObjectToObjectConverter.convert(sjpResultedPayload, PublicSjpResulted.class);

        final BaseStructure baseStructure = new BaseSessionStructureConverterForSjp().convert(publicSjpCaseResulted);
        final List<CaseDetails> caseDetails = new CaseDetailsConverterForSjp(referenceCache).convert(publicSjpCaseResulted);
        final JsonArrayBuilder caseDetailsJsonArrayBuilder = createArrayBuilder();
        caseDetails.stream().forEach(c -> caseDetailsJsonArrayBuilder.add(objectToJsonObjectConverter.convert(c)));
        final JsonObjectBuilder resultJsonPayload = Json.createObjectBuilder();
        baseStructure.setSourceType("SJP");
        resultJsonPayload.add("session", objectToJsonObjectConverter.convert(baseStructure));
        resultJsonPayload.add("cases", caseDetailsJsonArrayBuilder.build());

        sender.sendAsAdmin(enveloper.withMetadataFrom(envelope, "results.create-results").apply(resultJsonPayload.build()));
    }


    @Handles("public.progression.events.case-or-application-ejected")
    public void handleCaseOrApplicationEjected(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();
        if (payload.containsKey(HEARING_IDS)) {
            final JsonArray hearingIds = payload.getJsonArray(HEARING_IDS);
            final Metadata metadata = metadataFrom(envelope.metadata())
                    .withName(RESULTS_COMMAND_HANDLER_CASE_OR_APPLICATION_EJECTED)
                    .build();
            if(payload.containsKey(PROSECUTION_CASE_ID)) {
                final String caseId = payload.getString(PROSECUTION_CASE_ID);
                    final JsonObject caseEjectedCommandPayload = Json.createObjectBuilder()
                            .add(HEARING_IDS,hearingIds)
                            .add(CASE_ID, caseId)
                            .build();
                    sender.sendAsAdmin(JsonEnvelope.envelopeFrom(metadata, caseEjectedCommandPayload));
            } else  {
                final String applicationId = payload.getString(APPLICATION_ID);
                    final JsonObject applicationEjectedCommandPayload = Json.createObjectBuilder()
                            .add(HEARING_IDS, hearingIds)
                            .add(APPLICATION_ID, applicationId)
                            .build();
                    sender.sendAsAdmin(JsonEnvelope.envelopeFrom(metadata, applicationEjectedCommandPayload));
            }

        } else  {
            if(LOGGER.isInfoEnabled()) {
                LOGGER.info("The Payload has been ignored as it does not contain hearing ids : {}" ,  envelope.toObfuscatedDebugString());
            }
        }
    }
}
