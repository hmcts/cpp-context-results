package uk.gov.moj.cpp.results.event.processor;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.results.event.service.InformantRegisterPublishException;
import uk.gov.moj.cpp.results.event.service.InformantRegisterQueueService;

@ExtendWith(MockitoExtension.class)
public class InformantRegisterPublishRequestedProcessorTest {

    private static final String EVENT_NAME = "results.events.informant-register-publish-requested";
    private static final String HEARING_ID = "0aa5bf35-1e51-45e5-9e42-64bd57e15c11";
    private static final String HEARING_DAY = "2026-08-19";
    private static final String SHARED_TIME = "2026-08-19T16:42:07.512Z";
    private static final UUID USER_ID = UUID.fromString("1a3f7c68-4c4b-4a1f-93cd-6e2ac2c4a1d0");

    @Mock
    private InformantRegisterQueueService informantRegisterQueueService;

    @InjectMocks
    private InformantRegisterPublishRequestedProcessor processor;

    /**
     * A typo in the @Handles string or a wrong @ServiceComponent value would otherwise only surface
     * at deploy, as an event published to the topic that no selector matches - silently discarded.
     */
    @Test
    public void publishInformantRegisterRequest_should_beWiredAsAnEventProcessor() {
        assertThat(new InformantRegisterPublishRequestedProcessor(), isHandler(EVENT_PROCESSOR)
                .with(method("publishInformantRegisterRequest").thatHandles(EVENT_NAME)));
    }

    @Test
    public void publishInformantRegisterRequest_should_publishTheShareValuesFromTheEvent() {
        processor.publishInformantRegisterRequest(publishRequestedEvent(SHARED_TIME));

        verify(informantRegisterQueueService).publishDistributionCommand(HEARING_ID, HEARING_DAY, SHARED_TIME, USER_ID);
    }

    /**
     * The requestId minted downstream is a hash of hearingDay and sharedTime, so they have to reach
     * the queue exactly as the hearing-resulted event carried them. A timestamp that would not
     * survive a parse-and-re-render round trip proves this leg does not perform one.
     */
    @Test
    public void publishInformantRegisterRequest_should_notReformatTheSharedTime() {
        final String awkwardSharedTime = "2026-08-19T16:42:07.5Z";

        processor.publishInformantRegisterRequest(publishRequestedEvent(awkwardSharedTime));

        verify(informantRegisterQueueService).publishDistributionCommand(HEARING_ID, HEARING_DAY, awkwardSharedTime, USER_ID);
    }

    /**
     * The acceptance criterion this class exists for: a failed publish must never be silently lost.
     * Letting the exception escape rolls back the delivery, so the framework redelivers and
     * ultimately parks the event on the DLQ, where the owed publish is recoverable. Catching here
     * would put us back to the fire-and-forget behaviour this design replaced.
     */
    @Test
    public void publishInformantRegisterRequest_whenThePublishFails_should_propagateSoTheEventIsRedelivered() {
        final InformantRegisterPublishException publishFailure =
                new InformantRegisterPublishException("broker unavailable", new RuntimeException());
        doThrow(publishFailure).when(informantRegisterQueueService)
                .publishDistributionCommand(anyString(), anyString(), anyString(), any(UUID.class));

        final InformantRegisterPublishException thrown = assertThrows(InformantRegisterPublishException.class,
                () -> processor.publishInformantRegisterRequest(publishRequestedEvent(SHARED_TIME)));

        assertThat(thrown, is(publishFailure));
    }

    private JsonEnvelope publishRequestedEvent(final String sharedTime) {
        final JsonObject payload = createObjectBuilder()
                .add("hearingId", HEARING_ID)
                .add("hearingDay", HEARING_DAY)
                .add("sharedTime", sharedTime)
                .add("userId", USER_ID.toString())
                .build();

        return envelopeFrom(metadataWithRandomUUID(EVENT_NAME).withStreamId(randomUUID()).build(), payload);
    }
}
