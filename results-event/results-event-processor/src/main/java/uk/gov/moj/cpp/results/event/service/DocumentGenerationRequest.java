package uk.gov.moj.cpp.results.event.service;

import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class DocumentGenerationRequest {

    private final TemplateIdentifier templateIdentifier;
    private final ConversionFormat conversionFormat;
    private final String sourceCorrelationId;
    private final UUID payloadFileServiceId;
    private final String payloadFileServiceUri;
    private final String documentFileServiceUri;
    private final String originatingSource;

    private final Map<String, String> additionalInformation;

    public DocumentGenerationRequest(final String originatingSource,
                                     final TemplateIdentifier templateIdentifier,
                                     final ConversionFormat conversionFormat,
                                     final String sourceCorrelationId,
                                     final UUID payloadFileServiceId,
                                     final Map<String, String> additionalInformation) {
        this.originatingSource = originatingSource;
        this.templateIdentifier = templateIdentifier;
        this.conversionFormat = conversionFormat;
        this.sourceCorrelationId = sourceCorrelationId;
        this.payloadFileServiceId = payloadFileServiceId;
        this.additionalInformation = additionalInformation;
        this.payloadFileServiceUri = null;
        this.documentFileServiceUri = null;
    }

    public DocumentGenerationRequest(final String originatingSource,
                                     final TemplateIdentifier templateIdentifier,
                                     final ConversionFormat conversionFormat,
                                     final String sourceCorrelationId,
                                     final Map<String, String> additionalInformation,
                                     final String payloadFileServiceUri,
                                     final String documentFileServiceUri) {
        this.originatingSource = originatingSource;
        this.templateIdentifier = templateIdentifier;
        this.conversionFormat = conversionFormat;
        this.sourceCorrelationId = sourceCorrelationId;
        this.payloadFileServiceId = null;
        this.additionalInformation = additionalInformation;
        this.payloadFileServiceUri = payloadFileServiceUri;
        this.documentFileServiceUri = documentFileServiceUri;
    }

    public Map<String, String> getAdditionalInformation() {
        return additionalInformation;
    }

    public String getOriginatingSource() {
        return originatingSource;
    }

    public TemplateIdentifier getTemplateIdentifier() {
        return templateIdentifier;
    }

    public ConversionFormat getConversionFormat() {
        return conversionFormat;
    }

    public String getSourceCorrelationId() {
        return sourceCorrelationId;
    }

    public UUID getPayloadFileServiceId() {
        return payloadFileServiceId;
    }

    public String getPayloadFileServiceUri() {
        return payloadFileServiceUri;
    }

    public String getDocumentFileServiceUri() {
        return documentFileServiceUri;
    }

    @Override
    public boolean equals(final Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
