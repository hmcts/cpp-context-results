package uk.gov.moj.cpp.results.domain.aggregate;

import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Collections.emptyMap;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.ACON_EMAIL_SUBJECT;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.AMEND_AND_RESHARE;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.WRITE_OFF_ONE_DAY_DEEMED_SERVED;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.getApplicationAcceptedSubjects;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.getApplicationDeniedSubjects;
import static uk.gov.moj.cpp.results.domain.aggregate.utils.GobAccountHelper.getOldAccountCorrelations;
import static uk.gov.moj.cpp.results.domain.event.MarkedAggregateSendEmailWhenAccountReceived.markedAggregateSendEmailWhenAccountReceived;

import uk.gov.justice.hearing.courts.HearingFinancialResultRequest;
import uk.gov.justice.hearing.courts.OffenceResults;
import uk.gov.justice.hearing.courts.OffenceResultsDetails;
import uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants;
import uk.gov.moj.cpp.results.domain.aggregate.utils.CorrelationItem;
import uk.gov.moj.cpp.results.domain.aggregate.utils.OldAccountDetailsWrapper;
import uk.gov.moj.cpp.results.domain.event.ImpositionOffenceDetails;
import uk.gov.moj.cpp.results.domain.event.MarkedAggregateSendEmailWhenAccountReceived;
import uk.gov.moj.cpp.results.domain.event.NewApplicationResults;
import uk.gov.moj.cpp.results.domain.event.NewOffenceByResult;
import uk.gov.moj.cpp.results.domain.event.OriginalApplicationResults;

import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Builder class for constructing a MarkedAggregateSendEmailWhenAccountReceived event.
 */
public class MarkedAggregateSendEmailEventBuilder {
    private static final String HEARING_SITTING_DAY_PATTERN = "yyyy-MM-dd";
    private static final String AMENDMENT_REASON = "Admin error on shared result (a result recorded incorrectly)";
    private final String ncesEmail;
    private final LinkedList<CorrelationItem> correlationItemList;

    private MarkedAggregateSendEmailEventBuilder(final String ncesEmail, final LinkedList<CorrelationItem> correlationItemList) {
        this.ncesEmail = ncesEmail;
        this.correlationItemList = new LinkedList<>(correlationItemList);
    }

    public static MarkedAggregateSendEmailEventBuilder markedAggregateSendEmailEventBuilder(final String ncesEmail, final LinkedList<CorrelationItem> correlationItemList) {
        return new MarkedAggregateSendEmailEventBuilder(ncesEmail, correlationItemList);
    }

    @SuppressWarnings("java:S107")
    public MarkedAggregateSendEmailWhenAccountReceived buildMarkedAggregateWithoutOldsForSpecificCorrelationIdWithEmail(final HearingFinancialResultRequest hearingFinancialResultRequest,
                                                                                                                        final String subject,
                                                                                                                        final List<ImpositionOffenceDetails> impositionOffenceDetails,
                                                                                                                        final String isFinancialPenaltiesWrittenOff,
                                                                                                                        final String originalDateOfOffenceList, final String originalDateOfSentenceList,
                                                                                                                        final List<NewOffenceByResult> newResultByOffence, final String applicationResult,
                                                                                                                        final OriginalApplicationResults originalApplicationResults, final NewApplicationResults newApplicationResults,
                                                                                                                        final Map<UUID, List<OffenceResultsDetails>> prevApplicationResultsDetails) {


        final MarkedAggregateSendEmailWhenAccountReceived.Builder builder = markedAggregateSendEmailWhenAccountReceived();

        final List<UUID> offenceIdList = hearingFinancialResultRequest.getOffenceResults().stream().map(OffenceResults::getOffenceId).toList();

        if (isNull(hearingFinancialResultRequest.getAccountCorrelationId())) {
            final OldAccountDetailsWrapper oldCorrelationsWrapper = getOldAccountCorrelations(correlationItemList, hearingFinancialResultRequest.getAccountCorrelationId(), offenceIdList, prevApplicationResultsDetails);
            builder.withAccountCorrelationId(oldCorrelationsWrapper.getRecentAccountCorrelationId());
            builder.withDivisionCode(oldCorrelationsWrapper.getOldDivisionCodes());
            builder.withGobAccountNumber(oldCorrelationsWrapper.getOldGobAccounts());
        }
        builder.withId(randomUUID())
                .withSendTo(ofNullable(hearingFinancialResultRequest.getNcesEmail()).orElse(ncesEmail))
                .withSubject(subject)
                .withHearingCourtCentreName(hearingFinancialResultRequest.getHearingCourtCentreName())
                .withApplicationResult(applicationResult)
                .withImpositionOffenceDetails(impositionOffenceDetails);

        setDefendantDetails(hearingFinancialResultRequest, builder);
        setCasedetails(hearingFinancialResultRequest, builder);
        setHearingSittingDay(hearingFinancialResultRequest, builder);

        if (subject.equals(NCESDecisionConstants.AMEND_AND_RESHARE)) {
            getOffenceAmendmentDate(hearingFinancialResultRequest).ifPresent(offence ->
                    builder.withAmendmentDate(offence.getAmendmentDate())
                            .withAmendmentReason(offence.getAmendmentReason())
            );
            builder.withNewOffenceByResult(newResultByOffence);
            builder.withNewApplicationResults(newApplicationResults);
            builder.withOriginalApplicationResults(originalApplicationResults);

        } else if (getApplicationAcceptedSubjects().contains(subject)) {
            builder.withIsFinancialPenaltiesWrittenOff(isFinancialPenaltiesWrittenOff);
            builder.withOriginalDateOfOffence(originalDateOfOffenceList);
            builder.withOriginalDateOfSentence(originalDateOfSentenceList);
            builder.withNewOffenceByResult(newResultByOffence);
            getOffenceDateOfResult(hearingFinancialResultRequest)
                    .ifPresent(offence -> builder.withDateDecisionMade(offence.getDateOfResult()));
        } else if (originalApplicationResults != null && (getApplicationDeniedSubjects().contains(subject))) {
            builder.withOriginalApplicationResults(originalApplicationResults);
            setDecisionMade(hearingFinancialResultRequest, builder);
        } else {
            setDecisionMade(hearingFinancialResultRequest, builder);
        }
        return builder.build();
    }

