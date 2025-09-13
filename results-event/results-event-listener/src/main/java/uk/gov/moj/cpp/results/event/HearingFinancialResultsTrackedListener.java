package uk.gov.moj.cpp.results.event;

import static javax.json.Json.createArrayBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.hearing.courts.HearingFinancialResultsTracked;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.results.persist.DefendantGobAccountsEntity;
import uk.gov.moj.cpp.results.persist.DefendantGobAccountsRepository;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_LISTENER)
public class HearingFinancialResultsTrackedListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingFinancialResultsTrackedListener.class);
    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private DefendantGobAccountsRepository defendantGobAccountsRepository;

    @Handles("results.event.hearing-financial-results-tracked")
    public void processDefendantGobAccounts(final JsonEnvelope event) {
        final JsonObject requestJson = event.payloadAsJsonObject();
        final HearingFinancialResultsTracked hearingFinancialResultsTracked = jsonObjectToObjectConverter.convert(requestJson, HearingFinancialResultsTracked.class);
        saveDefendantGobAccounts(hearingFinancialResultsTracked);
    }

    private void saveDefendantGobAccounts(HearingFinancialResultsTracked hearingFinancialResultsTracked) {
        final var hearingFinancialResultRequest = hearingFinancialResultsTracked.getHearingFinancialResultRequest();
        final UUID masterDefendantId = hearingFinancialResultRequest.getMasterDefendantId();
        final UUID correlationId = hearingFinancialResultRequest.getAccountCorrelationId();
        final UUID hearingId = hearingFinancialResultRequest.getHearingId();
        
        // Only save if correlationId is present
        if (correlationId != null) {
            defendantGobAccountsRepository.save(createDefendantGobAccountsEntity(hearingFinancialResultsTracked));
            LOGGER.info("Correlation details are saved successfully stored for masterDefendantId id & correlationId id & hearingId : {} & {} & {}", masterDefendantId, correlationId, hearingId);
        } else {
            LOGGER.info("Skipping Correlation details save - no correlationId present for masterDefendantId id & hearingId : {} & {}", masterDefendantId, hearingId);
        }
    }

    private DefendantGobAccountsEntity createDefendantGobAccountsEntity(HearingFinancialResultsTracked hearingFinancialResultsTracked) {
        final var hearingFinancialResultRequest = hearingFinancialResultsTracked.getHearingFinancialResultRequest();
        final DefendantGobAccountsEntity defendantGobAccountsEntity = new DefendantGobAccountsEntity();
        defendantGobAccountsEntity.setMasterDefendantId(hearingFinancialResultRequest.getMasterDefendantId());
        defendantGobAccountsEntity.setCorrelationId(hearingFinancialResultRequest.getAccountCorrelationId());
        // account_number is not set here as it may not be available if GOB happens in one go after multiple amendments
        defendantGobAccountsEntity.setCaseReferences(convertCaseReferencesToJsonArray(hearingFinancialResultRequest.getProsecutionCaseReferences()).toString());
        defendantGobAccountsEntity.setCreatedTime(ZonedDateTime.now());
        defendantGobAccountsEntity.setHearingId(hearingFinancialResultRequest.getHearingId());
        defendantGobAccountsEntity.setUpdatedTime(ZonedDateTime.now());
        return defendantGobAccountsEntity;
    }

    private JsonArray convertCaseReferencesToJsonArray(final java.util.List<String> caseReferences) {
        final JsonArrayBuilder arrayBuilder = createArrayBuilder();
        if (caseReferences != null) {
            caseReferences.forEach(arrayBuilder::add);
        }
        return arrayBuilder.build();
    }
}
