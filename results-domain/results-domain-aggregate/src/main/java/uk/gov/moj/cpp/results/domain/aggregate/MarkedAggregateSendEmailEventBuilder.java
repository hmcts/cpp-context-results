package uk.gov.moj.cpp.results.domain.aggregate;

import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.collections.CollectionUtils.isEqualCollection;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.AMEND_AND_RESHARE;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.getApplicationAppealAllowedSubjects;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.getApplicationAppealSubjects;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.getApplicationGrantedSubjects;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.getApplicationNonGrantedSubjects;
import static uk.gov.moj.cpp.results.domain.event.MarkedAggregateSendEmailWhenAccountReceived.markedAggregateSendEmailWhenAccountReceived;

import uk.gov.justice.core.courts.CorrelationIdHistoryItem;
import uk.gov.justice.hearing.courts.HearingFinancialResultRequest;
import uk.gov.justice.hearing.courts.OffenceResults;
import uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants;
import uk.gov.moj.cpp.results.domain.event.ImpositionOffenceDetails;
import uk.gov.moj.cpp.results.domain.event.MarkedAggregateSendEmailWhenAccountReceived;
import uk.gov.moj.cpp.results.domain.event.NewApplicationResults;
import uk.gov.moj.cpp.results.domain.event.NewOffenceByResult;
import uk.gov.moj.cpp.results.domain.event.OriginalApplicationResults;

import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Builder class for constructing a MarkedAggregateSendEmailWhenAccountReceived event.
 */
class MarkedAggregateSendEmailEventBuilder {
    private static final String HEARING_SITTING_DAY_PATTERN = "yyyy-MM-dd";
    private static final String AMENDMENT_REASON = "Admin error on shared result (a result recorded incorrectly)";
    private final String ncesEmail;
    private final LinkedList<CorrelationIdHistoryItem> correlationIdHistoryItemList;

    private MarkedAggregateSendEmailEventBuilder(final String ncesEmail, final LinkedList<CorrelationIdHistoryItem> correlationIdHistoryItemList) {
        this.ncesEmail = ncesEmail;
        this.correlationIdHistoryItemList = correlationIdHistoryItemList;
    }

    static MarkedAggregateSendEmailEventBuilder markedAggregateSendEmailEventBuilder(final String ncesEmail, final LinkedList<CorrelationIdHistoryItem> correlationIdHistoryItemList) {
        return new MarkedAggregateSendEmailEventBuilder(ncesEmail, correlationIdHistoryItemList);
    }


    @SuppressWarnings("java:S107")
    MarkedAggregateSendEmailWhenAccountReceived buildMarkedAggregateWithoutOldsForSpecificCorrelationId(final HearingFinancialResultRequest hearingFinancialResultRequest, final String subject, final CorrelationIdHistoryItem correlationIdHistoryItem, final List<ImpositionOffenceDetails> impositionOffenceDetails,
                                                                                                        final String isWrittenOffExists,
                                                                                                        final String originalDateOfOffenceList,
                                                                                                        final String originalDateOfSentenceList,
                                                                                                        final List<NewOffenceByResult> newResultByOffenceList,
                                                                                                        final String applicationResult,
                                                                                                        final Optional<OriginalApplicationResults> originalApplicationResults,
                                                                                                        final Optional<NewApplicationResults> newApplicationResults) {
        return buildMarkedAggregateWithoutOldsForSpecificCorrelationIdWithEmail(hearingFinancialResultRequest, subject, correlationIdHistoryItem, impositionOffenceDetails,
                ofNullable(hearingFinancialResultRequest.getNcesEmail()).orElse(ncesEmail), isWrittenOffExists, originalDateOfOffenceList,
                originalDateOfSentenceList, newResultByOffenceList, applicationResult, originalApplicationResults, newApplicationResults);
    }