    @SuppressWarnings("java:S107")
    public MarkedAggregateSendEmailWhenAccountReceived buildMarkedAggregateForApplicationAccepted(final HearingFinancialResultRequest hearingFinancialResultRequest, final String subject,
                                                                                                  final List<ImpositionOffenceDetails> impositionOffenceDetails,
                                                                                                  final String ncesEMail, final String isFinancialPenaltiesWrittenOff,
                                                                                                  final String originalDateOfOffenceList, final String originalDateOfSentenceList,
                                                                                                  final List<NewOffenceByResult> newResultByOffence, final NewApplicationResults applicationResults,
                                                                                                  final Map<UUID, List<OffenceResultsDetails>> prevApplicationResultsDetails) {
        return buildMarkedAggregateWithAccountDetails(hearingFinancialResultRequest, subject, impositionOffenceDetails,
                ncesEMail, null, isFinancialPenaltiesWrittenOff, originalDateOfOffenceList, originalDateOfSentenceList,
                newResultByOffence, null, applicationResults, prevApplicationResultsDetails, true);
    }

    public MarkedAggregateSendEmailWhenAccountReceived buildMarkedAggregateWithoutOlds(final HearingFinancialResultRequest hearingFinancialResultRequest,
                                                                                       final String subject, final List<ImpositionOffenceDetails> impositionOffenceDetails,
                                                                                       final Boolean includeOldAccountInfoIfAvailable) {

        final List<UUID> offenceIdList = hearingFinancialResultRequest.getOffenceResults().stream().map(OffenceResults::getOffenceId).toList();
        final OldAccountDetailsWrapper correlationsWrapper = getOldAccountCorrelations(correlationItemList, hearingFinancialResultRequest.getAccountCorrelationId(), offenceIdList, emptyMap());

        final MarkedAggregateSendEmailWhenAccountReceived.Builder builder = markedAggregateSendEmailWhenAccountReceived()
                .withId(randomUUID())
                .withSendTo(ofNullable(hearingFinancialResultRequest.getNcesEmail()).orElse(ncesEmail))
                .withSubject(subject)
                .withHearingCourtCentreName(hearingFinancialResultRequest.getHearingCourtCentreName())
                .withAccountCorrelationId(hearingFinancialResultRequest.getAccountCorrelationId())
                .withDivisionCode(hearingFinancialResultRequest.getAccountDivisionCode())
                .withImpositionOffenceDetails(impositionOffenceDetails);

        setDefendantDetails(hearingFinancialResultRequest, builder);
        setCasedetails(hearingFinancialResultRequest, builder);
        setHearingSittingDay(hearingFinancialResultRequest, builder);

        if (Boolean.TRUE.equals(includeOldAccountInfoIfAvailable) && isNotEmpty(correlationsWrapper.getOldAccountDetails())) {
            builder.withGobAccountNumber(correlationsWrapper.getOldGobAccounts())
                    .withAccountCorrelationId(correlationsWrapper.getRecentAccountCorrelationId());
        }
        if (WRITE_OFF_ONE_DAY_DEEMED_SERVED.equals(subject) || ACON_EMAIL_SUBJECT.equals(subject)) {
            getOffenceDateOfResult(hearingFinancialResultRequest)
                    .ifPresent(offence -> builder.withDateDecisionMade(offence.getDateOfResult()));
        } else {
            getOffenceAmendmentDate(hearingFinancialResultRequest)
                    .ifPresent(offence -> builder.withAmendmentDate(offence.getAmendmentDate())
                            .withAmendmentReason(offence.getAmendmentReason()));
        }
        return builder.build();
    }

