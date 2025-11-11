package uk.gov.moj.cpp.results.query.api;

import static uk.gov.justice.services.core.annotation.Component.QUERY_API;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.results.query.view.NcesEmailNotificationQueryView;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(QUERY_API)
public class NcesEmailNotificationQueryApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(NcesEmailNotificationQueryApi.class);

    @Inject
    private NcesEmailNotificationQueryView ncesEmailNotificationQueryView;

    @Handles("results.query.nces-email-notification-details")
    public JsonEnvelope getNcesEmailNotificationDetails(final JsonEnvelope envelope) {
        LOGGER.info("Received getNcesEmailNotificationDetails api {}", envelope.toObfuscatedDebugString());
        return ncesEmailNotificationQueryView.getNcesEmailNotificationDetails(envelope);
    }
}
