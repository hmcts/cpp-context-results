package uk.gov.moj.cpp.results.it;

import static java.nio.charset.Charset.defaultCharset;
import static java.time.LocalDate.of;
import static java.util.Arrays.asList;
import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createReader;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang3.StringUtils.countMatches;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.closeMessageConsumers;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.createMessageConsumers;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.getSummariesByDate;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.hearingResultsHaveBeenSharedV2;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.verifyPrivateEventsPoliceNotificationRequestedGeneratedV2;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.verifyPrivateEventsWithPoliceResultGenerated;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.whenPrisonAdminTriesToViewResultsForThePerson;
import static uk.gov.moj.cpp.results.it.steps.data.factory.HearingResultDataFactory.getUserId;
import static uk.gov.moj.cpp.results.it.stub.NotificationServiceStub.verifyEmailNotificationIsRaised;
import static uk.gov.moj.cpp.results.it.stub.ProgressionStub.stubGetProgressionCaseExistsByUrn;
import static uk.gov.moj.cpp.results.it.stub.ProgressionStub.stubGetProgressionProsecutionCase_WhichHasLikedCases_AndHearings;
import static uk.gov.moj.cpp.results.it.stub.ProgressionStub.stubGetProgressionProsecutionCasesFromPayload;
import static uk.gov.moj.cpp.results.it.utils.EventGridStub.stubEventGridEndpoint;
import static uk.gov.moj.cpp.results.it.utils.ReferenceDataServiceStub.stubBailStatuses;
import static uk.gov.moj.cpp.results.it.utils.ReferenceDataServiceStub.stubCountryNationalities;
import static uk.gov.moj.cpp.results.it.utils.ReferenceDataServiceStub.stubGetOrgainsationUnit;
import static uk.gov.moj.cpp.results.it.utils.ReferenceDataServiceStub.stubJudicialResults;
import static uk.gov.moj.cpp.results.it.utils.ReferenceDataServiceStub.stubModeOfTrialReasons;
import static uk.gov.moj.cpp.results.it.utils.ReferenceDataServiceStub.stubSpiOutFlag;
import static uk.gov.moj.cpp.results.it.utils.WireMockStubUtils.setupUserAsPrisonAdminGroup;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.test.utils.core.messaging.MessageConsumerClient;
import uk.gov.moj.cpp.domains.results.shareresults.PublicHearingResulted;
import uk.gov.moj.cpp.results.it.stub.NotificationServiceStub;

import java.io.InputStream;
import java.io.StringReader;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import javax.jms.JMSException;
import javax.json.JsonObject;
import javax.json.JsonReader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.IOUtils;
import org.hamcrest.core.IsNull;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.AfterAll;

@SuppressWarnings({"unchecked", "serial", "squid:S2925", "squid:S1607", "java:S2699"})
public class HearingResultSharedUseCaseForProsecutorCommunicationIT {

    private static final String EMAIL = "email@email.com";
    private static final String GOV_EMAIL_TEMPLATE_ID_FOR_POLICE_NOTIFICATION_AMEND_RESHARE_NO_APPLICATION = "c8b5a9dd-df0c-4f0d-83b1-b1c4c58dec13";
    private static final String GOV_EMAIL_TEMPLATE_ID_FOR_POLICE_NOTIFICATION_AMEND_RESHARE_WITH_APPLICATION = "f6c999fd-0495-4502-90d6-f6dc4676da6f";
    public static final String TEMPLATE_ID = "templateId";
    private static final String SEND_TO_ADDRESS = "sendToAddress";
    private static final String SUBJECT = "Subject";
    private static final String IS_AMEND_RESHARE_YES = "yes";
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
    private final JsonObjectToObjectConverter jsonToObjectConverter = new JsonObjectToObjectConverter(objectMapper);
    private static final String RESULTS_EVENT_POLICE_NOTIFICATION_REQUESTED_V2 = "results.event.police-notification-requested-v2";

