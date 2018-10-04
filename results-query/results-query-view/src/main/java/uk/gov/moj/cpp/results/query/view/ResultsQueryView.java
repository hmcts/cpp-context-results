package uk.gov.moj.cpp.results.query.view;

import static java.util.UUID.fromString;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.QUERY_VIEW;
import static uk.gov.moj.cpp.domains.results.result.ResultLevel.CASE;
import static uk.gov.moj.cpp.domains.results.result.ResultLevel.DEFENDANT;
import static uk.gov.moj.cpp.domains.results.result.ResultLevel.OFFENCE;
import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.domains.results.result.ResultLevel;
import uk.gov.moj.cpp.results.domain.event.HearingResultsAdded;
import uk.gov.moj.cpp.results.persist.DefendantRepository;
import uk.gov.moj.cpp.results.persist.HearingRepository;
import uk.gov.moj.cpp.results.persist.HearingResultRepository;
import uk.gov.moj.cpp.results.persist.VariantDirectoryRepository;
import uk.gov.moj.cpp.results.persist.entity.HearingResult;
import uk.gov.moj.cpp.results.query.view.response.HearingResultSummariesView;
import uk.gov.moj.cpp.results.query.view.service.HearingService;
import uk.gov.moj.cpp.results.query.view.service.UserGroupsService;

import javax.inject.Inject;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@SuppressWarnings({"CdiInjectionPointsInspection", "SpringAutowiredFieldsWarningInspection", "WeakerAccess", "squid:S1188"})
@ServiceComponent(QUERY_VIEW)
public class ResultsQueryView {

    private static final String FIELD_VERDICT = "verdict";
    private static final String RESPONSE_NAME_HEARING_DETAILS = "results.hearing-details";
    private static final String RESPONSE_NAME_RESULTS_SUMMARY = "results.results-summary";
    private static final String RESPONSE_NAME_RESULTS_DETAILS = "results.results-details";

    private static final String FIELD_GENERIC_ID = "id";
    private static final String FIELD_PERSON_ID = "personId";
    private static final String FIELD_DEFENDANT_ID = "defendantId";

    private static final String FIELD_HEARING_ID = "hearingId";
    private static final String FIELD_FROM_DATE = "fromDate";

    private static final String FIELD_DEFENDANT_LEVEL_RESULTS = "defendantLevelResults";
    private static final String FIELD_CASE_LEVEL_RESULTS = "caseLevelResults";
    private static final String FIELD_OFFENCE_LEVEL_RESULTS = "offenceLevelResults";
    private static final String FIELD_LABEL = "label";
    private static final String FIELD_URN = "urn";
    private static final String FIELD_CASES = "cases";
    private static final String FIELD_VALUE = "value";
    private static final String FIELD_PROMPTS = "prompts";
    private static final String FIELD_OFFENCES = "offences";
    private static final String FIELD_OFFENCE_WORDING = "offenceWording";
    private static final String FIELD_PLEA = "plea";
    private static final String FIELD_PLEA_VALUE = "pleaValue";
    private static final String FIELD_PLEA_DATE = "pleaDate";
    private static final String FIELD_START_DATE = "startDate";
    private static final String FIELD_END_DATE = "endDate";
    private static final String FIELD_COURT_ROOM = "courtRoom";
    private static final String FIELD_ORDERED_DATE = "orderedDate";
    private static final String FIELD_LAST_SHARED_DATE = "lastSharedDate";
    private static final String FIELD_COURT = "court";
    private static final String FIELD_CONVICTION_DATE = "convictionDate";
    private static final String FIELD_VERDICT_DATE = "verdictDate";
    private static final String FIELD_VERDICT_CATEGORY = "verdictCategory";
    private static final String FIELD_VERDICT_DESCRIPTION = "verdictDescription";

    @Inject
    private Enveloper enveloper;

    @Inject
    private DefendantRepository defendantRepository;

    @Inject
    private HearingRepository hearingRepository;

    @Inject
    private HearingResultRepository hearingResultRepository;

    @Inject
    private VariantDirectoryRepository variantDirectoryRepository;

    @Inject
    private HearingService hearingService;

    @Inject
    private UserGroupsService userGroupsService;

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