    @SuppressWarnings("java:S107")
    MarkedAggregateSendEmailWhenAccountReceived buildMarkedAggregateWithoutOldsForSpecificCorrelationIdWithEmail(final HearingFinancialResultRequest hearingFinancialResultRequest, final String subject, final CorrelationIdHistoryItem correlationIdHistoryItem, final List<ImpositionOffenceDetails> impositionOffenceDetails, final String ncesEMail,
                                                                                                                 final String isFinancialPenaltiesWrittenOff,
                                                                                                                 final String originalDateOfOffenceList,
                                                                                                                 final String originalDateOfSentenceList,
                                                                                                                 final List<NewOffenceByResult> newResultByOffence,
                                                                                                                 final String applicationResult,
                                                                                                                 final Optional<OriginalApplicationResults> originalApplicationResults,
                                                                                                                 final Optional<NewApplicationResults> newApplicationResults) {
        final MarkedAggregateSendEmailWhenAccountReceived.Builder builder = markedAggregateSendEmailWhenAccountReceived()
                .withId(randomUUID())
                .withSendTo(ncesEMail)
                .withSubject(subject)
                .withHearingCourtCentreName(hearingFinancialResultRequest.getHearingCourtCentreName())
                .withDefendantName(hearingFinancialResultRequest.getDefendantName())
                .withDefendantDateOfBirth(hearingFinancialResultRequest.getDefendantDateOfBirth())
                .withDefendantAddress(hearingFinancialResultRequest.getDefendantAddress())
                .withDefendantEmail(hearingFinancialResultRequest.getDefendantEmail())
                .withDefendantContactNumber(hearingFinancialResultRequest.getDefendantContactNumber())
                .withIsSJPHearing(hearingFinancialResultRequest.getIsSJPHearing())
                .withApplicationResult(applicationResult)
                .withCaseReferences(String.join(NCESDecisionConstants.COMMA, hearingFinancialResultRequest.getProsecutionCaseReferences()))
                .withMasterDefendantId(hearingFinancialResultRequest.getMasterDefendantId())
                .withAccountCorrelationId(correlationIdHistoryItem.getAccountCorrelationId())
                .withGobAccountNumber(correlationIdHistoryItem.getAccountNumber())
                .withDivisionCode(correlationIdHistoryItem.getAccountDivisionCode())
                .withImpositionOffenceDetails(impositionOffenceDetails);

        ofNullable(hearingFinancialResultRequest.getHearingSittingDay())
                .ifPresent(a -> builder.withHearingSittingDay(a.format(ofPattern(HEARING_SITTING_DAY_PATTERN))));

        if (subject.equals(NCESDecisionConstants.AMEND_AND_RESHARE)) {
            hearingFinancialResultRequest.getOffenceResults().stream().filter(offence -> nonNull(offence.getAmendmentDate())).findFirst().ifPresent(offence ->
                    builder.withAmendmentDate(offence.getAmendmentDate())
                            .withAmendmentReason(offence.getAmendmentReason())
            );
            builder.withNewOffenceByResult(newResultByOffence);
            builder.withNewApplicationResults(newApplicationResults.orElse(null));
            builder.withOriginalApplicationResults(originalApplicationResults.orElse(null));

        } else if (getApplicationGrantedSubjects().contains(subject) || getApplicationAppealAllowedSubjects().contains(subject)) {
            builder.withIsFinancialPenaltiesWrittenOff(isFinancialPenaltiesWrittenOff);
            builder.withOriginalDateOfOffence(originalDateOfOffenceList);
            builder.withOriginalDateOfSentence(originalDateOfSentenceList);
            builder.withNewOffenceByResult(newResultByOffence);
            hearingFinancialResultRequest.getOffenceResults().stream().filter(offence -> nonNull(offence.getDateOfResult())).findFirst().ifPresent(offence ->
                    builder.withDateDecisionMade(offence.getDateOfResult()));
        } else if (originalApplicationResults.isPresent() && (getApplicationAppealSubjects().contains(subject) || getApplicationNonGrantedSubjects().contains(subject))) {
            builder.withOriginalApplicationResults(originalApplicationResults.get());
            buildDecisionMade(hearingFinancialResultRequest, builder);
        } else {
            buildDecisionMade(hearingFinancialResultRequest, builder);
        }
        return builder.build();
    }

