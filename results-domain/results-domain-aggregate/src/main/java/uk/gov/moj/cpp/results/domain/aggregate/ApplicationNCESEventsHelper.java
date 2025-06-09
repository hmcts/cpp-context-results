package uk.gov.moj.cpp.results.domain.aggregate;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static uk.gov.moj.cpp.results.domain.aggregate.ImpositionOffenceDetailsBuilder.buildImpositionOffenceDetailsFromAggregate;
import static uk.gov.moj.cpp.results.domain.aggregate.ImpositionOffenceDetailsBuilder.buildImpositionOffenceDetailsFromRequest;
import static uk.gov.moj.cpp.results.domain.aggregate.MarkedAggregateSendEmailEventBuilder.markedAggregateSendEmailEventBuilder;
import static uk.gov.moj.cpp.results.domain.aggregate.NCESDecisionHelper.buildNewOffenceResultForSV;
import static uk.gov.moj.cpp.results.domain.aggregate.NCESDecisionHelper.buildOriginalOffenceResultForSV;
import static uk.gov.moj.cpp.results.domain.aggregate.NCESDecisionHelper.hasApplicationAmendmentDate;
import static uk.gov.moj.cpp.results.domain.aggregate.NCESDecisionHelper.hasApplicationResult;
import static uk.gov.moj.cpp.results.domain.aggregate.NCESDecisionHelper.hasNoCorrelationIdForAmendedApplication;
import static uk.gov.moj.cpp.results.domain.aggregate.NCESDecisionHelper.hasSentenceVaried;
import static uk.gov.moj.cpp.results.domain.aggregate.NCESDecisionHelper.shouldNotifyNCESForAppAppealResult;
import static uk.gov.moj.cpp.results.domain.aggregate.NCESDecisionHelper.shouldNotifyNCESForAppResultAmendment;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.ACON;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.APPLICATION_SUBJECT;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.APPLICATION_TYPES;

import uk.gov.justice.core.courts.CorrelationIdHistoryItem;
import uk.gov.justice.hearing.courts.HearingFinancialResultRequest;
import uk.gov.justice.hearing.courts.OffenceResults;
import uk.gov.justice.hearing.courts.OffenceResultsDetails;
import uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants;
import uk.gov.moj.cpp.results.domain.event.ImpositionOffenceDetails;
import uk.gov.moj.cpp.results.domain.event.MarkedAggregateSendEmailWhenAccountReceived;
import uk.gov.moj.cpp.results.domain.event.NewApplicationResults;
import uk.gov.moj.cpp.results.domain.event.NewOffenceByResult;
import uk.gov.moj.cpp.results.domain.event.OriginalApplicationResults;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Helper class for processing NCES events for applications.
 * <p>
 * This class provides methods for building and processing NCES events for applications based on the incoming hearing financial result request.
 * It contains methods for requesting NCES email notifications for rejected, granted, and updated applications, as well as for application amendment events.
 */
class ApplicationNCESEventsHelper {
    private final Map<UUID, OffenceResultsDetails> offenceResultsDetails = new HashMap<>();
    private final Map<UUID, List<OffenceResultsDetails>> applicationResultsDetails = new HashMap<>();
    private final Map<UUID, OffenceResultsDetails> applicationOffenceResultsDetails = new HashMap<>();
    private final String ncesEmail;
    private final LinkedList<CorrelationIdHistoryItem> correlationIdHistoryItemList = new LinkedList<>();

    private ApplicationNCESEventsHelper(Map<UUID, OffenceResultsDetails> offenceResultsDetails, Map<UUID, List<OffenceResultsDetails>> applicationResultsDetails,
                                        Map<UUID, OffenceResultsDetails> applicationOffenceResultsDetails, String ncesEmail, LinkedList<CorrelationIdHistoryItem> correlationIdHistoryItemList) {
        this.offenceResultsDetails.putAll(offenceResultsDetails);
        this.applicationResultsDetails.putAll(applicationResultsDetails);
        this.applicationOffenceResultsDetails.putAll(applicationOffenceResultsDetails);
        this.ncesEmail = ncesEmail;
        this.correlationIdHistoryItemList.addAll(correlationIdHistoryItemList);
    }

