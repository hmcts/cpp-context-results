package uk.gov.moj.cpp.results.domain.aggregate;

import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Collections.emptyMap;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.ACON_EMAIL_SUBJECT;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.AMEND_AND_RESHARE;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.WRITE_OFF_ONE_DAY_DEEMED_SERVED;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.getApplicationAppealAllowedSubjects;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.getApplicationAppealSubjects;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.getApplicationGrantedSubjects;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.getApplicationNonGrantedSubjects;
import static uk.gov.moj.cpp.results.domain.aggregate.utils.GobAccountHelper.getOldAccountCorrelations;
import static uk.gov.moj.cpp.results.domain.event.MarkedAggregateSendEmailWhenAccountReceived.markedAggregateSendEmailWhenAccountReceived;

import uk.gov.justice.hearing.courts.HearingFinancialResultRequest;
import uk.gov.justice.hearing.courts.OffenceResults;
import uk.gov.justice.hearing.courts.OffenceResultsDetails;
import uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants;
import uk.gov.moj.cpp.results.domain.aggregate.utils.CorrelationItem;
import uk.gov.moj.cpp.results.domain.aggregate.utils.OldAccountCorrelation;
import uk.gov.moj.cpp.results.domain.aggregate.utils.OldAccountCorrelationsWrapper;
import uk.gov.moj.cpp.results.domain.event.ImpositionOffenceDetails;
import uk.gov.moj.cpp.results.domain.event.MarkedAggregateSendEmailWhenAccountReceived;
import uk.gov.moj.cpp.results.domain.event.NewApplicationResults;
import uk.gov.moj.cpp.results.domain.event.NewOffenceByResult;
import uk.gov.moj.cpp.results.domain.event.OriginalApplicationResults;

