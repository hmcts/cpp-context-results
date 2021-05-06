package uk.gov.moj.cpp.results.event.service;

public enum ConversionFormat {

    PDF("pdf");

    private final String value;

    ConversionFormat(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
    }