    static ApplicationNCESEventsHelper applicationNCESEventsHelper(Map<UUID, OffenceResultsDetails> offenceResultsDetails, Map<UUID, List<OffenceResultsDetails>> applicationResultsDetails,
                                                                   Map<UUID, OffenceResultsDetails> applicationOffencResultsDetails, String ncesEmail, LinkedList<CorrelationIdHistoryItem> correlationIdHistoryItemList) {
        return new ApplicationNCESEventsHelper(offenceResultsDetails, applicationResultsDetails, applicationOffencResultsDetails, ncesEmail, correlationIdHistoryItemList);
    }

    /**
     * Requests an NCES email notification for a rejected or granted application.
     * <p>
     * This method processes the given financial result request to find the first offence result that matches
     * the application types and subjects. If a matching offence result is found, it retrieves the imposition
     * offence details for the application and builds the original application results from the track request.
     * Depending on whether NCES should be notified for the application statdec, reopen, appeal result, it either processes the
     * appeal results or builds a marked aggregate without old correlation IDs and returns it.
     */
    Optional<MarkedAggregateSendEmailWhenAccountReceived> requestNcesEmailNotificationForRejectedOrGrantedApplication(final HearingFinancialResultRequest hearingFinancialResultRequest,
                                                                                                                      final String isWrittenOffExists,
                                                                                                                      final String originalDateOfOffenceList,
                                                                                                                      final String originalDateOfSentenceList,
                                                                                                                      final List<NewOffenceByResult> newResultByOffenceList,
                                                                                                                      final String applicationResult,
                                                                                                                      final Map<UUID, String> offenceDateMap) {
        final boolean notificationAlreadySent = hearingFinancialResultRequest.getOffenceResults().stream()
                .filter(result -> nonNull(result.getApplicationId()))
                .map(offenceFromRequest -> applicationResultsDetails.get(offenceFromRequest.getApplicationId()))
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .filter(offence -> APPLICATION_TYPES.containsKey(offence.getApplicationType()))
                .anyMatch(offence -> APPLICATION_SUBJECT.get(offence.getApplicationType()).containsKey(offence.getResultCode()));
        if (notificationAlreadySent) {
            return Optional.empty();
        }

        final Optional<OffenceResults> offenceForApplication = hearingFinancialResultRequest.getOffenceResults().stream()
                .filter(offence -> NCESDecisionConstants.APPLICATION_TYPES.containsKey(offence.getApplicationType()))
                .filter(offence -> NCESDecisionConstants.APPLICATION_SUBJECT.get(offence.getApplicationType()).containsKey(offence.getResultCode()))
                .findFirst();

        return offenceForApplication.map(offence -> {
            List<ImpositionOffenceDetails> impositionOffenceDetailsForApplication = getApplicationImpositionOffenceDetails(hearingFinancialResultRequest, offenceDateMap);
            if (!impositionOffenceDetailsForApplication.isEmpty()) {
                final Optional<OriginalApplicationResults> originalApplicationResults = buildApplicationResultsFromTrackRequest(hearingFinancialResultRequest.getOffenceResults());
                if (shouldNotifyNCESForAppAppealResult(hearingFinancialResultRequest)) {
                    return processAppealResults(hearingFinancialResultRequest, isWrittenOffExists, originalDateOfOffenceList, originalDateOfSentenceList, newResultByOffenceList, applicationResult, offence, impositionOffenceDetailsForApplication, originalApplicationResults);
                } else {
                    return Optional.of(markedAggregateSendEmailEventBuilder(ncesEmail, correlationIdHistoryItemList).buildMarkedAggregateWithoutOldsForSpecificCorrelationIdWithEmail(hearingFinancialResultRequest,
                            NCESDecisionConstants.APPLICATION_SUBJECT.get(offence.getApplicationType()).get(offence.getResultCode()),
                            correlationIdHistoryItemList.peekLast(), impositionOffenceDetailsForApplication, ncesEmail, isWrittenOffExists, originalDateOfOffenceList,
                            originalDateOfSentenceList, newResultByOffenceList, applicationResult, originalApplicationResults, Optional.empty()));
                }
            } else {
                return null;
            }
        }).orElse(Optional.empty());
    }

