package uk.gov.moj.cpp.results.domain.aggregate;

import static java.time.ZonedDateTime.now;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.builder;
import static java.util.stream.Stream.empty;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.core.courts.HearingFinancialResultsUpdated.hearingFinancialResultsUpdated;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;
import static uk.gov.justice.hearing.courts.OffenceResultsDetails.offenceResultsDetails;
import static uk.gov.moj.cpp.results.domain.aggregate.NCESDecisionHelper.buildApplicationResultsDetailsFromOffenceResults;
import static uk.gov.moj.cpp.results.domain.aggregate.NCESDecisionHelper.isApplicationDenied;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.AMEND_AND_RESHARE;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.APPLICATION_TYPES;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.APPLICATION_UPDATED_SUBJECT;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.WRITE_OFF_ONE_DAY_DEEMED_SERVED;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.getApplicationAppealAllowedSubjects;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.getApplicationAppealSubjects;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.getApplicationGrantedSubjects;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.getApplicationNonGrantedSubjects;
import static uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.ResultNotificationRuleEngine.resultNotificationRuleEngine;
import static uk.gov.moj.cpp.results.domain.aggregate.utils.GobAccountHelper.getOldGobAccounts;
import static uk.gov.moj.cpp.results.domain.event.MarkedAggregateSendEmailWhenAccountReceived.markedAggregateSendEmailWhenAccountReceived;
import static uk.gov.moj.cpp.results.domain.event.NcesEmailNotificationRequested.ncesEmailNotificationRequested;
import static uk.gov.moj.cpp.results.domain.event.NewOffenceByResult.newOffenceByResult;
import static uk.gov.moj.cpp.results.domain.event.SendNcesEmailNotFound.sendNcesEmailNotFound;

import uk.gov.justice.core.courts.Address;
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
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.ResultNotificationRule.RuleInput;
import uk.gov.moj.cpp.results.domain.aggregate.utils.CorrelationItem;
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
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;


@SuppressWarnings({"PMD.BeanMembersShouldSerialize"})
public class HearingFinancialResultsAggregate implements Aggregate {

    private static final Logger LOGGER = getLogger(HearingFinancialResultsAggregate.class);
    private static final long serialVersionUID = 1691228462960025058L;
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

    private final LinkedList<CorrelationItem> correlationItemList = new LinkedList<>();
    private final Map<UUID, OffenceResultsDetails> caseOffenceResultsDetails = new HashMap<>();
    private final List<MarkedAggregateSendEmailWhenAccountReceived> markedAggregateSendEmailWhenAccountReceivedList = new ArrayList<>();
    private final Map<UUID, List<OffenceResultsDetails>> applicationResultsDetails = new HashMap<>();
    private final Map<UUID, List<OffenceResultsDetails>> applicationOffenceResultsDetails = new HashMap<>();

