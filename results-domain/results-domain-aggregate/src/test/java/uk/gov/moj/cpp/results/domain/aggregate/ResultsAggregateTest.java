package uk.gov.moj.cpp.results.domain.aggregate;

import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingResultsAdded;
import uk.gov.moj.cpp.domains.results.shareresults.PublicHearingResulted;

import java.time.ZonedDateTime;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class ResultsAggregateTest {

    @InjectMocks
    private ResultsAggregate resultsAggregate;

    private final PublicHearingResulted input = PublicHearingResulted.publicHearingResulted()
            .setHearing(Hearing.hearing()
                    .withId(UUID.randomUUID())
                    .build())
            .setSharedTime(ZonedDateTime.now());

    @Test
    public void testSaveShareResults_shouldRaiseHearingResultsAddedEvent() {
        final HearingResultsAdded hearingResultsAdded = resultsAggregate.saveHearingResults(input)
                .map(o -> (HearingResultsAdded) o)
                .findFirst()
                .orElse(null);

        assertNotNull(hearingResultsAdded);
        assertEquals(input.getHearing(), hearingResultsAdded.getHearing());
        assertEquals(input.getSharedTime(), hearingResultsAdded.getSharedTime());
    }
}