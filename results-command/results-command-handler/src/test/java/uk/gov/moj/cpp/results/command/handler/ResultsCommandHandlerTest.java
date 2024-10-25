package uk.gov.moj.cpp.results.command.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.nio.charset.Charset.defaultCharset;
import static java.util.Optional.empty;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
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
import uk.gov.justice.core.courts.CorrelationIdHistoryItem;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationCase;
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
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.PoliceResultGenerated;
import uk.gov.justice.core.courts.SessionAddedEvent;
import uk.gov.justice.core.courts.SessionDay;
import uk.gov.justice.core.courts.SjpCaseRejectedEvent;
import uk.gov.justice.hearing.courts.HearingFinancialResultsTracked;
import uk.gov.justice.hearing.courts.OffenceResultsDetails;
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
import uk.gov.moj.cpp.results.command.handler.utils.FileUtil;
import uk.gov.moj.cpp.results.domain.aggregate.DefendantAggregate;
import uk.gov.moj.cpp.results.domain.aggregate.HearingFinancialResultsAggregate;
import uk.gov.moj.cpp.results.domain.aggregate.ResultsAggregate;
import uk.gov.moj.cpp.results.domain.event.MarkedAggregateSendEmailWhenAccountReceived;
import uk.gov.moj.cpp.results.domain.event.NcesEmailNotificationRequested;
import uk.gov.moj.cpp.results.domain.event.PoliceNotificationRequested;
import uk.gov.moj.cpp.results.domain.event.PoliceNotificationRequestedV2;
import uk.gov.moj.cpp.results.test.TestTemplates;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;

