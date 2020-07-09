package uk.gov.moj.cpp.results.query.view;

import static java.util.UUID.fromString;
import static uk.gov.justice.services.core.annotation.Component.QUERY_VIEW;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.HearingResultsAdded;
import uk.gov.justice.core.courts.external.ApiHearing;
import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.domains.HearingTransformer;
import uk.gov.moj.cpp.results.query.view.response.HearingResultSummariesView;
import uk.gov.moj.cpp.results.query.view.service.HearingService;

import java.time.LocalDate;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;


@SuppressWarnings({"CdiInjectionPointsInspection", "SpringAutowiredFieldsWarningInspection", "WeakerAccess", "squid:S1188", "squid:CallToDeprecatedMethod"})
@ServiceComponent(QUERY_VIEW)
public class ResultsQueryView {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResultsQueryView.class);

    private static final String RESPONSE_NAME_HEARING_DETAILS = "results.hearing-details";
    private static final String RESPONSE_NAME_RESULTS_SUMMARY = "results.results-summary";
    private static final String RESPONSE_NAME_HEARING_INFORMATION_DETAILS = "results.hearing-information-details";
    private static final String FIELD_DEFENDANT_ID = "defendantId";
    private static final String FIELD_HEARING_ID = "hearingId";
    private static final String FIELD_FROM_DATE = "fromDate";


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

        return enveloper.withMetadataFrom(query, RESPONSE_NAME_HEARING_DETAILS)
                .apply(jsonResult);

    }

    @Handles("results.get-results-summary")
    public JsonEnvelope getResultsSummary(final JsonEnvelope query) {
        final JsonObject payload = query.payloadAsJsonObject();
        final LocalDate fromDate = LocalDates.from(payload.getString(FIELD_FROM_DATE));
        final HearingResultSummariesView view = hearingService.findHearingResultSummariesFromDate(fromDate);
        return enveloper.withMetadataFrom(query, RESPONSE_NAME_RESULTS_SUMMARY).apply(view);
    }


    @Handles("results.get-hearing-information-details-for-hearing")
    public JsonEnvelope getHearingDetailsForHearingId(final JsonEnvelope query) {
        final JsonObject payload = query.payloadAsJsonObject();
        final UUID hearingId = fromString(payload.getString(FIELD_HEARING_ID));
        final HearingResultsAdded hearingResultAdded = hearingService.findHearingForHearingId(hearingId);
        if(hearingResultAdded != null){
            final ApiHearing hearing = hearingTransformer.hearing(hearingResultAdded.getHearing()).build();
            final JsonObject jsonValue = objectToJsonObjectConverter.convert(hearing);
            final JsonObject jsonResult = Json.createObjectBuilder()
                    .add("hearing", jsonValue)
                    .add("sharedTime", hearingResultAdded.getSharedTime().toString())
                    .build();
            return enveloper.withMetadataFrom(query, RESPONSE_NAME_HEARING_INFORMATION_DETAILS)
                    .apply(jsonResult);
        }
        LOGGER.warn("No records exists for Hearing id {}", hearingId);
        return enveloper.withMetadataFrom(query, RESPONSE_NAME_HEARING_INFORMATION_DETAILS)
                .apply(Json.createObjectBuilder().build());
    }

    @Handles("results.get-hearing-details-internal")
    public JsonEnvelope getHearingDetailsInternal(final JsonEnvelope query) {
        final JsonObject payload = query.payloadAsJsonObject();
        final UUID hearingId = fromString(payload.getString(FIELD_HEARING_ID));
        final HearingResultsAdded hearingResultAdded = hearingService.findHearingForHearingId(hearingId);
        final JsonObject jsonResult = objectToJsonObjectConverter.convert(hearingResultAdded);
        return enveloper.withMetadataFrom(query, RESPONSE_NAME_HEARING_INFORMATION_DETAILS)
                .apply(jsonResult);

    }
}
