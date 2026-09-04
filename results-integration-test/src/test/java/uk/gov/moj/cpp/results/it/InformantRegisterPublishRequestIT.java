package uk.gov.moj.cpp.results.it;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.results.it.utils.EventGridStub.stubEventGridEndpoint;
import static uk.gov.moj.cpp.results.it.utils.FeatureStubUtil.setFeatureToggle;
import static uk.gov.moj.cpp.results.it.utils.FileUtil.convertStringToJson;
import static uk.gov.moj.cpp.results.it.utils.FileUtil.getPayload;
import static uk.gov.moj.cpp.results.it.utils.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.results.it.utils.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.results.it.utils.QueueUtil.retrieveMessage;
import static uk.gov.moj.cpp.results.it.utils.QueueUtil.sendMessage;
import static uk.gov.moj.cpp.results.it.utils.ReferenceDataServiceStub.stubCountryNationalities;
import static uk.gov.moj.cpp.results.it.utils.ReferenceDataServiceStub.stubGetOrganisationUnit;
import static uk.gov.moj.cpp.results.it.utils.WireMockStubUtils.setupUsersGroupQueryStub;

import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.json.JsonObject;

import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Covers the durable half of the informant-register publish chain: resulting a regular hearing with
 * the InformantRegisterService feature on records a
 * {@code results.events.informant-register-publish-requested} event, and with the feature off
 * records nothing.
 *
 * <p>The Service Bus publish itself is deliberately not exercised here. This environment has no
 * Service Bus emulator and no {@code informantRegisterQueueNamespace} configured, so the publisher
 * is inert by design; the message shape and its broker properties are covered by
 * {@code InformantRegisterQueuePublisherTest}. What this test does cover is the part that used to be
 * missing entirely - that the publish intent is written somewhere durable before anyone tries to
 * publish it, so a broker failure is recoverable.
 *
 * <p>The public event is published with metadata built here rather than through
 * {@code ResultsStepDefinitions.hearingResultsHaveBeenSharedV2}, because that helper's envelope
 * carries no userId and the publish request is only made for a share that names its sharing user.
 */
public class InformantRegisterPublishRequestIT {

    private static final String INFORMANT_REGISTER_SERVICE_FEATURE = "InformantRegisterService";
    private static final String PUBLIC_HEARING_RESULTED = "public.events.hearing.hearing-resulted";
    private static final String PUBLISH_REQUESTED = "results.events.informant-register-publish-requested";

    private static final String TEMPLATE_PAYLOAD = "json/public.events.hearing.hearing-resulted.json";
    private static final String EXPECTED_HEARING_DAY = "2019-06-03";
    private static final String EXPECTED_SHARED_TIME = "2019-06-03T15:14:14.438Z";

    /**
     * Long enough for the append-publish-dispatch hop when the event is expected. The absence case
     * uses a shorter wait: it can only ever time out, and a failing negative assertion should not
     * cost the full window.
     */
    private static final long PUBLISH_REQUESTED_TIMEOUT = 20000;
    private static final long ABSENCE_TIMEOUT = 10000;

    private MessageProducer publicProducer;
    private MessageConsumer publishRequestedConsumer;

    @BeforeAll
    public static void setUpClass() {
        setupUsersGroupQueryStub();
        stubEventGridEndpoint();
        stubCountryNationalities();
        stubGetOrganisationUnit();
    }

    @BeforeEach
    public void setUp() {
        publicProducer = publicEvents.createProducer();
        // Subscribe before publishing: the topic is not durable for this consumer, so a consumer
        // created afterwards would miss the event and the test would fail for the wrong reason.
        publishRequestedConsumer = privateEvents.createConsumer(PUBLISH_REQUESTED);
    }

    @AfterEach
    public void tearDown() throws JMSException {
        setFeatureToggle(INFORMANT_REGISTER_SERVICE_FEATURE, false);
        try {
            publishRequestedConsumer.close();
        } finally {
            // Closed in a finally so a consumer that fails to close cannot leak the producer too and
            // starve later tests in the shared connection of sessions.
            publicProducer.close();
        }
    }

    @Test
    public void resultingARegularHearingWithTheFeatureOnShouldRecordAPublishRequest() {
        setFeatureToggle(INFORMANT_REGISTER_SERVICE_FEATURE, true);

        final UUID hearingId = randomUUID();
        final UUID userId = randomUUID();

        shareHearingResults(hearingId, userId);

        final JsonPath publishRequested = retrieveMessage(publishRequestedConsumer, PUBLISH_REQUESTED_TIMEOUT);

        assertThat(publishRequested, is(notNullValue()));
        assertThat(publishRequested.getString("hearingId"), is(hearingId.toString()));
        assertThat(publishRequested.getString("hearingDay"), is(EXPECTED_HEARING_DAY));
        assertThat(publishRequested.getString("sharedTime"), is(EXPECTED_SHARED_TIME));
        assertThat(publishRequested.getString("userId"), is(userId.toString()));

        // The Event Grid publish happens inside the same delivery, before it commits, so by the time
        // the publish-requested event is observable the POST has already been made. Exactly one:
        // the share was processed once, and the processor publishes to Event Grid only after both
        // command sends have succeeded, so a clean run must never produce a second Hearing_Resulted.
        // Matched on body rather than path so the assertion does not depend on the SDK's URL layout.
        verify(1, postRequestedFor(urlMatching("/.*"))
                .withRequestBody(containing(hearingId.toString()))
                .withRequestBody(containing("Hearing_Resulted")));
    }

    /**
     * The feature is off by default and off in every environment still served by the legacy
     * informant-register function app, where a recorded publish request would mean the register is
     * distributed twice.
     */
    @Test
    public void resultingARegularHearingWithTheFeatureOffShouldRecordNoPublishRequest() {
        setFeatureToggle(INFORMANT_REGISTER_SERVICE_FEATURE, false);

        shareHearingResults(randomUUID(), randomUUID());

        assertThat(retrieveMessage(publishRequestedConsumer, ABSENCE_TIMEOUT), is(nullValue()));
    }

    private void shareHearingResults(final UUID hearingId, final UUID userId) {
        final JsonObject payload = convertStringToJson(
                getPayload(TEMPLATE_PAYLOAD).replaceAll("HEARING_ID", hearingId.toString()));

        sendMessage(publicProducer, PUBLIC_HEARING_RESULTED, payload,
                metadataWithRandomUUID(PUBLIC_HEARING_RESULTED).withUserId(userId.toString()).build());
    }
}