    /**
     * Requests an NCES email notification for an updated application.
     * <p>
     * This method processes the given financial result request to find the first offence result that matches
     * the application types. If a matching offence result is found, it retrieves the imposition offence details
     * for the application. If imposition offence details are found, it builds and returns a marked aggregate
     * without old correlation IDs for sending an email notification.
     */
    Optional<MarkedAggregateSendEmailWhenAccountReceived> requestNcesEmailNotificationForUpdatedApplication(final HearingFinancialResultRequest hearingFinancialResultRequest,
                                                                                                            final String isWrittenOffExists,
                                                                                                            final String originalDateOfOffenceList,
                                                                                                            final String originalDateOfSentenceList,
                                                                                                            final List<NewOffenceByResult> newResultByOffenceList,
                                                                                                            final String applicationResult,
                                                                                                            final Map<UUID, String> offenceDateMap) {
        final Optional<OffenceResults> offenceForApplication = hearingFinancialResultRequest.getOffenceResults().stream()
                .filter(offence -> NCESDecisionConstants.APPLICATION_TYPES.containsKey(offence.getApplicationType()))
                .findFirst();

        return offenceForApplication.map(offence -> {
            List<ImpositionOffenceDetails> impositionOffenceDetailsForApplication = getApplicationImpositionOffenceDetails(hearingFinancialResultRequest, offenceDateMap);
            if (!impositionOffenceDetailsForApplication.isEmpty()) {
                return Optional.of(markedAggregateSendEmailEventBuilder(ncesEmail, correlationIdHistoryItemList).buildMarkedAggregateWithoutOldsForSpecificCorrelationIdWithEmail(hearingFinancialResultRequest,
                        NCESDecisionConstants.APPLICATION_UPDATED_SUBJECT.get(offence.getApplicationType()),
                        correlationIdHistoryItemList.peekLast(), impositionOffenceDetailsForApplication, ncesEmail, isWrittenOffExists, originalDateOfOffenceList,
                        originalDateOfSentenceList, newResultByOffenceList, applicationResult, Optional.empty(), Optional.empty()));
            } else {
                return null;
            }
        }).orElse(Optional.empty());
    }

    /**
     * Appends financial result events for applications based on the incoming hearing financial result request.
     * <p>
     * This method processes the hearing financial result request to determine whether to append application amendment events
     * or deemed served events and ACON events. It builds a new `HearingFinancialResultRequest` object with values from the
     * original request and filters out offence results without application types.
     * <p>
     * Depending on whether the request is part of an application amendment process, it either appends application amendment
     * events or builds deemed served events and ACON events for the application.
     * <p>
     * The method ensures that the appropriate financial result events for applications are added to the list of marked events.
     *
     * @return
     */
    @SuppressWarnings("java:S107")
    List<MarkedAggregateSendEmailWhenAccountReceived> buildFinancialResultEventsForApplication(final HearingFinancialResultRequest hearingFinancialResultRequest,
                                                                                               final String isWrittenOffExists,
                                                                                               final String originalDateOfOffenceList,
                                                                                               final String originalDateOfSentenceList,
                                                                                               final List<NewOffenceByResult> newResultByOffenceList,
                                                                                               final String applicationResult, final Map<UUID, String> offenceDateMap) {

        final List<MarkedAggregateSendEmailWhenAccountReceived> markedEvents = new ArrayList<>();
        final boolean hasApplicationResult = hasApplicationResult(hearingFinancialResultRequest);

        final HearingFinancialResultRequest hfRequest = HearingFinancialResultRequest.hearingFinancialResultRequest()
                .withValuesFrom(hearingFinancialResultRequest)
                .withOffenceResults(new ArrayList<>(hearingFinancialResultRequest.getOffenceResults())).build();

        hfRequest.getOffenceResults().removeIf(result -> isNull(result.getApplicationType()));

        if (hearingFinancialResultRequest.getOffenceResults().stream()
                .anyMatch(offence -> NCESDecisionConstants.APPLICATION_TYPES.containsKey(offence.getApplicationType()))) {
            if (isApplicationAmendmentProcess(hfRequest, hasApplicationResult)) {
                final Optional<NewApplicationResults> newApplicationResults = buildNewApplicationResultsFromTrackRequest(hearingFinancialResultRequest.getOffenceResults());
                appendApplicationAmendmentEvents(hfRequest, markedEvents, isWrittenOffExists, originalDateOfOffenceList,
                        originalDateOfSentenceList, newResultByOffenceList, applicationResult, offenceDateMap, newApplicationResults);
            } else {
                buildApplicationDeemedServedEvents(hfRequest, markedEvents, offenceDateMap);
                buildApplicationAconEvents(hfRequest, markedEvents, offenceDateMap);
            }
        }
        return markedEvents;
    }

