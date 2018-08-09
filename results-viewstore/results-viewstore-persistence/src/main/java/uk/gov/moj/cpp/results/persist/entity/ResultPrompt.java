package uk.gov.moj.cpp.results.persist.entity;

import java.util.Objects;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;

@Entity
@Table(name = "result_prompt")
@IdClass(value = ResultPromptKey.class)
public class ResultPrompt {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "label")
    private String label;

    @Column(name = "value")
    private String value;

    @Id
    @Column(name = "hearing_result_id")
    private UUID hearingResultId;

    public ResultPrompt() {
        // for JPA
    }

    private ResultPrompt(final UUID id, final String label, final String value, final UUID hearingResultId) {
        this.id = id;
        this.label = label;
        this.value = value;
        this.hearingResultId = hearingResultId;
    }

    public UUID getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getValue() {
        return value;
    }

    public UUID getHearingResultId() {
        return hearingResultId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, hearingResultId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ResultPrompt other = (ResultPrompt) obj;
        return Objects.equals(this.id, other.id) && Objects.equals(this.hearingResultId, other.hearingResultId);
    }

    public static Builder of(ResultPrompt resultPrompt) {
        return new Builder(resultPrompt.getId(),
                resultPrompt.getLabel(),
                resultPrompt.getValue(),
                resultPrompt.getHearingResultId());
    }

    public static Builder builder(){
        return new Builder();
    }

    public static class Builder {

        private UUID id;

        private String label;

        private String value;

        private UUID hearingResultId;

        private Builder(){

        }

        private Builder(final UUID id, final String label, final String value, final UUID hearingResultId) {
            this.id = id;
            this.label = label;
            this.value = value;
            this.hearingResultId = hearingResultId;
        }

        public Builder withId(final UUID id) {
            this.id = id;
            return this;
        }

        public Builder withLabel(final String label) {
            this.label = label;
            return this;
        }

        public Builder withValue(final String value) {
            this.value = value;
            return this;
        }

        public Builder withHearingResultId(UUID hearingResultId) {
            this.hearingResultId = hearingResultId;
            return this;
        }

        public ResultPrompt build(){
            return new ResultPrompt(this.id, this.label, this.value, this.hearingResultId);
        }
    }
}
