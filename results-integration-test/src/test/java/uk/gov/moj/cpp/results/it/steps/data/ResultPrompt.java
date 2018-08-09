package uk.gov.moj.cpp.results.it.steps.data;

public class ResultPrompt {
    private String label;
    private String value;

    public ResultPrompt(final String label, final String value) {
        this.label = label;
        this.value = value;
    }

    public String getLabel() {
        return label;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "ResultPrompt{" +
                "label='" + label + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