    @Handles("results.get-results-details")
    public JsonEnvelope getResultsDetails(final JsonEnvelope query) {
        final JsonObject payload = query.payloadAsJsonObject();
        final UUID hearingId = fromString(payload.getString(FIELD_HEARING_ID));
        final UUID personId = fromString(payload.getString(FIELD_PERSON_ID));

        final List<HearingResult> hearingResults = hearingResultRepository.findByHearingIdAndPersonId(hearingId, personId);

        if (hearingResults.isEmpty()) {
            return enveloper.withMetadataFrom(query, RESPONSE_NAME_RESULTS_DETAILS)
                    .apply(null);
        }

        final JsonArrayBuilder defendantLevelResultsBuilder = extractResultsAtLevel(hearingResults, DEFENDANT);

        final Map<UUID, List<HearingResult>> resultsByCaseId = hearingResults.stream()
                .collect(Collectors.groupingBy(HearingResult::getCaseId));

        final JsonArrayBuilder caseLevelResultBuilder = constructCaseView(resultsByCaseId);

        return enveloper.withMetadataFrom(query, RESPONSE_NAME_RESULTS_DETAILS)
                .apply(createObjectBuilder()
                        .add(FIELD_PERSON_ID, personId.toString())
                        .add(FIELD_HEARING_ID, hearingId.toString())
                        .add(FIELD_DEFENDANT_LEVEL_RESULTS, defendantLevelResultsBuilder)
                        .add(FIELD_CASES, caseLevelResultBuilder)
                        .build());
    }

    private JsonArrayBuilder constructCaseView(final Map<UUID, List<HearingResult>> resultsByCaseId) {
        final JsonArrayBuilder casesObjectBuilder = createArrayBuilder();

        resultsByCaseId.keySet().forEach(caseId -> {
                    final Map resultsByOffenceId = resultsByCaseId.get(caseId).stream()
                            .filter(r -> r.getOffenceId() != null)
                            .collect(Collectors.groupingBy(HearingResult::getOffenceId));

                    final JsonArrayBuilder offenceLevelResultBuilder = constructOffenceView(resultsByOffenceId);

                    final HearingResult firstResultWithCaseId = resultsByCaseId.get(caseId).get(0);
                    casesObjectBuilder.add(
                            createObjectBuilder()
                                    .add(FIELD_GENERIC_ID, caseId.toString())
                                    .add(FIELD_URN, firstResultWithCaseId.getUrn())
                                    .add(FIELD_CASE_LEVEL_RESULTS, extractResultsAtLevel(resultsByCaseId.get(caseId), CASE))
                                    .add(FIELD_OFFENCES, offenceLevelResultBuilder)
                    );
                }
        );

        return casesObjectBuilder;
    }

