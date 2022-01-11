package uk.gov.moj.cpp.results.domain.aggregate;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.builder;
import static java.util.stream.Stream.empty;
import static uk.gov.justice.core.courts.HearingFinancialResultsUpdated.hearingFinancialResultsUpdated;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;
import static uk.gov.justice.hearing.courts.OffenceResultsDetails.offenceResultsDetails;
import static uk.gov.moj.cpp.results.domain.event.MarkedAggregateSendEmailWhenAccountReceived.markedAggregateSendEmailWhenAccountReceived;
import static uk.gov.moj.cpp.results.domain.event.NcesEmailNotificationRequested.ncesEmailNotificationRequested;
import static uk.gov.moj.cpp.results.domain.event.SendNcesEmailNotFound.sendNcesEmailNotFound;

import uk.gov.justice.core.courts.CorrelationIdHistoryItem;
import uk.gov.justice.core.courts.HearingFinancialResultsUpdated;
import uk.gov.justice.core.courts.UnmarkedAggregateSendEmailWhenAccountReceived;
import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.justice.hearing.courts.HearingFinancialResultRequest;
import uk.gov.justice.hearing.courts.HearingFinancialResultsTracked;
import uk.gov.justice.hearing.courts.OffenceResults;
import uk.gov.justice.hearing.courts.OffenceResultsDetails;
import uk.gov.moj.cpp.results.domain.event.ImpositionOffenceDetails;
import uk.gov.moj.cpp.results.domain.event.MarkedAggregateSendEmailWhenAccountReceived;
import uk.gov.moj.cpp.results.domain.event.NcesEmailNotificationRequested;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
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

import com.google.common.collect.ImmutableMap;

@SuppressWarnings({"PMD.BeanMembersShouldSerialize"})
public class HearingFinancialResultsAggregate implements Aggregate {

    private static final long serialVersionUID = 7715225910142528288L;

    public static final String STAT_DEC = "STAT_DEC";
    public static final String REOPEN = "REOPEN";
    public static final String APPEAL = "APPEAL";
    public static final String RFSD = "RFSD";
    public static final String WDRN = "WDRN";
    public static final String G = "G";
    public static final String APPEAL_DISMISSED = "APPEAL DISMISSED";
    private static final String ACON = "ACON";
    private static final String ACON_EMAIL_SUBJECT = "ACCOUNTS TO BE CONSOLIDATED";
    private static final String COMMA = ",";

    public static final String WRITE_OFF_ONE_DAY_DEEMED_SERVED = "WRITE OFF ONE DAY DEEMED SERVED";

    private static final Map<String, String> APPLICATION_TYPES = ImmutableMap.<String, String>builder()
            .put(STAT_DEC, "APPLICATION FOR A STATUTORY DECLARATION RECEIVED")
            .put(REOPEN, "APPLICATION TO REOPEN RECEIVED")
            .put(APPEAL, "APPEAL APPLICATION RECEIVED")
            .build();

    private static final Map<String, Map<String, String>> APPLICATION_SUBJECT = ImmutableMap.<String, Map<String, String>>builder()
            .put(STAT_DEC, ImmutableMap.<String, String>builder()
                    .put(RFSD, "STATUTORY DECLARATION REFUSED")
                    .put(WDRN, "STATUTORY DECLARATION WITHDRAWN")
                    .put(G, "STATUTORY DECLARATION GRANTED")
                    .put("STDEC", "STATUTORY DECLARATION GRANTED")
                    .build())
            .put(REOPEN, ImmutableMap.<String, String>builder()
                    .put(RFSD, "APPLICATION TO REOPEN REFUSED")
                    .put(WDRN, "APPLICATION TO REOPEN WITHDRAWN")
                    .put(G, "APPLICATION TO REOPEN GRANTED")
                    .put("ROPENED", "APPLICATION TO REOPEN GRANTED")
                    .build())
            .put(APPEAL, ImmutableMap.<String, String>builder()
                    .put("AACD", APPEAL_DISMISSED)
                    .put("AASD", APPEAL_DISMISSED)
                    .put("ACSD", APPEAL_DISMISSED)
                    .put("ASV", "APPEAL DISMISSED SENTENCE VARIED")
                    .put("APA", "APPEAL ABANDONED")
                    .put(WDRN, "APPEAL WITHDRAWN")
                    .put("AACA", "APPEAL ALLOWED")
                    .put("AASA", "APPEAL ALLOWED")
                    .put("G", "APPEAL GRANTED")
                    .put("ROPENED", "APPEAL ROPENED")
                    .build())
            .build();
    public static final String AMEND_RESULT_INPUT_ERROR = "AMEND RESULT/INPUT ERROR";

