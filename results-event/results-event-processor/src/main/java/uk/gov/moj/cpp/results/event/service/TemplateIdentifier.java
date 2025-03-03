package uk.gov.moj.cpp.results.event.service;

public enum TemplateIdentifier {
    NCES_EMAIL_NOTIFICATION_TEMPLATE_ID("NCESNotification"),
    POLICE_NOTIFICATION_HEARING_RESULTS_TEMPLATE("Police Notification Hearing Results");

    private final String value;

    TemplateIdentifier(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
