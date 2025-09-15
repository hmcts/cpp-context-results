package uk.gov.moj.cpp.results.query.view;

import static uk.gov.justice.services.core.annotation.Component.QUERY_VIEW;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
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

@ServiceComponent(QUERY_VIEW)
public class DefendantGobAccountsQueryView {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefendantGobAccountsQueryView.class);
    @Inject
    private DefendantGobAccountsRepository defendantGobAccountsRepository;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Handles("results.query.defendant-gob-account")
    public JsonEnvelope getDefendantGobAccounts(final JsonEnvelope envelope) {
        LOGGER.info("Received getDefendantGobAccounts view {}", envelope.toObfuscatedDebugString());
        final UUID masterDefendantId = UUID.fromString(envelope.payloadAsJsonObject().getString("masterDefendantId"));
        final UUID hearingId = UUID.fromString(envelope.payloadAsJsonObject().getString("hearingId"));

        final DefendantGobAccountsEntity defendantGobAccountsEntity = defendantGobAccountsRepository.findAccountNumberByMasterDefendantIdAndHearingId(masterDefendantId, hearingId);

        if (defendantGobAccountsEntity == null) {
            LOGGER.warn("No defendant GOB accounts found for masterDefendantId: {} and hearingId: {}", masterDefendantId, hearingId);
            return envelopeFrom(envelope.metadata(), null);
        }

        final JsonObject jsonObject = objectToJsonObjectConverter.convert(defendantGobAccountsEntity);
        return envelopeFrom(envelope.metadata(), jsonObject);
    }
}
