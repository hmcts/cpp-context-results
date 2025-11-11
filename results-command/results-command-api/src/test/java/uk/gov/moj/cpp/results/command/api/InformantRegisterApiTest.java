package uk.gov.moj.cpp.results.command.api;

import static java.time.LocalDate.now;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.spi.DefaultEnvelope;
import uk.gov.justice.services.messaging.spi.DefaultJsonEnvelopeProvider;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class InformantRegisterApiTest {

    private static final String ADD_INFORMANT_REGISTER_REQUEST_NAME = "results.add-informant-register";
    private static final String ADD_INFORMANT_REGISTER_COMMAND_NAME = "results.command.add-informant-register";
    private static final String GENERATE_INFORMANT_REGISTER_REQUEST_NAME = "results.generate-informant-register";
    private static final String GENERATE_INFORMANT_REGISTER_COMMAND_NAME = "results.command.generate-informant-register";
    private static final String GENERATE_INFORMANT_REGISTER_BY_DATE_REQUEST_NAME = "results.generate-informant-register-by-date";
    private static final String GENERATE_INFORMANT_REGISTER_BY_DATE_COMMAND_NAME = "results.command.generate-informant-register-by-date";

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<DefaultEnvelope> envelopeCaptor;

    @InjectMocks
    private InformantRegisterApi informantRegisterApi;

    @Test
    public void shouldHandleAddInformantRegisterCommand() {
        assertThat(InformantRegisterApi.class, isHandlerClass(COMMAND_API)
                .with(method("handleAddInformantRegister").thatHandles(ADD_INFORMANT_REGISTER_REQUEST_NAME)));
    }

    @Test
    public void shouldRecordInformantRegisterRequest() {

        final JsonEnvelope commandEnvelope = buildEnvelope();

        informantRegisterApi.handleAddInformantRegister(commandEnvelope);

        verify(sender).send(envelopeCaptor.capture());

        final DefaultEnvelope newCommand = envelopeCaptor.getValue();

        assertThat(newCommand.metadata().name(), is(ADD_INFORMANT_REGISTER_COMMAND_NAME));
        assertThat(newCommand.payload(), equalTo(commandEnvelope.payloadAsJsonObject()));
    }

    @Test
    public void shouldGenerateInformantRegister() {

        final JsonEnvelope commandEnvelope = buildGenerateInformantRegisterEnvelope(GENERATE_INFORMANT_REGISTER_REQUEST_NAME);

        informantRegisterApi.handleGenerateInformantRegister(commandEnvelope);

        verify(sender).send(envelopeCaptor.capture());

        final DefaultEnvelope newCommand = envelopeCaptor.getValue();

        assertThat(newCommand.metadata().name(), is(GENERATE_INFORMANT_REGISTER_COMMAND_NAME));
        assertThat(newCommand.payload(), equalTo(commandEnvelope.payloadAsJsonObject()));
    }

    @Test
    public void shouldGenerateInformantRegisterByDate() {

        final JsonEnvelope commandEnvelope = buildGenerateInformantRegisterEnvelope(GENERATE_INFORMANT_REGISTER_BY_DATE_REQUEST_NAME);

        informantRegisterApi.handleGenerateInformantRegisterByDate(commandEnvelope);

        verify(sender).send(envelopeCaptor.capture());

        final DefaultEnvelope newCommand = envelopeCaptor.getValue();

        assertThat(newCommand.metadata().name(), is(GENERATE_INFORMANT_REGISTER_BY_DATE_COMMAND_NAME));
        assertThat(newCommand.payload(), equalTo(commandEnvelope.payloadAsJsonObject()));
    }

    private JsonEnvelope buildEnvelope() {
        final JsonObject payload = createObjectBuilder()
                .add("informantRegisterRequest", createObjectBuilder().add("prosecutionAuthorityId", randomUUID().toString()).build())
                .build();

        final Metadata metadata = metadataBuilder()
                .withName(ADD_INFORMANT_REGISTER_REQUEST_NAME)
                .withId(randomUUID())
                .withUserId(randomUUID().toString())
                .build();

        return new DefaultJsonEnvelopeProvider().envelopeFrom(metadata, payload);
    }

    private JsonEnvelope buildGenerateInformantRegisterEnvelope(final String commandName) {
        final JsonObject payload = createObjectBuilder()
                .add("registerDate", now().toString())
                .build();

        final Metadata metadata = metadataBuilder()
                .withName(commandName)
                .withId(randomUUID())
                .withUserId(randomUUID().toString())
                .build();

        return new DefaultJsonEnvelopeProvider().envelopeFrom(metadata, payload);
    }
}
