package uk.gov.moj.cpp.results.event.processor;

import static com.google.common.collect.ImmutableList.of;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Arrays.asList;
import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.justice.core.courts.AssociatedIndividual.associatedIndividual;
import static uk.gov.justice.core.courts.BailStatus.bailStatus;
import static uk.gov.justice.core.courts.CaseDefendant.caseDefendant;
import static uk.gov.justice.core.courts.Individual.individual;
import static uk.gov.justice.core.courts.IndividualDefendant.individualDefendant;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataOf;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.results.event.service.TemplateIdentifier.POLICE_NOTIFICATION_HEARING_RESULTS_TEMPLATE;

import com.google.common.io.Resources;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.CaseDefendant;
import uk.gov.justice.core.courts.ContactNumber;
import uk.gov.justice.core.courts.Gender;
import uk.gov.justice.core.courts.Individual;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.OffenceDetails;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.framework.api.JsonObjectConvertersFactory;
import uk.gov.moj.cpp.domains.HearingHelper;
import uk.gov.moj.cpp.domains.results.shareresults.PublicHearingResulted;
import uk.gov.moj.cpp.material.url.MaterialUrlGenerator;
import uk.gov.moj.cpp.results.domain.event.AppealUpdateNotificationRequested;
import uk.gov.moj.cpp.results.domain.event.ApplicationResultDetails;
import uk.gov.moj.cpp.results.domain.event.CaseResultDetails;
import uk.gov.moj.cpp.results.domain.event.DefendantResultDetails;
import uk.gov.moj.cpp.results.domain.event.PoliceNotificationRequestedV2;
import uk.gov.moj.cpp.results.domain.event.PublishToDcs;
import uk.gov.moj.cpp.results.event.helper.BaseStructureConverter;
import uk.gov.moj.cpp.results.event.helper.CasesConverter;
import uk.gov.moj.cpp.results.event.helper.DcsCaseHelper;
import uk.gov.moj.cpp.results.event.helper.PoliceEmailHelper;
import uk.gov.moj.cpp.results.event.helper.ReferenceCache;
import uk.gov.moj.cpp.results.event.helper.resultdefinition.Prompt;
import uk.gov.moj.cpp.results.event.helper.resultdefinition.ResultDefinition;
import uk.gov.moj.cpp.results.event.service.ApplicationParameters;
import uk.gov.moj.cpp.results.event.service.CacheService;
import uk.gov.moj.cpp.results.event.service.ConversionFormat;
import uk.gov.moj.cpp.results.event.service.DocumentGenerationRequest;
import uk.gov.moj.cpp.results.event.service.DocumentGeneratorService;
import uk.gov.moj.cpp.results.event.service.EmailNotification;
import uk.gov.moj.cpp.results.event.service.EventGridService;
import uk.gov.moj.cpp.results.event.service.FileService;
import uk.gov.moj.cpp.results.event.service.FileParams;
import uk.gov.moj.cpp.results.event.service.NotificationNotifyService;
import uk.gov.moj.cpp.results.event.service.ProgressionService;
import uk.gov.moj.cpp.results.event.service.ReferenceDataService;
import uk.gov.moj.cpp.results.event.service.SystemDocGenerator;
import uk.gov.moj.cpp.results.test.TestTemplates;
import uk.gov.moj.cpp.results.event.service.SjpService;

import java.io.StringReader;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonValue;

import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;

public class ResultsEventProcessorTest {

    public static final String FIELD_AMEND_RESHARE = "Amend_Reshare";
    public static final String YES = "yes";
    public static final String AMEND_RESHARE = YES;
    private static final UUID PROMPT_ID = fromString("e4003b92-419b-4e47-b3f9-89a4bbd6741d");
    private static final UUID PROMPT_ID_1 = fromString("e4003b92-419b-4e47-b3f9-89a4bbd6742d");
    private static final UUID RESULT_ID = fromString("e4003b92-419b-4e47-b3f9-89a4bbd6743d");
    private static final UUID NATIONALITY_ID = fromString("dddd4444-1e20-4c21-916a-81a6c90239e5");
    private static final UUID DEFAULT_DEFENDANT_ID = fromString("dddd1111-1e20-4c21-916a-81a6c90239e5");
    private static final UUID DEFAULT_DEFENDANT_ID2 = fromString("dddd2222-1e20-4c21-916a-81a6c90239e5");
    private static final String OU_CODE = "GFL123";
    private static final String URN = "urn123";
    private static final String SUBJECT = "Amend & Reshare urn123 06-06-2023 Croydon Magistrates' Court Bail without conditions / Remand / Final Sentence / Warrant Withdrawn";
    private static final String EMAIL_ADDRESS = "test@hmcts.net";
    private static final String COMMON_PLATFORM_URL = "http://xxx.xx.com/";
    private static final String COMMON_PLATFORM_URL_CAAG = "http://xxx.xx.com/prosecution-casefile/case-at-a-glance/b45b1b7d-ee42-4d02-94d4-41877873bb71";
    private static final String POLICE_TEMPLATE_ID = "781b970d-a13e-4440-97c3-ecf22a4540d5";

    private static final String POLICE_EMAIL_HEARING_RESULTS_TEMPLATE_ID = "efc18c42-bea2-4124-8c02-7a7ae4556b73";

    private static final String POLICE_EMAIL_HEARING_RESULTS_WITH_APPLICATIONS_TEMPLATE_ID = "f6c999fd-0495-4502-90d6-f6dc4676da6f";

    private static final String POLICE_NOTIFICATION_HEARING_RESULTS_AMENDED_TEMPLATE_ID = "5d4f46ba-a4e4-4367-b63a-97240cd314c1";

    private static final String POLICE_EMAIL_HEARING_RESULTS_AMENDED_WITH_APPLICATIONS_TEMPLATE_ID = "f3359bce-8cfb-454f-a504-aa916ea9e9e9";

    private static final String APPEAL_UPDATED_TEMPLATE_ID = "a316a21e-1911-4b77-97fc-ee0118d533af";

    private static final String CASE_ID = "b45b1b7d-ee42-4d02-94d4-41877873bb71";
    private static final String FIELD_SEND_TO_ADDRESS = "sendToAddress";
    private static final String FIELD_PERSONALISATION = "personalisation";
    private static final String FIELD_URN = "URN";
    private static final String FIELD_COMMON_PLATFORM_URL = "common_platform_url";
    private static final String FIELD_COMMON_PLATFORM_URL_CAAG = "common_platform_url_caag";
    private static final String FIELD_COMMON_PLATFORM_URL_AAAG = "common_platform_url_aaag";
    private static final String FIELD_TEMPLATE_ID = "templateId";
    private static final String FIELD_NOTIFICATION_ID = "notificationId";
    public static final String DEFENDANTS = "Jack Smith, Henry Cole";
    public static final String AMENDED_DEFENDANTS_WITHOUT_DETAILS = "Jack Smith, Henry Cole";
    public static final String AMENDED_DEFENDANTS = "Jack Smith<br/>Offence Count:1 Offence Title - ADDED: New Result";
    public static final String AMENDED_APPLICATIONS = "Application 1 - ADDED: New Result";
    public static final String FIELD_DEFENDANTS = "Defendants";
    public static final String FIELD_APPLICATIONS = "Applications";
    public static final String FILED_SUBJECT = "Subject";
    public static final String NO = "no";
    public static final String COURT_CENTRE = "Croydon Magistrates' Court";
    public static final String AMEND_RESHARE1 = "Amend & Reshare";

    @Spy
    private final Enveloper enveloper = createEnveloper();

    @Mock
    ReferenceDataService referenceDataService;

    @Mock
    DcsCaseHelper dcsCaseHelper;
    @Mock
    ProgressionService progressionService;

    @Mock
    ReferenceCache referenceCache;

    @Mock
    private Sender sender;

    @Mock
    private HearingHelper hearingHelper;

    @Mock
    private CacheService cacheService;

    @Mock
    private EventGridService eventGridService;

    @Mock
    private Requester requester;

    @Mock
    private CasesConverter casesConverter;

    @Mock
    private BaseStructureConverter baseStructureConverter;

    @Mock
    private ApplicationParameters applicationParameters;

    @Mock
    private NotificationNotifyService notificationNotifyService;

    @Mock
    private FileService fileService;

    @Mock
    DocumentGeneratorService documentGeneratorService;

    @Mock
    private SystemDocGenerator systemDocGenerator;

    @Mock
    SjpService sjpService;

    @Mock
    MaterialUrlGenerator materialUrlGenerator;

    @Mock
    PoliceEmailHelper policeEmailHelper;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectConvertersFactory().jsonObjectToObjectConverter();

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new JsonObjectConvertersFactory().objectToJsonObjectConverter();

    @InjectMocks
    private ResultsEventProcessor resultsEventProcessor;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeArgumentCaptor;

    @Captor
    private ArgumentCaptor<JsonObject> jsonObjectArgumentCaptor;

    @Captor
    private ArgumentCaptor<JsonEnvelope> jsonEnvelopeArgumentCaptor;

    @Captor
    private ArgumentCaptor<EmailNotification> emailNotificationArgumentCaptor;

    @Spy
    private UtcClock utcClock;

    @BeforeEach
    public void setUp() {
        initMocks(this);
        setField(jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        final JsonObject result = createObjectBuilder()
                .add("isoCode", "USA")
                .add("id", NATIONALITY_ID.toString())
                .build();
        final JsonObject resultPayload = createObjectBuilder().add("countryNationality", createArrayBuilder()
                        .add(
                                result)
                        .build())
                .build();
        final JsonEnvelope envelope = envelopeFrom(metadataOf(randomUUID(), "results.hearing-results-added").build(), resultPayload);

        final JsonEnvelope envelopeForCourt = envelopeFrom(metadataOf(randomUUID(), "referencedata.query.justice-area-national-court-code-and-oucode-mapping").build(), createObjectBuilder().
                add("nationalCourtCode", "1234")
                .add("oucode", "B22KS00")
                .build());
        when(referenceCache.getNationalityById(any())).thenReturn(Optional.of(result));
        when(referenceCache.getResultDefinitionById(any(), any(), any())).thenReturn(buildResultDefinition());
        when(referenceDataService.getOrgainsationUnit(any(), any())).thenReturn(envelopeForCourt.payloadAsJsonObject());
        when(progressionService.caseExistsByCaseUrn(any())).thenReturn(Optional.of(Json.createObjectBuilder().add("caseId", randomUUID().toString()).build()));
    }

    @Test
    public void shouldForwardAsIsAsPrivateEventWhenHearingResulted() {

        final PublicHearingResulted shareResultsMessage = TestTemplates.basicShareResultsWithShadowListedOffencesTemplate();
        final String hearingId = UUID.randomUUID().toString();
        final UUID userId = UUID.randomUUID();
        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("public.hearing.resulted"),
                objectToJsonObjectConverter.convert(shareResultsMessage));
        final JsonObject jsonResult = Json.createObjectBuilder().add("val", randomUUID().toString()).build();
        final JsonObject transformedHearing = envelope.asJsonObject();
        when(hearingHelper.transformedHearing(envelope.payloadAsJsonObject().getJsonObject("hearing"))).thenReturn(transformedHearing.getJsonObject("hearing"));
        when(cacheService.add(hearingId, transformedHearing.getJsonObject("hearing").toString())).thenReturn("");
        when(eventGridService.sendHearingResultedEvent(userId, hearingId, "Hearing_Resulted")).thenReturn(true);

        resultsEventProcessor.hearingResulted(envelope);

        verify(sender).sendAsAdmin(envelopeArgumentCaptor.capture());

        final List<Envelope<JsonObject>> argumentCaptor = envelopeArgumentCaptor.getAllValues();

        final JsonEnvelope allValues = envelopeFrom(argumentCaptor.get(0).metadata(), argumentCaptor.get(0).payload());


        assertThat(allValues,
                jsonEnvelope(
                        metadata().withName("results.command.add-hearing-result"),
                        payloadIsJson(allOf(
                                withJsonPath("$.hearing.id", is(shareResultsMessage.getHearing().getId().toString())),
                                withJsonPath("$.shadowListedOffences[0]", is(shareResultsMessage.getShadowListedOffences().get(0).toString())))
                        )));

    }