    @BeforeClass
    public static void setUpClass() {
        setupUserAsPrisonAdminGroup(getUserId());
        stubEventGridEndpoint();
        stubCountryNationalities();
        stubGetOrgainsationUnit();
        stubJudicialResults();
        stubBailStatuses();
        stubModeOfTrialReasons();
        NotificationServiceStub.setUp();
    }

    @AfterAll
    public static void teardown() throws JMSException {
        closeMessageConsumers();
    }

    @Before
    public void setUp() {
        stubSpiOutFlag(true, true);
        createMessageConsumers();
        setField(this.jsonToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void should_SendSpiOutOnInitialShare_EmailOnReshare_OneCase_OneDefendant_OneOffence_NoApplication_MagsCourtHearingResulted() throws JMSException {
        MessageConsumerClient publicPoliceGeneratedEventConsumer = createPublicConsumerForPublicEvent("public.results.police-result-generated");
        stubSpiOutFlag(true, true, EMAIL);
        final UUID caseId = randomUUID();
        final String caseUrn = randomAlphanumeric(11).toUpperCase();
        final UUID hearingId = randomUUID();
        final JsonObject payload = getPayload("json/public-hearing-results-shared/firstshare-onecase-onedefendant-oneoffence.json", caseId, caseUrn, hearingId, null);
        final PublicHearingResulted publicHearingResulted = jsonToObjectConverter.convert(payload, PublicHearingResulted.class);

        stubGetProgressionProsecutionCasesFromPayload("stub-data/hearing-resulted/prosecutioncase-onedefendant-oneoffence.json", caseId, caseUrn, hearingId, null);
        stubGetProgressionCaseExistsByUrn(caseUrn, caseId);
        hearingResultsHaveBeenSharedV2(publicHearingResulted);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());

        LocalDate startDate = publicHearingResulted.getHearing().getHearingDays().get(0).getSittingDay().toLocalDate();
        startDate = of(startDate.getYear(), startDate.getMonth(), startDate.getDayOfMonth() - 1);

        getSummariesByDate(startDate);
        verifyPrivateEventsWithPoliceResultGenerated();

        String response = getPublicEventByEventName(publicPoliceGeneratedEventConsumer);
        //below line matching same number of judicialResult present in the public hearing shared from hearing and same number of judicial results going to police generated event.
        // since it was only one case and one defendent
        assertThat(countMatches(payload.toString(), "\"judicialResultId\""), is(countMatches(response, "\"judicialResultId\"")));
        JSONObject jsonObject = new JSONObject(response);
        assertThat(jsonObject.getString("caseId"), is(caseId.toString()));
        assertThat(jsonObject.getString("urn"), is(caseUrn));
        assertThat(jsonObject.getJSONObject("defendant").getString("defendantId"), is("28d9c834-29ed-4c08-9f9e-1cc4886dee37"));

        final JsonObject resharePayload = getPayload("json/public-hearing-results-shared/reshare-onecase-onedefendant-oneoffence.json", caseId, caseUrn, hearingId, null);
        final PublicHearingResulted resharedPublicHearingResulted = jsonToObjectConverter.convert(resharePayload, PublicHearingResulted.class);
        hearingResultsHaveBeenSharedV2(resharedPublicHearingResulted);

        verifyPrivateEventsPoliceNotificationRequestedGeneratedV2(ImmutableMap.of("policeEmailAddress", EMAIL));

        //Verifying below all the details should be present in the email sent to prosecutors.
        List<String> expectedDetailsPresentInEmailFormat = asList(TEMPLATE_ID, SEND_TO_ADDRESS, SUBJECT, caseUrn, EMAIL, GOV_EMAIL_TEMPLATE_ID_FOR_POLICE_NOTIFICATION_AMEND_RESHARE_NO_APPLICATION, IS_AMEND_RESHARE_YES);

        final String expectedDefendantName = "JohnYYYB8 Bot6651";
        final String firstOffenceWithNumber = "Offence number: 1";
        final String firstOffenceLabel = "Theft from a shop";
        final String addedResultForFirstOffence = "Added: Costs";
        final String deletedResultForFirstOffence = "Deleted: Surcharge";
        List<String> expectedDetailsPresentInDefendantHtmlSection = asList(expectedDefendantName, firstOffenceWithNumber, firstOffenceLabel, addedResultForFirstOffence, deletedResultForFirstOffence);

        final String application = "Applications";
        List<String> detailsShouldNotBePresentInEmail = asList(application);

        final List<String> allExpectedDetailsPresentInEmail = Stream.of(expectedDetailsPresentInEmailFormat, expectedDetailsPresentInDefendantHtmlSection)
                .flatMap(Collection::stream)
                .collect(toList());
        verifyEmailNotificationIsRaised(allExpectedDetailsPresentInEmail, detailsShouldNotBePresentInEmail);

    }


    @Test
    public void should_SendSpiOutOnInitialShare_EmailOnReshare_TwoCase_FourDefendant_EightOffence_TwoHearing_NoApplication_MagsCourtHearingResulted() throws JMSException {
        MessageConsumerClient publicPoliceGeneratedEventConsumer = createPublicConsumerForPublicEvent("public.results.police-result-generated");
        stubSpiOutFlag(true, true, EMAIL);
        final UUID caseId1 = randomUUID();
        final UUID caseId2 = randomUUID();
        final String caseUrn1 = randomAlphanumeric(11).toUpperCase();
        final String caseUrn2 = randomAlphanumeric(11).toUpperCase();
        final UUID hearingId1 = randomUUID();
        final UUID hearingId2 = randomUUID();
        final Map<String, String> caseIdMap = new HashMap<>();
        caseIdMap.put("MAIN_CASE_ID", caseId1.toString());
        caseIdMap.put("CASE_ID_2", caseId2.toString());
        final Map<String, String> caseUrnMap = new HashMap<>();
        caseUrnMap.put("MAIN_CASE_URN", caseUrn1);
        caseUrnMap.put("CASE_URN_2", caseUrn2);
        final Map<String, String> hearingIdMap = new HashMap<>();
        hearingIdMap.put("MAIN_HEARING_ID", hearingId1.toString());
        hearingIdMap.put("HEARING_ID_2", hearingId2.toString());

        final JsonObject payload = getPayloadForMultipleEntities("json/public-hearing-results-shared/firstshare-twocase-fourdefendant-eightoffence-twohearing.json", caseIdMap, caseUrnMap, hearingIdMap, null);
        final PublicHearingResulted publicHearingResulted = jsonToObjectConverter.convert(payload, PublicHearingResulted.class);

        //case 1 progression query response
        final Map<String, String> hearingIdMapForCase = new HashMap<>();
        hearingIdMap.put("HEARING_ID_1", hearingId1.toString());
        hearingIdMap.put("HEARING_ID_2", hearingId2.toString());
        stubGetProgressionProsecutionCase_WhichHasLikedCases_AndHearings("stub-data/hearing-resulted/prosecutioncase-firstcase-fourdefendant-eightoffence-twohearing.json", caseIdMap, caseUrnMap, hearingIdMapForCase, null);

        // case 2 progression query response
        final UUID caseTwoHearingId1 = randomUUID();
        final UUID caseTwoHearingId2 = randomUUID();
        final Map<String, String> hearingIdMapForCaseTwo = new HashMap<>();
        hearingIdMapForCaseTwo.put("HEARING_ID_1", caseTwoHearingId1.toString());
        hearingIdMapForCaseTwo.put("HEARING_ID_2", caseTwoHearingId2.toString());
        final Map<String, String> caseTwoIdMap = new HashMap<>();
        caseTwoIdMap.put("MAIN_CASE_ID", caseId2.toString());
        caseTwoIdMap.put("CASE_ID_2", caseId1.toString());
        final Map<String, String> caseTwoUrnMap = new HashMap<>();
        caseTwoUrnMap.put("MAIN_CASE_URN", caseUrn2);
        caseTwoUrnMap.put("CASE_URN_2", caseUrn1);
        stubGetProgressionProsecutionCase_WhichHasLikedCases_AndHearings("stub-data/hearing-resulted/prosecutioncase-secondcase-fourdefendant-eightoffence-twohearing.json", caseTwoIdMap, caseTwoUrnMap, hearingIdMapForCaseTwo, null);

        stubGetProgressionCaseExistsByUrn(caseUrn1, caseId1);
        stubGetProgressionCaseExistsByUrn(caseUrn2, caseId2);

        hearingResultsHaveBeenSharedV2(publicHearingResulted);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());

        LocalDate startDate = publicHearingResulted.getHearing().getHearingDays().get(0).getSittingDay().toLocalDate();
        startDate = of(startDate.getYear(), startDate.getMonth(), startDate.getDayOfMonth() - 1);

        getSummariesByDate(startDate);
        verifyPrivateEventsWithPoliceResultGenerated();

        final List<String> response = retrieveAllMessages(publicPoliceGeneratedEventConsumer);
        //four police generated events since 4 defendants are there in 2 cases.
        assertThat(response.size(), is(4));
        final List<String> defendantIds = payload.getJsonObject("hearing").getJsonArray("prosecutionCases").getValuesAs(JsonObject.class).stream()
                .map(prosecutionCase -> prosecutionCase.getJsonArray("defendants").getValuesAs(JsonObject.class).stream()
                        .map(defendant -> defendant.getString("id"))
                        .collect(toList())
                ).flatMap(Collection::stream)
                .collect(toList());
        response.stream()
                .map(stringEvent -> new JSONObject(stringEvent))
                .map(jsonEvent -> jsonEvent.getJSONObject("defendant").getString("defendantId"))
                        .forEach(defendantId -> assertThat(defendantId, defendantIds.contains(defendantId)));

        final JsonObject resharePayload = getPayloadForMultipleEntities("json/public-hearing-results-shared/reshare-twocase-fourdefendant-eightoffence-twohearing.json", caseIdMap, caseUrnMap, hearingIdMap, null);
        final PublicHearingResulted resharedPublicHearingResulted = jsonToObjectConverter.convert(resharePayload, PublicHearingResulted.class);
        hearingResultsHaveBeenSharedV2(resharedPublicHearingResulted);

        verifyPrivateEventsPoliceNotificationRequestedGeneratedV2(ImmutableMap.of("policeEmailAddress", EMAIL));

        //Verifying below all the details should be present in the email sent to prosecutors.
        List<String> expectedDetailsPresentInEmailFormat = asList(TEMPLATE_ID, SEND_TO_ADDRESS, SUBJECT, caseUrn1, EMAIL, GOV_EMAIL_TEMPLATE_ID_FOR_POLICE_NOTIFICATION_AMEND_RESHARE_NO_APPLICATION, IS_AMEND_RESHARE_YES);

        final String expectedDefendantOneName = "Tianna Botsford";
        final String defendantOneFirstOffenceWithNumber = "Offence number: 1";
        final String defendantOneFirstOffenceLabel = "Theft in dwelling other than an automatic machine or meter";
        final String defendantOneAddedResultForFirstOffence = "Updated: Adjournment";
        List<String> expectedDetailsPresentInForDefendantOneInHtmlSection = asList(expectedDefendantOneName, defendantOneFirstOffenceWithNumber, defendantOneFirstOffenceLabel, defendantOneAddedResultForFirstOffence);

        final String expectedDefendantTwoName = "Norberto Haley";
        final String defendantTwoFirstOffenceWithNumber = "Offence number: 1";
        final String defendantTwoFirstOffenceLabel = "Drive whilst unfit through drink";
        final String defendantTwoAddedResultForFirstOffence = "Updated: Adjournment";
        List<String> expectedDetailsPresentInForDefendantTwoInHtmlSection = asList(expectedDefendantTwoName, defendantTwoFirstOffenceWithNumber, defendantTwoFirstOffenceLabel, defendantTwoAddedResultForFirstOffence);

        final String application = "Applications";
        List<String> detailsShouldNotBePresentInEmail = asList(application);

        final List<String> allExpectedDetailsPresentInEmail = Stream.of(expectedDetailsPresentInEmailFormat, expectedDetailsPresentInForDefendantOneInHtmlSection,expectedDetailsPresentInForDefendantTwoInHtmlSection).
                flatMap(Collection::stream)
                .collect(toList());
        verifyEmailNotificationIsRaised(allExpectedDetailsPresentInEmail, detailsShouldNotBePresentInEmail);

    }

