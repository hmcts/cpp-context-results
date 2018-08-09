package uk.gov.moj.cpp.results.domain.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.justice.domain.annotation.Event;

import javax.json.JsonArray;
import javax.json.JsonObject;
import java.time.ZonedDateTime;

@Event("results.hearing-results-added")
public final class HearingResultsAdded {

    private final JsonObject hearing;
    private final ZonedDateTime sharedTime;
    private final JsonArray variants;

    @JsonCreator
    public HearingResultsAdded(@JsonProperty(value = "hearing", required = true) final JsonObject hearing,
                               @JsonProperty(value = "sharedTime", required = true) final ZonedDateTime sharedTime, @JsonProperty(value = "variants", required = true) final JsonArray variants) {
        this.hearing = hearing;
        this.sharedTime = sharedTime;
        this.variants = variants;
    }

    public JsonObject getHearing() {
        return hearing;
    }

    public ZonedDateTime getSharedTime() {
        return sharedTime;
    }

    public JsonArray getVariants() {
        return variants;
    }
}