package uk.gov.moj.cpp.results.domain.event;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.results.domain.informant.model.InformantRegisterDocumentRequest;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("results.event.informant-register-generated-v2")
public class InformantRegisterGeneratedV2 {

    public static final String NAME = "results.event.informant-register-generated-v2";

    private final List<InformantRegisterDocumentRequest> informantRegisterDocumentRequests;
    private final Boolean systemGenerated;

    @JsonCreator
    public InformantRegisterGeneratedV2(
            @JsonProperty("informantRegisterDocumentRequests") final List<InformantRegisterDocumentRequest> informantRegisterDocumentRequests,
            @JsonProperty("systemGenerated") final Boolean systemGenerated) {
        this.informantRegisterDocumentRequests = informantRegisterDocumentRequests;
        this.systemGenerated = systemGenerated;
    }

    public List<InformantRegisterDocumentRequest> getInformantRegisterDocumentRequests() { return informantRegisterDocumentRequests; }
    public Boolean getSystemGenerated() { return systemGenerated; }

    public static Builder informantRegisterGeneratedV2() {
        return new Builder();
    }

    public static class Builder {
        private List<InformantRegisterDocumentRequest> informantRegisterDocumentRequests;
        private Boolean systemGenerated;

        public Builder withInformantRegisterDocumentRequests(final List<InformantRegisterDocumentRequest> informantRegisterDocumentRequests) { this.informantRegisterDocumentRequests = informantRegisterDocumentRequests; return this; }
        public Builder withSystemGenerated(final Boolean systemGenerated) { this.systemGenerated = systemGenerated; return this; }

        public InformantRegisterGeneratedV2 build() {
            return new InformantRegisterGeneratedV2(informantRegisterDocumentRequests, systemGenerated);
        }
    }
}
