package uk.gov.moj.cpp.domains.results.result;

import java.io.Serializable;

public class Prompt implements Serializable {

    private final String label;
    private final String value;

    public Prompt(final String label, final String value) {
        this.label = label;
        this.value = value;
    }

    public String getLabel() {
        return label;
    }

    public String getValue() {
        return value;
    }
}
