package uk.gov.moj.cpp.results.command.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.nio.charset.Charset.defaultCharset;
import static java.util.Optional.of;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createObjectBuilder;
import static javax.json.Json.createReader;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloperWithEvents;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataOf;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.CaseAddedEvent;
import uk.gov.justice.core.courts.CaseDetails;
import uk.gov.justice.core.courts.CourtCentreWithLJA;
import uk.gov.justice.core.courts.DefendantAddedEvent;
import uk.gov.justice.core.courts.DefendantRejectedEvent;
import uk.gov.justice.core.courts.DefendantUpdatedEvent;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingApplicationEjected;
import uk.gov.justice.core.courts.HearingCaseEjected;
import uk.gov.justice.core.courts.HearingResultsAdded;
import uk.gov.justice.core.courts.HearingResultsAddedForDay;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.PoliceResultGenerated;
import uk.gov.justice.core.courts.SessionAddedEvent;
import uk.gov.justice.core.courts.SessionDay;
import uk.gov.justice.core.courts.SjpCaseRejectedEvent;
import uk.gov.justice.hearing.courts.HearingFinancialResultsTracked;
import uk.gov.justice.results.courts.DefendantTrackingStatusUpdated;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.domains.results.shareresults.PublicHearingResulted;
import uk.gov.moj.cpp.results.domain.aggregate.DefendantAggregate;
import uk.gov.moj.cpp.results.domain.aggregate.HearingFinancialResultsAggregate;
import uk.gov.moj.cpp.results.domain.aggregate.ResultsAggregate;
import uk.gov.moj.cpp.results.domain.event.MarkedAggregateSendEmailWhenAccountReceived;
import uk.gov.moj.cpp.results.domain.event.NcesEmailNotificationRequested;
import uk.gov.moj.cpp.results.domain.event.PoliceNotificationRequested;
import uk.gov.moj.cpp.results.test.TestTemplates;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;

