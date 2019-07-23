package uk.gov.moj.cpp.results.domain.transformation;


import static com.google.common.collect.Lists.newArrayList;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.DEACTIVATE;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.NO_ACTION;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.tools.eventsourcing.transformation.api.Action;
import uk.gov.justice.tools.eventsourcing.transformation.api.EventTransformation;
import uk.gov.justice.tools.eventsourcing.transformation.api.annotation.Transformation;

import java.util.List;
import java.util.stream.Stream;

@Transformation
public class ResultsEventStreamArchiver implements EventTransformation {

    private static final List<String> EVENTS_TO_ARCHIVE = newArrayList("results.event.nows-material-status-updated", "results.pending-material-status-update");

    private static final String EARLIER_ARCHIVED_EVENT_NAME_ENDS_WITH = ".archived.2.4.release";

    private Enveloper enveloper;

    @Override
    public Stream<JsonEnvelope> apply(final JsonEnvelope event) {
        final String restoredEventName = event.metadata().name().replace(EARLIER_ARCHIVED_EVENT_NAME_ENDS_WITH, "");

        final JsonEnvelope transformedEnvelope = enveloper
                .withMetadataFrom(event, restoredEventName)
                .apply(event.payload());
        return Stream.of(transformedEnvelope);
    }

    @Override
    public Action actionFor(final JsonEnvelope event) {
        if (EVENTS_TO_ARCHIVE.stream()
                .anyMatch(eventToArchive -> event.metadata().name().equalsIgnoreCase(eventToArchive))) {
            return DEACTIVATE;
        } else if (event.metadata().name().toLowerCase().endsWith(EARLIER_ARCHIVED_EVENT_NAME_ENDS_WITH)) {
            return new Action(true, true, false);
        }
        return NO_ACTION;
    }

    @Override
    public void setEnveloper(final Enveloper enveloper) {
        this.enveloper = enveloper;
    }
}