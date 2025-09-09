package uk.gov.moj.cpp.results.event;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.core.courts.HearingFinancialResultsUpdated;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.results.persist.DefendantGobAccountsEntity;
import uk.gov.moj.cpp.results.persist.DefendantGobAccountsRepository;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_LISTENER)
public class HearingFinancialResultsUpdatedListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingFinancialResultsUpdatedListener.class);
    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private DefendantGobAccountsRepository defendantGobAccountsRepository;

    @Handles("results.event.hearing-financial-results-updated")
    public void handleDefendantGobAccounts(final JsonEnvelope event) {
        final JsonObject requestJson = event.payloadAsJsonObject();
        final HearingFinancialResultsUpdated hearingFinancialResultsUpdated = jsonObjectToObjectConverter.convert(requestJson, HearingFinancialResultsUpdated.class);
        saveDefendantGobAccounts(hearingFinancialResultsUpdated);
    }

    private void saveDefendantGobAccounts(HearingFinancialResultsUpdated hearingFinancialResultsUpdated) {
        final UUID masterDefendantId = hearingFinancialResultsUpdated.getMasterDefendantId();
        final UUID correlationId = hearingFinancialResultsUpdated.getCorrelationId();
        defendantGobAccountsRepository.save(createDefendantGobAccountsEntity(hearingFinancialResultsUpdated));
        LOGGER.info("GOB account Details are saved successfully stored for masterDefendantId id & correlationId id : {} & {}", masterDefendantId, correlationId);
    }

    private DefendantGobAccountsEntity createDefendantGobAccountsEntity(HearingFinancialResultsUpdated hearingFinancialResultsUpdated) {
        final DefendantGobAccountsEntity defendantGobAccountsEntity = new DefendantGobAccountsEntity();
        defendantGobAccountsEntity.setId(randomUUID());
        defendantGobAccountsEntity.setMasterDefendantId(hearingFinancialResultsUpdated.getMasterDefendantId());
        defendantGobAccountsEntity.setCorrelationId(hearingFinancialResultsUpdated.getCorrelationId());
        defendantGobAccountsEntity.setAccountNumber(hearingFinancialResultsUpdated.getAccountNumber());
        defendantGobAccountsEntity.setCaseReferences(hearingFinancialResultsUpdated.getCaseReferences().toString());
        defendantGobAccountsEntity.setCreatedDateTime(hearingFinancialResultsUpdated.getCreatedDateTime());
        return defendantGobAccountsEntity;
    }
}