    /**
     * Appends amendment events for applications based on the incoming hearing financial result request.
     * <p>
     * This method processes the hearing financial result request to build imposition offence details for financial and non-financial offences.
     * It filters and maps the offence results to create lists of imposition offence details, and then appends the appropriate amendment events
     * to the list of marked events.
     * <p>
     * The method also builds original application results and handles the creation of amended non-financial results, amended application results,
     * amended financial results, deemed served events, and ACON events for the application.
     * <p>
     * The method ensures that the correct imposition offence details and application results are added to the marked events list.
     */
    @SuppressWarnings("java:S107")
    private void appendApplicationAmendmentEvents(final HearingFinancialResultRequest hearingFinancialResultRequest,
                                                  final List<MarkedAggregateSendEmailWhenAccountReceived> markedEvents,
                                                  final String isWrittenOffExists,
                                                  final String originalDateOfOffenceList,
                                                  final String originalDateOfSentenceList,
                                                  final List<NewOffenceByResult> newResultByOffenceList,
                                                  final String applicationResult, final Map<UUID, String> offenceDateMap,
                                                  final Optional<NewApplicationResults> newResultsByApplication) {
        // Get original imposition offence details from the aggregate.
        final List<ImpositionOffenceDetails> originalImpositionDetails = hearingFinancialResultRequest.getOffenceResults().stream()
                .filter(result -> nonNull(result.getApplicationType()))
                .map(offenceFromRequest -> applicationOffenceResultsDetails.get(offenceFromRequest.getOffenceId()))
                .filter(Objects::nonNull)
                .map(offenceResults -> buildImpositionOffenceDetailsFromAggregate(offenceResults, offenceDateMap)).distinct()
                .toList();

        // Get imposition offence details for financial offences that are corresponding to original financial offences
        final List<ImpositionOffenceDetails> impositionOffenceDetailsForFinancial = new ArrayList<>();
        for (OffenceResults results : hearingFinancialResultRequest.getOffenceResults()) {
            if (nonNull(results.getApplicationType()) && results.getIsFinancial() &&
                    nonNull(results.getAmendmentDate()) && ofNullable(applicationOffenceResultsDetails.get(results.getOffenceId())).map(OffenceResultsDetails::getIsFinancial).orElse(false)) {
                ImpositionOffenceDetails impositionOffenceDetails = buildImpositionOffenceDetailsFromRequest(results, offenceDateMap);
                impositionOffenceDetailsForFinancial.add(impositionOffenceDetails);
            }
        }

        // Get imposition offence details for non-financial offences corresponding to original financial offences
        final List<ImpositionOffenceDetails> impositionOffenceDetailsForNonFinancial = hearingFinancialResultRequest.getOffenceResults().stream()
                .filter(o -> nonNull(o.getApplicationType()))
                .filter(o -> !o.getIsFinancial())
                .filter(o -> Objects.nonNull(o.getAmendmentDate()))
                .filter(offenceFromRequest -> ofNullable(applicationOffenceResultsDetails.get(offenceFromRequest.getOffenceId())).map(OffenceResultsDetails::getIsFinancial).orElse(false))
                .map(offenceResults -> buildImpositionOffenceDetailsFromRequest(offenceResults, offenceDateMap))
                .toList();


        // Get original application results from the aggregate.
        final Optional<OriginalApplicationResults> originalResultsByApplication = hearingFinancialResultRequest.getOffenceResults().stream()
                .filter(result -> Objects.nonNull(result.getApplicationId()))
                .map(offenceFromRequest -> applicationResultsDetails.get(offenceFromRequest.getApplicationId()))
                .filter(Objects::nonNull)
                .map(this::buildOriginalApplicationResultsFromAggregate)
                .findFirst();

        buildAmendedApplicationResultsEvents(hearingFinancialResultRequest, markedEvents, isWrittenOffExists, originalDateOfOffenceList, originalDateOfSentenceList,
                newResultByOffenceList, applicationResult, newResultsByApplication, originalImpositionDetails, impositionOffenceDetailsForFinancial,
                impositionOffenceDetailsForNonFinancial, originalResultsByApplication);

        buildAmendedApplicationOffenceResultsEvents(hearingFinancialResultRequest, markedEvents, isWrittenOffExists, originalDateOfOffenceList,
                originalDateOfSentenceList, newResultByOffenceList, applicationResult, newResultsByApplication, originalImpositionDetails,
                impositionOffenceDetailsForFinancial, impositionOffenceDetailsForNonFinancial, originalResultsByApplication);

        buildApplicationDeemedServedEvents(hearingFinancialResultRequest, markedEvents, offenceDateMap);
        buildAmendAconEvents(hearingFinancialResultRequest, markedEvents, offenceDateMap);

    }

