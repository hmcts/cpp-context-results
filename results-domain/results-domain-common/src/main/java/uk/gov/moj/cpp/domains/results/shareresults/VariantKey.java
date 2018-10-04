package uk.gov.moj.cpp.domains.results.shareresults;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@SuppressWarnings({"squid:S2384"})
public class VariantKey implements Serializable {

    private UUID hearingId;

    private UUID defendantId;

    private UUID nowsTypeId;

    private List<String> usergroups;

    public static VariantKey variantKey() {
        return new VariantKey();
    }

    public UUID getHearingId() {
        return this.hearingId;
    }

    public VariantKey setHearingId(UUID hearingId) {
        this.hearingId = hearingId;
        return this;
    }

    public UUID getDefendantId() {
        return this.defendantId;
    }

    public VariantKey setDefendantId(UUID defendantId) {
        this.defendantId = defendantId;
        return this;
    }

    public UUID getNowsTypeId() {
        return this.nowsTypeId;
    }

    public VariantKey setNowsTypeId(UUID nowsTypeId) {
        this.nowsTypeId = nowsTypeId;
        return this;
    }

    public List<String> getUsergroups() {
        return this.usergroups;
    }

    public VariantKey setUsergroups(List<String> usergroups) {
        this.usergroups = usergroups;
        return this;
    }
}
