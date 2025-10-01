package uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.cases;

import static java.lang.Boolean.TRUE;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;

import uk.gov.justice.hearing.courts.HearingFinancialResultRequest;
import uk.gov.justice.hearing.courts.OffenceResults;
import uk.gov.justice.hearing.courts.OffenceResultsDetails;
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.ResultNotificationRule;
import uk.gov.moj.cpp.results.domain.event.ImpositionOffenceDetails;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Base class for case result notification rules.
 */
public abstract class AbstractCaseResultNotificationRule implements ResultNotificationRule {
    private static final Predicate<OffenceResults> isCaseAmended = o -> isNull(o.getApplicationType()) && nonNull(o.getAmendmentDate());
    private static final String ACON = "ACON";

    protected ImpositionOffenceDetails buildImpositionOffenceDetailsFromRequest(final OffenceResults offencesFromRequest, final Map<UUID, String> offenceDateMap) {
        return ImpositionOffenceDetails.impositionOffenceDetails()
                .withDetails(offencesFromRequest.getImpositionOffenceDetails())
                .withOffenceDate(offenceDateMap.get(offencesFromRequest.getOffenceId()))
                .withOffenceId(offencesFromRequest.getOffenceId())
                .withTitle(offencesFromRequest.getOffenceTitle())
                .build();
    }

    protected ImpositionOffenceDetails buildImpositionOffenceDetailsFromAggregate(final OffenceResultsDetails offencesFromAggregate, final Map<UUID, String> offenceDateMap) {
        return ImpositionOffenceDetails.impositionOffenceDetails()
                .withOffenceId(offencesFromAggregate.getOffenceId())
                .withDetails(offencesFromAggregate.getImpositionOffenceDetails())
                .withOffenceDate(offenceDateMap.get(offencesFromAggregate.getOffenceId()))
                .withTitle(offencesFromAggregate.getOffenceTitle())
                .build();
    }

    protected HearingFinancialResultRequest filteredCaseResults(HearingFinancialResultRequest request) {
        final HearingFinancialResultRequest filtered = HearingFinancialResultRequest.hearingFinancialResultRequest()
                .withValuesFrom(request)
                .withOffenceResults(new ArrayList<>(request.getOffenceResults())).build();

        filtered.getOffenceResults().removeIf(result -> nonNull(result.getApplicationType()));
        return filtered;
    }

    protected List<ImpositionOffenceDetails> getImpositionOffenceDetailsFinToNonFin(final HearingFinancialResultRequest request, final Map<UUID, OffenceResultsDetails> prevOffenceResultsDetails, final Map<UUID, String> offenceDateMap) {
        return request.getOffenceResults().stream()
                .filter(isCaseAmended)
                .filter(o -> !o.getIsFinancial())
                .filter(offenceFromRequest -> ofNullable(prevOffenceResultsDetails.get(offenceFromRequest.getOffenceId())).map(OffenceResultsDetails::getIsFinancial).orElse(false))
                .map(offenceResults -> this.buildImpositionOffenceDetailsFromRequest(offenceResults, offenceDateMap))
                .toList();
    }

    protected List<ImpositionOffenceDetails> getImpositionOffenceDetailsFinToFin(final HearingFinancialResultRequest request, final Map<UUID, OffenceResultsDetails> prevOffenceResultsDetails, final Map<UUID, String> offenceDateMap) {
        return request.getOffenceResults().stream()
                .filter(isCaseAmended)
                .filter(OffenceResults::getIsFinancial)
                .filter(offenceFromRequest -> ofNullable(prevOffenceResultsDetails.get(offenceFromRequest.getOffenceId())).map(OffenceResultsDetails::getIsFinancial).orElse(false))
                .map(offenceResults -> this.buildImpositionOffenceDetailsFromRequest(offenceResults, offenceDateMap))
                .toList();
    }