    /**
     * Builds amended application results events for non-fine offences.
     * <p>
     * This method processes the given financial result request and updates the list of marked events
     * based on the presence of financial and non-financial imposition offence details. If non-financial
     * imposition details are present, they are either added to the financial imposition details or used
     * to build a new marked event without old correlation IDs.
     */
    @SuppressWarnings("java:S107")
    private void buildAmendedApplicationOffenceResultsEvents(final HearingFinancialResultRequest hearingFinancialResultRequest, final List<MarkedAggregateSendEmailWhenAccountReceived> markedEvents,
                                                             final String isWrittenOffExists, final String originalDateOfOffenceList, final String originalDateOfSentenceList, final List<NewOffenceByResult> newResultByOffenceList,
                                                             final String applicationResult, final Optional<NewApplicationResults> newResultsByApplication, final List<ImpositionOffenceDetails> originalImpositionDetails,
                                                             final List<ImpositionOffenceDetails> impositionOffenceDetailsForFinancial, final List<ImpositionOffenceDetails> impositionOffenceDetailsForNonFinancial,
                                                             final Optional<OriginalApplicationResults> originalResultsByApplication) {
        if (!impositionOffenceDetailsForNonFinancial.isEmpty()) {
            if (!impositionOffenceDetailsForFinancial.isEmpty()) {
                impositionOffenceDetailsForFinancial.addAll(impositionOffenceDetailsForNonFinancial);
            } else {
                markedEvents.add(markedAggregateSendEmailEventBuilder(ncesEmail, correlationIdHistoryItemList).buildMarkedAggregateWithoutOldsForSpecificCorrelationId(hearingFinancialResultRequest, NCESDecisionConstants.AMEND_AND_RESHARE, correlationIdHistoryItemList.peekLast(), originalImpositionDetails,
                        isWrittenOffExists, originalDateOfOffenceList, originalDateOfSentenceList, newResultByOffenceList, applicationResult,
                        originalResultsByApplication, newResultsByApplication));
            }
        }

        if (!impositionOffenceDetailsForFinancial.isEmpty()) {
            if (hasSentenceVaried(newResultByOffenceList)) {
                markedEvents.add(markedAggregateSendEmailEventBuilder(ncesEmail, correlationIdHistoryItemList).buildMarkedAggregateWithOlds(hearingFinancialResultRequest,
                        buildOriginalOffenceResultForSV(originalImpositionDetails), applicationResult,
                        buildNewOffenceResultForSV(newResultByOffenceList),
                        originalResultsByApplication, newResultsByApplication,
                        NCESDecisionConstants.AMEND_AND_RESHARE));
            } else {
                markedEvents.add(markedAggregateSendEmailEventBuilder(ncesEmail, correlationIdHistoryItemList).buildMarkedAggregateWithOlds(hearingFinancialResultRequest, originalImpositionDetails,
                        applicationResult, newResultByOffenceList, originalResultsByApplication, newResultsByApplication,
                        NCESDecisionConstants.AMEND_AND_RESHARE));
            }
        }
    }

