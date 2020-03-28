package uk.gov.moj.cpp.results.event.helper;

import static java.util.UUID.randomUUID;
import static javax.json.JsonValue.NULL;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.core.courts.BailStatus;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.Optional;

import javax.inject.Inject;

public class BailStatusConverter implements Converter<String, Optional<BailStatus>> {

    private final ReferenceCache referenceCache;

    @Inject
    public BailStatusConverter(final ReferenceCache referenceCache) {
        this.referenceCache = referenceCache;
    }

    @Override
    public Optional<BailStatus> convert(final String sjpBailStatus) {
        final JsonEnvelope context = envelopeFrom(metadataBuilder().withName("public.sjp.case-resulted").withId(randomUUID()).build(), NULL);
        return referenceCache.getBailStatusObjectByCode(context, sjpBailStatus);
    }
}