    @SuppressWarnings("java:S107")
    public MarkedAggregateSendEmailWhenAccountReceived buildMarkedAggregateWithOlds(final HearingFinancialResultRequest hearingFinancialResultRequest, final List<ImpositionOffenceDetails> impositionOffenceDetails,
                                                                                    final String applicationResult, final List<NewOffenceByResult> newResultByOffenceList,
                                                                                    final OriginalApplicationResults originalApplicationResults,
                                                                                    final NewApplicationResults newApplicationResults,
                                                                                    final String subject, final Map<UUID, List<OffenceResultsDetails>> prevApplicationResultsDetails) {
        return buildMarkedAggregateWithAccountDetails(hearingFinancialResultRequest, subject, impositionOffenceDetails,
                null, applicationResult, null, null, null, newResultByOffenceList, originalApplicationResults,
                newApplicationResults, prevApplicationResultsDetails, false);
    }

    @SuppressWarnings("java:S107")
    private MarkedAggregateSendEmailWhenAccountReceived buildMarkedAggregateWithAccountDetails(
            final HearingFinancialResultRequest hearingFinancialResultRequest,
            final String subject,
            final List<ImpositionOffenceDetails> impositionOffenceDetails,
            final String ncesEMail,
            final String applicationResult,
            final String isFinancialPenaltiesWrittenOff,
            final String originalDateOfOffenceList,
            final String originalDateOfSentenceList,
            final List<NewOffenceByResult> newResultByOffence,
            final OriginalApplicationResults originalApplicationResults,
            final NewApplicationResults newApplicationResults,
            final Map<UUID, List<OffenceResultsDetails>> prevApplicationResultsDetails,
            final boolean isApplicationAccepeted) {

        final List<UUID> offenceIdList = hearingFinancialResultRequest.getOffenceResults().stream().map(OffenceResults::getOffenceId).toList();
        final OldAccountDetailsWrapper correlationsWrapper = getOldAccountCorrelations(correlationItemList, hearingFinancialResultRequest.getAccountCorrelationId(), offenceIdList, prevApplicationResultsDetails);

        final MarkedAggregateSendEmailWhenAccountReceived.Builder builder = markedAggregateSendEmailWhenAccountReceived()
                .withId(randomUUID())
                .withSendTo(ofNullable(ncesEMail).orElse(ofNullable(hearingFinancialResultRequest.getNcesEmail()).orElse(ncesEmail)))
                .withSubject(subject)
                .withDivisionCode(hearingFinancialResultRequest.getAccountDivisionCode())
                .withImpositionOffenceDetails(impositionOffenceDetails);

        setDefendantDetails(hearingFinancialResultRequest, builder);
        setCasedetails(hearingFinancialResultRequest, builder);
        setHearingSittingDay(hearingFinancialResultRequest, builder);

        if (isApplicationAccepeted) {
            if (isNull(hearingFinancialResultRequest.getAccountCorrelationId())) {
                builder.withAccountCorrelationId(correlationsWrapper.getRecentAccountCorrelationId());
                builder.withGobAccountNumber(correlationsWrapper.getOldGobAccounts());
            } else {
                builder.withAccountCorrelationId(hearingFinancialResultRequest.getAccountCorrelationId())
                        .withOldAccountDetails(correlationsWrapper.getOldAccountDetails());
            }
            builder.withHearingCourtCentreName(hearingFinancialResultRequest.getHearingCourtCentreName());
            setDecisionMade(hearingFinancialResultRequest, builder);
        } else {
            builder.withAccountCorrelationId(hearingFinancialResultRequest.getAccountCorrelationId());
            if (isNotEmpty(correlationsWrapper.getOldAccountDetails())) {
                builder.withOldAccountDetails(correlationsWrapper.getOldAccountDetails());
            }
        }
        ofNullable(newApplicationResults).ifPresent(builder::withNewApplicationResults);
        ofNullable(originalApplicationResults).ifPresent(builder::withOriginalApplicationResults);
        ofNullable(newResultByOffence).ifPresent(builder::withNewOffenceByResult);

        if (AMEND_AND_RESHARE.equals(subject)) {
            builder.withHearingCourtCentreName(hearingFinancialResultRequest.getHearingCourtCentreName());
            ofNullable(applicationResult).ifPresent(builder::withApplicationResult);
            final Optional<OffenceResults> offenceResult = getOffenceAmendmentDate(hearingFinancialResultRequest);
            builder.withAmendmentDate(offenceResult.map(OffenceResults::getAmendmentDate).orElse(LocalDate.now().toString()));
            builder.withAmendmentReason(offenceResult.map(OffenceResults::getAmendmentReason).orElse(AMENDMENT_REASON));
        } else if (getApplicationAcceptedSubjects().contains(subject)) {
            ofNullable(isFinancialPenaltiesWrittenOff).ifPresent(builder::withIsFinancialPenaltiesWrittenOff);
            ofNullable(originalDateOfOffenceList).ifPresent(builder::withOriginalDateOfOffence);
            ofNullable(originalDateOfSentenceList).ifPresent(builder::withOriginalDateOfSentence);
            getOffenceDateOfResult(hearingFinancialResultRequest)
                    .ifPresent(offence -> builder.withDateDecisionMade(offence.getDateOfResult()));
        }
        return builder.build();
    }