import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    public MarkedAggregateSendEmailWhenAccountReceived buildMarkedAggregateWithoutOldsForSpecificCorrelationId(final HearingFinancialResultRequest hearingFinancialResultRequest,
                                                                                                               final String subject,
                                                                                                               final List<ImpositionOffenceDetails> impositionOffenceDetails,
                                                                                                               final String isWrittenOffExists,
                                                                                                               final String originalDateOfOffenceList,
                                                                                                               final String originalDateOfSentenceList,
                                                                                                               final List<NewOffenceByResult> newResultByOffenceList,
                                                                                                               final String applicationResult,
                                                                                                               final OriginalApplicationResults originalApplicationResults,
                                                                                                               final NewApplicationResults newApplicationResults,
                                                                                                               final Map<UUID, List<OffenceResultsDetails>> prevApplicationResultsDetails
    ) {
        return buildMarkedAggregateWithoutOldsForSpecificCorrelationIdWithEmail(hearingFinancialResultRequest, subject, impositionOffenceDetails,
                ofNullable(hearingFinancialResultRequest.getNcesEmail()).orElse(ncesEmail), isWrittenOffExists, originalDateOfOffenceList,
                originalDateOfSentenceList, newResultByOffenceList, applicationResult, originalApplicationResults, newApplicationResults, prevApplicationResultsDetails);
    }

    @SuppressWarnings("java:S107")
    public MarkedAggregateSendEmailWhenAccountReceived buildMarkedAggregateWithoutOldsForSpecificCorrelationIdWithEmail(final HearingFinancialResultRequest hearingFinancialResultRequest,
                                                                                                                        final String subject,
                                                                                                                        final List<ImpositionOffenceDetails> impositionOffenceDetails,
                                                                                                                        final String ncesEMail, final String isFinancialPenaltiesWrittenOff,
                                                                                                                        final String originalDateOfOffenceList, final String originalDateOfSentenceList,
                                                                                                                        final List<NewOffenceByResult> newResultByOffence, final String applicationResult,
                                                                                                                        final OriginalApplicationResults originalApplicationResults, final NewApplicationResults newApplicationResults,
                                                                                                                        final Map<UUID, List<OffenceResultsDetails>> prevApplicationResultsDetails) {


        final MarkedAggregateSendEmailWhenAccountReceived.Builder builder = markedAggregateSendEmailWhenAccountReceived();

        final List<UUID> offenceIdList = hearingFinancialResultRequest.getOffenceResults().stream().map(OffenceResults::getOffenceId).toList();
        final OldAccountCorrelationsWrapper correlationsWrapper = getOldAccountCorrelations(correlationItemList, hearingFinancialResultRequest.getAccountCorrelationId(), offenceIdList, prevApplicationResultsDetails);

        if (isNull(hearingFinancialResultRequest.getAccountCorrelationId())) {
            final OldAccountCorrelationsWrapper oldCorrelationsWrapper = getOldAccountCorrelations(correlationItemList, hearingFinancialResultRequest.getAccountCorrelationId(), offenceIdList, prevApplicationResultsDetails);

            builder.withAccountCorrelationId(oldCorrelationsWrapper.getRecentAccountCorrelationId());
            builder.withDivisionCode(oldCorrelationsWrapper.getOldDivisionCodes());
            builder.withGobAccountNumber(oldCorrelationsWrapper.getOldGobAccounts());
        }
        builder.withId(randomUUID())
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
                .withImpositionOffenceDetails(impositionOffenceDetails)
                .withIsValidOldCorrelationAndAccount(isValidOldCorrelationAndAccount(correlationsWrapper.getOldAccountCorrelationsList()));

        ofNullable(hearingFinancialResultRequest.getHearingSittingDay())
                .ifPresent(a -> builder.withHearingSittingDay(a.format(ofPattern(HEARING_SITTING_DAY_PATTERN))));

        if (subject.equals(NCESDecisionConstants.AMEND_AND_RESHARE)) {
            hearingFinancialResultRequest.getOffenceResults().stream().filter(offence -> nonNull(offence.getAmendmentDate())).findFirst().ifPresent(offence ->
                    builder.withAmendmentDate(offence.getAmendmentDate())
                            .withAmendmentReason(offence.getAmendmentReason())
            );
            builder.withNewOffenceByResult(newResultByOffence);
            builder.withNewApplicationResults(newApplicationResults);
            builder.withOriginalApplicationResults(originalApplicationResults);

        } else if (getApplicationGrantedSubjects().contains(subject) || getApplicationAppealAllowedSubjects().contains(subject)) {
            builder.withIsFinancialPenaltiesWrittenOff(isFinancialPenaltiesWrittenOff);
            builder.withOriginalDateOfOffence(originalDateOfOffenceList);
            builder.withOriginalDateOfSentence(originalDateOfSentenceList);
            builder.withNewOffenceByResult(newResultByOffence);
            hearingFinancialResultRequest.getOffenceResults().stream().filter(offence -> nonNull(offence.getDateOfResult())).findFirst().ifPresent(offence ->
                    builder.withDateDecisionMade(offence.getDateOfResult()));
        } else if (originalApplicationResults != null && (getApplicationAppealSubjects().contains(subject) || getApplicationNonGrantedSubjects().contains(subject))) {
            builder.withOriginalApplicationResults(originalApplicationResults);
            buildDecisionMade(hearingFinancialResultRequest, builder);
        } else {
            buildDecisionMade(hearingFinancialResultRequest, builder);
        }
        return builder.build();
    }

    @SuppressWarnings("java:S107")
    public MarkedAggregateSendEmailWhenAccountReceived buildMarkedAggregateGranted(final HearingFinancialResultRequest hearingFinancialResultRequest, final String subject,
                                                                                   final List<ImpositionOffenceDetails> impositionOffenceDetails,
                                                                                   final String ncesEMail, final String isFinancialPenaltiesWrittenOff,
                                                                                   final String originalDateOfOffenceList, final String originalDateOfSentenceList,
                                                                                   final List<NewOffenceByResult> newResultByOffence, final NewApplicationResults applicationResults,
                                                                                   final Map<UUID, List<OffenceResultsDetails>> prevApplicationResultsDetails) {

        final List<UUID> offenceIdList = hearingFinancialResultRequest.getOffenceResults().stream().map(OffenceResults::getOffenceId).toList();
        final OldAccountCorrelationsWrapper correlationsWrapper = getOldAccountCorrelations(correlationItemList, hearingFinancialResultRequest.getAccountCorrelationId(), offenceIdList, prevApplicationResultsDetails);

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
                .withNewApplicationResults(applicationResults)
                .withCaseReferences(String.join(NCESDecisionConstants.COMMA, hearingFinancialResultRequest.getProsecutionCaseReferences()))
                .withMasterDefendantId(hearingFinancialResultRequest.getMasterDefendantId())
                .withDivisionCode(hearingFinancialResultRequest.getAccountDivisionCode())
                .withOldDivisionCode(correlationsWrapper.getOldDivisionCodes())
                .withImpositionOffenceDetails(impositionOffenceDetails)
                .withIsValidOldCorrelationAndAccount(isValidOldCorrelationAndAccount(correlationsWrapper.getOldAccountCorrelationsList()));

        if (isNull(hearingFinancialResultRequest.getAccountCorrelationId())) {
            builder.withAccountCorrelationId(correlationsWrapper.getRecentAccountCorrelationId());
            builder.withGobAccountNumber(correlationsWrapper.getOldGobAccounts());
        } else {
            builder.withAccountCorrelationId(hearingFinancialResultRequest.getAccountCorrelationId())

                    .withOldAccountCorrelationId(correlationsWrapper.getRecentAccountCorrelationId())
                    .withOldGobAccountNumber(correlationsWrapper.getOldGobAccounts());
        }

        ofNullable(hearingFinancialResultRequest.getHearingSittingDay())
                .ifPresent(a -> builder.withHearingSittingDay(a.format(ofPattern(HEARING_SITTING_DAY_PATTERN))));

        if (getApplicationGrantedSubjects().contains(subject) || getApplicationAppealAllowedSubjects().contains(subject)) {
            builder.withIsFinancialPenaltiesWrittenOff(isFinancialPenaltiesWrittenOff);
            builder.withOriginalDateOfOffence(originalDateOfOffenceList);
            builder.withOriginalDateOfSentence(originalDateOfSentenceList);
            builder.withNewOffenceByResult(newResultByOffence);
            hearingFinancialResultRequest.getOffenceResults().stream().filter(offence -> nonNull(offence.getDateOfResult())).findFirst().ifPresent(offence ->
                    builder.withDateDecisionMade(offence.getDateOfResult()));
        } else {
            buildDecisionMade(hearingFinancialResultRequest, builder);
        }
        return builder.build();
    }

    public MarkedAggregateSendEmailWhenAccountReceived buildMarkedAggregateWithoutOlds(final HearingFinancialResultRequest hearingFinancialResultRequest,
                                                                                       final String subject, final List<ImpositionOffenceDetails> impositionOffenceDetails,
                                                                                       final Boolean includeOldAccountInfoIfAvailable) {

        final List<UUID> offenceIdList = hearingFinancialResultRequest.getOffenceResults().stream().map(OffenceResults::getOffenceId).toList();
        final OldAccountCorrelationsWrapper correlationsWrapper = getOldAccountCorrelations(correlationItemList, hearingFinancialResultRequest.getAccountCorrelationId(), offenceIdList, emptyMap());

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
                .withImpositionOffenceDetails(impositionOffenceDetails)
                .withIsValidOldCorrelationAndAccount(isValidOldCorrelationAndAccount(correlationsWrapper.getOldAccountCorrelationsList()));

        if (Boolean.TRUE.equals(includeOldAccountInfoIfAvailable) && isNotEmpty(correlationsWrapper.getOldAccountCorrelationsList())) {
            builder.withGobAccountNumber(correlationsWrapper.getOldGobAccounts())
                    .withAccountCorrelationId(correlationsWrapper.getRecentAccountCorrelationId());
        }

        ofNullable(hearingFinancialResultRequest.getHearingSittingDay())
                .ifPresent(a -> builder.withHearingSittingDay(a.format(ofPattern(HEARING_SITTING_DAY_PATTERN))));

        if (WRITE_OFF_ONE_DAY_DEEMED_SERVED.equals(subject) || ACON_EMAIL_SUBJECT.equals(subject)) {
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

    @SuppressWarnings("java:S107")
    public MarkedAggregateSendEmailWhenAccountReceived buildMarkedAggregateWithOlds(final HearingFinancialResultRequest hearingFinancialResultRequest, final List<ImpositionOffenceDetails> impositionOffenceDetails,
                                                                                    final String applicationResult, final List<NewOffenceByResult> newResultByOffenceList,
                                                                                    final OriginalApplicationResults originalApplicationResults,
                                                                                    final NewApplicationResults newApplicationResults,
                                                                                    final String subject, final Map<UUID, List<OffenceResultsDetails>> prevApplicationResultsDetails) {

        final List<UUID> offenceIdList = hearingFinancialResultRequest.getOffenceResults().stream().map(OffenceResults::getOffenceId).toList();
        final OldAccountCorrelationsWrapper correlationsWrapper = getOldAccountCorrelations(correlationItemList, hearingFinancialResultRequest.getAccountCorrelationId(), offenceIdList, prevApplicationResultsDetails);

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
                .withDivisionCode(hearingFinancialResultRequest.getAccountDivisionCode())
                .withImpositionOffenceDetails(impositionOffenceDetails)
                .withIsSJPHearing(hearingFinancialResultRequest.getIsSJPHearing())
                .withNewOffenceByResult(newResultByOffenceList)
                .withOriginalApplicationResults(originalApplicationResults)
                .withNewApplicationResults(newApplicationResults)
                .withIsValidOldCorrelationAndAccount(isValidOldCorrelationAndAccount(correlationsWrapper.getOldAccountCorrelationsList()));

        if (isNotEmpty(correlationsWrapper.getOldAccountCorrelationsList())) {
            builder.withOldAccountCorrelationId(correlationsWrapper.getRecentAccountCorrelationId());
            builder.withOldGobAccountNumber(correlationsWrapper.getOldGobAccounts());
            builder.withOldDivisionCode(correlationsWrapper.getOldDivisionCodes());
        }

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

    /**
     * returns true when all the accountCorrelationItems have old AccountCorrelationId & old GobAccountNumber OR old AccountCorrelationId & old GobAccountNumber are null
     **/
    private Boolean isValidOldCorrelationAndAccount(final List<OldAccountCorrelation> oldAccountCorrelationsList) {
        return isEmpty(oldAccountCorrelationsList) || oldAccountCorrelationsList.stream()
                .allMatch(oac -> Objects.isNull(oac.getAccountCorrelationId()) == Objects.isNull(oac.getGobAccountNumber()));
    }

}
