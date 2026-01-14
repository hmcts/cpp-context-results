package uk.gov.moj.cpp.results.event.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.results.event.service.ProgressionService;

import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonReader;
import static javax.json.Json.createReader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class StagingEnforcementAcknowledgmentEventProcessorTest {

    private static final String JSON_PAYLOAD = """
        {
            "inactiveMigratedCaseSummaries": [
                {
                    "inactiveCaseSummary": {
                        "id": "b00acc1c-eb69-4b3c-960e-76be9153125a",
                        "defendants": [
                            {
                                "defendantId": "1a9176f4-3adc-4ea1-a808-26c4632f38ab",
                                "convictingcourtId": "f8254db1-1683-483e-afb3-b87fde5a0a26",
                                "masterDefendantId": "1a9176f4-3adc-4ea1-a808-26c4632f38ab"
                            }
                        ],
                        "migrationSourceSystem": {
                            "migrationCaseStatus": "INACTIVE",
                            "migrationSourceSystemName": "XHIBIT",
                            "defendantFineAccountNumbers": [
                                {
                                    "defendantId": "1a9176f4-3adc-4ea1-a808-26c4632f38ab",
                                    "fineAccountNumber": "12345"
                                }
                            ],
                            "migrationSourceSystemCaseIdentifier": "T20250001"
                        }
                    }
                },
                {
                    "inactiveCaseSummary": {
                        "id": "7776f4-3adc-4ea1-a808-26c4632f38ab",
                        "defendants": [
                            {
                                "defendantId": "999176f4-3adc-4ea1-a808-26c4632f38ab",
                                "convictingcourtId": "f8254db1-1683-483e-afb3-b87fde5a0a26",
                                "masterDefendantId": "1a9176f4-3adc-4ea1-a808-26c4632f38ab"
                            }
                        ],
                        "migrationSourceSystem": {
                            "migrationCaseStatus": "INACTIVE",
                            "migrationSourceSystemName": "XHIBIT",
                            "defendantFineAccountNumbers": [
                                {
                                    "defendantId": "999176f4-3adc-4ea1-a808-26c4632f38ab",
                                    "fineAccountNumber": "67890"
                                }
                            ],
                            "migrationSourceSystemCaseIdentifier": "T20259999"
                        }
                    }
                }
            ]
        }
        """;

    @Mock
    private Sender sender;

    @Mock
    private ProgressionService progressionService;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeArgumentCaptor;

    @InjectMocks
    private StagingEnforcementAcknowledgmentEventProcessor stagingEnforcementAcknowledgmentEventProcessor;

    @Test
    public void shouldUpdateGobAccount() {
        final String requestId = randomUUID().toString();
        final JsonObject enforcementResponsePayload = createObjectBuilder()
                .add("originator", "Courts")
                .add("requestId", requestId)
                .add("exportStatus", "ENFORCEMENT_ACKNOWLEDGED")
                .add("acknowledgement", createObjectBuilder().add("accountNumber", "201366829").build())
                .build();

        final JsonEnvelope event = JsonEnvelope.envelopeFrom(
                metadataWithRandomUUID("public.stagingenforcement.enforce-financial-imposition-acknowledgement"), enforcementResponsePayload);


        stagingEnforcementAcknowledgmentEventProcessor.processAcknowledgement(event);

        verify(sender).sendAsAdmin(envelopeArgumentCaptor.capture());
        final Envelope<JsonObject> argumentCaptor = envelopeArgumentCaptor.getValue();
        final JsonEnvelope allValues = envelopeFrom(argumentCaptor.metadata(), argumentCaptor.payload());
        assertThat(allValues,
                jsonEnvelope(
                        metadata().withName("result.command.update-gob-account"),
                        payloadIsJson(allOf(
                                withJsonPath("$.accountNumber", is("201366829")),
                                withJsonPath("$.correlationId", is(requestId))
                        ))));
    }

    @Test
    public void shouldUpdateGobAccountForATCM() {
        final String requestId = randomUUID().toString();
        final JsonObject enforcementResponsePayload = createObjectBuilder()
                .add("originator", "ATCM")
                .add("requestId", requestId)
                .add("exportStatus", "ENFORCEMENT_ACKNOWLEDGED")
                .add("acknowledgement", createObjectBuilder().add("accountNumber", "201366829").build())
                .build();

        final JsonEnvelope event = JsonEnvelope.envelopeFrom(
                metadataWithRandomUUID("public.stagingenforcement.enforce-financial-imposition-acknowledgement"), enforcementResponsePayload);


        stagingEnforcementAcknowledgmentEventProcessor.processAcknowledgement(event);

        verify(sender).sendAsAdmin(envelopeArgumentCaptor.capture());
        final Envelope<JsonObject> argumentCaptor = envelopeArgumentCaptor.getValue();
        final JsonEnvelope allValues = envelopeFrom(argumentCaptor.metadata(), argumentCaptor.payload());
        assertThat(allValues,
                jsonEnvelope(
                        metadata().withName("result.command.update-gob-account"),
                        payloadIsJson(allOf(
                                withJsonPath("$.accountNumber", is("201366829")),
                                withJsonPath("$.correlationId", is(requestId))
                        ))));
    }

    @Test
    public void shouldNotCallCommandWhenOriginatorIsNotCourts() {
        final String requestId = randomUUID().toString();
        final JsonObject enforcementResponsePayload = createObjectBuilder()
                .add("originator", "XXXXX")
                .add("requestId", requestId)
                .add("exportStatus", "ENFORCEMENT_ACKNOWLEDGED")
                .add("acknowledgement", createObjectBuilder().add("accountNumber", "201366829").build())
                .build();

        final JsonEnvelope event = JsonEnvelope.envelopeFrom(
                metadataWithRandomUUID("public.stagingenforcement.enforce-financial-imposition-acknowledgement"), enforcementResponsePayload);


        stagingEnforcementAcknowledgmentEventProcessor.processAcknowledgement(event);

        verify(sender, times(0)).sendAsAdmin(envelopeArgumentCaptor.capture());
    }

    @Test
    public void shouldNotCallCommandWhenAcknowledgmentIsFail() {
        final String requestId = randomUUID().toString();
        final JsonObject enforcementResponsePayload = createObjectBuilder()
                .add("originator", "Courts")
                .add("requestId", requestId)
                .add("exportStatus", "ENFORCEMENT_ACKNOWLEDGEMENT_FAILED")
                .add("acknowledgement", createObjectBuilder().add("errorCode", "errorCode").build())
                .build();

        final JsonEnvelope event = JsonEnvelope.envelopeFrom(
                metadataWithRandomUUID("public.stagingenforcement.enforce-financial-imposition-acknowledgement"), enforcementResponsePayload);


        stagingEnforcementAcknowledgmentEventProcessor.processAcknowledgement(event);

        verify(sender, times(0)).sendAsAdmin(envelopeArgumentCaptor.capture());
    }

    @Test
    public void shouldCallUpdatecorrelationId() {
        final String masterDefendantId = randomUUID().toString();
        final String accountCorrelationId = randomUUID().toString();

        final JsonObject enforcementResponsePayload = createObjectBuilder()
                .add("hearingFinancialResultRequest", createObjectBuilder()
                        .add("accountCorrelationId", accountCorrelationId)
                        .add("masterDefendantId", masterDefendantId))
                .build();

        final JsonEnvelope event = JsonEnvelope.envelopeFrom(
                metadataWithRandomUUID("results.event.hearing-financial-results-tracked"), enforcementResponsePayload);

        stagingEnforcementAcknowledgmentEventProcessor.processHearingFinancialResultsTracked(event);

        verify(sender).sendAsAdmin(envelopeArgumentCaptor.capture());
        final Envelope<JsonObject> argumentCaptor = envelopeArgumentCaptor.getValue();
        final JsonEnvelope allValues = envelopeFrom(argumentCaptor.metadata(), argumentCaptor.payload());
        assertThat(allValues,
                jsonEnvelope(
                        metadata().withName("result.command.add-correlation-id"),
                        payloadIsJson(allOf(
                                withJsonPath("$.masterDefendantId", is(masterDefendantId)),
                                withJsonPath("$.correlationId", is(accountCorrelationId))
                        ))));

    }

    @Test
    void shouldProcessSendNcesMailForNewApplication() {
        final String masterDefendantId = "1a9176f4-3adc-4ea1-a808-26c4632f38ab";
        final String caseId1 = "b00acc1c-eb69-4b3c-960e-76be9153125a";
        final String caseId2 = "7776f4-3adc-4ea1-a808-26c4632f38ab";
        final String caseId3 = "b10acc1c-eb69-4b3c-960e-76be9153125a";

        final JsonObject notificationPayload = createObjectBuilder()
                .add("masterDefendantId", masterDefendantId)
                .add("caseIds", createCaseIds(caseId1, caseId2, caseId3))
                .build();

        final JsonReader jsonReader = createReader(new StringReader(JSON_PAYLOAD));
        final JsonObject progressionResponse = jsonReader.readObject();

        when(progressionService.getInactiveMigratedCasesByCaseIds(List.of(caseId1, caseId2, caseId3)))
                .thenReturn(Optional.of(progressionResponse));

        final JsonEnvelope event = JsonEnvelope.envelopeFrom(
                metadataWithRandomUUID("public.hearing.nces-email-notification-for-application"), notificationPayload);
        stagingEnforcementAcknowledgmentEventProcessor.processSendNcesMailForNewApplication(event);

        verify(progressionService).getInactiveMigratedCasesByCaseIds(List.of(caseId1, caseId2, caseId3));
        
        verify(sender).sendAsAdmin(envelopeArgumentCaptor.capture());
        final Envelope<JsonObject> capturedEnvelope = envelopeArgumentCaptor.getValue();
        final JsonEnvelope allValues = envelopeFrom(capturedEnvelope.metadata(), capturedEnvelope.payload());
        assertThat(allValues,
                jsonEnvelope(
                        metadata().withName("result.command.send-nces-email-for-application"),
                        payloadIsJson(allOf(
                                withJsonPath("$.masterDefendantId", is(masterDefendantId)),
                                withJsonPath("$.caseIds[0]", is(caseId1)),
                                withJsonPath("$.caseIds[1]", is(caseId2)),
                                withJsonPath("$.caseIds[2]", is(caseId3)),
                                withJsonPath("$.migratedMasterDefendantCourtEmailAndFineAccount.courtEmail", is("court@email.com")),
                                withJsonPath("$.migratedMasterDefendantCourtEmailAndFineAccount.fineAccountNumber", is("12345"))
                        ))));
    }

    private JsonArrayBuilder createCaseIds(String... caseIds) {
        final JsonArrayBuilder builder = createArrayBuilder();
        Arrays.stream(caseIds).forEach(builder::add);
        return builder;
    }
}
