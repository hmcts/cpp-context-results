package uk.gov.moj.cpp.results.it;

import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createReader;
import static uk.gov.moj.cpp.results.it.utils.FileUtil.getPayload;
import static uk.gov.moj.cpp.results.it.utils.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.results.it.utils.QueueUtil.sendMessage;
import static uk.gov.moj.cpp.results.it.utils.WireMockStubUtils.setupUsersGroupQueryStub;

import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.results.it.helper.InformantRegisterDocumentRequestHelper;

import java.io.StringReader;
import java.time.ZonedDateTime;
import java.util.Random;
import java.util.UUID;

import jakarta.jms.JMSException;
import jakarta.jms.MessageProducer;
import jakarta.json.JsonObject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Exercises the four informant-register domain-event flows at the event-listener boundary by publishing each event
 * directly onto the internal {@code results.event} topic and asserting the resulting view-store projection:
 *
 * <ul>
 *   <li>{@code results.event.informant-register-recorded}        (V1, core namespace)    -> RECORDED projection</li>
 *   <li>{@code results.event.informant-register-recorded-v2}     (V2, results namespace) -> RECORDED projection</li>
 *   <li>{@code results.event.informant-register-generated}       (V1) advances a RECORDED register past RECORDED</li>
 *   <li>{@code results.event.informant-register-generated-v2}    (V2) advances a RECORDED register past RECORDED</li>
 * </ul>
 *
 * <p>The current command path only ever emits the V2 events, so the V1 events are produced here synthetically to
 * confirm their listener handlers still resolve and project correctly (this is the listener-resolution guarantee the
 * V2/generated-class consolidation depends on). The {@code generated} events only transition existing RECORDED
 * registers, so each generate test first injects the matching {@code recorded} event.</p>
 */
public class InformantRegisterEventFlowsIT {

    private static final String EVENT_RECORDED = "results.event.informant-register-recorded";
    private static final String EVENT_RECORDED_V2 = "results.event.informant-register-recorded-v2";
    private static final String EVENT_GENERATED = "results.event.informant-register-generated";
    private static final String EVENT_GENERATED_V2 = "results.event.informant-register-generated-v2";

    private static final String V2_DOCUMENT_REQUEST_RESOURCE =
            "json/informant-register/results.add-informant-register-document-request.json";

    private static final Random RANDOM = new Random();

    private InformantRegisterDocumentRequestHelper helper;
    private MessageProducer privateProducer;

    @BeforeAll
    public static void setupStubs() {
        setupUsersGroupQueryStub();
    }

    @BeforeEach
    public void setup() {
        helper = new InformantRegisterDocumentRequestHelper();
        privateProducer = privateEvents.createProducer();
    }

    @AfterEach
    public void tearDown() throws JMSException {
        privateProducer.close();
    }

    @Test
    public void recordedV2EventShouldProjectRecordedRegister() {
        final UUID prosecutionAuthorityId = randomUUID();
        final ZonedDateTime registerDate = now(UTC);
        final JsonObject documentRequest = v2DocumentRequest(prosecutionAuthorityId, registerDate, randomUUID());

        publishRecorded(EVENT_RECORDED_V2, prosecutionAuthorityId, documentRequest);

        helper.verifyInformantRegisterRequestsExists(prosecutionAuthorityId);
    }

    @Test
    public void generatedV2EventShouldAdvanceRecordedRegister() {
        final UUID prosecutionAuthorityId = randomUUID();
        final ZonedDateTime registerDate = now(UTC);
        final JsonObject documentRequest = v2DocumentRequest(prosecutionAuthorityId, registerDate, randomUUID());

        publishRecorded(EVENT_RECORDED_V2, prosecutionAuthorityId, documentRequest);
        helper.verifyInformantRegisterRequestsExists(prosecutionAuthorityId);

        publishGenerated(EVENT_GENERATED_V2, documentRequest);
        helper.verifyInformantRegisterNoLongerRecorded(prosecutionAuthorityId);
    }

    @Test
    public void recordedEventShouldProjectRecordedRegister() {
        final UUID prosecutionAuthorityId = randomUUID();
        final ZonedDateTime registerDate = now(UTC);
        final JsonObject documentRequest = v1DocumentRequest(prosecutionAuthorityId, registerDate, randomUUID());

        publishRecorded(EVENT_RECORDED, prosecutionAuthorityId, documentRequest);

        helper.verifyInformantRegisterRequestsExists(prosecutionAuthorityId);
    }