    public static JsonObject getPayload(final String path, final UUID caseId, final String urn, final UUID hearingId, final UUID applicationId) {
        String request = null;
        try {
            final InputStream inputStream = HearingResultSharedUseCaseForProsecutorCommunicationIT.class.getClassLoader().getResourceAsStream(path);
            assertThat(inputStream, IsNull.notNullValue());
            request = IOUtils.toString(inputStream, defaultCharset())
                    .replaceAll("HEARING_ID", hearingId.toString())
                    .replaceAll("CASE_ID", caseId.toString())
                    .replaceAll("CASE_URN", urn);
            if(nonNull(applicationId)){
                request.replaceAll("APPLICATION_ID", applicationId.toString());
            }
        } catch (final Exception e) {
            fail("Error consuming file from location " + path);
        }
        final JsonReader reader = createReader(new StringReader(request));
        return reader.readObject();
    }

    public static JsonObject getPayloadForMultipleEntities(final String path, final Map<String,String> caseIdMap, final Map<String,String> urnMap, final Map<String,String> hearingIdMap, final Map<String,String> applicationIdMap) {
        String request = null;
        try {
            final InputStream inputStream = HearingResultSharedUseCaseForProsecutorCommunicationIT.class.getClassLoader().getResourceAsStream(path);
            assertThat(inputStream, IsNull.notNullValue());
            request = IOUtils.toString(inputStream, defaultCharset());
            request = updateKeyValueInString(caseIdMap,request);
            request = updateKeyValueInString(urnMap,request);
            request = updateKeyValueInString(applicationIdMap,request);
            request = updateKeyValueInString(hearingIdMap,request);

        } catch (final Exception e) {
            fail("Error consuming file from location " + path);
        }
        final JsonReader reader = createReader(new StringReader(request));
        return reader.readObject();
    }

    public static String updateKeyValueInString(final Map<String, String> keyValueMap, String payload) {
        if (nonNull(keyValueMap)) {
            for (Map.Entry entry : keyValueMap.entrySet()) {
                payload = payload.replaceAll(entry.getKey().toString(), entry.getValue().toString());
            }
        }
        return payload;
    }


    private static String getPublicEventByEventName(final MessageConsumerClient publicMessageConsumer) {
        final Optional<String> response = publicMessageConsumer.retrieveMessage();
        assertThat(response, not(empty()));
        assertThat(response.get(), notNullValue());
        return response.get();
    }

    private MessageConsumerClient createPublicConsumerForPublicEvent(final String publicEventName) {
        MessageConsumerClient publicMessageConsumer = new MessageConsumerClient();
        publicMessageConsumer.startConsumer(publicEventName, "jms.topic.public.event");
        return publicMessageConsumer;
    }

    public static List<String> retrieveAllMessages(final MessageConsumerClient publicMessageConsumer) {
        List<String> publicEventMessages = new ArrayList<>();
        Optional<String> response;
        do {
            response = publicMessageConsumer.retrieveMessage();
            response.ifPresent(res -> publicEventMessages.add(res));
        } while (response.isPresent());
        return publicEventMessages;
    }

}