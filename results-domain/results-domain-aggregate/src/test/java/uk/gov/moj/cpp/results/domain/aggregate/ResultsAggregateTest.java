package uk.gov.moj.cpp.results.domain.aggregate;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import uk.gov.justice.core.courts.SharedHearing;
import uk.gov.justice.core.courts.SharedVariant;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.domains.results.shareresults.PublicHearingResulted;
import uk.gov.moj.cpp.results.domain.event.HearingResultsAdded;
import uk.gov.moj.cpp.results.domain.event.NowsMaterialStatusUpdated;
import uk.gov.moj.cpp.results.test.TestTemplates;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static uk.gov.justice.services.test.utils.common.reflection.ReflectionUtils.setField;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.UUID;

@RunWith(MockitoJUnitRunner.class)
public class ResultsAggregateTest {

    @InjectMocks
    private ResultsAggregate resultsAggregate;

    private final PublicHearingResulted input = PublicHearingResulted.publicHearingResulted()
            .setVariants(Arrays.asList(
                    SharedVariant.sharedVariant()
                            .withMaterialId(UUID.randomUUID())
                            .withStatus("processing").build()
            ))
            .setHearing(SharedHearing.sharedHearing()
                    .withId(UUID.randomUUID())
                    .build())
            .setSharedTime(ZonedDateTime.now())
            ;

    @Test
    public void testStatusUpdateInOrder() {
        final UUID hearingId = UUID.randomUUID();
        final UUID materialId = UUID.randomUUID();
        final String status = "processing";
        resultsAggregate.apply(new HearingResultsAdded(input.getHearing(), input.getSharedTime(), input.getVariants()));

        final NowsMaterialStatusUpdated update = resultsAggregate.updateNowsMaterialStatus(hearingId, materialId, status)
                .map(o -> (NowsMaterialStatusUpdated) o)
                .findFirst()
                .orElse(null);
        assertNotNull(update);
        assertEquals(hearingId, update.getHearingId());
        assertEquals(materialId, update.getMaterialId());
        assertEquals(status, update.getStatus());
    }

    @Test
    public void testStatusUpdateOutOfOrder() {
        final UUID hearingId = UUID.randomUUID();
        final UUID materialId = UUID.randomUUID();
        final String status = "processing";

        final PendingMaterialStatusUpdate update = resultsAggregate.updateNowsMaterialStatus(hearingId, materialId, status)
                .map(o -> (PendingMaterialStatusUpdate) o)
                .findFirst()
                .orElse(null);
        assertNotNull(update);
        assertEquals(hearingId, update.getHearingId());
        assertEquals(materialId, update.getMaterialId());
        assertEquals(status, update.getStatus());
    }

    @Test
    public void testSaveShareResults_shouldRaiseHearingResultsAddedEvent() {



        final String newStatus = "generated";

        resultsAggregate.apply(new PendingMaterialStatusUpdate(input.getHearing().getId(), input.getVariants().get(0).getMaterialId(), newStatus));

        final HearingResultsAdded hearingResultsAdded = resultsAggregate.saveHearingResults(input)
                .map(o -> (HearingResultsAdded) o)
                .findFirst()
                .orElse(null);

        assertNotNull(hearingResultsAdded);
        assertEquals(input.getHearing(), hearingResultsAdded.getHearing());
        assertEquals(input.getVariants(), hearingResultsAdded.getVariants());
        assertEquals(input.getSharedTime(), hearingResultsAdded.getSharedTime());
        assertEquals(newStatus, hearingResultsAdded.getVariants().get(0).getStatus());

    }
}