import com.google.common.io.Resources;
import org.apache.commons.io.IOUtils;
import org.hamcrest.core.IsNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ResultsCommandHandlerTest {
    private static final String TEMPLATE_PAYLOAD = "json/results.create-results-magistrates-example.json";
    private static final String TEMPLATE_PAYLOAD_1 = "json/results.create-results-without-offence-results.json";
    private static final String TEMPLATE_PAYLOAD_2 = "json/results.update-results-magistrates-example.json";
    private static final String TEMPLATE_PAYLOAD_3 = "json/results.create-results-example-for-sjp-session-rejected.json";
    private static final String TEMPLATE_PAYLOAD_4 = "json/results.command.generate-police-results-for-a-defendant-example.json";
    private static final String TEMPLATE_PAYLOAD_5 = "json/results.create-results-example-sjp.json";
    private static final String TEMPLATE_PAYLOAD_6 = "json/results.update-results-crown-example.json";
    private static final String TEMPLATE_PAYLOAD_7 = "json/results.create-results-crown-example.json";
    private static final String TEMPLATE_PAYLOAD_8 = "json/results.update-results-crown-example-wo-origorganisation.json";
    private static final String TEMPLATE_PAYLOAD_9 = "json/results.track-results-example.json";
    private static final String TEMPLATE_PAYLOAD_10 = "json/results.command.update-defendant-tracking-status.json";
    private static final String PAYLOAD_FOR_POLICE_WITH_ORIGINATING_ORGANISATION = "json/results.create-results-magistrates-example_with_originating_organisation.json";
    private static final String EMAIL = "mail@mail.com";
    private static UUID metadataId;
    public static final String SURREY_POLICE_CPS_ORGANISATION = "A45AA00";
    public static final String SURREY_POLICE_ORIG_ORGANISATION = "045AA00";
    public static final String SUSSEX_POLICE_CPS_ORGANISATION = "A47AA00";
    public static final String SUSSEX_POLICE_ORIG_ORGANISATION = "047AA00";
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
            HearingResultsAddedForDay.class,
            HearingFinancialResultsTracked.class,
            MarkedAggregateSendEmailWhenAccountReceived.class,
            NcesEmailNotificationRequested.class,
            DefendantTrackingStatusUpdated.class
    );

    @Captor
    private ArgumentCaptor<Stream<JsonEnvelope>> streamArgumentCaptor;

    @Spy
    private ResultsAggregate resultsAggregateSpy;

    @InjectMocks
    private HearingFinancialResultsAggregate hearingFinancialResultsAggregateSpy;

    @InjectMocks
    private DefendantAggregate defendantAggregate;

    @Mock
    private EventStream eventStream;

    @Mock
    private EventSource eventSource;

    @Mock
    private AggregateService aggregateService;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private ReferenceDataService referenceDataService;

    private static PublicHearingResulted shareResultsWithMagistratesMessage;
    private static PublicHearingResulted shareResultsWithCrownMessage;


    @InjectMocks
    private ResultsCommandHandler resultsCommandHandler;

    @BeforeClass
    public static void init() {
        shareResultsWithMagistratesMessage = TestTemplates.basicShareResultsTemplate(JurisdictionType.MAGISTRATES);
        shareResultsWithCrownMessage = TestTemplates.basicShareResultsTemplate(JurisdictionType.CROWN);
        metadataId = randomUUID();
    }

    private static JsonObject getPayload(final String path) {
        String request = null;
        try {
            request = Resources.toString(
                    Resources.getResource(path),
                    Charset.defaultCharset()
            );
        } catch (final Exception e) {
            fail("Error consuming file from location " + path);
        }
        final JsonReader reader = Json.createReader(new StringReader(request));
        return reader.readObject();
    }

    private static JsonObject getPayload(final String path, final String oucode) {
        String request = null;
        try {
            final InputStream inputStream = ResultsCommandHandlerTest.class.getClassLoader().getResourceAsStream(path);
            assertThat(inputStream, IsNull.notNullValue());
            request = IOUtils.toString(inputStream, defaultCharset()).replace("OUCODE", oucode);
        } catch (final Exception e) {
            fail("Error consuming file from location " + path);
        }
        final JsonReader reader = createReader(new StringReader(request));
        return reader.readObject();
    }

    @Before
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        when(eventSource.getStreamById(anyObject())).thenReturn(eventStream);
        when(this.eventSource.getStreamById(shareResultsWithMagistratesMessage.getHearing().getId())).thenReturn(this.eventStream);
        when(this.eventSource.getStreamById(shareResultsWithCrownMessage.getHearing().getId())).thenReturn(this.eventStream);

        when(aggregateService.get(any(EventStream.class), any())).thenReturn(resultsAggregateSpy);
    }

    @Test
    public void shouldRaiseHearingResultsAddedWhenSaveShareResults() throws EventStreamException {

        when(this.aggregateService.get(this.eventStream, ResultsAggregate.class)).thenReturn(new ResultsAggregate());

        final JsonEnvelope envelope = envelopeFrom(metadataOf(metadataId, "results.add-hearing-result"),
                objectToJsonObjectConverter.convert(shareResultsWithMagistratesMessage));

        this.resultsCommandHandler.addHearingResult(envelope);

        assertThat(verifyAppendAndGetArgumentFrom(this.eventStream), streamContaining(
                jsonEnvelope(
                        withMetadataEnvelopedFrom(envelope).withName("results.hearing-results-added"),
                        payloadIsJson(allOf(
                                        withJsonPath("$.hearing.id", is(shareResultsWithMagistratesMessage.getHearing().getId().toString()))
                                )
                        ))));
    }

    @Test
    public void shouldRaiseHearingResultsForDayAddedWhenSaveShareResultsForDay() throws EventStreamException {
        final LocalDate hearingDay = LocalDate.now();
        final UUID hearingId = UUID.randomUUID();
        when(this.aggregateService.get(this.eventStream, ResultsAggregate.class)).thenReturn(new ResultsAggregate());

        final JsonEnvelope envelope = envelopeFrom(metadataOf(metadataId, "results.command.add-hearing-result-for-day"),
                Json.createObjectBuilder()
                        .add("hearingDay", hearingDay.toString())
                        .add("hearing", Json.createObjectBuilder()
                                .add("id", hearingId.toString())
                                .build())
                        .build());

        this.resultsCommandHandler.addHearingResultForDay(envelope);

        assertThat(verifyAppendAndGetArgumentFrom(this.eventStream), streamContaining(
                jsonEnvelope(
                        withMetadataEnvelopedFrom(envelope).withName("results.events.hearing-results-added-for-day"),
                        payloadIsJson(allOf(
                                        withJsonPath("$.hearing.id", is(hearingId.toString())),
                                        withJsonPath("$.hearingDay", is(hearingDay.toString()))
                                )
                        ))));

    }

    @Test
    public void shouldHandleCaseOrApplicationEjectedWhenPayloadContainsCaseIdExpectHearingCaseEjectedEvent() throws EventStreamException {

        when(this.aggregateService.get(this.eventStream, ResultsAggregate.class)).thenReturn(new ResultsAggregate());
        final UUID hearingId1 = randomUUID();
        final UUID hearingId2 = randomUUID();
        final UUID caseId = randomUUID();

        shareResultsWithMagistratesMessage.setHearing(Hearing.hearing().withValuesFrom(shareResultsWithMagistratesMessage.getHearing()).withId(hearingId1).build());

        final JsonEnvelope envelop1 = envelopeFrom(metadataOf(metadataId, "results.add-hearing-result"),
                objectToJsonObjectConverter.convert(shareResultsWithMagistratesMessage));
        when(this.eventSource.getStreamById(shareResultsWithMagistratesMessage.getHearing().getId())).thenReturn(this.eventStream);

        this.resultsCommandHandler.addHearingResult(envelop1);

        shareResultsWithMagistratesMessage.setHearing(Hearing.hearing().withValuesFrom(shareResultsWithMagistratesMessage.getHearing()).withId(hearingId2).build());


        final JsonEnvelope envelop2 = envelopeFrom(metadataOf(metadataId, "results.add-hearing-result"),
                objectToJsonObjectConverter.convert(shareResultsWithMagistratesMessage));
        when(this.eventSource.getStreamById(shareResultsWithMagistratesMessage.getHearing().getId())).thenReturn(this.eventStream);

        this.resultsCommandHandler.addHearingResult(envelop2);

        final String CASE_ID = "caseId";
        final String HEARING_IDS = "hearingIds";
        final JsonObject payload = Json.createObjectBuilder()
                .add(HEARING_IDS,
                        Json.createArrayBuilder().add(hearingId1.toString()).add(hearingId2.toString()).build())
                .add(CASE_ID, caseId.toString())
                .build();

        final JsonEnvelope envelope = envelopeFrom(metadataOf(metadataId, "results.case-or-application-ejected"),
                payload);
        when(this.eventSource.getStreamById(any())).thenReturn(this.eventStream);

        this.resultsCommandHandler.handleCaseOrApplicationEjected(envelope);

        verify(eventStream, times(4)).append(streamArgumentCaptor.capture());
        final List<JsonEnvelope> jsonEnvelopeList = convertStreamToEventList(streamArgumentCaptor.getAllValues());
        final List<JsonEnvelope> filterdJsonEnvelopList = jsonEnvelopeList.stream().filter(jsonEnvelope -> jsonEnvelope.metadata().name().equals("results.hearing-case-ejected")).collect(toList());

        final JsonObject hearingCaseEjectedPayload = filterdJsonEnvelopList.get(1).payloadAsJsonObject();
        final Metadata metadata = filterdJsonEnvelopList.get(1).metadata();
        Assert.assertThat(metadata.name(), is("results.hearing-case-ejected"));
        Assert.assertThat(hearingCaseEjectedPayload.getString(CASE_ID), is(caseId.toString()));

    }

    @Test
    public void shouldHandleCaseOrApplicationEjectedWhenPayloadContainsApplicationIdExpectHearingApplicationEjectedEvent() throws EventStreamException {

        when(this.aggregateService.get(this.eventStream, ResultsAggregate.class)).thenReturn(new ResultsAggregate());
        final UUID hearingId1 = randomUUID();
        final UUID hearingId2 = randomUUID();
        final UUID applicationId = randomUUID();

        shareResultsWithMagistratesMessage.setHearing(Hearing.hearing().withValuesFrom(shareResultsWithMagistratesMessage.getHearing()).withId(hearingId1).build());


        final JsonEnvelope envelop1 = envelopeFrom(metadataOf(metadataId, "results.add-hearing-result"),
                objectToJsonObjectConverter.convert(shareResultsWithMagistratesMessage));
        when(this.eventSource.getStreamById(shareResultsWithMagistratesMessage.getHearing().getId())).thenReturn(this.eventStream);

        this.resultsCommandHandler.addHearingResult(envelop1);

        shareResultsWithMagistratesMessage.setHearing(Hearing.hearing().withValuesFrom(shareResultsWithMagistratesMessage.getHearing()).withId(hearingId2).build());


        final JsonEnvelope envelop2 = envelopeFrom(metadataOf(metadataId, "results.add-hearing-result"),
                objectToJsonObjectConverter.convert(shareResultsWithMagistratesMessage));
        when(this.eventSource.getStreamById(shareResultsWithMagistratesMessage.getHearing().getId())).thenReturn(this.eventStream);

        this.resultsCommandHandler.addHearingResult(envelop2);

        final String APPLICATION_ID = "applicationId";
        final String HEARING_IDS = "hearingIds";
        final JsonObject payload = Json.createObjectBuilder()
                .add(HEARING_IDS,
                        Json.createArrayBuilder().add(hearingId1.toString()).add(hearingId2.toString()).build())
                .add(APPLICATION_ID, applicationId.toString())
                .build();

        final JsonEnvelope envelope = envelopeFrom(metadataOf(metadataId, "results.case-or-application-ejected"),
                payload);
        when(this.eventSource.getStreamById(any())).thenReturn(this.eventStream);

        this.resultsCommandHandler.handleCaseOrApplicationEjected(envelope);

        verify(eventStream, times(4)).append(streamArgumentCaptor.capture());
        final List<JsonEnvelope> jsonEnvelopeList = convertStreamToEventList(streamArgumentCaptor.getAllValues());
        final List<JsonEnvelope> filterdJsonEnvelopList = jsonEnvelopeList.stream().filter(jsonEnvelope -> jsonEnvelope.metadata().name().equals("results.hearing-application-ejected")).collect(toList());


        final JsonObject hearingCaseEjectedPayload = filterdJsonEnvelopList.get(1).payloadAsJsonObject();
        final Metadata metadata = filterdJsonEnvelopList.get(1).metadata();
        Assert.assertThat(metadata.name(), is("results.hearing-application-ejected"));
        Assert.assertThat(hearingCaseEjectedPayload.getString(APPLICATION_ID), is(applicationId.toString()));

    }

    @Test
    public void shouldCreateResultCommandForRejectedEvent() throws EventStreamException {
        final JsonObject payload = getPayload(TEMPLATE_PAYLOAD_1);
        final JsonEnvelope envelope = envelopeFrom(metadataOf(metadataId, "results.create-results"), payload);
        final JsonObjectBuilder jsonProsecutorBuilder = createObjectBuilder();
        jsonProsecutorBuilder
                .add("spiOutFlag", true)
                .add("policeFlag", true)
                .add("contactEmailAddress", EMAIL);
        when(referenceDataService.getSpiOutFlagForProsecutionAuthorityCode("someCode")).thenReturn(of(jsonProsecutorBuilder.build()));

        this.resultsCommandHandler.createResult(envelope);
        verify(enveloper, Mockito.times(3)).withMetadataFrom(envelope);
        verify(eventStream, Mockito.times(3)).append(streamArgumentCaptor.capture());
        final List<JsonEnvelope> allValues = convertStreamToEventList(streamArgumentCaptor.getAllValues());

        assertThat(allValues, containsInAnyOrder(
                jsonEnvelope(
                        withMetadataEnvelopedFrom(envelope).withName("results.event.session-added-event"),
                        payloadIsJson(allOf(
                                        withJsonPath("$.id", is(payload.getJsonObject("session").getString("id")))
                                )
                        )),
                jsonEnvelope(
                        withMetadataEnvelopedFrom(envelope).withName("results.event.case-added-event"),
                        payloadIsJson(allOf(
                                        withJsonPath("$.caseId", is("bfd697f8-31ae-4e25-8654-4a10b812f5dd"))
                                )
                        )),
                jsonEnvelope(
                        withMetadataEnvelopedFrom(envelope).withName("results.event.defendant-rejected-event"),
                        payloadIsJson(allOf(
                                        withJsonPath("$.caseId", is("bfd697f8-31ae-4e25-8654-4a10b812f5dd")),
                                        withJsonPath("$.defendantId", is("1db6bdbb-a9ac-435f-acda-b735971daa74"))
                                )
                        ))
        ));

    }

    @Test
    public void shouldCreateResultCommandWhenOriginatingOrganisationStartsWithAIsProvidedForSurreyPolice() throws EventStreamException {
        final JsonObjectBuilder jsonProsecutorBuilder = createObjectBuilder()
                .add("spiOutFlag", true)
                .add("policeFlag", true)
                .add("contactEmailAddress", EMAIL);

        final JsonObject payload = getPayload(PAYLOAD_FOR_POLICE_WITH_ORIGINATING_ORGANISATION, SURREY_POLICE_CPS_ORGANISATION);
        final JsonEnvelope envelope = envelopeFrom(metadataOf(metadataId, "results.create-results"), payload);
        when(referenceDataService.getSpiOutFlagForOriginatingOrganisation(SURREY_POLICE_ORIG_ORGANISATION)).thenReturn(of(jsonProsecutorBuilder.build()));
        createResults(payload, envelope, jsonProsecutorBuilder);
        verify(referenceDataService, times(1)).getSpiOutFlagForOriginatingOrganisation(SURREY_POLICE_ORIG_ORGANISATION);
    }


    @Test
    public void shouldCreateResultCommandWhenOriginatingOrganisationStartsWithAIsProvidedForSussexPolice() throws EventStreamException {
        final JsonObjectBuilder jsonProsecutorBuilder = createObjectBuilder()
                .add("spiOutFlag", true)
                .add("policeFlag", true)
                .add("contactEmailAddress", EMAIL);

        final JsonObject payload = getPayload(PAYLOAD_FOR_POLICE_WITH_ORIGINATING_ORGANISATION, SUSSEX_POLICE_CPS_ORGANISATION);
        final JsonEnvelope envelope = envelopeFrom(metadataOf(metadataId, "results.create-results"), payload);
        when(referenceDataService.getSpiOutFlagForOriginatingOrganisation(SUSSEX_POLICE_ORIG_ORGANISATION)).thenReturn(of(jsonProsecutorBuilder.build()));
        createResults(payload, envelope, jsonProsecutorBuilder);
        verify(referenceDataService, times(1)).getSpiOutFlagForOriginatingOrganisation(SUSSEX_POLICE_ORIG_ORGANISATION);
    }

    @Test
    public void shouldCreateResultCommandWhenOriginatingOrganisationStartsWithZeroForSurreyPolice() throws EventStreamException {
        final JsonObjectBuilder jsonProsecutorBuilder = createObjectBuilder()
                .add("spiOutFlag", true)
                .add("policeFlag", true)
                .add("contactEmailAddress", EMAIL);

        final JsonObject payload = getPayload(PAYLOAD_FOR_POLICE_WITH_ORIGINATING_ORGANISATION, SURREY_POLICE_ORIG_ORGANISATION);
        final JsonEnvelope envelope = envelopeFrom(metadataOf(metadataId, "results.create-results"), payload);
        when(referenceDataService.getSpiOutFlagForOriginatingOrganisation(SURREY_POLICE_ORIG_ORGANISATION)).thenReturn(of(jsonProsecutorBuilder.build()));
        createResults(payload, envelope, jsonProsecutorBuilder);
        verify(referenceDataService, times(1)).getSpiOutFlagForOriginatingOrganisation(SURREY_POLICE_ORIG_ORGANISATION);
    }

    @Test
    public void shouldCreateResultCommandWhenOriginatingOrganisationStartsWithZeroForSussexPolice() throws EventStreamException {
        final JsonObjectBuilder jsonProsecutorBuilder = createObjectBuilder()
                .add("spiOutFlag", true)
                .add("policeFlag", true)
                .add("contactEmailAddress", EMAIL);

        final JsonObject payload = getPayload(PAYLOAD_FOR_POLICE_WITH_ORIGINATING_ORGANISATION, SUSSEX_POLICE_ORIG_ORGANISATION);
        final JsonEnvelope envelope = envelopeFrom(metadataOf(metadataId, "results.create-results"), payload);
        when(referenceDataService.getSpiOutFlagForOriginatingOrganisation(SUSSEX_POLICE_ORIG_ORGANISATION)).thenReturn(of(jsonProsecutorBuilder.build()));
        createResults(payload, envelope, jsonProsecutorBuilder);
        verify(referenceDataService, times(1)).getSpiOutFlagForOriginatingOrganisation(SUSSEX_POLICE_ORIG_ORGANISATION);
    }


    @Test
    public void shouldCreateResultCommandForSjpCaseRejected() throws EventStreamException {
        final ResultsAggregate resultsAggregate = new ResultsAggregate();
        when(aggregateService.get(any(EventStream.class), any())).thenReturn(resultsAggregate);

        final JsonObject payload = getPayload(TEMPLATE_PAYLOAD_3);
        final JsonEnvelope envelope = envelopeFrom(metadataOf(metadataId, "results.create-results"), payload);
        final List<JsonObject> cases = (List<JsonObject>) payload.get("cases");
        final CaseDetails convertedCaseDetails = jsonObjectToObjectConverter.convert(cases.get(0), CaseDetails.class);
        final JsonObject session = payload.getJsonObject("session");
        final CourtCentreWithLJA courtCentre = jsonObjectToObjectConverter.convert(session.getJsonObject("courtCentreWithLJA"), CourtCentreWithLJA.class);
        resultsAggregate.handleSession(fromString(session.getString("id")), courtCentre, (List<SessionDay>) session.get("sessionDays"));
        resultsAggregate.handleCase(convertedCaseDetails);
        resultsAggregate.handleDefendants(convertedCaseDetails, true, of(JurisdictionType.MAGISTRATES), EMAIL, true, Optional.empty());
        this.resultsCommandHandler.createResult(envelope);
        verify(enveloper, Mockito.times(2)).withMetadataFrom(envelope);
        verify(eventStream, Mockito.times(2)).append(streamArgumentCaptor.capture());
        final List<JsonEnvelope> allValues = convertStreamToEventList(streamArgumentCaptor.getAllValues());

        assertThat(allValues, containsInAnyOrder(
                jsonEnvelope(
                        withMetadataEnvelopedFrom(envelope).withName("results.event.sjp-case-rejected-event"),
                        payloadIsJson(allOf(
                                        withJsonPath("$.id", is(envelope.payloadAsJsonObject().getJsonArray("cases").getJsonObject(0).getString("caseId")))
                                )
                        ))
        ));

    }

    @Test
    public void shouldCreateResultCommandCcCase() throws EventStreamException {
        final JsonObjectBuilder jsonProsecutorBuilder = createObjectBuilder()
                .add("spiOutFlag", true)
                .add("policeFlag", true)
                .add("contactEmailAddress", EMAIL);

        final JsonObject payload = getPayload(TEMPLATE_PAYLOAD);
        final JsonEnvelope envelope = envelopeFrom(metadataOf(metadataId, "results.create-results"), payload);
        when(referenceDataService.getSpiOutFlagForOriginatingOrganisation("OUCODE")).thenReturn(of(jsonProsecutorBuilder.build()));
        createResults(payload, envelope, jsonProsecutorBuilder);
    }

    @Test
    public void shouldCreateResultCommandSjpCase() throws EventStreamException {
        final JsonObjectBuilder jsonProsecutorBuilder = createObjectBuilder()
                .add("spiOutFlag", true)
                .add("policeFlag", true)
                .add("contactEmailAddress", EMAIL);
        when(referenceDataService.getSpiOutFlagForProsecutionAuthorityCode(any())).thenReturn(of(jsonProsecutorBuilder.build()));


        final JsonObject payload = getPayload(TEMPLATE_PAYLOAD_5);
        final JsonEnvelope envelope = envelopeFrom(metadataOf(metadataId, "results.create-results"), payload);
        when(referenceDataService.getSpiOutFlagForOriginatingOrganisation("someCode")).thenReturn(of(jsonProsecutorBuilder.build()));
        createResults(payload, envelope, jsonProsecutorBuilder);
    }

    private void createResults(final JsonObject payload, final JsonEnvelope envelope, final JsonObjectBuilder jsonProsecutorBuilder) throws EventStreamException {
        resultsCommandHandler.createResult(envelope);
        final JsonObject session = payload.getJsonObject("session");
        final UUID sessionId = fromString(session.getString("id"));
        final CourtCentreWithLJA courtCentreWithLJA = jsonObjectToObjectConverter.convert(session.getJsonObject("courtCentreWithLJA"), CourtCentreWithLJA.class);
        final List<SessionDay> sessionDays = (List<SessionDay>) session.get("sessionDays");

        final JsonObject caseDetailsJson = payload.getJsonArray("cases").getJsonObject(0);
        final CaseDetails caseDetails = jsonObjectToObjectConverter.convert(caseDetailsJson, CaseDetails.class);
        verify(resultsAggregateSpy).handleSession(eq(sessionId), eq(courtCentreWithLJA), eq(sessionDays));
        verify(resultsAggregateSpy).handleCase(eq(caseDetails));
        verify(resultsAggregateSpy).handleDefendants(eq(caseDetails), anyBoolean(), any(), any(), anyBoolean(), eq(Optional.empty()));
    }

    @Test
    public void shouldUpdateDefendantTrackingStatus() throws EventStreamException, IOException {
        final JsonObject payload = getPayload(TEMPLATE_PAYLOAD_10);
        final JsonEnvelope envelope = envelopeFrom(metadataOf(metadataId, "results.command.update-defendant-tracking-status.json"), payload);

        when(aggregateService.get(any(EventStream.class), any())).thenReturn(defendantAggregate);

        resultsCommandHandler.updateDefendantTrackingStatus(envelope);
        verify(eventStream).append(streamArgumentCaptor.capture());
        final List<JsonEnvelope> jsonEnvelopeList = convertStreamToEventList(streamArgumentCaptor.getAllValues());
        assertThat(jsonEnvelopeList.size(), is(1));
        assertThat(jsonEnvelopeList.get(0).metadata().name(), is("results.events.defendant-tracking-status-updated"));

    }

    @Test
    public void shouldTrackResult() throws EventStreamException {
        final JsonObject payload = getPayload(TEMPLATE_PAYLOAD_9);
        final JsonEnvelope envelope = envelopeFrom(metadataOf(metadataId, "results.command.track-results"), payload);

        when(aggregateService.get(any(EventStream.class), any())).thenReturn(hearingFinancialResultsAggregateSpy);

        resultsCommandHandler.trackResult(envelope);
        verify(eventStream).append(streamArgumentCaptor.capture());
        final List<JsonEnvelope> jsonEnvelopeList = convertStreamToEventList(streamArgumentCaptor.getAllValues());
        assertThat(jsonEnvelopeList.size(), is(1));
        assertThat(jsonEnvelopeList.get(0).metadata().name(), is("results.event.hearing-financial-results-tracked"));
    }

    @Test
    public void shouldUpdateResultCommandWithJurisdictionMagistrates() throws EventStreamException {
        final ResultsAggregate resultsAggregate = new ResultsAggregate();
        when(aggregateService.get(any(EventStream.class), any())).thenReturn(resultsAggregate);

        final JsonObjectBuilder jsonProsecutorBuilder = createObjectBuilder();
        jsonProsecutorBuilder
                .add("spiOutFlag", true)
                .add("policeFlag", true)
                .add("contactEmailAddress", EMAIL);
        when(referenceDataService.getSpiOutFlagForOriginatingOrganisation("OUCODE")).thenReturn(of(jsonProsecutorBuilder.build()));


        final JsonObject payload = getPayload(TEMPLATE_PAYLOAD);
        final JsonEnvelope envelope = envelopeFrom(metadataOf(metadataId, "results.create-results"), payload);
        final List<JsonObject> cases = (List<JsonObject>) payload.get("cases");
        final CaseDetails convertedCaseDetails = jsonObjectToObjectConverter.convert(cases.get(0), CaseDetails.class);
        final JsonObject session = payload.getJsonObject("session");
        final CourtCentreWithLJA courtCentre = jsonObjectToObjectConverter.convert(session.getJsonObject("courtCentreWithLJA"), CourtCentreWithLJA.class);
        resultsAggregate.handleSession(fromString(session.getString("id")), courtCentre, (List<SessionDay>) session.get("sessionDays"));
        resultsAggregate.handleCase(convertedCaseDetails);
        resultsAggregate.handleDefendants(convertedCaseDetails, true, of(JurisdictionType.MAGISTRATES), EMAIL, true, Optional.empty());

        final JsonObject updatePayload = getPayload(TEMPLATE_PAYLOAD_2);
        final JsonEnvelope updateEnvelope = envelopeFrom(metadataOf(metadataId, "results.create-results"), updatePayload);

        this.resultsCommandHandler.createResult(updateEnvelope);
        verify(enveloper, Mockito.times(3)).withMetadataFrom(updateEnvelope);
        verify(eventStream, Mockito.times(3)).append(streamArgumentCaptor.capture());
        final List<JsonEnvelope> allValues = convertStreamToEventList(streamArgumentCaptor.getAllValues());

        assertThat(allValues, containsInAnyOrder(
                jsonEnvelope(
                        withMetadataEnvelopedFrom(envelope).withName("results.event.defendant-updated-event"),
                        payloadIsJson(allOf(
                                        withJsonPath("$.caseId", is("bfd697f8-31ae-4e25-8654-4a10b812f5dd")),
                                        withJsonPath("$.defendant.defendantId", is("1db6bdbb-a9ac-435f-acda-b735971daa74"))
                                )
                        )),
                jsonEnvelope(
                        withMetadataEnvelopedFrom(envelope).withName("results.event.police-result-generated"),
                        payloadIsJson(allOf(
                                        withJsonPath("$.caseId", is("bfd697f8-31ae-4e25-8654-4a10b812f5dd")),
                                        withJsonPath("$.defendant.defendantId", is("1db6bdbb-a9ac-435f-acda-b735971daa74"))
                                )
                        ))
        ));

    }

    @Test
    public void shouldUpdateResultCommandWithJurisdictionCrownAndRaisePoliceNotificationRequested() throws EventStreamException {
        final ResultsAggregate resultsAggregate = new ResultsAggregate();
        when(aggregateService.get(any(EventStream.class), any())).thenReturn(resultsAggregate);

        final JsonObjectBuilder jsonProsecutorBuilder = createObjectBuilder();
        jsonProsecutorBuilder
                .add("spiOutFlag", true)
                .add("policeFlag", true)
                .add("contactEmailAddress", EMAIL);
        when(referenceDataService.getSpiOutFlagForOriginatingOrganisation("OUCODE")).thenReturn(of(jsonProsecutorBuilder.build()));

        final JsonObject payload = getPayload(TEMPLATE_PAYLOAD_7);
        final JsonEnvelope envelope = envelopeFrom(metadataOf(metadataId, "results.create-results"), payload);
        final List<JsonObject> cases = (List<JsonObject>) payload.get("cases");
        final CaseDetails convertedCaseDetails = jsonObjectToObjectConverter.convert(cases.get(0), CaseDetails.class);
        final JsonObject session = payload.getJsonObject("session");
        final CourtCentreWithLJA courtCentre = jsonObjectToObjectConverter.convert(session.getJsonObject("courtCentreWithLJA"), CourtCentreWithLJA.class);
        resultsAggregate.handleSession(fromString(session.getString("id")), courtCentre, (List<SessionDay>) session.get("sessionDays"));
        resultsAggregate.handleCase(convertedCaseDetails);
        resultsAggregate.handleDefendants(convertedCaseDetails, true, of(JurisdictionType.MAGISTRATES), EMAIL, true, Optional.empty());

        final JsonObject updatePayload = getPayload(TEMPLATE_PAYLOAD_6);
        final JsonEnvelope updateEnvelope = envelopeFrom(metadataOf(metadataId, "results.create-results"), updatePayload);

        this.resultsCommandHandler.createResult(updateEnvelope);
        verify(enveloper, Mockito.times(3)).withMetadataFrom(updateEnvelope);
        verify(eventStream, Mockito.times(3)).append(streamArgumentCaptor.capture());
        final List<JsonEnvelope> allValues = convertStreamToEventList(streamArgumentCaptor.getAllValues());

        assertThat(allValues, containsInAnyOrder(
                jsonEnvelope(
                        withMetadataEnvelopedFrom(envelope).withName("results.event.defendant-updated-event"),
                        payloadIsJson(allOf(
                                        withJsonPath("$.caseId", is("bfd697f8-31ae-4e25-8654-4a10b812f5dd")),
                                        withJsonPath("$.defendant.defendantId", is("1db6bdbb-a9ac-435f-acda-b735971daa74"))
                                )
                        )),
                jsonEnvelope(
                        withMetadataEnvelopedFrom(envelope).withName("results.event.police-notification-requested"),
                        payloadIsJson(allOf(
                                        withJsonPath("$.urn", is("123445")),
                                        withJsonPath("$.policeEmailAddress", is(EMAIL))
                                )
                        ))
        ));
    }

    @Test
    public void shouldUpdateResultCommandWithJurisdictionCrownAndRaisePoliceResultGenerated() throws EventStreamException {
        final ResultsAggregate resultsAggregate = new ResultsAggregate();
        when(aggregateService.get(any(EventStream.class), any())).thenReturn(resultsAggregate);

        final JsonObjectBuilder jsonProsecutorBuilder = createObjectBuilder();
        jsonProsecutorBuilder
                .add("spiOutFlag", true)
                .add("policeFlag", false)
                .add("contactEmailAddress", EMAIL);
        when(referenceDataService.getSpiOutFlagForOriginatingOrganisation("OUCODE")).thenReturn(of(jsonProsecutorBuilder.build()));


        final JsonObject payload = getPayload(TEMPLATE_PAYLOAD_7);
        final JsonEnvelope envelope = envelopeFrom(metadataOf(metadataId, "results.create-results"), payload);
        final List<JsonObject> cases = (List<JsonObject>) payload.get("cases");
        final CaseDetails convertedCaseDetails = jsonObjectToObjectConverter.convert(cases.get(0), CaseDetails.class);
        final JsonObject session = payload.getJsonObject("session");
        final CourtCentreWithLJA courtCentre = jsonObjectToObjectConverter.convert(session.getJsonObject("courtCentreWithLJA"), CourtCentreWithLJA.class);
        resultsAggregate.handleSession(fromString(session.getString("id")), courtCentre, (List<SessionDay>) session.get("sessionDays"));
        resultsAggregate.handleCase(convertedCaseDetails);
        resultsAggregate.handleDefendants(convertedCaseDetails, true, of(JurisdictionType.MAGISTRATES), EMAIL, true, Optional.empty());

        final JsonObject updatePayload = getPayload(TEMPLATE_PAYLOAD_6);
        final JsonEnvelope updateEnvelope = envelopeFrom(metadataOf(metadataId, "results.create-results"), updatePayload);

        this.resultsCommandHandler.createResult(updateEnvelope);
        verify(enveloper, Mockito.times(3)).withMetadataFrom(updateEnvelope);
        verify(eventStream, Mockito.times(3)).append(streamArgumentCaptor.capture());
        final List<JsonEnvelope> allValues = convertStreamToEventList(streamArgumentCaptor.getAllValues());

        assertThat(allValues, containsInAnyOrder(
                jsonEnvelope(
                        withMetadataEnvelopedFrom(envelope).withName("results.event.defendant-updated-event"),
                        payloadIsJson(allOf(
                                        withJsonPath("$.caseId", is("bfd697f8-31ae-4e25-8654-4a10b812f5dd")),
                                        withJsonPath("$.defendant.defendantId", is("1db6bdbb-a9ac-435f-acda-b735971daa74"))
                                )
                        ))
        ));

    }

    @Test
    public void shouldGeneratePoliceResultsForDefendantWhenProsecutionAuthorityCodeSet() throws EventStreamException {

        final JsonObjectBuilder jsonProsecutorBuilder = createObjectBuilder();
        jsonProsecutorBuilder
                .add("spiOutFlag", false)
                .add("policeFlag", true)
                .add("contactEmailAddress", EMAIL);
        when(referenceDataService.getSpiOutFlagForOriginatingOrganisation("someCode")).thenReturn(of(jsonProsecutorBuilder.build()));

        final JsonObject policeResultsPayload = getPayload(TEMPLATE_PAYLOAD_4);
        final JsonEnvelope policeResultsEnvelope = envelopeFrom(metadataOf(metadataId, "results.command.generate-police-results-for-a-defendant"), policeResultsPayload);
        resultsCommandHandler.generatePoliceResultsForDefendant(policeResultsEnvelope);
        verify(resultsAggregateSpy).generatePoliceResults(policeResultsPayload.getString("caseId"), policeResultsPayload.getString("defendantId"), Optional.empty());
    }

    @Test
    public void shouldUpdateResultCommandWithProsecutionAuthorityCodeAndWithoutOriginatingOrganisation() throws EventStreamException {
        final ResultsAggregate resultsAggregate = new ResultsAggregate();
        when(aggregateService.get(any(EventStream.class), any())).thenReturn(resultsAggregate);

        final JsonObjectBuilder jsonProsecutorBuilder = createObjectBuilder();
        jsonProsecutorBuilder
                .add("spiOutFlag", true)
                .add("policeFlag", true)
                .add("contactEmailAddress", EMAIL);

        when(referenceDataService.getSpiOutFlagForProsecutionAuthorityCode(any())).thenReturn(of(jsonProsecutorBuilder.build()));

        final JsonObject payload = getPayload(TEMPLATE_PAYLOAD_7);
        final JsonEnvelope envelope = envelopeFrom(metadataOf(metadataId, "results.create-results"), payload);
        final List<JsonObject> cases = (List<JsonObject>) payload.get("cases");
        final CaseDetails convertedCaseDetails = jsonObjectToObjectConverter.convert(cases.get(0), CaseDetails.class);
        final JsonObject session = payload.getJsonObject("session");
        final CourtCentreWithLJA courtCentre = jsonObjectToObjectConverter.convert(session.getJsonObject("courtCentreWithLJA"), CourtCentreWithLJA.class);
        resultsAggregate.handleSession(fromString(session.getString("id")), courtCentre, (List<SessionDay>) session.get("sessionDays"));
        resultsAggregate.handleCase(convertedCaseDetails);
        resultsAggregate.handleDefendants(convertedCaseDetails, true, of(JurisdictionType.MAGISTRATES), EMAIL, true, Optional.empty());

        final JsonObject updatePayload = getPayload(TEMPLATE_PAYLOAD_8);

        final JsonEnvelope updateEnvelope = envelopeFrom(metadataOf(metadataId, "results.create-results"), updatePayload);

        this.resultsCommandHandler.createResult(updateEnvelope);
        verify(enveloper, Mockito.times(3)).withMetadataFrom(updateEnvelope);
        verify(eventStream, Mockito.times(3)).append(streamArgumentCaptor.capture());
        final List<JsonEnvelope> allValues = convertStreamToEventList(streamArgumentCaptor.getAllValues());

        assertThat(allValues, containsInAnyOrder(
                jsonEnvelope(
                        withMetadataEnvelopedFrom(envelope).withName("results.event.defendant-updated-event"),
                        payloadIsJson(allOf(
                                        withJsonPath("$.caseId", is("bfd697f8-31ae-4e25-8654-4a10b812f5dd")),
                                        withJsonPath("$.defendant.defendantId", is("1db6bdbb-a9ac-435f-acda-b735971daa74"))
                                )
                        )),
                jsonEnvelope(
                        withMetadataEnvelopedFrom(envelope).withName("results.event.police-notification-requested"),
                        payloadIsJson(allOf(
                                        withJsonPath("$.urn", is("123445")),
                                        withJsonPath("$.policeEmailAddress", is(EMAIL))
                                )
                        ))
        ));

    }

    private List<JsonEnvelope> convertStreamToEventList(final List<Stream<JsonEnvelope>> listOfStreams) {
        return listOfStreams.stream()
                .flatMap(jsonEnvelopeStream -> jsonEnvelopeStream).collect(toList());
    }
}