    private UUID hearingId;
    private String defendantName;
    private UUID masterDefendantId;
    private String ncesEmail;
    private List<String> prosecutionCaseReferences;

    private final LinkedList<CorrelationIdHistoryItem> correlationIdHistoryItemList = new LinkedList<>();
    private final Map<UUID, OffenceResultsDetails> offenceResultsDetails = new HashMap<>();
    private final List<MarkedAggregateSendEmailWhenAccountReceived> markedAggregateSendEmailWhenAccountReceivedList = new ArrayList<>();

    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(HearingFinancialResultsTracked.class).apply(this::handleFinancialResultsRequest),
                when(HearingFinancialResultsUpdated.class).apply(this::handleHearingFinancialResultsUpdated),
                when(MarkedAggregateSendEmailWhenAccountReceived.class).apply(this::handleMarkedAggregateSendEmailWhenAccountReceived),
                when(UnmarkedAggregateSendEmailWhenAccountReceived.class).apply(this::handleUnmarkedAggregateSendEmailWhenAccountReceived),
                otherwiseDoNothing());
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
                .forEach(correlationIdHistoryItem -> correlationIdHistoryItem.setAccountNumber(hearingFinancialResultsUpdated.getAccountNumber()));
    }

    public Stream<Object> updateFinancialResults(final HearingFinancialResultRequest hearingFinancialResultRequest) {
        final Stream.Builder<Object> builder = Stream.builder();
        final List<MarkedAggregateSendEmailWhenAccountReceived> markedEvents = new ArrayList<>();

        final boolean hasApplicationResult = hasApplicationResult(hearingFinancialResultRequest);

        if (hasApplicationResult && !(hasAmendmentDate(hearingFinancialResultRequest))) {
            requestNcesEmailNotificationForRejectedOrGrantedApplication(hearingFinancialResultRequest).ifPresent(markedEvents::add);
        }

        if (hasCorrelationId(hearingFinancialResultRequest) || hasResultOtherThanApplication(hearingFinancialResultRequest)) {
            if (masterDefendantId != null || hearingFinancialResultRequest.getOffenceResults().stream().anyMatch(OffenceResults::getIsFinancial)) {
                builder.add(
                        HearingFinancialResultsTracked.hearingFinancialResultsTracked()
                                .withHearingFinancialResultRequest(hearingFinancialResultRequest)
                                .withCreatedTime(ZonedDateTime.now())
                                .build());
            }

            appendFinancialResultEvents(hearingFinancialResultRequest, hasApplicationResult, markedEvents);
        }

        markedEvents.stream()
                .filter(event -> Objects.isNull(event.getAccountCorrelationId()) == Objects.isNull(event.getGobAccountNumber()))
                .filter(event -> Objects.isNull(event.getOldAccountCorrelationId()) == Objects.isNull(event.getOldGobAccountNumber()))
                .collect(toList())
                .forEach(e -> {
                    builder.add(buildNcesApplicationMail(e));
                    markedEvents.remove(e);
                });

        markedEvents.forEach(builder::add);
        return apply(builder.build());
    }

    private boolean hasCorrelationId(final HearingFinancialResultRequest hearingFinancialResultRequest) {
        return nonNull(hearingFinancialResultRequest.getAccountCorrelationId());
    }

    private boolean hasApplicationResult(final HearingFinancialResultRequest hearingFinancialResultRequest) {
        return hearingFinancialResultRequest
                .getOffenceResults().stream()
                .anyMatch(offence -> nonNull(offence.getApplicationType()));
    }

    private boolean hasResultOtherThanApplication(final HearingFinancialResultRequest hearingFinancialResultRequest) {
        return hearingFinancialResultRequest
                .getOffenceResults().stream()
                .anyMatch(offence -> isNull(offence.getApplicationType()));
    }

    private boolean isAmendmentProcess(final HearingFinancialResultRequest hearingFinancialResultRequest, final boolean hasApplicationResult) {
        return hasAmendmentDate(hearingFinancialResultRequest)
                || (!hasApplicationResult && hasDeemedServedValueChanged(hearingFinancialResultRequest));
    }

    private boolean hasAmendmentDate(final HearingFinancialResultRequest hearingFinancialResultRequest) {
        return hearingFinancialResultRequest
                .getOffenceResults().stream()
                .anyMatch(o -> nonNull(o.getAmendmentDate()));
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
            return apply(Stream.of(sendNcesEmailNotFound()
                    .withMasterDefendantId(fromString(accountReceived.getMasterDefendantId().toString()))
                    .withSubject(accountReceived.getSubject())
                    .withAmendmentDate(accountReceived.getAmendmentDate())
                    .withAmendmentReason(accountReceived.getAmendmentReason())
                    .withCaseReference(accountReceived.getCaseReferences())
                    .withDateDecisionMade(accountReceived.getDateDecisionMade())
                    .withDefendantName(accountReceived.getDefendantName())
                    .withDivisionCode(accountReceived.getDivisionCode())
                    .withGobAccountNumber(accountReceived.getGobAccountNumber())
                    .withListedDate(accountReceived.getListedDate())
                    .withOldDivisionCode(accountReceived.getOldDivisionCode())
                    .withOldGobAccountNumber(accountReceived.getOldGobAccountNumber())
                    .withImpositionOffenceDetails(accountReceived.getImpositionOffenceDetails())
                    .build()
            ));
        }
        return empty();
    }

    private void appendFinancialResultEvents(final HearingFinancialResultRequest hearingFinancialResultRequest,
                                             final boolean hasApplicationResult,
                                             final List<MarkedAggregateSendEmailWhenAccountReceived> markedEvents) {
        hearingFinancialResultRequest.getOffenceResults().removeIf(result -> nonNull(result.getApplicationType()));

        if (isAmendmentProcess(hearingFinancialResultRequest, hasApplicationResult)) {
            appendAmendmentEvents(hearingFinancialResultRequest, markedEvents);
        } else {
            appendDeemedServedEvents(hearingFinancialResultRequest, markedEvents);
            appendACONEvents(hearingFinancialResultRequest, markedEvents);
        }
    }

    private void appendAmendmentEvents(final HearingFinancialResultRequest hearingFinancialResultRequest,
                                       final List<MarkedAggregateSendEmailWhenAccountReceived> markedEvents) {
        final List<ImpositionOffenceDetails> impositionOffenceDetailsForFinancial = hearingFinancialResultRequest.getOffenceResults().stream()
                .filter(OffenceResults::getIsFinancial)
                .filter(offenceFromRequest -> ofNullable(offenceResultsDetails.get(offenceFromRequest.getOffenceId())).map(OffenceResultsDetails::getIsFinancial).orElse(false))
                .map(this::buildImpositionOffenceDetailsFromRequest)
                .collect(toList());

        final List<ImpositionOffenceDetails> impositionOffenceDetailsForNonFinancial = hearingFinancialResultRequest.getOffenceResults().stream()
                .filter(o -> Objects.nonNull(o.getAmendmentDate()))
                .filter(o -> !o.getIsFinancial())
                .filter(offenceFromRequest -> ofNullable(offenceResultsDetails.get(offenceFromRequest.getOffenceId())).map(OffenceResultsDetails::getIsFinancial).orElse(false))
                .map(this::buildImpositionOffenceDetailsFromRequest)
                .collect(toList());
        if (!impositionOffenceDetailsForNonFinancial.isEmpty()) {
            if (!impositionOffenceDetailsForFinancial.isEmpty()) {
                impositionOffenceDetailsForFinancial.addAll(impositionOffenceDetailsForNonFinancial);
            } else {
                markedEvents.add(buildMarkedAggregateWithoutOldsForSpecificCorrelationId(hearingFinancialResultRequest, AMEND_RESULT_INPUT_ERROR, correlationIdHistoryItemList.peekLast(), impositionOffenceDetailsForNonFinancial));
            }
        }

        if (!impositionOffenceDetailsForFinancial.isEmpty()) {
            markedEvents.add(buildMarkedAggregateWithOlds(hearingFinancialResultRequest, impositionOffenceDetailsForFinancial));
        }

        final List<ImpositionOffenceDetails> impositionOffenceDetailsForDeemed = hearingFinancialResultRequest.getOffenceResults().stream()
                .filter(OffenceResults::getIsDeemedServed)
                .filter(offence -> Objects.nonNull(offence.getAmendmentDate()))
                .map(this::buildImpositionOffenceDetailsFromRequest)
                .collect(toList());
        if (!impositionOffenceDetailsForDeemed.isEmpty()) {
            markedEvents.add(buildMarkedAggregateWithoutOlds(hearingFinancialResultRequest, WRITE_OFF_ONE_DAY_DEEMED_SERVED, impositionOffenceDetailsForDeemed));
        }

        final List<ImpositionOffenceDetails> impositionOffenceDetailsForACON = hearingFinancialResultRequest.getOffenceResults().stream()
                .filter(OffenceResults::getIsFinancial)
                .filter(offence -> ACON.equals(offence.getResultCode()))
                .filter(offence -> Objects.nonNull(offence.getAmendmentDate()))
                .map(this::buildImpositionOffenceDetailsFromRequest)
                .collect(toList());
        if (!impositionOffenceDetailsForACON.isEmpty()) {
            markedEvents.add(buildMarkedAggregateWithoutOlds(hearingFinancialResultRequest, ACON_EMAIL_SUBJECT, impositionOffenceDetailsForACON));
        }
    }

    private void appendDeemedServedEvents(final HearingFinancialResultRequest hearingFinancialResultRequest,
                                          final List<MarkedAggregateSendEmailWhenAccountReceived> markedEvents) {
        final List<ImpositionOffenceDetails> impositionOffenceDetailsForDeemed = hearingFinancialResultRequest.getOffenceResults().stream()
                .filter(OffenceResults::getIsDeemedServed)
                .map(this::buildImpositionOffenceDetailsFromRequest)
                .collect(toList());
        if (!impositionOffenceDetailsForDeemed.isEmpty()) {
            markedEvents.add(buildMarkedAggregateWithoutOlds(hearingFinancialResultRequest, WRITE_OFF_ONE_DAY_DEEMED_SERVED, impositionOffenceDetailsForDeemed));
        }
    }

    private void appendACONEvents(final HearingFinancialResultRequest hearingFinancialResultRequest,
                                  final List<MarkedAggregateSendEmailWhenAccountReceived> markedEvents) {
        final List<ImpositionOffenceDetails> impositionOffenceDetailsForAcon = hearingFinancialResultRequest.getOffenceResults().stream()
                .filter(OffenceResults::getIsFinancial)
                .filter(offence -> ACON.equals(offence.getResultCode()))
                .map(this::buildImpositionOffenceDetailsFromRequest)
                .collect(toList());
        if (!impositionOffenceDetailsForAcon.isEmpty()) {
            markedEvents.add(buildMarkedAggregateWithoutOlds(hearingFinancialResultRequest, ACON_EMAIL_SUBJECT, impositionOffenceDetailsForAcon));
        }
    }

    private void handleFinancialResultsRequest(final HearingFinancialResultsTracked hearingFinancialResultsTracked) {
        final HearingFinancialResultRequest request = hearingFinancialResultsTracked.getHearingFinancialResultRequest();
        this.defendantName = request.getDefendantName();
        this.masterDefendantId = request.getMasterDefendantId();
        this.hearingId = request.getHearingId();
        this.prosecutionCaseReferences = request.getProsecutionCaseReferences();

        if (request.getNcesEmail() != null) {
            this.ncesEmail = request.getNcesEmail();
        }

        updateOffenceResults(request, hearingFinancialResultsTracked.getCreatedTime());
    }

    private void updateOffenceResults(HearingFinancialResultRequest request, final ZonedDateTime createdTime) {
        request.getOffenceResults().forEach(resultFromRequest ->
                this.offenceResultsDetails.put(resultFromRequest.getOffenceId(), buildOffenceResultsDetailsFromOffenceResults(resultFromRequest)));

        if (request.getAccountCorrelationId() != null) {
            correlationIdHistoryItemList.add(CorrelationIdHistoryItem.correlationIdHistoryItem()
                    .withAccountCorrelationId(request.getAccountCorrelationId())
                    .withAccountDivisionCode(request.getAccountDivisionCode())
                    .withCreatedTime(createdTime)
                    .build());
        }

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

    public Stream<Object> sendNcesEmailForNewApplication(final String applicationType, final String listingDate, final List<String> caseUrns) {

        if (masterDefendantId == null) {
            return empty();
        }

        final String subject = APPLICATION_TYPES.get(applicationType);
        final NcesEmailNotificationRequested.Builder ncesEmailNotificationRequested = ncesEmailNotificationRequested()
                .withNotificationId(randomUUID())
                .withSendTo(ncesEmail)
                .withSubject(subject)
                .withDefendantName(defendantName)
                .withCaseReferences(String.join(COMMA, caseUrns))
                .withMasterDefendantId(masterDefendantId)
                .withListedDate(listingDate);

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

        markedAggregateSendEmailWhenAccountReceivedList.stream()
                .filter(marked -> correlationIdHistoryItemList.stream().anyMatch(item -> marked.getAccountCorrelationId().equals(item.getAccountCorrelationId())
                        || Objects.equals(marked.getOldAccountCorrelationId(), item.getAccountCorrelationId())))
                .forEach(marked -> {
                    final String accountNumber = correlationIdHistoryItemList.stream().filter(item -> marked.getAccountCorrelationId().equals(item.getAccountCorrelationId()))
                            .findFirst().map(CorrelationIdHistoryItem::getAccountNumber).orElse(null);

                    marked.setGobAccountNumber(accountNumber);

                    if (nonNull(marked.getOldAccountCorrelationId())) {
                        final String oldAccountNumber = correlationIdHistoryItemList.stream().filter(item -> marked.getOldAccountCorrelationId().equals(item.getAccountCorrelationId()))
                                .findFirst().map(CorrelationIdHistoryItem::getAccountNumber).orElse(null);
                        marked.setOldGobAccountNumber(oldAccountNumber);
                    }

                    if (nonNull(marked.getGobAccountNumber()) && (isNull(marked.getOldAccountCorrelationId()) || nonNull(marked.getOldGobAccountNumber()))) {
                        builder.add(buildNcesApplicationMail(marked));
                        idsToBeUnmarked.add(marked.getId());
                    }

                });

        idsToBeUnmarked.forEach(id -> builder.add(UnmarkedAggregateSendEmailWhenAccountReceived.unmarkedAggregateSendEmailWhenAccountReceived()
                .withId(id)
                .build()));
        return apply(builder.build());
    }


    private Optional<MarkedAggregateSendEmailWhenAccountReceived> requestNcesEmailNotificationForRejectedOrGrantedApplication(final HearingFinancialResultRequest hearingFinancialResultRequest) {

        final Optional<OffenceResults> offenceForApplication = hearingFinancialResultRequest.getOffenceResults().stream()
                .filter(offence -> APPLICATION_TYPES.containsKey(offence.getApplicationType()))
                .filter(offence -> APPLICATION_SUBJECT.get(offence.getApplicationType()).containsKey(offence.getResultCode()))
                .findFirst();


        return offenceForApplication.map(offence -> {
            final List<ImpositionOffenceDetails> impositionOffenceDetailsForApplication = hearingFinancialResultRequest.getOffenceResults().stream()
                    .filter(result -> nonNull(result.getApplicationType()))
                    .map(offenceFromRequest -> offenceResultsDetails.get(offenceFromRequest.getOffenceId()))
                    .filter(Objects::nonNull)
                    .filter(OffenceResultsDetails::getIsFinancial)
                    .map(this::buildImpositionOffenceDetailsFromAggregate)
                    .collect(Collectors.toList());

            if (!impositionOffenceDetailsForApplication.isEmpty()) {
                return Optional.of(buildMarkedAggregateWithoutOldsForSpecificCorrelationIdWithEmail(hearingFinancialResultRequest,
                        APPLICATION_SUBJECT.get(offence.getApplicationType()).get(offence.getResultCode()),
                        correlationIdHistoryItemList.peekLast(), impositionOffenceDetailsForApplication, ncesEmail));
            } else {
                return null;
            }
        }).orElse(Optional.empty());

    }

    public UUID getHearingId() {
        return hearingId;
    }

    public String getDefendantName() {
        return defendantName;
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
        return ncesEmailNotificationRequested()
                .withNotificationId(randomUUID())
                .withDivisionCode(marked.getDivisionCode())
                .withSendTo(marked.getSendTo())
                .withSubject(marked.getSubject())
                .withDefendantName(marked.getDefendantName())
                .withCaseReferences(marked.getCaseReferences())
                .withMasterDefendantId(marked.getMasterDefendantId())
                .withListedDate(marked.getListedDate())
                .withDateDecisionMade(marked.getDateDecisionMade())
                .withImpositionOffenceDetails(marked.getImpositionOffenceDetails())
                .withGobAccountNumber(marked.getGobAccountNumber())
                .withAmendmentDate(marked.getAmendmentDate())
                .withAmendmentReason(marked.getAmendmentReason())
                .withOldDivisionCode(marked.getOldDivisionCode())
                .withOldGobAccountNumber(marked.getOldGobAccountNumber())
                .build();

    }

    private MarkedAggregateSendEmailWhenAccountReceived buildMarkedAggregateWithoutOldsForSpecificCorrelationId(final HearingFinancialResultRequest hearingFinancialResultRequest, final String subject, final CorrelationIdHistoryItem correlationIdHistoryItem, final List<ImpositionOffenceDetails> impositionOffenceDetails) {
        return buildMarkedAggregateWithoutOldsForSpecificCorrelationIdWithEmail(hearingFinancialResultRequest, subject, correlationIdHistoryItem, impositionOffenceDetails,
                ofNullable(hearingFinancialResultRequest.getNcesEmail()).orElse(ncesEmail));
    }

    private MarkedAggregateSendEmailWhenAccountReceived buildMarkedAggregateWithoutOldsForSpecificCorrelationIdWithEmail(final HearingFinancialResultRequest hearingFinancialResultRequest, final String subject, final CorrelationIdHistoryItem correlationIdHistoryItem, final List<ImpositionOffenceDetails> impositionOffenceDetails, final String ncesEMail) {
        final MarkedAggregateSendEmailWhenAccountReceived.Builder builder = markedAggregateSendEmailWhenAccountReceived()
                .withId(randomUUID())
                .withSendTo(ncesEMail)
                .withSubject(subject)
                .withDefendantName(hearingFinancialResultRequest.getDefendantName())
                .withCaseReferences(String.join(COMMA, hearingFinancialResultRequest.getProsecutionCaseReferences()))
                .withMasterDefendantId(hearingFinancialResultRequest.getMasterDefendantId())
                .withAccountCorrelationId(correlationIdHistoryItem.getAccountCorrelationId())
                .withGobAccountNumber(correlationIdHistoryItem.getAccountNumber())
                .withDivisionCode(correlationIdHistoryItem.getAccountDivisionCode())
                .withImpositionOffenceDetails(impositionOffenceDetails);

        if (subject.equals(AMEND_RESULT_INPUT_ERROR)) {
            hearingFinancialResultRequest.getOffenceResults().stream().filter(offence -> nonNull(offence.getAmendmentDate())).findFirst().ifPresent(offence ->
                    builder.withAmendmentDate(offence.getAmendmentDate())
                            .withAmendmentReason(offence.getAmendmentReason())
            );
        } else {
            hearingFinancialResultRequest.getOffenceResults().stream().filter(offence -> nonNull(offence.getDateOfResult())).findFirst().ifPresent(offence ->
                    builder.withDateDecisionMade(offence.getDateOfResult())
            );
        }
        return builder.build();
    }

    private MarkedAggregateSendEmailWhenAccountReceived buildMarkedAggregateWithoutOlds(final HearingFinancialResultRequest hearingFinancialResultRequest, final String subject, final List<ImpositionOffenceDetails> impositionOffenceDetails) {
        final MarkedAggregateSendEmailWhenAccountReceived.Builder builder = markedAggregateSendEmailWhenAccountReceived()
                .withId(randomUUID())
                .withSendTo(ofNullable(hearingFinancialResultRequest.getNcesEmail()).orElse(ncesEmail))
                .withSubject(subject)
                .withDefendantName(hearingFinancialResultRequest.getDefendantName())
                .withCaseReferences(String.join(COMMA, hearingFinancialResultRequest.getProsecutionCaseReferences()))
                .withMasterDefendantId(hearingFinancialResultRequest.getMasterDefendantId())
                .withAccountCorrelationId(hearingFinancialResultRequest.getAccountCorrelationId())
                .withDivisionCode(hearingFinancialResultRequest.getAccountDivisionCode())
                .withImpositionOffenceDetails(impositionOffenceDetails);

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

    private MarkedAggregateSendEmailWhenAccountReceived buildMarkedAggregateWithOlds(final HearingFinancialResultRequest hearingFinancialResultRequest, final List<ImpositionOffenceDetails> impositionOffenceDetails) {
        final CorrelationIdHistoryItem previousItem = correlationIdHistoryItemList.peekLast();
        final Optional<OffenceResults> offenceResult = hearingFinancialResultRequest.getOffenceResults().stream().filter(offence -> nonNull(offence.getAmendmentDate())).findFirst();
        return markedAggregateSendEmailWhenAccountReceived()
                .withId(randomUUID())
                .withSendTo(ofNullable(hearingFinancialResultRequest.getNcesEmail()).orElse(ncesEmail))
                .withSubject(AMEND_RESULT_INPUT_ERROR)
                .withDefendantName(hearingFinancialResultRequest.getDefendantName())
                .withCaseReferences(String.join(COMMA, hearingFinancialResultRequest.getProsecutionCaseReferences()))
                .withMasterDefendantId(hearingFinancialResultRequest.getMasterDefendantId())
                .withAccountCorrelationId(hearingFinancialResultRequest.getAccountCorrelationId())
                .withOldAccountCorrelationId(previousItem.getAccountCorrelationId())
                .withDivisionCode(hearingFinancialResultRequest.getAccountDivisionCode())
                .withOldDivisionCode(previousItem.getAccountDivisionCode())
                .withImpositionOffenceDetails(impositionOffenceDetails)
                .withAmendmentDate(offenceResult.map(OffenceResults::getAmendmentDate).orElse(LocalDate.now().toString()))
                .withAmendmentReason(offenceResult.map(OffenceResults::getAmendmentReason).orElse("Admin error on shared result (a result recorded incorrectly)"))
                .build();
    }

    private ImpositionOffenceDetails buildImpositionOffenceDetailsFromRequest(final OffenceResults offencesFromRequest) {
        return ImpositionOffenceDetails.impositionOffenceDetails()
                .withDetails(offencesFromRequest.getImpositionOffenceDetails())
                .withTitle(offencesFromRequest.getOffenceTitle())
                .build();
    }

    private ImpositionOffenceDetails buildImpositionOffenceDetailsFromAggregate(final OffenceResultsDetails offencesFromAggregate) {
        return ImpositionOffenceDetails.impositionOffenceDetails()
                .withDetails(offencesFromAggregate.getImpositionOffenceDetails())
                .withTitle(offencesFromAggregate.getOffenceTitle())
                .build();
    }
}