package uk.gov.moj.cpp.results.domain.aggregate;

import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;

import uk.gov.justice.core.courts.HearingResultsAdded;
import uk.gov.justice.core.courts.HearingApplicationEjected;
import uk.gov.justice.core.courts.HearingCaseEjected;
import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.moj.cpp.domains.results.shareresults.PublicHearingResulted;

import javax.json.JsonObject;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static java.util.UUID.fromString;
import static java.util.stream.Stream.empty;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;

@SuppressWarnings({"squid:S1068", "squid:S1450"})
public class ResultsAggregate implements Aggregate {

    private static final String CASE_ID = "caseId";
    private static final String APPLICATION_ID = "applicationId";
    private static final long serialVersionUID = 100L;
    private final Set<UUID> hearingIds = new HashSet<>() ;


    @Override
    public Object apply(final Object event) {
        return match(event).with(
        when(HearingResultsAdded.class).apply(
                e -> this.hearingIds.add(e.getHearing().getId())
        ),
        otherwiseDoNothing());
    }

    public Stream<Object> saveHearingResults(final PublicHearingResulted payload) {
        return apply(Stream.of(new HearingResultsAdded(payload.getHearing(), payload.getSharedTime())));
    }

    public Stream<Object> ejectCaseOrApplication(final UUID hearingId, final JsonObject payload) {
        if(hearingIds.contains(hearingId)) {
            if (payload.containsKey(CASE_ID)) {
                return apply(Stream.of(new HearingCaseEjected(fromString(payload.getString(CASE_ID)), hearingId)));
            } else {
                return apply(Stream.of(new HearingApplicationEjected(fromString(payload.getString(APPLICATION_ID)), hearingId)));
            }
        }
        return empty();
    }


}