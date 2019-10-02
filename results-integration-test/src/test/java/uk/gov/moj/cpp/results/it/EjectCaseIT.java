package uk.gov.moj.cpp.results.it;

import com.jayway.restassured.path.json.JsonPath;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingResultsAdded;
import uk.gov.justice.services.test.utils.core.messaging.MessageProducerClient;
import uk.gov.moj.cpp.domains.results.shareresults.PublicHearingResulted;
import uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions;
import uk.gov.moj.cpp.results.it.utils.QueueUtil;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.json.Json;
import javax.json.JsonObject;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.hearingResultsHaveBeenShared;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.whenPrisonAdminTriesToViewResultsForThePerson;
import static uk.gov.moj.cpp.results.it.steps.data.factory.HearingResultDataFactory.getUserId;
import static uk.gov.moj.cpp.results.it.utils.AuthorisationServiceStub.stubEnableAllCapabilities;
import static uk.gov.moj.cpp.results.it.utils.QueueUtil.retrieveMessage;
import static uk.gov.moj.cpp.results.it.utils.WireMockStubUtils.setupUserAsPrisonAdminGroup;
import static uk.gov.moj.cpp.results.test.TestTemplates.basicShareResultsTemplate;
import static uk.gov.moj.cpp.results.test.matchers.BeanMatcher.isBean;

public class EjectCaseIT {

    private static final String PUBLIC_EVENT_TOPIC = "public.event";
    private static final String PUBLIC_PROGRESSION_EVENTS_CASE_OR_APPLICATION_EJECTED = "public.progression.events.case-or-application-ejected";
    private MessageConsumer hearingCaseEjectedConsumer;
    private MessageConsumer hearingApplicationEjectedConsumer;
    private static final String PROSECUTION_CASE_ID = "prosecutionCaseId";
    private static final String APPLICATION_ID = "applicationId";
    private static final String HEARING_IDS = "hearingIds";
    private static final String REMOVAL_REASON = "removalReason";

    @Before
    public void setUp() {
        setupUserAsPrisonAdminGroup(getUserId());
        stubEnableAllCapabilities();
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());
        hearingCaseEjectedConsumer = QueueUtil.privateEvents.createConsumer("results.hearing-case-ejected");
        hearingApplicationEjectedConsumer = QueueUtil.privateEvents.createConsumer("results.hearing-application-ejected");

    }

    @Test
    public void testEjectCaseWithCaseIdInPayload() {
        PublicHearingResulted resultsMessage = basicShareResultsTemplate();

        final Hearing hearingIn = resultsMessage.getHearing();

        final UUID defendantId0 =
                resultsMessage.getHearing().getProsecutionCases().get(0).getDefendants().get(0).getId();


        //share results
        hearingResultsHaveBeenShared(resultsMessage);

        //matcher to check details results
        Matcher<HearingResultsAdded> hearingResultsAddedMatcher = isBean(HearingResultsAdded.class)
                .with(HearingResultsAdded::getHearing, isBean(Hearing.class)
                        .withValue(Hearing::getId, hearingIn.getId())
                        .withValue(Hearing::getJurisdictionType, hearingIn.getJurisdictionType())
                        .withValue(Hearing::getType, hearingIn.getType())
                );

        ResultsStepDefinitions.getHearingDetails(resultsMessage.getHearing().getId(), defendantId0, hearingResultsAddedMatcher);

        final UUID hearingId = hearingIn.getId();
        final UUID caseId = hearingIn.getProsecutionCases().stream().findFirst().get().getId();

        final JsonObject payload = Json.createObjectBuilder()
                .add(HEARING_IDS,
                        Json.createArrayBuilder().add(hearingId.toString()).build())
                .add(PROSECUTION_CASE_ID, caseId.toString())
                .add(REMOVAL_REASON, "legal")
                .build();

        raisePublicEventForEjectedCaseOrApplication(payload);

        final JsonPath jsonResponse = retrieveMessage(hearingCaseEjectedConsumer);
        assertThat(jsonResponse.getString("caseId"), is(caseId.toString()));
        assertThat(jsonResponse.getString("hearingId"), is(hearingId.toString()));
    }

    @Test
    public void testEjectApplicationWithApplicationIdInPayload() {

        PublicHearingResulted resultsMessage = basicShareResultsTemplate();

        final Hearing hearingIn = resultsMessage.getHearing();

        final UUID defendantId0 =
                resultsMessage.getHearing().getProsecutionCases().get(0).getDefendants().get(0).getId();


        //share results
        hearingResultsHaveBeenShared(resultsMessage);

        //matcher to check details results
        Matcher<HearingResultsAdded> hearingResultsAddedMatcher = isBean(HearingResultsAdded.class)
                .with(HearingResultsAdded::getHearing, isBean(Hearing.class)
                        .withValue(Hearing::getId, hearingIn.getId())
                        .withValue(Hearing::getJurisdictionType, hearingIn.getJurisdictionType())
                        .withValue(Hearing::getType, hearingIn.getType())
                );

        ResultsStepDefinitions.getHearingDetails(resultsMessage.getHearing().getId(), defendantId0, hearingResultsAddedMatcher);

        final UUID hearingId = hearingIn.getId();
        final UUID applicationId = hearingIn.getCourtApplications().stream().findFirst().get().getId();

        final JsonObject payload = Json.createObjectBuilder()
                .add(HEARING_IDS,
                        Json.createArrayBuilder().add(hearingId.toString()).build())
                .add(APPLICATION_ID, applicationId.toString())
                .add(REMOVAL_REASON, "legal")
                .build();

        raisePublicEventForEjectedCaseOrApplication(payload);

        final JsonPath jsonResponse = retrieveMessage(hearingApplicationEjectedConsumer);
        assertThat(jsonResponse.getString("applicationId"), is(applicationId.toString()));
        assertThat(jsonResponse.getString("hearingId"), is(hearingId.toString()));

    }

    @After
    public void tearDown() throws JMSException {
        hearingCaseEjectedConsumer.close();
        hearingApplicationEjectedConsumer.close();
    }

    private void raisePublicEventForEjectedCaseOrApplication(final JsonObject payload) {
        try (final MessageProducerClient messageProducer = new MessageProducerClient()) {
            messageProducer.startProducer(PUBLIC_EVENT_TOPIC);
            messageProducer.sendMessage(PUBLIC_PROGRESSION_EVENTS_CASE_OR_APPLICATION_EJECTED, payload);
        }
    }

}