    MarkedAggregateSendEmailWhenAccountReceived buildMarkedAggregateWithoutOlds(final HearingFinancialResultRequest hearingFinancialResultRequest, final String subject, final List<ImpositionOffenceDetails> impositionOffenceDetails, final Boolean includeOlds) {

        CorrelationIdHistoryItem previousItem = null;
        if (Boolean.TRUE.equals(includeOlds)) {
            previousItem = correlationIdHistoryItemList.peekLast();
            final LinkedList<CorrelationIdHistoryItem> filteredList = correlationIdHistoryItemList.stream()
                    .filter(e -> isNotEmpty(e.getProsecutionCaseReferences())
                            && isNotEmpty(hearingFinancialResultRequest.getProsecutionCaseReferences())
                            && isEqualCollection(e.getProsecutionCaseReferences(), hearingFinancialResultRequest.getProsecutionCaseReferences()))
                    .collect(Collectors.toCollection(LinkedList::new));

            if (!isEmpty(filteredList)) {
                previousItem = filteredList.peekLast();
            }
        }
        final MarkedAggregateSendEmailWhenAccountReceived.Builder builder = markedAggregateSendEmailWhenAccountReceived()
                .withId(randomUUID())
                .withSendTo(ofNullable(hearingFinancialResultRequest.getNcesEmail()).orElse(ncesEmail))
                .withSubject(subject)
                .withIsSJPHearing(hearingFinancialResultRequest.getIsSJPHearing())
                .withHearingCourtCentreName(hearingFinancialResultRequest.getHearingCourtCentreName())
                .withDefendantName(hearingFinancialResultRequest.getDefendantName())
                .withDefendantDateOfBirth(hearingFinancialResultRequest.getDefendantDateOfBirth())
                .withDefendantAddress(hearingFinancialResultRequest.getDefendantAddress())
                .withDefendantEmail(hearingFinancialResultRequest.getDefendantEmail())
                .withDefendantContactNumber(hearingFinancialResultRequest.getDefendantContactNumber())
                .withCaseReferences(String.join(NCESDecisionConstants.COMMA, hearingFinancialResultRequest.getProsecutionCaseReferences()))
                .withMasterDefendantId(hearingFinancialResultRequest.getMasterDefendantId())
                .withAccountCorrelationId(hearingFinancialResultRequest.getAccountCorrelationId())
                .withDivisionCode(hearingFinancialResultRequest.getAccountDivisionCode())
                .withImpositionOffenceDetails(impositionOffenceDetails);

        if (Boolean.TRUE.equals(includeOlds) && previousItem != null) {
            builder.withGobAccountNumber(previousItem.getAccountNumber())
                    .withAccountCorrelationId(previousItem.getAccountCorrelationId());
        }

        ofNullable(hearingFinancialResultRequest.getHearingSittingDay())
                .ifPresent(a -> builder.withHearingSittingDay(a.format(ofPattern(HEARING_SITTING_DAY_PATTERN))));

        if (NCESDecisionConstants.WRITE_OFF_ONE_DAY_DEEMED_SERVED.equals(subject) || NCESDecisionConstants.ACON_EMAIL_SUBJECT.equals(subject)) {
            hearingFinancialResultRequest.getOffenceResults().stream()
                    .filter(offence -> nonNull(offence.getDateOfResult()))
                    .findFirst()
                    .ifPresent(offence -> builder.withDateDecisionMade(offence.getDateOfResult()));
        } else {
            hearingFinancialResultRequest.getOffenceResults().stream()
                    .filter(offence -> nonNull(offence.getAmendmentDate()))
                    .findFirst()
                    .ifPresent(offence -> builder.withAmendmentDate(offence.getAmendmentDate()).withAmendmentReason(offence.getAmendmentReason()));
        }
        return builder.build();
    }

