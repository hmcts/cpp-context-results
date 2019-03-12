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

    private Enveloper enveloper;

    @Override
    public Action actionFor(final JsonEnvelope event) {
        return DEACTIVATE;
    }

    @Override
    public void setEnveloper(final Enveloper enveloper) {
        this.enveloper = enveloper;
    }
}