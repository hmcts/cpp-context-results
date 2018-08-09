package uk.gov.moj.cpp.results.query.view.service;

import static java.util.stream.Collectors.toList;

import uk.gov.moj.cpp.results.persist.HearingRepository;
import uk.gov.moj.cpp.results.persist.HearingResultRepository;
import uk.gov.moj.cpp.results.persist.entity.HearingResult;
import uk.gov.moj.cpp.results.persist.entity.HearingResultSummary;
import uk.gov.moj.cpp.results.query.view.response.DefendantView;
import uk.gov.moj.cpp.results.query.view.response.HearingResultSummaryView;
import uk.gov.moj.cpp.results.query.view.response.HearingResultSummariesView;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

public class HearingService {

    @Inject
    private HearingRepository hearingRepository;

    @Inject
    private HearingResultRepository hearingResultRepository;

    public HearingResultSummariesView findHearingResultSummariesFromDate(final LocalDate fromDate) {
        List<HearingResultSummaryView> hearingResultSummaryViews =
                hearingRepository.findHearingResultSummariesByFromDate(fromDate)
                        .stream()
                        .map(this::hearingResultSummaryView)
                        .collect(toList());

        return new HearingResultSummariesView(hearingResultSummaryViews);
    }

    private HearingResultSummaryView hearingResultSummaryView(HearingResultSummary hrs) {
        UUID hearingId = hrs.getHearingId();
        UUID personId = hrs.getPersonId();
        DefendantView defendantView = new DefendantView(personId, hrs.getDefendantFirstName(), hrs.getDefendantLastName());
        final List<HearingResult> hearingResults = hearingResultRepository.findByHearingIdAndPersonId(hearingId, personId);

        List<String> urns = hearingResults.stream().map(HearingResult::getUrn).distinct().collect(toList());

        HearingResultSummaryView.Builder hrsvBuilder = new HearingResultSummaryView.Builder(
                                    hearingId, hrs.getHearingType(), hrs.getHearingDate(), defendantView);

        return hrsvBuilder.urns(urns).build();
    }
}
