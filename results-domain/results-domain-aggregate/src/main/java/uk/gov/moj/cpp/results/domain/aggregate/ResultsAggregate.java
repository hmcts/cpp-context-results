package uk.gov.moj.cpp.results.domain.aggregate;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.moj.cpp.results.domain.event.HearingResultsAdded;

import javax.json.JsonObject;
import java.util.stream.Stream;

import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.services.common.converter.ZonedDateTimes.fromJsonString;

public class ResultsAggregate implements Aggregate {

    private static final long serialVersionUID = 1L;

    @Override
    public Object apply(final Object event) {
        return match(event).with(otherwiseDoNothing());
    }

    public Stream<Object> saveHearingResults(final JsonObject payload) {

        return apply(Stream.of(new HearingResultsAdded(payload.getJsonObject("hearing"), fromJsonString(payload.getJsonString("sharedTime")), payload.getJsonArray("variants"))));
    }
}