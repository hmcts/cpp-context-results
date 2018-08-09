package uk.gov.moj.cpp.results.event;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.domains.results.shareResults.ShareResultsMessage;
import uk.gov.moj.cpp.results.domain.event.HearingResultsAdded;
import uk.gov.moj.cpp.results.persist.HearingRepository;
import uk.gov.moj.cpp.results.persist.HearingResultRepository;
import uk.gov.moj.cpp.results.persist.DefendantRepository;
import uk.gov.moj.cpp.results.persist.VariantDirectoryRepository;
import uk.gov.moj.cpp.results.persist.entity.Hearing;
import uk.gov.moj.cpp.results.persist.entity.HearingResult;
import uk.gov.moj.cpp.results.persist.entity.Defendant;
import uk.gov.moj.cpp.results.persist.entity.VariantDirectory;
import uk.gov.moj.cpp.results.test.TestTemplates;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.common.reflection.ReflectionUtils.setField;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloperWithEvents;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import java.util.Arrays;
import java.util.List;


@RunWith(MockitoJUnitRunner.class)
public class ResultsEventListenerTest {

    @InjectMocks
    private ResultsEventListener resultsEventListener;

    @Mock
    private HearingResultRepository hearingResultRepository;

    @Mock
    private HearingRepository hearingRepository;

    @Mock
    private DefendantRepository defendantRepository;

    @Mock
    private VariantDirectoryRepository variantDirectoryRepository;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Captor
    private ArgumentCaptor<HearingResult> hearingResultAddedArgumentCaptor;

    @Captor
    private ArgumentCaptor<Hearing> hearingAddedArgumentCaptor;

    @Captor
    private ArgumentCaptor<Defendant> personAddedArgumentCaptor;

    @Captor
    private ArgumentCaptor<VariantDirectory> variantDirectoryArgumentCaptor;

    @Spy
    private final Enveloper enveloper = createEnveloperWithEvents(HearingResultsAdded.class);

    private static ShareResultsMessage shareResultsMessage;

    @BeforeClass
    public static void init() {
        shareResultsMessage = TestTemplates.basicShareResultsTemplate();
    }

    @Before
    public void setup() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void should_saveHearingResultAdded() {

        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("results.hearing-results-added"),
                objectToJsonObjectConverter.convert(shareResultsMessage));

        resultsEventListener.hearingResultsAdded(envelope);

        verify(this.defendantRepository, times(shareResultsMessage.getHearing().getDefendants().size())).saveAndFlush(this.personAddedArgumentCaptor.capture());
        verify(this.hearingRepository, times(1)).saveAndFlush(this.hearingAddedArgumentCaptor.capture());
        verify(this.hearingResultRepository, times(shareResultsMessage.getHearing().getSharedResultLines().size())).saveAndFlush(this.hearingResultAddedArgumentCaptor.capture());
        verify(this.variantDirectoryRepository, times(1)).saveAndFlush(this.variantDirectoryArgumentCaptor.capture());

        assertThat(personAddedArgumentCaptor.getAllValues(), is(notNullValue()));
        assertThat(personAddedArgumentCaptor.getAllValues().size(), is(shareResultsMessage.getHearing().getDefendants().size()));

        assertThat(hearingAddedArgumentCaptor.getAllValues(), is(notNullValue()));
        assertThat(hearingAddedArgumentCaptor.getAllValues().size(), is(shareResultsMessage.getHearing().getDefendants().size()));

        assertThat(hearingResultAddedArgumentCaptor.getAllValues(), is(notNullValue()));
        assertThat(hearingResultAddedArgumentCaptor.getAllValues().size(), is(shareResultsMessage.getHearing().getSharedResultLines().size()));

        assertThat(variantDirectoryArgumentCaptor.getAllValues().size(), is(shareResultsMessage.getVariants().size()));

    }

    @Test
    public void should_saveHearingResultAddedAndRemoveAllPreviousSavedNowsMaterials() {

        shareResultsMessage.getVariants().clear();

        final VariantDirectory previousNowMaterial = new VariantDirectory(randomUUID(), randomUUID(), randomUUID(), randomUUID(), randomUUID(), Arrays.asList(STRING.next()), randomUUID(), STRING.next(), STRING.next(), "GENERATED");
        final List<VariantDirectory> previousNowsMaterials = Arrays.asList(previousNowMaterial);

        when(variantDirectoryRepository.findByHearingId(shareResultsMessage.getHearing().getId())).thenReturn(previousNowsMaterials);

        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("results.hearing-results-added"),
                objectToJsonObjectConverter.convert(shareResultsMessage));

        resultsEventListener.hearingResultsAdded(envelope);

        verify(this.defendantRepository, times(shareResultsMessage.getHearing().getDefendants().size())).saveAndFlush(this.personAddedArgumentCaptor.capture());
        verify(this.hearingRepository, times(1)).saveAndFlush(this.hearingAddedArgumentCaptor.capture());
        verify(this.hearingResultRepository, times(shareResultsMessage.getHearing().getSharedResultLines().size())).saveAndFlush(this.hearingResultAddedArgumentCaptor.capture());
        verify(this.variantDirectoryRepository, times(1)).remove(this.variantDirectoryArgumentCaptor.capture());

        assertThat(personAddedArgumentCaptor.getAllValues(), is(notNullValue()));
        assertThat(personAddedArgumentCaptor.getAllValues().size(), is(shareResultsMessage.getHearing().getDefendants().size()));

        assertThat(hearingAddedArgumentCaptor.getAllValues(), is(notNullValue()));
        assertThat(hearingAddedArgumentCaptor.getAllValues().size(), is(shareResultsMessage.getHearing().getDefendants().size()));

        assertThat(hearingResultAddedArgumentCaptor.getAllValues(), is(notNullValue()));
        assertThat(hearingResultAddedArgumentCaptor.getAllValues().size(), is(shareResultsMessage.getHearing().getSharedResultLines().size()));

        assertThat(variantDirectoryArgumentCaptor.getAllValues().size(), is(previousNowsMaterials.size()));
        assertThat(variantDirectoryArgumentCaptor.getValue(), is(previousNowMaterial));

    }
}