package uk.gov.moj.cpp.results.command;

import static java.util.UUID.fromString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.results.command.api.GeneratePoliceResultsDefendantApi;

import java.util.List;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class GeneratePoliceResultsDefendantApiTest {

    private static final String COMMAND_NAME = "results.command.generate-police-results-for-a-defendant";
    private static final String SESSION_ID = "sessionId";
    private static final String CASE_ID = "caseId";
    private static final String DEFENDENT_ID = "defendentId";
    private static final String SESSION_ID_VALUE = "4ebc5dd1-9629-4b7d-a56b-efbcf0ec5e1d";
    private static final String CASE_ID_VALUE = "5ebc5dd1-9629-4b7d-a56b-efbcf0ec5e1d";
    private static final String DEFENDENT_ID_VALUE = "6ebc5dd1-9629-4b7d-a56b-efbcf0ec5e1d";

    @Spy
    @SuppressWarnings("unused")
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private Sender sender;

    @InjectMocks
    private GeneratePoliceResultsDefendantApi generatePoliceResultsDefendantApi;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeCaptor;

    @Test
    public void shouldHandleCommand() {
        assertThat(GeneratePoliceResultsDefendantApi.class, isHandlerClass(COMMAND_API)
                .with(method("generatePoliceResultsForDefendant").thatHandles(COMMAND_NAME)));
    }

    @Test
    public void shouldGeneratePoliceResultsForDefendantCommand() {
        final JsonEnvelope command = envelope().with(metadataWithRandomUUID(COMMAND_NAME))
                .withPayloadOf(fromString(SESSION_ID_VALUE), SESSION_ID)
                .withPayloadOf(fromString(CASE_ID_VALUE), CASE_ID)
                .withPayloadOf(fromString(DEFENDENT_ID_VALUE), DEFENDENT_ID)
                .build();

        generatePoliceResultsDefendantApi.generatePoliceResultsForDefendant(command);
        verify(sender).send(envelopeCaptor.capture());
        List<JsonEnvelope> jsonEnvelopeList  = envelopeCaptor.getAllValues();
        JsonObject jsonObject =  jsonEnvelopeList.get(0).payloadAsJsonObject();

        assertThat(jsonObject.getString(SESSION_ID), is(SESSION_ID_VALUE));
        assertThat(jsonObject.getString(CASE_ID), is(CASE_ID_VALUE));
        assertThat(jsonObject.getString(DEFENDENT_ID), is(DEFENDENT_ID_VALUE));
    }
}
