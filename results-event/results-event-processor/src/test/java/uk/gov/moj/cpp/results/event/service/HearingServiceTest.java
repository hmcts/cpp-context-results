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
public class HearingServiceTest {

    private static final String FIELD_HEARING_ID = "hearingId";
    private static final String FIELD_CASE_ID = "caseId";

    @Mock
    private Requester requester;

    @Spy
    private Enveloper enveloper = createEnveloper();

    @InjectMocks
    private HearingService hearingService;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor;

    @Test
    public void shouldRequestForHearingDetailsById() {
        final UUID hearingId = randomUUID();
        final JsonEnvelope event = envelope()
                .with(metadataWithRandomUUIDAndName())
                .withPayloadOf(hearingId, FIELD_HEARING_ID)
                .build();

        hearingService.getHearingById(hearingId, event);

        verify(requester).requestAsAdmin(envelopeArgumentCaptor.capture());
        assertThat(envelopeArgumentCaptor.getValue(), is(jsonEnvelope(
                withMetadataEnvelopedFrom(event)
                        .withName("hearing.get.hearing"),
                payloadIsJson(
                        withJsonPath("$.hearingId", equalTo(hearingId.toString()))
                ))
        ));
        verifyNoMoreInteractions(requester);
    }

    @Test
    public void shouldRequestForDefenceCounselsByHearingId() {
        final UUID hearingId = randomUUID();
        final JsonEnvelope event = envelope()
                .with(metadataWithRandomUUIDAndName())
                .withPayloadOf(hearingId, FIELD_HEARING_ID)
                .build();

        hearingService.getDefenceCounselsByHearingId(hearingId, event);

        verify(requester).requestAsAdmin(envelopeArgumentCaptor.capture());
        assertThat(envelopeArgumentCaptor.getValue(), is(jsonEnvelope(
                withMetadataEnvelopedFrom(event)
                        .withName("hearing.get.defence-counsels"),
                payloadIsJson(
                        withJsonPath("$.hearingId", equalTo(hearingId.toString()))
                ))
        ));
        verifyNoMoreInteractions(requester);
    }

    @Test
    public void shouldRequestForProsecutionCounselsByHearingId() {
        final UUID hearingId = randomUUID();
        final JsonEnvelope event = envelope()
                .with(metadataWithRandomUUIDAndName())
                .withPayloadOf(hearingId, FIELD_HEARING_ID)
                .build();

        hearingService.getProsecutionCounselsByHearingId(hearingId, event);

        verify(requester).requestAsAdmin(envelopeArgumentCaptor.capture());
        assertThat(envelopeArgumentCaptor.getValue(), is(jsonEnvelope(
                withMetadataEnvelopedFrom(event)
                        .withName("hearing.get.prosecution-counsels"),
                payloadIsJson(
                        withJsonPath("$.hearingId", equalTo(hearingId.toString()))
                ))
        ));
        verifyNoMoreInteractions(requester);
    }

    @Test
    public void shouldRequestForPleasByCaseId() {
        final UUID caseId = randomUUID();
        final JsonEnvelope event = envelope()
                .with(metadataWithRandomUUIDAndName())
                .withPayloadOf(caseId, FIELD_CASE_ID)
                .build();

        hearingService.getPleasByCaseId(caseId, event);

        verify(requester).requestAsAdmin(envelopeArgumentCaptor.capture());
        assertThat(envelopeArgumentCaptor.getValue(), is(jsonEnvelope(
                withMetadataEnvelopedFrom(event)
                        .withName("hearing.get.case.pleas"),
                payloadIsJson(
                        withJsonPath("$.caseId", equalTo(caseId.toString()))
                ))
        ));
        verifyNoMoreInteractions(requester);
    }
}