package uk.gov.moj.cpp.results.domain.aggregate;

import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Arrays.asList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.builder;
import static java.util.stream.Stream.empty;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.core.courts.HearingFinancialResultsUpdated.hearingFinancialResultsUpdated;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;
import static uk.gov.justice.hearing.courts.OffenceResultsDetails.offenceResultsDetails;
import static uk.gov.moj.cpp.results.domain.aggregate.ApplicationNCESEventsHelper.applicationNCESEventsHelper;
import static uk.gov.moj.cpp.results.domain.aggregate.MarkedAggregateSendEmailEventBuilder.markedAggregateSendEmailEventBuilder;
import static uk.gov.moj.cpp.results.domain.aggregate.NCESDecisionHelper.buildApplicationResultsDetailsFromOffenceResults;
import static uk.gov.moj.cpp.results.domain.aggregate.NCESDecisionHelper.buildNewApplicationOffenceResultsFromTrackRequest;
import static uk.gov.moj.cpp.results.domain.aggregate.NCESDecisionHelper.hasAmendmentDate;
import static uk.gov.moj.cpp.results.domain.aggregate.NCESDecisionHelper.hasApplicationResult;
import static uk.gov.moj.cpp.results.domain.aggregate.NCESDecisionHelper.hasCorrelationId;
import static uk.gov.moj.cpp.results.domain.aggregate.NCESDecisionHelper.hasNoCorrelationIdForAmendedApplication;
import static uk.gov.moj.cpp.results.domain.aggregate.NCESDecisionHelper.hasResultOtherThanApplication;
import static uk.gov.moj.cpp.results.domain.aggregate.NCESDecisionHelper.isNewApplication;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.ACON;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.AMEND_AND_RESHARE;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.APPLICATION_SUBJECT;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.APPLICATION_TYPES;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.APPLICATION_UPDATED_SUBJECT;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.WRITE_OFF_ONE_DAY_DEEMED_SERVED;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.getApplicationAppealAllowedSubjects;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.getApplicationAppealSubjects;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.getApplicationGrantedSubjects;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.getApplicationNonGrantedSubjects;
import static uk.gov.moj.cpp.results.domain.event.MarkedAggregateSendEmailWhenAccountReceived.markedAggregateSendEmailWhenAccountReceived;
import static uk.gov.moj.cpp.results.domain.event.NcesEmailNotificationRequested.ncesEmailNotificationRequested;
import static uk.gov.moj.cpp.results.domain.event.SendNcesEmailNotFound.sendNcesEmailNotFound;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.CorrelationIdHistoryItem;
import uk.gov.justice.core.courts.DefendantAddressUpdatedFromApplication;
import uk.gov.justice.core.courts.HearingFinancialResultsUpdated;
import uk.gov.justice.core.courts.MasterDefendant;
import uk.gov.justice.core.courts.UnmarkedAggregateSendEmailWhenAccountReceived;
import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.justice.hearing.courts.HearingFinancialResultRequest;
import uk.gov.justice.hearing.courts.HearingFinancialResultsTracked;
import uk.gov.justice.hearing.courts.OffenceResults;
import uk.gov.justice.hearing.courts.OffenceResultsDetails;
import uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants;
import uk.gov.moj.cpp.results.domain.event.ImpositionOffenceDetails;
import uk.gov.moj.cpp.results.domain.event.MarkedAggregateSendEmailWhenAccountReceived;
import uk.gov.moj.cpp.results.domain.event.NcesEmailNotification;
import uk.gov.moj.cpp.results.domain.event.NcesEmailNotificationRequested;
import uk.gov.moj.cpp.results.domain.event.NewOffenceByResult;
import uk.gov.moj.cpp.results.domain.event.SendNcesEmailNotFound;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;


@SuppressWarnings({"PMD.BeanMembersShouldSerialize"})
public class HearingFinancialResultsAggregate implements Aggregate {

    private static final Logger LOGGER = getLogger(HearingFinancialResultsAggregate.class);
    private static final long serialVersionUID = -7417688645983920686L;
    private static final String HEARING_SITTING_DAY_PATTERN = "yyyy-MM-dd";
    public static final String EMPTY_STRING = "";
    public static final String BRITISH_DATE_FORMAT = "dd/MM/yyyy";
    private static final String AMENDMENT_REASON = "Admin error on shared result (a result recorded incorrectly)";

    private UUID hearingId;
    private ZonedDateTime hearingSittingDay;
    private String hearingCourtCentreName;
    private String defendantName;
    private String defendantDateOfBirth;
    private String defendantAddress;
    private String defendantEmail;
    private String defendantContactNumber;
    private Boolean isSJPHearing;
    private UUID masterDefendantId;
    private String ncesEmail;
    private List<String> prosecutionCaseReferences;
    private UUID initialHearingId;

