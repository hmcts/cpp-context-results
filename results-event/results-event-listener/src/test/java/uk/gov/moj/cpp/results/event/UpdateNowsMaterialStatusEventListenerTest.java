package uk.gov.moj.cpp.results.event;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.common.reflection.ReflectionUtils.setField;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

import java.io.StringReader;
import java.time.LocalDate;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import uk.gov.justice.json.schemas.core.Key;
import uk.gov.justice.json.schemas.core.SharedVariant;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.domains.results.shareresults.PublicHearingResulted;
import uk.gov.moj.cpp.results.domain.event.NowsMaterialStatusUpdated;
import uk.gov.moj.cpp.results.persist.HearingResultedDocumentRepository;
import uk.gov.moj.cpp.results.persist.entity.HearingResultedDocument;
import uk.gov.moj.cpp.results.test.TestTemplates;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

@RunWith(MockitoJUnitRunner.class)
public class UpdateNowsMaterialStatusEventListenerTest {

    private static final String GENERATED = "GENERATED";

    @InjectMocks
    private UpdateNowsMaterialStatusEventListener nowsGeneratedEventListener;

    @Mock
    private HearingResultedDocumentRepository hearingResultedDocumentRepository;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Before
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void updateNowsMaterialStatusToGeneratedFromPendingForValidMaterialId() throws Exception {

        HearingResultedDocument document = new HearingResultedDocument();
        PublicHearingResulted shareResultsMessage = TestTemplates.basicShareResultsTemplate();
        shareResultsMessage.setVariants(Arrays.asList(SharedVariant.sharedVariant()
                .withKey(Key.key()
                        .withHearingId(shareResultsMessage.getHearing().getId())
                        .withDefendantId(randomUUID())
                        .withNowsTypeId(randomUUID())
                        .withUsergroups(Arrays.asList(STRING.next()))
                        .build())
                .withMaterialId(randomUUID())
                .withDescription(STRING.next())
                .withTemplateName(STRING.next())
                .withStatus("pending")
                .build()));
        document.setHearingId(shareResultsMessage.getHearing().getId());
        document.setStartDate(LocalDate.of(2018, 01, 01));
        document.setEndDate(LocalDate.of(2018, 02, 02));
        document.setPayload(objectToJsonObjectConverter.convert(shareResultsMessage).toString());

        final NowsMaterialStatusUpdated nowsMaterialStatusUpdated = new NowsMaterialStatusUpdated(shareResultsMessage.getHearing().getId(), shareResultsMessage.getVariants().get(0).getMaterialId(), "generated");

        when(hearingResultedDocumentRepository.findBy(shareResultsMessage.getHearing().getId())).thenReturn(document);
        when(hearingResultedDocumentRepository.save(any(HearingResultedDocument.class))).thenReturn(document);

        nowsGeneratedEventListener.onNowsMaterialStatusUpdated(envelopeFrom(metadataWithRandomUUID("results.event.nows-material-status-updated"),
        objectToJsonObjectConverter.convert(nowsMaterialStatusUpdated)));

        final ArgumentCaptor<HearingResultedDocument> hearingResultedDocumentArgumentCaptor = ArgumentCaptor.forClass(HearingResultedDocument.class);

        verify(this.hearingResultedDocumentRepository, times(1)).save(hearingResultedDocumentArgumentCaptor.capture());

        JsonObject object = jsonFromString(hearingResultedDocumentArgumentCaptor.getAllValues().get(0).getPayload());
        PublicHearingResulted responsePayload = jsonObjectToObjectConverter.convert(object, PublicHearingResulted.class);

        assertThat(hearingResultedDocumentArgumentCaptor.getAllValues(), is(notNullValue()));
        assertThat(hearingResultedDocumentArgumentCaptor.getAllValues().size(), is(1));
        assertThat(hearingResultedDocumentArgumentCaptor.getAllValues().get(0).getHearingId(), is(shareResultsMessage.getHearing().getId()));
        assertThat(hearingResultedDocumentArgumentCaptor.getAllValues().get(0).getStartDate(), is(LocalDate.of(2018, 01, 01)));
        assertThat(hearingResultedDocumentArgumentCaptor.getAllValues().get(0).getEndDate(), is(LocalDate.of(2018, 02, 02)));
        assertThat(responsePayload.getVariants().get(0).getStatus(), is("generated"));
    }

    @Test
    public void shouldNotUpdateNowsMaterialStatusForInvalidMaterialId() throws Exception {

        HearingResultedDocument document = new HearingResultedDocument();
        PublicHearingResulted shareResultsMessage = TestTemplates.basicShareResultsTemplate();
        shareResultsMessage.setVariants(Arrays.asList(SharedVariant.sharedVariant()
                .withKey(Key.key()
                        .withHearingId(shareResultsMessage.getHearing().getId())
                        .withDefendantId(randomUUID())
                        .withNowsTypeId(randomUUID())
                        .withUsergroups(Arrays.asList(STRING.next()))
                        .build())
                .withMaterialId(randomUUID())
                .withDescription(STRING.next())
                .withTemplateName(STRING.next())
                .withStatus("pending")
                .build()));
        document.setHearingId(shareResultsMessage.getHearing().getId());
        document.setStartDate(LocalDate.of(2018, 01, 01));
        document.setEndDate(LocalDate.of(2018, 02, 02));
        document.setPayload(objectToJsonObjectConverter.convert(shareResultsMessage).toString());

        final NowsMaterialStatusUpdated nowsMaterialStatusUpdated = new NowsMaterialStatusUpdated(shareResultsMessage.getHearing().getId(), randomUUID(), "generated");

        when(hearingResultedDocumentRepository.findBy(shareResultsMessage.getHearing().getId())).thenReturn(document);
        when(hearingResultedDocumentRepository.save(any(HearingResultedDocument.class))).thenReturn(document);

        nowsGeneratedEventListener.onNowsMaterialStatusUpdated(envelopeFrom(metadataWithRandomUUID("results.event.nows-material-status-updated"),
                objectToJsonObjectConverter.convert(nowsMaterialStatusUpdated)));

        final ArgumentCaptor<HearingResultedDocument> hearingResultedDocumentArgumentCaptor = ArgumentCaptor.forClass(HearingResultedDocument.class);

        verify(this.hearingResultedDocumentRepository, times(1)).save(hearingResultedDocumentArgumentCaptor.capture());

        JsonObject object = jsonFromString(hearingResultedDocumentArgumentCaptor.getAllValues().get(0).getPayload());
        PublicHearingResulted responsePayload = jsonObjectToObjectConverter.convert(object, PublicHearingResulted.class);

        assertThat(hearingResultedDocumentArgumentCaptor.getAllValues(), is(notNullValue()));
        assertThat(hearingResultedDocumentArgumentCaptor.getAllValues().size(), is(1));
        assertThat(hearingResultedDocumentArgumentCaptor.getAllValues().get(0).getHearingId(), is(shareResultsMessage.getHearing().getId()));
        assertThat(hearingResultedDocumentArgumentCaptor.getAllValues().get(0).getStartDate(), is(LocalDate.of(2018, 01, 01)));
        assertThat(hearingResultedDocumentArgumentCaptor.getAllValues().get(0).getEndDate(), is(LocalDate.of(2018, 02, 02)));
        assertThat(responsePayload.getVariants().get(0).getStatus(), is("pending"));
    }

    private static JsonObject jsonFromString(String payload) {
        JsonReader jsonReader = Json.createReader(new StringReader(payload));
        JsonObject object = jsonReader.readObject();
        jsonReader.close();
        return object;
    }

}