    //returns true when both have new AccountCorrelationId & new GobAccountNumber OR new AccountCorrelationId & new GobAccountNumber are null
    private static final Predicate<MarkedAggregateSendEmailWhenAccountReceived> hasNewGobAccountIfExistOrNull = event -> Objects.isNull(event.getAccountCorrelationId()) == Objects.isNull(event.getGobAccountNumber());
    //returns true when both have old AccountCorrelationId & old GobAccountNumber OR old AccountCorrelationId & old GobAccountNumber are null
    private static final Predicate<MarkedAggregateSendEmailWhenAccountReceived> hasOldGobAccountIfExistOrNull = event -> Objects.isNull(event.getOldAccountCorrelationId()) == Objects.isNull(event.getOldGobAccountNumber());


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
        correlationItemList.stream()
                .filter(correlationItem -> hearingFinancialResultsUpdated.getCorrelationId().equals(correlationItem.getAccountCorrelationId()))
                .forEach(correlationItem -> {
                    final CorrelationItem.Builder correlationItemBuilder = CorrelationItem.correlationItem().withValuesFrom(correlationItem);
                    correlationItemList.set(correlationItemList.indexOf(correlationItem), correlationItemBuilder.withAccountNumber(hearingFinancialResultsUpdated.getAccountNumber()).build());
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
        final List<MarkedAggregateSendEmailWhenAccountReceived> markedEvents = resultNotificationRuleEngine().evaluate(
                new RuleInput(
                        hearingFinancialResultRequest,
                        isWrittenOffExists,
                        originalDateOfOffenceList,
                        originalDateOfSentenceList,
                        newResultByOffenceList,
                        applicationResult,
                        offenceDateMap,
                        ncesEmail,
                        caseOffenceResultsDetails,
                        applicationResultsDetails,
                        applicationOffenceResultsDetails,
                        new LinkedList<>(correlationItemList)));

        processTrackedEvent(hearingFinancialResultRequest, builder);

        markedEvents.stream()
                .filter(hasNewGobAccountIfExistOrNull)
                .filter(hasOldGobAccountIfExistOrNull)
                .toList()
                .forEach(e -> {
                    LOGGER.info(":: Build Nces Application Mail ::");
                    builder.add(buildNcesApplicationMail(e));
                    markedEvents.remove(e);
                });
        markedEvents.forEach(builder::add);
        return apply(builder.build());
    }

    /**
     * Processes the tracked event for hearing financial results.
     * <p>
     * This method checks if the `masterDefendantId` is not null on the aggregate or not null on the request
     * If either condition is met, it adds the tracked hearing financial results to the provided builder.
     * The tracked event includes the hearing financial result request and the current creation time.
     */
    private void processTrackedEvent(final HearingFinancialResultRequest hearingFinancialResultRequest, final Stream.Builder<Object> builder) {
        if (nonNull(masterDefendantId) || nonNull(hearingFinancialResultRequest.getMasterDefendantId())) {
            builder.add(
                    HearingFinancialResultsTracked.hearingFinancialResultsTracked()
                            .withHearingFinancialResultRequest(hearingFinancialResultRequest)
                            .withCreatedTime(ZonedDateTime.now())
                            .build());
        }
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
            final List<OffenceResultsDetails> offenceResultsDetailsList = request.getOffenceResults().stream()
                    .map(this::buildOffenceResultsDetailsFromOffenceResults).toList();

            correlationItemList.add(CorrelationItem.correlationItem()
                    .withAccountCorrelationId(request.getAccountCorrelationId())
                    .withHearingId(request.getHearingId())
                    .withAccountDivisionCode(request.getAccountDivisionCode())
                    .withCreatedTime(createdTime)
                    .withProsecutionCaseReferences(request.getProsecutionCaseReferences())
                    .withOffenceResultsDetailsList(offenceResultsDetailsList)
                    .build());
        }
    }

