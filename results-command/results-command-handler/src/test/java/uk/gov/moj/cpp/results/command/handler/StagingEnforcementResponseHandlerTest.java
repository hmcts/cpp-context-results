package uk.gov.moj.cpp.results.command.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloperWithEvents;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataOf;
import static uk.gov.moj.cpp.results.domain.event.MarkedAggregateSendEmailWhenAccountReceived.markedAggregateSendEmailWhenAccountReceived;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.CaseAddedEvent;
import uk.gov.justice.core.courts.CorrelationIdAndMasterdefendantAdded;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.DefendantAddedEvent;
import uk.gov.justice.core.courts.DefendantAddressUpdatedFromApplication;
import uk.gov.justice.core.courts.DefendantRejectedEvent;
import uk.gov.justice.core.courts.DefendantUpdatedEvent;
import uk.gov.justice.core.courts.HearingApplicationEjected;
import uk.gov.justice.core.courts.HearingCaseEjected;
import uk.gov.justice.core.courts.HearingFinancialResultsUpdated;
import uk.gov.justice.core.courts.HearingResultsAdded;
import uk.gov.justice.core.courts.MasterDefendant;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.PoliceResultGenerated;
import uk.gov.justice.core.courts.SessionAddedEvent;
import uk.gov.justice.core.courts.SjpCaseRejectedEvent;
import uk.gov.justice.core.courts.UnmarkedAggregateSendEmailWhenAccountReceived;
import uk.gov.justice.hearing.courts.HearingFinancialResultRequest;
import uk.gov.justice.hearing.courts.HearingFinancialResultsTracked;
import uk.gov.justice.hearing.courts.OffenceResults;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.moj.cpp.results.domain.aggregate.HearingFinancialResultGobAccountAggregate;
import uk.gov.moj.cpp.results.domain.aggregate.HearingFinancialResultsAggregate;
import uk.gov.moj.cpp.results.domain.event.MarkedAggregateSendEmailWhenAccountReceived;
import uk.gov.moj.cpp.results.domain.event.NcesEmailNotificationRequested;
import uk.gov.moj.cpp.results.domain.event.PoliceNotificationRequested;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonArrayBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class StagingEnforcementResponseHandlerTest {

    @Spy
    private final Enveloper enveloper = createEnveloperWithEvents(
            HearingResultsAdded.class,
            HearingCaseEjected.class,
            HearingApplicationEjected.class,
            SessionAddedEvent.class,
            CaseAddedEvent.class,
            DefendantAddedEvent.class,
            DefendantUpdatedEvent.class,
            PoliceResultGenerated.class,
            DefendantRejectedEvent.class,
            PoliceResultGenerated.class,
            SjpCaseRejectedEvent.class,
            PoliceNotificationRequested.class,
            HearingFinancialResultsUpdated.class,
            CorrelationIdAndMasterdefendantAdded.class,
            MarkedAggregateSendEmailWhenAccountReceived.class,
            UnmarkedAggregateSendEmailWhenAccountReceived.class,
            NcesEmailNotificationRequested.class,
            DefendantAddressUpdatedFromApplication.class
    );

    @Mock
    private EventSource eventSource;

    @Mock
    private AggregateService aggregateService;
    @Mock
    private EventStream eventStream;
    @Mock
    private HearingFinancialResultsAggregate hearingFinancialResultsAggregate;
    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Captor
    private ArgumentCaptor<UUID> eventSourceArgumentCaptor;

    @Captor
    private ArgumentCaptor<Stream<JsonEnvelope>> eventStreamArgumentCaptor;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
    @Spy
    final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);

    @InjectMocks
    private StagingEnforcementResponseHandler stagingEnforcementResponseHandler;

    @BeforeEach
    public void setup() {
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(this.eventSource.getStreamById(any())).thenReturn(this.eventStream);
    }

    @Test
    public void shouldUpdateMasterDefandantId() throws EventStreamException {
        final String oldCorrelationId = randomUUID().toString();

        final String correlationId = randomUUID().toString();
        final String accountNumber = randomUUID().toString();
        final UUID masterDefendantId = randomUUID();

        final JsonEnvelope envelope = envelopeFrom(metadataOf(randomUUID(), "result.command.update-gob-account"),
                Json.createObjectBuilder().add("correlationId", correlationId).add("accountNumber", accountNumber).build());
        HearingFinancialResultGobAccountAggregate hearingFinancialResultGobAccountAggregate = new HearingFinancialResultGobAccountAggregate();
        hearingFinancialResultGobAccountAggregate.apply(CorrelationIdAndMasterdefendantAdded.correlationIdAndMasterdefendantAdded().withMasterDefendantId(masterDefendantId).build());

        HearingFinancialResultsAggregate hearingFinancialResultsAggregate = new HearingFinancialResultsAggregate();
        hearingFinancialResultsAggregate.updateFinancialResults(HearingFinancialResultRequest.hearingFinancialResultRequest()
                .withMasterDefendantId(masterDefendantId)
                .withAccountCorrelationId(UUID.fromString(oldCorrelationId))
                        .withIsSJPHearing(false)
                .withOffenceResults(Arrays.asList(OffenceResults.offenceResults()
                        .withOffenceId(randomUUID())
                        .withIsFinancial(true)
                        .withIsDeemedServed(false)
                        .build()))
                .build(), null, null, null, null, null);

        when(this.aggregateService.get(this.eventStream, HearingFinancialResultGobAccountAggregate.class)).thenReturn(hearingFinancialResultGobAccountAggregate);
        when(this.aggregateService.get(this.eventStream, HearingFinancialResultsAggregate.class)).thenReturn(hearingFinancialResultsAggregate);

        stagingEnforcementResponseHandler.updateEnforcementAcknowledgement(envelope);

        verify(eventSource, times(2)).getStreamById(eventSourceArgumentCaptor.capture());
        verify(eventStream, times(1)).append(eventStreamArgumentCaptor.capture());

        assertThat(eventSourceArgumentCaptor.getAllValues().get(0).toString(), is(correlationId));
        assertThat(eventSourceArgumentCaptor.getAllValues().get(1), is(masterDefendantId));

        JsonEnvelope event = eventStreamArgumentCaptor.getValue().collect(toList()).get(0);

        final JsonEnvelope allValues = envelopeFrom(event.metadata(), event.payload());
        assertThat(allValues,
                jsonEnvelope(
                        metadata().withName("results.event.hearing-financial-results-updated"),
                        payloadIsJson(allOf(
                                withJsonPath("$.accountNumber", is(accountNumber)),
                                withJsonPath("$.masterDefendantId", is(masterDefendantId.toString())),
                                withJsonPath("$.correlationId", is(correlationId))
                        ))));
    }

    @Test
    public void shouldHandleNcesEmailNotAvailable() throws EventStreamException {
        final UUID masterDefendantId = randomUUID();
        final JsonEnvelope envelope = envelopeFrom(metadataOf(randomUUID(), "results.event.send-nces-email-not-found"),
                Json.createObjectBuilder().add("masterDefendantId", masterDefendantId.toString()).build());
        MarkedAggregateSendEmailWhenAccountReceived markedAggregateSendEmailWhenAccountReceived = markedAggregateSendEmailWhenAccountReceived()
                .withMasterDefendantId(masterDefendantId)
                .build();
        when(jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), MarkedAggregateSendEmailWhenAccountReceived.class)).thenReturn(markedAggregateSendEmailWhenAccountReceived);
        when(hearingFinancialResultsAggregate.ncesEmailNotFound(markedAggregateSendEmailWhenAccountReceived)).thenReturn(Stream.empty());
        when(this.aggregateService.get(this.eventStream, HearingFinancialResultsAggregate.class)).thenReturn(hearingFinancialResultsAggregate);
        stagingEnforcementResponseHandler.handleSendNcesEmailRejected(envelope);
        verify(hearingFinancialResultsAggregate).ncesEmailNotFound(markedAggregateSendEmailWhenAccountReceived);
    }

    @Test
    public void shouldAddMasterDefendantIdAndCorrelationIdToAggregate() throws EventStreamException {
        final String correlationId = randomUUID().toString();
        final String masterDefendantId = randomUUID().toString();

        final JsonEnvelope envelope = envelopeFrom(metadataOf(randomUUID(), "result.command.add-correlation-id"),
                Json.createObjectBuilder().add("correlationId", correlationId).add("masterDefendantId", masterDefendantId).build());
        HearingFinancialResultGobAccountAggregate hearingFinancialResultGobAccountAggregate = new HearingFinancialResultGobAccountAggregate();
        when(this.aggregateService.get(this.eventStream, HearingFinancialResultGobAccountAggregate.class)).thenReturn(hearingFinancialResultGobAccountAggregate);

        stagingEnforcementResponseHandler.updateCorrelationID(envelope);

        verify(eventSource, times(1)).getStreamById(eventSourceArgumentCaptor.capture());
        assertThat(eventSourceArgumentCaptor.getAllValues().get(0).toString(), is(correlationId));

        verify(eventStream, times(1)).append(eventStreamArgumentCaptor.capture());
        JsonEnvelope event = eventStreamArgumentCaptor.getValue().collect(toList()).get(0);

        final JsonEnvelope allValues = envelopeFrom(event.metadata(), event.payload());
        assertThat(allValues,
                jsonEnvelope(
                        metadata().withName("results.event.correlation-id-and-masterdefendant-added"),
                        payloadIsJson(allOf(
                                withJsonPath("$.masterDefendantId", is(masterDefendantId.toString())),
                                withJsonPath("$.correlationId", is(correlationId))
                        ))));
    }

    @Test
    public void shouldSendNcesEmailForNewApplication() throws EventStreamException {
        final UUID masterDefendantId = randomUUID();
        final UUID offenceId = randomUUID();
        final ZonedDateTime hearingSittingDay = ZonedDateTimes.fromString("2020-03-07T14:22:00.000Z");

        final JsonEnvelope envelope = envelopeFrom(metadataOf(randomUUID(), "result.command.send-nces-email-for-application"),
                Json.createObjectBuilder()
                        .add("applicationType", "applicationType")
                        .add("masterDefendantId", masterDefendantId.toString())
                        .add("listingDate", "28/12/2021")
                        .add("caseUrns", createCaseUrns())
                        .add("hearingCourtCentreName", "Croydon Crown Court").build());
        HearingFinancialResultsAggregate hearingFinancialResultsAggregate = new HearingFinancialResultsAggregate();
        hearingFinancialResultsAggregate.apply(HearingFinancialResultsTracked.hearingFinancialResultsTracked()
                                                .withCreatedTime(ZonedDateTime.now())
                                                .withHearingFinancialResultRequest(HearingFinancialResultRequest.hearingFinancialResultRequest()
                                                        .withHearingId(randomUUID())
                                                        .withMasterDefendantId(masterDefendantId)
                                                        .withAccountDivisionCode("10")
                                                        .withIsSJPHearing(false)
                                                        .withOffenceResults(getOffenceResults(offenceId)).withHearingSittingDay(hearingSittingDay)
                                                        .build())
                                                .build());
        hearingFinancialResultsAggregate.apply(HearingFinancialResultsTracked.hearingFinancialResultsTracked()
                .withCreatedTime(ZonedDateTime.now())
                .withHearingFinancialResultRequest(HearingFinancialResultRequest.hearingFinancialResultRequest()
                        .withHearingId(randomUUID())
                        .withMasterDefendantId(masterDefendantId)
                        .withAccountCorrelationId(randomUUID())
                        .withIsSJPHearing(false)
                        .withAccountDivisionCode("10")
                        .withAccountNumber("abc")
                        .withHearingSittingDay(hearingSittingDay)
                        .withOffenceResults(getOffenceResults(offenceId))
                        .build())
                .build());

        when(this.aggregateService.get(this.eventStream, HearingFinancialResultsAggregate.class)).thenReturn(hearingFinancialResultsAggregate);

        stagingEnforcementResponseHandler.sendNcesEmailForNewApplication(envelope);

        verify(eventSource, times(1)).getStreamById(eventSourceArgumentCaptor.capture());

        verify(eventStream, times(1)).append(eventStreamArgumentCaptor.capture());
        JsonEnvelope event = eventStreamArgumentCaptor.getValue().collect(toList()).get(0);

        final JsonEnvelope allValues = envelopeFrom(event.metadata(), event.payload());
        assertThat(allValues,
                jsonEnvelope(
                        metadata().withName("results.event.marked-aggregate-send-email-when-account-received"),
                        payloadIsJson(allOf(
                                withJsonPath("$.masterDefendantId", is(masterDefendantId.toString())),
                                withJsonPath("$.listedDate", is("2021-12-28")),
                                withJsonPath("$.hearingSittingDay", is("2020-03-07")),
                                withJsonPath("$.originalDateOfSentence", is("07/03/2020"))
                        ))));
    }

    @Test
    public void shouldSendNcesEmailForNewApplicationWithoutHearingCountCentreName() throws EventStreamException {
        final UUID masterDefendantId = randomUUID();
        final UUID offenceId = randomUUID();
        final ZonedDateTime hearingSittingDay = ZonedDateTimes.fromString("2020-03-07T14:22:00.000Z");

        final JsonEnvelope envelope = envelopeFrom(metadataOf(randomUUID(), "result.command.send-nces-email-for-application"),
                Json.createObjectBuilder()
                        .add("applicationType", "applicationType")
                        .add("masterDefendantId", masterDefendantId.toString())
                        .add("listingDate", "28/12/2021")
                        .add("caseUrns", createCaseUrns()).build());
        HearingFinancialResultsAggregate hearingFinancialResultsAggregate = new HearingFinancialResultsAggregate();
        hearingFinancialResultsAggregate.apply(HearingFinancialResultsTracked.hearingFinancialResultsTracked()
                .withCreatedTime(ZonedDateTime.now())
                .withHearingFinancialResultRequest(HearingFinancialResultRequest.hearingFinancialResultRequest()
                        .withHearingId(randomUUID())
                        .withMasterDefendantId(masterDefendantId)
                        .withAccountDivisionCode("10")
                        .withOffenceResults(getOffenceResults(offenceId)).withHearingSittingDay(hearingSittingDay)
                        .build())
                .build());
        hearingFinancialResultsAggregate.apply(HearingFinancialResultsTracked.hearingFinancialResultsTracked()
                .withCreatedTime(ZonedDateTime.now())
                .withHearingFinancialResultRequest(HearingFinancialResultRequest.hearingFinancialResultRequest()
                        .withHearingId(randomUUID())
                        .withMasterDefendantId(masterDefendantId)
                        .withAccountCorrelationId(randomUUID())
                        .withAccountDivisionCode("10")
                        .withAccountNumber("abc")
                        .withHearingSittingDay(hearingSittingDay)
                        .withOffenceResults(getOffenceResults(offenceId))
                        .build())
                .build());

        when(this.aggregateService.get(this.eventStream, HearingFinancialResultsAggregate.class)).thenReturn(hearingFinancialResultsAggregate);

        stagingEnforcementResponseHandler.sendNcesEmailForNewApplication(envelope);

        verify(eventSource, times(1)).getStreamById(eventSourceArgumentCaptor.capture());

        verify(eventStream, times(1)).append(eventStreamArgumentCaptor.capture());
        JsonEnvelope event = eventStreamArgumentCaptor.getValue().collect(toList()).get(0);

        final JsonEnvelope allValues = envelopeFrom(event.metadata(), event.payload());
        assertThat(allValues,
                jsonEnvelope(
                        metadata().withName("results.event.marked-aggregate-send-email-when-account-received"),
                        payloadIsJson(allOf(
                                withJsonPath("$.masterDefendantId", is(masterDefendantId.toString())),
                                withJsonPath("$.listedDate", is("2021-12-28")),
                                withJsonPath("$.hearingSittingDay", is("2020-03-07")),
                                withJsonPath("$.originalDateOfSentence", is("07/03/2020"))
                        ))));
    }

    @Test
    @SuppressWarnings("java:S2699")
    public void shouldUpdateDefendantAddressInAggregateForNewApplication() throws EventStreamException {
        final MetadataBuilder metadataBuilder = getMetadata("result.command.update-defendant-address-for-application");
        final JsonEnvelope event = envelopeFrom(metadataBuilder,  Json.createObjectBuilder().add("courtApplication",createObjectBuilder().build()).build());
        final UUID masterDefendantId = randomUUID();
        when(jsonObjectToObjectConverter.convert(event.payloadAsJsonObject().getJsonObject("courtApplication"), CourtApplication.class))
                .thenReturn(CourtApplication.courtApplication()
                        .withSubject(CourtApplicationParty.courtApplicationParty()
                                .withMasterDefendant(MasterDefendant.masterDefendant()
                                        .withMasterDefendantId(masterDefendantId)
                                        .withPersonDefendant(PersonDefendant.personDefendant()
                                                .withPersonDetails(Person.person()
                                                        .withAddress(Address.address()
                                                                .withAddress1("Address1 New")
                                                                .withAddress2("Address2 New")
                                                                .withPostcode("Rg1 8KL").build()).build()).build()).build()).build()).build());
        HearingFinancialResultsAggregate hearingFinancialResultsAggregate = new HearingFinancialResultsAggregate();
        hearingFinancialResultsAggregate.updateFinancialResults(HearingFinancialResultRequest.hearingFinancialResultRequest()
                .withMasterDefendantId(masterDefendantId)
                .withDefendantAddress("Flat 2 10 Russell St RG5 8XD")
                        .withIsSJPHearing(false)
                .withOffenceResults(Arrays.asList(OffenceResults.offenceResults()
                        .withOffenceId(randomUUID())
                        .withIsFinancial(true)
                        .withIsDeemedServed(false)
                        .build()))
                .build(), null, null, null, null, null);

        when(this.aggregateService.get(this.eventStream, HearingFinancialResultsAggregate.class)).thenReturn(hearingFinancialResultsAggregate);
        stagingEnforcementResponseHandler.handleUpdateDefendantAddressInAggregateForNewApplication(event);
    }

    private MetadataBuilder getMetadata(final String eventName) {
        return metadataBuilder()
                .withId(randomUUID())
                .withName(eventName)
                .withUserId(randomUUID().toString());
    }

    private JsonArrayBuilder createCaseUrns() {
        final JsonArrayBuilder builder = createArrayBuilder();
        builder.add("URN!").add("URN2").build();
        return builder;
    }

    private List<OffenceResults> getOffenceResults(final UUID offenceId) {
        List<OffenceResults> offenceResults = new ArrayList<>();
        offenceResults.add(OffenceResults.offenceResults().withIsFinancial(true).withOffenceId(offenceId).build());
        return  offenceResults;
    }
}
