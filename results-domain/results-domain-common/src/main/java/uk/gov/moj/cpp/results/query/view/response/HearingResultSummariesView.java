package uk.gov.moj.cpp.results.query.view.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@SuppressWarnings({"squid:S2384"})
public class HearingResultSummariesView {

    private final List<HearingResultSummaryView> results;

    public HearingResultSummariesView(@JsonProperty(value = "results", required = true) final List<HearingResultSummaryView> results) {
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