    private final LinkedList<CorrelationIdHistoryItem> correlationIdHistoryItemList = new LinkedList<>();
    private final Map<UUID, OffenceResultsDetails> offenceResultsDetails = new HashMap<>();
    private final List<MarkedAggregateSendEmailWhenAccountReceived> markedAggregateSendEmailWhenAccountReceivedList = new ArrayList<>();
    private final Map<UUID, List<OffenceResultsDetails>> applicationResultsDetails = new HashMap<>();
    private final Map<UUID, OffenceResultsDetails> applicationOffencResultsDetails = new HashMap<>();
    private final Map<UUID, OffenceResultsDetails> applicationOffenceACONDetails = new HashMap<>();

    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(HearingFinancialResultsTracked.class).apply(this::handleFinancialResultsRequest),
                when(HearingFinancialResultsUpdated.class).apply(this::handleHearingFinancialResultsUpdated),
                when(MarkedAggregateSendEmailWhenAccountReceived.class).apply(this::handleMarkedAggregateSendEmailWhenAccountReceived),
                when(UnmarkedAggregateSendEmailWhenAccountReceived.class).apply(this::handleUnmarkedAggregateSendEmailWhenAccountReceived),
                when(DefendantAddressUpdatedFromApplication.class).apply(this::handleUpdateDefendantAddress),
                otherwiseDoNothing());
    }

    private void handleUpdateDefendantAddress(final DefendantAddressUpdatedFromApplication defendantAddressUpdatedFromApplication) {
        this.defendantAddress = defendantAddressUpdatedFromApplication.getDefendantAddress();
    }

    private void handleUnmarkedAggregateSendEmailWhenAccountReceived(final UnmarkedAggregateSendEmailWhenAccountReceived unmarkedAggregateSendEmailWhenAccountReceived) {
        markedAggregateSendEmailWhenAccountReceivedList.removeIf(marked -> marked.getId().equals(unmarkedAggregateSendEmailWhenAccountReceived.getId()));
    }

    private void handleMarkedAggregateSendEmailWhenAccountReceived(final MarkedAggregateSendEmailWhenAccountReceived markedAggregateSendEmailWhenAccountReceived) {
        markedAggregateSendEmailWhenAccountReceivedList.add(markedAggregateSendEmailWhenAccountReceived);
    }

    private void handleHearingFinancialResultsUpdated(final HearingFinancialResultsUpdated hearingFinancialResultsUpdated) {
        correlationIdHistoryItemList.stream()
                .filter(correlationIdHistoryItem -> hearingFinancialResultsUpdated.getCorrelationId().equals(correlationIdHistoryItem.getAccountCorrelationId()))
                .forEach(correlationIdHistoryItem -> {
                    final CorrelationIdHistoryItem.Builder correlationIdHistoryItemBuilder = CorrelationIdHistoryItem.correlationIdHistoryItem().withValuesFrom(correlationIdHistoryItem);
                    correlationIdHistoryItemList.set(correlationIdHistoryItemList.indexOf(correlationIdHistoryItem), correlationIdHistoryItemBuilder.withAccountNumber(hearingFinancialResultsUpdated.getAccountNumber()).build());
                });
    }

    public Stream<Object> updateFinancialResults(final HearingFinancialResultRequest hearingFinancialResultRequest,
                                                 final String isWrittenOffExists,
                                                 final String originalDateOfOffenceList,
                                                 final String originalDateOfSentenceList,
                                                 final List<NewOffenceByResult> newResultByOffenceList,
                                                 final String applicationResult) {
        return this.updateFinancialResults(hearingFinancialResultRequest, isWrittenOffExists, originalDateOfOffenceList,
                originalDateOfSentenceList, newResultByOffenceList, applicationResult, new HashMap<>());
    }

    /**
     * Updates the financial results based on the incoming hearing financial result request.
     * <p>
     * This method processes the financial results from the given hearing financial result request.
     * It builds a stream of updated financial results, including handling new applications,
     * requesting email notifications for
     * 1. rejected or granted applications (for the pre defined APPLICATION_TYPES and its associated ResultCodes in APPLICATION_SUBJECT),
     * 2. updated notification ( if the application is resulted other than the associated ResultCodes in APPLICATION_SUBJECT ex: Adjournment)
     * 3. processing amended financial results for cases and applications.
     * <p>
     * The method also filters and processes marked events for NCES application mail, ensuring
     * that the appropriate notifications are built and added to the stream of results.
     *
     * @return a stream of updated financial results.
     */
    public Stream<Object> updateFinancialResults(final HearingFinancialResultRequest hearingFinancialResultRequest,
                                                 final String isWrittenOffExists,
                                                 final String originalDateOfOffenceList,
                                                 final String originalDateOfSentenceList,
                                                 final List<NewOffenceByResult> newResultByOffenceList,
                                                 final String applicationResult,
                                                 final Map<UUID, String> offenceDateMap) {
        final Stream.Builder<Object> builder = Stream.builder();
        final List<MarkedAggregateSendEmailWhenAccountReceived> markedEvents = new ArrayList<>();
        final List<NewOffenceByResult> newApplicationOffenceResults = buildNewApplicationOffenceResultsFromTrackRequest(hearingFinancialResultRequest.getOffenceResults(), offenceDateMap);

        final boolean hasApplicationResult = hasApplicationResult(hearingFinancialResultRequest);

        if (hasApplicationResult && isNewApplication(hearingFinancialResultRequest)) {
            final ApplicationNCESEventsHelper applicationNCESEventsHelper = applicationNCESEventsHelper(offenceResultsDetails, applicationResultsDetails, applicationOffencResultsDetails, ncesEmail, correlationIdHistoryItemList);
            if (hearingFinancialResultRequest.getOffenceResults().stream()
                    .filter(offence -> APPLICATION_TYPES.containsKey(offence.getApplicationType()))
                    .anyMatch(offence -> APPLICATION_SUBJECT.get(offence.getApplicationType()).containsKey(offence.getResultCode()))) {
                applicationNCESEventsHelper.requestNcesEmailNotificationForRejectedOrGrantedApplication(hearingFinancialResultRequest, isWrittenOffExists, originalDateOfOffenceList,
                        originalDateOfSentenceList, newApplicationOffenceResults, applicationResult, offenceDateMap).ifPresent(markedEvents::add);
            } else {
                applicationNCESEventsHelper.requestNcesEmailNotificationForUpdatedApplication(hearingFinancialResultRequest, isWrittenOffExists, originalDateOfOffenceList,
                        originalDateOfSentenceList, newResultByOffenceList, applicationResult, offenceDateMap).ifPresent(markedEvents::add);
            }
        }
        processCaseAndApplicationAmendedFinancialResults(hearingFinancialResultRequest, isWrittenOffExists, originalDateOfOffenceList,
                originalDateOfSentenceList, newApplicationOffenceResults, applicationResult, offenceDateMap, builder, markedEvents,
                hasApplicationResult);
        processTrackedEvent(hearingFinancialResultRequest, builder);
        markedEvents.stream()
                .filter(event -> Objects.isNull(event.getAccountCorrelationId()) == Objects.isNull(event.getGobAccountNumber()))
                .filter(event -> Objects.isNull(event.getOldAccountCorrelationId()) == Objects.isNull(event.getOldGobAccountNumber()))
                .toList()
                .forEach(e -> {
                    builder.add(buildNcesApplicationMail(e));
                    markedEvents.remove(e);
                });
        markedEvents.forEach(builder::add);
        return apply(builder.build());
    }

    /**
     * Processes the amended financial results for cases and applications based on the incoming hearing financial result request.
     * <p>
     * This method handles the processing of financial results for both cases and applications. It checks for the presence of
     * correlation IDs and results other than applications, and processes the tracked events accordingly. It appends financial
     * result events for cases and applications, ensuring that the appropriate events are added to the builder and marked events list.
     * <p>
     * The method also handles scenarios where there are no correlation IDs for amended applications or new application results,
     * processing the tracked events and appending the necessary financial result events.
     */
    @SuppressWarnings("java:S107")
    private void processCaseAndApplicationAmendedFinancialResults(final HearingFinancialResultRequest hearingFinancialResultRequest,
                                                                  final String isWrittenOffExists, final String originalDateOfOffenceList,
                                                                  final String originalDateOfSentenceList,
                                                                  final List<NewOffenceByResult> newApplicationOffenceResults,
                                                                  final String applicationResult, final Map<UUID, String> offenceDateMap,
                                                                  final Stream.Builder<Object> builder,
                                                                  final List<MarkedAggregateSendEmailWhenAccountReceived> markedEvents,
                                                                  final boolean hasApplicationResult) {
        final ApplicationNCESEventsHelper applicationNCESEventsHelper = applicationNCESEventsHelper(offenceResultsDetails, applicationResultsDetails, applicationOffencResultsDetails, ncesEmail, correlationIdHistoryItemList);
        if (hasCorrelationId(hearingFinancialResultRequest) || hasResultOtherThanApplication(hearingFinancialResultRequest)) {
            appendFinancialResultEvents(hearingFinancialResultRequest, hasApplicationResult, markedEvents, isWrittenOffExists, originalDateOfOffenceList,
                    originalDateOfSentenceList, newApplicationOffenceResults, applicationResult, offenceDateMap);
            markedEvents.addAll(applicationNCESEventsHelper.buildFinancialResultEventsForApplication(hearingFinancialResultRequest, isWrittenOffExists, originalDateOfOffenceList,
                    originalDateOfSentenceList, newApplicationOffenceResults, applicationResult, offenceDateMap));
        }
        if (hasNoCorrelationIdForAmendedApplication(hearingFinancialResultRequest)) {
            markedEvents.addAll(applicationNCESEventsHelper.buildFinancialResultEventsForApplication(hearingFinancialResultRequest, isWrittenOffExists, originalDateOfOffenceList,
                    originalDateOfSentenceList, newApplicationOffenceResults, applicationResult, offenceDateMap));
        }
    }

    /**
     * Processes the tracked event for hearing financial results.
     * <p>
     * This method checks if the `masterDefendantId` is not null or if any offence result in the request is financial.
     * If either condition is met, it adds the tracked hearing financial results to the provided builder.
     * The tracked event includes the hearing financial result request and the current creation time.
     */
    private void processTrackedEvent(final HearingFinancialResultRequest hearingFinancialResultRequest, final Stream.Builder<Object> builder) {
        if (masterDefendantId != null || hearingFinancialResultRequest.getOffenceResults().stream().anyMatch(OffenceResults::getIsFinancial)) {
            builder.add(
                    HearingFinancialResultsTracked.hearingFinancialResultsTracked()
                            .withHearingFinancialResultRequest(hearingFinancialResultRequest)
                            .withCreatedTime(ZonedDateTime.now())
                            .build());
        }
    }

    private boolean isAmendmentProcess(final HearingFinancialResultRequest hearingFinancialResultRequest, final boolean hasApplicationResult) {
        return hasAmendmentDate(hearingFinancialResultRequest)
                || (!hasApplicationResult && hasDeemedServedValueChanged(hearingFinancialResultRequest));
    }

    private boolean hasDeemedServedValueChanged(final HearingFinancialResultRequest hearingFinancialResultRequest) {
        return hearingFinancialResultRequest
                .getOffenceResults().stream()
                .anyMatch(offenceFromResult ->
                        ofNullable(offenceResultsDetails.get(offenceFromResult.getOffenceId()))
                                .map(offenceFromAggregate -> !offenceFromAggregate.getIsDeemedServed().equals(offenceFromResult.getIsDeemedServed()))
                                .orElse(false));
    }

    public Stream<Object> ncesEmailNotFound(final MarkedAggregateSendEmailWhenAccountReceived accountReceived) {
        if (accountReceived.getMasterDefendantId() != null) {
            final SendNcesEmailNotFound.Builder result = sendNcesEmailNotFound()
                    .withMasterDefendantId(fromString(accountReceived.getMasterDefendantId().toString()))
                    .withSubject(accountReceived.getSubject())
                    .withAmendmentDate(accountReceived.getAmendmentDate())
                    .withAmendmentReason(accountReceived.getAmendmentReason())
                    .withCaseReference(accountReceived.getCaseReferences())
                    .withDateDecisionMade(accountReceived.getDateDecisionMade())
                    .withDefendantName(accountReceived.getDefendantName())
                    .withHearingSittingDay(accountReceived.getHearingSittingDay())
                    .withHearingCourtCentreName(accountReceived.getHearingCourtCentreName())
                    .withDefendantAddress(accountReceived.getDefendantAddress())
                    .withDefendantEmail(accountReceived.getDefendantEmail())
                    .withDefendantContactNumber(accountReceived.getDefendantContactNumber())
                    .withIsSJPHearing(accountReceived.getIsSJPHearing())
                    .withApplicationResult(accountReceived.getApplicationResult())
                    .withDivisionCode(accountReceived.getDivisionCode())
                    .withGobAccountNumber(accountReceived.getGobAccountNumber())
                    .withListedDate(accountReceived.getListedDate())
                    .withOldDivisionCode(accountReceived.getOldDivisionCode())
                    .withOldGobAccountNumber(accountReceived.getOldGobAccountNumber())
                    .withImpositionOffenceDetails(accountReceived.getImpositionOffenceDetails());
            Optional.ofNullable(accountReceived.getDefendantDateOfBirth()).ifPresent(result::withDefendantDateOfBirth);
            return apply(Stream.of(result.build()));
        }
        return empty();
    }

    /**
     * Appends financial result events for cases scenarios based on incoming the hearing financial result request.
     * <p>
     * This method processes the hearing financial result request to determine whether to append amendment events or deemed served events.
     * It builds a new `HearingFinancialResultRequest` object with values from the original request and filters out offence results with
     * application types.
     * Depending on whether the request is part of an amendment process, it either appends amendment events or deemed served events and ACON events.
     * <p>
     * The method ensures that the appropriate financial result events are added to the list of marked events.
     */
    @SuppressWarnings("java:S107")
    private void appendFinancialResultEvents(final HearingFinancialResultRequest hearingFinancialResultRequest,
                                             final boolean hasApplicationResult,
                                             final List<MarkedAggregateSendEmailWhenAccountReceived> markedEvents,
                                             final String isWrittenOffExists,
                                             final String originalDateOfOffenceList,
                                             final String originalDateOfSentenceList,
                                             final List<NewOffenceByResult> newResultByOffenceList,
                                             final String applicationResult, final Map<UUID, String> offenceDateMap) {
        final HearingFinancialResultRequest hfRequest = HearingFinancialResultRequest.hearingFinancialResultRequest()
                .withValuesFrom(hearingFinancialResultRequest)
                .withOffenceResults(getOffenceResults(hearingFinancialResultRequest.getOffenceResults())).build();
        hfRequest.getOffenceResults().removeIf(result -> nonNull(result.getApplicationType()));
        if (isAmendmentProcess(hfRequest, hasApplicationResult)) {
            appendAmendmentEvents(hfRequest, markedEvents, isWrittenOffExists, originalDateOfOffenceList,
                    originalDateOfSentenceList, newResultByOffenceList, applicationResult, offenceDateMap);
        } else {
            appendDeemedServedEvents(hfRequest, markedEvents, offenceDateMap);
            appendACONEvents(hfRequest, markedEvents, offenceDateMap);
        }
    }

    private List<OffenceResults> getOffenceResults(final List<OffenceResults> offenceResults) {
        return new ArrayList<>(offenceResults);
    }

    /**
     * Appends amendment events based on the incoming hearing financial result request.
     * <p>
     * This method processes the hearing financial result request to determine and build imposition offence details for financial,
     * non-financial, deemed served, and ACON offences. It filters and maps the offence results to create lists of imposition offence
     * details, and then appends the appropriate amendment events to the list of marked events.
     * <p>
     * The method ensures that the correct imposition offence details are added to the marked events list, handling both financial
     * and non-financial amendments, deemed served events, and ACON events.
     */
    @SuppressWarnings("java:S107")
    private void appendAmendmentEvents(final HearingFinancialResultRequest hearingFinancialResultRequest,
                                       final List<MarkedAggregateSendEmailWhenAccountReceived> markedEvents,
                                       final String isWrittenOffExists,
                                       final String originalDateOfOffenceList,
                                       final String originalDateOfSentenceList,
                                       final List<NewOffenceByResult> newResultByOffenceList,
                                       final String applicationResult, final Map<UUID, String> offenceDateMap) {
        // Get original imposition offence details from the aggregate for a case
        final List<ImpositionOffenceDetails> originalImpositions = hearingFinancialResultRequest.getOffenceResults().stream()
                .filter(result -> isNull(result.getApplicationType()))
                .map(offenceFromRequest -> offenceResultsDetails.get(offenceFromRequest.getOffenceId()))
                .filter(Objects::nonNull)
                .map(offenceResults -> this.buildImpositionOffenceDetailsFromAggregate(offenceResults, offenceDateMap)).distinct()
                .toList();

        final List<ImpositionOffenceDetails> impositionOffenceDetailsForFinancial = hearingFinancialResultRequest.getOffenceResults().stream()
                .filter(o -> isNull(o.getApplicationType()))
                .filter(OffenceResults::getIsFinancial)
                .filter(offenceFromRequest -> ofNullable(offenceResultsDetails.get(offenceFromRequest.getOffenceId())).map(OffenceResultsDetails::getIsFinancial).orElse(false))
                .map(offenceResults -> this.buildImpositionOffenceDetailsFromRequest(offenceResults, offenceDateMap))
                .collect(toList());

        final List<ImpositionOffenceDetails> impositionOffenceDetailsForNonFinancial = hearingFinancialResultRequest.getOffenceResults().stream()
                .filter(o -> isNull(o.getApplicationType()))
                .filter(o -> Objects.nonNull(o.getAmendmentDate()))
                .filter(o -> !o.getIsFinancial())
                .filter(offenceFromRequest -> ofNullable(offenceResultsDetails.get(offenceFromRequest.getOffenceId())).map(OffenceResultsDetails::getIsFinancial).orElse(false))
                .map(offenceResults -> this.buildImpositionOffenceDetailsFromRequest(offenceResults, offenceDateMap))
                .toList();

        final MarkedAggregateSendEmailEventBuilder markedAggregateSendEmailEventBuilder = markedAggregateSendEmailEventBuilder(ncesEmail, correlationIdHistoryItemList);
        if (!impositionOffenceDetailsForNonFinancial.isEmpty()) {
            if (!impositionOffenceDetailsForFinancial.isEmpty()) {
                impositionOffenceDetailsForFinancial.addAll(impositionOffenceDetailsForNonFinancial);
            } else {
                markedEvents.add(markedAggregateSendEmailEventBuilder.buildMarkedAggregateWithoutOldsForSpecificCorrelationId(hearingFinancialResultRequest, AMEND_AND_RESHARE, correlationIdHistoryItemList.peekLast(), originalImpositions,
                        isWrittenOffExists, originalDateOfOffenceList, originalDateOfSentenceList, newResultByOffenceList, applicationResult,
                        Optional.empty(), Optional.empty()));
            }
        }

        if (!impositionOffenceDetailsForFinancial.isEmpty()) {
            markedEvents.add(markedAggregateSendEmailEventBuilder.buildMarkedAggregateWithOlds(hearingFinancialResultRequest, originalImpositions, applicationResult, newResultByOffenceList, Optional.empty(), Optional.empty(), AMEND_AND_RESHARE));
        }

        final List<ImpositionOffenceDetails> impositionOffenceDetailsForDeemed = hearingFinancialResultRequest.getOffenceResults().stream()
                .filter(o -> isNull(o.getApplicationType()))
                .filter(OffenceResults::getIsDeemedServed)
                .filter(offence -> Objects.nonNull(offence.getAmendmentDate()))
                .map(offenceResults -> this.buildImpositionOffenceDetailsFromRequest(offenceResults, offenceDateMap))
                .toList();
        if (!impositionOffenceDetailsForDeemed.isEmpty()) {
            markedEvents.add(markedAggregateSendEmailEventBuilder.buildMarkedAggregateWithoutOlds(hearingFinancialResultRequest, WRITE_OFF_ONE_DAY_DEEMED_SERVED, impositionOffenceDetailsForDeemed, Boolean.FALSE));
        }

        final List<ImpositionOffenceDetails> impositionOffenceDetailsForACON = hearingFinancialResultRequest.getOffenceResults().stream()
                .filter(o -> isNull(o.getApplicationType()))
                .filter(OffenceResults::getIsFinancial)
                .filter(offence -> ACON.equals(offence.getResultCode()))
                .filter(offence -> Objects.nonNull(offence.getAmendmentDate()))
                .map(offenceResults -> this.buildImpositionOffenceDetailsFromRequest(offenceResults, offenceDateMap))
                .toList();
        if (!impositionOffenceDetailsForACON.isEmpty()) {
            markedEvents.add(markedAggregateSendEmailEventBuilder.buildMarkedAggregateWithoutOlds(hearingFinancialResultRequest, NCESDecisionConstants.ACON_EMAIL_SUBJECT, impositionOffenceDetailsForACON, Boolean.FALSE));
        }
    }

    private void appendDeemedServedEvents(final HearingFinancialResultRequest hearingFinancialResultRequest,
                                          final List<MarkedAggregateSendEmailWhenAccountReceived> markedEvents, final Map<UUID, String> offenceDateMap) {
        final List<ImpositionOffenceDetails> impositionOffenceDetailsForDeemed = hearingFinancialResultRequest.getOffenceResults().stream()
                .filter(o -> isNull(o.getApplicationType()))
                .filter(OffenceResults::getIsDeemedServed)
                .map(offenceResults -> this.buildImpositionOffenceDetailsFromRequest(offenceResults, offenceDateMap))
                .toList();
        if (!impositionOffenceDetailsForDeemed.isEmpty()) {
            markedEvents.add(markedAggregateSendEmailEventBuilder(ncesEmail, correlationIdHistoryItemList).buildMarkedAggregateWithoutOlds(hearingFinancialResultRequest, WRITE_OFF_ONE_DAY_DEEMED_SERVED, impositionOffenceDetailsForDeemed, Boolean.FALSE));
        }
    }


    private void appendACONEvents(final HearingFinancialResultRequest hearingFinancialResultRequest,
                                  final List<MarkedAggregateSendEmailWhenAccountReceived> markedEvents, final Map<UUID, String> offenceDateMap) {
        final List<ImpositionOffenceDetails> impositionOffenceDetailsForAcon = hearingFinancialResultRequest.getOffenceResults().stream()
                .filter(o -> isNull(o.getApplicationType()))
                .filter(OffenceResults::getIsFinancial)
                .filter(offence -> ACON.equals(offence.getResultCode()))
                .map(offenceResults -> this.buildImpositionOffenceDetailsFromRequest(offenceResults, offenceDateMap))
                .toList();
        if (!impositionOffenceDetailsForAcon.isEmpty()) {
            markedEvents.add(markedAggregateSendEmailEventBuilder(ncesEmail, correlationIdHistoryItemList).buildMarkedAggregateWithoutOlds(hearingFinancialResultRequest, NCESDecisionConstants.ACON_EMAIL_SUBJECT, impositionOffenceDetailsForAcon, Boolean.FALSE));
        }
    }

    private void handleFinancialResultsRequest(final HearingFinancialResultsTracked hearingFinancialResultsTracked) {
        final HearingFinancialResultRequest request = hearingFinancialResultsTracked.getHearingFinancialResultRequest();
        this.hearingSittingDay = request.getHearingSittingDay();
        this.hearingCourtCentreName = request.getHearingCourtCentreName();
        this.defendantName = request.getDefendantName();
        this.defendantDateOfBirth = request.getDefendantDateOfBirth();
        this.defendantAddress = request.getDefendantAddress();
        this.defendantEmail = request.getDefendantEmail();
        this.defendantContactNumber = request.getDefendantContactNumber();
        this.isSJPHearing = request.getIsSJPHearing();
        this.masterDefendantId = request.getMasterDefendantId();
        this.hearingId = request.getHearingId();
        this.prosecutionCaseReferences = request.getProsecutionCaseReferences();

        if (request.getNcesEmail() != null) {
            this.ncesEmail = request.getNcesEmail();
        }
        if (isNull(this.initialHearingId)) {
            this.initialHearingId = request.getHearingId();
        }

        updateResults(request, hearingFinancialResultsTracked.getCreatedTime());
    }

    private void updateResults(HearingFinancialResultRequest request, final ZonedDateTime createdTime) {
        updateCaseLevelOffenceResults(request);
        updateApplicationLevelOffenceResults(request);
        updateApplicationResults(request);
        if (request.getAccountCorrelationId() != null) {
            correlationIdHistoryItemList.add(CorrelationIdHistoryItem.correlationIdHistoryItem()
                    .withAccountCorrelationId(request.getAccountCorrelationId())
                    .withAccountDivisionCode(request.getAccountDivisionCode())
                    .withCreatedTime(createdTime)
                    .withProsecutionCaseReferences(request.getProsecutionCaseReferences())
                    .build());
        }
    }

    private void updateApplicationLevelOffenceResults(final HearingFinancialResultRequest request) {
        request.getOffenceResults().stream()
                .filter(result -> nonNull(result.getApplicationType()) && nonNull(result.getImpositionOffenceDetails()))
                .filter(result -> Boolean.TRUE.equals(result.getIsParentFlag()))
                .forEach(resultFromRequest ->
                        this.applicationOffencResultsDetails.put(resultFromRequest.getOffenceId(),
                                buildOffenceResultsDetailsFromOffenceResults(resultFromRequest)));

        request.getOffenceResults().stream()
                .filter(result -> nonNull(result.getApplicationType()))
                .filter(result -> Boolean.FALSE.equals(result.getIsParentFlag()))
                .filter(result -> ACON.equals(result.getResultCode()))
                .forEach(resultFromRequest ->
                        this.applicationOffenceACONDetails.put(resultFromRequest.getOffenceId(),
                                buildOffenceResultsDetailsFromOffenceResults(resultFromRequest)));
    }

    private void updateApplicationResults(final HearingFinancialResultRequest request) {

        List<OffenceResults> applicationResults = request.getOffenceResults().stream()
                .filter(offence -> APPLICATION_TYPES.containsKey(offence.getApplicationType()))
                .filter(offence -> nonNull(offence.getApplicationId()))
                .filter(offence -> nonNull(offence.getApplicationTitle())).toList();
        if (!applicationResults.isEmpty()) {
            applicationResults.stream()
                    .filter(result -> nonNull(result.getApplicationId()))
                    .forEach(resultFromRequest ->
                            this.applicationResultsDetails.put(resultFromRequest.getApplicationId(), buildApplicationResultsDetailsFromOffenceResults(applicationResults)));
        }
    }

    private void updateCaseLevelOffenceResults(final HearingFinancialResultRequest request) {
        request.getOffenceResults().stream()
                .filter(result -> isNull(result.getApplicationType()))
                .forEach(resultFromRequest ->
                        this.offenceResultsDetails.put(resultFromRequest.getOffenceId(), buildOffenceResultsDetailsFromOffenceResults(resultFromRequest)));
    }

    private OffenceResultsDetails buildOffenceResultsDetailsFromOffenceResults(OffenceResults resultFromRequest) {
        return offenceResultsDetails()
                .withAmendmentReason(resultFromRequest.getAmendmentReason())
                .withAmendmentDate(resultFromRequest.getAmendmentDate())
                .withApplicationType(resultFromRequest.getApplicationType())
                .withApplicationResultType(resultFromRequest.getApplicationResultType())
                .withDateOfResult(resultFromRequest.getDateOfResult())
                .withImpositionOffenceDetails(resultFromRequest.getImpositionOffenceDetails())
                .withOffenceTitle(resultFromRequest.getOffenceTitle())
                .withOffenceId(resultFromRequest.getOffenceId())
                .withResultId(resultFromRequest.getResultId())
                .withIsDeemedServed(resultFromRequest.getIsDeemedServed())
                .withResultCode(resultFromRequest.getResultCode())
                .withIsFinancial(resultFromRequest.getIsFinancial())
                .withIsParentFlag(resultFromRequest.getIsParentFlag())
                .build();
    }

    public Stream<Object> updateAccountNumber(final String accountNumber, final UUID correlationId) {
        final HearingFinancialResultsUpdated hearingFinancialResultsUpdated = hearingFinancialResultsUpdated()
                .withAccountNumber(accountNumber)
                .withMasterDefendantId(masterDefendantId)
                .withCorrelationId(correlationId)
                .build();
        return apply(builder().add(hearingFinancialResultsUpdated).build());
    }

    public Stream<Object> sendNcesEmailForNewApplication(final String applicationType, final String listingDate, final List<String> caseUrns, String hearingCourtCentreName) {

        if (masterDefendantId == null) {
            return empty();
        }

        final String subject = APPLICATION_TYPES.get(applicationType);
        final NcesEmailNotificationRequested.Builder ncesEmailNotificationRequested = ncesEmailNotificationRequested()
                .withNotificationId(randomUUID())
                .withMaterialId(randomUUID())
                .withSendTo(ncesEmail)
                .withSubject(subject)
                .withHearingCourtCentreName(hearingCourtCentreName.isEmpty() ? this.hearingCourtCentreName : hearingCourtCentreName)
                .withDefendantName(defendantName)
                .withDefendantDateOfBirth(defendantDateOfBirth)
                .withDefendantAddress(defendantAddress)
                .withDefendantEmail(defendantEmail)
                .withDefendantContactNumber(defendantContactNumber)
                .withIsSJPHearing(isSJPHearing)
                .withCaseReferences(String.join(NCESDecisionConstants.COMMA, caseUrns))
                .withMasterDefendantId(masterDefendantId)
                .withListedDate(listingDate);

        ofNullable(hearingSittingDay)
                .ifPresent(a -> ncesEmailNotificationRequested.withHearingSittingDay(a.format(ofPattern(HEARING_SITTING_DAY_PATTERN)))
                        .withOriginalDateOfSentence(a.format(ofPattern(BRITISH_DATE_FORMAT))));


        final Stream<Object> events = ofNullable(correlationIdHistoryItemList.peekLast())
                .map(correlationIdHistoryItem ->
                        correlationIdHistoryItem.getAccountNumber() != null ?
                                builder().add(ncesEmailNotificationRequested
                                                .withGobAccountNumber(correlationIdHistoryItem.getAccountNumber())
                                                .withDivisionCode(correlationIdHistoryItem.getAccountDivisionCode())
                                                .build())
                                        .build() :
                                builder().add(buildMarkedAggregateSendEmailWhenAccountReceived(correlationIdHistoryItem, ncesEmailNotificationRequested.build()))
                                        .build()
                )
                .orElseGet(Stream::empty);

        return apply(events);
    }

    public Stream<Object> checkApplicationEmailAndSend() {
        if (markedAggregateSendEmailWhenAccountReceivedList.isEmpty()) {
            return empty();
        }
        final Set<UUID> idsToBeUnmarked = new HashSet<>();
        final Stream.Builder<Object> builder = Stream.builder();
        final MarkedAggregateSendEmailWhenAccountReceived.Builder markedBuilder = markedAggregateSendEmailWhenAccountReceived();

        markedAggregateSendEmailWhenAccountReceivedList.stream()
                .filter(marked -> correlationIdHistoryItemList.stream().anyMatch(item -> (item.getAccountCorrelationId().equals(marked.getAccountCorrelationId()))
                        || Objects.equals(marked.getOldAccountCorrelationId(), item.getAccountCorrelationId())))
                .forEach(marked -> {
                    markedBuilder.withValuesFrom(marked);

                    final String accountNumber = correlationIdHistoryItemList.stream().filter(item -> item.getAccountCorrelationId().equals(marked.getAccountCorrelationId()))
                            .findFirst().map(CorrelationIdHistoryItem::getAccountNumber).orElse(null);

                    markedBuilder.withGobAccountNumber(accountNumber);

                    if (nonNull(marked.getOldAccountCorrelationId())) {
                        final String oldAccountNumber = correlationIdHistoryItemList.stream().filter(item -> marked.getOldAccountCorrelationId().equals(item.getAccountCorrelationId()))
                                .findFirst().map(CorrelationIdHistoryItem::getAccountNumber).orElse(null);
                        markedBuilder.withOldGobAccountNumber(oldAccountNumber);
                    }

                    final MarkedAggregateSendEmailWhenAccountReceived finalMarked = markedBuilder.build();

                    if (nonNull(finalMarked.getGobAccountNumber()) && (isNull(finalMarked.getOldAccountCorrelationId()) || nonNull(finalMarked.getOldGobAccountNumber()))) {
                        builder.add(buildNcesApplicationMail(finalMarked));
                        idsToBeUnmarked.add(finalMarked.getId());
                    }

                });

        idsToBeUnmarked.forEach(id -> builder.add(UnmarkedAggregateSendEmailWhenAccountReceived.unmarkedAggregateSendEmailWhenAccountReceived()
                .withId(id)
                .build()));
        return apply(builder.build());
    }

    public UUID getHearingId() {
        return hearingId;
    }

    public ZonedDateTime getHearingSittingDay() {
        return hearingSittingDay;
    }

    public String getHearingCourtCentreName() {
        return hearingCourtCentreName;
    }

    public String getDefendantName() {
        return defendantName;
    }

    public String getDefendantDateOfBirth() {
        return defendantDateOfBirth;
    }

    public String getDefendantAddress() {
        return defendantAddress;
    }

    public String getDefendantEmail() {
        return defendantEmail;
    }

    public String getDefendantContactNumber() {
        return defendantContactNumber;
    }

    public UUID getInitialHearingId() {
        return initialHearingId;
    }

    public Boolean getIsSJPHearing() {
        return isSJPHearing;
    }

    public UUID getMasterDefendantId() {
        return masterDefendantId;
    }

    public Map<UUID, OffenceResultsDetails> getOffenceResultsDetails() {
        return offenceResultsDetails;
    }

    public List<CorrelationIdHistoryItem> getCorrelationIdHistoryItemList() {
        return correlationIdHistoryItemList;
    }

    public String getNcesEmail() {
        return ncesEmail;
    }


    public List<String> getProsecutionCaseReferences() {
        return prosecutionCaseReferences;
    }


    private MarkedAggregateSendEmailWhenAccountReceived buildMarkedAggregateSendEmailWhenAccountReceived(final CorrelationIdHistoryItem correlationIdHistoryItem, final NcesEmailNotificationRequested ncesEmailNotificationRequested) {
        return markedAggregateSendEmailWhenAccountReceived()
                .withId(randomUUID())
                .withAccountCorrelationId(correlationIdHistoryItem.getAccountCorrelationId())
                .withAccountCorrelationId(correlationIdHistoryItem.getAccountCorrelationId())
                .withDivisionCode(correlationIdHistoryItem.getAccountDivisionCode())
                .withSendTo(ncesEmailNotificationRequested.getSendTo())
                .withSubject(ncesEmailNotificationRequested.getSubject())
                .withDefendantName(ncesEmailNotificationRequested.getDefendantName())
                .withHearingSittingDay(ncesEmailNotificationRequested.getHearingSittingDay())
                .withOriginalDateOfSentence(ncesEmailNotificationRequested.getOriginalDateOfSentence())
                .withHearingCourtCentreName(ncesEmailNotificationRequested.getHearingCourtCentreName())
                .withDefendantDateOfBirth(ncesEmailNotificationRequested.getDefendantDateOfBirth())
                .withDefendantAddress(ncesEmailNotificationRequested.getDefendantAddress())
                .withDefendantEmail(ncesEmailNotificationRequested.getDefendantEmail())
                .withDefendantContactNumber(ncesEmailNotificationRequested.getDefendantContactNumber())
                .withIsSJPHearing(ncesEmailNotificationRequested.getIsSJPHearing())
                .withApplicationResult(ncesEmailNotificationRequested.getApplicationResult())
                .withCaseReferences(ncesEmailNotificationRequested.getCaseReferences())
                .withMasterDefendantId(ncesEmailNotificationRequested.getMasterDefendantId())
                .withListedDate(ncesEmailNotificationRequested.getListedDate())
                .withDateDecisionMade(ncesEmailNotificationRequested.getDateDecisionMade())
                .withImpositionOffenceDetails(ncesEmailNotificationRequested.getImpositionOffenceDetails())
                .withAmendmentReason(ncesEmailNotificationRequested.getAmendmentReason())
                .withAmendmentDate(ncesEmailNotificationRequested.getAmendmentDate())
                .build();
    }

    private Object buildNcesApplicationMail(final MarkedAggregateSendEmailWhenAccountReceived marked) {
        final NcesEmailNotificationRequested.Builder ncesNotification = getNcesEmailNotificationRequested(marked);

        if (APPLICATION_TYPES.values().contains(marked.getSubject())) {
            buildDefendantParameters(ncesNotification, marked);
            ncesNotification.withHearingSittingDay(marked.getHearingSittingDay());
            ncesNotification.withOriginalDateOfSentence(marked.getOriginalDateOfSentence());
            ncesNotification.withHearingCourtCentreName(marked.getHearingCourtCentreName());
            return ncesNotification.withDateDecisionMade(marked.getDateDecisionMade())
                    .build();
        } else if (WRITE_OFF_ONE_DAY_DEEMED_SERVED.equals(marked.getSubject())) {
            buildDefendantParameters(ncesNotification, marked);
            return ncesNotification.withHearingSittingDay(marked.getHearingSittingDay())
                    .withOriginalDateOfSentence(getFormattedDates(marked.getHearingSittingDay()))
                    .build();
        } else if (getApplicationGrantedSubjects().contains(marked.getSubject()) || getApplicationAppealAllowedSubjects().contains(marked.getSubject())) {
            ncesNotification.withHearingSittingDay(marked.getHearingSittingDay());
            ncesNotification.withOriginalDateOfOffence(marked.getOriginalDateOfOffence());
            ncesNotification.withHearingCourtCentreName(marked.getHearingCourtCentreName());
            buildDefendantParameters(ncesNotification, marked);
            ncesNotification.withIsFinancialPenaltiesWrittenOff(marked.getIsFinancialPenaltiesWrittenOff());
            ncesNotification.withOriginalDateOfSentence(marked.getOriginalDateOfSentence());
            ncesNotification.withNewOffenceByResult(marked.getNewOffenceByResult());
            return ncesNotification.withHearingSittingDay(marked.getHearingSittingDay())
                    .withDateDecisionMade(marked.getDateDecisionMade())
                    .build();
        } else if (isThisApplicationUpdated(marked)) {
            ncesNotification.withHearingCourtCentreName(marked.getHearingCourtCentreName());
            buildDefendantParameters(ncesNotification, marked);
            ncesNotification.withApplicationResult(marked.getApplicationResult());
            ncesNotification.withImpositionOffenceDetails(null);
        } else if (AMEND_AND_RESHARE.equals(marked.getSubject())) {
            ncesNotification.withAmendmentReason(marked.getAmendmentReason() != null ? marked.getAmendmentReason() : AMENDMENT_REASON);
            ncesNotification.withDefendantDateOfBirth(marked.getDefendantDateOfBirth());
            ncesNotification.withNewOffenceByResult(marked.getNewOffenceByResult());
            ncesNotification.withOriginalApplicationResults(marked.getOriginalApplicationResults());
            ncesNotification.withNewApplicationResults(marked.getNewApplicationResults());
            //AccountWriteoff email is triggerred even when there no new GOB account number and OldGOB will be considered the latest GOB
            if (isNull(marked.getGobAccountNumber()) && nonNull(marked.getOldGobAccountNumber())) {
                ncesNotification.withGobAccountNumber(marked.getOldGobAccountNumber())
                        .withOldGobAccountNumber(null);
            }
            return ncesNotification.build();
        } else if (getApplicationAppealSubjects().contains(marked.getSubject())
                || getApplicationNonGrantedSubjects().contains(marked.getSubject())) {
            buildDefendantParameters(ncesNotification, marked);
            ncesNotification.withNewOffenceByResult(marked.getNewOffenceByResult());
            ncesNotification.withOriginalApplicationResults(marked.getOriginalApplicationResults());
            ncesNotification.withNewApplicationResults(marked.getNewApplicationResults());
        }
        return ncesNotification.withDateDecisionMade(marked.getDateDecisionMade())
                .build();
    }

    private String getFormattedDates(final String dates) {
        if (!dates.isEmpty()) {
            final DateTimeFormatter inputFormat = DateTimeFormatter.ofPattern(HEARING_SITTING_DAY_PATTERN);
            final DateTimeFormatter outputFormat = DateTimeFormatter.ofPattern(BRITISH_DATE_FORMAT);
            return Arrays.stream(dates.split(","))
                    .map(date -> LocalDate.parse(date, inputFormat).format(outputFormat))
                    .collect(Collectors.joining(","));
        }
        return "";
    }

    private boolean isThisApplicationUpdated(MarkedAggregateSendEmailWhenAccountReceived marked) {
        return APPLICATION_UPDATED_SUBJECT.values().stream()
                .anyMatch(e -> e.equals(marked.getSubject()));
    }

    private void buildDefendantParameters(NcesEmailNotificationRequested.Builder ncesNotification,
                                          MarkedAggregateSendEmailWhenAccountReceived marked) {
        ncesNotification.withDefendantDateOfBirth(marked.getDefendantDateOfBirth())
                .withDefendantAddress(marked.getDefendantAddress())
                .withDefendantEmail(marked.getDefendantEmail())
                .withDefendantContactNumber(marked.getDefendantContactNumber());
    }

    private NcesEmailNotificationRequested.Builder getNcesEmailNotificationRequested(MarkedAggregateSendEmailWhenAccountReceived marked) {
        return ncesEmailNotificationRequested()
                .withNotificationId(randomUUID())
                .withMaterialId(randomUUID())
                .withDivisionCode(marked.getDivisionCode())
                .withSendTo(marked.getSendTo())
                .withSubject(marked.getSubject())
                .withDefendantName(marked.getDefendantName())
                .withCaseReferences(marked.getCaseReferences())
                .withMasterDefendantId(marked.getMasterDefendantId())
                .withListedDate(marked.getListedDate())
                .withImpositionOffenceDetails(marked.getImpositionOffenceDetails())
                .withGobAccountNumber(marked.getGobAccountNumber())
                .withAmendmentDate(marked.getAmendmentDate())
                .withAmendmentReason(marked.getAmendmentReason())
                .withOldDivisionCode(marked.getOldDivisionCode())
                .withOldGobAccountNumber(marked.getOldGobAccountNumber())
                .withIsSJPHearing(marked.getIsSJPHearing());
    }

    private ImpositionOffenceDetails buildImpositionOffenceDetailsFromRequest(final OffenceResults offencesFromRequest, final Map<UUID, String> offenceDateMap) {
        return ImpositionOffenceDetails.impositionOffenceDetails()
                .withDetails(offencesFromRequest.getImpositionOffenceDetails())
                .withOffenceDate(offenceDateMap.get(offencesFromRequest.getOffenceId()))
                .withOffenceId(offencesFromRequest.getOffenceId())
                .withTitle(offencesFromRequest.getOffenceTitle())
                .build();
    }

    private ImpositionOffenceDetails buildImpositionOffenceDetailsFromAggregate(final OffenceResultsDetails offencesFromAggregate, final Map<UUID, String> offenceDateMap) {
        return ImpositionOffenceDetails.impositionOffenceDetails()
                .withDetails(offencesFromAggregate.getImpositionOffenceDetails())
                .withOffenceDate(offenceDateMap.get(offencesFromAggregate.getOffenceId()))
                .withTitle(offencesFromAggregate.getOffenceTitle())
                .build();
    }

    public Stream<Object> saveNcesEmailNotificationDetails(final NcesEmailNotification payload) {
        return apply(Stream.of(payload));
    }

    public Stream<Object> updateDefendantAddressInAggregate(final MasterDefendant masterDefendant) {
        final Stream.Builder<Object> builder = Stream.builder();
        final String addressOnApplication = getDefendantAddressFromApplication(masterDefendant);
        if (!EMPTY_STRING.equals(addressOnApplication) && !addressOnApplication.equals(defendantAddress)) {
            builder.add(DefendantAddressUpdatedFromApplication.defendantAddressUpdatedFromApplication()
                    .withDefendantId(masterDefendant.getMasterDefendantId())
                    .withDefendantAddress(addressOnApplication).build());
            return apply(builder.build());
        }
        return apply(empty());
    }

    private String getDefendantAddressFromApplication(final MasterDefendant masterDefendant) {
        if (nonNull(masterDefendant.getLegalEntityDefendant()) && nonNull(masterDefendant.getLegalEntityDefendant().getOrganisation())) {
            return getAddressAsString(masterDefendant.getLegalEntityDefendant().getOrganisation().getAddress());

        }
        if (nonNull(masterDefendant.getPersonDefendant()) && nonNull(masterDefendant.getPersonDefendant().getPersonDetails())) {
            return getAddressAsString(masterDefendant.getPersonDefendant().getPersonDetails().getAddress());
        }
        return EMPTY_STRING;
    }

    private String getAddressAsString(final Address address) {
        final List<String> addressLines = asList(address.getAddress1(), address.getAddress2(), address.getAddress3(), address.getAddress4(), address.getAddress5(), address.getPostcode());
        return addressLines.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" "));
    }
}