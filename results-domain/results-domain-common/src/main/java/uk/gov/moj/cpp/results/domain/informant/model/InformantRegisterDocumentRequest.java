package uk.gov.moj.cpp.results.domain.informant.model;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class InformantRegisterDocumentRequest {

    private final ZonedDateTime registerDate;
    private final ZonedDateTime hearingDate;
    private final UUID hearingId;
    private final UUID prosecutionAuthorityId;
    private final String prosecutionAuthorityCode;
    private final String prosecutionAuthorityOuCode;
    private final String majorCreditorCode;
    private final String prosecutionAuthorityName;
    private final String fileName;
    private final List<InformantRegisterRecipient> recipients;
    private final InformantRegisterHearingVenue hearingVenue;
    private final UUID groupId;

    @JsonCreator
    public InformantRegisterDocumentRequest(
            @JsonProperty("registerDate") final ZonedDateTime registerDate,
            @JsonProperty("hearingDate") final ZonedDateTime hearingDate,
            @JsonProperty("hearingId") final UUID hearingId,
            @JsonProperty("prosecutionAuthorityId") final UUID prosecutionAuthorityId,
            @JsonProperty("prosecutionAuthorityCode") final String prosecutionAuthorityCode,
            @JsonProperty("prosecutionAuthorityOuCode") final String prosecutionAuthorityOuCode,
            @JsonProperty("majorCreditorCode") final String majorCreditorCode,
            @JsonProperty("prosecutionAuthorityName") final String prosecutionAuthorityName,
            @JsonProperty("fileName") final String fileName,
            @JsonProperty("recipients") final List<InformantRegisterRecipient> recipients,
            @JsonProperty("hearingVenue") final InformantRegisterHearingVenue hearingVenue,
            @JsonProperty("groupId") final UUID groupId) {
        this.registerDate = registerDate;
        this.hearingDate = hearingDate;
        this.hearingId = hearingId;
        this.prosecutionAuthorityId = prosecutionAuthorityId;
        this.prosecutionAuthorityCode = prosecutionAuthorityCode;
        this.prosecutionAuthorityOuCode = prosecutionAuthorityOuCode;
        this.majorCreditorCode = majorCreditorCode;
        this.prosecutionAuthorityName = prosecutionAuthorityName;
        this.fileName = fileName;
        this.recipients = recipients;
        this.hearingVenue = hearingVenue;
        this.groupId = groupId;
    }

    public ZonedDateTime getRegisterDate() { return registerDate; }
    public ZonedDateTime getHearingDate() { return hearingDate; }
    public UUID getHearingId() { return hearingId; }
    public UUID getProsecutionAuthorityId() { return prosecutionAuthorityId; }
    public String getProsecutionAuthorityCode() { return prosecutionAuthorityCode; }
    public String getProsecutionAuthorityOuCode() { return prosecutionAuthorityOuCode; }
    public String getMajorCreditorCode() { return majorCreditorCode; }
    public String getProsecutionAuthorityName() { return prosecutionAuthorityName; }
    public String getFileName() { return fileName; }
    public List<InformantRegisterRecipient> getRecipients() { return recipients; }
    public InformantRegisterHearingVenue getHearingVenue() { return hearingVenue; }
    public UUID getGroupId() { return groupId; }

    public static Builder informantRegisterDocumentRequest() {
        return new Builder();
    }

    public static class Builder {
        private ZonedDateTime registerDate;
        private ZonedDateTime hearingDate;
        private UUID hearingId;
        private UUID prosecutionAuthorityId;
        private String prosecutionAuthorityCode;
        private String prosecutionAuthorityOuCode;
        private String majorCreditorCode;
        private String prosecutionAuthorityName;
        private String fileName;
        private List<InformantRegisterRecipient> recipients;
        private InformantRegisterHearingVenue hearingVenue;
        private UUID groupId;

        public Builder withRegisterDate(final ZonedDateTime registerDate) { this.registerDate = registerDate; return this; }
        public Builder withHearingDate(final ZonedDateTime hearingDate) { this.hearingDate = hearingDate; return this; }
        public Builder withHearingId(final UUID hearingId) { this.hearingId = hearingId; return this; }
        public Builder withProsecutionAuthorityId(final UUID prosecutionAuthorityId) { this.prosecutionAuthorityId = prosecutionAuthorityId; return this; }
        public Builder withProsecutionAuthorityCode(final String prosecutionAuthorityCode) { this.prosecutionAuthorityCode = prosecutionAuthorityCode; return this; }
        public Builder withProsecutionAuthorityOuCode(final String prosecutionAuthorityOuCode) { this.prosecutionAuthorityOuCode = prosecutionAuthorityOuCode; return this; }
        public Builder withMajorCreditorCode(final String majorCreditorCode) { this.majorCreditorCode = majorCreditorCode; return this; }
        public Builder withProsecutionAuthorityName(final String prosecutionAuthorityName) { this.prosecutionAuthorityName = prosecutionAuthorityName; return this; }
        public Builder withFileName(final String fileName) { this.fileName = fileName; return this; }
        public Builder withRecipients(final List<InformantRegisterRecipient> recipients) { this.recipients = recipients; return this; }
        public Builder withHearingVenue(final InformantRegisterHearingVenue hearingVenue) { this.hearingVenue = hearingVenue; return this; }
        public Builder withGroupId(final UUID groupId) { this.groupId = groupId; return this; }

        public InformantRegisterDocumentRequest build() {
            return new InformantRegisterDocumentRequest(registerDate, hearingDate, hearingId,
                    prosecutionAuthorityId, prosecutionAuthorityCode, prosecutionAuthorityOuCode,
                    majorCreditorCode, prosecutionAuthorityName, fileName, recipients, hearingVenue, groupId);
        }
    }
}
