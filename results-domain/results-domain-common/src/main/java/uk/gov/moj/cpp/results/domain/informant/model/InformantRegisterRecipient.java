package uk.gov.moj.cpp.results.domain.informant.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class InformantRegisterRecipient implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String recipientName;
    private final String emailAddress1;
    private final String emailAddress2;
    private final String emailTemplateName;

    @JsonCreator
    public InformantRegisterRecipient(
            @JsonProperty("recipientName") final String recipientName,
            @JsonProperty("emailAddress1") final String emailAddress1,
            @JsonProperty("emailAddress2") final String emailAddress2,
            @JsonProperty("emailTemplateName") final String emailTemplateName) {
        this.recipientName = recipientName;
        this.emailAddress1 = emailAddress1;
        this.emailAddress2 = emailAddress2;
        this.emailTemplateName = emailTemplateName;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public String getEmailAddress1() {
        return emailAddress1;
    }

    public String getEmailAddress2() {
        return emailAddress2;
    }

    public String getEmailTemplateName() {
        return emailTemplateName;
    }

    public static Builder informantRegisterRecipient() {
        return new Builder();
    }

    public static class Builder {
        private String recipientName;
        private String emailAddress1;
        private String emailAddress2;
        private String emailTemplateName;

        public Builder withRecipientName(final String recipientName) {
            this.recipientName = recipientName;
            return this;
        }

        public Builder withEmailAddress1(final String emailAddress1) {
            this.emailAddress1 = emailAddress1;
            return this;
        }

        public Builder withEmailAddress2(final String emailAddress2) {
            this.emailAddress2 = emailAddress2;
            return this;
        }

        public Builder withEmailTemplateName(final String emailTemplateName) {
            this.emailTemplateName = emailTemplateName;
            return this;
        }

        public InformantRegisterRecipient build() {
            return new InformantRegisterRecipient(recipientName, emailAddress1, emailAddress2, emailTemplateName);
        }
    }
}
