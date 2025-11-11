package uk.gov.moj.cpp.domains.results.shareresults;

import java.time.ZonedDateTime;

@SuppressWarnings({"squid:S2384"})
public class ShareResultsMessage {

    private Hearing hearing;

    private ZonedDateTime sharedTime;

    public Hearing getHearing() {
        return hearing;
    }

    public ZonedDateTime getSharedTime() {
        return sharedTime;
    }

    public ShareResultsMessage setHearing(Hearing hearing) {
        this.hearing = hearing;
        return this;
    }

    public ShareResultsMessage setSharedTime(ZonedDateTime sharedTime) {
        this.sharedTime = sharedTime;
        return this;
    }


    public static ShareResultsMessage shareResultsMessage() {
        return new ShareResultsMessage();
    }
}
