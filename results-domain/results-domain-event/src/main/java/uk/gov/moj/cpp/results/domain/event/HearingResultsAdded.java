package uk.gov.moj.cpp.results.domain.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.justice.domain.annotation.Event;
import uk.gov.justice.json.schemas.core.SharedHearing;
import uk.gov.justice.json.schemas.core.SharedVariant;

import java.time.ZonedDateTime;
import java.util.List;

@Event("results.hearing-results-added")
public class HearingResultsAdded {

    private final SharedHearing hearing;
    private final ZonedDateTime sharedTime;
    private final List<SharedVariant> variants;

    @JsonCreator
    public HearingResultsAdded(@JsonProperty(value = "hearing", required = true) final SharedHearing hearing,
                               @JsonProperty(value = "sharedTime", required = true) final ZonedDateTime sharedTime, @JsonProperty(value = "variants", required = true) final List<SharedVariant> variants) {
        this.hearing = hearing;
        this.sharedTime = sharedTime;
        this.variants = variants;
    }

    public SharedHearing getHearing() {
        return hearing;
    }

    public ZonedDateTime getSharedTime() {
        return sharedTime;
    }

    public List<SharedVariant> getVariants() {
        return variants;
    }
}