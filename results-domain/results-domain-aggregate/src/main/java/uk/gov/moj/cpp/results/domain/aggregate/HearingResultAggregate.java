package uk.gov.moj.cpp.results.domain.aggregate;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.moj.cpp.results.domain.event.NowsMaterialStatusUpdated;

import java.util.UUID;
import java.util.stream.Stream;

import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;

public class HearingResultAggregate implements Aggregate {

    private static final long serialVersionUID = 1L;

    @Override
    public Object apply(Object event) {
        return match(event).with(
                otherwiseDoNothing()
        );
    }

    public Stream<Object> updateNowsMaterialStatus(UUID hearingId, UUID materialId, String status) {
        return apply(Stream.of(new NowsMaterialStatusUpdated(hearingId, materialId, status)));
    }
}