    private void setDecisionMade(final HearingFinancialResultRequest hearingFinancialResultRequest, final MarkedAggregateSendEmailWhenAccountReceived.Builder builder) {
        getOffenceDateOfResult(hearingFinancialResultRequest)
                .ifPresent(offence -> builder.withDateDecisionMade(offence.getDateOfResult()));
    }

    private void setDefendantDetails(final HearingFinancialResultRequest hearingFinancialResultRequest,
                                     final MarkedAggregateSendEmailWhenAccountReceived.Builder builder) {
        builder.withDefendantName(hearingFinancialResultRequest.getDefendantName())
                .withDefendantDateOfBirth(hearingFinancialResultRequest.getDefendantDateOfBirth())
                .withDefendantAddress(hearingFinancialResultRequest.getDefendantAddress())
                .withDefendantEmail(hearingFinancialResultRequest.getDefendantEmail())
                .withDefendantContactNumber(hearingFinancialResultRequest.getDefendantContactNumber());
    }

    private void setCasedetails(final HearingFinancialResultRequest hearingFinancialResultRequest,
                                final MarkedAggregateSendEmailWhenAccountReceived.Builder builder) {
        builder.withCaseReferences(String.join(NCESDecisionConstants.COMMA, hearingFinancialResultRequest.getProsecutionCaseReferences()))
                .withMasterDefendantId(hearingFinancialResultRequest.getMasterDefendantId())
                .withIsSJPHearing(hearingFinancialResultRequest.getIsSJPHearing());
    }

    private void setHearingSittingDay(final HearingFinancialResultRequest hearingFinancialResultRequest,
                                      final MarkedAggregateSendEmailWhenAccountReceived.Builder builder) {
        ofNullable(hearingFinancialResultRequest.getHearingSittingDay())
                .ifPresent(sittingDay -> builder.withHearingSittingDay(sittingDay.format(ofPattern(HEARING_SITTING_DAY_PATTERN))));
    }

    private Optional<OffenceResults> getOffenceDateOfResult(final HearingFinancialResultRequest hearingFinancialResultRequest) {
        return hearingFinancialResultRequest.getOffenceResults().stream()
                .filter(offence -> nonNull(offence.getDateOfResult()))
                .findFirst();
    }

    private Optional<OffenceResults> getOffenceAmendmentDate(final HearingFinancialResultRequest hearingFinancialResultRequest) {
        return hearingFinancialResultRequest.getOffenceResults().stream()
                .filter(offence -> nonNull(offence.getAmendmentDate()))
                .findFirst();
    }
}
