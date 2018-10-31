package uk.gov.moj.cpp.results.query.view;

import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.results.domain.event.HearingResultsAdded;
import uk.gov.moj.cpp.results.query.view.response.HearingResultSummariesView;
import uk.gov.moj.cpp.results.query.view.service.HearingService;

import javax.inject.Inject;
import javax.json.JsonObject;
import java.time.LocalDate;
import java.util.UUID;

import static java.util.UUID.fromString;
import static uk.gov.justice.services.core.annotation.Component.QUERY_VIEW;


@SuppressWarnings({"CdiInjectionPointsInspection", "SpringAutowiredFieldsWarningInspection", "WeakerAccess", "squid:S1188"})
@ServiceComponent(QUERY_VIEW)
public class ResultsQueryView {

    private static final String RESPONSE_NAME_HEARING_DETAILS = "results.hearing-details";
    private static final String RESPONSE_NAME_RESULTS_SUMMARY = "results.results-summary";
    private static final String FIELD_DEFENDANT_ID = "defendantId";
    private static final String FIELD_HEARING_ID = "hearingId";
    private static final String FIELD_FROM_DATE = "fromDate";


    @Inject
    private Enveloper enveloper;

    @Inject
    private HearingService hearingService;

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

}