    @SuppressWarnings("java:S107")
    private void buildAmendedApplicationResultsEvents(final HearingFinancialResultRequest hearingFinancialResultRequest, final List<MarkedAggregateSendEmailWhenAccountReceived> markedEvents,
                                                      final String isWrittenOffExists, final String originalDateOfOffenceList, final String originalDateOfSentenceList, final List<NewOffenceByResult> newResultByOffenceList,
                                                      final String applicationResult, final Optional<NewApplicationResults> newResultsByApplication, final List<ImpositionOffenceDetails> originalImpositionDetails,
                                                      final List<ImpositionOffenceDetails> impositionOffenceDetailsForFinancial, final List<ImpositionOffenceDetails> impositionOffenceDetailsForNonFinancial,
                                                      final Optional<OriginalApplicationResults> originalResultsByApplication) {
        // If original and new application results are present, and there are no imposition offence details, create a new marked event.
        if (originalResultsByApplication.isPresent() &&
                newResultsByApplication.isPresent() &&
                impositionOffenceDetailsForNonFinancial.isEmpty() &&
                impositionOffenceDetailsForFinancial.isEmpty() &&
                shouldNotifyNCESForAppResultAmendment(hearingFinancialResultRequest)) {
            markedEvents.add(markedAggregateSendEmailEventBuilder(ncesEmail, correlationIdHistoryItemList).buildMarkedAggregateWithoutOldsForSpecificCorrelationId(hearingFinancialResultRequest,
                    NCESDecisionConstants.AMEND_AND_RESHARE, correlationIdHistoryItemList.peekLast(), originalImpositionDetails,
                    isWrittenOffExists, originalDateOfOffenceList, originalDateOfSentenceList, newResultByOffenceList, applicationResult,
                    originalResultsByApplication, newResultsByApplication));
        }
    }

    /**
     * Appends deemed served events for applications based on the hearing financial result request.
     */
    private void buildApplicationDeemedServedEvents(final HearingFinancialResultRequest hearingFinancialResultRequest,
                                                    final List<MarkedAggregateSendEmailWhenAccountReceived> markedEvents, final Map<UUID, String> offenceDateMap) {
        // Get imposition offence details for deemed served offences.
        final List<ImpositionOffenceDetails> impositionOffenceDetailsForDeemed = hearingFinancialResultRequest.getOffenceResults().stream()
                .filter(o -> nonNull(o.getApplicationType()))
                .filter(o -> nonNull(o.getIsParentFlag()) && o.getIsParentFlag())
                .filter(OffenceResults::getIsDeemedServed)
                .filter(o -> nonNull(o.getImpositionOffenceDetails()))
                .map(offenceResults -> buildImpositionOffenceDetailsFromRequest(offenceResults, offenceDateMap)).distinct()
                .toList();
        // If there are deemed served imposition offence details, create a new marked event.
        if (!impositionOffenceDetailsForDeemed.isEmpty()) {
            if (hasNoCorrelationIdForAmendedApplication(hearingFinancialResultRequest)) {
                markedEvents.add(markedAggregateSendEmailEventBuilder(ncesEmail, correlationIdHistoryItemList).buildMarkedAggregateWithoutOlds(hearingFinancialResultRequest, NCESDecisionConstants.WRITE_OFF_ONE_DAY_DEEMED_SERVED, impositionOffenceDetailsForDeemed, Boolean.TRUE));
            } else {
                markedEvents.add(markedAggregateSendEmailEventBuilder(ncesEmail, correlationIdHistoryItemList).buildMarkedAggregateWithoutOlds(hearingFinancialResultRequest, NCESDecisionConstants.WRITE_OFF_ONE_DAY_DEEMED_SERVED, impositionOffenceDetailsForDeemed, Boolean.FALSE));
            }
        }
    }

    private void buildAmendAconEvents(final HearingFinancialResultRequest hearingFinancialResultRequest, final List<MarkedAggregateSendEmailWhenAccountReceived> markedEvents, final Map<UUID, String> offenceDateMap) {
        final List<ImpositionOffenceDetails> impositionOffenceDetailsForACON = hearingFinancialResultRequest.getOffenceResults().stream()
                .filter(o -> nonNull(o.getApplicationType()))
                .filter(o -> Boolean.TRUE.equals(o.getIsParentFlag()) && o.getIsFinancial()
                        && nonNull(o.getImpositionOffenceDetails()))
                .filter(offence -> ACON.equals(offence.getResultCode()))
                .filter(offence -> Objects.nonNull(offence.getAmendmentDate()))
                .map(offenceResults -> buildImpositionOffenceDetailsFromRequest(offenceResults, offenceDateMap)).distinct()
                .toList();
        if (!impositionOffenceDetailsForACON.isEmpty()) {
            markedEvents.add(markedAggregateSendEmailEventBuilder(ncesEmail, correlationIdHistoryItemList).buildMarkedAggregateWithoutOlds(hearingFinancialResultRequest, NCESDecisionConstants.ACON_EMAIL_SUBJECT, impositionOffenceDetailsForACON, Boolean.FALSE));
        }
    }

