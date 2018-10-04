package uk.gov.moj.cpp.results.domain.aggregate;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.domains.results.shareresults.PublicHearingResulted;
import uk.gov.moj.cpp.results.test.TestTemplates;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static uk.gov.justice.services.test.utils.common.reflection.ReflectionUtils.setField;

@RunWith(MockitoJUnitRunner.class)
public class ResultsAggregateTest {

    @InjectMocks
    private ResultsAggregate resultsAggregate;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    private static PublicHearingResulted shareResultsMessage;

    @BeforeClass
    public static void init() {
        shareResultsMessage = TestTemplates.basicShareResultsTemplate();
    }

    @Before
    public void setup() {
        setField(this.objectToJsonObjectConverter, "mapper",  new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void testSaveShareResults_shouldRaiseHearingResultsAddedEvent() {

        /*final HearingResultsAdded hearingResultsAdded = resultsAggregate.saveHearingResults(objectToJsonObjectConverter.convert(shareResultsMessage))
                .map(o -> (HearingResultsAdded) o)
                .findFirst()
                .orElse(null);

        assertNotNull(hearingResultsAdded);

        assertEquals(objectToJsonObjectConverter.convert(shareResultsMessage.getHearing()), hearingResultsAdded.getHearing());

        assertEquals(shareResultsMessage.getSharedTime().toLocalDateTime(), hearingResultsAdded.getSharedTime().toLocalDateTime());*/
    }
}