package uk.gov.moj.cpp.domains.results.shareresults;

import uk.gov.justice.core.courts.Hearing;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@SuppressWarnings({"squid:S2384"})
public class PublicHearingResulted {
    private Hearing hearing;
    private ZonedDateTime sharedTime;
    private List<UUID> shadowListedOffences;

    public static PublicHearingResulted publicHearingResulted() {
        return new PublicHearingResulted();
    }

    public Hearing getHearing() {
        return hearing;
    }

    public PublicHearingResulted setHearing(Hearing hearing) {
        this.hearing = hearing;
        return this;
    }

    public ZonedDateTime getSharedTime() {
        return sharedTime;
    }

    public PublicHearingResulted setSharedTime(ZonedDateTime sharedTime) {
        this.sharedTime = sharedTime;
        return this;
    }

    public List<UUID> getShadowListedOffences() { return shadowListedOffences; }

    public PublicHearingResulted setShadowListedOffences(List<UUID> shadowListedOffences) {
        this.shadowListedOffences = shadowListedOffences;
        return this;
    }
}