    private void updateApplicationLevelOffenceResults(final HearingFinancialResultRequest request) {
        final List<OffenceResultsDetails> allOffenceResultsDetails = request.getOffenceResults().stream()
                .filter(result -> nonNull(result.getApplicationType()) && nonNull(result.getImpositionOffenceDetails()))
                .filter(result -> Boolean.TRUE.equals(result.getIsParentFlag()))
                .distinct()
                .map(this::buildOffenceResultsDetailsFromOffenceResults).toList();

        final UUID applicationId = request.getOffenceResults().stream().map(OffenceResults::getApplicationId).filter(Objects::nonNull).findFirst().orElse(null);
        if (nonNull(applicationId)) {
            applicationOffenceResultsDetails.put(applicationId, allOffenceResultsDetails);
        }
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
                        this.caseOffenceResultsDetails.put(resultFromRequest.getOffenceId(), buildOffenceResultsDetailsFromOffenceResults(resultFromRequest)));
    }

    private OffenceResultsDetails buildOffenceResultsDetailsFromOffenceResults(OffenceResults resultFromRequest) {
        return offenceResultsDetails()
                .withAmendmentReason(resultFromRequest.getAmendmentReason())
                .withAmendmentDate(resultFromRequest.getAmendmentDate())
                .withApplicationId(resultFromRequest.getApplicationId())
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
                .withCreatedTime(ZonedDateTime.now())
                .build();
    }

    public Stream<Object> updateAccountNumber(final String accountNumber, final UUID correlationId) {

        final HearingFinancialResultsUpdated hearingFinancialResultsUpdated = hearingFinancialResultsUpdated()
                .withAccountNumber(accountNumber)
                .withMasterDefendantId(masterDefendantId)
                .withCorrelationId(correlationId)
                .withUpdatedTime(now())
                .build();
        return apply(builder().add(hearingFinancialResultsUpdated).build());
    }

    public Stream<Object> sendNcesEmailForNewApplication(final String applicationType, final String listingDate, final List<String> caseUrns, final String hearingCourtCentreName, final List<String> clonedOffenceIdList) {

        if (masterDefendantId == null || hasNoPreviousFinancialImposition(clonedOffenceIdList)) {
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
                .withListedDate(listingDate)
                .withIsWriteOff(Boolean.FALSE);

        ofNullable(hearingSittingDay)
                .ifPresent(a -> ncesEmailNotificationRequested.withHearingSittingDay(a.format(ofPattern(HEARING_SITTING_DAY_PATTERN)))
                        .withOriginalDateOfSentence(a.format(ofPattern(BRITISH_DATE_FORMAT))));

        if (isNotEmpty(correlationItemList)) {
            final List<UUID> offenceIdList = isNotEmpty(clonedOffenceIdList) ? clonedOffenceIdList.stream().map(UUID::fromString).toList() : emptyList();
            final List<String> gobAccounts = getOldGobAccounts(new LinkedList<>(correlationItemList), null, offenceIdList, applicationResultsDetails);
            if (isNotEmpty(gobAccounts)) {
                return apply(builder().add(ncesEmailNotificationRequested
                                .withGobAccountNumber(String.join(",", gobAccounts))
                                .withDivisionCode(getCorrelationItemForDivisionCode().getAccountDivisionCode())
                                .build())
                        .build());
            } else {
                return apply(builder().add(buildMarkedAggregateSendEmailWhenAccountReceived(correlationItemList.peekLast(), ncesEmailNotificationRequested.build()))
                        .build());
            }
        } else {
            return apply(Stream.empty());
        }
    }

    private CorrelationItem getCorrelationItemForDivisionCode() {
        final LinkedList<CorrelationItem> correlationItemListGob = new LinkedList<>(correlationItemList);
        correlationItemListGob.sort(comparing(CorrelationItem::getCreatedTime).reversed());

        return correlationItemListGob.stream()
                //filter to skip correlation if that is for an application that was declined
                .filter(correlationItem -> {
                    final UUID applicationId = correlationItem.getOffenceResultsDetailsList().stream()
                            .map(OffenceResultsDetails::getApplicationId)
                            .filter(Objects::nonNull).findFirst().orElse(null);
                    return !isApplicationDenied(applicationResultsDetails.get(applicationId));
                })
                .findFirst().orElse(correlationItemList.peekLast());
    }

    public Stream<Object> checkApplicationEmailAndSend() {
        if (markedAggregateSendEmailWhenAccountReceivedList.isEmpty()) {
            return empty();
        }
        final Set<UUID> idsToBeUnmarked = new HashSet<>();
        final Stream.Builder<Object> builder = Stream.builder();

        markedAggregateSendEmailWhenAccountReceivedList.stream()
                .filter(marked -> correlationItemList.stream().anyMatch(item -> (item.getAccountCorrelationId().equals(marked.getAccountCorrelationId()))
                        || Objects.equals(marked.getOldAccountCorrelationId(), item.getAccountCorrelationId())))
                .forEach(marked -> {
                    final MarkedAggregateSendEmailWhenAccountReceived.Builder markedBuilder = markedAggregateSendEmailWhenAccountReceived().withValuesFrom(marked);

                    updateOldAndNewGobAccounts(marked, markedBuilder);

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

    public Map<UUID, OffenceResultsDetails> getCaseOffenceResultsDetails() {
        return caseOffenceResultsDetails;
    }

    public List<CorrelationItem> getCorrelationItemList() {
        return correlationItemList;
    }

    public String getNcesEmail() {
        return ncesEmail;
    }


    public List<String> getProsecutionCaseReferences() {
        return prosecutionCaseReferences;
    }

    private void updateOldAndNewGobAccounts(final MarkedAggregateSendEmailWhenAccountReceived marked, final MarkedAggregateSendEmailWhenAccountReceived.Builder markedBuilder) {

        final String accountNumber = correlationItemList.stream()
                .filter(item -> item.getAccountCorrelationId().equals(marked.getAccountCorrelationId()))
                .findFirst().map(CorrelationItem::getAccountNumber).orElse(null);
        markedBuilder.withGobAccountNumber(accountNumber);

        if (nonNull(marked.getOldAccountCorrelationId())) {
            final List<UUID> offenceIdList = marked.getImpositionOffenceDetails().stream().map(ImpositionOffenceDetails::getOffenceId).toList();
            final List<String> oldGobAccounts = getOldGobAccounts(new LinkedList<>(correlationItemList), marked.getAccountCorrelationId(), offenceIdList, applicationResultsDetails);

            markedBuilder.withOldGobAccountNumber(oldGobAccounts.isEmpty() ? null : String.join(",", oldGobAccounts));
        }
    }

    private boolean hasNoPreviousFinancialImposition(final List<String> clonedOffenceIdList) {
        final List<OffenceResultsDetails> allOffenceResults = getAllOffenceResultsDetailsList();
        allOffenceResults.sort(comparing(OffenceResultsDetails::getCreatedTime).reversed());

        return isNotEmpty(clonedOffenceIdList) && clonedOffenceIdList.stream()
                .map(UUID::fromString)
                .map(clonedOffenceId -> getPreviousResults(allOffenceResults, clonedOffenceId))
                .filter(Objects::nonNull)
                .noneMatch(previousOffenceResultDetails -> Boolean.TRUE.equals(previousOffenceResultDetails.getIsFinancial()));
    }

    private List<OffenceResultsDetails> getAllOffenceResultsDetailsList() {
        final List<OffenceResultsDetails> allOffenceResults = new ArrayList<>(caseOffenceResultsDetails.values());
        applicationOffenceResultsDetails.forEach((applicationId, offenceResultsDetails) -> allOffenceResults.addAll(offenceResultsDetails));
        return allOffenceResults;
    }

    private OffenceResultsDetails getPreviousResults(final List<OffenceResultsDetails> allOffenceResults, final UUID caseOffenceId) {
        return allOffenceResults.stream().filter(o -> caseOffenceId.equals(o.getOffenceId())).findFirst().orElse(null);
    }

    private MarkedAggregateSendEmailWhenAccountReceived buildMarkedAggregateSendEmailWhenAccountReceived(final CorrelationItem correlationItem, final NcesEmailNotificationRequested ncesEmailNotificationRequested) {
        return markedAggregateSendEmailWhenAccountReceived()
                .withId(randomUUID())
                .withAccountCorrelationId(correlationItem.getAccountCorrelationId())
                .withDivisionCode(correlationItem.getAccountDivisionCode())
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
            ncesNotification.withNewOffenceByResult(groupedByOffenceId(marked.getNewOffenceByResult()));
            ncesNotification.withNewApplicationResults(marked.getNewApplicationResults());
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
            ncesNotification.withNewOffenceByResult(groupedByOffenceId(marked.getNewOffenceByResult()));
            ncesNotification.withOriginalApplicationResults(marked.getOriginalApplicationResults());
            ncesNotification.withNewApplicationResults(marked.getNewApplicationResults());
            ncesNotification.withIsWriteOff(Boolean.TRUE);
            //AccountWriteoff email is triggerred even when there no new GOB account number and OldGOB will be considered the latest GOB
            if (isNull(marked.getGobAccountNumber()) && nonNull(marked.getOldGobAccountNumber())) {
                ncesNotification.withGobAccountNumber(marked.getOldGobAccountNumber())
                        .withOldGobAccountNumber(null);
            }
            return ncesNotification.build();
        } else if (getApplicationAppealSubjects().contains(marked.getSubject())
                || getApplicationNonGrantedSubjects().contains(marked.getSubject())) {
            buildDefendantParameters(ncesNotification, marked);
            ncesNotification.withNewOffenceByResult(groupedByOffenceId(marked.getNewOffenceByResult()));
            ncesNotification.withOriginalApplicationResults(marked.getOriginalApplicationResults());
            ncesNotification.withNewApplicationResults(marked.getNewApplicationResults());
        }
        return ncesNotification.withDateDecisionMade(marked.getDateDecisionMade())
                .build();
    }

    private List<NewOffenceByResult> groupedByOffenceId(final List<NewOffenceByResult> newOffenceByResult) {
        if (isNotEmpty(newOffenceByResult)) {
            final Map<UUID, List<NewOffenceByResult>> newOffenceByResultGrouped = newOffenceByResult.stream()
                    .collect(groupingBy(NewOffenceByResult::getOffenceId, toList()));
            final List<NewOffenceByResult> newOffenceByResultsGroupedByOffenceId = new ArrayList<>();
            newOffenceByResult.forEach(nor -> {
                List<NewOffenceByResult> newOffenceByResultList = newOffenceByResultGrouped.get(nor.getOffenceId());
                if (newOffenceByResultsGroupedByOffenceId.stream().noneMatch(no -> nor.getOffenceId().equals(no.getOffenceId()))) {
                    //ensure original list order maintained
                    final NewOffenceByResult anyOffenceByResult = newOffenceByResultList.get(0);
                    final String title = newOffenceByResultList.stream().map(NewOffenceByResult::getTitle).filter(StringUtils::isNotEmpty).findFirst().orElse(anyOffenceByResult.getTitle());
                    final String offenceDate = newOffenceByResultList.stream().map(NewOffenceByResult::getOffenceDate).filter(StringUtils::isNotEmpty).findFirst().orElse(anyOffenceByResult.getTitle());
                    final String allDetails = newOffenceByResultList.stream().map(NewOffenceByResult::getDetails).filter(StringUtils::isNotEmpty).collect(joining("\n"));
                    newOffenceByResultsGroupedByOffenceId.add(newOffenceByResult()
                            .withTitle(title)
                            .withOffenceId(anyOffenceByResult.getOffenceId())
                            .withOffenceDate(offenceDate)
                            .withDetails(allDetails)
                            .build());
                }
            });
            return newOffenceByResultsGroupedByOffenceId;
        }
        return newOffenceByResult;
    }

    private String getFormattedDates(final String dates) {
        if (!dates.isEmpty()) {
            final DateTimeFormatter inputFormat = DateTimeFormatter.ofPattern(HEARING_SITTING_DAY_PATTERN);
            final DateTimeFormatter outputFormat = DateTimeFormatter.ofPattern(BRITISH_DATE_FORMAT);
            return Arrays.stream(dates.split(","))
                    .map(date -> LocalDate.parse(date, inputFormat).format(outputFormat))
                    .collect(joining(","));
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
                .withIsSJPHearing(marked.getIsSJPHearing())
                .withIsWriteOff(Boolean.FALSE);
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
                .collect(joining(" "));
    }
}