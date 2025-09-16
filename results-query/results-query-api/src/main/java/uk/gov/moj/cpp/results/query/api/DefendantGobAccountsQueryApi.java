package uk.gov.moj.cpp.results.query.api;

import static uk.gov.justice.services.core.annotation.Component.QUERY_API;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.results.query.view.DefendantGobAccountsQueryView;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(QUERY_API)
public class DefendantGobAccountsQueryApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefendantGobAccountsQueryApi.class);

    @Inject
    private DefendantGobAccountsQueryView defendantGobAccountsQueryView;

    @Handles("results.query.defendant-gob-account")
    public JsonEnvelope getDefendantGobAccounts(final JsonEnvelope envelope) {
        LOGGER.info("Received getDefendantGobAccounts api {}", envelope.toObfuscatedDebugString());
        return defendantGobAccountsQueryView.getDefendantGobAccounts(envelope);
    }
}
