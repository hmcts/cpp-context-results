package uk.gov.moj.cpp.results.event.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class StagingEnforcementAcknowledgmentEventProcessorTest {

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeArgumentCaptor;

    @InjectMocks
    private StagingEnforcementAcknowledgmentEventProcessor stagingEnforcementAcknowledgmentEventProcessor;

    @Test
    public void shouldUpdateGobAccount() {
        final String requestId = randomUUID().toString();
        final JsonObject enforcementResponsePayload = createObjectBuilder()
                .add("originator", "Courts")
                .add("requestId", requestId)
                .add("exportStatus", "ENFORCEMENT_ACKNOWLEDGED")
                .add("acknowledgement", createObjectBuilder().add("accountNumber", "201366829").build())
                .build();

        final JsonEnvelope event = JsonEnvelope.envelopeFrom(
                metadataWithRandomUUID("public.stagingenforcement.enforce-financial-imposition-acknowledgement"), enforcementResponsePayload);


        stagingEnforcementAcknowledgmentEventProcessor.processAcknowledgement(event);

        verify(sender).sendAsAdmin(envelopeArgumentCaptor.capture());
        final Envelope<JsonObject> argumentCaptor = envelopeArgumentCaptor.getValue();
        final JsonEnvelope allValues = envelopeFrom(argumentCaptor.metadata(), argumentCaptor.payload());
        assertThat(allValues,
                jsonEnvelope(
                        metadata().withName("result.command.update-gob-account"),
                        payloadIsJson(allOf(
                                withJsonPath("$.accountNumber", is("201366829")),
                                withJsonPath("$.correlationId", is(requestId))
                        ))));
    }

    @Test
    public void shouldUpdateGobAccountForATCM() {
        final String requestId = randomUUID().toString();
        final JsonObject enforcementResponsePayload = createObjectBuilder()
                .add("originator", "ATCM")
                .add("requestId", requestId)
                .add("exportStatus", "ENFORCEMENT_ACKNOWLEDGED")
                .add("acknowledgement", createObjectBuilder().add("accountNumber", "201366829").build())
                .build();

        final JsonEnvelope event = JsonEnvelope.envelopeFrom(
                metadataWithRandomUUID("public.stagingenforcement.enforce-financial-imposition-acknowledgement"), enforcementResponsePayload);


        stagingEnforcementAcknowledgmentEventProcessor.processAcknowledgement(event);

        verify(sender).sendAsAdmin(envelopeArgumentCaptor.capture());
        final Envelope<JsonObject> argumentCaptor = envelopeArgumentCaptor.getValue();
        final JsonEnvelope allValues = envelopeFrom(argumentCaptor.metadata(), argumentCaptor.payload());
        assertThat(allValues,
                jsonEnvelope(
                        metadata().withName("result.command.update-gob-account"),
                        payloadIsJson(allOf(
                                withJsonPath("$.accountNumber", is("201366829")),
                                withJsonPath("$.correlationId", is(requestId))
                        ))));
    }

    @Test
    public void shouldNotCallCommandWhenOriginatorIsNotCourts() {
        final String requestId = randomUUID().toString();
        final JsonObject enforcementResponsePayload = createObjectBuilder()
                .add("originator", "XXXXX")
                .add("requestId", requestId)
                .add("exportStatus", "ENFORCEMENT_ACKNOWLEDGED")
                .add("acknowledgement", createObjectBuilder().add("accountNumber", "201366829").build())
                .build();

        final JsonEnvelope event = JsonEnvelope.envelopeFrom(
                metadataWithRandomUUID("public.stagingenforcement.enforce-financial-imposition-acknowledgement"), enforcementResponsePayload);


        stagingEnforcementAcknowledgmentEventProcessor.processAcknowledgement(event);

        verify(sender, times(0)).sendAsAdmin(envelopeArgumentCaptor.capture());
    }

    @Test
    public void shouldNotCallCommandWhenAcknowledgmentIsFail() {
        final String requestId = randomUUID().toString();
        final JsonObject enforcementResponsePayload = createObjectBuilder()
                .add("originator", "Courts")
                .add("requestId", requestId)
                .add("exportStatus", "ENFORCEMENT_ACKNOWLEDGEMENT_FAILED")
                .add("acknowledgement", createObjectBuilder().add("errorCode", "errorCode").build())
                .build();

        final JsonEnvelope event = JsonEnvelope.envelopeFrom(
                metadataWithRandomUUID("public.stagingenforcement.enforce-financial-imposition-acknowledgement"), enforcementResponsePayload);


        stagingEnforcementAcknowledgmentEventProcessor.processAcknowledgement(event);

        verify(sender, times(0)).sendAsAdmin(envelopeArgumentCaptor.capture());
    }

    @Test
    public void shouldCallUpdatecorrelationId() {
        final String masterDefendantId = randomUUID().toString();
        final String accountCorrelationId = randomUUID().toString();

        final JsonObject enforcementResponsePayload = createObjectBuilder()
                .add("hearingFinancialResultRequest", createObjectBuilder()
                        .add("accountCorrelationId", accountCorrelationId)
                        .add("masterDefendantId", masterDefendantId))
                .build();

        final JsonEnvelope event = JsonEnvelope.envelopeFrom(
                metadataWithRandomUUID("results.event.hearing-financial-results-tracked"), enforcementResponsePayload);

        stagingEnforcementAcknowledgmentEventProcessor.processHearingFinancialResultsTracked(event);

        verify(sender).sendAsAdmin(envelopeArgumentCaptor.capture());
        final Envelope<JsonObject> argumentCaptor = envelopeArgumentCaptor.getValue();
        final JsonEnvelope allValues = envelopeFrom(argumentCaptor.metadata(), argumentCaptor.payload());
        assertThat(allValues,
                jsonEnvelope(
                        metadata().withName("result.command.add-correlation-id"),
                        payloadIsJson(allOf(
                                withJsonPath("$.masterDefendantId", is(masterDefendantId)),
                                withJsonPath("$.correlationId", is(accountCorrelationId))
                        ))));

    }
}
