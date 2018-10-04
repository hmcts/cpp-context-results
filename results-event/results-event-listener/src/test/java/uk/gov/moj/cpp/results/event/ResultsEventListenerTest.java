package uk.gov.moj.cpp.results.event;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.common.reflection.ReflectionUtils.setField;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.json.schemas.core.HearingDay;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.domains.results.shareresults.PublicHearingResulted;
import uk.gov.moj.cpp.results.persist.HearingResultedDocumentRepository;
import uk.gov.moj.cpp.results.persist.entity.HearingResultedDocument;
import uk.gov.moj.cpp.results.test.TestTemplates;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;


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

}
