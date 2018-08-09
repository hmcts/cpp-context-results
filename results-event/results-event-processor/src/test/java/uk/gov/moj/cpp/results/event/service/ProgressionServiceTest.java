package uk.gov.moj.cpp.results.event.service;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
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

import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.UUID;

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
public class ProgressionServiceTest {

    private static final String FIELD_CASE_ID = "caseId";

    @Mock
    private Requester requester;

    @Spy
    private Enveloper enveloper = createEnveloper();

    @InjectMocks
    private ProgressionService progressionService;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor;

    @Test
    public void shouldRequestForDefendantDetailsByCaseId() {
        final UUID caseId = randomUUID();
        final JsonEnvelope event = envelope()
                .with(metadataWithRandomUUIDAndName())
                .withPayloadOf(caseId, FIELD_CASE_ID)
                .build();

        progressionService.getDefendantsByCaseId(caseId, event);

        verify(requester).requestAsAdmin(envelopeArgumentCaptor.capture());
        assertThat(envelopeArgumentCaptor.getValue(), is(jsonEnvelope(
                withMetadataEnvelopedFrom(event)
                        .withName("progression.query.defendants"),
                payloadIsJson(
                        withJsonPath("$.caseId", equalTo(caseId.toString()))
                ))
        ));
        verifyNoMoreInteractions(requester);
    }

    @Test
    public void shouldRequestForCaseByCaseId() {
        final UUID caseId = randomUUID();
        final JsonEnvelope event = envelope()
                .with(metadataWithRandomUUIDAndName())
                .withPayloadOf(caseId, FIELD_CASE_ID)
                .build();

        progressionService.getCase(caseId, event);

        verify(requester).requestAsAdmin(envelopeArgumentCaptor.capture());
        assertThat(envelopeArgumentCaptor.getValue(), is(jsonEnvelope(
                withMetadataEnvelopedFrom(event).withName("progression.query.caseprogressiondetail"),
                payloadIsJson(withJsonPath("$.caseId", equalTo(caseId.toString()))
                ))
        ));
        verifyNoMoreInteractions(requester);
    }

}