    MarkedAggregateSendEmailWhenAccountReceived buildMarkedAggregateWithOlds(final HearingFinancialResultRequest hearingFinancialResultRequest, final List<ImpositionOffenceDetails> impositionOffenceDetails,
                                                                             final String applicationResult, final List<NewOffenceByResult> newResultByOffenceList,
                                                                             final Optional<OriginalApplicationResults> originalApplicationResults,
                                                                             final Optional<NewApplicationResults> newApplicationResults,
                                                                             final String subject) {
        CorrelationIdHistoryItem previousItem = correlationIdHistoryItemList.peekLast();
        final LinkedList<CorrelationIdHistoryItem> filteredList = correlationIdHistoryItemList.stream()
                .filter(e -> isNotEmpty(e.getProsecutionCaseReferences())
                        && isNotEmpty(hearingFinancialResultRequest.getProsecutionCaseReferences())
                        && isEqualCollection(e.getProsecutionCaseReferences(), hearingFinancialResultRequest.getProsecutionCaseReferences()))
                .collect(Collectors.toCollection(LinkedList::new));

        if (!isEmpty(filteredList)) {
            previousItem = filteredList.peekLast();
        }


        final Optional<OffenceResults> offenceResult = hearingFinancialResultRequest.getOffenceResults().stream().filter(offence -> nonNull(offence.getAmendmentDate())).findFirst();
        final MarkedAggregateSendEmailWhenAccountReceived.Builder builder = markedAggregateSendEmailWhenAccountReceived()
                .withId(randomUUID())
                .withSendTo(ofNullable(hearingFinancialResultRequest.getNcesEmail()).orElse(ncesEmail))
                .withSubject(subject)
                .withDefendantName(hearingFinancialResultRequest.getDefendantName())
                .withDefendantDateOfBirth(hearingFinancialResultRequest.getDefendantDateOfBirth())
                .withDefendantAddress(hearingFinancialResultRequest.getDefendantAddress())
                .withDefendantEmail(hearingFinancialResultRequest.getDefendantEmail())
                .withDefendantContactNumber(hearingFinancialResultRequest.getDefendantContactNumber())
                .withCaseReferences(String.join(NCESDecisionConstants.COMMA, hearingFinancialResultRequest.getProsecutionCaseReferences()))
                .withMasterDefendantId(hearingFinancialResultRequest.getMasterDefendantId())
                .withAccountCorrelationId(hearingFinancialResultRequest.getAccountCorrelationId())
                .withOldAccountCorrelationId(previousItem.getAccountCorrelationId())
                .withOldGobAccountNumber(previousItem.getAccountNumber())
                .withDivisionCode(hearingFinancialResultRequest.getAccountDivisionCode())
                .withOldDivisionCode(previousItem.getAccountDivisionCode())
                .withImpositionOffenceDetails(impositionOffenceDetails)
                .withIsSJPHearing(hearingFinancialResultRequest.getIsSJPHearing())
                .withNewOffenceByResult(newResultByOffenceList)
                .withOriginalApplicationResults(originalApplicationResults.isPresent() ? originalApplicationResults.get() : null)
                .withNewApplicationResults(newApplicationResults.isPresent() ? newApplicationResults.get() : null);
        if (AMEND_AND_RESHARE.equals(subject)) {
            builder.withHearingCourtCentreName(hearingFinancialResultRequest.getHearingCourtCentreName());
            builder.withApplicationResult(applicationResult);
            builder.withAmendmentDate(offenceResult.map(OffenceResults::getAmendmentDate).orElse(LocalDate.now().toString()));
            builder.withAmendmentReason(offenceResult.map(OffenceResults::getAmendmentReason).orElse(AMENDMENT_REASON));
        }
        ofNullable(hearingFinancialResultRequest.getHearingSittingDay())
                .ifPresent(a -> builder.withHearingSittingDay(a.format(ofPattern(HEARING_SITTING_DAY_PATTERN))));
        return builder.build();
    }

    private void buildDecisionMade(final HearingFinancialResultRequest hearingFinancialResultRequest, final MarkedAggregateSendEmailWhenAccountReceived.Builder builder) {
        hearingFinancialResultRequest.getOffenceResults().stream().filter(offence -> nonNull(offence.getDateOfResult())).findFirst().ifPresent(offence ->
                builder.withDateDecisionMade(offence.getDateOfResult())
        );
    }
}
