package uk.gov.moj.cpp.results.event;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.core.courts.HearingFinancialResultsUpdated;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.results.persist.DefendantGobAccountsEntity;
import uk.gov.moj.cpp.results.persist.DefendantGobAccountsRepository;

import java.time.ZonedDateTime;
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
        final UUID masterDefendantId = hearingFinancialResultsUpdated.getMasterDefendantId();
        final UUID correlationId = hearingFinancialResultsUpdated.getCorrelationId();
        final UUID hearingId = hearingFinancialResultsUpdated.getHearingId();

        // Find existing record to update with account_number
        final DefendantGobAccountsEntity existingEntity = defendantGobAccountsRepository.findAccountNumberByMasterDefendantIdAndHearingIdAndCorrelationId(masterDefendantId, hearingId, correlationId);

        if (existingEntity != null) {
            // Update existing record with account_number and account_request_time
            existingEntity.setAccountNumber(hearingFinancialResultsUpdated.getAccountNumber());
            existingEntity.setAccountRequestTime(hearingFinancialResultsUpdated.getAccountRequestTime());
            existingEntity.setUpdateTime(ZonedDateTime.now());
            defendantGobAccountsRepository.save(existingEntity);
            LOGGER.info("GOB account Details updated with account_number for masterDefendantId id & correlationId id & hearingId : {} & {} & {}", masterDefendantId, correlationId, hearingId);
        }
    }
}
