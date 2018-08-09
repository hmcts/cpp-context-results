package uk.gov.moj.cpp.results.event.service;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUIDAndName;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@SuppressWarnings("unused")
@RunWith(MockitoJUnitRunner.class)
public class ReferenceDataServiceTest {

    private static final String FIELD_CJS_OFFENCE_CODE = "cjsoffencecode";

    @Mock
    private Requester requester;

    @Spy
    private Enveloper enveloper = createEnveloper();

    @InjectMocks
    private ReferenceDataService referenceDataService;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor;

    // TODO: What about queryParameter usage rather than uriParameter?
    @Test
    public void shouldRequestForOffenceByCjsOffenceCode() {
        final String cjsOffenceCode = "OF61131";
        final JsonEnvelope event = envelope()
                .with(metadataWithRandomUUIDAndName())
                .withPayloadOf(cjsOffenceCode, FIELD_CJS_OFFENCE_CODE)
                .build();
        referenceDataService.getOffenceByCjsCode(cjsOffenceCode, event);

        verify(requester).requestAsAdmin(envelopeArgumentCaptor.capture());
        assertThat(envelopeArgumentCaptor.getValue(), is(jsonEnvelope(
                withMetadataEnvelopedFrom(event)
                        .withName("referencedata.query.offences"),
                payloadIsJson(
                        withJsonPath("$.cjsoffencecode", equalTo(cjsOffenceCode))
                ))
        ));
        verifyNoMoreInteractions(requester);
    }

}