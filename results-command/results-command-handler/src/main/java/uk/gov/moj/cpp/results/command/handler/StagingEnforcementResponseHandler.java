package uk.gov.moj.cpp.results.command.handler;

import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.results.domain.aggregate.HearingFinancialResultGobAccountAggregate;
import uk.gov.moj.cpp.results.domain.aggregate.HearingFinancialResultsAggregate;
import uk.gov.moj.cpp.results.domain.event.MarkedAggregateSendEmailWhenAccountReceived;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonString;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.COMMAND_HANDLER)
public class StagingEnforcementResponseHandler extends AbstractCommandHandler {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(StagingEnforcementResponseHandler.class.getName());
    public static final String MASTER_DEFENDANT_ID = "masterDefendantId";
    public static final String CORRELATION_ID = "correlationId";
    public static final String ACCOUNT_NUMBER = "accountNumber";
    public static final String APPLICATION_TYPE = "applicationType";
    public static final String HEARING_COURT_CENTRE_NAME = "hearingCourtCentreName";
    public static final String LISTING_DATE = "listingDate";
    public static final String CASE_URNS = "caseUrns";
    public static final String CASE_OFFENCE_ID_LIST = "caseOffenceIdList";
    public static final String IN_FORMAT = "dd/MM/yyyy";
    public static final String EMPTY_STRING = "";

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;
    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;


    @Inject
    public StagingEnforcementResponseHandler(final EventSource eventSource, final Enveloper enveloper, final AggregateService aggregateService, final ObjectToJsonObjectConverter objectToJsonObjectConverter, final JsonObjectToObjectConverter jsonObjectToObjectConverter) {
        super(eventSource, enveloper, aggregateService);
        this.objectToJsonObjectConverter = objectToJsonObjectConverter;
        this.jsonObjectToObjectConverter = jsonObjectToObjectConverter;
    }


    @Handles("result.command.update-gob-account")
    public void updateEnforcementAcknowledgement(final JsonEnvelope envelope) throws EventStreamException {
        final JsonObject payload = envelope.payloadAsJsonObject();
        final UUID correlationId = fromString(payload.getString(CORRELATION_ID));
        final String accountNumber = payload.getString(ACCOUNT_NUMBER);
        final EventStream eventStream = eventSource.getStreamById(correlationId);
        final HearingFinancialResultGobAccountAggregate aggregate = aggregateService.get(eventStream, HearingFinancialResultGobAccountAggregate.class);
        final Optional<UUID> masterDefendantId = aggregate.getMasterDefendantId();

        if (masterDefendantId.isPresent()) {
            final EventStream eventStreamFinancialResult = eventSource.getStreamById(masterDefendantId.get());
            final HearingFinancialResultsAggregate hearingFinancialResultsAggregate = aggregateService.get(eventStreamFinancialResult, HearingFinancialResultsAggregate.class);

            final Stream<Object> accountUpdateEvents = hearingFinancialResultsAggregate.updateAccountNumber(accountNumber, correlationId);
            final Stream<Object> applicationEvents = hearingFinancialResultsAggregate.checkApplicationEmailAndSend();

            final Stream<Object> eventsToAppend = Stream.of(accountUpdateEvents, applicationEvents).flatMap(stream -> stream);

            eventStreamFinancialResult.append(eventsToAppend.map(enveloper.withMetadataFrom(envelope)));

            LOGGER.info("HearingFinancialResultsAggregate updated for masterDefendantId : {}", masterDefendantId);
        } else {
            LOGGER.error("Could not find masterDefendantId for correlationId : '{}' ", correlationId);
        }

    }

    @Handles("result.command.add-correlation-id")
    public void updateCorrelationID(final JsonEnvelope envelope) throws EventStreamException {
        final JsonObject payload = envelope.payloadAsJsonObject();
        final UUID correlationId = fromString(payload.getString(CORRELATION_ID));
        final UUID masterDefendantId = fromString(payload.getString(MASTER_DEFENDANT_ID));
        final HearingFinancialResultGobAccountAggregate hearingFinancialResultGobAccountAggregate = aggregate(HearingFinancialResultGobAccountAggregate.class, correlationId,
                envelope, a -> a.addGobAccountDefendantId(masterDefendantId, correlationId));
        LOGGER.info("correlationId : {} - HearingFinancialResultGobAccountAggregate updated for masterDefendantId: {}", correlationId, masterDefendantId);
    }

