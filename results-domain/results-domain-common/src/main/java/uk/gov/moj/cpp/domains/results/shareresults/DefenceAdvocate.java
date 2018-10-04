package uk.gov.moj.cpp.domains.results.shareresults;

import java.util.List;
import java.util.UUID;

@SuppressWarnings({"squid:S2384"})
public class DefenceAdvocate extends Attendee<DefenceAdvocate> {

    private List<UUID> defendantIds;

    private String status;

    public List<UUID> getDefendantIds() {
        return defendantIds;
    }

    public String getStatus() {
        return status;
    }

    public DefenceAdvocate setDefendantIds(List<UUID> defendantIds) {
        this.defendantIds = defendantIds;
        return this;
    }

    public DefenceAdvocate setStatus(String status) {
        this.status = status;
        return this;
    }

    public static DefenceAdvocate defenceAdvocate() {
        return new DefenceAdvocate();
    }
}
