package uk.gov.moj.cpp.results.command.handler;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.spi.DefaultJsonMetadata;

import java.util.Optional;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class ReferenceDataServiceTest {

    @Mock
    private Requester requester;

    @Mock
    private Envelope<JsonObject> jsonEnvelope;

    @Mock
    private JsonObject wrapper;

    @Mock
    private JsonArray prosecutors;

    @InjectMocks
    private ReferenceDataService referenceDataService;

    @Before
    public void setup() {
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

    @Test
    public void getSpiOutFlagForProsecutionAuthorityCodeTrue() {
        final JsonObjectBuilder jsonProsecutorBuilder = createObjectBuilder();
        jsonProsecutorBuilder
                .add("spiOutFlag", true)
                .add("policeFlag", true);

        when(requester.requestAsAdmin(any(JsonEnvelope.class), any())).thenAnswer(invocationOnMock -> {
            final JsonEnvelope envelope = (JsonEnvelope) invocationOnMock.getArguments()[0];
            JsonObject responsePayload = createObjectBuilder()
                    .add("prosecutors", prosecutors)
                    .build();

            return envelopeFrom(envelope.metadata(), responsePayload);

        });

        when(prosecutors.getJsonObject(0)).thenReturn(jsonProsecutorBuilder.build());

        Optional<JsonObject> refDataProsecutorJson = referenceDataService.getSpiOutFlagForProsecutionAuthorityCode("someCode");
        assertThat(refDataProsecutorJson.get().getBoolean("spiOutFlag"), is(true));
        assertThat(refDataProsecutorJson.get().getBoolean("policeFlag"), is(true));
    }


    @Test
    public void getSpiOutFlagForProsecutionAuthorityCodeFalse() {
        final JsonObjectBuilder jsonProsecutorBuilder = createObjectBuilder();
        jsonProsecutorBuilder
                .add("spiOutFlag", false)
                .add("policeFlag", false);
        when(requester.requestAsAdmin(any(JsonEnvelope.class), any())).thenAnswer(invocationOnMock -> {
            final JsonEnvelope envelope = (JsonEnvelope) invocationOnMock.getArguments()[0];
            JsonObject responsePayload = createObjectBuilder()
                    .add("prosecutors", prosecutors)
                    .build();

            return envelopeFrom(envelope.metadata(), responsePayload);

        });

        when(prosecutors.getJsonObject(0)).thenReturn(jsonProsecutorBuilder.build());

        Optional<JsonObject> refDataProsecutorJson = referenceDataService.getSpiOutFlagForProsecutionAuthorityCode("someCode");

        assertThat(refDataProsecutorJson.get().getBoolean("spiOutFlag"), is(false));
        assertThat(refDataProsecutorJson.get().getBoolean("policeFlag"), is(false));
    }

    @Test
    public void getSpiOutFlagForOriginatingOrganisationFalse() {
        final JsonObject jsonProsecutor = createObjectBuilder()
                .add("spiOutFlag", false)
                .add("policeFlag", false).build();

        when(requester.requestAsAdmin(any(), any())).thenReturn(Envelope.envelopeFrom(DefaultJsonMetadata.metadataBuilder().withId(randomUUID()).withName("referencedata.query.get.prosecutor.by.oucode"), jsonProsecutor));

        Optional<JsonObject> refDataProsecutorJson = referenceDataService.getSpiOutFlagForOriginatingOrganisation("someCode");

        assertThat(refDataProsecutorJson.get().getBoolean("spiOutFlag"), is(false));
        assertThat(refDataProsecutorJson.get().getBoolean("policeFlag"), is(false));
    }


    @Test
    public void shouldReturnFalseProsecutorsNotFoundInResponseJson() {
        when(requester.requestAsAdmin(any(), any())).thenReturn(null);

        Optional<JsonObject> refDataProsecutorJson = referenceDataService.getSpiOutFlagForOriginatingOrganisation("someCode");

        assertThat(refDataProsecutorJson.isPresent(), is(false));
    }

    @Test
    public void shouldNotReturnWhenSpiOutIsNotSet() {

        final JsonObject jsonProsecutor = createObjectBuilder()
                .add("policeFlag", false).build();

        when(requester.requestAsAdmin(any(), any())).thenReturn(Envelope.envelopeFrom(DefaultJsonMetadata.metadataBuilder().withId(randomUUID()).withName("referencedata.query.get.prosecutor.by.oucode"), jsonProsecutor));

        Optional<JsonObject> refDataProsecutorJson = referenceDataService.getSpiOutFlagForOriginatingOrganisation("someCode");
        
        assertThat(refDataProsecutorJson.get().containsKey("spiOutFlag"), is(false));
        assertThat(refDataProsecutorJson.get().getBoolean("policeFlag"), is(false));
    }
}