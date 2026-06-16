package uk.gov.moj.cpp.results.domain.informant.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class InformantRegisterResult {

    private final String resultText;
    private final String cjsResultCode;
    private final InformantRegisterResultData resultData;

    @JsonCreator
    public InformantRegisterResult(
            @JsonProperty("resultText") final String resultText,
            @JsonProperty("cjsResultCode") final String cjsResultCode,
            @JsonProperty("resultData") final InformantRegisterResultData resultData) {
        this.resultText = resultText;
        this.cjsResultCode = cjsResultCode;
        this.resultData = resultData;
    }

    public String getResultText() {
        return resultText;
    }

    public String getCjsResultCode() {
        return cjsResultCode;
    }

    public InformantRegisterResultData getResultData() {
        return resultData;
    }

    public static Builder informantRegisterResult() {
        return new Builder();
    }

    public static class Builder {
        private String resultText;
        private String cjsResultCode;
        private InformantRegisterResultData resultData;

        public Builder withResultText(final String resultText) {
            this.resultText = resultText;
            return this;
        }

        public Builder withCjsResultCode(final String cjsResultCode) {
            this.cjsResultCode = cjsResultCode;
            return this;
        }

        public Builder withResultData(final InformantRegisterResultData resultData) {
            this.resultData = resultData;
            return this;
        }

        public InformantRegisterResult build() {
            return new InformantRegisterResult(resultText, cjsResultCode, resultData);
        }
    }
}