    @Test
    public void generatedEventShouldAdvanceRecordedRegister() {
        final UUID prosecutionAuthorityId = randomUUID();
        final ZonedDateTime registerDate = now(UTC);
        final JsonObject documentRequest = v1DocumentRequest(prosecutionAuthorityId, registerDate, randomUUID());

        publishRecorded(EVENT_RECORDED, prosecutionAuthorityId, documentRequest);
        helper.verifyInformantRegisterRequestsExists(prosecutionAuthorityId);

        publishGenerated(EVENT_GENERATED, documentRequest);
        helper.verifyInformantRegisterNoLongerRecorded(prosecutionAuthorityId);
    }

    private void publishRecorded(final String eventName, final UUID prosecutionAuthorityId, final JsonObject documentRequest) {
        final JsonObject payload = createObjectBuilder()
                .add("prosecutionAuthorityId", prosecutionAuthorityId.toString())
                .add("informantRegister", documentRequest)
                .build();
        sendMessage(privateProducer, eventName, payload, metadataFor(eventName));
    }

    private void publishGenerated(final String eventName, final JsonObject documentRequest) {
        // Both generated schemas are additionalProperties:false and expose only
        // informantRegisterDocumentRequests + systemGenerated (the V1 schema also allows materialId),
        // so no extra fields may be added or the envelope is rejected at validation.
        final JsonObject payload = createObjectBuilder()
                .add("informantRegisterDocumentRequests", createArrayBuilder().add(documentRequest))
                .add("systemGenerated", true)
                .build();
        sendMessage(privateProducer, eventName, payload, metadataFor(eventName));
    }

    private Metadata metadataFor(final String eventName) {
        // Inject under a dedicated test source (not "results") with a high random event number so the
        // event buffer does not treat these synthetic events as already-seen for the real results source.
        // A fresh random stream id keeps each injected event at position 1 of its own stream.
        return metadataBuilder()
                .withId(randomUUID())
                .withStreamId(randomUUID())
                .withPosition(1)
                .withPreviousEventNumber(123)
                .withEventNumber(Math.abs(RANDOM.nextLong()))
                .withSource("event-indexer-test")
                .withName(eventName)
                .withUserId(randomUUID().toString())
                .build();
    }

    /**
     * A complete, schema-valid V2 (results-namespace) informant-register document request, built by reusing the
     * command-side test fixture so it stays in lock-step with the production schema.
     */
    private JsonObject v2DocumentRequest(final UUID prosecutionAuthorityId, final ZonedDateTime registerDate, final UUID hearingId) {
        final String body = getPayload(V2_DOCUMENT_REQUEST_RESOURCE)
                .replaceAll("%PROSECUTION_AUTHORITY_ID%", prosecutionAuthorityId.toString())
                .replaceAll("%PROSECUTION_AUTHORITY_CODE%", randomAlphanumeric(7))
                .replaceAll("%PROSECUTION_AUTHORITY_OU_CODE%", randomAlphanumeric(7))
                .replaceAll("%REGISTER_DATE%", registerDate.toString())
                .replaceAll("%HEARING_ID%", hearingId.toString())
                .replaceAll("%HEARING_DATE%", registerDate.minusHours(1).toString());
        return createReader(new StringReader(body)).readObject();
    }

    /**
     * A V1 (core-namespace) informant-register document request. The pre-migration shape carries a flat
     * {@code verdictCode} on the offence rather than a structured verdict object.
     */
    private JsonObject v1DocumentRequest(final UUID prosecutionAuthorityId, final ZonedDateTime registerDate, final UUID hearingId) {
        final JsonObject offence = createObjectBuilder()
                .add("offenceCode", "PS90010")
                .add("orderIndex", 1)
                .add("offenceTitle", "Theft")
                .add("pleaValue", "NOT_GUILTY")
                .add("verdictCode", "G")
                .build();

        final JsonObject hearingVenue = createObjectBuilder()
                .add("courtHouse", "Crown Court")
                .add("ljaName", "LJA")
                .add("courtSessions", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("courtRoom", "Room 1")
                                .add("hearingStartTime", registerDate.minusHours(1).toString())
                                .add("defendants", createArrayBuilder()
                                        .add(createObjectBuilder()
                                                .add("name", "John Smith")
                                                .add("address1", "1 High St")
                                                .add("firstName", "John")
                                                .add("lastName", "Smith")
                                                .add("prosecutionCasesOrApplications", createArrayBuilder()
                                                        .add(createObjectBuilder()
                                                                .add("caseOrApplicationReference", "TFL123")
                                                                .add("offences", createArrayBuilder().add(offence))))))))
                .build();

        return createObjectBuilder()
                .add("prosecutionAuthorityId", prosecutionAuthorityId.toString())
                .add("registerDate", registerDate.toString())
                .add("hearingDate", registerDate.minusHours(1).toString())
                .add("hearingId", hearingId.toString())
                .add("prosecutionAuthorityCode", randomAlphanumeric(7))
                .add("fileName", "informant-register.csv")
                .add("hearingVenue", hearingVenue)
                .build();
    }
}
