package uk.gov.moj.cpp.results.event;

import static java.util.Objects.nonNull;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.results.courts.DefendantTrackingStatusUpdated;
import uk.gov.justice.results.courts.TrackingStatus;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.results.persist.DefendantTrackingStatusRepository;
import uk.gov.moj.cpp.results.persist.entity.DefendantTrackingStatus;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(EVENT_LISTENER)
public class DefendantTrackingStatusEventListener {

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private DefendantTrackingStatusRepository defendantTrackingStatusRepository;

    @Handles("results.events.defendant-tracking-status-updated")
    public void saveDefendantTrackingStatus(final JsonEnvelope event) {

        final JsonObject defendantTrackingStatusJson = event.payloadAsJsonObject();

        final DefendantTrackingStatusUpdated defendantTrackingStatusUpdated = jsonObjectToObjectConverter.convert(defendantTrackingStatusJson, DefendantTrackingStatusUpdated.class);
        for (final TrackingStatus trackingStatus : defendantTrackingStatusUpdated.getTrackingStatus()) {

            final DefendantTrackingStatus defendantTrackingStatus = defendantTrackingStatusRepository.findBy(trackingStatus.getOffenceId());
            if (nonNull(defendantTrackingStatus)) {
                setDefendantTrackingStatus(defendantTrackingStatusUpdated.getDefendantId(), trackingStatus, defendantTrackingStatus);
            } else {
                setDefendantTrackingStatus(defendantTrackingStatusUpdated.getDefendantId(), trackingStatus, new DefendantTrackingStatus());
            }
        }
    }

    private void setDefendantTrackingStatus(final UUID defendantId,
                                            final TrackingStatus trackingStatus,
                                            final DefendantTrackingStatus defendantTrackingStatus) {

        defendantTrackingStatus.setDefendantId(defendantId);
        defendantTrackingStatus.setOffenceId(trackingStatus.getOffenceId());

        if (nonNull(trackingStatus.getEmLastModifiedTime())) {
            defendantTrackingStatus.setEmLastModifiedTime(trackingStatus.getEmLastModifiedTime());
        }

        if (nonNull(trackingStatus.getEmStatus())) {
            defendantTrackingStatus.setEmStatus(trackingStatus.getEmStatus());
        }

        if (nonNull(trackingStatus.getWoaLastModifiedTime())) {
            defendantTrackingStatus.setWoaLastModifiedTime(trackingStatus.getWoaLastModifiedTime());
        }

        if (nonNull(trackingStatus.getWoaStatus())) {
            defendantTrackingStatus.setWoaStatus(trackingStatus.getWoaStatus());
        }

        defendantTrackingStatusRepository.save(defendantTrackingStatus);
    }
}
