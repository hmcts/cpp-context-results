package uk.gov.moj.cpp.results.domain.transformation;

import static java.util.stream.Stream.of;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.NO_ACTION;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.TRANSFORM;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.tools.eventsourcing.transformation.api.Action;
import uk.gov.justice.tools.eventsourcing.transformation.api.EventTransformation;
import uk.gov.justice.tools.eventsourcing.transformation.api.annotation.Transformation;
import uk.gov.moj.cpp.results.domain.transformation.util.ResultsAddHearingTransformationEventHelper;

import java.util.stream.Stream;

import org.slf4j.Logger;

@SuppressWarnings({"squid:S1068", "squid:CallToDeprecatedMethod", "squid:S2221"})
@Transformation
public class ResultsNewTransformer implements EventTransformation {

    private static final String EVENT_RESULTS_ADD_HEARING = "results.hearing-results-added";

    private Enveloper enveloper;

    private static final Logger LOGGER = getLogger(ResultsNewTransformer.class);

    private final ResultsAddHearingTransformationEventHelper resultsAddHearingTransformationEventHelper = new ResultsAddHearingTransformationEventHelper();

    @Override
    public Action actionFor(final JsonEnvelope event) {
        if (event.metadata().name().equalsIgnoreCase(EVENT_RESULTS_ADD_HEARING)) {
            return TRANSFORM;
        }
        return NO_ACTION;
    }

    @Override
    public Stream<JsonEnvelope> apply(final JsonEnvelope event) {

        final JsonEnvelope transformedEvent = resultsAddHearingTransformationEventHelper.buildTransformedPayloadForResults(event, EVENT_RESULTS_ADD_HEARING);

        final JsonEnvelope transformedEnvelope = enveloper.withMetadataFrom(event, transformedEvent.metadata().asJsonObject().getString("name")).apply(transformedEvent.payload());

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("TransformedEnvelope: {}", transformedEnvelope);
        }

        return of(transformedEnvelope);
    }

    @Override
    public void setEnveloper(final Enveloper enveloper) {
        this.enveloper = enveloper;
    }
}
