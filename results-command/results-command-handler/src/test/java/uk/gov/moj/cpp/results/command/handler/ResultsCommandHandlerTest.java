package uk.gov.moj.cpp.results.command.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.nio.charset.Charset.defaultCharset;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createObjectBuilder;
import static javax.json.Json.createReader;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
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
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.CourtCentreWithLJA;
import uk.gov.justice.core.courts.Defendant;
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
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.PoliceResultGenerated;
import uk.gov.justice.core.courts.PoliceResultGeneratedForStandaloneApplication;
import uk.gov.justice.core.courts.ProsecutionCase;
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
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher;
import uk.gov.moj.cpp.domains.results.shareresults.PublicHearingResulted;
import uk.gov.moj.cpp.results.command.handler.utils.FileUtil;
import uk.gov.moj.cpp.results.domain.aggregate.DefendantAggregate;
import uk.gov.moj.cpp.results.domain.aggregate.HearingFinancialResultsAggregate;
import uk.gov.moj.cpp.results.domain.aggregate.ResultsAggregate;
import uk.gov.moj.cpp.results.domain.aggregate.utils.CorrelationItem;
import uk.gov.moj.cpp.results.domain.event.AppealUpdateNotificationRequested;
import uk.gov.moj.cpp.results.domain.event.MarkedAggregateSendEmailWhenAccountReceived;
import uk.gov.moj.cpp.results.domain.event.NcesEmailNotificationRequested;
import uk.gov.moj.cpp.results.domain.event.PoliceNotificationRequested;
import uk.gov.moj.cpp.results.domain.event.PoliceNotificationRequestedV2;
import uk.gov.moj.cpp.results.domain.event.PublishToDcs;
import uk.gov.moj.cpp.results.test.TestTemplates;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import org.apache.commons.io.IOUtils;
import org.hamcrest.core.IsNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
    private static final String TEMPLATE_PAYLOAD_FINANCIAL_PENALTIES_FOR_SJP = "json/results.events.hearing-results-added-for-day_financialPenalties_for_sjp.json";
    private static final String TEMPLATE_RESULT_AGGREGATE_OFFENCES_ARE_EMPTY = "json/result-aggregate-offences-are-empty.json";
    private static final String TEMPLATE_RESULT_AGGREGATE_FOR_NEXH_WITH_LINE_FEED = "json/result-aggregate-offences-for-nexh-with-line-feed.json";
    private static final String TEMPLATE_RESULT_AGGREGATE_FOR_PROSECUTOR_WITH_LINE_FEED = "json/result-command-handler-get-prosecutor-from-court-applications.json";
    private static final String TEMPLATE_RESULT_AGGREGATE_FOR_PROSECUTOR_WITH_LINE_FEED_NULL_MASTER_DEFENDANT = "json/result-command-handler-get-prosecutor-from-court-applications-withMasterDefendant_isNull.json";
    private static final String TEMPLATE_PAYLOAD_FINANCIAL_PENALTIES_TO_BE_WRITTEN_OFF_TRUE_NULL = "json/results.events.hearing-results-added-for-day_financialPenaltiesToBeWrittenOff_trueWithNullOrderedDate.json";

    private static final String TEMPLATE_PAYLOAD_FINANCIAL_PENALTIES_TO_BE_RESPONDS_EMPTY ="json/results.create-results-for-day-court-application-without-responds.json";
    private static final String TEMPLATE_PAYLOAD_FINANCIAL_PENALTIES_TO_BE_WRITTEN_OFF_TRUE_WITH_OFFENCE_IS_NULL_AND_JUDICIAL_RESULT_IS_NULL = "json/results.events.hearing-results-added-for-day_financialPenaltiesToBeWrittenOff_true_with_juducial_result_and_offence_is_null.json";
    private static final String TEMPLATE_PAYLOAD_FINANCIAL_PENALTIES_TO_BE_WRITTEN_OFF_TRUE_WITH_OFFENCE_IS_NULL_AND_JUDICIAL_PROMPT_IS_NULL = "json/results.events.hearing-results-added-for-day_financialPenaltiesToBeWrittenOff_true_with_juducial_promt_is_null.json";
    private static final String TEMPLATE_PAYLOAD_11 = "json/results.create-results-court-application.json";
    private static final String PAYLOAD_FOR_POLICE_WITH_ORIGINATING_ORGANISATION = "json/results.create-results-magistrates-example_with_originating_organisation.json";
    private static final String TEMPLATE_PAYLOAD_12 = "json/case-amended.json";
    private static final String RESULTS_PAYLOAD_12 = "json/results-added-for-case-amendement.json";
    private static final String TEMPLATE_PAYLOAD_13 = "json/application-amended.json";
    private static final String RESULTS_PAYLOAD_13 = "json/results-added-for-application-amendement.json";


    private static final String TEMPLATE_PAYLOAD_APPLICATION_RESULTED = "json/results.create-results-for-day-court-application.json";
    private static final String TEMPLATE_PAYLOAD_APPLICATION_RESULTED_WITHOUT_APPLICATION_LEVEL_RESULTS = "json/results.create-results-for-day-court-application-without-results.json";
    private static final String EMAIL = "mail@mail.com";

    private static final String EMAIL2 = "mail2@mail2.com";
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
            PoliceResultGeneratedForStandaloneApplication.class,
            SjpCaseRejectedEvent.class,
            PoliceNotificationRequested.class,
            PoliceNotificationRequestedV2.class,
            HearingResultsAddedForDay.class,
            HearingFinancialResultsTracked.class,
            MarkedAggregateSendEmailWhenAccountReceived.class,
            NcesEmailNotificationRequested.class,
            DefendantTrackingStatusUpdated.class,
            AppealUpdateNotificationRequested.class,
            PublishToDcs.class
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
                        )),
                jsonEnvelope(
                        withMetadataEnvelopedFrom(envelope).withName("results.event.publish-to-dcs"),
                        payloadIsJson(allOf(
                                        withJsonPath("$.currentHearing.id", is(hearingId.toString())),
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
                                                                .add("offences", Json.createArrayBuilder()
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
                                                                .add("offences", Json.createArrayBuilder()
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
                                                                .add("offences", Json.createArrayBuilder()
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
                                                                .add("offences", Json.createArrayBuilder()
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
                        ))),
                jsonEnvelope(
                        withMetadataEnvelopedFrom(envelope).withName("results.event.publish-to-dcs"),
                        payloadIsJson(allOf(
                                withJsonPath("$.currentHearing.id", is(hearingId.toString())),
                                withJsonPath("$.hearingDay", is(hearingDay.toString())),
                                withJsonPath("$.currentHearing.isGroupProceedings", is(true)),
                                withJsonPath("$.currentHearing.prosecutionCases[0].id", is(case1Id.toString())),
                                withJsonPath("$.currentHearing.prosecutionCases[1].id", is(case2Id.toString())),
                                withJsonPath("$.currentHearing.prosecutionCases[2].id", is(case3Id.toString())),
                                withJsonPath("$.currentHearing.prosecutionCases[3].id", is(case4Id.toString())),
                                withJsonPath("$.currentHearing.prosecutionCases[0].isCivil", is(true)),
                                withJsonPath("$.currentHearing.prosecutionCases[1].isCivil", is(true)),
                                withJsonPath("$.currentHearing.prosecutionCases[2].isCivil", is(true)),
                                withJsonPath("$.currentHearing.prosecutionCases[3].isCivil", is(true)),
                                withJsonPath("$.currentHearing.prosecutionCases[0].groupId", is(groupId.toString())),
                                withJsonPath("$.currentHearing.prosecutionCases[1].groupId", is(groupId.toString())),
                                withJsonPath("$.currentHearing.prosecutionCases[2].groupId", is(groupId.toString())),
                                withJsonPath("$.currentHearing.prosecutionCases[3].groupId", is(groupId.toString())),
                                withJsonPath("$.currentHearing.prosecutionCases[0].isGroupMember", is(true)),
                                withJsonPath("$.currentHearing.prosecutionCases[1].isGroupMember", is(true)),
                                withJsonPath("$.currentHearing.prosecutionCases[2].isGroupMember", is(false)),
                                withJsonPath("$.currentHearing.prosecutionCases[3].isGroupMember", is(false)),
                                withJsonPath("$.currentHearing.prosecutionCases[0].isGroupMember", is(true)),
                                withJsonPath("$.currentHearing.prosecutionCases[1].isGroupMember", is(true)),
                                withJsonPath("$.currentHearing.prosecutionCases[2].isGroupMember", is(false)),
                                withJsonPath("$.currentHearing.prosecutionCases[3].isGroupMember", Objects::isNull)
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

        final JsonObject caseDetailsJson = payload.getJsonArray("cases").getJsonObject(0);
        final CaseDetails caseDetails = jsonObjectToObjectConverter.convert(caseDetailsJson, CaseDetails.class);
        this.resultsAggregateSpy.apply(hearingResultsAddedForDay(caseDetails));

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
        final List<SessionDay> sessionDays = ofNullable((List<JsonObject>) session.get("sessionDays")).orElse(emptyList()).stream()
                .map(jsonObject -> jsonObjectToObjectConverter.convert(jsonObject, SessionDay.class))
                .collect(toList());
        final CourtCentreWithLJA courtCentre = jsonObjectToObjectConverter.convert(session.getJsonObject("courtCentreWithLJA"), CourtCentreWithLJA.class);
        resultsAggregate.apply(hearingResultsAddedForDay(convertedCaseDetails));
        resultsAggregate.handleSession(fromString(session.getString("id")), courtCentre, sessionDays);
        resultsAggregate.handleCase(convertedCaseDetails);
        resultsAggregate.handleDefendants(convertedCaseDetails, true, of(JurisdictionType.MAGISTRATES), EMAIL, true, empty(), "", "", Optional.of(Boolean.TRUE));
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

        final JsonObject session = payload.getJsonObject("session");
        final UUID sessionId = fromString(session.getString("id"));
        final CourtCentreWithLJA courtCentreWithLJA = jsonObjectToObjectConverter.convert(session.getJsonObject("courtCentreWithLJA"), CourtCentreWithLJA.class);
        final List<SessionDay> sessionDays = ofNullable((List<JsonObject>) session.get("sessionDays")).orElse(emptyList()).stream()
                .map(jsonObject -> jsonObjectToObjectConverter.convert(jsonObject, SessionDay.class))
                .collect(toList());

        final JsonObject caseDetailsJson = payload.getJsonArray("cases").getJsonObject(0);
        final CaseDetails caseDetails = jsonObjectToObjectConverter.convert(caseDetailsJson, CaseDetails.class);

        resultsAggregateSpy.apply(hearingResultsAddedForDay(caseDetails));
        resultsCommandHandler.createResult(envelope);

        verify(resultsAggregateSpy).handleSession(eq(sessionId), eq(courtCentreWithLJA), eq(sessionDays));
        verify(resultsAggregateSpy).handleCase(eq(caseDetails));
        verify(resultsAggregateSpy).handleDefendants(eq(caseDetails), anyBoolean(), any(), any(), anyBoolean(), eq(empty()), any(), any(), any(),any());
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
        final String offenceId = "2c1394a1-5dfc-49d1-a6c0-ef6f2df55423";
        final JsonObject payload = getPayload(TEMPLATE_PAYLOAD_9);
        final JsonEnvelope envelope = envelopeFrom(metadataOf(metadataId, "results.command.track-results"), payload);

        final JsonObject hearingPayload = getPayload(TEMPLATE_PAYLOAD_FINANCIAL_PENALTIES_TO_BE_WRITTEN_OFF_TRUE);
        Hearing hearingObject = jsonObjectToObjectConverter.convert((JsonObject) hearingPayload.get("hearing"), Hearing.class);
        when(this.resultsAggregateSpy.getHearing()).thenReturn(hearingObject);

        Map<UUID, OffenceResultsDetails> offenceResultsDetails = Collections.singletonMap(UUID.randomUUID(), OffenceResultsDetails.offenceResultsDetails()
                .withOffenceId(fromString(offenceId)).build());
        setField(this.hearingFinancialResultsAggregateSpy, "caseOffenceResultsDetails", offenceResultsDetails);
        setField(this.resultsAggregateSpy, "hearing", hearingObject);

        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(this.aggregateService.get(any(), eq(HearingFinancialResultsAggregate.class))).thenReturn(hearingFinancialResultsAggregateSpy);
        when(this.aggregateService.get(any(), eq(ResultsAggregate.class))).thenReturn(resultsAggregateSpy);

        resultsCommandHandler.trackResult(envelope);
        verify(eventStream).append(streamArgumentCaptor.capture());
        final List<JsonEnvelope> jsonEnvelopeList = convertStreamToEventList(streamArgumentCaptor.getAllValues());
        assertThat(jsonEnvelopeList.size(), is(1));
        assertThat(jsonEnvelopeList.get(0).metadata().name(), is("results.event.hearing-financial-results-tracked"));
    }

    @Test
    public void shouldTrackResultWhenApplicationIsGrantedWithNoOffenceResults() throws Exception {
        final String offenceId = "66ab677a-722d-418a-a670-507512e2b88d";

        final String payload = FileUtil.getPayload(TEMPLATE_PAYLOAD_9_WITH_GRANTED_APPLICATION);
        final JsonEnvelope envelope = envelopeFrom(metadataOf(metadataId, "results.command.track-results"),
                FileUtil.convertStringToJson(payload));

        final JsonObject hearingPayload = getPayload(TEMPLATE_PAYLOAD_FINANCIAL_PENALTIES_TO_BE_WRITTEN_OFF_TRUE);
        Hearing hearingObject = jsonObjectToObjectConverter.convert((JsonObject) hearingPayload.get("hearing"), Hearing.class);
        when(this.resultsAggregateSpy.getHearing()).thenReturn(hearingObject);

        Map<UUID, OffenceResultsDetails> offenceResultsDetails = Collections.singletonMap(
                UUID.fromString(offenceId),
                OffenceResultsDetails.offenceResultsDetails()
                        .withApplicationType("REOPEN")
                        .withOffenceId(fromString(offenceId))
                        .withImpositionOffenceDetails("ImpositionOffenceDetails")
                        .withOffenceTitle("OffenceTitle")
                        .withIsFinancial(true).build());

        CorrelationItem correlationItem = CorrelationItem.correlationItem()
                .withAccountCorrelationId(randomUUID())
                .withAccountNumber("AccountNumber")
                .withAccountDivisionCode("Code").build();
        LinkedList<CorrelationItem> list = new LinkedList<>();
        list.add(correlationItem);

        setField(this.hearingFinancialResultsAggregateSpy, "caseOffenceResultsDetails", offenceResultsDetails);
        setField(this.hearingFinancialResultsAggregateSpy, "correlationItemList", list);
        setField(this.hearingFinancialResultsAggregateSpy, "hearingId", randomUUID());

        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(this.eventStream, HearingFinancialResultsAggregate.class)).thenReturn(hearingFinancialResultsAggregateSpy);
        when(aggregateService.get(this.eventStream, ResultsAggregate.class)).thenReturn(resultsAggregateSpy);

        setField(this.resultsAggregateSpy, "hearing", hearingObject);

        resultsCommandHandler.trackResult(envelope);
        verify(eventStream).append(streamArgumentCaptor.capture());
        final List<JsonEnvelope> jsonEnvelopeList = convertStreamToEventList(streamArgumentCaptor.getAllValues());
        assertThat(jsonEnvelopeList.size(), is(1));
        assertThat(jsonEnvelopeList.get(0).metadata().name(), is("results.event.hearing-financial-results-tracked"));

        assertThat(jsonEnvelopeList.get(0).metadata().name(), is("results.event.hearing-financial-results-tracked"));
        assertThat(jsonEnvelopeList.get(0).payloadAsJsonObject().getJsonObject("hearingFinancialResultRequest").getJsonArray("offenceResults").size(), is(2));
    }

    @Test
    public void shouldTrackResultWhenApplicationIsSJPGrantedWithNoOffenceResults() throws Exception {
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        final String offenceId1 = "eb4146cb-39f9-4ed3-ba86-088d2954a937";
        final String payload = FileUtil.getPayload(TEMPLATE_PAYLOAD_9_WITH_GRANTED_APPLICATION);
        final JsonEnvelope envelope = envelopeFrom(metadataOf(metadataId, "results.command.track-results"),
                FileUtil.convertStringToJson(payload));

        final JsonObject hearingPayload = getPayload(TEMPLATE_PAYLOAD_FINANCIAL_PENALTIES_FOR_SJP);
        Hearing hearingObject = jsonObjectToObjectConverter.convert((JsonObject) hearingPayload.get("hearing"), Hearing.class);
        when(this.resultsAggregateSpy.getHearing()).thenReturn(hearingObject);

        Map<UUID, OffenceResultsDetails> offenceResultsDetails = new HashMap<>();
        offenceResultsDetails.put(UUID.fromString(offenceId1),
                OffenceResultsDetails.offenceResultsDetails()
                        .withOffenceId(fromString(offenceId1))
                        .withImpositionOffenceDetails("ImpositionOffenceDetails")
                        .withOffenceTitle("OffenceTitle")
                        .withIsFinancial(true).build());

        CorrelationItem correlationItem = CorrelationItem.correlationItem()
                .withAccountCorrelationId(randomUUID())
                .withAccountNumber("AccountNumber")
                .withAccountDivisionCode("Code").build();
        LinkedList<CorrelationItem> list = new LinkedList<>();
        list.add(correlationItem);

        setField(this.hearingFinancialResultsAggregateSpy, "caseOffenceResultsDetails", offenceResultsDetails);
        setField(this.hearingFinancialResultsAggregateSpy, "correlationItemList", list);
        setField(this.hearingFinancialResultsAggregateSpy, "hearingId", randomUUID());

        when(this.aggregateService.get(any(), eq(HearingFinancialResultsAggregate.class))).thenReturn(hearingFinancialResultsAggregateSpy);
        when(this.aggregateService.get(any(), eq(ResultsAggregate.class))).thenReturn(resultsAggregateSpy);

        setField(this.resultsAggregateSpy, "hearing", hearingObject);

        resultsCommandHandler.trackResult(envelope);
        verify(eventStream).append(streamArgumentCaptor.capture());
        final List<JsonEnvelope> jsonEnvelopeList = convertStreamToEventList(streamArgumentCaptor.getAllValues());
        assertThat(jsonEnvelopeList.size(), is(1));
        assertThat(jsonEnvelopeList.get(0).metadata().name(), is("results.event.hearing-financial-results-tracked"));
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

        CorrelationItem correlationItem = CorrelationItem.correlationItem()
                .withAccountCorrelationId(randomUUID())
                .withAccountNumber("AccountNumber")
                .withOffenceResultsDetailsList(emptyList())
                .withAccountDivisionCode("Code").build();
        LinkedList<CorrelationItem> list = new LinkedList<>();
        list.add(correlationItem);

        setField(this.hearingFinancialResultsAggregateSpy, "caseOffenceResultsDetails", offenceResultsDetails);
        setField(this.hearingFinancialResultsAggregateSpy, "correlationItemList", list);

        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(this.aggregateService.get(any(), eq(HearingFinancialResultsAggregate.class))).thenReturn(hearingFinancialResultsAggregateSpy);
        when(this.aggregateService.get(any(), eq(ResultsAggregate.class))).thenReturn(resultsAggregateSpy);

        setField(this.resultsAggregateSpy, "hearing", hearingObject);
        resultsCommandHandler.trackResult(envelope);
        verify(eventStream).append(streamArgumentCaptor.capture());
        final List<JsonEnvelope> jsonEnvelopeList = convertStreamToEventList(streamArgumentCaptor.getAllValues());
        assertThat(jsonEnvelopeList.size(), is(2));
        assertThat(jsonEnvelopeList.get(0).metadata().name(), is("results.event.hearing-financial-results-tracked"));

        String s = "PDATE - Pay by date\\nPay by date. Date to pay in full by: 22/12/2023.\",\"title\":\"Keep a vehicle without a valid vehicle licence\"}]";
        assertThat(jsonEnvelopeList.get(1).metadata().name(), is("results.event.nces-email-notification-requested"));
        assertThat(jsonEnvelopeList.get(1).payloadAsJsonObject().getString("subject"), is("STATUTORY DECLARATION UPDATED"));
        assertThat(jsonEnvelopeList.get(1).payloadAsJsonObject().getString("dateDecisionMade"), is("01-01-2021"));
        assertThat(jsonEnvelopeList.get(1).payloadAsJsonObject().getString("defendantName"), is("defendantName"));
        assertThat(jsonEnvelopeList.get(1).payloadAsJsonObject().getString("applicationResult"), is("hearing on 18/04/2024 at 10:00 in Courtroom 02, Lavender Hill Magistrates' Court"));
    }


    @Test
    public void shouldTrackResultWhenResultAggregateHearingOffencesAreEmpty() throws Exception {
        final String offenceId = "eb4146cb-39f9-4ed3-ba86-088d2954a937";

        final String payload = FileUtil.getPayload(TEMPLATE_PAYLOAD_9_WITH_GRANTED_APPLICATION);
        final JsonEnvelope envelope = envelopeFrom(metadataOf(metadataId, "results.command.track-results"),
                FileUtil.convertStringToJson(payload));

        final JsonObject hearingPayload = getPayload(TEMPLATE_RESULT_AGGREGATE_OFFENCES_ARE_EMPTY);
        Hearing hearingObject = jsonObjectToObjectConverter.convert((JsonObject) hearingPayload.get("hearing"), Hearing.class);
        when(this.resultsAggregateSpy.getHearing()).thenReturn(hearingObject);

        Map<UUID, OffenceResultsDetails> offenceResultsDetails = Collections.singletonMap(
                UUID.fromString(offenceId),
                OffenceResultsDetails.offenceResultsDetails()
                        .withOffenceId(fromString(offenceId))
                        .withImpositionOffenceDetails("ImpositionOffenceDetails")
                        .withDateOfResult("24/05/2024")
                        .withOffenceTitle("OffenceTitle")
                        .withIsFinancial(true).build());

        CorrelationItem correlationItem = CorrelationItem.correlationItem()
                .withAccountCorrelationId(randomUUID())
                .withAccountNumber("AccountNumber")
                .withAccountDivisionCode("Code").build();
        LinkedList<CorrelationItem> list = new LinkedList<>();
        list.add(correlationItem);

        setField(this.hearingFinancialResultsAggregateSpy, "caseOffenceResultsDetails", offenceResultsDetails);
        setField(this.hearingFinancialResultsAggregateSpy, "correlationItemList", list);
        setField(this.resultsAggregateSpy, "hearing", hearingObject);

        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(this.aggregateService.get(any(), eq(HearingFinancialResultsAggregate.class))).thenReturn(hearingFinancialResultsAggregateSpy);
        when(this.aggregateService.get(any(), eq(ResultsAggregate.class))).thenReturn(resultsAggregateSpy);

        resultsCommandHandler.trackResult(envelope);
        verify(eventStream).append(streamArgumentCaptor.capture());
        final List<JsonEnvelope> jsonEnvelopeList = convertStreamToEventList(streamArgumentCaptor.getAllValues());
        assertThat(jsonEnvelopeList.size(), is(1));
        assertThat(jsonEnvelopeList.get(0).metadata().name(), is("results.event.hearing-financial-results-tracked"));
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
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        final JsonObject payload = getPayload(TEMPLATE_PAYLOAD_9);
        final JsonEnvelope envelope = envelopeFrom(metadataOf(metadataId, "results.command.track-results"), payload);

        final JsonObject hearingPayload = getPayload(TEMPLATE_PAYLOAD_FINANCIAL_PENALTIES_TO_BE_WRITTEN_OFF_TRUE_WITH_OFFENCE_IS_NULL_AND_JUDICIAL_PROMPT_IS_NULL);
        Hearing hearingObject = jsonObjectToObjectConverter.convert((JsonObject) hearingPayload.get("hearing"), Hearing.class);
        when(this.resultsAggregateSpy.getHearing()).thenReturn(hearingObject);

        when(this.aggregateService.get(any(), eq(HearingFinancialResultsAggregate.class))).thenReturn(hearingFinancialResultsAggregateSpy);
        when(this.aggregateService.get(any(), eq(ResultsAggregate.class))).thenReturn(resultsAggregateSpy);

        resultsCommandHandler.trackResult(envelope);
        verify(eventStream).append(streamArgumentCaptor.capture());
        final List<JsonEnvelope> jsonEnvelopeList = convertStreamToEventList(streamArgumentCaptor.getAllValues());
        assertThat(jsonEnvelopeList.size(), is(1));
        assertThat(jsonEnvelopeList.get(0).metadata().name(), is("results.event.hearing-financial-results-tracked"));
    }

    @Test
    public void shouldTrackResultForNullOrderedDate() throws EventStreamException {
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
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
        setField(this.hearingFinancialResultsAggregateSpy, "caseOffenceResultsDetails", offenceResultsDetails);

        when(this.aggregateService.get(any(), eq(HearingFinancialResultsAggregate.class))).thenReturn(hearingFinancialResultsAggregateSpy);
        when(this.aggregateService.get(any(), eq(ResultsAggregate.class))).thenReturn(resultsAggregateSpy);

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
        final List<SessionDay> sessionDays = ofNullable((List<JsonObject>) session.get("sessionDays")).orElse(emptyList()).stream()
                .map(jsonObject -> jsonObjectToObjectConverter.convert(jsonObject, SessionDay.class))
                .collect(toList());
        resultsAggregate.apply(hearingResultsAddedForDay(convertedCaseDetails));
        resultsAggregate.handleSession(fromString(session.getString("id")), courtCentre, sessionDays);
        resultsAggregate.handleCase(convertedCaseDetails);
        resultsAggregate.handleDefendants(convertedCaseDetails, true, of(JurisdictionType.MAGISTRATES), EMAIL, true, empty(), "", "", Optional.of(Boolean.TRUE));

        final JsonObject updatePayload = getPayload(TEMPLATE_PAYLOAD_2);
        final JsonEnvelope updateEnvelope = envelopeFrom(metadataOf(metadataId, "results.create-results"), updatePayload);
        final List<JsonObject> updatedCases = (List<JsonObject>) updatePayload.get("cases");
        final CaseDetails updatedCaseDetails = jsonObjectToObjectConverter.convert(updatedCases.get(0), CaseDetails.class);

        resultsAggregate.apply(hearingResultsAddedForDay(updatedCaseDetails));
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
        resultsAggregate.apply(hearingResultsAddedForDay(convertedCaseDetails));
        final List<SessionDay> sessionDays = ofNullable((List<JsonObject>) session.get("sessionDays")).orElse(emptyList()).stream()
                .map(jsonObject -> jsonObjectToObjectConverter.convert(jsonObject, SessionDay.class))
                .collect(toList());
        resultsAggregate.handleSession(fromString(session.getString("id")), courtCentre, sessionDays);
        resultsAggregate.handleCase(convertedCaseDetails);
        resultsAggregate.handleDefendants(convertedCaseDetails, true, of(JurisdictionType.MAGISTRATES), EMAIL, true, empty(), "", "", Optional.of(Boolean.TRUE));

        final JsonObject updatePayload = getPayload(TEMPLATE_PAYLOAD_6);
        final JsonEnvelope updateEnvelope = envelopeFrom(metadataOf(metadataId, "results.create-results"), updatePayload);

        final List<JsonObject> updatedCases = (List<JsonObject>) updatePayload.get("cases");
        final CaseDetails updatedCaseDetails = jsonObjectToObjectConverter.convert(updatedCases.get(0), CaseDetails.class);
        resultsAggregate.apply(hearingResultsAddedForDay(updatedCaseDetails));
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
        resultsAggregate.apply(hearingResultsAddedForDay(convertedCaseDetails));
        final List<SessionDay> sessionDays = ofNullable((List<JsonObject>) session.get("sessionDays")).orElse(emptyList()).stream()
                .map(jsonObject -> jsonObjectToObjectConverter.convert(jsonObject, SessionDay.class))
                .collect(toList());
        resultsAggregate.handleSession(fromString(session.getString("id")), courtCentre, sessionDays);
        resultsAggregate.handleCase(convertedCaseDetails);
        resultsAggregate.handleDefendants(convertedCaseDetails, true, of(JurisdictionType.MAGISTRATES), EMAIL, true, empty(), "", "", Optional.of(Boolean.TRUE));

        final JsonObject updatePayload = getPayload(TEMPLATE_PAYLOAD_6);
        final JsonEnvelope updateEnvelope = envelopeFrom(metadataOf(metadataId, "results.create-results"), updatePayload);

        final List<JsonObject> updatedCases = (List<JsonObject>) updatePayload.get("cases");
        final CaseDetails updatedCaseDetails = jsonObjectToObjectConverter.convert(updatedCases.get(0), CaseDetails.class);
        resultsAggregate.apply(hearingResultsAddedForDay(updatedCaseDetails));
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
    void shouldBuildApplicationTypeForCase() throws EventStreamException {
        final ResultsAggregate resultsAggregate = new ResultsAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(any(EventStream.class), any())).thenReturn(resultsAggregate);

        final JsonObjectBuilder jsonProsecutorBuilder = createObjectBuilder();
        jsonProsecutorBuilder
                .add("spiOutFlag", true)
                .add("policeFlag", true)
                .add("contactEmailAddress", EMAIL);
        when(referenceDataService.getSpiOutFlagForOriginatingOrganisation("OUCODE")).thenReturn(of(jsonProsecutorBuilder.build()));


        final JsonObject payload = getPayload(TEMPLATE_PAYLOAD_11);

        final JsonEnvelope createResultEnvelope = envelopeFrom(metadataOf(metadataId, "results.create-results"), payload);

        final List<JsonObject> cases = (List<JsonObject>) payload.get("cases");
        final List<CaseDetails> caseDetails = cases.stream()
                .map(c -> jsonObjectToObjectConverter.convert(c, CaseDetails.class))
                .collect(toList());

        resultsAggregate.apply(hearingResultsAddedForDay(caseDetails));
        this.resultsCommandHandler.createResult(createResultEnvelope);

        verify(eventStream, Mockito.times(5)).append(streamArgumentCaptor.capture());

        final List<JsonEnvelope> allValues = convertStreamToEventList(streamArgumentCaptor.getAllValues());
        final Optional<JsonEnvelope> policeNotificationEnv1 = allValues.stream().filter(json -> json.metadata().name().equals("results.event.police-notification-requested-v2")
                        && json.payloadAsJsonObject().getJsonString("caseId").getString().equals("bfd697f8-31ae-4e25-8654-4a10b812f5dd"))
                .findFirst();

        assertThat(policeNotificationEnv1.isPresent(), is(true));
        assertThat(policeNotificationEnv1.get().payloadAsJsonObject().getString("applicationTypeForCase"), is("Sorry Application, Breach of a supervision default order"));

        final Optional<JsonEnvelope> policeNotificationEnv2 = allValues.stream().filter(json -> json.metadata().name().equals("results.event.police-notification-requested-v2")
                        && json.payloadAsJsonObject().getJsonString("caseId").getString().equals("cffb892f-e5a5-45bb-8fe9-e443a7840385"))
                .findFirst();

        assertThat(policeNotificationEnv2.isPresent(), is(true));
        assertThat(policeNotificationEnv2.get().payloadAsJsonObject().getString("applicationId"), is(notNullValue()));
        assertThat(policeNotificationEnv2.get().payloadAsJsonObject().getString("applicationTypeForCase"), is("Only Breach, Breach of a supervision default order, Sorry Application"));
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
        resultsAggregate.apply(hearingResultsAddedForDay(convertedCaseDetails));
        final List<SessionDay> sessionDays = ofNullable((List<JsonObject>) session.get("sessionDays")).orElse(emptyList()).stream()
                .map(jsonObject -> jsonObjectToObjectConverter.convert(jsonObject, SessionDay.class))
                .collect(toList());
        resultsAggregate.handleSession(fromString(session.getString("id")), courtCentre, sessionDays);
        resultsAggregate.handleCase(convertedCaseDetails);
        resultsAggregate.handleDefendants(convertedCaseDetails, true, of(JurisdictionType.MAGISTRATES), EMAIL, true, empty(), "", "", Optional.of(Boolean.FALSE));

        final JsonObject updatePayload = getPayload(TEMPLATE_PAYLOAD_8);
        final List<JsonObject> updateCases = (List<JsonObject>) updatePayload.get("cases");
        final CaseDetails updatedCaseDetails = jsonObjectToObjectConverter.convert(updateCases.get(0), CaseDetails.class);

        final JsonEnvelope updateEnvelope = envelopeFrom(metadataOf(metadataId, "results.create-results"), updatePayload);

        resultsAggregate.apply(hearingResultsAddedForDay(updatedCaseDetails));
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

    @Test
    public void shouldTrackResultForAmendedCasesForNewOffenceResults() throws EventStreamException {

        final String offenceId = "2c1394a1-5dfc-49d1-a6c0-ef6f2df55423";
        final JsonObject payload = getPayload(TEMPLATE_PAYLOAD_12);
        final JsonEnvelope envelope = envelopeFrom(metadataOf(metadataId, "results.command.track-results"), payload);
        final JsonObject hearingPayload = getPayload(RESULTS_PAYLOAD_12);
        Hearing hearingObject = jsonObjectToObjectConverter.convert((JsonObject) hearingPayload.get("hearing"), Hearing.class);
        when(this.resultsAggregateSpy.getHearing()).thenReturn(hearingObject);
        Map<UUID, OffenceResultsDetails> offenceResultsDetails = new HashMap<UUID, OffenceResultsDetails>();
        offenceResultsDetails.put(fromString(offenceId), OffenceResultsDetails.offenceResultsDetails()
                .withOffenceId(fromString(offenceId))
                .withImpositionOffenceDetails("FO 500 Pay By date 15/04/2025").build());
        setField(this.hearingFinancialResultsAggregateSpy, "caseOffenceResultsDetails", offenceResultsDetails);
        setField(this.resultsAggregateSpy, "hearing", hearingObject);
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(this.aggregateService.get(any(), eq(HearingFinancialResultsAggregate.class))).thenReturn(hearingFinancialResultsAggregateSpy);
        when(this.aggregateService.get(any(), eq(ResultsAggregate.class))).thenReturn(resultsAggregateSpy);
        resultsCommandHandler.trackResult(envelope);
        verify(eventStream).append(streamArgumentCaptor.capture());
        final List<JsonEnvelope> jsonEnvelopeList = convertStreamToEventList(streamArgumentCaptor.getAllValues());
        assertThat(jsonEnvelopeList.size(), is(1));
        assertThat(jsonEnvelopeList.get(0).metadata().name(), is("results.event.hearing-financial-results-tracked"));

    }

    @Test
    public void shouldTrackResultForAmendedCasesForNewOffenceAndApplicationResults() throws EventStreamException {

        final String offenceId = "2c1394a1-5dfc-49d1-a6c0-ef6f2df55423";
        final String applicationId = "3c1394a1-5dfc-49d1-a6c0-ef6f2df55423";
        final JsonObject payload = getPayload(TEMPLATE_PAYLOAD_13);
        final JsonEnvelope envelope = envelopeFrom(metadataOf(metadataId, "results.command.track-results"), payload);
        final JsonObject hearingPayload = getPayload(RESULTS_PAYLOAD_13);
        Hearing hearingObject = jsonObjectToObjectConverter.convert((JsonObject) hearingPayload.get("hearing"), Hearing.class);
        when(this.resultsAggregateSpy.getHearing()).thenReturn(hearingObject);
        Map<UUID, OffenceResultsDetails> offenceResultsDetails = new HashMap<UUID, OffenceResultsDetails>();
        Map<UUID, List<OffenceResultsDetails>> applicationResultsDetails = new HashMap<>();
        offenceResultsDetails.put(fromString(offenceId), OffenceResultsDetails.offenceResultsDetails()
                .withOffenceId(fromString(offenceId))
                .withImpositionOffenceDetails("FO 500 Pay By date 15/04/2025").build());
        applicationResultsDetails.put(UUID.fromString(applicationId), singletonList(OffenceResultsDetails.offenceResultsDetails()
                .withOffenceId(fromString(applicationId))
                .withApplicationTitle("Application title")
                .withApplicationResultType("G").build()));
        setField(this.hearingFinancialResultsAggregateSpy, "caseOffenceResultsDetails", offenceResultsDetails);
        setField(this.hearingFinancialResultsAggregateSpy, "applicationResultsDetails", applicationResultsDetails);
        setField(this.resultsAggregateSpy, "hearing", hearingObject);
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(this.aggregateService.get(any(), eq(HearingFinancialResultsAggregate.class))).thenReturn(hearingFinancialResultsAggregateSpy);
        when(this.aggregateService.get(any(), eq(ResultsAggregate.class))).thenReturn(resultsAggregateSpy);
        resultsCommandHandler.trackResult(envelope);
        verify(eventStream).append(streamArgumentCaptor.capture());
        final List<JsonEnvelope> jsonEnvelopeList = convertStreamToEventList(streamArgumentCaptor.getAllValues());
        assertThat(jsonEnvelopeList.size(), is(1));
        assertThat(jsonEnvelopeList.get(0).metadata().name(), is("results.event.hearing-financial-results-tracked"));

    }


    @Test
    public void shouldCreateAppealUpdateEvent() throws EventStreamException {
        final ResultsAggregate resultsAggregate = new ResultsAggregate();

        when(eventSource.getStreamById(any())).thenReturn(this.eventStream);
        when(aggregateService.get(any(EventStream.class), any())).thenReturn(resultsAggregate);

        final JsonObjectBuilder jsonProsecutorBuilder1 = createObjectBuilder();
        jsonProsecutorBuilder1
                .add("spiOutFlag", true)
                .add("policeFlag", true)
                .add("contactEmailAddress", EMAIL)
                .add("mcContactEmailAddress", EMAIL);

        final JsonObjectBuilder jsonProsecutorBuilder2 = createObjectBuilder();
        jsonProsecutorBuilder2
                .add("spiOutFlag", true)
                .add("policeFlag", true)
                .add("informantEmailAddress", EMAIL2);

        when(referenceDataService.getSpiOutFlagForProsecutionAuthorityCode(any()))
                .thenReturn(of(jsonProsecutorBuilder1.build()))
                .thenReturn(of(jsonProsecutorBuilder2.build()));

        final JsonObject payload = getPayload(TEMPLATE_PAYLOAD_APPLICATION_RESULTED);
        final JsonEnvelope envelope = envelopeFrom(metadataOf(metadataId, "results.command.create-results-for-day"), payload);
        this.resultsCommandHandler.createResultsForDay(envelope);

        verify(enveloper, Mockito.times(3)).withMetadataFrom(envelope);
        verify(eventStream, Mockito.times(3)).append(streamArgumentCaptor.capture());
        final List<JsonEnvelope> allValues = convertStreamToEventList(streamArgumentCaptor.getAllValues());

        assertThat(allValues, containsInAnyOrder(
                jsonEnvelope(
                        withMetadataEnvelopedFrom(envelope).withName("results.event.session-added-event"),
                        payloadIsJson(allOf(
                                        withJsonPath("$.id", notNullValue()),
                                        withJsonPath("$.courtCentreWithLJA", notNullValue()),
                                        withJsonPath("$.sessionDays", notNullValue())
                                )
                        )),
                jsonEnvelope(
                        withMetadataEnvelopedFrom(envelope)
                                .withName("results.event.appeal-update-notification-requested"),
                        payloadIsJson(allOf(
                                        withJsonPath("$.applicationId", notNullValue()),
                                        withJsonPath("$.subject", is("Appeal Update")),
                                        withJsonPath("$.defendant", is("Korbin Ismael")),
                                        withJsonPath("$.urn", is("20NX8422480")),
                                        withJsonPath("$.notificationId", notNullValue()),
                                        withJsonPath("$.emailAddress", is(EMAIL))
                                )
                        )),
                jsonEnvelope(
                        withMetadataEnvelopedFrom(envelope)
                                .withName("results.event.appeal-update-notification-requested"),
                        payloadIsJson(allOf(
                                        withJsonPath("$.applicationId", notNullValue()),
                                        withJsonPath("$.subject", is("Appeal Update")),
                                        withJsonPath("$.defendant", is("Korbin Ismael")),
                                        withJsonPath("$.urn", is("20NX8422480")),
                                        withJsonPath("$.notificationId", notNullValue()),
                                        withJsonPath("$.emailAddress", is(EMAIL2))
                                )
                        ))
        ));
    }

    private List<JsonEnvelope> convertStreamToEventList(final List<Stream<JsonEnvelope>> listOfStreams) {
        return listOfStreams.stream()
                .flatMap(jsonEnvelopeStream -> jsonEnvelopeStream).collect(toList());
    }

    /***
     *  Delete the existing result  and add a new result on Application and do NOT amend the result on offence
     * @throws EventStreamException
     * @throws IOException
     */
    @Test
    void deleteResultAndAddResultOnApplication() throws EventStreamException, IOException {
        final String resourceRoot = "json/ delete-result-and-add-result-on-application/";
        final ResultsAggregate resultsAggregate = new ResultsAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        doReturn(resultsAggregate).when(aggregateService).get(any(EventStream.class), eq(ResultsAggregate.class));

        final JsonObjectBuilder jsonProsecutorBuilder = createObjectBuilder();
        jsonProsecutorBuilder
                .add("spiOutFlag", true)
                .add("policeFlag", true)
                .add("contactEmailAddress", EMAIL)
                .add("mcContactEmailAddress", EMAIL);

        when(referenceDataService.getSpiOutFlagForProsecutionAuthorityCode(any())).thenReturn(of(jsonProsecutorBuilder.build()));

        resultsAggregate.apply(loadHearingResultsAddedForDay(resourceRoot + "hearing-results-added-for-day-1.json"));
        whenCreateResultCommandHandled(resourceRoot + "create-results-1.json");

        resultsAggregate.apply(loadHearingResultsAddedForDay(resourceRoot + "hearing-results-added-for-day-2.json"));
        final JsonEnvelope createResultEnvelope = whenCreateResultCommandHandled(resourceRoot + "create-results-2.json");

        resultsAggregate.apply(loadHearingResultsAddedForDay(resourceRoot + "hearing-results-added-for-day-3.json"));
        whenCreateResultCommandHandled(resourceRoot + "create-results-3.json");

        verify(eventStream, atLeast(1)).append(streamArgumentCaptor.capture());
        final List<JsonEnvelope> allValues = convertStreamToEventList(streamArgumentCaptor.getAllValues());


        final List<JsonEnvelope> policeNoficationRequestedEvents = allValues.stream()
                .filter(envelope -> envelope.metadata().name().equals("results.event.police-notification-requested-v2"))
                .collect(toList());

        assertThat(policeNoficationRequestedEvents.size(), is(3));

        JsonObject payload1 = getPayload(resourceRoot + "police-notification-requested-1.json");
        JsonObject payload2 = getPayload(resourceRoot + "police-notification-requested-2.json");
        JsonObject payload3 = getPayload(resourceRoot + "police-notification-requested-3.json");

        verifyPoliceNotificationRequests(policeNoficationRequestedEvents, Arrays.asList(payload1, payload2, payload3), createResultEnvelope);
    }

    /***
     *  Amend only the new offence existing result on the case and do NOT amend cloned offence and Application result
     * @throws EventStreamException
     * @throws IOException
     */
    @Test
    void amendNewOffenceNotAmendOthers() throws EventStreamException, IOException {
        final String resourceRoot = "json/amend-new-offence-only/";
        final ResultsAggregate resultsAggregate = new ResultsAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        doReturn(resultsAggregate).when(aggregateService).get(any(EventStream.class), eq(ResultsAggregate.class));

        final JsonObjectBuilder jsonProsecutorBuilder = createObjectBuilder();
        jsonProsecutorBuilder
                .add("spiOutFlag", true)
                .add("policeFlag", true)
                .add("contactEmailAddress", EMAIL)
                .add("mcContactEmailAddress", EMAIL);

        when(referenceDataService.getSpiOutFlagForOriginatingOrganisation(any())).thenReturn(of(jsonProsecutorBuilder.build()));

        resultsAggregate.apply(loadHearingResultsAddedForDay(resourceRoot + "hearing-results-added-for-day-1.json"));
        whenCreateResultCommandHandled(resourceRoot + "create-results-1.json");

        resultsAggregate.apply(loadHearingResultsAddedForDay(resourceRoot + "hearing-results-added-for-day-2.json"));
        final JsonEnvelope createResultEnvelope = whenCreateResultCommandHandled(resourceRoot + "create-results-2.json");

        resultsAggregate.apply(loadHearingResultsAddedForDay(resourceRoot + "hearing-results-added-for-day-3.json"));
        whenCreateResultCommandHandled(resourceRoot + "create-results-3.json");

        resultsAggregate.apply(loadHearingResultsAddedForDay(resourceRoot + "hearing-results-added-for-day-4.json"));
        whenCreateResultCommandHandled(resourceRoot + "create-results-4.json");

        resultsAggregate.apply(loadHearingResultsAddedForDay(resourceRoot + "hearing-results-added-for-day-5.json"));
        whenCreateResultCommandHandled(resourceRoot + "create-results-5.json");

        resultsAggregate.apply(loadHearingResultsAddedForDay(resourceRoot + "hearing-results-added-for-day-6.json"));
        whenCreateResultCommandHandled(resourceRoot + "create-results-6.json");

        verify(eventStream, atLeast(1)).append(streamArgumentCaptor.capture());
        final List<JsonEnvelope> allValues = convertStreamToEventList(streamArgumentCaptor.getAllValues());


        final List<JsonEnvelope> policeNoficationRequestedEvents = allValues.stream()
                .filter(envelope -> envelope.metadata().name().equals("results.event.police-notification-requested-v2"))
                .collect(toList());

        assertThat(policeNoficationRequestedEvents.size(), is(6));

        verifyPoliceNotificationRequests(policeNoficationRequestedEvents, Arrays.asList(
                getPayload(resourceRoot + "police-notification-requested-1.json"),
                getPayload(resourceRoot + "police-notification-requested-2.json"),
                getPayload(resourceRoot + "police-notification-requested-3.json"),
                getPayload(resourceRoot + "police-notification-requested-4.json"),
                getPayload(resourceRoot + "police-notification-requested-5.json"),
                getPayload(resourceRoot + "police-notification-requested-6.json")

        ), createResultEnvelope);
    }


    /**
     * Create command handler for test cases where we expect PoliceResultGeneration and PoliceNotification events
     * and one PoliceNotificationEvent are raised.
     *
     * @throws EventStreamException
     * @throws IOException
     */
    @ParameterizedTest
    @MethodSource("createResultTestCasesParams")
    void createResultTestCases(final String resourceRoot, final int policeResultGeneratedEventCount, final int policeNotificationEventCount) throws EventStreamException, IOException {
        final ResultsAggregate resultsAggregate = new ResultsAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        doReturn(resultsAggregate).when(aggregateService).get(any(EventStream.class), eq(ResultsAggregate.class));

        final JsonObject jsonProsecutor = createObjectBuilder()
                .add("spiOutFlag", true)
                .add("policeFlag", true)
                .add("contactEmailAddress", EMAIL)
                .add("mcContactEmailAddress", EMAIL)
                .build();

        lenient().when(referenceDataService.getSpiOutFlagForOriginatingOrganisation(any())).thenReturn(of(jsonProsecutor));
        lenient().when(referenceDataService.getSpiOutFlagForProsecutionAuthorityCode(any())).thenReturn(of(jsonProsecutor));

        resultsAggregate.apply(loadHearingResultsAddedForDay(resourceRoot + "hearing-results-added-for-day-1.json"));
        whenCreateResultCommandHandled(resourceRoot + "create-results-1.json");


        resultsAggregate.apply(loadHearingResultsAddedForDay(resourceRoot + "hearing-results-added-for-day-2.json"));
        final JsonEnvelope createResultEnvelope = whenCreateResultCommandHandled(resourceRoot + "create-results-2.json");

        verify(eventStream, atLeast(policeResultGeneratedEventCount + policeNotificationEventCount)).append(streamArgumentCaptor.capture());
        final List<JsonEnvelope> allValues = convertStreamToEventList(streamArgumentCaptor.getAllValues());

        final List<JsonObject> policeResultGeneratedEvents = allValues.stream()
                .filter(envelope -> envelope.metadata().name().equals("results.event.police-result-generated"))
                .map(JsonEnvelope::payloadAsJsonObject)
                .collect(toList());

        assertThat(policeResultGeneratedEvents.size(), is(policeResultGeneratedEventCount));

        assertThat(policeResultGeneratedEvents, containsInAnyOrder(
                expectedPoliceResultGeneratedEvents(resourceRoot, policeResultGeneratedEventCount)
        ));


        final List<JsonEnvelope> policeNoficationRequestedEvents = allValues.stream()
                .filter(envelope -> envelope.metadata().name().equals("results.event.police-notification-requested-v2"))
                .collect(toList());

        assertThat(policeNoficationRequestedEvents.size(), is(policeNotificationEventCount));

        verifyPoliceNotificationRequests(policeNoficationRequestedEvents, expectedPoliceNotificationRequestedEvents(resourceRoot, policeNotificationEventCount), createResultEnvelope);
    }


    @Test
    void givenStandaloneApplicationWhenSpiOutFlagTrueAndPoliceFlagTrueThenPoliceResultGeneratedEventIsRaised() throws EventStreamException, IOException {
        final ResultsAggregate resultsAggregate = new ResultsAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        doReturn(resultsAggregate).when(aggregateService).get(any(EventStream.class), eq(ResultsAggregate.class));

        final JsonObject jsonProsecutor = createObjectBuilder()
                .add("spiOutFlag", true)
                .add("policeFlag", true)
                .build();

        lenient().when(referenceDataService.getSpiOutFlagForOriginatingOrganisation(any())).thenReturn(of(jsonProsecutor));
        lenient().when(referenceDataService.getSpiOutFlagForProsecutionAuthorityCode(any())).thenReturn(of(jsonProsecutor));

        resultsAggregate.apply(loadHearingResultsAddedForDay("json/hearing-results-added-for-day_standalone-application.json"));

        final JsonObject commandPayload = getPayload("json/create-results_standalone-application.json");
        final JsonEnvelope commandEnvelope = envelopeFrom(metadataOf(metadataId, "results.command.create-results-for-day"), commandPayload);
        this.resultsCommandHandler.createResultsForDay(commandEnvelope);

        verify(eventStream, times(2)).append(streamArgumentCaptor.capture());
        final List<JsonEnvelope> allValues = convertStreamToEventList(streamArgumentCaptor.getAllValues());

        final List<JsonObject> policeResultGeneratedEvents = allValues.stream()
                .filter(envelope -> envelope.metadata().name().equals("results.event.police-result-generated-for-standalone-application"))
                .map(JsonEnvelope::payloadAsJsonObject)
                .toList();

        assertThat(policeResultGeneratedEvents.size(), is(1));

        final JsonObject eventPayload = policeResultGeneratedEvents.get(0);
        final JsonObject application = commandPayload.getJsonArray("courtApplications").getJsonObject(0);
        final JsonObject session = commandPayload.getJsonObject("session");
        final JsonObject courtCentre = session.getJsonObject("courtCentreWithLJA").getJsonObject("courtCentre");

        assertThat(eventPayload.getString("applicationId"), is(application.getString("id")));
        assertThat(eventPayload.getString("urn"), is(application.getString("applicationReference")));
        assertThat(eventPayload.getJsonObject("defendant").getString("defendantId"), is(application.getJsonObject("subject").getString("id")));
        assertThat(eventPayload.getJsonObject("courtCentreWithLJA").getJsonObject("courtCentre").getString("code"), is(courtCentre.getString("code")));
        assertThat(eventPayload.getJsonObject("courtCentreWithLJA").getJsonObject("courtCentre").getString("id"), is(courtCentre.getString("id")));
        assertThat(eventPayload.getJsonObject("courtCentreWithLJA").getJsonObject("courtCentre").getString("name"), is(courtCentre.getString("name")));
    }

    @Test
    void givenStandaloneApplicationWhenSpiOutFlagFalseAndPoliceFlagTrueThenNoPoliceResultGeneratedEventIsRaised() throws EventStreamException, IOException {
        final ResultsAggregate resultsAggregate = new ResultsAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        doReturn(resultsAggregate).when(aggregateService).get(any(EventStream.class), eq(ResultsAggregate.class));

        final JsonObject jsonProsecutor = createObjectBuilder()
                .add("spiOutFlag", false)
                .add("policeFlag", true)
                .build();

        lenient().when(referenceDataService.getSpiOutFlagForOriginatingOrganisation(any())).thenReturn(of(jsonProsecutor));
        lenient().when(referenceDataService.getSpiOutFlagForProsecutionAuthorityCode(any())).thenReturn(of(jsonProsecutor));

        resultsAggregate.apply(loadHearingResultsAddedForDay("json/hearing-results-added-for-day_standalone-application.json"));

        final JsonObject commandPayload = getPayload("json/create-results_standalone-application.json");
        final JsonEnvelope commandEnvelope = envelopeFrom(metadataOf(metadataId, "results.command.create-results-for-day"), commandPayload);
        this.resultsCommandHandler.createResultsForDay(commandEnvelope);

        verify(eventStream, atLeast(1)).append(streamArgumentCaptor.capture());
        final List<JsonEnvelope> allValues = convertStreamToEventList(streamArgumentCaptor.getAllValues());

        final List<JsonObject> policeResultGeneratedEvents = allValues.stream()
                .filter(envelope -> envelope.metadata().name().equals("results.event.police-result-generated-for-standalone-application"))
                .map(JsonEnvelope::payloadAsJsonObject)
                .toList();

        assertThat(policeResultGeneratedEvents.size(), is(0));

    }

    @Test
    void givenStandaloneApplicationWhenSpiOutFlagTrueAndPoliceFlagFalseThenPoliceResultGeneratedEventIsRaised() throws EventStreamException, IOException {
        final ResultsAggregate resultsAggregate = new ResultsAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        doReturn(resultsAggregate).when(aggregateService).get(any(EventStream.class), eq(ResultsAggregate.class));

        final JsonObject jsonProsecutor = createObjectBuilder()
                .add("spiOutFlag", true)
                .add("policeFlag", false)
                .build();

        lenient().when(referenceDataService.getSpiOutFlagForOriginatingOrganisation(any())).thenReturn(of(jsonProsecutor));
        lenient().when(referenceDataService.getSpiOutFlagForProsecutionAuthorityCode(any())).thenReturn(of(jsonProsecutor));

        resultsAggregate.apply(loadHearingResultsAddedForDay("json/hearing-results-added-for-day_standalone-application.json"));

        final JsonObject commandPayload = getPayload("json/create-results_standalone-application.json");
        final JsonEnvelope commandEnvelope = envelopeFrom(metadataOf(metadataId, "results.command.create-results-for-day"), commandPayload);
        this.resultsCommandHandler.createResultsForDay(commandEnvelope);

        verify(eventStream, atLeast(1)).append(streamArgumentCaptor.capture());
        final List<JsonEnvelope> allValues = convertStreamToEventList(streamArgumentCaptor.getAllValues());

        final List<JsonObject> policeResultGeneratedEvents = allValues.stream()
                .filter(envelope -> envelope.metadata().name().equals("results.event.police-result-generated-for-standalone-application"))
                .map(JsonEnvelope::payloadAsJsonObject)
                .toList();

        assertThat(policeResultGeneratedEvents.size(), is(1));

    }

    @Test
    void givenStandaloneApplicationWhenSingleHearingDayThenPoliceResultGeneratedEventIsRaised() throws EventStreamException, IOException {
        final ResultsAggregate resultsAggregate = new ResultsAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        doReturn(resultsAggregate).when(aggregateService).get(any(EventStream.class), eq(ResultsAggregate.class));

        final JsonObject jsonProsecutor = createObjectBuilder()
                .add("spiOutFlag", true)
                .add("policeFlag", true)
                .build();

        lenient().when(referenceDataService.getSpiOutFlagForOriginatingOrganisation(any())).thenReturn(of(jsonProsecutor));
        lenient().when(referenceDataService.getSpiOutFlagForProsecutionAuthorityCode(any())).thenReturn(of(jsonProsecutor));

        resultsAggregate.apply(loadHearingResultsAddedForDay("json/hearing-results-added-for-day_single-hearing-day_standalone-application.json"));

        final JsonObject commandPayload = getPayload("json/create-results_standalone-application.json");
        final JsonEnvelope commandEnvelope = envelopeFrom(metadataOf(metadataId, "results.command.create-results-for-day"), commandPayload);
        this.resultsCommandHandler.createResultsForDay(commandEnvelope);

        verify(eventStream, atLeast(1)).append(streamArgumentCaptor.capture());
        final List<JsonEnvelope> allValues = convertStreamToEventList(streamArgumentCaptor.getAllValues());

        final List<JsonObject> policeResultGeneratedEvents = allValues.stream()
                .filter(envelope -> envelope.metadata().name().equals("results.event.police-result-generated-for-standalone-application"))
                .map(JsonEnvelope::payloadAsJsonObject)
                .toList();

        assertThat(policeResultGeneratedEvents.size(), is(1));

    }

    @Test
    void givenStandaloneApplicationWhenJurisdictionIsCrownThenNoPoliceResultGeneratedEventIsRaised() throws EventStreamException, IOException {
        final ResultsAggregate resultsAggregate = new ResultsAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        doReturn(resultsAggregate).when(aggregateService).get(any(EventStream.class), eq(ResultsAggregate.class));

        final JsonObject jsonProsecutor = createObjectBuilder()
                .add("spiOutFlag", true)
                .add("policeFlag", true)
                .build();

        lenient().when(referenceDataService.getSpiOutFlagForOriginatingOrganisation(any())).thenReturn(of(jsonProsecutor));
        lenient().when(referenceDataService.getSpiOutFlagForProsecutionAuthorityCode(any())).thenReturn(of(jsonProsecutor));

        resultsAggregate.apply(loadHearingResultsAddedForDay("json/hearing-results-added-for-day_standalone-application.json"));

        final JsonObject commandPayload = getPayload("json/create-results_standalone-application_crown.json");
        final JsonEnvelope commandEnvelope = envelopeFrom(metadataOf(metadataId, "results.command.create-results-for-day"), commandPayload);
        this.resultsCommandHandler.createResultsForDay(commandEnvelope);

        verify(eventStream, atLeast(1)).append(streamArgumentCaptor.capture());
        final List<JsonEnvelope> allValues = convertStreamToEventList(streamArgumentCaptor.getAllValues());

        final List<JsonObject> policeResultGeneratedEvents = allValues.stream()
                .filter(envelope -> envelope.metadata().name().equals("results.event.police-result-generated-for-standalone-application"))
                .map(JsonEnvelope::payloadAsJsonObject)
                .toList();

        assertThat(policeResultGeneratedEvents.size(), is(0));

    }


    /**
     * arg 0: payload
     * arg 1: policeResultGeneratedEventCount
     * arg 2: policeNotificationEventCount
     *
     * @return
     */
    public static Stream<Arguments> createResultTestCasesParams() {
        return Stream.of(
                arguments(named(
                        "Delete the existing result on new offence on the case and cloned offence and do NOT amend Application result",
                        "json/delete-result-new-offence-on-case/"
                ), 1, 1),
                arguments(named(
                        "Add another new result to the cloned offence and do NOT amend new offence and Application result",
                        "json/add-result-cloned-offence/"
                ), 1, 1),
                arguments(named(
                        " Add a new result on Application and cloned offence and do NOT amend new offence result",
                        "json/add-result-on-application-and-cloned-offence/"
                ), 1, 1),
                arguments(named(
                        "Inactive case with 2 defendants but Application for 1 defendant -Amend and Reshare",
                        "json/inactive-case-2-defendant-application-1-defendant/"
                ), 1, 1),
                arguments(named(
                        "Delete the existing result on Application and do NOT amend the result on offence",
                        "json/delete-on-application-not-amend-offence/"
                ), 1, 1),
                arguments(named(
                        "Delete the existing result and add a new result on Application and do NOT amend the result on offence",
                        "json/delete-and-add-on-application-not-amend-offence/"
                ), 1, 1),
                arguments(named(
                        "Multiple case with multiple defendant and multiple offence",
                        "json/multi-case-multi-defendant-multi-offence/"
                ), 4, 2),
                arguments(named(
                        "Multiple case multiple defendant resulted and amended (deleted/updated/added)",
                        "json/multi-case-defendant-amendment-add-remove-update/"
                ), 3, 3),
                arguments(named(
                        "Delete the existing result on offence and do NOT amend result on Application",
                        "json/delete-result-on-offence-dont-amend-application/"
                ), 0, 2),
                arguments(named(
                        "Delete existing result and add another new result to the cloned offence and do NOT amend new offence and Application result",
                        "json/delete-add-cloned-offence-not-amend-others/"
                ), 1, 1),
                arguments(named(
                        "Delete the existing result on Application and cloned offence and do NOT amend new offence result",
                        "json/delete-application-and-clones-offence-result/"
                ), 1, 1),
                arguments(named(
                        "Delete the existing result and add new result on new offence on the case and cloned offence and do NOT amend Application result",
                        "json/delete-add-on-offence-not-amend-others/"
                ), 1, 1),
                arguments(named(
                        "Multiple case multiple defendant resulted and amended (deleted)",
                        "json/multi-case-multi-defendant-result-deleted/"
                ), 2, 2),
                arguments(named(
                        "Delete the existing result and add a new result on offence and do NOT amend result on Application",
                        "json/delete-and-add-offence/"
                ), 1, 1),
                arguments(named(
                        "Amend the existing result on both offence and Application",
                        "json/amend-on-offence-and-application/"
                ), 1, 1),
                arguments(named(
                        "Application resulted and amended on an InActive case",
                        "json/application-amended-on-inactive-case/"
                ), 1, 1),
                arguments(named(
                        "Application and Offences resulted and amended for single InActive case",
                        "json/application-and-offences-amended-on-inactive-case/"
                ), 1, 1),
                arguments(named(
                        "Multiple case with multiple defendant and result NOT amended on defendant offences for 1 or more case",
                        "json/multi-case-with-multi-defendant-result-not-amended-some-cases/"
                ), 2, 1),
                arguments(named(
                        "Multiple case with multiple defendant and multiple offences, result NOT entered on 1 case, later amended and added result in Mags court",
                        "json/multi-case-when-case-resulted-second-share/"
                ), 2, 0)
        );
    }

    private Object[] expectedPoliceResultGeneratedEvents(final String resourceRoot, int count) {
        return IntStream.range(1, count + 1)
                .mapToObj(index -> getPayload(resourceRoot + String.format("police-result-generated-%d.json", index)))
                .toArray();
    }

    private List<JsonObject> expectedPoliceNotificationRequestedEvents(final String resourceRoot, int count) {
        return IntStream.range(1, count + 1)
                .mapToObj(index -> getPayload(resourceRoot + String.format("police-notification-requested-%d.json", index)))
                .collect(toList());
    }

    private void verifyPoliceNotificationRequests(final List<JsonEnvelope> actualEvents, final List<JsonObject> expectedEvents, final JsonEnvelope createResultEnvelope) {

        assertThat(actualEvents, containsInAnyOrder(expectedEvents.stream()
                .map(payload -> policeNotificationMatcher(payload, createResultEnvelope)).collect(toList())));

        final List<JsonObject> policeNoficationRequestedDetails = actualEvents.stream()
                .map(envelope -> envelope.payloadAsJsonObject().getJsonObject("caseResultDetails"))
                .collect(toList());

        assertThat(policeNoficationRequestedDetails, containsInAnyOrder(
                expectedEvents.stream().map(payload -> payload.getJsonObject("caseResultDetails")).toArray()
        ));
    }

    private JsonEnvelopeMatcher policeNotificationMatcher(JsonObject payload, final JsonEnvelope createResultEnvelope) {
        return jsonEnvelope(
                withMetadataEnvelopedFrom(createResultEnvelope)
                        .withName("results.event.police-notification-requested-v2"),
                payloadIsJson(allOf(
                                withJsonPath("$.amendReshare", is(payload.getString("amendReshare"))),
                                withJsonPath("$.applicationTypeForCase", is(payload.getString("applicationTypeForCase"))),
                                withJsonPath("$.caseId", is(payload.getString("caseId"))),
                                withJsonPath("$.applicationId", is(notNullValue()))
                        )
                ));
    }

    private JsonEnvelope whenCreateResultCommandHandled(final String filename) throws EventStreamException {
        final JsonObject payload = getPayload(filename);
        JsonEnvelope envelope = envelopeFrom(metadataOf(metadataId, "results.create-results"), payload);
        this.resultsCommandHandler.createResult(envelope);
        return envelope;
    }

    private HearingResultsAddedForDay loadHearingResultsAddedForDay(final String file) throws IOException {
        ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
        return objectMapper.readValue(Resources.getResource(file), HearingResultsAddedForDay.class);
    }

    private HearingResultsAddedForDay hearingResultsAddedForDay(List<CaseDetails> caseDetailsList) {
        return HearingResultsAddedForDay.hearingResultsAddedForDay()
                .withHearing(Hearing.hearing()
                        .withProsecutionCases(caseDetailsList.stream()
                                .map(this::convertProsecutionCase)
                                .collect(toList()))
                        .build())
                .build();
    }

    private HearingResultsAddedForDay hearingResultsAddedForDay(CaseDetails caseDetails) {
        return HearingResultsAddedForDay.hearingResultsAddedForDay()
                .withHearing(Hearing.hearing()
                        .withProsecutionCases(Arrays.asList(convertProsecutionCase(caseDetails)))
                        .build())
                .build();
    }

    private ProsecutionCase convertProsecutionCase(CaseDetails caseDetails) {
        return ProsecutionCase.prosecutionCase()
                .withId(caseDetails.getCaseId())
                .withDefendants(
                        caseDetails.getDefendants().stream()
                                .map(d -> Defendant.defendant()
                                        .withId(d.getDefendantId())
                                        .withPersonDefendant(PersonDefendant.personDefendant()
                                                .withPersonDetails(Person.person()
                                                        .withFirstName(d.getIndividualDefendant().getPerson().getFirstName())
                                                        .withLastName(d.getIndividualDefendant().getPerson().getLastName())
                                                        .build())
                                                .build())
                                        .withOffences(
                                                d.getOffences().stream()
                                                        .map(o -> Offence.offence()
                                                                .withId(o.getId())
                                                                .withJudicialResults(o.getJudicialResults())
                                                                .build())
                                                        .collect(toList())
                                        )
                                        .build())
                                .collect(toList())
                )
                .build();
    }

    @Test
    public void shouldNotCreateAppealUpdateEventWhenNoResultForApplication() throws EventStreamException {
        final ResultsAggregate resultsAggregate = new ResultsAggregate();

        when(eventSource.getStreamById(any())).thenReturn(this.eventStream);
        when(aggregateService.get(any(EventStream.class), any())).thenReturn(resultsAggregate);

        final JsonObjectBuilder jsonProsecutorBuilder1 = createObjectBuilder();
        jsonProsecutorBuilder1
                .add("spiOutFlag", true)
                .add("policeFlag", true)
                .add("contactEmailAddress", EMAIL)
                .add("mcContactEmailAddress", EMAIL);

        final JsonObjectBuilder jsonProsecutorBuilder2 = createObjectBuilder();
        jsonProsecutorBuilder2
                .add("spiOutFlag", true)
                .add("policeFlag", true)
                .add("informantEmailAddress", EMAIL2);

        final JsonObject payload = getPayload(TEMPLATE_PAYLOAD_APPLICATION_RESULTED_WITHOUT_APPLICATION_LEVEL_RESULTS);
        final JsonEnvelope envelope = envelopeFrom(metadataOf(metadataId, "results.command.create-results-for-day"), payload);
        this.resultsCommandHandler.createResultsForDay(envelope);

        verify(enveloper, Mockito.times(1)).withMetadataFrom(envelope);
        verify(eventStream, Mockito.times(1)).append(streamArgumentCaptor.capture());
        final List<JsonEnvelope> allValues = convertStreamToEventList(streamArgumentCaptor.getAllValues());

        assertThat(allValues, containsInAnyOrder(
                jsonEnvelope(
                        withMetadataEnvelopedFrom(envelope).withName("results.event.session-added-event"),
                        payloadIsJson(allOf(
                                        withJsonPath("$.id", notNullValue()),
                                        withJsonPath("$.courtCentreWithLJA", notNullValue()),
                                        withJsonPath("$.sessionDays", notNullValue())
                                )
                        ))
        ));
    }

    @Test
    public void shouldSendAppealStatusFinalUpdateNotification() throws EventStreamException {
        final ResultsAggregate resultsAggregate = new ResultsAggregate();

        when(eventSource.getStreamById(any())).thenReturn(this.eventStream);
        when(aggregateService.get(any(EventStream.class), any())).thenReturn(resultsAggregate);

        final JsonObjectBuilder jsonProsecutorBuilder1 = createObjectBuilder();
        jsonProsecutorBuilder1
                .add("spiOutFlag", true)
                .add("policeFlag", true)
                .add("contactEmailAddress", EMAIL)
                .add("mcContactEmailAddress", EMAIL);

        final JsonObjectBuilder jsonProsecutorBuilder2 = createObjectBuilder();
        jsonProsecutorBuilder2
                .add("spiOutFlag", true)
                .add("policeFlag", true)
                .add("informantEmailAddress", EMAIL2);

        when(referenceDataService.getSpiOutFlagForProsecutionAuthorityCode(any()))
                .thenReturn(of(jsonProsecutorBuilder1.build()))
                .thenReturn(of(jsonProsecutorBuilder2.build()));

        final JsonObject payload = getPayload(TEMPLATE_RESULT_AGGREGATE_FOR_PROSECUTOR_WITH_LINE_FEED);
        final JsonEnvelope envelope = envelopeFrom(metadataOf(metadataId, "results.command.create-results-for-day"), payload);
        this.resultsCommandHandler.createResultsForDay(envelope);

        verify(enveloper, Mockito.times(2)).withMetadataFrom(envelope);
        verify(eventStream, Mockito.times(2)).append(streamArgumentCaptor.capture());
        final List<JsonEnvelope> allValues = convertStreamToEventList(streamArgumentCaptor.getAllValues());

        assertThat(allValues, containsInAnyOrder(
                jsonEnvelope(withMetadataEnvelopedFrom(envelope).withName("results.event.session-added-event"),
                        payloadIsJson(allOf(
                                        withJsonPath("$.id", notNullValue()),
                                        withJsonPath("$.courtCentreWithLJA", notNullValue()),
                                        withJsonPath("$.sessionDays", notNullValue())))),
                jsonEnvelope(withMetadataEnvelopedFrom(envelope).withName("results.event.appeal-update-notification-requested"),
                        payloadIsJson(allOf(
                                        withJsonPath("$.applicationId", notNullValue()),
                                        withJsonPath("$.subject", is("Appeal Update")),
                                        withJsonPath("$.defendant", is("Korbin Ismael")),
                                        withJsonPath("$.urn", is("20NX8422480")),
                                        withJsonPath("$.notificationId", notNullValue()),
                                        withJsonPath("$.emailAddress", is(EMAIL)))))
        ));
    }

    @Test
    public void shouldSendAppealStatusFinalUpdateWhenMasterDefendantIsNullNotification() throws EventStreamException {
        final ResultsAggregate resultsAggregate = new ResultsAggregate();

        when(eventSource.getStreamById(any())).thenReturn(this.eventStream);
        when(aggregateService.get(any(EventStream.class), any())).thenReturn(resultsAggregate);

        final JsonObjectBuilder jsonProsecutorBuilder1 = createObjectBuilder();
        jsonProsecutorBuilder1
                .add("spiOutFlag", true)
                .add("policeFlag", true)
                .add("contactEmailAddress", EMAIL)
                .add("mcContactEmailAddress", EMAIL);

        final JsonObjectBuilder jsonProsecutorBuilder2 = createObjectBuilder();
        jsonProsecutorBuilder2
                .add("spiOutFlag", true)
                .add("policeFlag", true)
                .add("informantEmailAddress", EMAIL2);

        final JsonObject payload = getPayload(TEMPLATE_RESULT_AGGREGATE_FOR_PROSECUTOR_WITH_LINE_FEED_NULL_MASTER_DEFENDANT);
        final JsonEnvelope envelope = envelopeFrom(metadataOf(metadataId, "results.command.create-results-for-day"), payload);
        this.resultsCommandHandler.createResultsForDay(envelope);

        verify(enveloper, Mockito.times(1)).withMetadataFrom(envelope);
        verify(eventStream, Mockito.times(1)).append(streamArgumentCaptor.capture());
        final List<JsonEnvelope> allValues = convertStreamToEventList(streamArgumentCaptor.getAllValues());

        assertThat(allValues, containsInAnyOrder(
                jsonEnvelope(withMetadataEnvelopedFrom(envelope).withName("results.event.session-added-event"),
                        payloadIsJson(allOf(
                                withJsonPath("$.id", notNullValue()),
                                withJsonPath("$.courtCentreWithLJA", notNullValue()),
                                withJsonPath("$.sessionDays", notNullValue()))))
               ));
    }

    @Test
    public void shouldSendAppealStatusFinalUpdateWhenRespondedntAreEmptyNotification() throws EventStreamException {
        final ResultsAggregate resultsAggregate = new ResultsAggregate();

        when(eventSource.getStreamById(any())).thenReturn(this.eventStream);
        when(aggregateService.get(any(EventStream.class), any())).thenReturn(resultsAggregate);

        final JsonObjectBuilder jsonProsecutorBuilder1 = createObjectBuilder();
        jsonProsecutorBuilder1
                .add("spiOutFlag", true)
                .add("policeFlag", true)
                .add("contactEmailAddress", EMAIL)
                .add("mcContactEmailAddress", EMAIL);

        final JsonObjectBuilder jsonProsecutorBuilder2 = createObjectBuilder();
        jsonProsecutorBuilder2
                .add("spiOutFlag", true)
                .add("policeFlag", true)
                .add("informantEmailAddress", EMAIL2);

        final JsonObject payload = getPayload(TEMPLATE_PAYLOAD_FINANCIAL_PENALTIES_TO_BE_RESPONDS_EMPTY);
        final JsonEnvelope envelope = envelopeFrom(metadataOf(metadataId, "results.command.create-results-for-day"), payload);
        this.resultsCommandHandler.createResultsForDay(envelope);

        verify(enveloper, Mockito.times(1)).withMetadataFrom(envelope);
        verify(eventStream, Mockito.times(1)).append(streamArgumentCaptor.capture());
        final List<JsonEnvelope> allValues = convertStreamToEventList(streamArgumentCaptor.getAllValues());

        assertThat(allValues, containsInAnyOrder(
                jsonEnvelope(withMetadataEnvelopedFrom(envelope).withName("results.event.session-added-event"),
                        payloadIsJson(allOf(
                                withJsonPath("$.id", notNullValue()),
                                withJsonPath("$.courtCentreWithLJA", notNullValue()),
                                withJsonPath("$.sessionDays", notNullValue()))))
        ));
    }

}