    private JsonArrayBuilder constructOffenceView(final Map<UUID, List<HearingResult>> resultsByOffenceId) {
        final JsonArrayBuilder offencesArrayBuilder = createArrayBuilder();


        resultsByOffenceId.keySet().forEach(offenceId -> {
            final List<HearingResult> hearingResultsForOffenceId = resultsByOffenceId.get(offenceId);

            final JsonObjectBuilder offenceBuilder = createObjectBuilder()
                    .add(FIELD_GENERIC_ID, offenceId.toString())
                    .add(FIELD_OFFENCE_LEVEL_RESULTS, extractResultsAtLevel(hearingResultsForOffenceId, OFFENCE));

            final Optional firstNonNullPlea = firstNonNullValueOf(hearingResultsForOffenceId, HearingResult::getPleaValue);
            if (firstNonNullPlea.isPresent()) {
                final Optional firstNonNullPleaDate = firstNonNullValueOf(hearingResultsForOffenceId, HearingResult::getPleaDate);

                final JsonObjectBuilder plea =
                        createObjectBuilder().add(FIELD_PLEA_VALUE, firstNonNullPlea.get().toString())
                                .add(FIELD_PLEA_DATE, firstNonNullPleaDate.get().toString());

                offenceBuilder.add(FIELD_PLEA, plea);
            }

            final Optional firstNonnullTitle = firstNonNullValueOf(hearingResultsForOffenceId, HearingResult::getOffenceTitle);
            if (firstNonnullTitle.isPresent()) {
                offenceBuilder.add(FIELD_OFFENCE_WORDING, firstNonnullTitle.get().toString());
            }

            final Optional firstNonnullEndDate = firstNonNullValueOf(hearingResultsForOffenceId, HearingResult::getEndDate);
            if (firstNonnullEndDate.isPresent()) {
                offenceBuilder.add(FIELD_END_DATE, firstNonnullEndDate.get().toString());
            }

            final Optional firstNonnullStartDate = firstNonNullValueOf(hearingResultsForOffenceId, HearingResult::getStartDate);
            if (firstNonnullStartDate.isPresent()) {
                offenceBuilder.add(FIELD_START_DATE, firstNonnullStartDate.get().toString());
            }
            final Optional firstNonNullConvictionDate = firstNonNullValueOf(
                    hearingResultsForOffenceId, HearingResult::getConvictionDate);
            if (firstNonNullConvictionDate.isPresent()) {
                offenceBuilder.add(FIELD_CONVICTION_DATE,
                        firstNonNullConvictionDate.get().toString());
            }
            final Optional firstNonNullVerdictCategory = firstNonNullValueOf(
                    hearingResultsForOffenceId, HearingResult::getVerdictCategory);
            if (firstNonNullVerdictCategory.isPresent()) {
                final Optional firstNonNullVerdictDate = firstNonNullValueOf(
                        hearingResultsForOffenceId, HearingResult::getVerdictDate);
                final Optional firstNonNullVerdictDescription = firstNonNullValueOf(
                        hearingResultsForOffenceId, HearingResult::getVerdictDescription);
                final JsonObjectBuilder verdict = createObjectBuilder()
                        .add(FIELD_VERDICT_CATEGORY,
                                firstNonNullVerdictCategory.get().toString())
                        .add(FIELD_VERDICT_DATE, firstNonNullVerdictDate.get().toString())
                        .add(FIELD_VERDICT_DESCRIPTION,
                                firstNonNullVerdictDescription.get().toString());
                offenceBuilder.add(FIELD_VERDICT, verdict);
            }
            offencesArrayBuilder.add(offenceBuilder);
        });

        return offencesArrayBuilder;
    }

    private Optional<Object> firstNonNullValueOf(final List<HearingResult> hearingResults, final Function<HearingResult, Object> extraction) {
        return hearingResults.stream().map(extraction).filter(Objects::nonNull).findAny();
    }

    private JsonArrayBuilder extractResultsAtLevel(final List<HearingResult> hearingResults, final ResultLevel resultLevel) {
        final JsonArrayBuilder resultsBuilder = createArrayBuilder();

        hearingResults.stream()
                .filter(result -> result.getResultLevel().equals(resultLevel))
                .forEach(result -> {
                    final JsonArrayBuilder promptsBuilder = createArrayBuilder();

                    result.getResultPrompts().forEach(
                            prompt -> promptsBuilder.add(
                                    createObjectBuilder()
                                            .add(FIELD_GENERIC_ID, prompt.getId().toString())
                                            .add(FIELD_LABEL, prompt.getLabel())
                                            .add(FIELD_VALUE, prompt.getValue())
                            )
                    );

                    resultsBuilder.add(createResultLevelBuilder(result, promptsBuilder));
                });

        return resultsBuilder;
    }

    private JsonObjectBuilder createResultLevelBuilder(HearingResult result,
                                                       final JsonArrayBuilder promptsBuilder) {
        final JsonObjectBuilder resultLevelBuilder = createObjectBuilder()
                .add(FIELD_LABEL, result.getResultLabel())
                .add(FIELD_PROMPTS, promptsBuilder)
                .add(FIELD_COURT, result.getCourt())
                .add(FIELD_COURT_ROOM, result.getCourtRoom())
                .add(FIELD_LAST_SHARED_DATE, result
                        .getLastSharedDateTime().toLocalDate().toString());
        if (Objects.nonNull(result.getOrderedDate())) {
            resultLevelBuilder.add(FIELD_ORDERED_DATE,
                    result.getOrderedDate().toString());
        }
        return resultLevelBuilder;
    }

    @Handles("results.get-results-summary")
    public JsonEnvelope getResultsSummary(final JsonEnvelope query) {
        final JsonObject payload = query.payloadAsJsonObject();
        final LocalDate fromDate = LocalDates.from(payload.getString(FIELD_FROM_DATE));
        final HearingResultSummariesView view = hearingService.findHearingResultSummariesFromDate(fromDate);
        return enveloper.withMetadataFrom(query, RESPONSE_NAME_RESULTS_SUMMARY).apply(view);
    }

}