import com.google.common.io.Resources;
import org.apache.commons.io.IOUtils;
import org.hamcrest.core.IsNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
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
    private static final String TEMPLATE_PAYLOAD_9_WITH_GRANTED_APPLICATION = "json/results.track-results-example-with_application-granted.json";
    private static final String TEMPLATE_PAYLOAD_9_WITH_UPDATED_APPLICATION = "json/results.track-results-example-with_application-updated.json";

    private static final String TEMPLATE_PAYLOAD_10 = "json/results.command.update-defendant-tracking-status_for_test.json";
    private static final String TEMPLATE_PAYLOAD_FINANCIAL_PENALTIES_TO_BE_WRITTEN_OFF_TRUE = "json/results.events.hearing-results-added-for-day_financialPenaltiesToBeWrittenOff_true.json";
    private static final String TEMPLATE_RESULT_AGGREGATE_OFFENCES_ARE_EMPTY = "json/result-aggregate-offences-are-empty.json";
    private static final String TEMPLATE_RESULT_AGGREGATE_FOR_NEXH_WITH_LINE_FEED = "json/result-aggregate-offences-for-nexh-with-line-feed.json";
    private static final String TEMPLATE_PAYLOAD_FINANCIAL_PENALTIES_TO_BE_WRITTEN_OFF_TRUE_NULL = "json/results.events.hearing-results-added-for-day_financialPenaltiesToBeWrittenOff_trueWithNullOrderedDate.json";

    private static final String TEMPLATE_PAYLOAD_FINANCIAL_PENALTIES_TO_BE_WRITTEN_OFF_TRUE_WITH_OFFENCE_IS_NULL_AND_JUDICIAL_RESULT_IS_NULL = "json/results.events.hearing-results-added-for-day_financialPenaltiesToBeWrittenOff_true_with_juducial_result_and_offence_is_null.json";
    private static final String TEMPLATE_PAYLOAD_FINANCIAL_PENALTIES_TO_BE_WRITTEN_OFF_TRUE_WITH_OFFENCE_IS_NULL_AND_JUDICIAL_PROMPT_IS_NULL = "json/results.events.hearing-results-added-for-day_financialPenaltiesToBeWrittenOff_true_with_juducial_promt_is_null.json";
    private static final String TEMPLATE_PAYLOAD_11 = "json/results.create-results-court-application.json";
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
            PoliceNotificationRequestedV2.class,
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

    @BeforeAll
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

    @BeforeEach
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldRaiseHearingResultsAddedWhenSaveShareResults() throws EventStreamException {

        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(this.eventSource.getStreamById(shareResultsWithMagistratesMessage.getHearing().getId())).thenReturn(this.eventStream);
        when(aggregateService.get(any(EventStream.class), any())).thenReturn(resultsAggregateSpy);
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

        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(any(EventStream.class), any())).thenReturn(resultsAggregateSpy);

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
    public void shouldRaiseHearingResultsForDayAddedWhenSaveShareResultsForDayForGroupCases() throws EventStreamException {
        final LocalDate hearingDay = LocalDate.now();
        final UUID hearingId = UUID.randomUUID();
        final UUID case1Id = UUID.randomUUID();
        final UUID case2Id = UUID.randomUUID();
        final UUID case3Id = UUID.randomUUID();
        final UUID case4Id = UUID.randomUUID();
        final UUID groupId = UUID.randomUUID();

        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(this.aggregateService.get(this.eventStream, ResultsAggregate.class)).thenReturn(new ResultsAggregate());

        final JsonEnvelope envelope = envelopeFrom(metadataOf(metadataId, "results.command.add-hearing-result-for-day"),
                Json.createObjectBuilder()
                        .add("hearingDay", hearingDay.toString())
                        .add("hearing", Json.createObjectBuilder()
                                .add("id", hearingId.toString())
                                .add("isGroupProceedings", Boolean.TRUE)
                                .add("prosecutionCases", Json.createArrayBuilder()
                                        .add(Json.createObjectBuilder()
                                                .add("id", case1Id.toString())
                                                .add("isCivil", Boolean.TRUE)
                                                .add("groupId", groupId.toString())
                                                .add("isGroupMember", Boolean.TRUE)
                                                .add("isGroupMaster", Boolean.TRUE)
                                                .add("defendants", Json.createArrayBuilder()
                                                        .add(Json.createObjectBuilder()
                                                                .add("id", UUID.randomUUID().toString())
                                                                .add("offences",  Json.createArrayBuilder()
                                                                        .add(Json.createObjectBuilder()
                                                                                .add("id", UUID.randomUUID().toString()))
                                                                        .build())
                                                                .build())
                                                        .build())
                                                .build())
                                        .add(Json.createObjectBuilder()
                                                .add("id", case2Id.toString())
                                                .add("isCivil", Boolean.TRUE)
                                                .add("groupId", groupId.toString())
                                                .add("isGroupMember", Boolean.TRUE)
                                                .add("isGroupMaster", Boolean.FALSE)
                                                .add("defendants", Json.createArrayBuilder()
                                                        .add(Json.createObjectBuilder()
                                                                .add("id", UUID.randomUUID().toString())
                                                                .add("offences",  Json.createArrayBuilder()
                                                                        .add(Json.createObjectBuilder()
                                                                                .add("id", UUID.randomUUID().toString()))
                                                                                .build())
                                                                        .build())
                                                        .build())
                                                .build())
                                        .add(Json.createObjectBuilder()
                                                .add("id", case3Id.toString())
                                                .add("isCivil", Boolean.TRUE)
                                                .add("groupId", groupId.toString())
                                                .add("isGroupMember", Boolean.FALSE)
                                                .add("isGroupMaster", Boolean.FALSE)
                                                .add("defendants", Json.createArrayBuilder()
                                                        .add(Json.createObjectBuilder()
                                                                .add("id", UUID.randomUUID().toString())
                                                                .add("offences",  Json.createArrayBuilder()
                                                                        .add(Json.createObjectBuilder()
                                                                                .add("id", UUID.randomUUID().toString()))
                                                                        .build())
                                                                .build())
                                                        .build())
                                                .build())
                                        .add(Json.createObjectBuilder()
                                                .add("id", case4Id.toString())
                                                .add("isCivil", Boolean.TRUE)
                                                .add("groupId", groupId.toString())
                                                .add("isGroupMember", Boolean.FALSE)
                                                .add("defendants", Json.createArrayBuilder()
                                                        .add(Json.createObjectBuilder()
                                                                .add("id", UUID.randomUUID().toString())
                                                                .add("offences",  Json.createArrayBuilder()
                                                                        .add(Json.createObjectBuilder()
                                                                                .add("id", UUID.randomUUID().toString()))
                                                                        .build())
                                                                .build())
                                                        .build())
                                                .build())
                                        .build())
                                .build())
                        .build());

        this.resultsCommandHandler.addHearingResultForDay(envelope);

        assertThat(verifyAppendAndGetArgumentFrom(this.eventStream), streamContaining(
                jsonEnvelope(
                        withMetadataEnvelopedFrom(envelope).withName("results.events.hearing-results-added-for-day"),
                        payloadIsJson(allOf(
                                withJsonPath("$.hearing.id", is(hearingId.toString())),
                                withJsonPath("$.hearingDay", is(hearingDay.toString())),
                                withJsonPath("$.hearing.isGroupProceedings", is(true)),
                                withJsonPath("$.hearing.prosecutionCases[0].id", is(case1Id.toString())),
                                withJsonPath("$.hearing.prosecutionCases[1].id", is(case2Id.toString())),
                                withJsonPath("$.hearing.prosecutionCases[2].id", is(case3Id.toString())),
                                withJsonPath("$.hearing.prosecutionCases[3].id", is(case4Id.toString())),
                                withJsonPath("$.hearing.prosecutionCases[0].isCivil", is(true)),
                                withJsonPath("$.hearing.prosecutionCases[1].isCivil", is(true)),
                                withJsonPath("$.hearing.prosecutionCases[2].isCivil", is(true)),
                                withJsonPath("$.hearing.prosecutionCases[3].isCivil", is(true)),
                                withJsonPath("$.hearing.prosecutionCases[0].groupId", is(groupId.toString())),
                                withJsonPath("$.hearing.prosecutionCases[1].groupId", is(groupId.toString())),
                                withJsonPath("$.hearing.prosecutionCases[2].groupId", is(groupId.toString())),
                                withJsonPath("$.hearing.prosecutionCases[3].groupId", is(groupId.toString())),
                                withJsonPath("$.hearing.prosecutionCases[0].isGroupMember", is(true)),
                                withJsonPath("$.hearing.prosecutionCases[1].isGroupMember", is(true)),
                                withJsonPath("$.hearing.prosecutionCases[2].isGroupMember", is(false)),
                                withJsonPath("$.hearing.prosecutionCases[3].isGroupMember", is(false)),
                                withJsonPath("$.hearing.prosecutionCases[0].isGroupMember", is(true)),
                                withJsonPath("$.hearing.prosecutionCases[1].isGroupMember", is(true)),
                                withJsonPath("$.hearing.prosecutionCases[2].isGroupMember", is(false)),
                                withJsonPath("$.hearing.prosecutionCases[3].isGroupMember", Objects::isNull)
                        )))));
    }

    @Test
    public void shouldHandleCaseOrApplicationEjectedWhenPayloadContainsCaseIdExpectHearingCaseEjectedEvent() throws EventStreamException {

        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(any(EventStream.class), any())).thenReturn(resultsAggregateSpy);

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
        assertThat(metadata.name(), is("results.hearing-case-ejected"));
        assertThat(hearingCaseEjectedPayload.getString(CASE_ID), is(caseId.toString()));

    }

    @Test
    public void shouldHandleCaseOrApplicationEjectedWhenPayloadContainsApplicationIdExpectHearingApplicationEjectedEvent() throws EventStreamException {

        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(any(EventStream.class), any())).thenReturn(resultsAggregateSpy);
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
        assertThat(metadata.name(), is("results.hearing-application-ejected"));
        assertThat(hearingCaseEjectedPayload.getString(APPLICATION_ID), is(applicationId.toString()));

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

        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(any(EventStream.class), any())).thenReturn(resultsAggregateSpy);
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

        when(eventSource.getStreamById(any())).thenReturn(eventStream);
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

        when(eventSource.getStreamById(any())).thenReturn(eventStream);
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

        when(eventSource.getStreamById(any())).thenReturn(eventStream);
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

        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(referenceDataService.getSpiOutFlagForOriginatingOrganisation(SUSSEX_POLICE_ORIG_ORGANISATION)).thenReturn(of(jsonProsecutorBuilder.build()));
        createResults(payload, envelope, jsonProsecutorBuilder);
        verify(referenceDataService, times(1)).getSpiOutFlagForOriginatingOrganisation(SUSSEX_POLICE_ORIG_ORGANISATION);
    }


    @Test
    public void shouldCreateResultCommandForSjpCaseRejected() throws EventStreamException {
        final ResultsAggregate resultsAggregate = new ResultsAggregate();

        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(any(EventStream.class), any())).thenReturn(resultsAggregate);

        final JsonObject payload = getPayload(TEMPLATE_PAYLOAD_3);
        final JsonEnvelope envelope = envelopeFrom(metadataOf(metadataId, "results.create-results"), payload);
        final List<JsonObject> cases = (List<JsonObject>) payload.get("cases");
        final CaseDetails convertedCaseDetails = jsonObjectToObjectConverter.convert(cases.get(0), CaseDetails.class);
        final JsonObject session = payload.getJsonObject("session");
        final CourtCentreWithLJA courtCentre = jsonObjectToObjectConverter.convert(session.getJsonObject("courtCentreWithLJA"), CourtCentreWithLJA.class);
        resultsAggregate.handleSession(fromString(session.getString("id")), courtCentre, (List<SessionDay>) session.get("sessionDays"));
        resultsAggregate.handleCase(convertedCaseDetails);
        resultsAggregate.handleDefendants(convertedCaseDetails, true, of(JurisdictionType.MAGISTRATES), EMAIL, true, empty(), "", "");
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

        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(referenceDataService.getSpiOutFlagForOriginatingOrganisation("OUCODE")).thenReturn(of(jsonProsecutorBuilder.build()));
        createResults(payload, envelope, jsonProsecutorBuilder);
    }

    @Test
    public void shouldCreateResultCommandSjpCase() throws EventStreamException {
        final JsonObjectBuilder jsonProsecutorBuilder = createObjectBuilder()
                .add("spiOutFlag", true)
                .add("policeFlag", true)
                .add("contactEmailAddress", EMAIL);

        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(referenceDataService.getSpiOutFlagForProsecutionAuthorityCode(any())).thenReturn(of(jsonProsecutorBuilder.build()));


        final JsonObject payload = getPayload(TEMPLATE_PAYLOAD_5);
        final JsonEnvelope envelope = envelopeFrom(metadataOf(metadataId, "results.create-results"), payload);
        createResults(payload, envelope, jsonProsecutorBuilder);
    }

    private void createResults(final JsonObject payload, final JsonEnvelope envelope, final JsonObjectBuilder jsonProsecutorBuilder) throws EventStreamException {

        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(any(EventStream.class), any())).thenReturn(resultsAggregateSpy);

        resultsCommandHandler.createResult(envelope);
        final JsonObject session = payload.getJsonObject("session");
        final UUID sessionId = fromString(session.getString("id"));
        final CourtCentreWithLJA courtCentreWithLJA = jsonObjectToObjectConverter.convert(session.getJsonObject("courtCentreWithLJA"), CourtCentreWithLJA.class);
        final List<SessionDay> sessionDays = (List<SessionDay>) session.get("sessionDays");

        final JsonObject caseDetailsJson = payload.getJsonArray("cases").getJsonObject(0);
        final CaseDetails caseDetails = jsonObjectToObjectConverter.convert(caseDetailsJson, CaseDetails.class);
        verify(resultsAggregateSpy).handleSession(eq(sessionId), eq(courtCentreWithLJA), eq(sessionDays));
        verify(resultsAggregateSpy).handleCase(eq(caseDetails));
        verify(resultsAggregateSpy).handleDefendants(eq(caseDetails), anyBoolean(), any(), any(), anyBoolean(), eq(empty()), any(), any());
    }

     @Test
    public void shouldUpdateDefendantTrackingStatus() throws EventStreamException, IOException {
        final JsonObject payload = getPayload(TEMPLATE_PAYLOAD_10);
        final JsonEnvelope envelope = envelopeFrom(metadataOf(metadataId, "results.command.update-defendant-tracking-status-example.json"), payload);

        when(eventSource.getStreamById(any())).thenReturn(eventStream);
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

        final JsonObject hearingPayload = getPayload(TEMPLATE_PAYLOAD_FINANCIAL_PENALTIES_TO_BE_WRITTEN_OFF_TRUE);
        Hearing hearingObject = jsonObjectToObjectConverter.convert((JsonObject) hearingPayload.get("hearing"), Hearing.class);
        when(this.resultsAggregateSpy.getHearing()).thenReturn(hearingObject);

        Offence offence = hearingObject.getCourtApplications().stream()
                .map(CourtApplication::getCourtApplicationCases).flatMap(List::stream)
                .filter(courtApplicationCase -> Objects.nonNull(courtApplicationCase.getOffences()))
                .map(CourtApplicationCase::getOffences).flatMap(List::stream).collect(toList()).get(0);
        Map<UUID, OffenceResultsDetails> offenceResultsDetails = Collections.singletonMap(UUID.randomUUID(), OffenceResultsDetails.offenceResultsDetails()
                .withOffenceId(offence.getId()).build());
        setField(this.hearingFinancialResultsAggregateSpy, "offenceResultsDetails", offenceResultsDetails);

        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(this.eventStream, HearingFinancialResultsAggregate.class)).thenReturn(hearingFinancialResultsAggregateSpy);
        when(aggregateService.get(this.eventStream, ResultsAggregate.class)).thenReturn(resultsAggregateSpy);

        resultsCommandHandler.trackResult(envelope);
        verify(eventStream).append(streamArgumentCaptor.capture());
        final List<JsonEnvelope> jsonEnvelopeList = convertStreamToEventList(streamArgumentCaptor.getAllValues());
        assertThat(jsonEnvelopeList.size(), is(1));
        assertThat(jsonEnvelopeList.get(0).metadata().name(), is("results.event.hearing-financial-results-tracked"));
    }

    @Test
    public void shouldTrackResultForNullOrderedDate() throws EventStreamException {
        final JsonObject payload = getPayload(TEMPLATE_PAYLOAD_9);
        final JsonEnvelope envelope = envelopeFrom(metadataOf(metadataId, "results.command.track-results"), payload);

        final JsonObject hearingPayload = getPayload(TEMPLATE_PAYLOAD_FINANCIAL_PENALTIES_TO_BE_WRITTEN_OFF_TRUE_NULL);
        Hearing hearingObject = jsonObjectToObjectConverter.convert((JsonObject) hearingPayload.get("hearing"), Hearing.class);
        when(this.resultsAggregateSpy.getHearing()).thenReturn(hearingObject);

        Offence offence = hearingObject.getCourtApplications().stream()
                .map(CourtApplication::getCourtApplicationCases).flatMap(List::stream)
                .filter(courtApplicationCase -> Objects.nonNull(courtApplicationCase.getOffences()))
                .map(CourtApplicationCase::getOffences).flatMap(List::stream).collect(toList()).get(0);
        Map<UUID, OffenceResultsDetails> offenceResultsDetails = Collections.singletonMap(UUID.randomUUID(), OffenceResultsDetails.offenceResultsDetails()
                .withOffenceId(offence.getId()).build());
        setField(this.hearingFinancialResultsAggregateSpy, "offenceResultsDetails", offenceResultsDetails);

        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(this.eventStream, HearingFinancialResultsAggregate.class)).thenReturn(hearingFinancialResultsAggregateSpy);
        when(aggregateService.get(this.eventStream, ResultsAggregate.class)).thenReturn(resultsAggregateSpy);

        resultsCommandHandler.trackResult(envelope);
        verify(eventStream).append(streamArgumentCaptor.capture());
        final List<JsonEnvelope> jsonEnvelopeList = convertStreamToEventList(streamArgumentCaptor.getAllValues());
        assertThat(jsonEnvelopeList.size(), is(1));
        assertThat(jsonEnvelopeList.get(0).metadata().name(), is("results.event.hearing-financial-results-tracked"));
    }
    @Test
    public void shouldTrackResultWhenApplicationIsGranted() throws Exception {

        when(eventSource.getStreamById(any())).thenReturn(eventStream);

        final String offenceId = UUID.randomUUID().toString();

        final String payload = FileUtil.getPayload(TEMPLATE_PAYLOAD_9_WITH_GRANTED_APPLICATION).replaceAll("OFFENCE_ID", offenceId);
        final JsonEnvelope envelope = envelopeFrom(metadataOf(metadataId, "results.command.track-results"),
                                                                FileUtil.convertStringToJson(payload));

        final JsonObject hearingPayload = getPayload(TEMPLATE_PAYLOAD_FINANCIAL_PENALTIES_TO_BE_WRITTEN_OFF_TRUE);
        Hearing hearingObject = jsonObjectToObjectConverter.convert((JsonObject) hearingPayload.get("hearing"), Hearing.class);
        when(this.resultsAggregateSpy.getHearing()).thenReturn(hearingObject);

        Offence offence = hearingObject.getCourtApplications().stream()
                .map(CourtApplication::getCourtApplicationCases).flatMap(List::stream)
                .filter(courtApplicationCase -> Objects.nonNull(courtApplicationCase.getOffences()))
                .map(CourtApplicationCase::getOffences).flatMap(List::stream).collect(toList()).get(0);

        Map<UUID, OffenceResultsDetails> offenceResultsDetails = Collections.singletonMap(
                UUID.fromString(offenceId),
                OffenceResultsDetails.offenceResultsDetails()
                        .withOffenceId(offence.getId())
                        .withImpositionOffenceDetails("ImpositionOffenceDetails")
                        .withOffenceTitle("OffenceTitle")
                        .withIsFinancial(true).build());

        CorrelationIdHistoryItem correlationIdHistoryItem = CorrelationIdHistoryItem.correlationIdHistoryItem()
                .withAccountCorrelationId(randomUUID())
                .withAccountNumber("AccountNumber")
                .withAccountDivisionCode("Code").build();
        LinkedList<CorrelationIdHistoryItem> list = new LinkedList<>();
        list.add(correlationIdHistoryItem);

        setField(this.hearingFinancialResultsAggregateSpy, "offenceResultsDetails", offenceResultsDetails);
        setField(this.hearingFinancialResultsAggregateSpy, "correlationIdHistoryItemList", list);

        when(aggregateService.get(this.eventStream, HearingFinancialResultsAggregate.class)).thenReturn(hearingFinancialResultsAggregateSpy);
        when(aggregateService.get(this.eventStream, ResultsAggregate.class)).thenReturn(resultsAggregateSpy);

        resultsCommandHandler.trackResult(envelope);
        verify(eventStream).append(streamArgumentCaptor.capture());
        final List<JsonEnvelope> jsonEnvelopeList = convertStreamToEventList(streamArgumentCaptor.getAllValues());
        assertThat(jsonEnvelopeList.size(), is(2));
        assertThat(jsonEnvelopeList.get(0).metadata().name(), is("results.event.hearing-financial-results-tracked"));

        String s = "PDATE - Pay by date\\nPay by date. Date to pay in full by: 22/12/2023.\",\"title\":\"Keep a vehicle without a valid vehicle licence\"}]";
        assertThat(jsonEnvelopeList.get(1).metadata().name(), is("results.event.nces-email-notification-requested"));
        assertThat(jsonEnvelopeList.get(1).payloadAsJsonObject().getString("subject"), is ("STATUTORY DECLARATION GRANTED"));
        assertThat(jsonEnvelopeList.get(1).payloadAsJsonObject().getString("dateDecisionMade"), is ("01-01-2021"));
        assertThat(jsonEnvelopeList.get(1).payloadAsJsonObject().getString("defendantName"), is ("defendantName"));
        assertTrue(jsonEnvelopeList.get(1).payloadAsJsonObject().get("newOffenceByResult").toString().contains(s));
        assertThat(jsonEnvelopeList.get(1).payloadAsJsonObject().getString("originalDateOfOffence"), is ("2023-11-22"));
        assertThat(jsonEnvelopeList.get(1).payloadAsJsonObject().getString("originalDateOfSentence") , is ("2023-11-22"));
    }

    @Test
    public void shouldTrackResultWhenApplicationIsUpdated() throws Exception {
        final String offenceId = UUID.randomUUID().toString();

        final String payload = FileUtil.getPayload(TEMPLATE_PAYLOAD_9_WITH_UPDATED_APPLICATION).replaceAll("OFFENCE_ID", offenceId);
        final JsonEnvelope envelope = envelopeFrom(metadataOf(metadataId, "results.command.track-results"),
                FileUtil.convertStringToJson(payload));

        final JsonObject hearingPayload = getPayload(TEMPLATE_RESULT_AGGREGATE_FOR_NEXH_WITH_LINE_FEED);
        Hearing hearingObject = jsonObjectToObjectConverter.convert((JsonObject) hearingPayload.get("hearing"), Hearing.class);
        when(this.resultsAggregateSpy.getHearing()).thenReturn(hearingObject);

        Offence offence = hearingObject.getCourtApplications().stream()
                .map(CourtApplication::getCourtApplicationCases).flatMap(List::stream)
                .filter(courtApplicationCase -> Objects.nonNull(courtApplicationCase.getOffences()))
                .map(CourtApplicationCase::getOffences).flatMap(List::stream).collect(toList()).get(0);

        Map<UUID, OffenceResultsDetails> offenceResultsDetails = Collections.singletonMap(
                UUID.fromString(offenceId),
                OffenceResultsDetails.offenceResultsDetails()
                        .withOffenceId(offence.getId())
                        .withImpositionOffenceDetails("ImpositionOffenceDetails")
                        .withOffenceTitle("OffenceTitle")
                        .withIsFinancial(true).build());

        CorrelationIdHistoryItem correlationIdHistoryItem = CorrelationIdHistoryItem.correlationIdHistoryItem()
                .withAccountCorrelationId(randomUUID())
                .withAccountNumber("AccountNumber")
                .withAccountDivisionCode("Code").build();
        LinkedList<CorrelationIdHistoryItem> list = new LinkedList<>();
        list.add(correlationIdHistoryItem);

        setField(this.hearingFinancialResultsAggregateSpy, "offenceResultsDetails", offenceResultsDetails);
        setField(this.hearingFinancialResultsAggregateSpy, "correlationIdHistoryItemList", list);

        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(this.eventStream, HearingFinancialResultsAggregate.class)).thenReturn(hearingFinancialResultsAggregateSpy);
        when(aggregateService.get(this.eventStream, ResultsAggregate.class)).thenReturn(resultsAggregateSpy);

        resultsCommandHandler.trackResult(envelope);
        verify(eventStream).append(streamArgumentCaptor.capture());
        final List<JsonEnvelope> jsonEnvelopeList = convertStreamToEventList(streamArgumentCaptor.getAllValues());
        assertThat(jsonEnvelopeList.size(), is(2));
        assertThat(jsonEnvelopeList.get(0).metadata().name(), is("results.event.hearing-financial-results-tracked"));

        String s = "PDATE - Pay by date\\nPay by date. Date to pay in full by: 22/12/2023.\",\"title\":\"Keep a vehicle without a valid vehicle licence\"}]";
        assertThat(jsonEnvelopeList.get(1).metadata().name(), is("results.event.nces-email-notification-requested"));
        assertThat(jsonEnvelopeList.get(1).payloadAsJsonObject().getString("subject"), is ("STATUTORY DECLARATION UPDATED"));
        assertThat(jsonEnvelopeList.get(1).payloadAsJsonObject().getString("dateDecisionMade"), is ("01-01-2021"));
        assertThat(jsonEnvelopeList.get(1).payloadAsJsonObject().getString("defendantName"), is ("defendantName"));
        assertThat(jsonEnvelopeList.get(1).payloadAsJsonObject().getString("applicationResult") , is ("hearing on 18/04/2024 at 10:00 in Courtroom 02, Lavender Hill Magistrates' Court"));
    }


    @Test
    public void shouldTrackResultWhenResultAggregateHearingOffencesAreEmpty() throws Exception {
        final String offenceId = UUID.randomUUID().toString();

        final String payload = FileUtil.getPayload(TEMPLATE_PAYLOAD_9_WITH_GRANTED_APPLICATION).replaceAll("OFFENCE_ID", offenceId);
        final JsonEnvelope envelope = envelopeFrom(metadataOf(metadataId, "results.command.track-results"),
                FileUtil.convertStringToJson(payload));

        final JsonObject hearingPayload = getPayload(TEMPLATE_RESULT_AGGREGATE_OFFENCES_ARE_EMPTY);
        Hearing hearingObject = jsonObjectToObjectConverter.convert((JsonObject) hearingPayload.get("hearing"), Hearing.class);
        when(this.resultsAggregateSpy.getHearing()).thenReturn(hearingObject);

        Offence offence = hearingObject.getCourtApplications().stream()
                .map(CourtApplication::getCourtApplicationCases).flatMap(List::stream)
                .filter(courtApplicationCase -> Objects.nonNull(courtApplicationCase.getOffences()))
                .map(CourtApplicationCase::getOffences).flatMap(List::stream).collect(toList()).get(0);

        Map<UUID, OffenceResultsDetails> offenceResultsDetails = Collections.singletonMap(
                UUID.fromString(offenceId),
                OffenceResultsDetails.offenceResultsDetails()
                        .withOffenceId(offence.getId())
                        .withImpositionOffenceDetails("ImpositionOffenceDetails")
                        .withOffenceTitle("OffenceTitle")
                        .withIsFinancial(true).build());

        CorrelationIdHistoryItem correlationIdHistoryItem = CorrelationIdHistoryItem.correlationIdHistoryItem()
                .withAccountCorrelationId(randomUUID())
                .withAccountNumber("AccountNumber")
                .withAccountDivisionCode("Code").build();
        LinkedList<CorrelationIdHistoryItem> list = new LinkedList<>();
        list.add(correlationIdHistoryItem);

        setField(this.hearingFinancialResultsAggregateSpy, "offenceResultsDetails", offenceResultsDetails);
        setField(this.hearingFinancialResultsAggregateSpy, "correlationIdHistoryItemList", list);

        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(this.eventStream, HearingFinancialResultsAggregate.class)).thenReturn(hearingFinancialResultsAggregateSpy);
        when(aggregateService.get(this.eventStream, ResultsAggregate.class)).thenReturn(resultsAggregateSpy);

        resultsCommandHandler.trackResult(envelope);
        verify(eventStream).append(streamArgumentCaptor.capture());
        final List<JsonEnvelope> jsonEnvelopeList = convertStreamToEventList(streamArgumentCaptor.getAllValues());
        assertThat(jsonEnvelopeList.size(), is(2));
        assertThat(jsonEnvelopeList.get(0).metadata().name(), is("results.event.hearing-financial-results-tracked"));

        String s = "PDATE - Pay by date\\nPay by date. Date to pay in full by: 22/12/2023.\",\"title\":\"Keep a vehicle without a valid vehicle licence\"}]";
        assertThat(jsonEnvelopeList.get(1).metadata().name(), is("results.event.nces-email-notification-requested"));
        assertThat(jsonEnvelopeList.get(1).payloadAsJsonObject().getString("subject"), is ("STATUTORY DECLARATION GRANTED"));
        assertThat(jsonEnvelopeList.get(1).payloadAsJsonObject().getString("dateDecisionMade"), is ("01-01-2021"));
        assertThat(jsonEnvelopeList.get(1).payloadAsJsonObject().getString("defendantName"), is ("defendantName"));
        assertThat(jsonEnvelopeList.get(1).payloadAsJsonObject().getString("originalDateOfOffence"), is ("2022-12-12"));
    }
    @Test
    public void shouldTrackResultWithMissingOffencesAndJudicialResults() throws EventStreamException {
        final JsonObject payload = getPayload(TEMPLATE_PAYLOAD_9);
        final JsonEnvelope envelope = envelopeFrom(metadataOf(metadataId, "results.command.track-results"), payload);

        final JsonObject hearingPayload = getPayload(TEMPLATE_PAYLOAD_FINANCIAL_PENALTIES_TO_BE_WRITTEN_OFF_TRUE_WITH_OFFENCE_IS_NULL_AND_JUDICIAL_RESULT_IS_NULL);
        Hearing hearingObject = jsonObjectToObjectConverter.convert((JsonObject) hearingPayload.get("hearing"), Hearing.class);

        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(this.resultsAggregateSpy.getHearing()).thenReturn(hearingObject);
        when(aggregateService.get(this.eventStream, HearingFinancialResultsAggregate.class)).thenReturn(hearingFinancialResultsAggregateSpy);
        when(aggregateService.get(this.eventStream, ResultsAggregate.class)).thenReturn(resultsAggregateSpy);

        resultsCommandHandler.trackResult(envelope);
        verify(eventStream).append(streamArgumentCaptor.capture());
        final List<JsonEnvelope> jsonEnvelopeList = convertStreamToEventList(streamArgumentCaptor.getAllValues());
        assertThat(jsonEnvelopeList.size(), is(1));
        assertThat(jsonEnvelopeList.get(0).metadata().name(), is("results.event.hearing-financial-results-tracked"));
    }

    @Test
    public void shouldTrackResultWithMissingJudicialResultsPrompts() throws EventStreamException {
        final JsonObject payload = getPayload(TEMPLATE_PAYLOAD_9);
        final JsonEnvelope envelope = envelopeFrom(metadataOf(metadataId, "results.command.track-results"), payload);

        final JsonObject hearingPayload = getPayload(TEMPLATE_PAYLOAD_FINANCIAL_PENALTIES_TO_BE_WRITTEN_OFF_TRUE_WITH_OFFENCE_IS_NULL_AND_JUDICIAL_PROMPT_IS_NULL);
        Hearing hearingObject = jsonObjectToObjectConverter.convert((JsonObject) hearingPayload.get("hearing"), Hearing.class);
        when(this.resultsAggregateSpy.getHearing()).thenReturn(hearingObject);

        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(this.eventStream, HearingFinancialResultsAggregate.class)).thenReturn(hearingFinancialResultsAggregateSpy);
        when(aggregateService.get(this.eventStream, ResultsAggregate.class)).thenReturn(resultsAggregateSpy);

        resultsCommandHandler.trackResult(envelope);
        verify(eventStream).append(streamArgumentCaptor.capture());
        final List<JsonEnvelope> jsonEnvelopeList = convertStreamToEventList(streamArgumentCaptor.getAllValues());
        assertThat(jsonEnvelopeList.size(), is(1));
        assertThat(jsonEnvelopeList.get(0).metadata().name(), is("results.event.hearing-financial-results-tracked"));
        JsonObject res = jsonEnvelopeList.get(0).payloadAsJsonObject();
        JsonObject hearingobject = res.getJsonObject("hearingFinancialResultRequest");
        assertThat(hearingobject.getBoolean("isSJPHearing"), is(true));
    }

    @Test
    public void shouldUpdateResultCommandWithJurisdictionMagistratesAndRaisePoliceNotificationRequested() throws EventStreamException {
        final ResultsAggregate resultsAggregate = new ResultsAggregate();

        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(any(EventStream.class), any())).thenReturn(resultsAggregate);

        final JsonObjectBuilder jsonProsecutorBuilder = createObjectBuilder();
        jsonProsecutorBuilder
                .add("spiOutFlag", true)
                .add("policeFlag", true)
                .add("mcContactEmailAddress", EMAIL);
        when(referenceDataService.getSpiOutFlagForOriginatingOrganisation("OUCODE")).thenReturn(of(jsonProsecutorBuilder.build()));


        final JsonObject payload = getPayload(TEMPLATE_PAYLOAD);
        final JsonEnvelope envelope = envelopeFrom(metadataOf(metadataId, "results.create-results"), payload);
        final List<JsonObject> cases = (List<JsonObject>) payload.get("cases");
        final CaseDetails convertedCaseDetails = jsonObjectToObjectConverter.convert(cases.get(0), CaseDetails.class);
        final JsonObject session = payload.getJsonObject("session");
        final CourtCentreWithLJA courtCentre = jsonObjectToObjectConverter.convert(session.getJsonObject("courtCentreWithLJA"), CourtCentreWithLJA.class);
        resultsAggregate.handleSession(fromString(session.getString("id")), courtCentre, (List<SessionDay>) session.get("sessionDays"));
        resultsAggregate.handleCase(convertedCaseDetails);
        resultsAggregate.handleDefendants(convertedCaseDetails, true, of(JurisdictionType.MAGISTRATES), EMAIL, true, empty(), "", "");

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
                        withMetadataEnvelopedFrom(envelope).withName("results.event.police-notification-requested-v2"),
                        payloadIsJson(allOf(
                                        withJsonPath("$.caseId", is("bfd697f8-31ae-4e25-8654-4a10b812f5dd")),
                                        withJsonPath("$.caseDefendants[0].defendantId", is("1db6bdbb-a9ac-435f-acda-b735971daa74")),
                                        withJsonPath("$.policeEmailAddress", is(EMAIL))
                                )
                        ))
        ));

    }

    @Test
    public void shouldUpdateResultCommandWithJurisdictionCrownAndRaisePoliceNotificationRequested() throws EventStreamException {
        final ResultsAggregate resultsAggregate = new ResultsAggregate();

        when(eventSource.getStreamById(any())).thenReturn(eventStream);
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
        resultsAggregate.handleDefendants(convertedCaseDetails, true, of(JurisdictionType.MAGISTRATES), EMAIL, true, empty(), "", "");

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
                        withMetadataEnvelopedFrom(envelope).withName("results.event.police-notification-requested-v2"),
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

        when(eventSource.getStreamById(any())).thenReturn(eventStream);
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
        resultsAggregate.handleDefendants(convertedCaseDetails, true, of(JurisdictionType.MAGISTRATES), EMAIL, true, empty(), "", "");

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

        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(any(EventStream.class), any())).thenReturn(resultsAggregateSpy);

        final JsonObjectBuilder jsonProsecutorBuilder = createObjectBuilder();
        jsonProsecutorBuilder
                .add("spiOutFlag", false)
                .add("policeFlag", true)
                .add("contactEmailAddress", EMAIL);

        final JsonObject policeResultsPayload = getPayload(TEMPLATE_PAYLOAD_4);
        final JsonEnvelope policeResultsEnvelope = envelopeFrom(metadataOf(metadataId, "results.command.generate-police-results-for-a-defendant"), policeResultsPayload);
        resultsCommandHandler.generatePoliceResultsForDefendant(policeResultsEnvelope);
        verify(resultsAggregateSpy).generatePoliceResults(policeResultsPayload.getString("caseId"), policeResultsPayload.getString("defendantId"), empty());
    }

    @Test
    public void shouldUpdateResultCommandWithProsecutionAuthorityCodeAndWithoutOriginatingOrganisation() throws EventStreamException {
        final ResultsAggregate resultsAggregate = new ResultsAggregate();

        when(eventSource.getStreamById(any())).thenReturn(eventStream);
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
        resultsAggregate.handleDefendants(convertedCaseDetails, true, of(JurisdictionType.MAGISTRATES), EMAIL, true, empty(), "", "");

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
                        withMetadataEnvelopedFrom(envelope)
                                .withName("results.event.police-notification-requested-v2"),
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

