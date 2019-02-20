package uk.gov.moj.cpp.results.domain.aggregate;

import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;

import uk.gov.justice.core.courts.SharedVariant;
import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.moj.cpp.domains.results.shareresults.PublicHearingResulted;
import uk.gov.moj.cpp.results.domain.event.HearingResultsAdded;
import uk.gov.moj.cpp.results.domain.event.NowsMaterialStatusUpdated;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

public class ResultsAggregate implements Aggregate {

    private static final long serialVersionUID = 1L;
    private boolean hearingResultIsAdded = false;
    private Map<UUID, String> materialId2Status = new HashMap<>();

    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(HearingResultsAdded.class).apply(hearingResultsAdded -> hearingResultIsAdded = true),
                when(PendingMaterialStatusUpdate.class).apply(this::storeMaterialStatus),
                otherwiseDoNothing()
        );
    }

    private void storeMaterialStatus(final PendingMaterialStatusUpdate pendingMaterialStatusUpdate) {
        materialId2Status.put(pendingMaterialStatusUpdate.getMaterialId(), pendingMaterialStatusUpdate.getStatus());
    }

    public Stream<Object> saveHearingResults(final PublicHearingResulted payload) {
        if (payload.getVariants() != null) {
            for (final SharedVariant sharedVariant : payload.getVariants()) {
                if (materialId2Status.containsKey(sharedVariant.getMaterialId())) {
                    sharedVariant.setStatus(materialId2Status.get(sharedVariant.getMaterialId()));
                }
            }
        }
        return apply(Stream.of(new HearingResultsAdded(payload.getHearing(), payload.getSharedTime(), payload.getVariants())));
    }

    public Stream<Object> updateNowsMaterialStatus(UUID hearingId, UUID materialId, String status) {
        if (hearingResultIsAdded) {
            return apply(Stream.of(new NowsMaterialStatusUpdated(hearingId, materialId, status)));
        } else {
            return apply(Stream.of(new PendingMaterialStatusUpdate(hearingId, materialId, status)));
        }
    }
}