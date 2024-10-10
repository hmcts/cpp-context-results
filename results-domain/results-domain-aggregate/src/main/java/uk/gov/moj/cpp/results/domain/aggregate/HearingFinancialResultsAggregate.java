package uk.gov.moj.cpp.results.domain.aggregate;

import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.builder;
import static java.util.stream.Stream.empty;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.collections.CollectionUtils.isEqualCollection;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.core.courts.HearingFinancialResultsUpdated.hearingFinancialResultsUpdated;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;
import static uk.gov.justice.hearing.courts.OffenceResultsDetails.offenceResultsDetails;
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

import com.google.common.collect.ImmutableMap;
import uk.gov.moj.cpp.results.domain.event.ImpositionOffenceDetails;
import uk.gov.moj.cpp.results.domain.event.MarkedAggregateSendEmailWhenAccountReceived;
import uk.gov.moj.cpp.results.domain.event.NcesEmailNotification;
import uk.gov.moj.cpp.results.domain.event.NcesEmailNotificationRequested;
import uk.gov.moj.cpp.results.domain.event.NewOffenceByResult;
import uk.gov.moj.cpp.results.domain.event.SendNcesEmailNotFound;


@SuppressWarnings({"PMD.BeanMembersShouldSerialize"})
public class HearingFinancialResultsAggregate implements Aggregate {

    private static final long serialVersionUID = 7715225910142528288L;
    private static final String HEARING_SITTING_DAY_PATTERN = "yyyy-MM-dd";

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
    private static final String ROPENED = "ROPENED";
    private static final String FIDICI = "FIDICI";
    private static final String FIDICI_VALUE = "Fined and detained in default of payment until court rises";
    private static final String FIDICTI = "FIDICTI";
    private static final String FIDICTI_VALUE = "Fined and detained in default of payment until time";
    private static final String FIDIPI = "FIDIPI";
    private static final String FIDIPI_VALUE = "Fined and detained in default of payment in police station";
    private static final String STDEC = "STDEC";
    private static final String STDEC_VALUE = "Statutory Declaration";
    private static final String AACA = "AACA";
    private static final String AACA_VALUE = "Appeal against conviction allowed ";
    private static final String AASA = "AASA";
    private static final String AASA_VALUE = "Appeal against sentence allowed";
    private static final String AACD = "AACD";
    private static final String AACD_VALUE = "Appeal against conviction dismissed";
    private static final String AASD = "AASD";
    private static final String AASD_VALUE = "Appeal against sentence dismissed ";
    private static final String ACSD = "ACSD";
    private static final String ACSD_VALUE = "Appeal against conviction and sentence dismissed";
    private static final String ASV = "ASV";
    private static final String ASV_VALUE = "Appeal against conviction dismissed and sentence varied";
    private static final String APA = "APA";
    private static final String APA_VALUE = "Appeal abandoned";
    private static final String G_VALUE = "Granted";
    private static final String ROPENED_VALUE = "Case reopened";
    private static final String RFSD_VALUE = "Application refused";
    private static final String WDRN_VALUE = "Withdrawn";
    private static final String ACON_VALUE = "Account Consolidated";



    public static final String WRITE_OFF_ONE_DAY_DEEMED_SERVED = "WRITE OFF ONE DAY DEEMED SERVED";

    public static final  Map<String, String> resultCodeToString = ImmutableMap.<String, String>builder()
            .put(FIDICI, FIDICI_VALUE )
            .put(FIDICTI, FIDICTI_VALUE)
                    .put(FIDIPI, FIDIPI_VALUE)
                    .put(G, G_VALUE)
                    .put(STDEC, STDEC_VALUE)
                    .put(ROPENED, ROPENED_VALUE)
                    .put(AACA, AACA_VALUE)
                    .put(AASA, AASA_VALUE)
                    .put(RFSD, RFSD_VALUE)
                    .put(WDRN, WDRN_VALUE)
                    .put(AACD, AACD_VALUE)
                    .put(AASD, AASD_VALUE)
                    .put(ACSD, ACSD_VALUE)
                    .put(ASV, ASV_VALUE)
                    .put(APA, APA_VALUE)
                    .put(ACON, ACON_VALUE)
            .build();


