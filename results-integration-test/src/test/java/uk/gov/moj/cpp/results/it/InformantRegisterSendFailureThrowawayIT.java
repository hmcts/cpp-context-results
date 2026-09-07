package uk.gov.moj.cpp.results.it;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * THROWAWAY - never merge. Relies on a temporary hook in {@code HearingResultedEventProcessor}
 * that throws instead of sending the informant register publish request when the hearing id ends
 * in "dead". Proves, in the real container, that a failed send rolls the delivery back before
 * Event Grid is called.
 *
 * <p>What the test itself asserts: for the sentinel hearing no publish-requested event is ever
 * recorded (every delivery rolled back, so the resulting command never reached the handler), and a
 * normal hearing on the same deployment still produces its event.
 *
 * <p>What has to be read from the WildFly log, because the Azure SDK refuses a key credential over
 * plain HTTP and so the Event Grid POST never reaches WireMock in this environment:
 * <pre>
 *   docker logs containers-cpp-wildfly-1 2>&amp;1 | grep -o "Adding Hearing Resulted for hearing [0-9a-f-]*" | sort | uniq -c
 *   docker logs containers-cpp-wildfly-1 2>&amp;1 | grep -c "Requesting informant register publish for hearing 00000000-0000-0000-0000-00000000dead"
 * </pre>
 * Expected: the "Adding Hearing Resulted" line (written immediately before the Event Grid call)
 * appears once for the control hearing and never for the sentinel, while the sentinel shows ten
 * "Requesting informant register publish" attempts (Artemis default max-delivery-attempts) and one
 * message on {@code jms.queue.DLQ}.
 */
public class InformantRegisterSendFailureThrowawayIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(InformantRegisterSendFailureThrowawayIT.class);

    private static final String PUBLIC_HEARING_RESULTED = "public.events.hearing.hearing-resulted";
    private static final String PUBLISH_REQUESTED = "results.events.informant-register-publish-requested";
    private static final String INFORMANT_REGISTER_SERVICE_FEATURE = "InformantRegisterService";
    private static final String TEMPLATE_PAYLOAD = "json/public.events.hearing.hearing-resulted.json";

    private static final UUID SENTINEL_HEARING_ID = UUID.fromString("00000000-0000-0000-0000-00000000dead");

    /** Hook in sendEventToGrid throws for this id, outside the catch, so step 3 fails and the delivery rolls back. */
    private static final UUID EVENT_GRID_SENTINEL_HEARING_ID = UUID.fromString("00000000-0000-0000-0000-0000000e6bad");

    private static final long PUBLISH_REQUESTED_TIMEOUT = 20000;
    private static final long ABSENCE_TIMEOUT = 15000;

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
        publishRequestedConsumer = privateEvents.createConsumer(PUBLISH_REQUESTED);
    }

    @AfterEach
    public void tearDown() throws JMSException {
        setFeatureToggle(INFORMANT_REGISTER_SERVICE_FEATURE, false);
        try {
            publishRequestedConsumer.close();
        } finally {
            publicProducer.close();
        }
    }

    @Test
    public void aFailedPublishRequestSendShouldRollBackBeforeEventGridIsCalled() {
        setFeatureToggle(INFORMANT_REGISTER_SERVICE_FEATURE, true);

        final UUID controlHearingId = randomUUID();

        LOGGER.info("THROWAWAY IT - hearing that must FAIL (send throws, delivery rolls back, Event Grid never called): {}", SENTINEL_HEARING_ID);
        LOGGER.info("THROWAWAY IT - hearing that must SUCCEED (published to Event Grid exactly once): {}", controlHearingId);

        // Failing share: the processor throws where it would send the publish request.
        shareHearingResults(SENTINEL_HEARING_ID, randomUUID());

        // Every delivery attempt rolled back, so the resulting command never reached the handler and
        // no publish-requested event was ever recorded.
        assertThat(retrieveMessage(publishRequestedConsumer, ABSENCE_TIMEOUT), is(nullValue()));
        LOGGER.info("THROWAWAY IT - no publish-requested event recorded for failing hearing {}", SENTINEL_HEARING_ID);

        // Positive control: a normal share on the same deployment produces the event.
        shareHearingResults(controlHearingId, randomUUID());

        final JsonPath publishRequested = retrieveMessage(publishRequestedConsumer, PUBLISH_REQUESTED_TIMEOUT);
        assertThat(publishRequested, is(notNullValue()));
        assertThat(publishRequested.getString("hearingId"), is(controlHearingId.toString()));
        LOGGER.info("THROWAWAY IT - publish-requested event recorded for succeeding hearing {}", controlHearingId);

        // Event Grid evidence is in the WildFly log - see the class Javadoc. Ready-to-run checks:
        LOGGER.info("THROWAWAY IT - Event Grid attempts for the FAILING hearing (expect 0):   docker logs containers-cpp-wildfly-1 2>&1 | grep -c \"Adding Hearing Resulted for hearing {}\"", SENTINEL_HEARING_ID);
        LOGGER.info("THROWAWAY IT - delivery attempts for the FAILING hearing (expect 10/run): docker logs containers-cpp-wildfly-1 2>&1 | grep -c \"Requesting informant register publish for hearing {}\"", SENTINEL_HEARING_ID);
        LOGGER.info("THROWAWAY IT - Event Grid attempts for the SUCCEEDING hearing (expect 1): docker logs containers-cpp-wildfly-1 2>&1 | grep -c \"Adding Hearing Resulted for hearing {}\"", controlHearingId);
    }

    /**
     * Step 3 fails: the Event Grid publish throws and the exception escapes. The delivery rolls back,
     * so step 2 - the resulting command and the informant register publish request, both sent in
     * this transaction - is undone: no publish-requested event is ever recorded, on any attempt.
     *
     * <p>This also shows the cost of letting an Event Grid failure escape in production: every
     * redelivery reaches the Event Grid call again (the log shows one attempt per delivery), which is
     * why the real code swallows that failure instead.
     */
    @Test
    public void aFailedEventGridPublishShouldRollBackStepTwo() {
        setFeatureToggle(INFORMANT_REGISTER_SERVICE_FEATURE, true);

        LOGGER.info("THROWAWAY IT - hearing whose EVENT GRID publish must FAIL (exception escapes, delivery rolls back, step 2 undone): {}", EVENT_GRID_SENTINEL_HEARING_ID);

        shareHearingResults(EVENT_GRID_SENTINEL_HEARING_ID, randomUUID());

        // Step 2 rolled back on every attempt: the publish request was sent and undone each time, so
        // the command handler never saw it and no publish-requested event exists.
        assertThat(retrieveMessage(publishRequestedConsumer, ABSENCE_TIMEOUT), is(nullValue()));
        LOGGER.info("THROWAWAY IT - no publish-requested event recorded for hearing {} (step 2 rolled back)", EVENT_GRID_SENTINEL_HEARING_ID);

        LOGGER.info("THROWAWAY IT - delivery attempts, each one sending then rolling back step 2 (expect 10/run): docker logs containers-cpp-wildfly-1 2>&1 | grep -c \"Requesting informant register publish for hearing {}\"", EVENT_GRID_SENTINEL_HEARING_ID);
        LOGGER.info("THROWAWAY IT - Event Grid attempts, one per redelivery (expect 10/run):                   docker logs containers-cpp-wildfly-1 2>&1 | grep -c \"Adding Hearing Resulted for hearing {}\"", EVENT_GRID_SENTINEL_HEARING_ID);
        LOGGER.info("THROWAWAY IT - the escaped exception (expect 10/run):                                     docker logs containers-cpp-wildfly-1 2>&1 | grep -c \"THROWAWAY simulated Event Grid failure for hearing {}\"", EVENT_GRID_SENTINEL_HEARING_ID);
    }

    private void shareHearingResults(final UUID hearingId, final UUID userId) {
        final JsonObject payload = convertStringToJson(
                getPayload(TEMPLATE_PAYLOAD).replaceAll("HEARING_ID", hearingId.toString()));

        sendMessage(publicProducer, PUBLIC_HEARING_RESULTED, payload,
                metadataWithRandomUUID(PUBLIC_HEARING_RESULTED).withUserId(userId.toString()).build());
    }
}
