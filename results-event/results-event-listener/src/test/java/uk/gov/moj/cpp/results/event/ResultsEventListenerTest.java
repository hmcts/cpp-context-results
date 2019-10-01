package uk.gov.moj.cpp.results.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import net.minidev.json.JSONValue;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Captor;
import org.mockito.Spy;
import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;


import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.domains.results.shareresults.PublicHearingResulted;
import uk.gov.moj.cpp.results.persist.HearingResultedDocumentRepository;
import uk.gov.moj.cpp.results.persist.entity.HearingResultedDocument;
import uk.gov.moj.cpp.results.test.TestTemplates;

import javax.json.Json;
import javax.json.JsonObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;


@RunWith(MockitoJUnitRunner.class)
public class ResultsEventListenerTest {


    @InjectMocks
    private ResultsEventListener resultsEventListener;

    @Mock
    private HearingResultedDocumentRepository hearingResultedDocumentRepository;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Captor
    private ArgumentCaptor<HearingResultedDocument> hearingResultedDocumentArgumentCaptor;

    @Before
    public void setup() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.jsonObjectToObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void saveHearingResultWithOneHearingDate_ShouldHaveBothStartDateAndEndDateSame() {

        PublicHearingResulted shareResultsMessage = TestTemplates.basicShareResultsTemplate();
        shareResultsMessage.getHearing().setHearingDays(Arrays.asList(HearingDay.hearingDay()
                .withSittingDay(ZonedDateTime.of(LocalDate.of(2018, 06, 04), LocalTime.of(12, 00), ZoneId.of("UTC")))
                .withListedDurationMinutes(100)
                .build()));
        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("results.hearing-results-added"),
                objectToJsonObjectConverter.convert(shareResultsMessage));

        resultsEventListener.hearingResultsAdded(envelope);

        verify(this.hearingResultedDocumentRepository, times(1)).save(this.hearingResultedDocumentArgumentCaptor.capture());

