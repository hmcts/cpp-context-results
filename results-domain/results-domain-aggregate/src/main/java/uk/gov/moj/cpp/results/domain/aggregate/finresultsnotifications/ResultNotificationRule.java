package uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.APPLICATION_SUBJECT;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.APPLICATION_TYPES;
import static uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.cases.AbstractCaseResultNotificationRule.isCaseAmended;

import uk.gov.justice.hearing.courts.HearingFinancialResultRequest;
import uk.gov.justice.hearing.courts.OffenceResults;
import uk.gov.justice.hearing.courts.OffenceResultsDetails;
import uk.gov.moj.cpp.results.domain.aggregate.utils.CorrelationItem;
import uk.gov.moj.cpp.results.domain.event.MarkedAggregateSendEmailWhenAccountReceived;
import uk.gov.moj.cpp.results.domain.event.NewOffenceByResult;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Contract for rules that determine when to send result notifications.
 * `appliesTo` method makes initial checks to see if the rule applies to the given input.
 */
public interface ResultNotificationRule {
    /**
     * Preliminary check to see if the rule applies to the given input.
     *
     * @param input the input data for the rule
     * @return true if the rule applies, false otherwise
     */
    boolean appliesTo(RuleInput input);

    /**
     * Applies the rule to the given input and returns an optional result containing a MarkedAggregateSendEmailWhenAccountReceived event.
     *
     * @param input the input data for the rule
     * @return an optional containing the result if the rule applies, or empty if it does not
     */
    Optional<MarkedAggregateSendEmailWhenAccountReceived> apply(RuleInput input);

    /**
     * Input data for the rule. This includes the hearing financial result request, and previous offence results details
     * from Aggregate.
     */
    record RuleInput(HearingFinancialResultRequest request,
                     String isWrittenOffExists,
                     String originalDateOfOffenceList,
                     String originalDateOfSentenceList,
                     List<NewOffenceByResult> newOffenceResultsFromHearing,
                     String applicationResult,
                     Map<UUID, String> offenceDateMap,
                     String ncesEmail,
                     Map<UUID, OffenceResultsDetails> prevOffenceResultsDetails,
                     Map<UUID, List<OffenceResultsDetails>> prevApplicationResultsDetails,
                     Map<UUID, List<OffenceResultsDetails>> prevApplicationOffenceResultsMap,
                     LinkedList<CorrelationItem> correlationItemList) {

        public boolean hasAnyApplicationType() {
            return request.getOffenceResults().stream().anyMatch(offence -> nonNull(offence.getApplicationType()));
        }

        public boolean hasValidApplicationType() {
            return request.getOffenceResults().stream()
                    .anyMatch(offence -> APPLICATION_TYPES.containsKey(offence.getApplicationType()));
        }

        public boolean hasCaseResult() {
            return request.getOffenceResults().stream().anyMatch(offence -> isNull(offence.getApplicationType()));
        }

        public boolean isNewApplication() {
            final boolean noAmendments = request.getOffenceResults().stream().noneMatch(offence -> nonNull(offence.getAmendmentDate()));
            return hasAnyApplicationType() && noAmendments;
        }

        public boolean isValidApplicationTypeWithAllowedResultCode() {
            return request.getOffenceResults().stream()
                    .filter(offence -> APPLICATION_TYPES.containsKey(offence.getApplicationType()))
                    .anyMatch(offence -> APPLICATION_SUBJECT.get(offence.getApplicationType()).containsKey(offence.getResultCode()));
        }

        public boolean isCaseAmendmentProcess() {
            return isCaseAmendment() || isDeemedServedChangedForCase();
        }

        public boolean isAmendmentFlow() {
            return request.getOffenceResults().stream().anyMatch(o -> nonNull(o.getAmendmentDate()));
        }

        public boolean hasFinancialAmendments() {
            return nonNull(request.getAccountCorrelationId());
        }

        public boolean isFinancial() {
            return request.getOffenceResults().stream().anyMatch(OffenceResults::getIsFinancial);
        }

        public boolean isCaseAmendment() {
            return request.getOffenceResults().stream()
                    .filter(offence -> isNull(offence.getApplicationType()))
                    .anyMatch(offence -> nonNull(offence.getAmendmentDate()));
        }

        public boolean hasFinancialTransitionInTheCase() {
            final boolean currentAllFin = request.getOffenceResults().stream()
                    .filter(isCaseAmended)
                    .allMatch(OffenceResults::getIsFinancial);

            final boolean hasMixedFinancialOffences = prevOffenceResultsDetails.isEmpty()
                    || request.getOffenceResults().stream().filter(isCaseAmended).allMatch(o -> isNull(prevOffenceResultsDetails.get(o.getOffenceId())))
                    || (request.getOffenceResults().stream().filter(isCaseAmended)
                    .anyMatch(o -> nonNull(prevOffenceResultsDetails.get(o.getOffenceId())) && prevOffenceResultsDetails.get(o.getOffenceId()).getIsFinancial())
                    && request.getOffenceResults().stream().filter(isCaseAmended)
                    .anyMatch(o -> nonNull(prevOffenceResultsDetails.get(o.getOffenceId())) && !prevOffenceResultsDetails.get(o.getOffenceId()).getIsFinancial()));

            final boolean nonFinToFinTransition = prevOffenceResultsDetails.isEmpty()
                    || request.getOffenceResults().stream()
                    .filter(isCaseAmended)
                    .anyMatch(o -> o.getIsFinancial()
                            && nonNull(prevOffenceResultsDetails.get(o.getOffenceId()))
                            && !prevOffenceResultsDetails.get(o.getOffenceId()).getIsFinancial());


            return currentAllFin && hasMixedFinancialOffences && nonFinToFinTransition;
        }

        private boolean isDeemedServedChangedForCase() {
            return request.getOffenceResults().stream()
                    .anyMatch(offenceResult ->
                            ofNullable(prevOffenceResultsDetails.get(offenceResult.getOffenceId()))
                                    .map(prevOffenceResult -> !Objects.equals(prevOffenceResult.getIsDeemedServed(), offenceResult.getIsDeemedServed()))
                                    .orElse(false));
        }
    }
}