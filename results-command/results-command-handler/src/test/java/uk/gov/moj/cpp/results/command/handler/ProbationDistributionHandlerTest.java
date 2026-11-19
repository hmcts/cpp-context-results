package uk.gov.moj.cpp.results.command.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher;
import uk.gov.moj.cpp.results.command.RequestProbationDistributionPublish;
import uk.gov.moj.cpp.results.domain.event.ProbationDistributionPublishRequested;

import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProbationDistributionHandlerTest {

    private static final String REQUEST_PROBATION_DISTRIBUTION_PUBLISH_COMMAND_NAME = "results.command.request-probation-distribution-publish";
    private static final String PROBATION_DISTRIBUTION_PUBLISH_REQUESTED_EVENT_NAME = "results.events.probation-distribution-publish-requested";
    private static final UUID HEARING_ID = UUID.fromString("0aa5bf35-1e51-45e5-9e42-64bd57e15c11");
    private static final UUID SHARING_USER_ID = UUID.fromString("1a3f7c68-4c4b-4a1f-93cd-6e2ac2c4a1d0");
    private static final String HEARING_DAY = "2026-08-19";
    private static final String SHARED_TIME = "2026-08-19T16:42:07.512Z";

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(ProbationDistributionPublishRequested.class);

    @InjectMocks
    private ProbationDistributionHandler probationDistributionHandler;

    @Test
    public void handleRequestProbationDistributionPublish_should_beWiredAsACommandHandler() {
        assertThat(new ProbationDistributionHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handleRequestProbationDistributionPublish")
                        .thatHandles(REQUEST_PROBATION_DISTRIBUTION_PUBLISH_COMMAND_NAME)
                ));
    }

    /**
     * The stream id is the hearingId, not the requestId. The framework serialises dispatch per
     * stream, so keying on the hearing is what stops a re-share overtaking the share it supersedes
     * on the way to the queue.
     */
    @Test
    public void handleRequestProbationDistributionPublish_should_appendTheEventToTheHearingStream() throws Exception {
        when(eventSource.getStreamById(HEARING_ID)).thenReturn(eventStream);

        probationDistributionHandler.handleRequestProbationDistributionPublish(publishRequestEnvelope(SHARED_TIME));

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata().withName(PROBATION_DISTRIBUTION_PUBLISH_REQUESTED_EVENT_NAME),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.hearingId", is(HEARING_ID.toString())),
                                withJsonPath("$.hearingDay", is(HEARING_DAY)),
                                withJsonPath("$.sharedTime", is(SHARED_TIME)),
                                withJsonPath("$.userId", is(SHARING_USER_ID.toString()))
                        ))
                )
        ));
    }

    /**
     * A share and a later re-share of the same hearing must land on one stream, so the framework
     * dispatches them in order and the consuming service cannot apply the older share last.
     */
    @Test
    public void handleRequestProbationDistributionPublish_forAShareAndAReshare_should_useTheSameStream() throws Exception {
        when(eventSource.getStreamById(HEARING_ID)).thenReturn(eventStream);

        probationDistributionHandler.handleRequestProbationDistributionPublish(publishRequestEnvelope(SHARED_TIME));
        probationDistributionHandler.handleRequestProbationDistributionPublish(publishRequestEnvelope("2026-08-19T19:30:00.000Z"));

        verify(eventSource, times(2)).getStreamById(HEARING_ID);
        verifyNoMoreInteractions(eventSource);
    }

    /**
     * sharedTime must reach the event exactly as it arrived: the requestId minted downstream is a
     * hash of it, so a re-rendered value would mint a different id for the same share.
     */
    @Test
    public void handleRequestProbationDistributionPublish_should_carryTheShareValuesUnaltered() throws Exception {
        final String awkwardSharedTime = "2026-08-19T16:42:07.5Z";
        when(eventSource.getStreamById(HEARING_ID)).thenReturn(eventStream);

        probationDistributionHandler.handleRequestProbationDistributionPublish(publishRequestEnvelope(awkwardSharedTime));

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata().withName(PROBATION_DISTRIBUTION_PUBLISH_REQUESTED_EVENT_NAME),
                        JsonEnvelopePayloadMatcher.payload().isJson(
                                withJsonPath("$.sharedTime", is(awkwardSharedTime))
                        )
                )
        ));
    }

    private Envelope<RequestProbationDistributionPublish> publishRequestEnvelope(final String sharedTime) {
        return Envelope.envelopeFrom(
                metadataWithRandomUUID(REQUEST_PROBATION_DISTRIBUTION_PUBLISH_COMMAND_NAME).build(),
                RequestProbationDistributionPublish.requestProbationDistributionPublish()
                        .withHearingId(HEARING_ID)
                        .withHearingDay(HEARING_DAY)
                        .withSharedTime(sharedTime)
                        .withUserId(SHARING_USER_ID)
                        .build());
    }
}
