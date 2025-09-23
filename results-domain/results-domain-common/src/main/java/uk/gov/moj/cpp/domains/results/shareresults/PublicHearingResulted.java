package uk.gov.moj.cpp.domains.results.shareresults;

import uk.gov.justice.core.courts.Hearing;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings({"squid:S2384"})
public class PublicHearingResulted {
    private Hearing hearing;
    private ZonedDateTime sharedTime;
    private List<UUID> shadowListedOffences;
    private Optional<LocalDate> hearingDay;
    private Optional<Boolean> isReshare;

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

    public List<UUID> getShadowListedOffences() {
        return shadowListedOffences;
    }

    public PublicHearingResulted setShadowListedOffences(List<UUID> shadowListedOffences) {
        this.shadowListedOffences = shadowListedOffences;
        return this;
    }

    public Optional<LocalDate> getHearingDay() {
        return hearingDay;
    }

    public PublicHearingResulted setHearingDay(final Optional<LocalDate> hearingDay) {
        this.hearingDay = hearingDay;
        return this;
    }

    public Optional<Boolean> getIsReshare() {
        return isReshare;
    }

    public PublicHearingResulted setIsReshare(final Optional<Boolean> reshare) {
        isReshare = reshare;
        return this;
    }
}
