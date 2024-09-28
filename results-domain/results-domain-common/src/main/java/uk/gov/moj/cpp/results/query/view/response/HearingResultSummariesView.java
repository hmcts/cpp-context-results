package uk.gov.moj.cpp.results.query.view.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@SuppressWarnings({"squid:S2384"})
public class HearingResultSummariesView {

    private final List<HearingResultSummaryView> results;

    @JsonCreator
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
