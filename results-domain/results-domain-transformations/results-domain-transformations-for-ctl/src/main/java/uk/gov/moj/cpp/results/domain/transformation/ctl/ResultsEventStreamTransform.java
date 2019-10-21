package uk.gov.moj.cpp.results.domain.transformation.ctl;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.stream.Stream.of;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.NO_ACTION;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.TRANSFORM;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.tools.eventsourcing.transformation.api.Action;
import uk.gov.justice.tools.eventsourcing.transformation.api.EventTransformation;
import uk.gov.justice.tools.eventsourcing.transformation.api.annotation.Transformation;

import java.util.List;
import java.util.stream.Stream;

import javax.json.JsonObject;

import org.slf4j.Logger;

@Transformation
@SuppressWarnings({"pmd:BeanMembersShouldSerialize Severity", "squid:S1450"})
public class ResultsEventStreamTransform implements EventTransformation {

    public static final String HEARING_RESULTS_ADDED = "results.hearing-results-added";

    protected static final List<String> eventsToTransform = newArrayList(HEARING_RESULTS_ADDED);

    private Enveloper enveloper;

    private BailStatusEnum2ObjectTransformer bailStatusTransformer;

    private static final Logger LOGGER = getLogger(ResultsEventStreamTransform.class);

    public ResultsEventStreamTransform() {
        bailStatusTransformer = new BailStatusEnum2ObjectTransformer();
    }

    @Override
    public Action actionFor(final JsonEnvelope event) {
        if (eventsToTransform.stream().anyMatch(eventToTransform -> event.metadata().name().equalsIgnoreCase(eventToTransform))) {
            return TRANSFORM;
        }

        return NO_ACTION;
    }

    @Override
    public Stream<JsonEnvelope> apply(final JsonEnvelope event) {

        final JsonObject payload = event.payloadAsJsonObject();

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("----------------------event name------------ {}", event.metadata().name());
        }

        final JsonObject transformedPayload = bailStatusTransformer.transform(payload);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("-------------------transformedPayload---------------{}", transformedPayload);
        }

        return of(envelopeFrom(metadataFrom(event.metadata()), transformedPayload));

    }

    @Override
    public void setEnveloper(final Enveloper enveloper) {
        this.enveloper = enveloper;
    }

    public void setBailStatusTransformer(final BailStatusEnum2ObjectTransformer value) {
        this.bailStatusTransformer = value;
    }


}
