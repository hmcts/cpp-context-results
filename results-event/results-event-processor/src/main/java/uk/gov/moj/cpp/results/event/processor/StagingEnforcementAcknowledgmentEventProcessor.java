package uk.gov.moj.cpp.results.event.processor;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.JsonObjects.getJsonObject;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;

import java.util.Optional;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class StagingEnforcementAcknowledgmentEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(StagingEnforcementAcknowledgmentEventProcessor.class);
    private static final String ORIGINATOR = "originator";
    private static final String COURTS = "Courts";
    private static final String ATCM = "ATCM";
    private static final String ACKNOWLEDGEMENT = "acknowledgement";
    private static final String REQUEST_ID = "requestId";
    private static final String ERROR_CODE = "errorCode";
    private static final String ACCOUNT_NUMBER = "accountNumber";
    private static final String CORRELATION_ID ="correlationId";
    private static final String MASTER_DEFENDANT_ID ="masterDefendantId";
    private static final String ACCOUNT_CORRELATION_ID ="accountCorrelationId";
    private static final String HEARING_FINANCIAL_RESULT_REQUEST= "hearingFinancialResultRequest";


    @Inject
    private Sender sender;

    @Handles("public.stagingenforcement.enforce-financial-imposition-acknowledgement")
    public void processAcknowledgement(final JsonEnvelope event) {
        final JsonObject enforcementResponsePayload = event.payloadAsJsonObject();
        final Optional<String> originator = JsonObjects.getString(enforcementResponsePayload, ORIGINATOR);

        if (originator.isPresent()
                && (COURTS.equalsIgnoreCase(originator.get()) || ATCM.equalsIgnoreCase(originator.get()))) {
            final Optional<JsonObject> acknowledgement = getJsonObject(enforcementResponsePayload, ACKNOWLEDGEMENT);
            final Optional<String> optionalRequestId = JsonObjects.getString(enforcementResponsePayload, REQUEST_ID);
            final String requestId = optionalRequestId.orElseThrow(() -> new IllegalArgumentException("RequestId is mandatory from enforcement"));

            acknowledgement.map(ack -> JsonObjects.getString(ack, ERROR_CODE))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .ifPresent(error -> LOGGER.error("Acknowledgement has an error {} ", acknowledgement.get()));

            acknowledgement.map(ack -> JsonObjects.getString(ack, ACCOUNT_NUMBER))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .ifPresent(accountNumber -> updateGobAccount(event, accountNumber, requestId));

        }
    }

    @Handles("results.event.hearing-financial-results-tracked")
    public void processHearingFinancialResultsTracked(final JsonEnvelope event){
        final JsonObject enforcementResponsePayload = event.payloadAsJsonObject();
        final JsonObject hearingFinancialResultRequest = enforcementResponsePayload.getJsonObject(HEARING_FINANCIAL_RESULT_REQUEST);
        final String masterDefendantId = hearingFinancialResultRequest.getString(MASTER_DEFENDANT_ID);

        if(hearingFinancialResultRequest.containsKey(ACCOUNT_CORRELATION_ID)){
            final String correlationId = hearingFinancialResultRequest.getString(ACCOUNT_CORRELATION_ID);
            updaterCorrelationId(event, correlationId, masterDefendantId);
        }

    }

    @Handles("public.hearing.nces-email-notification-for-application")
    public void processSendNcesMailForNewApplication(final JsonEnvelope event){
        final Envelope<JsonObject> envelope = envelop(event.payloadAsJsonObject()).withName("result.command.send-nces-email-for-application").withMetadataFrom(event);
        this.sender.sendAsAdmin(envelope);
    }

    private void updateGobAccount(JsonEnvelope event, String accountNumber, String correlationId) {
            final JsonObject commandPayload = createObjectBuilder().add(ACCOUNT_NUMBER, accountNumber).add(CORRELATION_ID, correlationId).build();
            final Envelope<JsonObject> envelope = envelop(commandPayload).withName("result.command.update-gob-account").withMetadataFrom(event);
            this.sender.sendAsAdmin(envelope);
    }

    private void updaterCorrelationId(JsonEnvelope event, String correlationId, String masterDefendantId) {
        final JsonObject commandPayload = createObjectBuilder().add(CORRELATION_ID, correlationId).add(MASTER_DEFENDANT_ID, masterDefendantId).build();
        final Envelope<JsonObject> envelope = envelop(commandPayload).withName("result.command.add-correlation-id").withMetadataFrom(event);
        this.sender.sendAsAdmin(envelope);
    }
}
