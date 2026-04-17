package uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.applications;

import static java.lang.Boolean.TRUE;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static uk.gov.moj.cpp.results.domain.aggregate.ApplicationNCESEventsHelper.buildApplicationResultsFromTrackRequest;
import static uk.gov.moj.cpp.results.domain.aggregate.ImpositionOffenceDetailsBuilder.buildImpositionOffenceDetailsFromAggregate;
import static uk.gov.moj.cpp.results.domain.aggregate.ImpositionOffenceDetailsBuilder.buildImpositionOffenceDetailsFromRequest;
import static uk.gov.moj.cpp.results.domain.aggregate.MarkedAggregateSendEmailEventBuilder.markedAggregateSendEmailEventBuilder;
import static uk.gov.moj.cpp.results.domain.aggregate.NCESDecisionHelper.buildNewImpositionOffenceDetailsFromRequest;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.APPLICATION_SUBJECT;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.APPLICATION_TYPES;
import static uk.gov.moj.cpp.results.domain.aggregate.utils.OffenceResultsResolver.getNewOffenceResultsApplication;
import static uk.gov.moj.cpp.results.domain.aggregate.utils.OffenceResultsResolver.getOriginalOffenceResultsApplication;
import static uk.gov.moj.cpp.results.domain.aggregate.utils.OffenceResultsResolver.getPreviousOffenceResultsDetails;

import uk.gov.justice.hearing.courts.HearingFinancialResultRequest;
import uk.gov.justice.hearing.courts.OffenceResults;
import uk.gov.justice.hearing.courts.OffenceResultsDetails;
import uk.gov.moj.cpp.results.domain.aggregate.ApplicationNCESEventsHelper;
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.ResultNotificationRule;
import uk.gov.moj.cpp.results.domain.aggregate.utils.CorrelationItem;
import uk.gov.moj.cpp.results.domain.event.ImpositionOffenceDetails;
import uk.gov.moj.cpp.results.domain.event.MarkedAggregateSendEmailWhenAccountReceived;
import uk.gov.moj.cpp.results.domain.event.NewOffenceByResult;
import uk.gov.moj.cpp.results.domain.event.OriginalApplicationResults;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Base class for application result notification rules.
 */
public abstract class AbstractApplicationResultNotificationRule implements ResultNotificationRule {
    private static final Predicate<OffenceResults> isApplicationAmended = o -> nonNull(o.getApplicationType()) && nonNull(o.getAmendmentDate());

    /**
     * Generic method to send application resulted(Accepted/Denied) notification during application hearing including amend and reshare.
     * Note: this method be guarded by specific rules its used from.
     */
    protected Optional<MarkedAggregateSendEmailWhenAccountReceived> applicationResultedNotification(RuleInput input) {
        final HearingFinancialResultRequest request = input.request();
        final List<OffenceResults> offenceResults = request.getOffenceResults();
        final LinkedList<CorrelationItem> correlationItems = input.correlationItemList();
        final String ncesEmail = input.ncesEmail();

        final Optional<OffenceResults> offenceForApplication = offenceResults.stream()
                .filter(offence -> APPLICATION_TYPES.containsKey(offence.getApplicationType()))
                .filter(offence -> APPLICATION_SUBJECT.get(offence.getApplicationType()).containsKey(offence.getResultCode()))
                .findFirst();

        if (offenceForApplication.isPresent()) {
            final OffenceResults offence = offenceForApplication.get();
            final Map<UUID, String> offenceDateMap = input.offenceDateMap();
            final List<OffenceResultsDetails> originalOffenceResults = getOriginalOffenceResultsApplication(
                    input.prevOffenceResultsDetails(),
                    input.prevApplicationOffenceResultsMap(),
                    request.getOffenceResults());

            final List<ImpositionOffenceDetails> impositionOffenceDetailsForApplication = originalOffenceResults.stream()
                    .map(oor -> buildImpositionOffenceDetailsFromAggregate(oor, offenceDateMap))
                    .distinct().toList();

            final List<NewOffenceByResult> newApplicationOffenceResults = getNewOffenceResultsApplication(
                    request.getOffenceResults(),
                    input.prevOffenceResultsDetails(),
                    input.prevApplicationOffenceResultsMap()).stream()
                    .map(nor -> buildNewImpositionOffenceDetailsFromRequest(nor, offenceDateMap))
                    .distinct().toList();

            if (!impositionOffenceDetailsForApplication.isEmpty()) {
                final OriginalApplicationResults originalApplicationResults = buildApplicationResultsFromTrackRequest(offenceResults);
                final String writtenOffExists = input.isWrittenOffExists();
                final String originalDateOfOffenceList = input.originalDateOfOffenceList();
                final String originalDateOfSentenceList = input.originalDateOfSentenceList();
                final String applicationResult = input.applicationResult();

                return Optional.of(
                        markedAggregateSendEmailEventBuilder(ncesEmail, correlationItems)
                                .buildMarkedAggregateWithoutOldsForSpecificCorrelationIdWithEmail(request,
                                        APPLICATION_SUBJECT.get(offence.getApplicationType()).get(offence.getResultCode()),
                                        impositionOffenceDetailsForApplication,
                                        ncesEmail,
                                        writtenOffExists,
                                        originalDateOfOffenceList,
                                        originalDateOfSentenceList,
                                        newApplicationOffenceResults,
                                        applicationResult,
                                        originalApplicationResults,
                                        null,
                                        input.prevApplicationResultsDetails()));
            }
        }
        return Optional.empty();
    }