        assertThat(hearingResultedDocumentArgumentCaptor.getAllValues(), is(notNullValue()));
        assertThat(hearingResultedDocumentArgumentCaptor.getAllValues().size(), is(1));
        assertThat(hearingResultedDocumentArgumentCaptor.getAllValues().get(0).getHearingId(), is(shareResultsMessage.getHearing().getId()));
        assertThat(hearingResultedDocumentArgumentCaptor.getAllValues().get(0).getStartDate(), is(LocalDate.of(2018, 06, 04)));
        assertThat(hearingResultedDocumentArgumentCaptor.getAllValues().get(0).getEndDate(), is(LocalDate.of(2018, 06, 04)));
        assertThat(hearingResultedDocumentArgumentCaptor.getAllValues().get(0).getPayload(), is(objectToJsonObjectConverter.convert(shareResultsMessage).toString()));
    }

    @Test
    public void saveHearingResultWithMultipleHearingDates_ShouldHaveRightStartDateAndEndDate() {

        PublicHearingResulted shareResultsMessage = TestTemplates.basicShareResultsTemplate();
        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("results.hearing-results-added"),
                objectToJsonObjectConverter.convert(shareResultsMessage));

        resultsEventListener.hearingResultsAdded(envelope);

        verify(this.hearingResultedDocumentRepository, times(1)).save(this.hearingResultedDocumentArgumentCaptor.capture());

        assertThat(hearingResultedDocumentArgumentCaptor.getAllValues(), is(notNullValue()));
        assertThat(hearingResultedDocumentArgumentCaptor.getAllValues().size(), is(1));
        assertThat(hearingResultedDocumentArgumentCaptor.getAllValues().get(0).getHearingId(), is(shareResultsMessage.getHearing().getId()));
        assertThat(hearingResultedDocumentArgumentCaptor.getAllValues().get(0).getStartDate(), is(LocalDate.of(2018, 02, 02)));
        assertThat(hearingResultedDocumentArgumentCaptor.getAllValues().get(0).getEndDate(), is(LocalDate.of(2018, 06, 04)));
        assertThat(hearingResultedDocumentArgumentCaptor.getAllValues().get(0).getPayload(), is(objectToJsonObjectConverter.convert(shareResultsMessage).toString()));
    }

    @Test
    public void hearingCaseEjected_whenCaseIdMatchesInPayload_expectIsEjectedFlagAddedInPayloadForCase() throws IOException {
        final ObjectMapper mapper = new ObjectMapperProducer().objectMapper();
        final String PROSECUTION_CASES = "prosecutionCases";
        final String HEARING = "hearing";
        final UUID hearingId = randomUUID();
        final String caseId = "cccc1111-1e20-4c21-916a-81a6c90239e5";
        final JsonObject payload = Json.createObjectBuilder()
                .add("hearingId", hearingId.toString())
                .add("caseId", caseId)
                .build();
        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("results.hearing-case-ejected"), payload);
        final HearingResultedDocument document = getHearingResultDocument(hearingId ,"/json/hearingResultDocument.json");
        when(this.hearingResultedDocumentRepository.findBy(hearingId)).thenReturn(document);
        resultsEventListener.hearingCaseEjected(envelope);
        verify(this.hearingResultedDocumentRepository, times(2)).save(this.hearingResultedDocumentArgumentCaptor.capture());
        assertThat(hearingResultedDocumentArgumentCaptor.getAllValues(), is(notNullValue()));
        assertThat(hearingResultedDocumentArgumentCaptor.getAllValues().size(), is(2));
        final HearingResultedDocument uddatedDocument = hearingResultedDocumentArgumentCaptor.getAllValues().get(0);
        final JsonNode hearingNode = mapper.valueToTree(JSONValue.parse(uddatedDocument.getPayload()));
        final ArrayNode caseArrayNode = (ArrayNode) hearingNode.path(HEARING).path(PROSECUTION_CASES);
        caseArrayNode.forEach(caseNode -> {
            if (caseNode.get("id").asText().equals(caseId)) {
                Assert.assertEquals("Check if the application status is ejected", "true",
                        caseNode.path("isEjected").asText());
            }
        });
    }

    @Test
    public void hearingCaseEjected_whenCaseIdAndLinkedCasedIdMatchesInPayload_expectIsEjectedFlagAddedInPayloadForCaseAndLinkedApplication() throws IOException {
        final ObjectMapper mapper = new ObjectMapperProducer().objectMapper();
        final String PROSECUTION_CASES = "prosecutionCases";
        final String COURT_APPLICATIONS = "courtApplications";
        final String HEARING = "hearing";
        final UUID hearingId = randomUUID();
        final String caseId = "cccc1111-1e20-4c21-916a-81a6c90239e5";
        final JsonObject payload = Json.createObjectBuilder()
                .add("hearingId", hearingId.toString())
                .add("caseId", caseId)
                .build();
        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("results.hearing-case-ejected"), payload);
        final HearingResultedDocument document = getHearingResultDocument(hearingId ,"/json/hearingResultDocumentWithLinkedCaseId.json");
        when(this.hearingResultedDocumentRepository.findBy(hearingId)).thenReturn(document);
        resultsEventListener.hearingCaseEjected(envelope);
        verify(this.hearingResultedDocumentRepository, times(2)).save(this.hearingResultedDocumentArgumentCaptor.capture());
        assertThat(hearingResultedDocumentArgumentCaptor.getAllValues(), is(notNullValue()));
        assertThat(hearingResultedDocumentArgumentCaptor.getAllValues().size(), is(2));
        final HearingResultedDocument uddatedDocument = hearingResultedDocumentArgumentCaptor.getAllValues().get(0);
        final JsonNode hearingNode = mapper.valueToTree(JSONValue.parse(uddatedDocument.getPayload()));
        final ArrayNode caseArrayNode = (ArrayNode) hearingNode.path(HEARING).path(PROSECUTION_CASES);
        caseArrayNode.forEach(caseNode -> {
            if (caseNode.get("id").asText().equals(caseId)) {
                Assert.assertEquals("Check if the case status is ejected", "true",
                        caseNode.path("isEjected").asText());
            }
        });
        final ArrayNode applicationArrayNode = (ArrayNode) hearingNode.path(HEARING).path(COURT_APPLICATIONS);
        applicationArrayNode.forEach(applicationNode -> {
            final JsonNode linkedCaseNode = applicationNode.path("linkedCaseId");
            if (!linkedCaseNode.isMissingNode() && linkedCaseNode.asText().equals(caseId)) {
                Assert.assertEquals("Check if the application status is ejected", "true",
                        applicationNode.path("isEjected").asText());
            }
        });
    }

    @Test
    public void hearingCaseEjected_whenApplicationIdMatchesInPayload_expectIsEjectedFlagAddedInPayloadForApplicationAndChildApplication() throws IOException {
        final ObjectMapper mapper = new ObjectMapperProducer().objectMapper();
        final String COURT_APPLICATIONS = "courtApplications";
        final String HEARING = "hearing";
        final UUID hearingId = randomUUID();
        final String applicationId = "79dbbf11-8108-4834-aff1-f24c3612fb69";
        final JsonObject payload = Json.createObjectBuilder()
                .add("hearingId", hearingId.toString())
                .add("applicationId", applicationId)
                .build();
        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("results.hearing-case-ejected"), payload);
        final HearingResultedDocument document = getHearingResultDocument(hearingId, "/json/hearingResultDocument.json");
        when(this.hearingResultedDocumentRepository.findBy(hearingId)).thenReturn(document);
        resultsEventListener.hearingApplicationEjected(envelope);
        verify(this.hearingResultedDocumentRepository, times(1)).save(this.hearingResultedDocumentArgumentCaptor.capture());
        assertThat(hearingResultedDocumentArgumentCaptor.getAllValues(), is(notNullValue()));
        assertThat(hearingResultedDocumentArgumentCaptor.getAllValues().size(), is(1));
        final HearingResultedDocument uddatedDocument = hearingResultedDocumentArgumentCaptor.getAllValues().get(0);
        final JsonNode hearingNode = mapper.valueToTree(JSONValue.parse(uddatedDocument.getPayload()));
        final ArrayNode applicationArrayNode = (ArrayNode) hearingNode.path(HEARING).path(COURT_APPLICATIONS);
        applicationArrayNode.forEach(applicationNode -> {
            if (applicationNode.get("id").asText().equals(applicationId) || applicationNode.get("parentApplicationId").asText().equals(applicationId)) {
                Assert.assertEquals("Check if the application status is ejected", "true",
                        applicationNode.path("isEjected").asText());
            }
        });
    }

    @Test
    public void hearingCaseEjected_whenApplicationIdMatchesInPayloadWithoutPrentApplication_expectIsEjectedFlagAddedInPayloadForParentApplicationOnly() throws IOException {
        final ObjectMapper mapper = new ObjectMapperProducer().objectMapper();
        final String COURT_APPLICATIONS = "courtApplications";
        final String HEARING = "hearing";
        final UUID hearingId = randomUUID();
        final String applicationId = "79dbbf11-8108-4834-aff1-f24c3612fb69";
        final JsonObject payload = Json.createObjectBuilder()
                .add("hearingId", hearingId.toString())
                .add("applicationId", applicationId)
                .build();
        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("results.hearing-case-ejected"), payload);
        final HearingResultedDocument document = getHearingResultDocument(hearingId, "/json/hearingResultDocumentWithoutParentApplication.json");
        when(this.hearingResultedDocumentRepository.findBy(hearingId)).thenReturn(document);
        resultsEventListener.hearingApplicationEjected(envelope);
        verify(this.hearingResultedDocumentRepository, times(1)).save(this.hearingResultedDocumentArgumentCaptor.capture());
        assertThat(hearingResultedDocumentArgumentCaptor.getAllValues(), is(notNullValue()));
        assertThat(hearingResultedDocumentArgumentCaptor.getAllValues().size(), is(1));
        final HearingResultedDocument uddatedDocument = hearingResultedDocumentArgumentCaptor.getAllValues().get(0);
        final JsonNode hearingNode = mapper.valueToTree(JSONValue.parse(uddatedDocument.getPayload()));
        final ArrayNode applicationArrayNode = (ArrayNode) hearingNode.path(HEARING).path(COURT_APPLICATIONS);
        applicationArrayNode.forEach(applicationNode -> {
            if (applicationNode.get("id").asText().equals(applicationId)) {
                Assert.assertEquals("Check if the application status is ejected", "true",
                        applicationNode.path("isEjected").asText());
            }
        });
    }

    private HearingResultedDocument getHearingResultDocument(final UUID hearingId, final String payloadPath) throws IOException {
        HearingResultedDocument hearingResultedDocument = new HearingResultedDocument();
        hearingResultedDocument.setHearingId(hearingId);
        hearingResultedDocument.setPayload(createPayload(payloadPath));
        return hearingResultedDocument;
    }

    private String createPayload(final String payloadPath) throws IOException {
        final StringWriter writer = new StringWriter();
        InputStream inputStream = ResultsEventListenerTest.class.getResourceAsStream(payloadPath);
        IOUtils.copy(inputStream, writer, UTF_8);
        inputStream.close();
        return writer.toString();

    }

}