    /**
     * Appends ACON events for applications based on the hearing financial result request.
     */
    private void buildApplicationAconEvents(final HearingFinancialResultRequest hearingFinancialResultRequest,
                                            final List<MarkedAggregateSendEmailWhenAccountReceived> markedEvents, final Map<UUID, String> offenceDateMap) {
        // Get imposition offence details for ACON offences.
        final List<ImpositionOffenceDetails> impositionOffenceDetailsForAcon = hearingFinancialResultRequest.getOffenceResults().stream()
                .filter(o -> nonNull(o.getApplicationType()) &&
                        Boolean.TRUE.equals(o.getIsParentFlag()) &&
                        nonNull(o.getImpositionOffenceDetails()) && o.getImpositionOffenceDetails().contains(ACON))
                .filter(OffenceResults::getIsFinancial)
                .map(offenceResults -> buildImpositionOffenceDetailsFromRequest(offenceResults, offenceDateMap)).distinct()
                .toList();
        // If there are ACON imposition offence details, create a new marked event.
        if (!impositionOffenceDetailsForAcon.isEmpty()) {
            markedEvents.add(markedAggregateSendEmailEventBuilder(ncesEmail, correlationIdHistoryItemList).buildMarkedAggregateWithoutOlds(hearingFinancialResultRequest, NCESDecisionConstants.ACON_EMAIL_SUBJECT, impositionOffenceDetailsForAcon, Boolean.FALSE));
        }
    }

    private OriginalApplicationResults buildOriginalApplicationResultsFromAggregate(final List<OffenceResultsDetails> applicationResultsDetails) {
        List<String> applicationResult = new ArrayList<>();
        applicationResultsDetails.forEach(applicationResultDetail -> {
            if (!applicationResult.contains(applicationResultDetail.getApplicationResultType())) {
                applicationResult.add(applicationResultDetail.getApplicationResultType());
            }
        });
        return OriginalApplicationResults.originalApplicationResults()
                .withApplicationTitle(applicationResultsDetails.get(0).getApplicationTitle())
                .withApplicationResult(applicationResult)
                .build();
    }

    private Optional<NewApplicationResults> buildNewApplicationResultsFromTrackRequest(final List<OffenceResults> applicationResultsDetails) {
        List<String> applicationResults = new ArrayList<>();
        NewApplicationResults.Builder newApplicationResults = new NewApplicationResults.Builder();
        applicationResultsDetails.forEach(applicationResultDetail -> {
            if (!applicationResults.contains(applicationResultDetail.getApplicationResultType())
                    && nonNull(applicationResultDetail.getApplicationId())
                    && nonNull(applicationResultDetail.getApplicationTitle())) {
                applicationResults.add(applicationResultDetail.getApplicationResultType());
                newApplicationResults.withApplicationTitle(applicationResultDetail.getApplicationTitle());
            }
        });
        newApplicationResults.withApplicationResult(applicationResults);
        return Optional.of(newApplicationResults.build());
    }

    private Optional<OriginalApplicationResults> buildApplicationResultsFromTrackRequest(final List<OffenceResults> applicationResultsDetails) {
        List<String> applicationResults = new ArrayList<>();
        OriginalApplicationResults.Builder originalApplicationResults = new OriginalApplicationResults.Builder();
        applicationResultsDetails.forEach(applicationResultDetail -> {
            if (!applicationResults.contains(applicationResultDetail.getApplicationResultType())
                    && nonNull(applicationResultDetail.getApplicationId())
                    && nonNull(applicationResultDetail.getApplicationTitle())) {
                applicationResults.add(applicationResultDetail.getApplicationResultType());
                originalApplicationResults.withApplicationTitle(applicationResultDetail.getApplicationTitle());
            }
        });
        originalApplicationResults.withApplicationResult(applicationResults);
        return Optional.of(originalApplicationResults.build());
    }

    private boolean isApplicationAmendmentProcess(final HearingFinancialResultRequest hearingFinancialResultRequest, final boolean hasApplicationResult) {
        return hasApplicationAmendmentDate(hearingFinancialResultRequest)
                || (hasApplicationResult && hasDeemedServedChangedForApplication(hearingFinancialResultRequest));
    }