    protected List<ImpositionOffenceDetails> getImpositionOffenceDetailsNonFinToFin(final HearingFinancialResultRequest request, final Map<UUID, OffenceResultsDetails> prevOffenceResultsDetails, final Map<UUID, String> offenceDateMap) {
        return request.getOffenceResults().stream()
                .filter(isCaseAmended)
                .filter(OffenceResults::getIsFinancial)
                .filter(offenceFromRequest -> ofNullable(prevOffenceResultsDetails.get(offenceFromRequest.getOffenceId())).map(ofr -> !TRUE.equals(ofr.getIsFinancial())).orElse(false))
                .map(offenceResults -> this.buildImpositionOffenceDetailsFromRequest(offenceResults, offenceDateMap))
                .toList();
    }

    protected boolean isNonFinToFinImposition(final HearingFinancialResultRequest request, final Map<UUID, OffenceResultsDetails> prevOffenceResultsDetails, final Map<UUID, String> offenceDateMap) {
        return request.getOffenceResults().stream()
                .filter(isCaseAmended)
                .filter(OffenceResults::getIsFinancial)
                .anyMatch(offenceFromRequest -> ofNullable(prevOffenceResultsDetails.get(offenceFromRequest.getOffenceId())).map(ofr -> !TRUE.equals(ofr.getIsFinancial())).orElse(false));
    }

    protected List<ImpositionOffenceDetails> getCaseFinancialImpositionOffenceDetails(final RuleInput input, final HearingFinancialResultRequest request) {
        return request.getOffenceResults().stream()
                .filter(this::isValidCaseOffence)
                .filter(OffenceResults::getIsFinancial)
                .map(offenceResults -> buildImpositionOffenceDetailsFromRequest(offenceResults, input.offenceDateMap())).distinct()
                .toList();
    }

    // Common condition methods for case rules
    protected boolean isValidCaseOffence(OffenceResults offence) {
        return isNull(offence.getApplicationType()) &&
                nonNull(offence.getImpositionOffenceDetails());
    }

    protected boolean hasDeemedServedOffences(HearingFinancialResultRequest request) {
        return request.getOffenceResults().stream()
                .anyMatch(o -> isValidCaseOffence(o) && o.getIsDeemedServed());
    }

    protected boolean hasACONOffences(HearingFinancialResultRequest request) {
        return request.getOffenceResults().stream()
                .anyMatch(o -> isValidCaseOffence(o) && 
                        o.getIsFinancial() &&
                        ACON.equals(o.getResultCode()));
    }

    protected boolean hasACONAmendmentOffences(HearingFinancialResultRequest request) {
        return request.getOffenceResults().stream()
                .anyMatch(o -> isValidCaseOffence(o) &&
                        o.getIsFinancial() &&
                        ACON.equals(o.getResultCode()) &&
                        nonNull(o.getAmendmentDate()));
    }

    protected boolean hasDeemedServedAmendmentOffences(HearingFinancialResultRequest request) {
        return request.getOffenceResults().stream()
                .anyMatch(OffenceResults::getIsDeemedServed);
    }

    protected boolean hasDeemedServedRemovedOffences(HearingFinancialResultRequest request, Map<UUID, OffenceResultsDetails> prevOffenceResultsDetails) {
        return request.getOffenceResults().stream()
                .anyMatch(or -> isValidCaseOffence(or) &&
                        !or.getIsDeemedServed() && 
                        isPrevOffenceResultDeemedServed(or.getOffenceId(), prevOffenceResultsDetails) &&
                        nonNull(or.getAmendmentDate()));
    }

    protected boolean isPrevOffenceResultDeemedServed(final UUID offenceId, final Map<UUID, OffenceResultsDetails> prevOffenceResultsDetailsMap) {
        return prevOffenceResultsDetailsMap.containsKey(offenceId) && 
                prevOffenceResultsDetailsMap.get(offenceId).getIsDeemedServed();
    }
}
