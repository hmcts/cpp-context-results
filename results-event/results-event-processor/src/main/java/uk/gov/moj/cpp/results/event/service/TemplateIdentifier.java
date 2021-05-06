package uk.gov.moj.cpp.results.event.service;

public enum TemplateIdentifier {
    NCES_EMAIL_NOTIFICATION_TEMPLATE_ID("NCESNotification");

    private final String value;

    TemplateIdentifier(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
