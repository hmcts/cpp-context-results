package uk.gov.moj.cpp.results.event.service;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@SuppressWarnings("unused")
@ExtendWith(MockitoExtension.class)
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

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeJsonObjectArgumentCaptor;

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

    @Test
    public void shouldRequestForInactiveMigratedCasesByCaseIds() {
        final List<String> caseIds = asList("b00acc1c-eb69-4b3c-960e-76be9153125a", "case-id-2", "case-id-3");
        final String defendantId = "1a9176f4-3adc-4ea1-a808-26c4632f38ab";
        final String convictingCourtId = "f8254db1-1683-483e-afb3-b87fde5a0a26";
        final String fineAccountNumber = "12345";
        final String migrationSourceSystemCaseIdentifier = "T20250001";
        
        final JsonObject responsePayload = createObjectBuilder()
                .add("inactiveMigratedCaseSummaries", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("inactiveCaseSummary", createObjectBuilder()
                                        .add("id", "b00acc1c-eb69-4b3c-960e-76be9153125a")
                                        .add("defendants", createArrayBuilder()
                                                .add(createObjectBuilder()
                                                        .add("defendantId", defendantId)
                                                        .add("convictingcourtId", convictingCourtId)
                                                        .add("masterDefendantId", defendantId)
                                                        .build())
                                                .build())
                                        .add("migrationSourceSystem", createObjectBuilder()
                                                .add("migrationCaseStatus", "INACTIVE")
                                                .add("migrationSourceSystemName", "XHIBIT")
                                                .add("defendantFineAccountNumbers", createArrayBuilder()
                                                        .add(createObjectBuilder()
                                                                .add("defendantId", defendantId)
                                                                .add("fineAccountNumber", fineAccountNumber)
                                                                .build())
                                                        .build())
                                                .add("migrationSourceSystemCaseIdentifier", migrationSourceSystemCaseIdentifier)
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();

        final Envelope<JsonObject> responseEnvelope = envelopeFrom(
                metadataBuilder()
                        .withId(randomUUID())
                        .withName("progression.query.search-inactive-migrated-cases")
                        .build(),
                responsePayload);

        when(requester.requestAsAdmin(any(Envelope.class), eq(JsonObject.class)))
                .thenReturn(responseEnvelope);

        final Optional<JsonObject> result = progressionService.getInactiveMigratedCasesByCaseIds(caseIds);

        verify(requester).requestAsAdmin(envelopeJsonObjectArgumentCaptor.capture(), eq(JsonObject.class));
        final Envelope<JsonObject> capturedEnvelope = envelopeJsonObjectArgumentCaptor.getValue();
        assertThat(capturedEnvelope.metadata().name(), is("progression.query.search-inactive-migrated-cases"));
        assertThat(capturedEnvelope.payload().getString("caseIds"), is("b00acc1c-eb69-4b3c-960e-76be9153125a,case-id-2,case-id-3"));
        assertTrue(result.isPresent());
        assertThat(result.get(), is(responsePayload));
        verifyNoMoreInteractions(requester);
    }

}