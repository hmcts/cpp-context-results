package uk.gov.moj.cpp.results.command.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.informantRegisterDocument.InformantRegisterDocumentRequest.informantRegisterDocumentRequest;
import static uk.gov.justice.core.courts.informantRegisterDocument.InformantRegisterRecipient.informantRegisterRecipient;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.InformantRegisterRecorded;
import uk.gov.justice.core.courts.informantRegisterDocument.InformantRegisterCaseOrApplication;
import uk.gov.justice.core.courts.informantRegisterDocument.InformantRegisterDefendant;
import uk.gov.justice.core.courts.informantRegisterDocument.InformantRegisterDocumentRequest;
import uk.gov.justice.core.courts.informantRegisterDocument.InformantRegisterHearing;
import uk.gov.justice.core.courts.informantRegisterDocument.InformantRegisterHearingVenue;
import uk.gov.justice.core.courts.informantRegisterDocument.InformantRegisterOffence;
import uk.gov.justice.core.courts.informantRegisterDocument.InformantRegisterRecipient;
import uk.gov.justice.core.courts.informantRegisterDocument.InformantRegisterResult;
import uk.gov.justice.core.courts.informantRegisterDocument.InformantRegisterResultData;
import uk.gov.justice.results.courts.GenerateInformantRegister;
import uk.gov.justice.results.courts.InformantRegisterGenerated;
import uk.gov.justice.results.courts.InformantRegisterNotificationIgnored;
import uk.gov.justice.results.courts.InformantRegisterNotified;
import uk.gov.justice.results.courts.NotifyInformantRegister;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher;
import uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil;
import uk.gov.moj.cpp.results.command.GenerateInformantRegisterByDate;
import uk.gov.moj.cpp.results.command.service.ProgressionQueryService;
import uk.gov.moj.cpp.results.domain.aggregate.ProsecutionAuthorityAggregate;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class InformantRegisterHandlerTest {

    private static final String ADD_INFORMANT_REGISTER_COMMAND_NAME = "results.command.add-informant-register";
    private static final String RESULT_TYPE_DEFENDANT = "defendant";
    private static final String RESULT_TYPE_CASE = "case";
    private static final String RESULT_TYPE_OFFENCE = "offence";
    private static final UUID PROSECUTION_AUTHORITY_ID = randomUUID();
    private static final UUID GROUP_ID = randomUUID();

    @Mock
    private EventSource eventSource;

    @Mock
    private Requester requester;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private ProgressionQueryService progressionQueryService;

    @InjectMocks
    private InformantRegisterHandler informantRegisterHandler;

    private ProsecutionAuthorityAggregate aggregate;

    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    private final JsonObjectToObjectConverter jsonToObjectConverter = new JsonObjectToObjectConverter(objectMapper);

    @Spy
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);

    @Spy
    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(InformantRegisterRecorded.class, InformantRegisterGenerated.class, InformantRegisterNotified.class, InformantRegisterNotificationIgnored.class);

    @BeforeEach
    public void setup() {
        aggregate = new ProsecutionAuthorityAggregate();
        ReflectionUtil.setField(this.jsonToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldHandleCommand() {
        assertThat(new InformantRegisterHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handleAddInformantRegisterToEventStream")
                        .thatHandles("results.command.add-informant-register")
                ));
    }

    @Test
    public void shouldProcessCommand() throws Exception {

        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ProsecutionAuthorityAggregate.class)).thenReturn(aggregate);

        informantRegisterHandler.handleAddInformantRegisterToEventStream(buildEnvelope());

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata().withName("results.event.informant-register-recorded"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.prosecutionAuthorityId", is(PROSECUTION_AUTHORITY_ID.toString())),
                                withJsonPath("$.informantRegister", notNullValue())
                                )
                        ))
                )
        );
    }

    @Test
    public void shouldProcessCommandForGroupCases() throws Exception {
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ProsecutionAuthorityAggregate.class)).thenReturn(aggregate);
        when(progressionQueryService.getGroupMemberCases(any(), any())).thenReturn(getMemberCasesJson(GROUP_ID, 2));

        informantRegisterHandler.handleAddInformantRegisterToEventStream(buildEnvelope(GROUP_ID, true, true));

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata().withName("results.event.informant-register-recorded"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.prosecutionAuthorityId", is(PROSECUTION_AUTHORITY_ID.toString())),
                                withJsonPath("$.informantRegister", notNullValue()),
                                withJsonPath("$.informantRegister.hearingVenue.courtHouse", is("courtHouse")),
                                withJsonPath("$.informantRegister.hearingVenue.courtSessions[0].defendants", hasSize(3)),
                                withJsonPath("$.informantRegister.hearingVenue.courtSessions[0].defendants[0].title", is("title_Main_1")),
                                withJsonPath("$.informantRegister.hearingVenue.courtSessions[0].defendants[0].firstName", is("firstName_Main_1")),
                                withJsonPath("$.informantRegister.hearingVenue.courtSessions[0].defendants[0].address1", is("address1_Main_1")),
                                withJsonPath("$.informantRegister.hearingVenue.courtSessions[0].defendants[0]." +
                                        "prosecutionCasesOrApplications[0].caseOrApplicationReference", is("caseURN_Main_1")),
                                withJsonPath("$.informantRegister.hearingVenue.courtSessions[0].defendants[0]." +
                                        "prosecutionCasesOrApplications[0].offences[0].offenceCode", is("offenceCode_Main_1")),
                                withJsonPath("$.informantRegister.hearingVenue.courtSessions[0].defendants[0]." +
                                        "prosecutionCasesOrApplications[0].offences[0].offenceResults[0].cjsResultCode", is(RESULT_TYPE_OFFENCE + "CjsResultCode_Main_1")),
                                withJsonPath("$.informantRegister.hearingVenue.courtSessions[0].defendants[0]." +
                                        "prosecutionCasesOrApplications[0].results[0].cjsResultCode", is(RESULT_TYPE_CASE + "CjsResultCode_Main_1")),
                                withJsonPath("$.informantRegister.hearingVenue.courtSessions[0].defendants[0]." +
                                        "results[0].cjsResultCode", is(RESULT_TYPE_DEFENDANT + "CjsResultCode_Main_1")),
                                withJsonPath("$.informantRegister.hearingVenue.courtSessions[0].defendants[1].title", is("title_1_1")),
                                withJsonPath("$.informantRegister.hearingVenue.courtSessions[0].defendants[1].firstName", is("firstName_1_1")),
                                withJsonPath("$.informantRegister.hearingVenue.courtSessions[0].defendants[1].address1", is("address1_1_1")),
                                withJsonPath("$.informantRegister.hearingVenue.courtSessions[0].defendants[1]." +
                                        "prosecutionCasesOrApplications[0].caseOrApplicationReference", is("caseURN_1")),
                                withJsonPath("$.informantRegister.hearingVenue.courtSessions[0].defendants[1]." +
                                        "prosecutionCasesOrApplications[0].offences[0].offenceCode", is("offenceCode_Main_1")),
                                withJsonPath("$.informantRegister.hearingVenue.courtSessions[0].defendants[1]." +
                                        "prosecutionCasesOrApplications[0].offences[0].offenceResults[0].cjsResultCode", is(RESULT_TYPE_OFFENCE + "CjsResultCode_Main_1")),
                                withJsonPath("$.informantRegister.hearingVenue.courtSessions[0].defendants[1]." +
                                        "prosecutionCasesOrApplications[0].results[0].cjsResultCode", is(RESULT_TYPE_CASE + "CjsResultCode_Main_1")),
                                withJsonPath("$.informantRegister.hearingVenue.courtSessions[0].defendants[1]." +
                                        "results[0].cjsResultCode", is(RESULT_TYPE_DEFENDANT + "CjsResultCode_Main_1")),
                                withJsonPath("$.informantRegister.hearingVenue.courtSessions[0].defendants[2].title", is("title_2_1")),
                                withJsonPath("$.informantRegister.hearingVenue.courtSessions[0].defendants[2].firstName", is("firstName_2_1")),
                                withJsonPath("$.informantRegister.hearingVenue.courtSessions[0].defendants[2].address1", is("address1_2_1")),
                                withJsonPath("$.informantRegister.hearingVenue.courtSessions[0].defendants[2]." +
                                        "prosecutionCasesOrApplications[0].caseOrApplicationReference", is("caseURN_2")),
                                withJsonPath("$.informantRegister.hearingVenue.courtSessions[0].defendants[2]." +
                                        "prosecutionCasesOrApplications[0].offences[0].offenceCode", is("offenceCode_Main_1")),
                                withJsonPath("$.informantRegister.hearingVenue.courtSessions[0].defendants[2]." +
                                        "prosecutionCasesOrApplications[0].offences[0].offenceResults[0].cjsResultCode", is(RESULT_TYPE_OFFENCE + "CjsResultCode_Main_1")),
                                withJsonPath("$.informantRegister.hearingVenue.courtSessions[0].defendants[2]." +
                                        "prosecutionCasesOrApplications[0].results[0].cjsResultCode", is(RESULT_TYPE_CASE + "CjsResultCode_Main_1")),
                                withJsonPath("$.informantRegister.hearingVenue.courtSessions[0].defendants[2]." +
                                        "results[0].cjsResultCode", is(RESULT_TYPE_DEFENDANT + "CjsResultCode_Main_1"))
                        )))
                )
        );
    }

    @Test
    public void shouldProcessCommandForGroupCasesWithoutDefAndCaseResults() throws Exception {
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ProsecutionAuthorityAggregate.class)).thenReturn(aggregate);
        when(progressionQueryService.getGroupMemberCases(any(), any())).thenReturn(getMemberCasesJson(GROUP_ID, 2));

        informantRegisterHandler.handleAddInformantRegisterToEventStream(buildEnvelope(GROUP_ID, false, false));

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata().withName("results.event.informant-register-recorded"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.prosecutionAuthorityId", is(PROSECUTION_AUTHORITY_ID.toString())),
                                withJsonPath("$.informantRegister", notNullValue()),
                                withJsonPath("$.informantRegister.hearingVenue.courtHouse", is("courtHouse")),
                                withJsonPath("$.informantRegister.hearingVenue.courtSessions[0].defendants", hasSize(3)),
                                withJsonPath("$.informantRegister.hearingVenue.courtSessions[0].defendants[0].title", is("title_Main_1")),
                                withJsonPath("$.informantRegister.hearingVenue.courtSessions[0].defendants[0]." +
                                        "prosecutionCasesOrApplications[0].offences[0].offenceCode", is("offenceCode_Main_1")),
                                withJsonPath("$.informantRegister.hearingVenue.courtSessions[0].defendants[0]." +
                                        "prosecutionCasesOrApplications[0].offences[0].offenceResults[0].cjsResultCode", is(RESULT_TYPE_OFFENCE + "CjsResultCode_Main_1")),
                                withoutJsonPath("$.informantRegister.hearingVenue.courtSessions[0].defendants[0].prosecutionCasesOrApplications[0].results"),
                                withoutJsonPath("$.informantRegister.hearingVenue.courtSessions[0].defendants[0].results"),
                                withJsonPath("$.informantRegister.hearingVenue.courtSessions[0].defendants[1].title", is("title_1_1")),
                                withJsonPath("$.informantRegister.hearingVenue.courtSessions[0].defendants[1]." +
                                        "prosecutionCasesOrApplications[0].offences[0].offenceCode", is("offenceCode_Main_1")),
                                withJsonPath("$.informantRegister.hearingVenue.courtSessions[0].defendants[1]." +
                                        "prosecutionCasesOrApplications[0].offences[0].offenceResults[0].cjsResultCode", is(RESULT_TYPE_OFFENCE + "CjsResultCode_Main_1")),
                                withoutJsonPath("$.informantRegister.hearingVenue.courtSessions[0].defendants[1].prosecutionCasesOrApplications[0].results"),
                                withoutJsonPath("$.informantRegister.hearingVenue.courtSessions[0].defendants[1].results"),
                                withJsonPath("$.informantRegister.hearingVenue.courtSessions[0].defendants[2].title", is("title_2_1")),
                                withJsonPath("$.informantRegister.hearingVenue.courtSessions[0].defendants[2]." +
                                        "prosecutionCasesOrApplications[0].offences[0].offenceCode", is("offenceCode_Main_1")),
                                withJsonPath("$.informantRegister.hearingVenue.courtSessions[0].defendants[2]." +
                                        "prosecutionCasesOrApplications[0].offences[0].offenceResults[0].cjsResultCode", is(RESULT_TYPE_OFFENCE + "CjsResultCode_Main_1")),
                                withoutJsonPath("$.informantRegister.hearingVenue.courtSessions[0].defendants[2].prosecutionCasesOrApplications[0].results"),
                                withoutJsonPath("$.informantRegister.hearingVenue.courtSessions[0].defendants[2].results")
                        )))
                )
        );
    }

    @Test
    public void generateInformantRegister() throws EventStreamException {
        final Envelope<GenerateInformantRegister> generateInformantRegisterEnvelope = prepareEnvelope();
        final UUID prosecutionAuthorityId = randomUUID();
        final JsonEnvelope queryEnvelope = mock(JsonEnvelope.class);
        final InformantRegisterDocumentRequest informantRegisterDocumentRequest = informantRegisterDocumentRequest().withProsecutionAuthorityId(prosecutionAuthorityId).build();
        final JsonArray jsonValues = createArrayBuilder().add(createObjectBuilder().add("prosecutionAuthorityId", PROSECUTION_AUTHORITY_ID.toString())
                .add("payload", objectToJsonObjectConverter.convert(informantRegisterDocumentRequest).toString())
                .build()).build();
        final JsonObject jsonObject = createObjectBuilder().add("informantRegisterDocumentRequests", jsonValues).build();

        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ProsecutionAuthorityAggregate.class)).thenReturn(aggregate);
        when(queryEnvelope.payloadAsJsonObject()).thenReturn(jsonObject);
        when(requester.request(any(Envelope.class))).thenReturn(queryEnvelope);
        informantRegisterHandler.handleGenerateInformantRegister(generateInformantRegisterEnvelope);
        verifyInformantRegisterDocumentRequestHandlerResults();

    }

    @Test
    public void notifyInformantRegister() throws EventStreamException {
        final UUID materialId = randomUUID();
        final Envelope<NotifyInformantRegister> notifyInformantRegisterEnvelope = prepareNotificationEnvelope(materialId);

        final InformantRegisterRecipient recipient = informantRegisterRecipient().withRecipientName("John").build();
        aggregate.setInformantRegisterRecipients(Collections.singletonList(recipient));

        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ProsecutionAuthorityAggregate.class)).thenReturn(aggregate);

        informantRegisterHandler.handleNotifyInformantRegister(notifyInformantRegisterEnvelope);
        verifyNotifyInformantRegisterDocumentRequestHandlerResults();
    }

    @Test
    public void generateInformantRegisterByDate() throws EventStreamException {
        final Envelope<GenerateInformantRegisterByDate> generateInformantRegisterEnvelope = prepareGenerateInformantRegisterByDateEnvelope();
        final UUID prosecutionAuthorityId = randomUUID();
        final JsonEnvelope queryEnvelope = mock(JsonEnvelope.class);
        final InformantRegisterDocumentRequest informantRegisterDocumentRequest = informantRegisterDocumentRequest().withProsecutionAuthorityId(prosecutionAuthorityId).build();
        final JsonArray jsonValues = createArrayBuilder().add(createObjectBuilder().add("prosecutionAuthorityId", PROSECUTION_AUTHORITY_ID.toString())
                .add("payload", objectToJsonObjectConverter.convert(informantRegisterDocumentRequest).toString())
                .build()).build();
        final JsonObject jsonObject = createObjectBuilder().add("informantRegisterDocumentRequests", jsonValues).build();

        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ProsecutionAuthorityAggregate.class)).thenReturn(aggregate);
        when(queryEnvelope.payloadAsJsonObject()).thenReturn(jsonObject);
        when(requester.request(any(Envelope.class))).thenReturn(queryEnvelope);
        informantRegisterHandler.handleGenerateInformantRegisterByDate(generateInformantRegisterEnvelope);
        verifyInformantRegisterDocumentRequestHandlerResults();

    }


    private Envelope prepareNotificationEnvelope(final UUID fileId) {
        final NotifyInformantRegister notifyInformantRegister = NotifyInformantRegister.notifyInformantRegister()
                .withProsecutionAuthorityId(PROSECUTION_AUTHORITY_ID)
                .withFileId(fileId)
                .withTemplateId("template Id")
                .build();
        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataWithRandomUUID("results.notify-informant-register").withUserId(randomUUID().toString()),
                createObjectBuilder().build());
        return Enveloper.envelop(notifyInformantRegister)
                .withName("results.command.notify-informant-register")
                .withMetadataFrom(requestEnvelope);
    }

    private void verifyNotifyInformantRegisterDocumentRequestHandlerResults() throws EventStreamException {
        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("results.event.informant-register-notified"),
                        JsonEnvelopePayloadMatcher.payload().isJson(anyOf(
                                withJsonPath("prosecutionAuthorityId", is(PROSECUTION_AUTHORITY_ID.toString()))
                                )
                        ))
                )
        );
    }

    private Envelope prepareEnvelope() {
        final GenerateInformantRegister generateInformantRegister = GenerateInformantRegister.generateInformantRegister().withRegisterDate(LocalDate.now()).build();
        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataWithRandomUUID("results.generate-informant-register").withUserId(randomUUID().toString()),
                createObjectBuilder().add("registerDate", LocalDate.now().toString()).build());
        return Enveloper.envelop(generateInformantRegister)
                .withName("results.command.generate-informant-register")
                .withMetadataFrom(requestEnvelope);
    }

    private Envelope prepareGenerateInformantRegisterByDateEnvelope() {
        final GenerateInformantRegisterByDate generateInformantRegister = GenerateInformantRegisterByDate
                .generateInformantRegisterByDate()
                .withRegisterDate(LocalDate.now().toString())
                .withProsecutionAuthorities(Arrays.asList("TFL", "TVL"))
                .build();
        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataWithRandomUUID("results.generate-informant-register-by-date").withUserId(randomUUID().toString()),
                createObjectBuilder().add("registerDate", LocalDate.now().toString())
                        .add("prosecutionAuthorityCode", Arrays.asList("TFL", "TVL").toString()).build());
        return Enveloper.envelop(generateInformantRegister)
                .withName("results.command.generate-informant-register-by-date")
                .withMetadataFrom(requestEnvelope);
    }

    private void verifyInformantRegisterDocumentRequestHandlerResults() throws EventStreamException {
        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("results.event.informant-register-generated"),
                        JsonEnvelopePayloadMatcher.payload().isJson(anyOf(
                                withJsonPath("$.informantRegisterDocumentRequests.length()", is(greaterThan(0)))
                                )
                        ))

                )
        );
    }

    private Envelope<InformantRegisterDocumentRequest> buildEnvelope() {
        return buildEnvelope(null, false, false);
    }

    private Envelope<InformantRegisterDocumentRequest> buildEnvelope(final UUID groupId, final boolean hasDefendantResults, final boolean hasCaseResults) {
        final InformantRegisterDocumentRequest informantRegisterDocumentRequest = informantRegisterDocumentRequest()
                .withGroupId(groupId)
                .withHearingId(randomUUID())
                .withHearingDate(ZonedDateTime.now())
                .withRegisterDate(ZonedDateTime.now().minusDays(1))
                .withProsecutionAuthorityId(PROSECUTION_AUTHORITY_ID)
                .withProsecutionAuthorityCode("prosecutionAuthorityCode")
                .withProsecutionAuthorityOuCode("prosecutionAuthorityOuCode")
                .withProsecutionAuthorityName("prosecutionAuthorityName")
                .withFileName("fileName")
                .withMajorCreditorCode("majorCreditorCode")
                .withRecipients(getRecipients(1))
                .withHearingVenue(InformantRegisterHearingVenue.informantRegisterHearingVenue()
                        .withCourtHouse("courtHouse")
                        .withLjaName("ljaName")
                        .withCourtSessions(getCourtSessions(1, hasDefendantResults, hasCaseResults))
                        .build())
                .build();

        return envelope(ADD_INFORMANT_REGISTER_COMMAND_NAME, informantRegisterDocumentRequest);
    }

    private <T> Envelope<T> envelope(final String name, final T t) {
        final MetadataBuilder metadataBuilder = metadataFrom(metadataWithRandomUUID(name).withUserId(randomUUID().toString()).build());
        return envelopeFrom(metadataBuilder, t);
    }

    private final List<InformantRegisterRecipient> getRecipients(final int count) {
        final List<InformantRegisterRecipient> recipientList = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            recipientList.add(InformantRegisterRecipient.informantRegisterRecipient()
                    .withEmailAddress1("email_" + i + "@hmcts.net")
                    .build());
        }
        return recipientList;
    }

    private final List<InformantRegisterHearing> getCourtSessions(final int count, final boolean hasDefendantResults, final boolean hasCaseResults) {
        final List<InformantRegisterHearing> hearings = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            hearings.add(InformantRegisterHearing.informantRegisterHearing()
                    .withCourtRoom("courtroom_" + i)
                    .withHearingStartTime(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
                    .withDefendants(getDefendants(1, hasDefendantResults, hasCaseResults))
                    .build());
        }
        return hearings;
    }

    private final List<InformantRegisterDefendant> getDefendants(final int count, final boolean hasDefendantResults, final boolean hasCaseResults) {
        final List<InformantRegisterDefendant> defendantList = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            defendantList.add(InformantRegisterDefendant.informantRegisterDefendant()
                    .withTitle("title_Main_" + i)
                    .withFirstName("firstName_Main_" + i)
                    .withLastName("lastName_Main_" + i)
                    .withName("firstName_Main_" + i + " lastName_Main_" + i)
                    .withNationality("nationalityCode_Main_" + i)
                    .withAddress1("address1_Main_" + i)
                    .withPostCode("postCode_Main_" + i)
                    .withProsecutionCasesOrApplications(getCaseOrApplications(1, hasCaseResults))
                    .withResults(hasDefendantResults ? getResults(RESULT_TYPE_DEFENDANT, i, 1) : null)
                    .build());
        }
        return defendantList;
    }

    private final List<InformantRegisterCaseOrApplication> getCaseOrApplications(final int count, final boolean hasCaseResults) {
        final List<InformantRegisterCaseOrApplication> caseOrApplications = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            caseOrApplications.add(
                    InformantRegisterCaseOrApplication.informantRegisterCaseOrApplication()
                            .withArrestSummonsNumber("arrestSummonsNumber_Main_" + i)
                            .withCaseOrApplicationReference("caseURN_Main_" + i)
                            .withApplicationParticulars("applicationParticulars_Main_" + i)
                            .withOffences(Arrays.asList(InformantRegisterOffence.informantRegisterOffence()
                                    .withOrderIndex(1)
                                    .withOffenceTitle("offenceTitle_Main_" + i)
                                    .withOffenceCode("offenceCode_Main_" + i)
                                    .withVerdictCode("verdictCode_Main_" + i)
                                    .withOriginatingCaseUrn("originatingCaseUrn_Main_" + i)
                                    .withPleaValue("pleaValue_Main_" + i)
                                    .withOffenceResults(getResults(RESULT_TYPE_OFFENCE, i, 1))
                                    .build()))
                            .withResults(hasCaseResults ? getResults(RESULT_TYPE_CASE, i, 1) : null)
                            .build());
        }
        return caseOrApplications;
    }

    private final List<InformantRegisterResult> getResults(final String type, final int caseIndex, final int count) {
        final List<InformantRegisterResult> results = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            results.add(InformantRegisterResult.informantRegisterResult()
                    .withResultText(type + "ResultText_Main_" + i)
                    .withCjsResultCode(type + "CjsResultCode_Main_" + i)
                    .withResultData(InformantRegisterResultData.informantRegisterResultData()
                            .withAmount(type + "ResultDataAmount_Main_" + i)
                            .withDurationValue(type + "ResultDataDurationValue_Main_" + i)
                            .withDurationUnit(type + "ResultDataDurationUnit_Main_" + i)
                            .build())
                    .build());
        }
        return results;
    }

    private Optional<JsonObject> getMemberCasesJson(final UUID groupId, final int count) {
        return Optional.of(createObjectBuilder()
                .add("prosecutionCases", getCasesJson(groupId, count))
                .build());
    }

    private JsonArray getCasesJson(final UUID groupId, final int count) {
        final JsonArrayBuilder arrayBuilder = createArrayBuilder();
        for (int i = 1; i <= count; i++) {
            arrayBuilder.add(createObjectBuilder()
                    .add("id", randomUUID().toString())
                    .add("groupId", groupId.toString())
                    .add("isCivil", true)
                    .add("isGroupMember", true)
                    .add("isGroupMaster", false)
                    .add("defendants", getDefendantsJson(i, 1))
                    .add("prosecutionCaseIdentifier", createObjectBuilder()
                            .add("caseURN", "caseURN_" + i)
                            .build())
                    .build());
        }
        return arrayBuilder.build();
    }

    private JsonArray getDefendantsJson(final int caseIndex, final int count) {
        final JsonArrayBuilder arrayBuilder = createArrayBuilder();
        for (int i = 1; i <= count; i++) {
            arrayBuilder.add(createObjectBuilder()
                    .add("id", randomUUID().toString())
                    .add("personDefendant", createObjectBuilder()
                            .add("arrestSummonsNumber", "arrestSummonsNumber_" + caseIndex + "_" + i)
                            .add("personDetails", createObjectBuilder()
                                    .add("title", "title_" + caseIndex + "_" + i)
                                    .add("firstName", "firstName_" + caseIndex + "_" + i)
                                    .add("middleName", "middleName_" + caseIndex + "_" + i)
                                    .add("lastName", "lastName_" + caseIndex + "_" + i)
                                    .add("nationalityCode", "nationalityCode_" + caseIndex + "_" + i)
                                    .add("address", createObjectBuilder()
                                            .add("address1", "address1_" + caseIndex + "_" + i)
                                            .add("address2", "address2_" + caseIndex + "_" + i)
                                            .add("postcode", "postcode_" + caseIndex + "_" + i)
                                            .build())
                                    .build())
                            .build())
                    .build());
        }
        return arrayBuilder.build();
    }
}