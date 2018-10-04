package uk.gov.moj.cpp.results.domain.aggregate;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.moj.cpp.domains.results.shareresults.PublicHearingResulted;
import uk.gov.moj.cpp.results.domain.event.HearingResultsAdded;

import java.util.stream.Stream;

import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;

public class ResultsAggregate implements Aggregate {

    private static final long serialVersionUID = 1L;

    @Override
    public Object apply(final Object event) {
        return match(event).with(otherwiseDoNothing());
    }

    public Stream<Object> saveHearingResults(final PublicHearingResulted payload) {

        return apply(Stream.of(new HearingResultsAdded(payload.getHearing(), payload.getSharedTime(), payload.getVariants())));
    }
}