    @Handles("result.command.send-nces-email-for-application")
    public void sendNcesEmailForNewApplication(final JsonEnvelope envelope) throws EventStreamException {
        final String masterDefandantId = envelope.payloadAsJsonObject().getString(MASTER_DEFENDANT_ID);
        LOGGER.info("masterDefendantId : {} - sendNcesEmailForNewApplication: {}", masterDefandantId, envelope.toObfuscatedDebugString());
        final String applicationType = envelope.payloadAsJsonObject().getString(APPLICATION_TYPE);
        final String listingDate = LocalDate.parse(envelope.payloadAsJsonObject().getString(LISTING_DATE),DateTimeFormatter.ofPattern(IN_FORMAT)).toString();
        final List<String> caseUrns = envelope.payloadAsJsonObject().getJsonArray(CASE_URNS).stream().map(i -> ((JsonString) i).getString()).collect(Collectors.toList());
        final List<String> clonedOffenceIdList = envelope.payloadAsJsonObject().containsKey(CASE_OFFENCE_ID_LIST)
                ? envelope.payloadAsJsonObject().getJsonArray(CASE_OFFENCE_ID_LIST).stream().map(i -> ((JsonString) i).getString()).toList()
                : emptyList();
        final String hearingCourtCentreName = envelope.payloadAsJsonObject().containsKey(HEARING_COURT_CENTRE_NAME)
                ? envelope.payloadAsJsonObject().getString(HEARING_COURT_CENTRE_NAME)
                : EMPTY_STRING;
        final HearingFinancialResultsAggregate hearingFinancialResultsAggregate = aggregate(HearingFinancialResultsAggregate.class, fromString(masterDefandantId),
                envelope, a -> a.sendNcesEmailForNewApplication(applicationType, listingDate, caseUrns, hearingCourtCentreName, clonedOffenceIdList));

        LOGGER.info("HearingFinancialResultsAggregate updated for masterDefendantId : {}", masterDefandantId);
    }

    @Handles("results.event.send-nces-email-not-found")
    public void handleSendNcesEmailRejected(final JsonEnvelope envelope) throws EventStreamException {
        final JsonObject payload = envelope.payloadAsJsonObject();
        final MarkedAggregateSendEmailWhenAccountReceived markedAggregateSendEmailWhenAccountReceived = jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), MarkedAggregateSendEmailWhenAccountReceived.class);
        final UUID masterDefendantId = fromString(payload.getString(MASTER_DEFENDANT_ID));
        aggregate(HearingFinancialResultsAggregate.class, masterDefendantId,
                envelope, a -> a.ncesEmailNotFound(markedAggregateSendEmailWhenAccountReceived));
    }

    @Handles("result.command.update-defendant-address-for-application")
    public void handleUpdateDefendantAddressInAggregateForNewApplication(final JsonEnvelope envelope) throws EventStreamException {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("result.command.update-defendant-address-for-application received: {}", envelope.toObfuscatedDebugString());
        }
        if(nonNull(envelope.payloadAsJsonObject()) && nonNull(envelope.payloadAsJsonObject().getJsonObject("courtApplication"))) {
            final CourtApplication courtApplication = jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject().getJsonObject("courtApplication"), CourtApplication.class);
            final UUID masterDefendantId = nonNull(courtApplication.getSubject()) && nonNull(courtApplication.getSubject().getMasterDefendant())
                    ? courtApplication.getSubject().getMasterDefendant().getMasterDefendantId()
                    : null;
            if (nonNull(masterDefendantId)) {
                aggregate(HearingFinancialResultsAggregate.class, masterDefendantId,
                        envelope, a -> a.updateDefendantAddressInAggregate(courtApplication.getSubject().getMasterDefendant()));
            }
        }
    }
}