    private static final Map<String, String> APPLICATION_TYPES = ImmutableMap.<String, String>builder()
            .put(STAT_DEC, "APPLICATION FOR A STATUTORY DECLARATION RECEIVED")
            .put(REOPEN, "APPLICATION TO REOPEN RECEIVED")
            .put(APPEAL, "APPEAL APPLICATION RECEIVED")
            .build();

    public static final String STATUTORY_DECLARATION_UPDATED  = "STATUTORY DECLARATION UPDATED" ;
    public static final String APPLICATION_TO_REOPEN_UPDATED  = "APPLICATION TO REOPEN UPDATED" ;
    public static final String APPEAL_APPLICATION_UPDATED  = "APPEAL APPLICATION UPDATED";

    public static final Map<String, String> APPLICATION_UPDATED_SUBJECT = ImmutableMap.<String, String>builder()
            .put(STAT_DEC, STATUTORY_DECLARATION_UPDATED)
            .put(REOPEN, APPLICATION_TO_REOPEN_UPDATED)
            .put(APPEAL, APPEAL_APPLICATION_UPDATED)
            .build();


    private static final Map<String, Map<String, String>> APPLICATION_SUBJECT = ImmutableMap.<String, Map<String, String>>builder()
            .put(STAT_DEC, ImmutableMap.<String, String>builder()
                    .put(RFSD, "STATUTORY DECLARATION REFUSED")
                    .put(WDRN, "STATUTORY DECLARATION WITHDRAWN")
                    .put(G, "STATUTORY DECLARATION GRANTED")
                    .put(STDEC, "STATUTORY DECLARATION GRANTED")
                    .build())
            .put(REOPEN, ImmutableMap.<String, String>builder()
                    .put(RFSD, "APPLICATION TO REOPEN REFUSED")
                    .put(WDRN, "APPLICATION TO REOPEN WITHDRAWN")
                    .put(G, "APPLICATION TO REOPEN GRANTED")
                    .put(ROPENED, "APPLICATION TO REOPEN GRANTED")
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
                    .put(ROPENED, "APPEAL ROPENED")
                    .build())
            .build();
    public static final String AMEND_AND_RESHARE = "AMEND AND RESHARE- DUPLICATE ACCOUNT: WRITE OFF REQUIRED";
    public static final String EMPTY_STRING = "";
    public static final String BRITISH_DATE_FORMAT = "dd/MM/yyyy";

