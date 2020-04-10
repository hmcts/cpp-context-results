package uk.gov.moj.cpp.results.domain.transformation;

import org.slf4j.Logger;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.tools.eventsourcing.transformation.api.Action;
import uk.gov.justice.tools.eventsourcing.transformation.api.EventTransformation;
import uk.gov.justice.tools.eventsourcing.transformation.api.annotation.Transformation;
import uk.gov.moj.cpp.results.domain.transformation.transform.ResultsEventTransformer;
import uk.gov.moj.cpp.results.domain.transformation.transform.TransformFactory;

import javax.json.JsonObject;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Stream.of;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.NO_ACTION;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.TRANSFORM;

@SuppressWarnings({"squid:S1068", "squid:CallToDeprecatedMethod", "squid:S2221"})
@Transformation
public class ResultsNewTransformer implements EventTransformation {

    private final TransformFactory transformFactory;

    private Enveloper enveloper;

    private static final Logger LOGGER = getLogger(ResultsNewTransformer.class);

    public ResultsNewTransformer() {
        transformFactory = new TransformFactory();
    }

    @Override
    public Action actionFor(final JsonEnvelope event) {
        final List<ResultsEventTransformer> eventTransformer = transformFactory.getEventTransformer(event.metadata().name().toLowerCase());
        if (eventTransformer != null && !eventTransformer.isEmpty()) {
            return TRANSFORM;
        }

        return NO_ACTION;
    }

    @Override
    public Stream<JsonEnvelope> apply(final JsonEnvelope event) {

        JsonObject payload = event.payloadAsJsonObject();

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("----------------------event name------------ {}", event.metadata().name());
        }

        final String eventName = event.metadata().name().toLowerCase();
        for (final ResultsEventTransformer resultsEventTransformer : transformFactory.getEventTransformer(eventName)) {
            payload = resultsEventTransformer.transform(event.metadata(), payload);
        }

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("-------------------transformedPayload---------------{}", payload);
        }

        return of(envelopeFrom(metadataFrom(event.metadata()), payload));
    }

    @Override
    public void setEnveloper(final Enveloper enveloper) {
        this.enveloper = enveloper;
    }
}
