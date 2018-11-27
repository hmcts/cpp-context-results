package uk.gov.moj.cpp.domains.results.shareresults;

import uk.gov.justice.json.schemas.core.SharedHearing;
import uk.gov.justice.json.schemas.core.SharedVariant;

import java.time.ZonedDateTime;
import java.util.List;

@SuppressWarnings({"squid:S2384"})
public class PublicHearingResulted {
    private SharedHearing hearing;
    private List<SharedVariant> variants;
    private ZonedDateTime sharedTime;

    public static PublicHearingResulted publicHearingResulted() {
        return new PublicHearingResulted();
    }

    public SharedHearing getHearing() {
        return hearing;
    }

    public PublicHearingResulted setHearing(SharedHearing hearing) {
        this.hearing = hearing;
        return this;
    }

    public List<SharedVariant> getVariants() {
        return variants;
    }

    public PublicHearingResulted setVariants(List<SharedVariant> variants) {
        this.variants = variants;
        return this;
    }

    public ZonedDateTime getSharedTime() {
        return sharedTime;
    }

    public PublicHearingResulted setSharedTime(ZonedDateTime sharedTime) {
        this.sharedTime = sharedTime;
        return this;
    }
}
