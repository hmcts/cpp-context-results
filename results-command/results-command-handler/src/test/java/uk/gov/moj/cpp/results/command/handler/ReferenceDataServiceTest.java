package uk.gov.moj.cpp.results.command.handler;

import static javax.json.Json.createObjectBuilder;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.json.JsonArray;
import javax.json.JsonObject;

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
    private JsonEnvelope jsonEnvelope;
    @Mock
    private JsonObject wrapper;
    @Mock
    private JsonArray prosecutors;
    @Mock
    private JsonObject prosecutor;
    @InjectMocks
    private ReferenceDataService referenceDataService;

    @Before
    public void setup() {
        when(requester.requestAsAdmin(any())).thenReturn(jsonEnvelope);
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(wrapper);
        when(wrapper.getJsonArray("prosecutors")).thenReturn(prosecutors);
        when(prosecutors.size()).thenReturn(1);
        when(prosecutors.getJsonObject(0)).thenReturn(prosecutor);
    }

    @Test
    public void getSpiOutFlagForProsecutionAuthorityCodeTrue() {
        when(prosecutor.getBoolean("spiOutFlag")).thenReturn(true);
        assertTrue(referenceDataService.getSpiOutFlagForProsecutionAuthorityCode("someCode"));
    }

    @Test
    public void getSpiOutFlagForProsecutionAuthorityCodeFalse() {
        when(prosecutor.getBoolean("spiOutFlag")).thenReturn(false);
        assertFalse(referenceDataService.getSpiOutFlagForProsecutionAuthorityCode("someCode"));
    }


    @Test
    public void shouldReturnFalseProsecutorsNotFoundInResponseJson() {
        when(wrapper.getJsonArray("prosecutors")).thenReturn(null);
        assertFalse(referenceDataService.getSpiOutFlagForProsecutionAuthorityCode("someCode"));
    }

    @Test
    public void shouldReturnFalseProsecutorNotFound() {
        when(prosecutors.size()).thenReturn(0);
        assertFalse(referenceDataService.getSpiOutFlagForProsecutionAuthorityCode("someCode"));
    }

    @Test
    public void shouldReturnFalseWhenClassCaseException() {
        final JsonObject response = createObjectBuilder().add("spiOutFlag", "wibble").build();
        when(prosecutors.getJsonObject(0)).thenReturn(response);
        assertFalse(referenceDataService.getSpiOutFlagForProsecutionAuthorityCode("someCode"));
    }

    @Test
    public void shouldReturnFalseWhenspiOutFlagNotFound() {
        final JsonObject response = createObjectBuilder().build();
        when(prosecutors.getJsonObject(0)).thenReturn(response);
        assertFalse(referenceDataService.getSpiOutFlagForProsecutionAuthorityCode("someCode"));
    }
}