    @Test
    public void shouldIssueCreateResultCommandWhenHearingResulted() {

        final PublicHearingResulted shareResultsMessage = TestTemplates.basicShareResultsTemplate(JurisdictionType.MAGISTRATES);

        final UUID offence1Id = shareResultsMessage.getHearing().getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(0).getId();
        final UUID offence2Id = shareResultsMessage.getHearing().getProsecutionCases().get(0).getDefendants().get(1).getOffences().get(0).getId();

        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("results.hearing-results-added"),
                objectToJsonObjectConverter.convert(shareResultsMessage));

        resultsEventProcessor.hearingResultAdded(envelope);

        verify(sender, times(5)).sendAsAdmin(envelopeArgumentCaptor.capture());

        final List<Envelope<JsonObject>> argumentCaptor = envelopeArgumentCaptor.getAllValues();

        final JsonEnvelope createResultsRequestPayload = envelopeFrom(argumentCaptor.get(0).metadata(), argumentCaptor.get(0).payload());

        assertThat(createResultsRequestPayload,
                jsonEnvelope(
                        metadata().withName("results.create-results"),
                        payloadIsJson(allOf(
                                withJsonPath("$.session.id", is(shareResultsMessage.getHearing().getId().toString())),
                                withJsonPath("$.session.sourceType", is("CC")),
                                withJsonPath("$.session.sessionDays.[0].sittingDay", is("2018-05-02T12:01:01.000Z")))

                        )));

        final JsonEnvelope defendant1TrackingResultsRequestPayload = envelopeFrom(argumentCaptor.get(1).metadata(), argumentCaptor.get(1).payload());

        assertThat(defendant1TrackingResultsRequestPayload,
                jsonEnvelope(
                        metadata().withName("results.command.update-defendant-tracking-status"),
                        payloadIsJson(allOf(
                                withJsonPath("$.defendantId", is(DEFAULT_DEFENDANT_ID.toString())),
                                withJsonPath("$.offences[0].id", is(offence1Id.toString()))
                        ))));

        final JsonEnvelope defendant2TrackingResultsRequestPayload = envelopeFrom(argumentCaptor.get(2).metadata(), argumentCaptor.get(2).payload());

