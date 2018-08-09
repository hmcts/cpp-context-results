package uk.gov.moj.cpp.results.query.view.response;

import java.util.List;

public class HearingResultSummariesView {

    private final List<HearingResultSummaryView> results;

    public HearingResultSummariesView(final List<HearingResultSummaryView> results) {
        this.results = results;
    }

    public List<HearingResultSummaryView> getResults() {
        return results;
    }

    @Override
    public String toString() {
        return "HearingResultsView{" +
                "results='" + results +
                '}';
    }

}
