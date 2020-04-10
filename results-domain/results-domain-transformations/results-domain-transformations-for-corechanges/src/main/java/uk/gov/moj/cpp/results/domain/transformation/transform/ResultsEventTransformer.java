package uk.gov.moj.cpp.results.domain.transformation.transform;

import uk.gov.justice.services.messaging.Metadata;

import javax.json.JsonObject;

public interface ResultsEventTransformer {
    JsonObject transform(final Metadata eventMetadata, final JsonObject payload);
}
