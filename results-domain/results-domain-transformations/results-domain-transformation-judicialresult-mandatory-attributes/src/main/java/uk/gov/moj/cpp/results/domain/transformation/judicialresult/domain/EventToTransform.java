package uk.gov.moj.cpp.results.domain.transformation.judicialresult.domain;

import java.util.Arrays;

public enum EventToTransform {

    POLICE_RESULT_GENERATED("results.event.police-result-generated"),
    HEARING_RESULTS_ADDED("results.hearing-results-added"),
    DEFENDANT_ADDED_EVENT("results.event.defendant-added-event"),
    DEFENDANT_UPDATED_EVENT("results.event.defendant-updated-event");

    private final String eventName;

    EventToTransform(final String eventName) {
        this.eventName = eventName;
    }

    public String getEventName() {
        return eventName;
    }

    public static boolean isEventToTransform(final String eventName) {
        return Arrays.stream(values()).anyMatch(event -> event.eventName.equals(eventName));
    }
}
