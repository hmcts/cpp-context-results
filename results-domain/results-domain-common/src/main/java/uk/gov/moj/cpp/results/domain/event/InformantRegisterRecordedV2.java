package uk.gov.moj.cpp.results.domain.event;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.results.domain.informant.model.InformantRegisterDocumentRequest;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("results.event.informant-register-recorded-v2")
public class InformantRegisterRecordedV2 {

    public static final String NAME = "results.event.informant-register-recorded-v2";

    private final UUID prosecutionAuthorityId;
    private final InformantRegisterDocumentRequest informantRegister;

    @JsonCreator
    public InformantRegisterRecordedV2(
            @JsonProperty("prosecutionAuthorityId") final UUID prosecutionAuthorityId,
            @JsonProperty("informantRegister") final InformantRegisterDocumentRequest informantRegister) {
        this.prosecutionAuthorityId = prosecutionAuthorityId;
        this.informantRegister = informantRegister;
    }

    public UUID getProsecutionAuthorityId() { return prosecutionAuthorityId; }
    public InformantRegisterDocumentRequest getInformantRegister() { return informantRegister; }

    public static Builder informantRegisterRecordedV2() {
        return new Builder();
    }

    public static class Builder {
        private UUID prosecutionAuthorityId;
        private InformantRegisterDocumentRequest informantRegister;

        public Builder withProsecutionAuthorityId(final UUID prosecutionAuthorityId) { this.prosecutionAuthorityId = prosecutionAuthorityId; return this; }
        public Builder withInformantRegister(final InformantRegisterDocumentRequest informantRegister) { this.informantRegister = informantRegister; return this; }

        public InformantRegisterRecordedV2 build() {
            return new InformantRegisterRecordedV2(prosecutionAuthorityId, informantRegister);
        }
    }
}
