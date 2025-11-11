package uk.gov.moj.cpp.results.it;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.closeMessageConsumers;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.createMessageConsumers;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.getHearingDetails;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.hearingResultsHaveBeenSharedV2;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.whenPrisonAdminTriesToViewResultsForThePerson;
import static uk.gov.moj.cpp.results.it.steps.data.factory.HearingResultDataFactory.getUserId;
import static uk.gov.moj.cpp.results.it.utils.EventGridStub.stubEventGridEndpoint;
import static uk.gov.moj.cpp.results.it.utils.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.results.it.utils.QueueUtil.retrieveMessage;
import static uk.gov.moj.cpp.results.it.utils.ReferenceDataServiceStub.stubGetOrgainsationUnit;
import static uk.gov.moj.cpp.results.it.utils.ReferenceDataServiceStub.stubJudicialResults;
import static uk.gov.moj.cpp.results.it.utils.ReferenceDataServiceStub.stubSpiOutFlag;
import static uk.gov.moj.cpp.results.it.utils.WireMockStubUtils.setupUserAsPrisonAdminGroup;
import static uk.gov.moj.cpp.results.test.TestTemplates.basicShareResultsV2Template;
import static uk.gov.moj.cpp.results.test.matchers.BeanMatcher.isBean;

import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingResultsAdded;
import uk.gov.justice.services.test.utils.core.messaging.MessageProducerClient;
import uk.gov.moj.cpp.domains.results.shareresults.PublicHearingResulted;

import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.json.Json;
import javax.json.JsonObject;

import io.restassured.path.json.JsonPath;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class EjectCaseIT {

    private static final String PUBLIC_EVENT_TOPIC = "public.event";
    private static final String PUBLIC_PROGRESSION_EVENTS_CASE_OR_APPLICATION_EJECTED = "public.progression.events.case-or-application-ejected";
    private static final String PROSECUTION_CASE_ID = "prosecutionCaseId";
    private static final String APPLICATION_ID = "applicationId";
    private static final String HEARING_IDS = "hearingIds";
    private static final String REMOVAL_REASON = "removalReason";
    private MessageConsumer hearingCaseEjectedConsumer;
    private MessageConsumer hearingApplicationEjectedConsumer;

    @BeforeEach
    public void setUp() {
        setupUserAsPrisonAdminGroup(getUserId());
        stubEventGridEndpoint();
        stubJudicialResults();
        stubSpiOutFlag(true, true);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());
        stubGetOrgainsationUnit();
        hearingCaseEjectedConsumer = privateEvents.createConsumer("results.hearing-case-ejected");
        hearingApplicationEjectedConsumer = privateEvents.createConsumer("results.hearing-application-ejected");
        createMessageConsumers();

    }

    @Test
    public void shouldEjectCaseWithCaseIdInPayload() {
        final PublicHearingResulted resultsMessage = basicShareResultsV2Template(MAGISTRATES);

        final Hearing hearingIn = resultsMessage.getHearing();

        final UUID defendantId0 =
                resultsMessage.getHearing().getProsecutionCases().get(0).getDefendants().get(0).getId();


        //share results
        hearingResultsHaveBeenSharedV2(resultsMessage);

        //matcher to check details results
        final Matcher<HearingResultsAdded> hearingResultsAddedMatcher = isBean(HearingResultsAdded.class)
                .with(HearingResultsAdded::getHearing, isBean(Hearing.class)
                        .withValue(Hearing::getId, hearingIn.getId())
                        .withValue(Hearing::getJurisdictionType, hearingIn.getJurisdictionType())
                        .withValue(Hearing::getType, hearingIn.getType())
                );

        getHearingDetails(resultsMessage.getHearing().getId(), defendantId0, hearingResultsAddedMatcher);

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
    public void shouldEjectApplicationWithApplicationIdInPayload() {

        final PublicHearingResulted resultsMessage = basicShareResultsV2Template(MAGISTRATES);

        final Hearing hearingIn = resultsMessage.getHearing();

        final UUID defendantId0 =
                resultsMessage.getHearing().getProsecutionCases().get(0).getDefendants().get(0).getId();


        //share results
        hearingResultsHaveBeenSharedV2(resultsMessage);

        //matcher to check details results
        final Matcher<HearingResultsAdded> hearingResultsAddedMatcher = isBean(HearingResultsAdded.class)
                .with(HearingResultsAdded::getHearing, isBean(Hearing.class)
                        .withValue(Hearing::getId, hearingIn.getId())
                        .withValue(Hearing::getJurisdictionType, hearingIn.getJurisdictionType())
                        .withValue(Hearing::getType, hearingIn.getType())
                );

        getHearingDetails(resultsMessage.getHearing().getId(), defendantId0, hearingResultsAddedMatcher);

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

    @AfterEach
    public void tearDown() throws JMSException {
        hearingCaseEjectedConsumer.close();
        hearingApplicationEjectedConsumer.close();
        closeMessageConsumers();
    }

    private void raisePublicEventForEjectedCaseOrApplication(final JsonObject payload) {
        try (final MessageProducerClient messageProducer = new MessageProducerClient()) {
            messageProducer.startProducer(PUBLIC_EVENT_TOPIC);
            messageProducer.sendMessage(PUBLIC_PROGRESSION_EVENTS_CASE_OR_APPLICATION_EJECTED, payload);
        }
    }

}
