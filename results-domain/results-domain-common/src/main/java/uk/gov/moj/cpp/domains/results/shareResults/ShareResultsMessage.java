package uk.gov.moj.cpp.domains.results.shareResults;

import java.time.ZonedDateTime;
import java.util.List;

public class ShareResultsMessage {

    private Hearing hearing;

    private ZonedDateTime sharedTime;

    private List<Variant> variants;

    public Hearing getHearing() {
        return hearing;
    }

    public ZonedDateTime getSharedTime() {
        return sharedTime;
    }

    public List<Variant> getVariants() {
        return variants;
    }

    public ShareResultsMessage setHearing(Hearing hearing) {
        this.hearing = hearing;
        return this;
    }

    public ShareResultsMessage setSharedTime(ZonedDateTime sharedTime) {
        this.sharedTime = sharedTime;
        return this;
    }

    public ShareResultsMessage setVariants(final List<Variant> variants) {
        this.variants = variants;
        return this;
    }


    public static ShareResultsMessage shareResultsMessage(){
        return new ShareResultsMessage();
    }
}
