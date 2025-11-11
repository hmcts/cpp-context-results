package uk.gov.moj.cpp.domains.results.shareresults;

import java.util.UUID;

public class YouthCourt {

    private UUID youthCourtId;
    private Integer courtCode;
    private String name;

    public UUID getYouthCourtId() {
        return youthCourtId;
    }

    public void setYouthCourtId(UUID youthCourtId) {
        this.youthCourtId = youthCourtId;
    }

    public Integer getCourtCode() {
        return courtCode;
    }

    public void setCourtCode(Integer courtCode) {
        this.courtCode = courtCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static YouthCourt youthCourt() {
        return new YouthCourt();
    }
}
