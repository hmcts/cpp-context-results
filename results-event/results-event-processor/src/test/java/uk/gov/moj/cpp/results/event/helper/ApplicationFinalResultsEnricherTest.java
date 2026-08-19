package uk.gov.moj.cpp.results.event.helper;

import static java.nio.charset.Charset.defaultCharset;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.results.event.service.ProgressionService;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.hamcrest.core.IsNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApplicationFinalResultsEnricherTest {
    private static final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
    @InjectMocks
    private ApplicationFinalResultsEnricher applicationFinalResultsEnricher;
    @Mock
    private ProgressionService progressionService;

    @Test
    void shouldDoEnrichmentIfApplicationResultsMissing() throws JsonProcessingException {
        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();
        jsonObjectBuilder.add("courtApplication", loadAsJsonObject("testdata/application-final-results-enricher/app1_progression_finalised.json"));
        final JsonObject applicationDetails = jsonObjectBuilder.build();
        given(progressionService.getApplicationDetails(any(UUID.class))).willReturn(Optional.of(applicationDetails));

        final JsonObject hearingPayload = loadAsJsonObject("testdata/application-final-results-enricher/app1_adjourned_hearing.json");
        final JsonObject enrichedHearing = applicationFinalResultsEnricher.enrichIfApplicationResultsMissing(hearingPayload);

        final JsonObject expectedHearing = loadAsJsonObject("testdata/application-final-results-enricher/app1_adjourned_hearing_enriched.json");
        assertEquals(objectMapper.writeValueAsString(expectedHearing), objectMapper.writeValueAsString(enrichedHearing), true);
        verify(progressionService, times(1)).getApplicationDetails(any(UUID.class));
    }

    @Test
    void shouldNotDoAnyEnrichmentIfProgressionReturnsEmptyResponse() throws JsonProcessingException {
        given(progressionService.getApplicationDetails(any(UUID.class))).willReturn(Optional.empty());

        final JsonObject inputHearing = loadAsJsonObject("testdata/application-final-results-enricher/app1_adjourned_hearing.json");
        final JsonObject enrichedHearing = applicationFinalResultsEnricher.enrichIfApplicationResultsMissing(inputHearing);

        assertEquals(objectMapper.writeValueAsString(inputHearing), objectMapper.writeValueAsString(enrichedHearing), true);
        verify(progressionService, times(1)).getApplicationDetails(any(UUID.class));
    }

    @Test
    void shouldDoEnrichmentWithAmendmentFieldsStrippedOffIfApplicationResultsMissing() throws JsonProcessingException {
        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();
        jsonObjectBuilder.add("courtApplication", loadAsJsonObject("testdata/application-final-results-enricher/app2_progression_finalised_resultsamended.json"));
        final JsonObject applicationDetails = jsonObjectBuilder.build();
        given(progressionService.getApplicationDetails(any(UUID.class))).willReturn(Optional.of(applicationDetails));

        final JsonObject hearingPayload = loadAsJsonObject("testdata/application-final-results-enricher/app2_adjourned_hearing.json");
        final JsonObject enrichedHearing = applicationFinalResultsEnricher.enrichIfApplicationResultsMissing(hearingPayload);

        final JsonObject expectedHearing = loadAsJsonObject("testdata/application-final-results-enricher/app2_adjourned_hearing_enriched.json");
        assertEquals(objectMapper.writeValueAsString(expectedHearing), objectMapper.writeValueAsString(enrichedHearing), true);
        verify(progressionService, times(1)).getApplicationDetails(any(UUID.class));
    }

    @Test
    void shouldNotDoAnyEnrichmentIfApplicationResultsAreNotMissing() throws JsonProcessingException {
        final JsonObject inputHearing = loadAsJsonObject("testdata/application-final-results-enricher/app3_resulted_hearing.json");

        final JsonObject enrichedHearing = applicationFinalResultsEnricher.enrichIfApplicationResultsMissing(inputHearing);

        assertEquals(objectMapper.writeValueAsString(inputHearing), objectMapper.writeValueAsString(enrichedHearing), true);
        verify(progressionService, never()).getApplicationDetails(any(UUID.class));
    }

    @Test
    void shouldNotDoAnyEnrichmentIfApplicationResultsAreNotFinalised() throws JsonProcessingException {
        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();
        jsonObjectBuilder.add("courtApplication", loadAsJsonObject("testdata/application-final-results-enricher/app_progression_listed.json"));
        final JsonObject applicationDetails = jsonObjectBuilder.build();
        given(progressionService.getApplicationDetails(any(UUID.class))).willReturn(Optional.of(applicationDetails));

        final JsonObject hearingPayload = loadAsJsonObject("testdata/application-final-results-enricher/app1_adjourned_hearing.json");
        final JsonObject enrichedHearing = applicationFinalResultsEnricher.enrichIfApplicationResultsMissing(hearingPayload);

        assertEquals(objectMapper.writeValueAsString(hearingPayload), objectMapper.writeValueAsString(enrichedHearing), true);
        verify(progressionService, times(1)).getApplicationDetails(any(UUID.class));
    }

    private static JsonObject loadAsJsonObject(final String jsonFile) throws JsonProcessingException {
        final String hearingJson = payloadAsString(jsonFile);
        return objectMapper.readValue(hearingJson, JsonObject.class);
    }

    // Additional test cases can be added here
    private static String payloadAsString(final String path) {
        try {
            final InputStream inputStream = ApplicationFinalResultsEnricherTest.class.getClassLoader().getResourceAsStream(path);
            assertThat(path, inputStream, IsNull.notNullValue());
            return IOUtils.toString(inputStream, defaultCharset());
        } catch (final IOException e) {
            throw new AssertionError("Failed to read payload from file:" + path, e);
        }
    }
}