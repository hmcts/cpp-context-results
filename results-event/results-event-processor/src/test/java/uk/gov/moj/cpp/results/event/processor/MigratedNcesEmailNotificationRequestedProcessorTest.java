package uk.gov.moj.cpp.results.event.processor;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonMetadata.ID;
import static uk.gov.justice.services.messaging.JsonMetadata.NAME;
import static uk.gov.justice.services.messaging.JsonMetadata.USER_ID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.results.event.helper.Originator;
import uk.gov.moj.cpp.results.event.helper.ReferenceCache;
import uk.gov.moj.cpp.results.event.service.DocumentGeneratorService;
import uk.gov.moj.cpp.results.event.service.FileParams;
import uk.gov.moj.cpp.results.event.service.ReferenceDataService;

import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MigratedNcesEmailNotificationRequestedProcessorTest {

    private static final String MATERIAL_ID = "materialId";
    private static final String CASE_ID = "caseId";
    private static final String MASTER_DEFENDANT_ID = "masterDefendantId";
    private static final String PROGRESSION_ADD_COURT_DOCUMENT = "progression.add-court-document";
    private static final String DOCUMENT_TYPE_DESCRIPTION = "Electronic Notifications";
    private static final String APPLICATION_PDF = "application/pdf";

    @Mock
    private ReferenceDataService referenceDataService;

    @Mock
    private ReferenceCache referenceCache;

    @Mock
    private Sender sender;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private DocumentGeneratorService documentGeneratorService;

    @InjectMocks
    private MigratedNcesEmailNotificationRequestedProcessor processor;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeCaptor;

    @Captor
    private ArgumentCaptor<JsonEnvelope> jsonEnvelopeCaptor;

    @BeforeEach
    public void setUp() {
        setField(objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldHandleMigratedNcesEmailNotificationRequested() {
        // Given
        final UUID userId = randomUUID();
        final UUID caseUUID = randomUUID();
        final UUID masterDefendantUUID = randomUUID();
        final UUID materialId = randomUUID();
        final UUID fileId = randomUUID();
        final String fileName = "test-document.pdf";
        final String expectedOriginator = Originator.ORIGINATOR_VALUE_NCES_CASEID + masterDefendantUUID.toString() + "-" + caseUUID.toString();

        final String subject = "APPEAL APPLICATION RECEIVED";
        final String finAccountNumber = "12345";
        final String divisionCode = "6";
        final String legacyCaseReference = "N/A";
        final String caseReferences = "caseUrn1";
        final String originalDateOfConviction = "2025-11-09";
        final String listedDate = "2019-12-01";
        final String hearingCourtCentreName = "South West London Magistrates Court";
        final String defendantName = "Garfield Dare";
        final String defendantDateOfBirth = "1998-08-23";
        final String defendantAddress = "59 Meadow Lane Huddersfield";
        final String defendantEmail = "test@gmail.com";
        final String defendantContactNumber = "07431234511";

        final Metadata metadata = metadataFrom(createObjectBuilder()
                .add(ID, randomUUID().toString())
                .add(NAME, "results.event.migrated-inactive-nces-email-notification-requested")
                .add("context", createObjectBuilder()
                        .add(USER_ID, userId.toString()))
                .build()).build();

        final JsonEnvelope jsonEnvelope = envelopeFrom(metadata,
                createObjectBuilder()
                        .add(CASE_ID, caseUUID.toString())
                        .add(MASTER_DEFENDANT_ID, masterDefendantUUID.toString())
                        .add(MATERIAL_ID, materialId.toString())
                        .add("subject", subject)
                        .add("finAccountNumber", finAccountNumber)
                        .add("divisionCode", divisionCode)
                        .add("legacyCaseReference", legacyCaseReference)
                        .add("caseReferences", caseReferences)
                        .add("originalDateOfConviction", originalDateOfConviction)
                        .add("listedDate", listedDate)
                        .add("hearingCourtCentreName", hearingCourtCentreName)
                        .add("defendantName", defendantName)
                        .add("defendantDateOfBirth", defendantDateOfBirth)
                        .add("defendantAddress", defendantAddress)
                        .add("defendantEmail", defendantEmail)
                        .add("defendantContactNumber", defendantContactNumber)
                        .build());

        final FileParams fileParams = new FileParams(fileId, fileName);
        when(documentGeneratorService.generateNcesDocument(any(), any(JsonEnvelope.class), eq(userId), eq(materialId), eq(expectedOriginator)))
                .thenReturn(fileParams);

        // When
        processor.handleMigratedNcesEmailNotificationRequested(jsonEnvelope);

        // Then
        verify(documentGeneratorService, times(1)).generateNcesDocument(
                any(Sender.class), jsonEnvelopeCaptor.capture(), eq(userId), eq(materialId), eq(expectedOriginator));
        verify(sender, times(1)).send(envelopeCaptor.capture());

        // Verify transformed envelope
        final JsonEnvelope capturedEnvelope = jsonEnvelopeCaptor.getValue();
        final JsonObject transformedPayload = capturedEnvelope.payloadAsJsonObject();

        assertThat(transformedPayload.getString("subject"), is(subject));
        assertThat(transformedPayload.getString("fineAccountNumber"), is(finAccountNumber));
        assertThat(transformedPayload.getString("divisionCode"), is(divisionCode));
        assertThat(transformedPayload.getString("legacyCaseReference"), is(legacyCaseReference));
        assertThat(transformedPayload.getString("caseReferences"), is(caseReferences));
        assertThat(transformedPayload.getString("dateOfConviction"), is(originalDateOfConviction));
        assertThat(transformedPayload.getString("listedDate"), is(listedDate));
        assertThat(transformedPayload.getString("hearingCourtCentreName"), is(hearingCourtCentreName));
        assertThat(transformedPayload.getString("defendantName"), is(defendantName));
        assertThat(transformedPayload.getString("defendantDateOfBirth"), is(defendantDateOfBirth));
        assertThat(transformedPayload.getString("defendantAddress"), is(defendantAddress));
        assertThat(transformedPayload.getString("defendantEmail"), is(defendantEmail));
        assertThat(transformedPayload.getString("defendantContactNumber"), is(defendantContactNumber));

        // Verify removed fields are not present
        assertThat(transformedPayload.containsKey("finAccountNumber"), is(false));
        assertThat(transformedPayload.containsKey("originalDateOfConviction"), is(false));
        assertThat(transformedPayload.containsKey("caseId"), is(false));
        assertThat(transformedPayload.containsKey("masterDefendantId"), is(false));
        assertThat(transformedPayload.containsKey("materialId"), is(false));

        final Envelope<JsonObject> sentEnvelope = envelopeCaptor.getValue();
        assertThat(sentEnvelope.metadata().name(), is(PROGRESSION_ADD_COURT_DOCUMENT));
        
        final JsonObject payload = sentEnvelope.payload();
        assertThat(payload.getString(MATERIAL_ID), is(materialId.toString()));
        assertThat(payload.containsKey("courtDocument"), is(true));
    }

    @Test
    public void shouldThrowExceptionWhenUserIdIsMissing() {
        // Given
        final UUID caseUUID = randomUUID();
        final UUID masterDefendantUUID = randomUUID();
        final UUID materialId = randomUUID();

        final Metadata metadata = metadataFrom(createObjectBuilder()
                .add(ID, randomUUID().toString())
                .add(NAME, "results.event.migrated-inactive-nces-email-notification-requested")
                .build()).build();

        final JsonEnvelope jsonEnvelope = envelopeFrom(metadata,
                createObjectBuilder()
                        .add(CASE_ID, caseUUID.toString())
                        .add(MASTER_DEFENDANT_ID, masterDefendantUUID.toString())
                        .add(MATERIAL_ID, materialId.toString())
                        .build());

        // When & Then
        assertThrows(RuntimeException.class, () -> 
                processor.handleMigratedNcesEmailNotificationRequested(jsonEnvelope),
                "UserId missing from event.");
    }

    @Test
    public void shouldBuildCourtDocumentWithCorrectProperties() {
        // Given
        final UUID userId = randomUUID();
        final UUID caseUUID = randomUUID();
        final UUID masterDefendantUUID = randomUUID();
        final UUID materialId = randomUUID();
        final UUID fileId = randomUUID();
        final String fileName = "test-document.pdf";
        final String expectedOriginator = Originator.ORIGINATOR_VALUE_NCES_CASEID + masterDefendantUUID.toString() + "-" + caseUUID.toString();

        final Metadata metadata = metadataFrom(createObjectBuilder()
                .add(ID, randomUUID().toString())
                .add(NAME, "results.event.migrated-inactive-nces-email-notification-requested")
                .add("context", createObjectBuilder()
                        .add(USER_ID, userId.toString()))
                .build()).build();

        final JsonEnvelope jsonEnvelope = envelopeFrom(metadata,
                createObjectBuilder()
                        .add(CASE_ID, caseUUID.toString())
                        .add(MASTER_DEFENDANT_ID, masterDefendantUUID.toString())
                        .add(MATERIAL_ID, materialId.toString())
                        .build());

        final FileParams fileParams = new FileParams(fileId, fileName);
        when(documentGeneratorService.generateNcesDocument(any(), any(JsonEnvelope.class), eq(userId), eq(materialId), eq(expectedOriginator)))
                .thenReturn(fileParams);

        // When
        processor.handleMigratedNcesEmailNotificationRequested(jsonEnvelope);

        // Then
        verify(sender, times(1)).send(envelopeCaptor.capture());
        final Envelope<JsonObject> sentEnvelope = envelopeCaptor.getValue();
        final JsonObject payload = sentEnvelope.payload();
        final JsonObject courtDocumentJson = payload.getJsonObject("courtDocument");
        
        assertThat(courtDocumentJson.getString("documentTypeDescription"), is(DOCUMENT_TYPE_DESCRIPTION));
        assertThat(courtDocumentJson.getString("mimeType"), is(APPLICATION_PDF));
        assertThat(courtDocumentJson.getString("name"), is(fileName));
        assertThat(courtDocumentJson.getBoolean("sendToCps"), is(false));
        assertThat(courtDocumentJson.getBoolean("containsFinancialMeans"), is(false));
        assertThat(courtDocumentJson.containsKey("courtDocumentId"), is(true));
        assertThat(courtDocumentJson.containsKey("documentCategory"), is(true));
        assertThat(courtDocumentJson.containsKey("materials"), is(true));
    }

    @Test
    public void shouldCreateCorrectRootAggregateId() {
        // Given
        final UUID userId = randomUUID();
        final UUID caseUUID = randomUUID();
        final UUID masterDefendantUUID = randomUUID();
        final UUID materialId = randomUUID();
        final UUID fileId = randomUUID();
        final String fileName = "test-document.pdf";
        final String expectedRootAggregateId = masterDefendantUUID.toString() + "-" + caseUUID.toString();
        final String expectedOriginator = Originator.ORIGINATOR_VALUE_NCES_CASEID + expectedRootAggregateId;

        final Metadata metadata = metadataFrom(createObjectBuilder()
                .add(ID, randomUUID().toString())
                .add(NAME, "results.event.migrated-inactive-nces-email-notification-requested")
                .add("context", createObjectBuilder()
                        .add(USER_ID, userId.toString()))
                .build()).build();

        final JsonEnvelope jsonEnvelope = envelopeFrom(metadata,
                createObjectBuilder()
                        .add(CASE_ID, caseUUID.toString())
                        .add(MASTER_DEFENDANT_ID, masterDefendantUUID.toString())
                        .add(MATERIAL_ID, materialId.toString())
                        .build());

        final FileParams fileParams = new FileParams(fileId, fileName);
        when(documentGeneratorService.generateNcesDocument(any(), any(JsonEnvelope.class), eq(userId), eq(materialId), eq(expectedOriginator)))
                .thenReturn(fileParams);

        // When
        processor.handleMigratedNcesEmailNotificationRequested(jsonEnvelope);

        // Then
        verify(documentGeneratorService, times(1)).generateNcesDocument(
                any(Sender.class), any(JsonEnvelope.class), eq(userId), eq(materialId), eq(expectedOriginator));
    }
}
