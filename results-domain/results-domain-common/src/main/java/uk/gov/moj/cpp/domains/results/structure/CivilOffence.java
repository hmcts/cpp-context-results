package uk.gov.moj.cpp.domains.results.structure;

import java.io.Serializable;
import java.util.Objects;

public class CivilOffence implements Serializable {
    private long serialVersionUID = 7339455617608047938L;

    private Boolean isExParte;

    private Boolean isRespondent;

    public CivilOffence(final Boolean isExParte, final Boolean isRespondent) {
        this.isExParte = isExParte;
        this.isRespondent = isRespondent;
    }

    public Boolean getIsExParte() {
        return isExParte;
    }

    public Boolean getIsRespondent() {
        return isRespondent;
    }

    public static Builder civilOffence() {
        return new CivilOffence.Builder();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final CivilOffence that = (CivilOffence) obj;

        return Objects.equals(this.isExParte, that.isExParte) &&
                Objects.equals(this.isRespondent, that.isRespondent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(isExParte, isRespondent);
    }

    @Override
    public String toString() {
        return "CivilOffence{" +
                "isExParte='" + isExParte + "'," +
                "isRespondent='" + isRespondent + "'" +
                "}";
    }

    public static class Builder {
        private Boolean isExParte;

        private Boolean isRespondent;

        public Builder withIsExParte(final Boolean isExParte) {
            this.isExParte = isExParte;
            return this;
        }

        public Builder withIsRespondent(final Boolean isRespondent) {
            this.isRespondent = isRespondent;
            return this;
        }

        public Builder withValuesFrom(final CivilOffence civilOffence) {
            this.isExParte = civilOffence.getIsExParte();
            this.isRespondent = civilOffence.getIsRespondent();
            return this;
        }

        public CivilOffence build() {
            return new CivilOffence(isExParte, isRespondent);
        }
    }
}