    protected HearingFinancialResultRequest filteredApplicationResults(HearingFinancialResultRequest request) {
        final HearingFinancialResultRequest filtered = HearingFinancialResultRequest.hearingFinancialResultRequest()
                .withValuesFrom(request)
                .withOffenceResults(new ArrayList<>(request.getOffenceResults())).build();

        filtered.getOffenceResults().removeIf(result -> isNull(result.getApplicationType()));
        return filtered;
    }

    protected List<ImpositionOffenceDetails> getApplicationImpositionOffenceDetails(final HearingFinancialResultRequest hearingFinancialResultRequest,
                                                                                    final Map<UUID, String> offenceDateMap,
                                                                                    final Map<UUID, OffenceResultsDetails> caseOffenceResultsDetails,
                                                                                    final Map<UUID, List<OffenceResultsDetails>> prevApplicationOffenceResultsMap) {

        final UUID currentApplicationId = hearingFinancialResultRequest.getOffenceResults().stream().map(OffenceResults::getApplicationId).filter(Objects::nonNull).findFirst().orElse(null);

        return hearingFinancialResultRequest.getOffenceResults().stream()
                .filter(result -> nonNull(result.getApplicationType()))
                .map(offenceFromRequest -> getPreviousOffenceResultsDetails(offenceFromRequest.getOffenceId(), caseOffenceResultsDetails, prevApplicationOffenceResultsMap, currentApplicationId))
                .filter(Objects::nonNull)
                .filter(OffenceResultsDetails::getIsFinancial)
                .map(offenceResults -> buildImpositionOffenceDetailsFromAggregate(offenceResults, offenceDateMap)).distinct()
                .toList();
    }

    protected Optional<OriginalApplicationResults> getOriginalApplicationResults(final HearingFinancialResultRequest request, final Map<UUID, List<OffenceResultsDetails>> prevAppResultDetails) {
        // Get original application results from the aggregate.
        return request.getOffenceResults().stream()
                .filter(result -> Objects.nonNull(result.getApplicationId()))
                .map(offenceFromRequest -> prevAppResultDetails.get(offenceFromRequest.getApplicationId()))
                .filter(Objects::nonNull)
                .map(ApplicationNCESEventsHelper::buildOriginalApplicationResultsFromAggregate)
                .findFirst();
    }


    /**
     * Determines if there are valid financial amendments that require processing.
     * This method implements proper decision-making logic instead of just checking list presence.
     *
     * @return true if there are valid financial amendments that need processing, false otherwise
     */
    protected boolean isFineToFineApplicationAmendment(final RuleInput input, final HearingFinancialResultRequest request, final UUID currentApplicationId) {
        return request.getOffenceResults().stream()
                .anyMatch(o -> TRUE.equals(o.getIsFinancial()))
                &&
                request.getOffenceResults().stream()
                        .map(offenceFromRequest -> getPreviousOffenceResultsDetails(offenceFromRequest.getOffenceId(),
                                currentApplicationId,
                                input.prevOffenceResultsDetails(),
                                input.prevApplicationOffenceResultsMap(),
                                input.prevApplicationResultsDetails()))
                        .filter(Objects::nonNull)
                        .anyMatch(o -> TRUE.equals(o.getIsFinancial()));
    }

