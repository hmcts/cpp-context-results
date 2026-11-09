package uk.gov.moj.cpp.results.event;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.results.domain.event.DocumentDeliveryRecorded;
import uk.gov.moj.cpp.results.domain.event.SjpCases;
import uk.gov.moj.cpp.results.persist.ResultsDocumentDeliveryRepository;
import uk.gov.moj.cpp.results.persist.entity.NcesSjpCase;
import uk.gov.moj.cpp.results.persist.entity.ResultsDocumentDeliveryEntity;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

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
class DocumentDeliveryRecordedListenerTest {

    private static final String DOCUMENT_BLOB_URI = "http://localhost/results/sdg/documents/ENFDocumentOrder_1.pdf";
    private static final String PAYLOAD_BLOB_URI = "http://localhost/results/sdg/requests/POLICE_NOTIFICATION_HEARING_RESULTS1.html";

    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter();

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private ResultsDocumentDeliveryRepository resultsDocumentDeliveryRepository;

    @InjectMocks
    private DocumentDeliveryRecordedListener documentDeliveryRecordedListener;

    @Captor
    private ArgumentCaptor<ResultsDocumentDeliveryEntity> deliveryCaptor;

    private final UUID materialId = randomUUID();
    private final UUID firstCaseId = randomUUID();
    private final UUID secondCaseId = randomUUID();

    @BeforeEach
    void setUp() {
        setField(objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    void shouldCreateTheRecordWithEverySjpCaseTheDocumentWasSentTo() {
        documentDeliveryRecordedListener.handleDocumentDeliveryRecorded(event(DocumentDeliveryRecorded.documentDeliveryRecorded()
                .withMaterialId(materialId)
                .withDocumentType("NCES")
                .withMaterialStatus("PENDING")
                .withDocumentBlobUri(DOCUMENT_BLOB_URI)
                .withSjpCases(List.of(new SjpCases(firstCaseId, "first-document-id", "PENDING"),
                        new SjpCases(secondCaseId, "second-document-id", "PENDING")))
                .build()));

        verify(resultsDocumentDeliveryRepository).save(deliveryCaptor.capture());
        final ResultsDocumentDeliveryEntity delivery = deliveryCaptor.getValue();
        assertThat(delivery.getMaterialId(), is(materialId));
        assertThat(delivery.getDocumentType(), is("NCES"));
        assertThat(delivery.getMaterialStatus(), is("PENDING"));
        assertThat(delivery.getDocumentBlobUri(), is(DOCUMENT_BLOB_URI));
        assertThat(delivery.getCreatedAt(), is(notNullValue()));
        assertThat(delivery.getSjpCases().stream().map(NcesSjpCase::getCaseId).toList(), contains(firstCaseId, secondCaseId));
        assertThat(delivery.getSjpCases().stream().map(NcesSjpCase::getSjpCorrelationId).toList(), contains("first-document-id", "second-document-id"));
    }

    @Test
    void shouldCreateAPoliceNotificationRecordWithBothBlobUris() {
        documentDeliveryRecordedListener.handleDocumentDeliveryRecorded(event(DocumentDeliveryRecorded.documentDeliveryRecorded()
                .withMaterialId(materialId)
                .withDocumentType("POLICE_NOTIFICATION")
                .withMaterialStatus("PENDING")
                .withPayloadBlobUri(PAYLOAD_BLOB_URI)
                .withDocumentBlobUri(DOCUMENT_BLOB_URI)
                .build()));

        verify(resultsDocumentDeliveryRepository).save(deliveryCaptor.capture());
        assertThat(deliveryCaptor.getValue().getPayloadBlobUri(), is(PAYLOAD_BLOB_URI));
        assertThat(deliveryCaptor.getValue().getDocumentBlobUri(), is(DOCUMENT_BLOB_URI));
        assertThat(deliveryCaptor.getValue().getSjpCases().isEmpty(), is(true));
    }

    @Test
    void shouldUpdateOnlyTheFieldsTheEventCarries() {
        final ResultsDocumentDeliveryEntity existing = existingNcesDelivery();
        when(resultsDocumentDeliveryRepository.findBy(materialId)).thenReturn(existing);

        documentDeliveryRecordedListener.handleDocumentDeliveryRecorded(event(DocumentDeliveryRecorded.documentDeliveryRecorded()
                .withMaterialId(materialId)
                .withEmailStatus("SUCCESS")
                .build()));

        verify(resultsDocumentDeliveryRepository).save(existing);
        assertThat(existing.getEmailStatus(), is("SUCCESS"));
        assertThat(existing.getMaterialStatus(), is("SUCCESS"));
        assertThat(existing.getDocumentBlobUri(), is(DOCUMENT_BLOB_URI));
        assertThat(existing.getSjpCases().stream().map(NcesSjpCase::getSjpStatus).toList(), contains("PENDING", "PENDING"));
    }

    @Test
    void shouldUpdateOnlyTheSjpCasesTheEventNamesAndIgnoreOnesNotOnTheRecord() {
        final ResultsDocumentDeliveryEntity existing = existingNcesDelivery();
        when(resultsDocumentDeliveryRepository.findBy(materialId)).thenReturn(existing);

        documentDeliveryRecordedListener.handleDocumentDeliveryRecorded(event(DocumentDeliveryRecorded.documentDeliveryRecorded()
                .withMaterialId(materialId)
                .withSjpCases(List.of(new SjpCases(secondCaseId, null, "FAILED"), new SjpCases(randomUUID(), null, "SUCCESS")))
                .build()));

        verify(resultsDocumentDeliveryRepository).save(existing);
        assertThat(existing.getSjpCases().stream().map(NcesSjpCase::getCaseId).toList(), contains(firstCaseId, secondCaseId));
        assertThat(existing.getSjpCases().stream().map(NcesSjpCase::getSjpStatus).toList(), contains("PENDING", "FAILED"));
        assertThat(existing.getSjpCases().get(1).getSjpCorrelationId(), is("second-document-id"));
    }

    @Test
    void shouldNotCreateARecordWithoutADocumentType() {
        documentDeliveryRecordedListener.handleDocumentDeliveryRecorded(event(DocumentDeliveryRecorded.documentDeliveryRecorded()
                .withMaterialId(materialId)
                .withMaterialStatus("SUCCESS")
                .build()));

        verify(resultsDocumentDeliveryRepository, never()).save(any());
    }

    private ResultsDocumentDeliveryEntity existingNcesDelivery() {
        final ResultsDocumentDeliveryEntity delivery = new ResultsDocumentDeliveryEntity(materialId, "NCES", ZonedDateTime.now(), null, DOCUMENT_BLOB_URI);
        delivery.setMaterialStatus("SUCCESS");
        delivery.getSjpCases().add(new NcesSjpCase(firstCaseId, "first-document-id", "PENDING"));
        delivery.getSjpCases().add(new NcesSjpCase(secondCaseId, "second-document-id", "PENDING"));
        return delivery;
    }

    private JsonEnvelope event(final DocumentDeliveryRecorded recorded) {
        return envelopeFrom(metadataWithRandomUUID("results.event.document-delivery-recorded"), objectToJsonObjectConverter.convert(recorded));
    }
}
