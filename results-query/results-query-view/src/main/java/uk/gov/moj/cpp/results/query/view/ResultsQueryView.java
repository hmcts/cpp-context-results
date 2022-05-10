package uk.gov.moj.cpp.results.query.view;

import static java.lang.Boolean.TRUE;
import static java.util.Arrays.stream;
import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.QUERY_VIEW;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;

import uk.gov.justice.core.courts.HearingResultsAdded;
import uk.gov.justice.core.courts.external.ApiHearing;
import uk.gov.justice.results.courts.TrackingStatus;
import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.domains.HearingTransformer;
import uk.gov.moj.cpp.results.persist.entity.DefendantTrackingStatus;
import uk.gov.moj.cpp.results.query.view.response.HearingResultSummariesView;
import uk.gov.moj.cpp.results.query.view.service.DefendantTrackingStatusService;
import uk.gov.moj.cpp.results.query.view.service.HearingService;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@SuppressWarnings({"CdiInjectionPointsInspection", "SpringAutowiredFieldsWarningInspection", "WeakerAccess", "squid:S1188", "squid:CallToDeprecatedMethod"})
@ServiceComponent(QUERY_VIEW)
public class ResultsQueryView {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResultsQueryView.class);

    private static final String RESPONSE_NAME_HEARING_DETAILS = "results.hearing-details";
    private static final String RESPONSE_NAME_RESULTS_SUMMARY = "results.results-summary";
    private static final String RESPONSE_NAME_HEARING_INFORMATION_DETAILS = "results.hearing-information-details";
    private static final String RESPONSE_NAME_DEFENDANTS_TRACKING_STATUS = "results.get-defendants-tracking-status";
    private static final String FIELD_DEFENDANT_ID = "defendantId";
    private static final String FIELD_DEFENDANT_IDS = "defendantIds";
    private static final String FIELD_HEARING_ID = "hearingId";
    private static final String FIELD_FROM_DATE = "fromDate";
    private static final String DEFENDANTS_FIELD = "defendants";

    private static final String FIELD_TRACKING_STATUS = "trackingStatus";
    private static final String FIELD_OFFENCE_ID = "offenceId";
    private static final String FIELD_EM_STATUS = "emStatus";
    private static final String FIELD_EM_LAST_MODIFIED_TIME = "emLastModifiedTime";

    private static final String FIELD_WOA_STATUS = "woaStatus";
    private static final String FIELD_WOA_LAST_MODIFIED_TIME = "woaLastModifiedTime";

    @Inject
    private DefendantTrackingStatusService defendantTrackingStatusService;


    @Inject
    private Enveloper enveloper;

    @Inject
    private HearingService hearingService;

    @Inject
    private HearingTransformer hearingTransformer;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;


    @Handles("results.get-hearing-details")
    public JsonEnvelope getHearingDetails(final JsonEnvelope query) {
        final JsonObject payload = query.payloadAsJsonObject();
        final UUID hearingId = fromString(payload.getString(FIELD_HEARING_ID));
        final UUID defendantId = fromString(payload.getString(FIELD_DEFENDANT_ID));
        final HearingResultsAdded hearingResultAdded = hearingService.findHearingDetailsByHearingIdDefendantId(hearingId, defendantId);
        final JsonObject jsonResult = objectToJsonObjectConverter.convert(hearingResultAdded);

        return envelopeFrom(metadataFrom(query.metadata()).withName(RESPONSE_NAME_HEARING_DETAILS).build(), jsonResult);

    }

    @Handles("results.get-results-summary")
    public JsonEnvelope getResultsSummary(final JsonEnvelope query) {
        final JsonObject payload = query.payloadAsJsonObject();
        final LocalDate fromDate = LocalDates.from(payload.getString(FIELD_FROM_DATE));
        final HearingResultSummariesView view = hearingService.findHearingResultSummariesFromDate(fromDate);
        final JsonObject jsonResult = objectToJsonObjectConverter.convert(view);
        return envelopeFrom(metadataFrom(query.metadata()).withName(RESPONSE_NAME_RESULTS_SUMMARY).build(), jsonResult);
    }


    @Handles("results.get-hearing-information-details-for-hearing")
    public JsonEnvelope getHearingDetailsForHearingId(final JsonEnvelope query) {
        final JsonObject payload = query.payloadAsJsonObject();
        final UUID hearingId = fromString(payload.getString(FIELD_HEARING_ID));
        final HearingResultsAdded hearingResultAdded = hearingService.findHearingForHearingId(hearingId);
        if (hearingResultAdded != null) {
            final ApiHearing hearing = hearingTransformer.hearing(hearingResultAdded.getHearing()).build();
            final JsonObject jsonValue = objectToJsonObjectConverter.convert(hearing);
            final JsonObject jsonResult = createObjectBuilder()
                    .add("hearing", jsonValue)
                    .add("sharedTime", hearingResultAdded.getSharedTime().toString())
                    .build();
            return envelopeFrom(metadataFrom(query.metadata()).withName(RESPONSE_NAME_HEARING_INFORMATION_DETAILS).build(), jsonResult);
        }
        LOGGER.warn("No records exists for Hearing id {}", hearingId);
        return envelopeFrom(metadataFrom(query.metadata()).withName(RESPONSE_NAME_HEARING_INFORMATION_DETAILS).build(), createObjectBuilder().build());
    }

    @Handles("results.get-hearing-details-internal")
    public JsonEnvelope getHearingDetailsInternal(final JsonEnvelope query) {
        final JsonObject payload = query.payloadAsJsonObject();
        final UUID hearingId = fromString(payload.getString(FIELD_HEARING_ID));
        final HearingResultsAdded hearingResultAdded = hearingService.findHearingForHearingId(hearingId);
        if (nonNull(hearingResultAdded)) {
            final JsonObject jsonResult = objectToJsonObjectConverter.convert(hearingResultAdded);
            return envelopeFrom(metadataFrom(query.metadata()).withName(RESPONSE_NAME_HEARING_INFORMATION_DETAILS).build(), jsonResult);
        }
        LOGGER.warn("No record exists for Hearing id {}", hearingId);

        return envelopeFrom(metadataFrom(query.metadata()).withName(RESPONSE_NAME_HEARING_INFORMATION_DETAILS).build(),
                createObjectBuilder().build());
    }

    public JsonEnvelope getDefendantsTrackingStatus(final JsonEnvelope envelope) {
        final String strDefendantIds = envelope.payloadAsJsonObject().getString(FIELD_DEFENDANT_IDS);

        final List<UUID> defendantIds = stream(strDefendantIds.trim().split(",")).map(UUID::fromString).collect(toList());

        final List<DefendantTrackingStatus> defendantTrackingStatusEntities = defendantTrackingStatusService.findDefendantTrackingStatus(defendantIds);

        return envelopeFrom(metadataFrom(envelope.metadata()).withName(RESPONSE_NAME_DEFENDANTS_TRACKING_STATUS).build(),
                buildDefendantTrackingResponsePayload(defendantTrackingStatusEntities));
    }

    private JsonObject buildDefendantTrackingResponsePayload(final List<DefendantTrackingStatus> defendantTrackingStatusEntities) {

        final Map<UUID, Set<TrackingStatus>> defendantTrackingStatusResultMap = new LinkedHashMap<>();

        for (final DefendantTrackingStatus defendantTrackingStatusEntity : defendantTrackingStatusEntities) {

            final UUID defendantId = defendantTrackingStatusEntity.getDefendantId();
            final Set<TrackingStatus> existingTrackingStatuses = defendantTrackingStatusResultMap.get(defendantId);

            if (nonNull(existingTrackingStatuses)) {

                existingTrackingStatuses.add(trackingStatusFrom(defendantTrackingStatusEntity));
                defendantTrackingStatusResultMap.put(defendantId, existingTrackingStatuses);

            } else {

                final Set<TrackingStatus> trackingStatusesList = new HashSet<>();
                trackingStatusesList.add(trackingStatusFrom(defendantTrackingStatusEntity));
                defendantTrackingStatusResultMap.put(defendantId, trackingStatusesList);

            }
        }
        return buildDefendantTrackingStatusJson(defendantTrackingStatusResultMap);
    }

    private JsonObject buildDefendantTrackingStatusJson(final Map<UUID, Set<TrackingStatus>> defendantTrackingStatusResultMap) {
        final JsonArrayBuilder defendantsArrayBuilder = createArrayBuilder();

        for (final Map.Entry<UUID, Set<TrackingStatus>> entry : defendantTrackingStatusResultMap.entrySet()) {
            final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();
            final Set<TrackingStatus> trackingStatuses = entry.getValue();
            final JsonArrayBuilder jsonTrackingStatusArrayBuilder = createArrayBuilder();

            for (final TrackingStatus trackingStatus : trackingStatuses) {
                jsonTrackingStatusArrayBuilder.add(
                        trackingStatusJson(trackingStatus));
            }

            jsonObjectBuilder.add(FIELD_DEFENDANT_ID, entry.getKey().toString());
            jsonObjectBuilder.add(FIELD_TRACKING_STATUS, jsonTrackingStatusArrayBuilder.build());
            defendantsArrayBuilder.add(jsonObjectBuilder.build());
        }
        return createObjectBuilder().add(DEFENDANTS_FIELD, defendantsArrayBuilder.build()).build();
    }

    @SuppressWarnings("squid:S2259")
    private JsonObject trackingStatusJson(final TrackingStatus trackingStatus) {

        final JsonObjectBuilder trackingStatusObjectBuilder = createObjectBuilder();

        trackingStatusObjectBuilder.add(FIELD_OFFENCE_ID, trackingStatus.getOffenceId().toString());

        addLastModifiedTimeIfNotNull(trackingStatusObjectBuilder, trackingStatus.getEmLastModifiedTime(), FIELD_EM_LAST_MODIFIED_TIME);

        addLastModifiedTimeIfNotNull(trackingStatusObjectBuilder, trackingStatus.getWoaLastModifiedTime(), FIELD_WOA_LAST_MODIFIED_TIME);

        trackingStatusObjectBuilder.add(FIELD_EM_STATUS, TRUE.equals(trackingStatus.getEmStatus()));

        trackingStatusObjectBuilder.add(FIELD_WOA_STATUS, TRUE.equals(trackingStatus.getWoaStatus()));

        return trackingStatusObjectBuilder.build();
    }


    private TrackingStatus trackingStatusFrom(final DefendantTrackingStatus defendantTrackingStatusEntity) {

        final ZonedDateTime emLastModifiedTime = defendantTrackingStatusEntity.getEmLastModifiedTime();
        final ZonedDateTime emZonedDateTime = nonNull(emLastModifiedTime) ? ZonedDateTimes.fromString(emLastModifiedTime.toString()) : null;

        final ZonedDateTime woaLastModifiedTime = defendantTrackingStatusEntity.getWoaLastModifiedTime();
        final ZonedDateTime woaZonedDateTime = nonNull(woaLastModifiedTime) ? ZonedDateTimes.fromString(woaLastModifiedTime.toString()) : null;

        return new TrackingStatus(emZonedDateTime, defendantTrackingStatusEntity.getEmStatus(), defendantTrackingStatusEntity.getOffenceId(),
                woaZonedDateTime, defendantTrackingStatusEntity.getWoaStatus());
    }

    private void addLastModifiedTimeIfNotNull(final JsonObjectBuilder trackingStatusObjectBuilder,
                                              final ZonedDateTime lastModifiedTime,
                                              final String fieldLastModifiedTime) {
        if (nonNull(lastModifiedTime)) {
            trackingStatusObjectBuilder.add(fieldLastModifiedTime, ZonedDateTimes.fromString(lastModifiedTime.toString()).toString());
        }
    }
}