        assertThat(defendant2TrackingResultsRequestPayload,
                jsonEnvelope(
                        metadata().withName("results.command.update-defendant-tracking-status"),
                        payloadIsJson(allOf(
                                withJsonPath("$.defendantId", is(DEFAULT_DEFENDANT_ID2.toString())),
                                withJsonPath("$.offences[0].id", is(offence2Id.toString()))

                        ))));
    }

    @Test
    public void shouldIssueCreateResultCommandWhenHearingResultedForDay() {
        final PublicHearingResulted shareResultsMessage = TestTemplates.basicShareResultsV2Template(JurisdictionType.MAGISTRATES);

        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("results.events.hearing-results-added-for-day"),
                objectToJsonObjectConverter.convert(shareResultsMessage));

        resultsEventProcessor.hearingResultAddedForDay(envelope);

        verify(sender, times(5)).sendAsAdmin(envelopeArgumentCaptor.capture());

        final List<Envelope<JsonObject>> argumentCaptor = envelopeArgumentCaptor.getAllValues();

        final JsonEnvelope createResultsForDayRequestPayload = envelopeFrom(argumentCaptor.get(0).metadata(), argumentCaptor.get(0).payload());

        assertThat(createResultsForDayRequestPayload,
                jsonEnvelope(
                        metadata().withName("results.command.create-results-for-day"),
                        payloadIsJson(allOf(
                                withJsonPath("$.session.id", is(shareResultsMessage.getHearing().getId().toString())),
                                withJsonPath("$.session.sourceType", is("CC")),
                                withJsonPath("$.session.sessionDays.[0].sittingDay", is("2018-05-02T12:01:01.000Z")),
                                withJsonPath("$.hearingDay", is("2018-05-02")))
                        )));

        final JsonEnvelope defendantTrackingResultsRequestPayload = envelopeFrom(argumentCaptor.get(1).metadata(), argumentCaptor.get(1).payload());

        assertThat(defendantTrackingResultsRequestPayload,
                jsonEnvelope(
                        metadata().withName("results.command.update-defendant-tracking-status"),
                        payloadIsJson(allOf(
                                withJsonPath("$.defendantId", is(DEFAULT_DEFENDANT_ID.toString()))

                        ))));
    }

    @Test
    public void shouldCreateResultCommandForApplicationsWhenHearingResultedForDay() {
        final JsonObject payload = getPayload("results.events.hearing-results-added-for-day.json");

        final PublicHearingResulted publicHearingResulted = jsonObjectToObjectConverter.convert(payload, PublicHearingResulted.class);

        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("results.events.hearing-results-added-for-day"),
                objectToJsonObjectConverter.convert(publicHearingResulted));

        resultsEventProcessor.hearingResultAddedForDay(envelope);

        verify(sender, times(1)).sendAsAdmin(envelopeArgumentCaptor.capture());

        final List<Envelope<JsonObject>> argumentCaptor = envelopeArgumentCaptor.getAllValues();

        final JsonEnvelope createResultsForDayRequestPayload = envelopeFrom(argumentCaptor.get(0).metadata(), argumentCaptor.get(0).payload());

        assertThat(createResultsForDayRequestPayload,
                jsonEnvelope(
                        metadata().withName("results.command.create-results-for-day"),
                        payloadIsJson(allOf(
                                withJsonPath("$.courtApplications.[0].type.appealFlag", is(true))
                                )
                        )));
    }

    @Test
    public void shouldRaiseAPublicEventPoliceResultGenerated() {
        final String id = "d4a8d314-9cf0-411f-84fc-af603b46a388";
        final JsonEnvelope jsonEnvelope = envelope()
                .with(metadataBuilder().withId(randomUUID()).withName("dummy"))
                .withPayloadOf(id, "id")
                .withPayloadOf(createObjectBuilder().add("id", id).build(), "courtCentre")
                .withPayloadOf(createObjectBuilder().add("id", id).build(), "sessionDays")
                .withPayloadOf("urn123", "urn")
                .withPayloadOf("d4a8d314-9cf0-411f-84fc-af603b46a38813", "caseId")
                .withPayloadOf(createObjectBuilder().add("id", id).build(), "defendant")
                .build();

        resultsEventProcessor.createResult(jsonEnvelope);

        verify(sender).sendAsAdmin(envelopeArgumentCaptor.capture());

        final Envelope<JsonObject> argumentCaptor = envelopeArgumentCaptor.getValue();

        final JsonEnvelope allValues = envelopeFrom(argumentCaptor.metadata(), argumentCaptor.payload());


        assertThat(
                allValues, jsonEnvelope(
                        metadata().withName("public.results.police-result-generated"),
                        payloadIsJson(allOf(
                                withJsonPath("$.id", is(id))))));

    }

    @Test
    public void shouldHandleTheEventPoliceNotificationRequested() {
        final UUID notificationId = randomUUID();

        final JsonEnvelope jsonEnvelope = envelope()
                .with(metadataBuilder().withId(randomUUID()).withName("dummy"))
                .withPayloadOf(URN, "urn")
                .withPayloadOf(EMAIL_ADDRESS, "policeEmailAddress")
                .withPayloadOf(notificationId, "notificationId")
                .build();

        when(referenceDataService.fetchPoliceEmailAddressForProsecutorOuCode(OU_CODE)).thenReturn(EMAIL_ADDRESS);
        when(applicationParameters.getCommonPlatformUrl()).thenReturn(COMMON_PLATFORM_URL);
        when(applicationParameters.getEmailTemplateId()).thenReturn(POLICE_TEMPLATE_ID);

        resultsEventProcessor.handlePoliceNotificationRequested(jsonEnvelope);

        verify(notificationNotifyService, times(1)).sendEmailNotification(
                eq(jsonEnvelope), jsonObjectArgumentCaptor.capture());
        assertThat(jsonObjectArgumentCaptor.getValue().getString(FIELD_NOTIFICATION_ID), is(notificationId.toString()));
        assertThat(jsonObjectArgumentCaptor.getValue().getString(FIELD_TEMPLATE_ID), is(POLICE_TEMPLATE_ID));
        assertThat(jsonObjectArgumentCaptor.getValue().getString(FIELD_SEND_TO_ADDRESS), is(EMAIL_ADDRESS));
        assertThat(jsonObjectArgumentCaptor.getValue().getJsonObject(FIELD_PERSONALISATION).getString(FIELD_URN), is(URN));
        assertThat(jsonObjectArgumentCaptor.getValue().getJsonObject(FIELD_PERSONALISATION).getString(FIELD_COMMON_PLATFORM_URL), is(COMMON_PLATFORM_URL));

    }

    @Test
    public void shouldHandleTheEventPoliceNotificationRequestedV2WithAmendReshareAndApplication() {
        ArgumentCaptor<DocumentGenerationRequest> documentGenerationRequestArgumentCaptor = ArgumentCaptor.forClass(DocumentGenerationRequest.class);
        ArgumentCaptor<JsonObject> fileJsonObjectArgumentCaptor = ArgumentCaptor.forClass(JsonObject.class);
        final List<CaseDefendant> caseDefendants = getDefendants();

        final PoliceNotificationRequestedV2 policeNotificationRequestedV2 =
                buildPoliceNotificationRequestedV2(caseDefendants, "Application to Vary bail", true);
        final UUID notificationId = policeNotificationRequestedV2.getNotificationId();
        final UUID payloadFileId = randomUUID();


        final Metadata metadata = Envelope.metadataBuilder()
                .withId(randomUUID())
                .withName("dummy")
                .build();
        final JsonEnvelope jsonEnvelope = envelopeFrom(metadata, objectToJsonObjectConverter.convert(policeNotificationRequestedV2));

        when(referenceDataService.fetchPoliceEmailAddressForProsecutorOuCode(OU_CODE)).thenReturn(EMAIL_ADDRESS);
        when(applicationParameters.getCommonPlatformUrl()).thenReturn(COMMON_PLATFORM_URL);
        when(applicationParameters.getPoliceNotificationHearingResultsAmendedTemplateId()).thenReturn(POLICE_NOTIFICATION_HEARING_RESULTS_AMENDED_TEMPLATE_ID);
        when(policeEmailHelper.buildDefendantAmendmentDetails(any())).thenReturn(AMENDED_DEFENDANTS);
        when(policeEmailHelper.buildApplicationAmendmentDetails(anyList())).thenReturn(AMENDED_APPLICATIONS);
        when(policeEmailHelper.buildDefendantAmendmentDetails(any())).thenReturn(AMENDED_DEFENDANTS);
        when(fileService.storePayload(any(),eq("POLICE_NOTIFICATION_HEARING_RESULTS"+notificationId+".html"),eq(POLICE_NOTIFICATION_HEARING_RESULTS_TEMPLATE.getValue()),eq(ConversionFormat.THYMELEAF))).thenReturn(payloadFileId);

        resultsEventProcessor.handlePoliceNotificationRequestedV2(jsonEnvelope);

        verify(notificationNotifyService, times(0)).sendEmailNotification(any(), any());
        verify(systemDocGenerator).generateDocument(documentGenerationRequestArgumentCaptor.capture(),any());
        verify(fileService).storePayload(fileJsonObjectArgumentCaptor.capture(), any(), any(), any());

        final DocumentGenerationRequest documentGenerationRequest = documentGenerationRequestArgumentCaptor.getValue();
        final Map<String, String> additionalInformation = documentGenerationRequest.getAdditionalInformation();
        assertThat(additionalInformation.get(FIELD_SEND_TO_ADDRESS), is(EMAIL_ADDRESS));
        assertThat(additionalInformation.get(FIELD_URN), is(URN));
        assertThat(additionalInformation.get(FIELD_AMEND_RESHARE), is(AMEND_RESHARE));
        assertThat(additionalInformation.get(FIELD_DEFENDANTS), is(AMENDED_DEFENDANTS));
        assertThat(additionalInformation.get(FIELD_APPLICATIONS), is(AMENDED_APPLICATIONS));
        assertThat(additionalInformation.get(FILED_SUBJECT), is(SUBJECT));
        assertThat(additionalInformation.get(FIELD_COMMON_PLATFORM_URL_CAAG), is(COMMON_PLATFORM_URL_CAAG));

        final JsonObject fileJsonObject = fileJsonObjectArgumentCaptor.getValue();
        assertThat(fileJsonObject.getString("defendants"), is(AMENDED_DEFENDANTS));
        assertThat(fileJsonObject.getString("applications"), is(AMENDED_APPLICATIONS));

    }


    @Test
    public void shouldHandleTheEventPoliceNotificationRequestedV2WithAmendReshareAndNoApplication() {
        ArgumentCaptor<DocumentGenerationRequest> documentGenerationRequestArgumentCaptor = ArgumentCaptor.forClass(DocumentGenerationRequest.class);
        ArgumentCaptor<JsonObject> fileJsonObjectArgumentCaptor = ArgumentCaptor.forClass(JsonObject.class);
        final List<CaseDefendant> caseDefendants = getDefendantsWithNoApplication();

        final PoliceNotificationRequestedV2 policeNotificationRequestedV2 =
                buildPoliceNotificationRequestedV2(caseDefendants, null, true);
        final UUID notificationId = policeNotificationRequestedV2.getNotificationId();
        final UUID payloadFileId = randomUUID();


        final Metadata metadata = Envelope.metadataBuilder()
                .withId(randomUUID())
                .withName("dummy")
                .build();
        final JsonEnvelope jsonEnvelope = envelopeFrom(metadata, objectToJsonObjectConverter.convert(policeNotificationRequestedV2));

        when(referenceDataService.fetchPoliceEmailAddressForProsecutorOuCode(OU_CODE)).thenReturn(EMAIL_ADDRESS);
        when(applicationParameters.getCommonPlatformUrl()).thenReturn(COMMON_PLATFORM_URL);
        when(applicationParameters.getPoliceNotificationHearingResultsAmendedTemplateId()).thenReturn(POLICE_NOTIFICATION_HEARING_RESULTS_AMENDED_TEMPLATE_ID);
        when(policeEmailHelper.buildDefendantAmendmentDetails(any())).thenReturn(AMENDED_DEFENDANTS);
        when(fileService.storePayload(any(),eq("POLICE_NOTIFICATION_HEARING_RESULTS"+notificationId+".pdf"),eq(POLICE_NOTIFICATION_HEARING_RESULTS_TEMPLATE.getValue()),eq(ConversionFormat.THYMELEAF))).thenReturn(payloadFileId);

        resultsEventProcessor.handlePoliceNotificationRequestedV2(jsonEnvelope);

        verify(notificationNotifyService, times(0)).sendEmailNotification(any(), any());
        verify(systemDocGenerator).generateDocument(documentGenerationRequestArgumentCaptor.capture(),any());
        verify(fileService).storePayload(fileJsonObjectArgumentCaptor.capture(), any(), any(), any());

        final DocumentGenerationRequest documentGenerationRequest = documentGenerationRequestArgumentCaptor.getValue();
        final Map<String, String> additionalInformation = documentGenerationRequest.getAdditionalInformation();


        assertThat(additionalInformation.get(FIELD_SEND_TO_ADDRESS), is(EMAIL_ADDRESS));
        assertThat(additionalInformation.get(FIELD_URN), is(URN));
        assertThat(additionalInformation.get(FIELD_AMEND_RESHARE), is(AMEND_RESHARE));
        assertThat(additionalInformation.get(FIELD_DEFENDANTS), is(AMENDED_DEFENDANTS));
        assertThat(additionalInformation.get(FIELD_COMMON_PLATFORM_URL_CAAG), is(COMMON_PLATFORM_URL_CAAG));

        final JsonObject fileJsonObject = fileJsonObjectArgumentCaptor.getValue();
        assertThat(fileJsonObject.getString("defendants"), is(AMENDED_DEFENDANTS));
        assertThat(fileJsonObject.containsKey("applications"), is(false));

    }

    @Test
    public void shouldHandleTheEventPoliceNotificationRequestedV2WithAmendReshareJudicialResults() {
        ArgumentCaptor<DocumentGenerationRequest> documentGenerationRequestArgumentCaptor = ArgumentCaptor.forClass(DocumentGenerationRequest.class);
        ArgumentCaptor<JsonObject> fileJsonObjectArgumentCaptor = ArgumentCaptor.forClass(JsonObject.class);

        List<String> policeSubjectLineTitle = asList("final sentence", "");
        List<String> resultText = asList("Jorunal", "Fine paid");
        List<CaseDefendant> caseDefendants = getDefendantsWithJudicialResults(policeSubjectLineTitle, resultText);

        PoliceNotificationRequestedV2 policeNotificationRequestedV2 =
                buildPoliceNotificationRequestedV2(caseDefendants, "", true);
        final UUID notificationId = policeNotificationRequestedV2.getNotificationId();
        final UUID payloadFileId = randomUUID();

        final Metadata metadata = Envelope.metadataBuilder()
                .withId(randomUUID())
                .withName("dummy")
                .build();
        final JsonEnvelope jsonEnvelope = envelopeFrom(metadata, objectToJsonObjectConverter.convert(policeNotificationRequestedV2));

        when(referenceDataService.fetchPoliceEmailAddressForProsecutorOuCode(OU_CODE)).thenReturn(EMAIL_ADDRESS);
        when(applicationParameters.getCommonPlatformUrl()).thenReturn(COMMON_PLATFORM_URL);
        when(applicationParameters.getPoliceNotificationHearingResultsAmendedTemplateId()).thenReturn(POLICE_NOTIFICATION_HEARING_RESULTS_AMENDED_TEMPLATE_ID);
        when(policeEmailHelper.buildDefendantAmendmentDetails(any())).thenReturn(AMENDED_DEFENDANTS);
        when(fileService.storePayload(any(), eq("POLICE_NOTIFICATION_HEARING_RESULTS" + notificationId + ".pdf"), eq(POLICE_NOTIFICATION_HEARING_RESULTS_TEMPLATE.getValue()), eq(ConversionFormat.THYMELEAF))).thenReturn(payloadFileId);


        resultsEventProcessor.handlePoliceNotificationRequestedV2(jsonEnvelope);

        verify(notificationNotifyService, times(0)).sendEmailNotification(any(),any());
        verify(systemDocGenerator).generateDocument(documentGenerationRequestArgumentCaptor.capture(),any());
        verify(fileService).storePayload(fileJsonObjectArgumentCaptor.capture(), any(), any(), eq(ConversionFormat.THYMELEAF));

        final DocumentGenerationRequest documentGenerationRequest = documentGenerationRequestArgumentCaptor.getValue();
        final Map<String, String> additionalInformation = documentGenerationRequest.getAdditionalInformation();
        assertThat(additionalInformation.get(FIELD_SEND_TO_ADDRESS), is(EMAIL_ADDRESS));
        assertThat(additionalInformation.get(FIELD_URN), is(URN));
        assertThat(additionalInformation.get(FIELD_AMEND_RESHARE), is(AMEND_RESHARE));
        assertThat(additionalInformation.get(FIELD_DEFENDANTS), is(AMENDED_DEFENDANTS));
        assertThat(additionalInformation.get(FIELD_COMMON_PLATFORM_URL_CAAG), is(COMMON_PLATFORM_URL_CAAG));

        final JsonObject fileJsonObject = fileJsonObjectArgumentCaptor.getValue();
        assertThat(fileJsonObject.getString("defendants"), is(AMENDED_DEFENDANTS));
        assertThat(fileJsonObject.containsKey("applications"), is(false));

    }

    @Test
    public void shouldNotGenerateDetailedDefendantListForFirstReshare() {
        List<String> policeSubjectLineTitle = asList("final sentence", "");
        List<String> resultText = asList("Jorunal", "Fine paid");
        List<CaseDefendant> caseDefendants = getDefendantsWithJudicialResults(policeSubjectLineTitle, resultText);

        final PoliceNotificationRequestedV2 policeNotificationRequestedV2 =
                buildPoliceNotificationRequestedV2(caseDefendants, "Application to Vary bail", false);

        final Metadata metadata = Envelope.metadataBuilder()
                .withId(randomUUID())
                .withName("dummy")
                .build();
        final JsonEnvelope jsonEnvelope = envelopeFrom(metadata, objectToJsonObjectConverter.convert(policeNotificationRequestedV2));

        when(referenceDataService.fetchPoliceEmailAddressForProsecutorOuCode(OU_CODE)).thenReturn(EMAIL_ADDRESS);
        when(applicationParameters.getCommonPlatformUrl()).thenReturn(COMMON_PLATFORM_URL);
        when(applicationParameters.getPoliceEmailHearingResultsWithApplicationTemplateId()).thenReturn(POLICE_EMAIL_HEARING_RESULTS_AMENDED_WITH_APPLICATIONS_TEMPLATE_ID);
        when(policeEmailHelper.buildDefendantAmendmentDetails(any())).thenReturn(AMENDED_DEFENDANTS);

        resultsEventProcessor.handlePoliceNotificationRequestedV2(jsonEnvelope);

        verify(notificationNotifyService, times(1)).sendEmailNotification(
                Mockito.eq(jsonEnvelope), jsonObjectArgumentCaptor.capture());

        assertThat(jsonObjectArgumentCaptor.getValue().getString(FIELD_SEND_TO_ADDRESS), is(EMAIL_ADDRESS));
        assertThat(jsonObjectArgumentCaptor.getValue().getJsonObject(FIELD_PERSONALISATION).getString(FIELD_URN), is(URN));
        assertThat(jsonObjectArgumentCaptor.getValue().getJsonObject(FIELD_PERSONALISATION).getString(FIELD_AMEND_RESHARE), is("no"));
        assertThat(jsonObjectArgumentCaptor.getValue().getJsonObject(FIELD_PERSONALISATION).getString(FIELD_DEFENDANTS), is(DEFENDANTS));
        assertThat(jsonObjectArgumentCaptor.getValue().getJsonObject(FIELD_PERSONALISATION).getString(FIELD_APPLICATIONS), is("Application to Vary bail"));
        assertThat(jsonObjectArgumentCaptor.getValue().getJsonObject(FIELD_PERSONALISATION).getString(FIELD_COMMON_PLATFORM_URL_CAAG), is(COMMON_PLATFORM_URL_CAAG));
    }

    @Test
    public void shouldSendEmailWhenNoCaseDefendantsAndOnlyApplicationResults() {
        List<CaseDefendant> caseDefendants = Collections.emptyList();
        final PoliceNotificationRequestedV2 policeNotificationRequestedV2 =
                buildPoliceNotificationRequestedV2(caseDefendants, "Application to Vary bail", false);

        final Metadata metadata = Envelope.metadataBuilder()
                .withId(randomUUID())
                .withName("dummy")
                .build();
        final JsonEnvelope jsonEnvelope = envelopeFrom(metadata, objectToJsonObjectConverter.convert(policeNotificationRequestedV2));

        when(referenceDataService.fetchPoliceEmailAddressForProsecutorOuCode(OU_CODE)).thenReturn(EMAIL_ADDRESS);
        when(applicationParameters.getCommonPlatformUrl()).thenReturn(COMMON_PLATFORM_URL);
        when(applicationParameters.getEmailTemplateId()).thenReturn(POLICE_TEMPLATE_ID);

        resultsEventProcessor.handlePoliceNotificationRequestedV2(jsonEnvelope);

        verify(notificationNotifyService, times(1)).sendEmailNotification(
                Mockito.eq(jsonEnvelope), jsonObjectArgumentCaptor.capture());

        assertThat(jsonObjectArgumentCaptor.getValue().getString(FIELD_SEND_TO_ADDRESS), is(EMAIL_ADDRESS));
        assertThat(jsonObjectArgumentCaptor.getValue().getJsonObject(FIELD_PERSONALISATION).getString(FIELD_URN), is(URN));
        assertThat(jsonObjectArgumentCaptor.getValue().getJsonObject(FIELD_PERSONALISATION).getString(FIELD_AMEND_RESHARE), is("no"));
        assertThat(jsonObjectArgumentCaptor.getValue().getJsonObject(FIELD_PERSONALISATION).containsKey(DEFENDANTS), is(false));
        assertThat(jsonObjectArgumentCaptor.getValue().getJsonObject(FIELD_PERSONALISATION).containsKey(FIELD_APPLICATIONS), is(false));
        assertThat(jsonObjectArgumentCaptor.getValue().getJsonObject(FIELD_PERSONALISATION).getString(FIELD_COMMON_PLATFORM_URL), is(COMMON_PLATFORM_URL));
    }

    @Test
    public void shouldNotGenerateDetailedDefendantListWhenCaseDetailsNotExists() {
        ArgumentCaptor<DocumentGenerationRequest> documentGenerationRequestArgumentCaptor = ArgumentCaptor.forClass(DocumentGenerationRequest.class);
        ArgumentCaptor<JsonObject> fileJsonObjectArgumentCaptor = ArgumentCaptor.forClass(JsonObject.class);
        final List<String> policeSubjectLineTitle = asList("final sentence", "");
        final List<String> resultText = asList("Jorunal", "Fine paid");
        final List<CaseDefendant> caseDefendants = getDefendantsWithJudicialResults(policeSubjectLineTitle, resultText);

        final PoliceNotificationRequestedV2 policeNotificationRequestedV2 = PoliceNotificationRequestedV2.
                policeNotificationRequestedV2()
                .withNotificationId(randomUUID())
                .withCaseId(CASE_ID)
                .withPoliceEmailAddress(EMAIL_ADDRESS)
                .withAmendReshare(AMEND_RESHARE1)
                .withDateOfHearing(LocalDate.of(2023, 06, 6).toString())
                .withUrn(URN)
                .withCaseDefendants(caseDefendants)
                .withApplicationTypeForCase("Application to Vary bail")
                .build();
        final UUID notificationId = policeNotificationRequestedV2.getNotificationId();
        final UUID payloadFileId = randomUUID();

        final Metadata metadata = Envelope.metadataBuilder()
                .withId(randomUUID())
                .withName("dummy")
                .build();
        final JsonEnvelope jsonEnvelope = envelopeFrom(metadata, objectToJsonObjectConverter.convert(policeNotificationRequestedV2));

        when(referenceDataService.fetchPoliceEmailAddressForProsecutorOuCode(OU_CODE)).thenReturn(EMAIL_ADDRESS);
        when(applicationParameters.getCommonPlatformUrl()).thenReturn(COMMON_PLATFORM_URL);
        when(applicationParameters.getPoliceNotificationHearingResultsAmendedTemplateId()).thenReturn(POLICE_NOTIFICATION_HEARING_RESULTS_AMENDED_TEMPLATE_ID);
        when(policeEmailHelper.buildDefendantAmendmentDetails(any())).thenReturn(AMENDED_DEFENDANTS);
        when(fileService.storePayload(any(),eq("POLICE_NOTIFICATION_HEARING_RESULTS"+notificationId+".pdf"),eq(POLICE_NOTIFICATION_HEARING_RESULTS_TEMPLATE.getValue()), eq(ConversionFormat.THYMELEAF))).thenReturn(payloadFileId);


        resultsEventProcessor.handlePoliceNotificationRequestedV2(jsonEnvelope);

        verify(notificationNotifyService, times(0)).sendEmailNotification(any(), any());
        verify(systemDocGenerator).generateDocument(documentGenerationRequestArgumentCaptor.capture(),any());
        verify(fileService).storePayload(fileJsonObjectArgumentCaptor.capture(), any(), any(), any());

        final DocumentGenerationRequest documentGenerationRequest = documentGenerationRequestArgumentCaptor.getValue();
        final Map<String, String> additionalInformation = documentGenerationRequest.getAdditionalInformation();

        assertThat(additionalInformation.get(FIELD_SEND_TO_ADDRESS), is(EMAIL_ADDRESS));
        assertThat(additionalInformation.get(FIELD_URN), is(URN));
        assertThat(additionalInformation.get(FIELD_AMEND_RESHARE), is("yes"));
        assertThat(additionalInformation.get(FIELD_DEFENDANTS), is(AMENDED_DEFENDANTS_WITHOUT_DETAILS));
        assertThat(additionalInformation.get(FIELD_COMMON_PLATFORM_URL_CAAG), is(COMMON_PLATFORM_URL_CAAG));

        final JsonObject fileJsonObject = fileJsonObjectArgumentCaptor.getValue();
        assertThat(fileJsonObject.getString("defendants"), is(AMENDED_DEFENDANTS_WITHOUT_DETAILS));
        assertThat(fileJsonObject.containsKey("applications"), is(false));
    }

    @Test
    public void shouldNotIncludePoliceSubjectLineTitleWhenTitleIsBCCAndResultTextIsDomesticViolenceCase() {
        ArgumentCaptor<DocumentGenerationRequest> documentGenerationRequestArgumentCaptor = ArgumentCaptor.forClass(DocumentGenerationRequest.class);
        ArgumentCaptor<JsonObject> fileJsonObjectArgumentCaptor = ArgumentCaptor.forClass(JsonObject.class);
        List<String> policeSubjectLineTitle = asList("Bail Conditions Cancelled", null);
        List<String> resultText = asList("URGENT - Urgent\nUrgent result: Domestic Violence case.", null);

        List<CaseDefendant> caseDefendants = getDefendantsWithJudicialResults(policeSubjectLineTitle, resultText);

        PoliceNotificationRequestedV2 policeNotificationRequestedV2 =
                buildPoliceNotificationRequestedV2(caseDefendants, "", true);
        final UUID notificationId = policeNotificationRequestedV2.getNotificationId();
        final UUID payloadFileId = randomUUID();

        final Metadata metadata = Envelope.metadataBuilder()
                .withId(randomUUID())
                .withName("dummy")
                .build();
        final JsonEnvelope jsonEnvelope = envelopeFrom(metadata, objectToJsonObjectConverter.convert(policeNotificationRequestedV2));

        when(referenceDataService.fetchPoliceEmailAddressForProsecutorOuCode(OU_CODE)).thenReturn(EMAIL_ADDRESS);
        when(applicationParameters.getCommonPlatformUrl()).thenReturn(COMMON_PLATFORM_URL);
        when(applicationParameters.getPoliceNotificationHearingResultsAmendedTemplateId()).thenReturn(POLICE_NOTIFICATION_HEARING_RESULTS_AMENDED_TEMPLATE_ID);
        when(policeEmailHelper.buildDefendantAmendmentDetails(any())).thenReturn(AMENDED_DEFENDANTS);
        when(fileService.storePayload(any(),eq("POLICE_NOTIFICATION_HEARING_RESULTS"+notificationId+".pdf"),eq(POLICE_NOTIFICATION_HEARING_RESULTS_TEMPLATE.getValue()), eq(ConversionFormat.THYMELEAF))).thenReturn(payloadFileId);


        resultsEventProcessor.handlePoliceNotificationRequestedV2(jsonEnvelope);

        verify(notificationNotifyService, times(0)).sendEmailNotification(any(), any());
        verify(systemDocGenerator).generateDocument(documentGenerationRequestArgumentCaptor.capture(),any());
        verify(fileService).storePayload(fileJsonObjectArgumentCaptor.capture(), any(), any(), any());

        final DocumentGenerationRequest documentGenerationRequest = documentGenerationRequestArgumentCaptor.getValue();
        final Map<String, String> additionalInformation = documentGenerationRequest.getAdditionalInformation();
        assertThat(additionalInformation.get(FIELD_SEND_TO_ADDRESS), is(EMAIL_ADDRESS));
        assertThat(additionalInformation.get(FIELD_URN), is(URN));
        assertThat(additionalInformation.get(FIELD_AMEND_RESHARE), is(AMEND_RESHARE));
        assertThat(additionalInformation.get(FIELD_DEFENDANTS), is(AMENDED_DEFENDANTS));
        assertThat(additionalInformation.get(FILED_SUBJECT), is("Amend & Reshare urn123 06-06-2023 Croydon Magistrates' Court"));
        assertThat(additionalInformation.get(FIELD_COMMON_PLATFORM_URL_CAAG), is(COMMON_PLATFORM_URL_CAAG));

    }

    @Test
    public void shouldIncludePoliceSubjectLineTitleWhenTitleIsBCC() {
        ArgumentCaptor<DocumentGenerationRequest> documentGenerationRequestArgumentCaptor = ArgumentCaptor.forClass(DocumentGenerationRequest.class);
        ArgumentCaptor<JsonObject> fileJsonObjectArgumentCaptor = ArgumentCaptor.forClass(JsonObject.class);
        final List<String> policeSubjectLineTitle = asList("Bail Conditions Cancelled", null);
        final List<String> resultText = asList("Remanded on conditional bail.", null);

        final List<CaseDefendant> caseDefendants = getDefendantsWithJudicialResults(policeSubjectLineTitle, resultText);

        final PoliceNotificationRequestedV2 policeNotificationRequestedV2 =
                buildPoliceNotificationRequestedV2(caseDefendants, null, true);
        final UUID notificationId = policeNotificationRequestedV2.getNotificationId();
        final UUID payloadFileId = randomUUID();

        final Metadata metadata = Envelope.metadataBuilder()
                .withId(randomUUID())
                .withName("dummy")
                .build();
        final JsonEnvelope jsonEnvelope = envelopeFrom(metadata, objectToJsonObjectConverter.convert(policeNotificationRequestedV2));

        when(referenceDataService.fetchPoliceEmailAddressForProsecutorOuCode(OU_CODE)).thenReturn(EMAIL_ADDRESS);
        when(applicationParameters.getCommonPlatformUrl()).thenReturn(COMMON_PLATFORM_URL);
        when(applicationParameters.getPoliceNotificationHearingResultsAmendedTemplateId()).thenReturn(POLICE_NOTIFICATION_HEARING_RESULTS_AMENDED_TEMPLATE_ID);
        when(policeEmailHelper.buildDefendantAmendmentDetails(any())).thenReturn(AMENDED_DEFENDANTS);
        when(fileService.storePayload(any(),eq("POLICE_NOTIFICATION_HEARING_RESULTS"+notificationId+".pdf"),eq(POLICE_NOTIFICATION_HEARING_RESULTS_TEMPLATE.getValue()), eq(ConversionFormat.THYMELEAF))).thenReturn(payloadFileId);

        resultsEventProcessor.handlePoliceNotificationRequestedV2(jsonEnvelope);

        verify(notificationNotifyService, times(0)).sendEmailNotification(any(),any());
        verify(systemDocGenerator).generateDocument(documentGenerationRequestArgumentCaptor.capture(),any());
        verify(fileService).storePayload(fileJsonObjectArgumentCaptor.capture(), any(), any(), any());

        final DocumentGenerationRequest documentGenerationRequest = documentGenerationRequestArgumentCaptor.getValue();
        final Map<String, String> additionalInformation = documentGenerationRequest.getAdditionalInformation();
        assertThat(additionalInformation.get(FIELD_SEND_TO_ADDRESS), is(EMAIL_ADDRESS));
        assertThat(additionalInformation.get(FIELD_URN), is(URN));
        assertThat(additionalInformation.get(FIELD_AMEND_RESHARE), is(AMEND_RESHARE));
        assertThat(additionalInformation.get(FIELD_DEFENDANTS), is(AMENDED_DEFENDANTS));
        assertThat(additionalInformation.get(FILED_SUBJECT), is("Amend & Reshare urn123 06-06-2023 Croydon Magistrates' Court Bail Conditions Cancelled"));
        assertThat(additionalInformation.get(FIELD_COMMON_PLATFORM_URL_CAAG), is(COMMON_PLATFORM_URL_CAAG));

    }

    @Test
    public void shouldIncludePoliceSubjectLineTitleWhenResultTextIsBCCAnd() {
        ArgumentCaptor<DocumentGenerationRequest> documentGenerationRequestArgumentCaptor = ArgumentCaptor.forClass(DocumentGenerationRequest.class);
        ArgumentCaptor<JsonObject> fileJsonObjectArgumentCaptor = ArgumentCaptor.forClass(JsonObject.class);
        List<String> policeSubjectLineTitle = asList("Bail Conditions Cancelled", null);
        List<String> resultText = asList("URGENT - Urgent\nUrgent result: Vulnerable or Intimidated Victim/Witness.", null);

        List<CaseDefendant> caseDefendants = getDefendantsWithJudicialResults(policeSubjectLineTitle, resultText);

        PoliceNotificationRequestedV2 policeNotificationRequestedV2 =
                buildPoliceNotificationRequestedV2(caseDefendants, "Application X", true);
        final UUID notificationId = policeNotificationRequestedV2.getNotificationId();
        final UUID payloadFileId = randomUUID();

        final Metadata metadata = Envelope.metadataBuilder()
                .withId(randomUUID())
                .withName("dummy")
                .build();
        final JsonEnvelope jsonEnvelope = envelopeFrom(metadata, objectToJsonObjectConverter.convert(policeNotificationRequestedV2));

        when(referenceDataService.fetchPoliceEmailAddressForProsecutorOuCode(OU_CODE)).thenReturn(EMAIL_ADDRESS);
        when(applicationParameters.getCommonPlatformUrl()).thenReturn(COMMON_PLATFORM_URL);
        when(applicationParameters.getPoliceNotificationHearingResultsAmendedTemplateId()).thenReturn(POLICE_NOTIFICATION_HEARING_RESULTS_AMENDED_TEMPLATE_ID);
        when(policeEmailHelper.buildDefendantAmendmentDetails(any())).thenReturn(AMENDED_DEFENDANTS);
        when(policeEmailHelper.buildApplicationAmendmentDetails(anyList())).thenReturn(AMENDED_APPLICATIONS);
        when(fileService.storePayload(any(),eq("POLICE_NOTIFICATION_HEARING_RESULTS"+notificationId+".html"),eq(POLICE_NOTIFICATION_HEARING_RESULTS_TEMPLATE.getValue()), eq(ConversionFormat.THYMELEAF))).thenReturn(payloadFileId);

        resultsEventProcessor.handlePoliceNotificationRequestedV2(jsonEnvelope);

        verify(notificationNotifyService, times(0)).sendEmailNotification(any(), any());
        verify(systemDocGenerator).generateDocument(documentGenerationRequestArgumentCaptor.capture(),any());
        verify(fileService).storePayload(fileJsonObjectArgumentCaptor.capture(), any(), any(), any());

        final DocumentGenerationRequest documentGenerationRequest = documentGenerationRequestArgumentCaptor.getValue();
        assertThat(documentGenerationRequest.getConversionFormat(), is(ConversionFormat.THYMELEAF));
        assertThat(documentGenerationRequest.getOriginatingSource(), is("POLICE_NOTIFICATION_HEARING_RESULTS"));
        assertThat(documentGenerationRequest.getTemplateIdentifier(), is(POLICE_NOTIFICATION_HEARING_RESULTS_TEMPLATE));
        assertThat(documentGenerationRequest.getSourceCorrelationId(), is(notificationId.toString()));
        assertThat(documentGenerationRequest.getPayloadFileServiceId(), is(payloadFileId));
        final Map<String, String> additionalInformation = documentGenerationRequest.getAdditionalInformation();

        assertThat(additionalInformation.get(FIELD_SEND_TO_ADDRESS), is(EMAIL_ADDRESS));
        assertThat(additionalInformation.get(FIELD_URN), is(URN));
        assertThat(additionalInformation.get(FIELD_AMEND_RESHARE), is(AMEND_RESHARE));
        assertThat(additionalInformation.get(FIELD_DEFENDANTS), is(AMENDED_DEFENDANTS));
        assertThat(additionalInformation.get(FIELD_APPLICATIONS), is(AMENDED_APPLICATIONS));
        assertThat(additionalInformation.get(FILED_SUBJECT), is("Amend & Reshare urn123 06-06-2023 Croydon Magistrates' Court"));
        assertThat(additionalInformation.get(FIELD_COMMON_PLATFORM_URL_CAAG), is(COMMON_PLATFORM_URL_CAAG));

        final JsonObject fileJsonObject = fileJsonObjectArgumentCaptor.getValue();
        assertThat(fileJsonObject.getString("defendants"), is(AMENDED_DEFENDANTS));
        assertThat(fileJsonObject.getString("applications"), is(AMENDED_APPLICATIONS));

    }

    @Test
    public void shouldSendAppealUpdateNotification() {
        final UUID notificationId = randomUUID();

        final AppealUpdateNotificationRequested appealUpdateNotificationRequested = AppealUpdateNotificationRequested
                .appealUpdateNotificationRequested()
                .withDefendant("defendant")
                .withUrn("urn")
                .withSubject("subject")
                .withNotificationId(notificationId)
                .withEmailAddress("emailAddress")
                .withApplicationId("applicationId")
                .build();

        final Metadata metadata = Envelope.metadataBuilder()
                .withId(randomUUID())
                .withName("dummy")
                .build();

        final JsonEnvelope jsonEnvelope = envelopeFrom(metadata, objectToJsonObjectConverter.convert(appealUpdateNotificationRequested));

        when(applicationParameters.getAppealUpdateNotificationTemplateId()).thenReturn(APPEAL_UPDATED_TEMPLATE_ID);
        when(applicationParameters.getCommonPlatformUrl()).thenReturn(COMMON_PLATFORM_URL);

        resultsEventProcessor.handleAppealUpdateNotificationRequested(jsonEnvelope);

        verify(notificationNotifyService, times(1)).sendEmailNotification(
                Mockito.eq(jsonEnvelope), jsonObjectArgumentCaptor.capture());

        assertThat(jsonObjectArgumentCaptor.getValue().getString(FIELD_SEND_TO_ADDRESS), is("emailAddress"));
        assertThat(jsonObjectArgumentCaptor.getValue().getString("notificationId"), is(notificationId.toString()));
        assertThat(jsonObjectArgumentCaptor.getValue().getString("templateId"), is("a316a21e-1911-4b77-97fc-ee0118d533af"));
        assertThat(jsonObjectArgumentCaptor.getValue().getJsonObject(FIELD_PERSONALISATION).getString(FIELD_URN), is("urn"));
        assertThat(jsonObjectArgumentCaptor.getValue().getJsonObject(FIELD_PERSONALISATION).getString(FIELD_DEFENDANTS), is("defendant"));
        assertThat(jsonObjectArgumentCaptor.getValue().getJsonObject(FIELD_PERSONALISATION).getString(FIELD_COMMON_PLATFORM_URL), is("http://xxx.xx.com/"));
        assertThat(jsonObjectArgumentCaptor.getValue().getJsonObject(FIELD_PERSONALISATION).getString(FILED_SUBJECT), is("subject"));
        assertThat(jsonObjectArgumentCaptor.getValue().getJsonObject(FIELD_PERSONALISATION).getString(FIELD_COMMON_PLATFORM_URL_AAAG),
                is("http://xxx.xx.com/prosecution-casefile/application-at-a-glance/applicationId"));
    }

    @Test
    public void handleGetCaseSubject(){
        List<CaseDefendant> caseDefendantsList = getDefendantsWithNoIsNewAmendmentBooleanInJudicialResult();
        final String subject = resultsEventProcessor.getCaseSubject(caseDefendantsList);
        assertThat(subject, is("Final Sentence,Warrant Withdrawn,Bail without conditions"));
    }

    private PoliceNotificationRequestedV2 buildPoliceNotificationRequestedV2(final List<CaseDefendant> caseDefendants, final String applicationTypeForCase, final boolean amendAndReshare) {
        return PoliceNotificationRequestedV2.
                policeNotificationRequestedV2()
                .withNotificationId(randomUUID())
                .withCaseId(CASE_ID)
                .withPoliceEmailAddress(EMAIL_ADDRESS)
                .withAmendReshare(amendAndReshare ? AMEND_RESHARE1: "")
                .withDateOfHearing(LocalDate.of(2023, 06, 6).toString())
                .withUrn(URN)
                .withCaseDefendants(caseDefendants)
                .withApplicationTypeForCase(applicationTypeForCase)
                .withCourtCentre(COURT_CENTRE)
                .withCaseResultDetails(CaseResultDetails.caseResultDetails()
                        .withDefendantResultDetails(Collections.singletonList(DefendantResultDetails.defendantResultDetails().build()))
                        .withApplicationResultDetails(nonNull(applicationTypeForCase)
                                ? Collections.singletonList(ApplicationResultDetails.applicationResultDetails().build())
                                : null)
                        .build())
                .build();
    }

    private List<CaseDefendant> getDefendants() {
        JudicialResult judicialResult1 =
                JudicialResult.judicialResult()
                        .withIsNewAmendment(true)
                        .withPoliceSubjectLineTitle("Remand").build();
        JudicialResult judicialResult2 =
                JudicialResult.judicialResult()
                        .withIsNewAmendment(true)
                        .withPoliceSubjectLineTitle("Final Sentence").build();

        List<JudicialResult> judicialResults1 = asList(judicialResult1, judicialResult2);

        JudicialResult judicialResult3 =
                JudicialResult.judicialResult()
                        .withIsNewAmendment(true)
                        .withPoliceSubjectLineTitle("Warrant Withdrawn").build();
        JudicialResult judicialResult4 =
                JudicialResult.judicialResult()
                        .withIsNewAmendment(true)
                        .withPoliceSubjectLineTitle("Bail without conditions").build();

        List<JudicialResult> judicialResults2 = asList(judicialResult3, judicialResult4);


        final CaseDefendant caseDefendant1 = caseDefendant()
                .withDefendantId(UUID.randomUUID())
                .withProsecutorReference("prosecutorReference")
                .withJudicialResults(null)
                .withOffences(Collections.singletonList(OffenceDetails.offenceDetails().withJudicialResults(judicialResults1).build()))
                .withIndividualDefendant(individualDefendant()
                        .withBailConditions("bailCondition")
                        .withBailStatus(bailStatus().withCode("Bail status code").withDescription("Bail status description").withId(randomUUID()).build())
                        .withPerson(createPerson("Jack", "Smith", Gender.MALE, null, "TITLE", null, null)).build())
                .withAssociatedPerson(of(associatedIndividual().withPerson(createPerson("FIRST_NAME_1", "LAST_NAME_1", Gender.MALE, null, "TITLE_1", null, null))
                        .withRole("role").build()))
                .build();
        final CaseDefendant caseDefendant2 = caseDefendant()
                .withDefendantId(UUID.randomUUID())
                .withProsecutorReference("prosecutorReference")
                .withJudicialResults(null)
                .withOffences(Collections.singletonList(OffenceDetails.offenceDetails()
                        .withJudicialResults(judicialResults2).build()))
                .withIndividualDefendant(individualDefendant()
                        .withBailConditions("bailCondition")
                        .withBailStatus(bailStatus().withCode("Bail status code")
                                .withDescription("Bail status description").withId(randomUUID()).build())
                        .withPerson(createPerson("Henry", "Cole", Gender.MALE,
                                null, "TITLE", null, null)).build())
                .withAssociatedPerson(of(associatedIndividual()
                        .withPerson(createPerson("FIRST_NAME_1", "LAST_NAME_1",
                                Gender.MALE, null, "TITLE_1", null, null))
                        .withRole("role").build()))
                .build();
        return asList(caseDefendant1, caseDefendant2);
    }

    private List<CaseDefendant> getDefendantsWithNoApplication() {

        final CaseDefendant caseDefendant1 = caseDefendant()
                .withDefendantId(UUID.randomUUID())
                .withProsecutorReference("prosecutorReference")
                .withJudicialResults(null)
                .withOffences(Collections.singletonList(OffenceDetails.offenceDetails().build()))
                .withIndividualDefendant(individualDefendant()
                        .withBailConditions("bailCondition")
                        .withBailStatus(bailStatus().withCode("Bail status code").withDescription("Bail status description").withId(randomUUID()).build())
                        .withPerson(createPerson("Jack", "Smith", Gender.MALE, null, "TITLE", null, null)).build())
                .withAssociatedPerson(of(associatedIndividual().withPerson(createPerson("FIRST_NAME_1", "LAST_NAME_1", Gender.MALE, null, "TITLE_1", null, null))
                        .withRole("role").build()))
                .build();
        final CaseDefendant caseDefendant2 = caseDefendant()
                .withDefendantId(UUID.randomUUID())
                .withProsecutorReference("prosecutorReference")
                .withJudicialResults(null)
                .withOffences(Collections.singletonList(OffenceDetails.offenceDetails().build()))
                .withIndividualDefendant(individualDefendant()
                        .withBailConditions("bailCondition")
                        .withBailStatus(bailStatus().withCode("Bail status code")
                                .withDescription("Bail status description").withId(randomUUID()).build())
                        .withPerson(createPerson("Henry", "Cole", Gender.MALE,
                                null, "TITLE", null, null)).build())
                .withAssociatedPerson(of(associatedIndividual()
                        .withPerson(createPerson("FIRST_NAME_1", "LAST_NAME_1",
                                Gender.MALE, null, "TITLE_1", null, null))
                        .withRole("role").build()))
                .build();
        return asList(caseDefendant1, caseDefendant2);
    }

    private List<CaseDefendant> getDefendantsWithJudicialResults(List<String> policeSubjectLineTitle, List<String> resultText) {

        JudicialResult judicialResult1 = JudicialResult.judicialResult()
                .withIsNewAmendment(true)
                .withResultText(resultText.get(0)).withPoliceSubjectLineTitle(policeSubjectLineTitle.get(0))
                .withJudicialResultId(randomUUID()).build();
        JudicialResult judicialResult2 = JudicialResult.judicialResult()
                .withIsNewAmendment(true)
                .withResultText(resultText.get(1))
                .withPoliceSubjectLineTitle(policeSubjectLineTitle.get(1)).build();

        final CaseDefendant caseDefendant1 = caseDefendant()
                .withDefendantId(UUID.randomUUID())
                .withProsecutorReference("prosecutorReference")
                .withOffences(Collections.singletonList(OffenceDetails.offenceDetails().withJudicialResults(asList(judicialResult1, judicialResult2)).build()))
                .withIndividualDefendant(individualDefendant()
                        .withBailConditions("bailCondition")
                        .withBailStatus(bailStatus().withCode("Bail status code").withDescription("Bail status description").withId(randomUUID()).build())
                        .withPerson(createPerson("Jack", "Smith", Gender.MALE, null, "TITLE", null, null)).build())
                .withAssociatedPerson(of(associatedIndividual().withPerson(createPerson("FIRST_NAME_1", "LAST_NAME_1", Gender.MALE, null, "TITLE_1", null, null))
                        .withRole("role").build()))
                .build();
        final CaseDefendant caseDefendant2 = caseDefendant()
                .withDefendantId(UUID.randomUUID())
                .withProsecutorReference("prosecutorReference")
                .withJudicialResults(null)
                .withOffences(Collections.singletonList(OffenceDetails.offenceDetails().build()))
                .withIndividualDefendant(individualDefendant()
                        .withBailConditions("bailCondition")
                        .withBailStatus(bailStatus().withCode("Bail status code")
                                .withDescription("Bail status description").withId(randomUUID()).build())
                        .withPerson(createPerson("Henry", "Cole", Gender.MALE,
                                null, "TITLE", null, null)).build())
                .withAssociatedPerson(of(associatedIndividual()
                        .withPerson(createPerson("FIRST_NAME_1", "LAST_NAME_1",
                                Gender.MALE, null, "TITLE_1", null, null))
                        .withRole("role").build()))
                .build();
        return asList(caseDefendant1, caseDefendant2);
    }

    private Individual createPerson(final String firstName, final String lastName, final Gender gender, final LocalDate dateOfBirth, final String title, final Address address, final ContactNumber contactNumber) {
        return individual()
                .withFirstName(firstName)
                .withLastName(lastName)
                .withGender(gender)
                .withDateOfBirth(dateOfBirth)
                .withTitle(title)
                .withAddress(address)
                .withContact(contactNumber)
                .build();
    }

    @Test
    public void shouldHandleTheNcesEmailNotificationRequested() {
        when(fileService.storePayload(any(), anyString(), anyString(), any())).thenReturn(randomUUID());

        final String materialId = randomUUID().toString();
        final String email = "email@email.com";
        final String name = "myname";
        final JsonEnvelope jsonEnvelope = envelope()
                .with(metadataBuilder().withId(randomUUID()).withUserId(randomUUID().toString()).withName("dummy"))
                .withPayloadOf(materialId, "materialId")
                .withPayloadOf("abc11", "caseReferences")
                .withPayloadOf("C", "initiationCode")
                .withPayloadOf(false, "isSJPHearing")
                .build();

        when(documentGeneratorService.generateNcesDocument(any(), any(), any(), any())).thenReturn(new FileParams(randomUUID(), "file123.pdf"));
        resultsEventProcessor.handleNcesEmailNotificationRequested(jsonEnvelope);

        verify(documentGeneratorService, times(1)).generateNcesDocument(
                any(), jsonEnvelopeArgumentCaptor.capture(), any(), any());
        verify(progressionService, times(1)).caseExistsByCaseUrn(any());
        verify(sjpService, times(0)).caseExistsByCaseUrn(any());
        assertThat(jsonEnvelopeArgumentCaptor.getValue().payloadAsJsonObject().getString("materialId"), is(materialId));
    }

    @Test
    public void shouldHandlePublishToDcs() {

        final PublicHearingResulted shareResultsMessage = TestTemplates.basicShareResultsV2Template(JurisdictionType.MAGISTRATES);

        PublishToDcs publishToDcs = PublishToDcs.publishToDcs()
                .withCurrentHearing(shareResultsMessage.getHearing())
                .withSharedTime(shareResultsMessage.getSharedTime())
                .withHearingDay(shareResultsMessage.getHearingDay().get())
                .withIsReshare(shareResultsMessage.getIsReshare().get())
                .build();
        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("results.event.publish-to-dcs"),
                objectToJsonObjectConverter.convert(publishToDcs));

        resultsEventProcessor.publishToDcs(envelope);

        verify(dcsCaseHelper, times(1)).prepareAndSendToDCSIfEligible(jsonEnvelopeArgumentCaptor.capture());

        final JsonObject hearingJsonObject = jsonEnvelopeArgumentCaptor.getValue().payloadAsJsonObject().getJsonObject("currentHearing");

        final uk.gov.justice.core.courts.Hearing hearing = jsonObjectToObjectConverter.convert(hearingJsonObject, uk.gov.justice.core.courts.Hearing.class);
        assertEquals(hearing.getId(), shareResultsMessage.getHearing().getId());
    }

    private void setSjpServiceMock(){
        JsonObject sjpPayload = createObjectBuilder().add("id", randomUUID().toString()).build();
        when(sjpService.caseExistsByCaseUrn(any())).thenReturn(Optional.of(sjpPayload));

    }
    @Test
    public void shouldHandleTheNcesEmailNotificationRequestedWhenSjpCase() {
        final String materialId = randomUUID().toString();
        final String email = "email@email.com";
        final String name = "myname";
        final JsonEnvelope jsonEnvelope = envelope()
                .with(metadataBuilder().withId(randomUUID()).withUserId(randomUUID().toString()).withName("dummy"))
                .withPayloadOf(materialId, "materialId")
                .withPayloadOf("abc1", "caseReferences")
                .withPayloadOf(true, "isSJPHearing")
                .build();

        setSjpServiceMock();
        JsonObject progressionPayload = createObjectBuilder().add("id", randomUUID().toString()).build();

        when(documentGeneratorService.generateNcesDocument(any(), any(), any(), any())).thenReturn(new FileParams(randomUUID(), "file123.pdf"));
        when(progressionService.caseExistsByCaseUrn(any())).thenReturn(Optional.empty());
        resultsEventProcessor.handleNcesEmailNotificationRequested(jsonEnvelope);

        verify(documentGeneratorService, times(1)).generateNcesDocument(
                any(), jsonEnvelopeArgumentCaptor.capture(), any(), any());
        verify(sjpService, times(1)).caseExistsByCaseUrn(any());
        assertThat(jsonEnvelopeArgumentCaptor.getValue().payloadAsJsonObject().getString("materialId"), is(materialId));
    }

    @Test
    public void shouldHandleTheNcesEmailNotificationRequestedWhenCaseIdExistsInCacheForSJPCase() {
        final String materialId = randomUUID().toString();
        final JsonEnvelope jsonEnvelope = envelope()
                .with(metadataBuilder().withId(randomUUID()).withUserId(randomUUID().toString()).withName("dummy"))
                .withPayloadOf(materialId, "materialId")
                .withPayloadOf("abc", "caseReferences")
                .withPayloadOf(true, "isSJPHearing")
                .build();
        setSjpServiceMock();
        when(documentGeneratorService.generateNcesDocument(any(), any(), any(), any())).thenReturn(new FileParams(randomUUID(), "file123.pdf"));
        resultsEventProcessor.handleNcesEmailNotificationRequested(jsonEnvelope);

        verify(documentGeneratorService, times(1)).generateNcesDocument(
                any(), jsonEnvelopeArgumentCaptor.capture(), any(), any());
        assertThat(jsonEnvelopeArgumentCaptor.getValue().payloadAsJsonObject().getString("materialId"), is(materialId));

        resultsEventProcessor.handleNcesEmailNotificationRequested(jsonEnvelope);

        verify(documentGeneratorService, times(2)).generateNcesDocument(
                any(), jsonEnvelopeArgumentCaptor.capture(), any(), any());
        assertThat(jsonEnvelopeArgumentCaptor.getValue().payloadAsJsonObject().getString("materialId"), is(materialId));
    }

    @Test
    public void shouldHandleTheNcesEmailNotificationRequestedWhenProgressionServiceReturnsNull() {
        when(progressionService.caseExistsByCaseUrn(any())).thenReturn(Optional.ofNullable(null));
        JsonObject sjpPayload = createObjectBuilder().add("id", randomUUID().toString()).build();
        when(sjpService.caseExistsByCaseUrn(any())).thenReturn(Optional.ofNullable(sjpPayload));

        final String materialId = randomUUID().toString();
        final JsonEnvelope jsonEnvelope = envelope()
                .with(metadataBuilder().withId(randomUUID()).withUserId(randomUUID().toString()).withName("dummy"))
                .withPayloadOf(materialId, "materialId")
                .withPayloadOf("abc2", "caseReferences")
                .withPayloadOf(false, "isSJPHearing")
                .build();

        when(documentGeneratorService.generateNcesDocument(any(), any(), any(), any())).thenReturn(new FileParams(randomUUID(), "file123.pdf"));
        resultsEventProcessor.handleNcesEmailNotificationRequested(jsonEnvelope);

        verify(documentGeneratorService, times(1)).generateNcesDocument(
                any(), jsonEnvelopeArgumentCaptor.capture(), any(), any());
        verify(progressionService, times(1)).caseExistsByCaseUrn(any());
        assertThat(jsonEnvelopeArgumentCaptor.getValue().payloadAsJsonObject().getString("materialId"), is(materialId));

        resultsEventProcessor.handleNcesEmailNotificationRequested(jsonEnvelope);

        verify(documentGeneratorService, times(2)).generateNcesDocument(
                any(), jsonEnvelopeArgumentCaptor.capture(), any(), any());
        verify(progressionService, times(2)).caseExistsByCaseUrn(any());
        assertThat(jsonEnvelopeArgumentCaptor.getValue().payloadAsJsonObject().getString("materialId"), is(materialId));
    }

    @Test
    public void shouldHandleTheSendNcesEmail() {
        final String sendToAddress = "mail@email.com";
        final String subject = "my subject";
        final String materialUrl = "http://localhost:1234/";
        final String notificationId = randomUUID().toString();
        final String materialId = randomUUID().toString();
        final String templateId = randomUUID().toString();
        final JsonEnvelope jsonEnvelope = envelope()
                .with(metadataBuilder().withId(randomUUID()).withUserId(randomUUID().toString()).withName("dummy"))
                .withPayloadOf(sendToAddress, "sendToAddress")
                .withPayloadOf(subject, "subject")
                .withPayloadOf(materialUrl, "materialUrl")
                .withPayloadOf(notificationId, "notificationId")
                .withPayloadOf(materialId, "materialId")
                .withPayloadOf(templateId, "templateId")
                .build();

        resultsEventProcessor.handleSendNcesEmailNotification(jsonEnvelope);

        verify(notificationNotifyService, times(1)).sendNcesEmail(
                any(), jsonEnvelopeArgumentCaptor.capture());
        verify(notificationNotifyService, times(1)).sendNcesEmail(
                emailNotificationArgumentCaptor.capture(), any(JsonEnvelope.class));
        assertThat(jsonEnvelopeArgumentCaptor.getValue().payloadAsJsonObject().getString("sendToAddress"), is(sendToAddress));
        assertThat(jsonEnvelopeArgumentCaptor.getValue().payloadAsJsonObject().getString("subject"), is(subject));
        assertThat(jsonEnvelopeArgumentCaptor.getValue().payloadAsJsonObject().getString("materialUrl"), is(materialUrl));
        assertThat(jsonEnvelopeArgumentCaptor.getValue().payloadAsJsonObject().getString("notificationId"), is(notificationId));
        assertThat(jsonEnvelopeArgumentCaptor.getValue().payloadAsJsonObject().getString("materialId"), is(materialId));
        assertThat(jsonEnvelopeArgumentCaptor.getValue().payloadAsJsonObject().getString("templateId"), is(templateId));
        assertThat(emailNotificationArgumentCaptor.getValue().getMaterialUrl(), is(materialUrl));
    }

    @Test
    public void shouldHandleTheSendNcesEmailWhenEmailNotAvailable() {
        final String subject = "my subject";
        final String materialUrl = "http://localhost:1234/";
        final String notificationId = randomUUID().toString();
        final String materialId = randomUUID().toString();
        final String templateId = randomUUID().toString();
        final JsonEnvelope jsonEnvelope = envelope()
                .with(metadataBuilder().withId(randomUUID()).withUserId(randomUUID().toString()).withName("dummy"))
                .withPayloadOf(subject, "subject")
                .withPayloadOf(materialUrl, "materialUrl")
                .withPayloadOf(notificationId, "notificationId")
                .withPayloadOf(materialId, "materialId")
                .withPayloadOf(templateId, "templateId")
                .build();

        resultsEventProcessor.handleSendNcesEmailNotification(jsonEnvelope);

        verify(sender).sendAsAdmin(envelopeArgumentCaptor.capture());
        final Envelope<JsonObject> argumentCaptor = envelopeArgumentCaptor.getValue();
        final JsonEnvelope allValues = envelopeFrom(argumentCaptor.metadata(), argumentCaptor.payload());
        assertThat(
                allValues, jsonEnvelope(
                        metadata().withName("results.event.send-nces-email-not-found"),
                        payloadIsJson(allOf(
                                withJsonPath("$.subject", is(subject)),
                                withJsonPath("$.materialUrl", is(materialUrl)),
                                withJsonPath("$.notificationId", is(notificationId)),
                                withJsonPath("$.materialId", is(materialId)),
                                withJsonPath("$.templateId", is(templateId)))
                        )));
    }

    @Test
    public void shouldHandleCaseOrApplicationEjectedWhenPayloadContainsCaseIdExpectedCaseEjectedCommandRaisedWithCaseId() {
        final UUID hearingId1 = randomUUID();
        final UUID hearingId2 = randomUUID();
        final UUID caseId = randomUUID();
        final String PROSECUTION_CASE_ID = "prosecutionCaseId";
        final String HEARING_IDS = "hearingIds";
        final JsonObject payload = Json.createObjectBuilder()
                .add(HEARING_IDS,
                        Json.createArrayBuilder().add(hearingId1.toString()).add(hearingId2.toString()).build())
                .add(PROSECUTION_CASE_ID, caseId.toString())
                .build();
        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("public.progression.events.case-or-application-ejected"), payload);
        resultsEventProcessor.handleCaseOrApplicationEjected(envelope);

        verify(sender).sendAsAdmin(envelopeArgumentCaptor.capture());
        final Envelope<JsonObject> argumentCaptor = envelopeArgumentCaptor.getValue();

        final JsonEnvelope allValues = envelopeFrom(argumentCaptor.metadata(), argumentCaptor.payload());


        assertThat(
                allValues, jsonEnvelope(
                        metadata().withName("results.case-or-application-ejected"),
                        payloadIsJson(allOf(
                                withJsonPath("$.caseId", is(caseId.toString()))))));


    }

    @Test
    public void shouldHandleCaseOrApplicationEjectedWhenPayloadContainsApplicationIdExpectedCaseEjectedCommandRaisedWithApplicationId() {
        final UUID hearingId1 = randomUUID();
        final UUID hearingId2 = randomUUID();
        final UUID applicationId = randomUUID();
        final String APPLICATION_ID = "applicationId";
        final String HEARING_IDS = "hearingIds";
        final JsonObject payload = Json.createObjectBuilder()
                .add(HEARING_IDS,
                        Json.createArrayBuilder().add(hearingId1.toString()).add(hearingId2.toString()).build())
                .add(APPLICATION_ID, applicationId.toString())
                .build();
        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("public.progression.events.case-or-application-ejected"), payload);
        resultsEventProcessor.handleCaseOrApplicationEjected(envelope);

        verify(sender).sendAsAdmin(envelopeArgumentCaptor.capture());
        final Envelope<JsonObject> argumentCaptor = envelopeArgumentCaptor.getValue();

        final JsonEnvelope allValues = envelopeFrom(argumentCaptor.metadata(), argumentCaptor.payload());

        assertThat(
                allValues, jsonEnvelope(
                        metadata().withName("results.case-or-application-ejected"),
                        payloadIsJson(allOf(
                                withJsonPath("$.applicationId", is(applicationId.toString()))))));
    }

    @Test
    public void shouldContinueIfEventGridFailsWhenHearingResulted() {
        final PublicHearingResulted shareResultsMessage = TestTemplates.basicShareResultsTemplate(JurisdictionType.MAGISTRATES);
        final String hearingId = shareResultsMessage.getHearing().getId().toString();
        final UUID userId = randomUUID();
        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("public.hearing.resulted"),
                objectToJsonObjectConverter.convert(shareResultsMessage));
        final JsonObject transformedHearing = envelope.asJsonObject();

        when(hearingHelper.transformedHearing(envelope.payloadAsJsonObject().getJsonObject("hearing"))).thenReturn(transformedHearing.getJsonObject("hearing"));
        when(cacheService.add(hearingId, transformedHearing.getJsonObject("hearing").toString())).thenReturn("");
        when(eventGridService.sendHearingResultedEvent(userId, hearingId, "Hearing_Resulted")).thenThrow(new RuntimeException("Error"));

        resultsEventProcessor.hearingResulted(envelope);

        verify(sender).sendAsAdmin(envelopeArgumentCaptor.capture());
    }

    @Test
    public void shouldUseLAAEventTypeWhenHearingResulted() {
        final PublicHearingResulted shareResultsMessage = TestTemplates.basicShareResultsTemplate(JurisdictionType.MAGISTRATES);
        final String hearingId = shareResultsMessage.getHearing().getId().toString();
        final UUID userId = randomUUID();
        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("public.hearing.resulted"),
                objectToJsonObjectConverter.convert(shareResultsMessage));

        final JsonObject payload = envelope.asJsonObject();
        final JsonObject internalPayload = Json.createObjectBuilder()
                .add("hearing", payload.getJsonObject("hearing"))
                .add("sharedTime", payload.getJsonString("sharedTime"))
                .build();

        final JsonObject transformedHearing = payload.getJsonObject("hearing");

        when(hearingHelper.transformedHearing(envelope.payloadAsJsonObject().getJsonObject("hearing")))
                .thenReturn(transformedHearing);

        final JsonObject externalPayload = Json.createObjectBuilder()
                .add("hearing", transformedHearing)
                .add("sharedTime", payload.getJsonString("sharedTime"))
                .build();

        final String expectedCacheKeyExternal = "EXT_" + hearingId + "_result_";
        final String expectedCacheKeyInternal = "INT_" + hearingId + "_result_";

        when(cacheService.add(expectedCacheKeyExternal, externalPayload.toString())).thenReturn("");
        when(cacheService.add(expectedCacheKeyInternal, internalPayload.toString())).thenReturn("");

        when(eventGridService.sendHearingResultedEvent(userId, hearingId, "Hearing_Resulted")).thenReturn(true);

        resultsEventProcessor.hearingResulted(envelope);

        verify(cacheService, times(1)).add(expectedCacheKeyExternal, externalPayload.toString());
        verify(cacheService, times(1)).add(expectedCacheKeyInternal, internalPayload.toString());

        verify(sender).sendAsAdmin(envelopeArgumentCaptor.capture());
    }

    @Test
    public void shouldUseSJPEventTypeWhenSJPHearingResulted() {
        final PublicHearingResulted shareResultsMessage = TestTemplates.basicShareResultsTemplate(JurisdictionType.MAGISTRATES, true);
        final String hearingId = shareResultsMessage.getHearing().getId().toString();
        final UUID userId = randomUUID();
        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("public.hearing.resulted"),
                objectToJsonObjectConverter.convert(shareResultsMessage));

        final JsonObject payload = envelope.asJsonObject();

        JsonObject transformedHearing = payload.getJsonObject("hearing");
        transformedHearing = jsonObjectToBuilder(transformedHearing).add("isSJPHearing", true).build();

        when(hearingHelper.transformedHearing(envelope.payloadAsJsonObject().getJsonObject("hearing")))
                .thenReturn(transformedHearing);

        final JsonObject externalPayload = Json.createObjectBuilder()
                .add("hearing", transformedHearing)
                .add("sharedTime", payload.getJsonString("sharedTime"))
                .build();

        final String expectedCacheKeyExternal = "SJP_" + hearingId + "_result_";

        when(cacheService.add(expectedCacheKeyExternal, externalPayload.toString())).thenReturn("");

        when(eventGridService.sendHearingResultedEvent(userId, hearingId, "SJP_Hearing_Resulted")).thenReturn(true);

        resultsEventProcessor.hearingResulted(envelope);

        verify(cacheService, times(1)).add(expectedCacheKeyExternal, externalPayload.toString());

        verify(sender).sendAsAdmin(envelopeArgumentCaptor.capture());
    }


    private JsonObjectBuilder jsonObjectToBuilder(JsonObject jo) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        for (Map.Entry<String, JsonValue> entry : jo.entrySet()) {
            builder.add(entry.getKey(), entry.getValue());
        }
        return builder;
    }

    private ResultDefinition buildResultDefinition() {
        final ResultDefinition resultDefinition = new ResultDefinition();
        resultDefinition.setId(RESULT_ID);
        resultDefinition.setAdjournment("Y");
        resultDefinition.setLabel("label");
        resultDefinition.setShortCode("shortCode");
        resultDefinition.setLevel("level");
        resultDefinition.setRank(1);
        resultDefinition.setStartDate(new Date());
        resultDefinition.setEndDate(new Date());
        resultDefinition.setWelshLabel("welshLabel");
        resultDefinition.setIsAvailableForCourtExtract(true);
        resultDefinition.setFinancial("financial");
        resultDefinition.setCategory("A");
        resultDefinition.setCjsCode("cjsCode");
        resultDefinition.setConvicted("Y");
        resultDefinition.setVersion("1.0");
        resultDefinition.setUserGroups(of("1", "2"));
        resultDefinition.setPrompts(of(buildPrompt(PROMPT_ID), buildPrompt(PROMPT_ID_1)));
        return resultDefinition;
    }

    private Prompt buildPrompt(final UUID promptId) {
        final Prompt prompt = new Prompt();
        prompt.setDuration("duration");
        prompt.setLabel("label");
        prompt.setWelshLabel("welshLabel");
        prompt.setMandatory(false);
        prompt.setType("type");
        prompt.setSequence(2);
        prompt.setFixedListId(randomUUID());
        prompt.setReference("reference");
        prompt.setId(promptId);
        return prompt;
    }

    private List<CaseDefendant> getDefendantsWithNoIsNewAmendmentBooleanInJudicialResult() {
        JudicialResult judicialResult1 =
                JudicialResult.judicialResult()
                        .withPoliceSubjectLineTitle("Remand").build();
        JudicialResult judicialResult2 =
                JudicialResult.judicialResult()
                        .withIsNewAmendment(true)
                        .withPoliceSubjectLineTitle("Final Sentence").build();

        List<JudicialResult> judicialResults1 = asList(judicialResult1, judicialResult2);

        JudicialResult judicialResult3 =
                JudicialResult.judicialResult()
                        .withIsNewAmendment(true)
                        .withPoliceSubjectLineTitle("Warrant Withdrawn").build();
        JudicialResult judicialResult4 =
                JudicialResult.judicialResult()
                        .withIsNewAmendment(true)
                        .withPoliceSubjectLineTitle("Bail without conditions").build();

        List<JudicialResult> judicialResults2 = asList(judicialResult3, judicialResult4);


        final CaseDefendant caseDefendant1 = caseDefendant()
                .withDefendantId(UUID.randomUUID())
                .withProsecutorReference("prosecutorReference")
                .withJudicialResults(null)
                .withOffences(Collections.singletonList(OffenceDetails.offenceDetails().withJudicialResults(judicialResults1).build()))
                .withIndividualDefendant(individualDefendant()
                        .withBailConditions("bailCondition")
                        .withBailStatus(bailStatus().withCode("Bail status code").withDescription("Bail status description").withId(randomUUID()).build())
                        .withPerson(createPerson("Jack", "Smith", Gender.MALE, null, "TITLE", null, null)).build())
                .withAssociatedPerson(of(associatedIndividual().withPerson(createPerson("FIRST_NAME_1", "LAST_NAME_1", Gender.MALE, null, "TITLE_1", null, null))
                        .withRole("role").build()))
                .build();
        final CaseDefendant caseDefendant2 = caseDefendant()
                .withDefendantId(UUID.randomUUID())
                .withProsecutorReference("prosecutorReference")
                .withJudicialResults(null)
                .withOffences(Collections.singletonList(OffenceDetails.offenceDetails()
                        .withJudicialResults(judicialResults2).build()))
                .withIndividualDefendant(individualDefendant()
                        .withBailConditions("bailCondition")
                        .withBailStatus(bailStatus().withCode("Bail status code")
                                .withDescription("Bail status description").withId(randomUUID()).build())
                        .withPerson(createPerson("Henry", "Cole", Gender.MALE,
                                null, "TITLE", null, null)).build())
                .withAssociatedPerson(of(associatedIndividual()
                        .withPerson(createPerson("FIRST_NAME_1", "LAST_NAME_1",
                                Gender.MALE, null, "TITLE_1", null, null))
                        .withRole("role").build()))
                .build();
        return asList(caseDefendant1, caseDefendant2);
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

    @Test
    void shouldHandleTheNcesEmailNotificationRequestedWithMultipleCaseUrns() {
        when(fileService.storePayload(any(), anyString(), anyString(), any())).thenReturn(randomUUID());
        final String materialId = randomUUID().toString();
        final String caseUrn1 = "caseUrn1";
        final String caseUrn2 = "caseUrn2";
        final String caseUrn3 = "caseUrn3";
        final String caseReferences = caseUrn1 + "," + caseUrn2 + ", " + caseUrn3; // includes whitespace
        final JsonEnvelope jsonEnvelope = envelope()
                .with(metadataBuilder().withId(randomUUID()).withUserId(randomUUID().toString()).withName("dummy"))
                .withPayloadOf(materialId, "materialId")
                .withPayloadOf(caseReferences, "caseReferences")
                .withPayloadOf(false, "isSJPHearing")
                .build();

        when(documentGeneratorService.generateNcesDocument(any(), any(), any(), any())).thenReturn(new FileParams(randomUUID(), "file123.pdf"));
        when(progressionService.caseExistsByCaseUrn(any())).thenReturn(Optional.of(createObjectBuilder().add("caseId", randomUUID().toString()).build()));

        resultsEventProcessor.handleNcesEmailNotificationRequested(jsonEnvelope);

        verify(documentGeneratorService, times(1)).generateNcesDocument(
                any(), jsonEnvelopeArgumentCaptor.capture(), any(), any());
        // Should call progressionService for each caseUrn
        verify(progressionService, times(3)).caseExistsByCaseUrn(any());
        verify(sjpService, times(0)).caseExistsByCaseUrn(any());
    }

    @Test
    void shouldHandleTheNcesEmailNotificationRequestedWithMultipleSjpCaseUrns() {
        when(fileService.storePayload(any(), anyString(), anyString(), any())).thenReturn(randomUUID());
        final String materialId = randomUUID().toString();
        final String caseUrn1 = "sjpUrn1";
        final String caseUrn2 = "sjpUrn2";
        final String caseUrn3 = "sjpUrn3";
        final String caseReferences = caseUrn1 + "," + caseUrn2 + ", " + caseUrn3;
        final JsonEnvelope jsonEnvelope = envelope()
                .with(metadataBuilder().withId(randomUUID()).withUserId(randomUUID().toString()).withName("dummy"))
                .withPayloadOf(materialId, "materialId")
                .withPayloadOf(caseReferences, "caseReferences")
                .withPayloadOf(true, "isSJPHearing")
                .build();

        when(documentGeneratorService.generateNcesDocument(any(), any(), any(), any())).thenReturn(new FileParams(randomUUID(), "file123.pdf"));
        when(sjpService.caseExistsByCaseUrn(any())).thenReturn(Optional.of(createObjectBuilder().add("id", randomUUID().toString()).build()));

        resultsEventProcessor.handleNcesEmailNotificationRequested(jsonEnvelope);

        verify(documentGeneratorService, times(1)).generateNcesDocument(
                any(), jsonEnvelopeArgumentCaptor.capture(), any(), any());
        verify(sjpService, times(3)).caseExistsByCaseUrn(any());
        verify(progressionService, times(0)).caseExistsByCaseUrn(any());
    }

    @Test
    void shouldHandleTheNcesEmailNotificationRequestedWithSingleCaseUrn() {
        when(fileService.storePayload(any(), anyString(), anyString(), any())).thenReturn(randomUUID());
        final String materialId = randomUUID().toString();
        final String caseUrn = "singleUrn";
        final JsonEnvelope jsonEnvelope = envelope()
                .with(metadataBuilder().withId(randomUUID()).withUserId(randomUUID().toString()).withName("dummy"))
                .withPayloadOf(materialId, "materialId")
                .withPayloadOf(caseUrn, "caseReferences")
                .withPayloadOf(false, "isSJPHearing")
                .build();

        when(documentGeneratorService.generateNcesDocument(any(), any(), any(), any())).thenReturn(new FileParams(randomUUID(), "file123.pdf"));
        when(progressionService.caseExistsByCaseUrn(any())).thenReturn(Optional.of(createObjectBuilder().add("caseId", randomUUID().toString()).build()));

        resultsEventProcessor.handleNcesEmailNotificationRequested(jsonEnvelope);

        verify(documentGeneratorService, times(1)).generateNcesDocument(
                any(), jsonEnvelopeArgumentCaptor.capture(), any(), any());
        verify(progressionService, times(1)).caseExistsByCaseUrn(any());
        verify(sjpService, times(0)).caseExistsByCaseUrn(any());
    }

    @Test
    void shouldHandleTheNcesEmailNotificationRequestedWithEmptyCaseReferences() {
        when(fileService.storePayload(any(), anyString(), anyString(), any())).thenReturn(randomUUID());
        final String materialId = randomUUID().toString();
        final String caseReferences = "   ";
        final JsonEnvelope jsonEnvelope = envelope()
                .with(metadataBuilder().withId(randomUUID()).withUserId(randomUUID().toString()).withName("dummy"))
                .withPayloadOf(materialId, "materialId")
                .withPayloadOf(caseReferences, "caseReferences")
                .withPayloadOf(false, "isSJPHearing")
                .build();

        when(documentGeneratorService.generateNcesDocument(any(), any(), any(), any())).thenReturn(new FileParams(randomUUID(), "file123.pdf"));

        resultsEventProcessor.handleNcesEmailNotificationRequested(jsonEnvelope);

        verify(documentGeneratorService, times(1)).generateNcesDocument(
                any(), jsonEnvelopeArgumentCaptor.capture(), any(), any());
        verify(progressionService, times(0)).caseExistsByCaseUrn(any());
        verify(sjpService, times(0)).caseExistsByCaseUrn(any());
    }

    @Test
    void shouldHandleTheNcesEmailNotificationRequestedWithOnlyCommasAndWhitespace() {
        when(fileService.storePayload(any(), anyString(), anyString(), any())).thenReturn(randomUUID());
        final String materialId = randomUUID().toString();
        final String caseReferences = ", , ,";
        final JsonEnvelope jsonEnvelope = envelope()
                .with(metadataBuilder().withId(randomUUID()).withUserId(randomUUID().toString()).withName("dummy"))
                .withPayloadOf(materialId, "materialId")
                .withPayloadOf(caseReferences, "caseReferences")
                .withPayloadOf(false, "isSJPHearing")
                .build();
        when(documentGeneratorService.generateNcesDocument(any(), any(), any(), any())).thenReturn(new FileParams(randomUUID(), "file123.pdf"));
        resultsEventProcessor.handleNcesEmailNotificationRequested(jsonEnvelope);
        verify(documentGeneratorService, times(1)).generateNcesDocument(any(), jsonEnvelopeArgumentCaptor.capture(), any(), any());
        verify(progressionService, times(0)).caseExistsByCaseUrn(any());
        verify(sjpService, times(0)).caseExistsByCaseUrn(any());
    }

    @Test
    void shouldHandleTheNcesEmailNotificationRequestedWithDuplicateCaseUrns() {
        when(fileService.storePayload(any(), anyString(), anyString(), any())).thenReturn(randomUUID());
        final String materialId = randomUUID().toString();
        final String caseUrn1 = "urn1";
        final String caseUrn2 = "urn2";
        final String caseReferences = caseUrn1 + "," + caseUrn1 + "," + caseUrn2;
        final JsonEnvelope jsonEnvelope = envelope()
                .with(metadataBuilder().withId(randomUUID()).withUserId(randomUUID().toString()).withName("dummy"))
                .withPayloadOf(materialId, "materialId")
                .withPayloadOf(caseReferences, "caseReferences")
                .withPayloadOf(false, "isSJPHearing")
                .build();
        when(documentGeneratorService.generateNcesDocument(any(), any(), any(), any())).thenReturn(new FileParams(randomUUID(), "file123.pdf"));
        when(progressionService.caseExistsByCaseUrn(any())).thenReturn(Optional.of(createObjectBuilder().add("caseId", randomUUID().toString()).build()));
        resultsEventProcessor.handleNcesEmailNotificationRequested(jsonEnvelope);
        verify(documentGeneratorService, times(1)).generateNcesDocument(any(), jsonEnvelopeArgumentCaptor.capture(), any(), any());
        verify(progressionService, times(2)).caseExistsByCaseUrn(any());
        verify(sjpService, times(0)).caseExistsByCaseUrn(any());
    }

    @Test
    void shouldHandleTheNcesEmailNotificationRequestedWithLeadingAndTrailingCommas() {
        when(fileService.storePayload(any(), anyString(), anyString(), any())).thenReturn(randomUUID());
        final String materialId = randomUUID().toString();
        final String caseUrn1 = "urn1";
        final String caseUrn2 = "urn2";
        final String caseReferences = "," + caseUrn1 + "," + caseUrn2 + ",";
        final JsonEnvelope jsonEnvelope = envelope()
                .with(metadataBuilder().withId(randomUUID()).withUserId(randomUUID().toString()).withName("dummy"))
                .withPayloadOf(materialId, "materialId")
                .withPayloadOf(caseReferences, "caseReferences")
                .withPayloadOf(false, "isSJPHearing")
                .build();
        when(documentGeneratorService.generateNcesDocument(any(), any(), any(), any())).thenReturn(new FileParams(randomUUID(), "file123.pdf"));
        when(progressionService.caseExistsByCaseUrn(any())).thenReturn(Optional.of(createObjectBuilder().add("caseId", randomUUID().toString()).build()));
        resultsEventProcessor.handleNcesEmailNotificationRequested(jsonEnvelope);
        verify(documentGeneratorService, times(1)).generateNcesDocument(any(), jsonEnvelopeArgumentCaptor.capture(), any(), any());
        // Should call for each valid urn (2 times)
        verify(progressionService, times(2)).caseExistsByCaseUrn(any());
        verify(sjpService, times(0)).caseExistsByCaseUrn(any());
    }

    @Test
    void shouldHandleTheNcesEmailNotificationRequestedWithMixedEmptyWhitespaceAndValidUrns() {
        when(fileService.storePayload(any(), anyString(), anyString(), any())).thenReturn(randomUUID());
        final String materialId = randomUUID().toString();
        final String caseUrn1 = "urn1";
        final String caseUrn2 = "urn2";
        final String caseReferences = " , ," + caseUrn1 + ", ," + caseUrn2  + ",, ";
        final JsonEnvelope jsonEnvelope = envelope()
                .with(metadataBuilder().withId(randomUUID()).withUserId(randomUUID().toString()).withName("dummy"))
                .withPayloadOf(materialId, "materialId")
                .withPayloadOf(caseReferences, "caseReferences")
                .withPayloadOf(false, "isSJPHearing")
                .build();
        when(documentGeneratorService.generateNcesDocument(any(), any(), any(), any())).thenReturn(new FileParams(randomUUID(), "file123.pdf"));
        when(progressionService.caseExistsByCaseUrn(any())).thenReturn(Optional.of(createObjectBuilder().add("caseId", randomUUID().toString()).build()));
        resultsEventProcessor.handleNcesEmailNotificationRequested(jsonEnvelope);
        verify(documentGeneratorService, times(1)).generateNcesDocument(any(), jsonEnvelopeArgumentCaptor.capture(), any(), any());
        // Should call for each valid urn (2 times)
        verify(progressionService, times(2)).caseExistsByCaseUrn(any());
        verify(sjpService, times(0)).caseExistsByCaseUrn(any());
    }
}
