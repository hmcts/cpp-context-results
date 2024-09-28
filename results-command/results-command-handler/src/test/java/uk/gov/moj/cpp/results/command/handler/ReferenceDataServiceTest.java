package uk.gov.moj.cpp.results.command.handler;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.spi.DefaultJsonMetadata;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;

import java.util.Optional;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class ReferenceDataServiceTest {

    @Mock
    private Requester requester;

    @Mock
    private Envelope<JsonObject> jsonEnvelope;

    @Mock
    private JsonObject wrapper;

    @Mock
    private JsonArray prosecutors;

    @Mock
    private JsonObject prosecutor;

    @InjectMocks
    private ReferenceDataService referenceDataService;

    @Test
    public void getSpiOutFlagForProsecutionAuthorityCodeTrue() {
        setupProsecutor();
        final JsonObjectBuilder jsonProsecutorBuilder = createObjectBuilder();
        jsonProsecutorBuilder
                .add("spiOutFlag", true)
                .add("policeFlag", true);

        when(prosecutors.getJsonObject(0)).thenReturn(jsonProsecutorBuilder.build());

        Optional<JsonObject> refDataProsecutorJson = referenceDataService.getSpiOutFlagForProsecutionAuthorityCode("someCode");;
        assertThat(refDataProsecutorJson.get().getBoolean("spiOutFlag"), is(true));
        assertThat(refDataProsecutorJson.get().getBoolean("policeFlag"), is(true));
    }

    @Test
    public void getSpiOutFlagForProsecutionAuthorityCodeFalse() {
        setupProsecutor();
        final JsonObjectBuilder jsonProsecutorBuilder = createObjectBuilder();
        jsonProsecutorBuilder
                .add("spiOutFlag", false)
                .add("policeFlag", false);

        when(prosecutors.getJsonObject(0)).thenReturn(jsonProsecutorBuilder.build());

        Optional<JsonObject> refDataProsecutorJson = referenceDataService.getSpiOutFlagForProsecutionAuthorityCode("someCode");;
        assertThat(refDataProsecutorJson.get().getBoolean("spiOutFlag"), is(false));
        assertThat(refDataProsecutorJson.get().getBoolean("policeFlag"), is(false));
    }


    @Test
    public void shouldReturnFalseProsecutorsNotFoundInResponseJson() {
        setupProsecutor();
        when(prosecutors.getJsonObject(0)).thenReturn(null);

        Optional<JsonObject> refDataProsecutorJson = referenceDataService.getSpiOutFlagForProsecutionAuthorityCode("someCode");;
        assertThat(refDataProsecutorJson.isPresent(), is(false));
    }

    @Test
    public void shouldReturnFalseWhenSpiOutIsSetToNull() {
        setupProsecutor();
        final JsonObject prosecutor = createObjectBuilder()
                .add("spiOutFlag", JsonValue.NULL).build();
        final JsonArray prosecutors = createArrayBuilder()
                .add(prosecutor).build();

        final JsonObject responsePayload = createObjectBuilder().add("prosecutors", prosecutors).build();
        final JsonEnvelope queryResponse = envelopeFrom(MetadataBuilderFactory.metadataWithRandomUUIDAndName(), responsePayload);

        assertThat(referenceDataService.getSpiOutFlagForProsecutionAuthorityCode("someCode") , is(Optional.empty()));
    }

    @Test
    public void getSpiOutFlagForOriginatingOrganisationTrue() {
        final JsonObject jsonProsecutor = createObjectBuilder()
                .add("spiOutFlag", true)
                .add("policeFlag", true).build();

        when(requester.requestAsAdmin(any(), any())).thenReturn(Envelope.envelopeFrom(DefaultJsonMetadata.metadataBuilder().withId(randomUUID()).withName("referencedata.query.get.prosecutor.by.oucode"), jsonProsecutor));

        Optional<JsonObject> refDataProsecutorJson = referenceDataService.getSpiOutFlagForOriginatingOrganisation("someCode");

        assertThat(refDataProsecutorJson.get().getBoolean("spiOutFlag"), is(true));
        assertThat(refDataProsecutorJson.get().getBoolean("policeFlag"), is(true));
    }

    private void setupProsecutor(){
        when(requester.requestAsAdmin(any(JsonEnvelope.class), any())).thenAnswer(invocationOnMock -> {
            final JsonEnvelope envelope = (JsonEnvelope) invocationOnMock.getArguments()[0];
            JsonObject responsePayload = createObjectBuilder()
                    .add("prosecutors", prosecutors)
                    .build();

            return envelopeFrom(envelope.metadata(), responsePayload);
        });
    }


}