    private UUID hearingId;
    private ZonedDateTime hearingSittingDay;
    private String hearingCourtCentreName;
    private String defendantName;
    private String defendantDateOfBirth;
    private String defendantAddress;
    private String defendantEmail;
    private String defendantContactNumber;
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
        return this.updateFinancialResults(hearingFinancialResultRequest, isWrittenOffExists, originalDateOfOffenceList, originalDateOfSentenceList, newResultByOffenceList, applicationResult, new HashMap<>());
    }
    public Stream<Object> updateFinancialResults(final HearingFinancialResultRequest hearingFinancialResultRequest,
                                                 final String isWrittenOffExists,
                                                 final String originalDateOfOffenceList,
                                                 final String originalDateOfSentenceList,
                                                 final List<NewOffenceByResult> newResultByOffenceList,
                                                 final String applicationResult,
                                                 final Map<UUID, String> offenceDateMap) {
        final Stream.Builder<Object> builder = Stream.builder();
        final List<MarkedAggregateSendEmailWhenAccountReceived> markedEvents = new ArrayList<>();

        final boolean hasApplicationResult = hasApplicationResult(hearingFinancialResultRequest);

        if (hasApplicationResult) {
            if (hearingFinancialResultRequest.getOffenceResults().stream()
                    .filter(offence -> APPLICATION_TYPES.containsKey(offence.getApplicationType()))
                    .anyMatch(offence -> APPLICATION_SUBJECT.get(offence.getApplicationType()).containsKey(offence.getResultCode()))){
                requestNcesEmailNotificationForRejectedOrGrantedApplication(hearingFinancialResultRequest, isWrittenOffExists, originalDateOfOffenceList,
                        originalDateOfSentenceList, newResultByOffenceList, applicationResult,offenceDateMap).ifPresent(markedEvents::add);
            } else {
                requestNcesEmailNotificationForUpdatedApplication(hearingFinancialResultRequest, isWrittenOffExists, originalDateOfOffenceList,
                        originalDateOfSentenceList, newResultByOffenceList, applicationResult, offenceDateMap).ifPresent(markedEvents::add);
            }
        }

        if (hasCorrelationId(hearingFinancialResultRequest) || hasResultOtherThanApplication(hearingFinancialResultRequest)) {
            if (masterDefendantId != null || hearingFinancialResultRequest.getOffenceResults().stream().anyMatch(OffenceResults::getIsFinancial)) {
                builder.add(
                        HearingFinancialResultsTracked.hearingFinancialResultsTracked()
                                .withHearingFinancialResultRequest(hearingFinancialResultRequest)
                                .withCreatedTime(ZonedDateTime.now())
                                .build());
            }

            appendFinancialResultEvents(hearingFinancialResultRequest, hasApplicationResult, markedEvents, isWrittenOffExists, originalDateOfOffenceList,
                    originalDateOfSentenceList, newResultByOffenceList, applicationResult, offenceDateMap);
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

    private void appendAmendmentEvents(final HearingFinancialResultRequest hearingFinancialResultRequest,
                                       final List<MarkedAggregateSendEmailWhenAccountReceived> markedEvents,
                                       final String isWrittenOffExists,
                                       final String originalDateOfOffenceList,
                                       final String originalDateOfSentenceList,
                                       final List<NewOffenceByResult> newResultByOffenceList,
                                       final String applicationResult, final Map<UUID, String> offenceDateMap) {
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
                .collect(toList());
        if (!impositionOffenceDetailsForNonFinancial.isEmpty()) {
            if (!impositionOffenceDetailsForFinancial.isEmpty()) {
                impositionOffenceDetailsForFinancial.addAll(impositionOffenceDetailsForNonFinancial);
            } else {
                markedEvents.add(buildMarkedAggregateWithoutOldsForSpecificCorrelationId(hearingFinancialResultRequest, AMEND_AND_RESHARE, correlationIdHistoryItemList.peekLast(), impositionOffenceDetailsForNonFinancial,
                        isWrittenOffExists, originalDateOfOffenceList, originalDateOfSentenceList, newResultByOffenceList, applicationResult));
            }
        }

        if (!impositionOffenceDetailsForFinancial.isEmpty()) {
            markedEvents.add(buildMarkedAggregateWithOlds(hearingFinancialResultRequest, impositionOffenceDetailsForFinancial, applicationResult));
        }

        final List<ImpositionOffenceDetails> impositionOffenceDetailsForDeemed = hearingFinancialResultRequest.getOffenceResults().stream()
                .filter(o -> isNull(o.getApplicationType()))
                .filter(OffenceResults::getIsDeemedServed)
                .filter(offence -> Objects.nonNull(offence.getAmendmentDate()))
                .map(offenceResults -> this.buildImpositionOffenceDetailsFromRequest(offenceResults, offenceDateMap))
                .collect(toList());
        if (!impositionOffenceDetailsForDeemed.isEmpty()) {
            markedEvents.add(buildMarkedAggregateWithoutOlds(hearingFinancialResultRequest, WRITE_OFF_ONE_DAY_DEEMED_SERVED, impositionOffenceDetailsForDeemed));
        }

        final List<ImpositionOffenceDetails> impositionOffenceDetailsForACON = hearingFinancialResultRequest.getOffenceResults().stream()
                .filter(o -> isNull(o.getApplicationType()))
                .filter(OffenceResults::getIsFinancial)
                .filter(offence -> ACON.equals(offence.getResultCode()))
                .filter(offence -> Objects.nonNull(offence.getAmendmentDate()))
                .map(offenceResults -> this.buildImpositionOffenceDetailsFromRequest(offenceResults, offenceDateMap))
                .collect(toList());
        if (!impositionOffenceDetailsForACON.isEmpty()) {
            markedEvents.add(buildMarkedAggregateWithoutOlds(hearingFinancialResultRequest, ACON_EMAIL_SUBJECT, impositionOffenceDetailsForACON));
        }
    }

    private void appendDeemedServedEvents(final HearingFinancialResultRequest hearingFinancialResultRequest,
                                          final List<MarkedAggregateSendEmailWhenAccountReceived> markedEvents, final Map<UUID, String> offenceDateMap) {
        final List<ImpositionOffenceDetails> impositionOffenceDetailsForDeemed = hearingFinancialResultRequest.getOffenceResults().stream()
                .filter(o -> isNull(o.getApplicationType()))
                .filter(OffenceResults::getIsDeemedServed)
                .map(offenceResults -> this.buildImpositionOffenceDetailsFromRequest(offenceResults, offenceDateMap))
                .collect(toList());
        if (!impositionOffenceDetailsForDeemed.isEmpty()) {
            markedEvents.add(buildMarkedAggregateWithoutOlds(hearingFinancialResultRequest, WRITE_OFF_ONE_DAY_DEEMED_SERVED, impositionOffenceDetailsForDeemed));
        }
    }

    private void appendACONEvents(final HearingFinancialResultRequest hearingFinancialResultRequest,
                                  final List<MarkedAggregateSendEmailWhenAccountReceived> markedEvents, final Map<UUID, String> offenceDateMap) {
        final List<ImpositionOffenceDetails> impositionOffenceDetailsForAcon = hearingFinancialResultRequest.getOffenceResults().stream()
                .filter(o -> isNull(o.getApplicationType()))
                .filter(OffenceResults::getIsFinancial)
                .filter(offence -> ACON.equals(offence.getResultCode()))
                .map(offenceResults -> this.buildImpositionOffenceDetailsFromRequest(offenceResults, offenceDateMap))
                .collect(toList());
        if (!impositionOffenceDetailsForAcon.isEmpty()) {
            markedEvents.add(buildMarkedAggregateWithoutOlds(hearingFinancialResultRequest, ACON_EMAIL_SUBJECT, impositionOffenceDetailsForAcon));
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
        this.masterDefendantId = request.getMasterDefendantId();
        this.hearingId = request.getHearingId();
        this.prosecutionCaseReferences = request.getProsecutionCaseReferences();

        if (request.getNcesEmail() != null) {
            this.ncesEmail = request.getNcesEmail();
        }

        updateOffenceResults(request, hearingFinancialResultsTracked.getCreatedTime());
    }

    private void updateOffenceResults(HearingFinancialResultRequest request, final ZonedDateTime createdTime) {
        request.getOffenceResults().stream()
                .filter(result -> isNull(result.getApplicationType()))
                .forEach(resultFromRequest ->
                        this.offenceResultsDetails.put(resultFromRequest.getOffenceId(), buildOffenceResultsDetailsFromOffenceResults(resultFromRequest)));

        if (request.getAccountCorrelationId() != null) {
            correlationIdHistoryItemList.add(CorrelationIdHistoryItem.correlationIdHistoryItem()
                    .withAccountCorrelationId(request.getAccountCorrelationId())
                    .withAccountDivisionCode(request.getAccountDivisionCode())
                    .withCreatedTime(createdTime)
                    .withProsecutionCaseReferences(request.getProsecutionCaseReferences())
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
                .withHearingCourtCentreName(hearingCourtCentreName.isEmpty()?this.hearingCourtCentreName:hearingCourtCentreName)
                .withDefendantName(defendantName)
                .withDefendantDateOfBirth(defendantDateOfBirth)
                .withDefendantAddress(defendantAddress)
                .withDefendantEmail(defendantEmail)
                .withDefendantContactNumber(defendantContactNumber)
                .withCaseReferences(String.join(COMMA, caseUrns))
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
                .filter(marked -> correlationIdHistoryItemList.stream().anyMatch(item ->(item.getAccountCorrelationId().equals( marked.getAccountCorrelationId()))
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


    private Optional<MarkedAggregateSendEmailWhenAccountReceived> requestNcesEmailNotificationForRejectedOrGrantedApplication(final HearingFinancialResultRequest hearingFinancialResultRequest,
                                                                                                                              final String isWrittenOffExists,
                                                                                                                              final String originalDateOfOffenceList,
                                                                                                                              final String originalDateOfSentenceList,
                                                                                                                              final List<NewOffenceByResult> newResultByOffenceList,
                                                                                                                              final String applicationResult,
                                                                                                                              final Map<UUID, String> offenceDateMap) {

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
                    .map(offenceResults -> this.buildImpositionOffenceDetailsFromAggregate(offenceResults, offenceDateMap)).distinct()
                    .collect(Collectors.toList());

            if (!impositionOffenceDetailsForApplication.isEmpty()) {
                return Optional.of(buildMarkedAggregateWithoutOldsForSpecificCorrelationIdWithEmail(hearingFinancialResultRequest,
                        APPLICATION_SUBJECT.get(offence.getApplicationType()).get(offence.getResultCode()),
                        correlationIdHistoryItemList.peekLast(), impositionOffenceDetailsForApplication, ncesEmail, isWrittenOffExists, originalDateOfOffenceList,
                        originalDateOfSentenceList, newResultByOffenceList, applicationResult));
            } else {
                return null;
            }
        }).orElse(Optional.empty());

    }

    private Optional<MarkedAggregateSendEmailWhenAccountReceived> requestNcesEmailNotificationForUpdatedApplication(final HearingFinancialResultRequest hearingFinancialResultRequest,
                                                                                                                              final String isWrittenOffExists,
                                                                                                                              final String originalDateOfOffenceList,
                                                                                                                              final String originalDateOfSentenceList,
                                                                                                                              final List<NewOffenceByResult> newResultByOffenceList,
                                                                                                                              final String applicationResult,
                                                                                                                              final Map<UUID, String> offenceDateMap) {

        final Optional<OffenceResults> offenceForApplication = hearingFinancialResultRequest.getOffenceResults().stream()
                .filter(offence -> APPLICATION_TYPES.containsKey(offence.getApplicationType()))
                .findFirst();


        return offenceForApplication.map(offence -> {
            final List<ImpositionOffenceDetails> impositionOffenceDetailsForApplication = hearingFinancialResultRequest.getOffenceResults().stream()
                    .filter(result -> nonNull(result.getApplicationType()))
                    .map(offenceFromRequest -> offenceResultsDetails.get(offenceFromRequest.getOffenceId()))
                    .filter(Objects::nonNull)
                    .filter(OffenceResultsDetails::getIsFinancial)
                    .map(offenceResults -> this.buildImpositionOffenceDetailsFromAggregate(offenceResults, offenceDateMap)).distinct()
                    .collect(Collectors.toList());

            if (!impositionOffenceDetailsForApplication.isEmpty()) {
                return Optional.of(buildMarkedAggregateWithoutOldsForSpecificCorrelationIdWithEmail(hearingFinancialResultRequest,
                        APPLICATION_UPDATED_SUBJECT.get(offence.getApplicationType()),
                        correlationIdHistoryItemList.peekLast(), impositionOffenceDetailsForApplication, ncesEmail, isWrittenOffExists, originalDateOfOffenceList,
                        originalDateOfSentenceList, newResultByOffenceList, applicationResult));
            } else {
                return null;
            }
        }).orElse(Optional.empty());

    }

    public UUID getHearingId() {
        return hearingId;
    }
    public ZonedDateTime getHearingSittingDay() {  return hearingSittingDay;     }
    public String getHearingCourtCentreName()   {  return hearingCourtCentreName;     }
    public String getDefendantName()            {  return defendantName;     }
    public String getDefendantDateOfBirth()      {
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
            return ncesNotification.withDefendantDateOfBirth(marked.getDefendantDateOfBirth())
                    .build();
        }
        return ncesNotification.withDateDecisionMade(marked.getDateDecisionMade())
                .build();
    }

    private String getFormattedDates(final String dates){
        if(!dates.isEmpty()) {
            final DateTimeFormatter inputFormat = DateTimeFormatter.ofPattern(HEARING_SITTING_DAY_PATTERN);
            final DateTimeFormatter outputFormat = DateTimeFormatter.ofPattern(BRITISH_DATE_FORMAT);
            return Arrays.stream(dates.split(","))
                    .map(date -> LocalDate.parse(date, inputFormat).format(outputFormat))
                    .collect(Collectors.joining(","));
        }
        return "";
    }

    private boolean isThisApplicationUpdated(MarkedAggregateSendEmailWhenAccountReceived marked){
        return APPLICATION_UPDATED_SUBJECT.values().stream()
                .anyMatch(e -> e.equals(marked.getSubject()));
    }

    private List<String> getApplicationGrantedSubjects() {
        return Arrays.asList(APPLICATION_SUBJECT.get(STAT_DEC).get(G),
                APPLICATION_SUBJECT.get(STAT_DEC).get(STDEC),
                APPLICATION_SUBJECT.get(REOPEN).get(G),
                APPLICATION_SUBJECT.get(REOPEN).get(ROPENED),
                APPLICATION_SUBJECT.get(APPEAL).get(G));
    }

    private List<String> getApplicationAppealAllowedSubjects() {
        return Arrays.asList(APPLICATION_SUBJECT.get(APPEAL).get(AACA),
                APPLICATION_SUBJECT.get(APPEAL).get(AASA));
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
                .withOldGobAccountNumber(marked.getOldGobAccountNumber());
    }

    private MarkedAggregateSendEmailWhenAccountReceived buildMarkedAggregateWithoutOldsForSpecificCorrelationId(final HearingFinancialResultRequest hearingFinancialResultRequest, final String subject, final CorrelationIdHistoryItem correlationIdHistoryItem, final List<ImpositionOffenceDetails> impositionOffenceDetails,
                                                                                                                final String isWrittenOffExists,
                                                                                                                final String originalDateOfOffenceList,
                                                                                                                final String originalDateOfSentenceList,
                                                                                                                final List<NewOffenceByResult> newResultByOffenceList,
                                                                                                                final String applicationResult) {
        return buildMarkedAggregateWithoutOldsForSpecificCorrelationIdWithEmail(hearingFinancialResultRequest, subject, correlationIdHistoryItem, impositionOffenceDetails,
                ofNullable(hearingFinancialResultRequest.getNcesEmail()).orElse(ncesEmail), isWrittenOffExists, originalDateOfOffenceList,
                originalDateOfSentenceList, newResultByOffenceList, applicationResult);
    }

    private MarkedAggregateSendEmailWhenAccountReceived buildMarkedAggregateWithoutOldsForSpecificCorrelationIdWithEmail(final HearingFinancialResultRequest hearingFinancialResultRequest, final String subject, final CorrelationIdHistoryItem correlationIdHistoryItem, final List<ImpositionOffenceDetails> impositionOffenceDetails, final String ncesEMail,
                                                                                                                         final String isFinancialPenaltiesWrittenOff,
                                                                                                                         final String originalDateOfOffenceList,
                                                                                                                         final String originalDateOfSentenceList,
                                                                                                                         final List<NewOffenceByResult> newResultByOffence,
                                                                                                                         final String applicationResult) {
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
                .withApplicationResult(applicationResult)
                .withCaseReferences(String.join(COMMA, hearingFinancialResultRequest.getProsecutionCaseReferences()))
                .withMasterDefendantId(hearingFinancialResultRequest.getMasterDefendantId())
                .withAccountCorrelationId(correlationIdHistoryItem.getAccountCorrelationId())
                .withGobAccountNumber(correlationIdHistoryItem.getAccountNumber())
                .withDivisionCode(correlationIdHistoryItem.getAccountDivisionCode())
                .withImpositionOffenceDetails(impositionOffenceDetails);

        ofNullable(hearingFinancialResultRequest.getHearingSittingDay())
                .ifPresent(a -> builder.withHearingSittingDay(a.format(ofPattern(HEARING_SITTING_DAY_PATTERN))));

        if (subject.equals(AMEND_AND_RESHARE)) {
            hearingFinancialResultRequest.getOffenceResults().stream().filter(offence -> nonNull(offence.getAmendmentDate())).findFirst().ifPresent(offence ->
                    builder.withAmendmentDate(offence.getAmendmentDate())
                            .withAmendmentReason(offence.getAmendmentReason())
            );
        } else if (getApplicationGrantedSubjects().contains(subject) || getApplicationAppealAllowedSubjects().contains(subject)){
            builder.withIsFinancialPenaltiesWrittenOff(isFinancialPenaltiesWrittenOff);
            builder.withOriginalDateOfOffence(originalDateOfOffenceList);
            builder.withOriginalDateOfSentence(originalDateOfSentenceList);
            builder.withNewOffenceByResult(newResultByOffence);
            hearingFinancialResultRequest.getOffenceResults().stream().filter(offence -> nonNull(offence.getDateOfResult())).findFirst().ifPresent(offence ->
                    builder.withDateDecisionMade(offence.getDateOfResult()));
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
                .withHearingCourtCentreName(hearingFinancialResultRequest.getHearingCourtCentreName())
                .withDefendantName(hearingFinancialResultRequest.getDefendantName())
                .withDefendantDateOfBirth(hearingFinancialResultRequest.getDefendantDateOfBirth())
                .withDefendantAddress(hearingFinancialResultRequest.getDefendantAddress())
                .withDefendantEmail(hearingFinancialResultRequest.getDefendantEmail())
                .withDefendantContactNumber(hearingFinancialResultRequest.getDefendantContactNumber())
                .withCaseReferences(String.join(COMMA, hearingFinancialResultRequest.getProsecutionCaseReferences()))
                .withMasterDefendantId(hearingFinancialResultRequest.getMasterDefendantId())
                .withAccountCorrelationId(hearingFinancialResultRequest.getAccountCorrelationId())
                .withDivisionCode(hearingFinancialResultRequest.getAccountDivisionCode())
                .withImpositionOffenceDetails(impositionOffenceDetails);

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

    private MarkedAggregateSendEmailWhenAccountReceived buildMarkedAggregateWithOlds(final HearingFinancialResultRequest hearingFinancialResultRequest, final List<ImpositionOffenceDetails> impositionOffenceDetails,
                                                                                     final String applicationResult) {
        CorrelationIdHistoryItem previousItem = correlationIdHistoryItemList.peekLast();
        final LinkedList<CorrelationIdHistoryItem> filteredList = correlationIdHistoryItemList.stream()
                .filter(e -> isNotEmpty(e.getProsecutionCaseReferences())
                        && isNotEmpty(hearingFinancialResultRequest.getProsecutionCaseReferences())
                        && isEqualCollection(e.getProsecutionCaseReferences(), hearingFinancialResultRequest.getProsecutionCaseReferences()))
                .collect(Collectors.toCollection(LinkedList::new));

        if (!isEmpty(filteredList)){
            previousItem = filteredList.peekLast();
        }


        final Optional<OffenceResults> offenceResult = hearingFinancialResultRequest.getOffenceResults().stream().filter(offence -> nonNull(offence.getAmendmentDate())).findFirst();
        final MarkedAggregateSendEmailWhenAccountReceived.Builder builder = markedAggregateSendEmailWhenAccountReceived()
                .withId(randomUUID())
                .withSendTo(ofNullable(hearingFinancialResultRequest.getNcesEmail()).orElse(ncesEmail))
                .withSubject(AMEND_AND_RESHARE)
                .withHearingCourtCentreName(hearingFinancialResultRequest.getHearingCourtCentreName())
                .withDefendantName(hearingFinancialResultRequest.getDefendantName())
                .withDefendantDateOfBirth(hearingFinancialResultRequest.getDefendantDateOfBirth())
                .withDefendantAddress(hearingFinancialResultRequest.getDefendantAddress())
                .withDefendantEmail(hearingFinancialResultRequest.getDefendantEmail())
                .withDefendantContactNumber(hearingFinancialResultRequest.getDefendantContactNumber())
                .withApplicationResult(applicationResult)
                .withCaseReferences(String.join(COMMA, hearingFinancialResultRequest.getProsecutionCaseReferences()))
                .withMasterDefendantId(hearingFinancialResultRequest.getMasterDefendantId())
                .withAccountCorrelationId(hearingFinancialResultRequest.getAccountCorrelationId())
                .withOldAccountCorrelationId(previousItem.getAccountCorrelationId())
                .withDivisionCode(hearingFinancialResultRequest.getAccountDivisionCode())
                .withOldDivisionCode(previousItem.getAccountDivisionCode())
                .withImpositionOffenceDetails(impositionOffenceDetails)
                .withAmendmentDate(offenceResult.map(OffenceResults::getAmendmentDate).orElse(LocalDate.now().toString()))
                .withAmendmentReason(offenceResult.map(OffenceResults::getAmendmentReason).orElse("Admin error on shared result (a result recorded incorrectly)"));

        ofNullable(hearingFinancialResultRequest.getHearingSittingDay())
                .ifPresent(a -> builder.withHearingSittingDay(a.format(ofPattern(HEARING_SITTING_DAY_PATTERN))));
        return builder.build();
    }

    private ImpositionOffenceDetails buildImpositionOffenceDetailsFromRequest(final OffenceResults offencesFromRequest, final Map<UUID,String> offenceDateMap) {
        return ImpositionOffenceDetails.impositionOffenceDetails()
                .withDetails(offencesFromRequest.getImpositionOffenceDetails())
                .withOffenceDate(offenceDateMap.get(offencesFromRequest.getOffenceId()))
                .withTitle(offencesFromRequest.getOffenceTitle())
                .build();
    }

    private ImpositionOffenceDetails buildImpositionOffenceDetailsFromAggregate(final OffenceResultsDetails offencesFromAggregate, final Map<UUID,String> offenceDateMap) {
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
        if(!EMPTY_STRING.equals(addressOnApplication) && !addressOnApplication.equals(defendantAddress)){
           builder.add(DefendantAddressUpdatedFromApplication.defendantAddressUpdatedFromApplication()
                    .withDefendantId(masterDefendant.getMasterDefendantId())
                    .withDefendantAddress(addressOnApplication).build());
           return apply(builder.build());
        }
        return apply(empty());
    }

    private String getDefendantAddressFromApplication(final MasterDefendant masterDefendant) {
        if(nonNull(masterDefendant.getLegalEntityDefendant()) && nonNull(masterDefendant.getLegalEntityDefendant().getOrganisation())){
            return getAddressAsString(masterDefendant.getLegalEntityDefendant().getOrganisation().getAddress());

        }
        if(nonNull(masterDefendant.getPersonDefendant()) && nonNull(masterDefendant.getPersonDefendant().getPersonDetails())){
            return getAddressAsString(masterDefendant.getPersonDefendant().getPersonDetails().getAddress());
        }
        return EMPTY_STRING;
    }

    private String getAddressAsString(final Address address){
        final List<String> addressLines = Arrays.asList(address.getAddress1(), address.getAddress2(), address.getAddress3(), address.getAddress4(), address.getAddress5(), address.getPostcode());
        return addressLines.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" "));
    }
}