    /**
     * Determines if there are valid financial to non-financial amendments that require processing.
     * This method implements proper decision-making logic instead of just checking list presence.
     *
     * @return true if there are valid financial to non-financial amendments that need processing, false otherwise
     */
    protected boolean isFineToNonFineApplicationAmendment(final RuleInput input, final HearingFinancialResultRequest request, final UUID currentApplicationId) {
        return request.getOffenceResults().stream()
                .filter(isApplicationAmended)
                .filter(o -> !o.getIsFinancial())
                .anyMatch(offenceFromRequest ->
                        ofNullable(getPreviousOffenceResultsDetails(offenceFromRequest.getOffenceId(),
                                currentApplicationId, input.prevOffenceResultsDetails(),
                                input.prevApplicationOffenceResultsMap(),
                                input.prevApplicationResultsDetails()))
                                .map(OffenceResultsDetails::getIsFinancial).orElse(false));
    }

    /**
     * Determines if there are valid non-financial to non-financial amendments that require processing.
     * This method implements proper decision-making logic instead of just checking list presence.
     *
     * @return true if there are valid non-financial to non-financial amendments that need processing, false otherwise
     */
    protected boolean isNonFineToNonFineApplicationAmendment(final RuleInput input, final HearingFinancialResultRequest request, final UUID currentApplicationId) {
        return request.getOffenceResults().stream()
                .filter(isApplicationAmended)
                .filter(o -> !o.getIsFinancial())
                .anyMatch(offenceFromRequest ->
                        ofNullable(getPreviousOffenceResultsDetails(offenceFromRequest.getOffenceId(),
                                currentApplicationId,
                                input.prevOffenceResultsDetails(),
                                input.prevApplicationOffenceResultsMap(),
                                input.prevApplicationResultsDetails()))
                                .map(OffenceResultsDetails::getIsFinancial)
                                .map(isFinancial -> !isFinancial)
                                .orElse(true));
    }

    protected List<ImpositionOffenceDetails> getAppFinancialImpositionOffenceDetails(final RuleInput input, final HearingFinancialResultRequest request) {
        return request.getOffenceResults().stream()
                .filter(this::isValidApplicationOffence)
                .filter(OffenceResults::getIsFinancial)
                .map(offenceResults -> buildImpositionOffenceDetailsFromRequest(offenceResults, input.offenceDateMap())).distinct()
                .toList();
    }

    // Common condition methods for application rules
    protected boolean isValidApplicationOffence(OffenceResults offence) {
        return nonNull(offence.getApplicationType()) &&
                Boolean.TRUE.equals(offence.getIsParentFlag()) &&
                nonNull(offence.getImpositionOffenceDetails());
    }

    protected boolean hasDeemedServedOffences(HearingFinancialResultRequest request) {
        return request.getOffenceResults().stream()
                .anyMatch(o -> isValidApplicationOffence(o) && nonNull(o.getIsDeemedServed()) && o.getIsDeemedServed());
    }

    protected boolean hasACONOffences(HearingFinancialResultRequest request) {
        return request.getOffenceResults().stream()
                .anyMatch(o -> isValidApplicationOffence(o) &&
                        nonNull(o.getImpositionOffenceDetails()) &&
                        o.getImpositionOffenceDetails().contains("ACON") &&
                        o.getIsFinancial());
    }

    protected boolean hasACONAmendmentOffences(HearingFinancialResultRequest request) {
        return request.getOffenceResults().stream()
                .anyMatch(o -> isValidApplicationOffence(o) &&
                        o.getIsFinancial() &&
                        nonNull(o.getImpositionOffenceDetails()) &&
                        o.getImpositionOffenceDetails().contains("ACON"));
    }

    protected boolean hasDeemedServedAmendmentOffences(HearingFinancialResultRequest request) {
        return request.getOffenceResults().stream()
                .anyMatch(o -> isValidApplicationOffence(o) &&
                        nonNull(o.getIsDeemedServed()) && o.getIsDeemedServed());
    }

    protected boolean hasDeemedServedRemovedOffences(HearingFinancialResultRequest request, UUID applicationId, Map<UUID, List<OffenceResultsDetails>> prevApplicationOffenceResultsMap) {
        return request.getOffenceResults().stream()
                .anyMatch(or -> isValidApplicationOffence(or) &&
                        !or.getIsDeemedServed() &&
                        isPrevOffenceResultDeemedServed(or.getOffenceId(), applicationId, prevApplicationOffenceResultsMap) &&
                        Objects.nonNull(or.getAmendmentDate()));
    }

    protected boolean isPrevOffenceResultDeemedServed(final UUID offenceId, final UUID applicationId, final Map<UUID, List<OffenceResultsDetails>> prevApplicationOffenceResultsMap) {
        return prevApplicationOffenceResultsMap.containsKey(applicationId)
                && prevApplicationOffenceResultsMap.get(applicationId).stream()
                .filter(or -> offenceId.equals(or.getOffenceId()))
                .anyMatch(OffenceResultsDetails::getIsDeemedServed);
    }
}