    private boolean hasDeemedServedChangedForApplication(final HearingFinancialResultRequest hearingFinancialResultRequest) {
        return hearingFinancialResultRequest.getOffenceResults().stream()
                .filter(offenceFromResult -> Boolean.TRUE.equals(offenceFromResult.getIsParentFlag())
                        && Boolean.TRUE.equals(offenceFromResult.getIsFinancial())
                        && offenceFromResult.getImpositionOffenceDetails() != null)
                .anyMatch(offenceFromResult ->
                        Optional.ofNullable(applicationOffenceResultsDetails.get(offenceFromResult.getOffenceId()))
                                .map(applicationFromAggregate -> !Objects.equals(applicationFromAggregate.getIsDeemedServed(), offenceFromResult.getIsDeemedServed()))
                                .orElse(false)
                );
    }

    private List<ImpositionOffenceDetails> getApplicationImpositionOffenceDetails(final HearingFinancialResultRequest hearingFinancialResultRequest, final Map<UUID, String> offenceDateMap) {
        List<ImpositionOffenceDetails> impositionOffenceDetailsForApplication = hearingFinancialResultRequest.getOffenceResults().stream()
                .filter(result -> nonNull(result.getApplicationType()))
                .map(offenceFromRequest -> offenceResultsDetails.get(offenceFromRequest.getOffenceId()))
                .filter(Objects::nonNull)
                .filter(OffenceResultsDetails::getIsFinancial)
                .map(offenceResults -> buildImpositionOffenceDetailsFromAggregate(offenceResults, offenceDateMap)).distinct()
                .toList();

        if (impositionOffenceDetailsForApplication.isEmpty()) {
            impositionOffenceDetailsForApplication = hearingFinancialResultRequest.getOffenceResults().stream()
                    .filter(result -> nonNull(result.getApplicationType()))
                    .map(offenceFromRequest -> applicationOffenceResultsDetails.get(offenceFromRequest.getOffenceId()))
                    .filter(Objects::nonNull)
                    .filter(OffenceResultsDetails::getIsParentFlag)
                    .filter(OffenceResultsDetails::getIsFinancial)
                    .map(offenceResults -> buildImpositionOffenceDetailsFromAggregate(offenceResults, offenceDateMap)).distinct()
                    .toList();
        }
        return impositionOffenceDetailsForApplication;
    }

    @SuppressWarnings("java:S107")
    private Optional<MarkedAggregateSendEmailWhenAccountReceived> processAppealResults(final HearingFinancialResultRequest hearingFinancialResultRequest, final String isWrittenOffExists, final String originalDateOfOffenceList, final String originalDateOfSentenceList, final List<NewOffenceByResult> newResultByOffenceList, final String applicationResult, final OffenceResults offence, final List<ImpositionOffenceDetails> impositionOffenceDetailsForApplication, final Optional<OriginalApplicationResults> originalApplicationResults) {
        if (hasSentenceVaried(newResultByOffenceList)) {
            return Optional.of(markedAggregateSendEmailEventBuilder(ncesEmail, correlationIdHistoryItemList).buildMarkedAggregateWithOlds(hearingFinancialResultRequest,
                    impositionOffenceDetailsForApplication, applicationResult, buildNewOffenceResultForSV(newResultByOffenceList),
                    originalApplicationResults, Optional.empty(),
                    NCESDecisionConstants.APPLICATION_SUBJECT.get(offence.getApplicationType()).get(offence.getResultCode())));
        } else {
            return Optional.of(markedAggregateSendEmailEventBuilder(ncesEmail, correlationIdHistoryItemList).buildMarkedAggregateWithoutOldsForSpecificCorrelationIdWithEmail(hearingFinancialResultRequest,
                    NCESDecisionConstants.APPLICATION_SUBJECT.get(offence.getApplicationType()).get(offence.getResultCode()),
                    correlationIdHistoryItemList.peekLast(), impositionOffenceDetailsForApplication, ncesEmail, isWrittenOffExists, originalDateOfOffenceList,
                    originalDateOfSentenceList, newResultByOffenceList, applicationResult, originalApplicationResults, Optional.empty()));
        }
    }
}
