package uk.gov.moj.cpp.results.persist.entity;

import java.util.UUID;

public class CourtClerk {

    private final UUID clerkOfTheCourtId;

    private final String clerkOfTheCourtFirstName;

    private final String clerkOfTheCourtLastName;

    public CourtClerk(final UUID clerkOfTheCourtId, final String clerkOfTheCourtFirstName, final String clerkOfTheCourtLastName) {
        this.clerkOfTheCourtId = clerkOfTheCourtId;
        this.clerkOfTheCourtFirstName = clerkOfTheCourtFirstName;
        this.clerkOfTheCourtLastName = clerkOfTheCourtLastName;
    }

    public UUID getClerkOfTheCourtId() {
        return clerkOfTheCourtId;
    }

    public String getClerkOfTheCourtFirstName() {
        return clerkOfTheCourtFirstName;
    }

    public String getClerkOfTheCourtLastName() {
        return clerkOfTheCourtLastName;
    }

    public static Builder of(CourtClerk courtClerk) {
        return new Builder(courtClerk.getClerkOfTheCourtId(),
                courtClerk.getClerkOfTheCourtFirstName(),
                courtClerk.getClerkOfTheCourtLastName());
    }

    public static Builder builder(){
        return new Builder();
    }

    public static class Builder {

        private UUID clerkOfTheCourtId;

        private String clerkOfTheCourtFirstName;

        private String clerkOfTheCourtLastName;

        private Builder(){

        }

        public Builder(final UUID clerkOfTheCourtId, final String clerkOfTheCourtFirstName, final String clerkOfTheCourtLastName) {
            this.clerkOfTheCourtId = clerkOfTheCourtId;
            this.clerkOfTheCourtFirstName = clerkOfTheCourtFirstName;
            this.clerkOfTheCourtLastName = clerkOfTheCourtLastName;
        }

        public Builder withClerkOfTheCourtId(UUID clerkOfTheCourtId) {
            this.clerkOfTheCourtId = clerkOfTheCourtId;
            return this;
        }

        public Builder withClerkOfTheCourtFirstName(String clerkOfTheCourtFirstName) {
            this.clerkOfTheCourtFirstName = clerkOfTheCourtFirstName;
            return this;
        }

        public Builder withClerkOfTheCourtLastName(String clerkOfTheCourtLastName) {
            this.clerkOfTheCourtLastName = clerkOfTheCourtLastName;
            return this;
        }

        public CourtClerk build(){
            return new CourtClerk(this.clerkOfTheCourtId, this.clerkOfTheCourtFirstName, this.clerkOfTheCourtLastName);
        }
    }
}
