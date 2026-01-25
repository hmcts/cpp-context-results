package uk.gov.moj.cpp.results.event.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static javax.json.Json.createReader;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import uk.gov.moj.cpp.results.event.service.ReferenceDataService;

import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonReader;

import com.google.common.io.Resources;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class StagingEnforcementAcknowledgmentEventProcessorTest {

    @Mock
    private Sender sender;

    @Mock
    private ProgressionService progressionService;

    @Mock
    private ReferenceDataService referenceDataService;

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
        // GIVEN
        final String masterDefendantId = "1a9176f4-3adc-4ea1-a808-26c4632f38ab";
        final String caseId1 = "b00acc1c-eb69-4b3c-960e-76be9153125a";
        final String caseId2 = "7776f4-3adc-4ea1-a808-26c4632f38ab";
        final String caseId3 = "b10acc1c-eb69-4b3c-960e-76be9153125a";
        final String hearingCourtCentreId = "faa91bb2-19cb-384b-bcc1-06d31d12cc67";

        final JsonObject notificationPayload = createObjectBuilder()
                .add("masterDefendantId", masterDefendantId)
                .add("caseIds", createCaseIds(caseId1, caseId2, caseId3))
                .add("hearingCourtCentreId", hearingCourtCentreId)
                .build();

        final JsonObject progressionResponse = getPayload("inactive-migrated-cases.json");

        when(progressionService.getInactiveMigratedCasesByCaseIds(List.of(caseId1, caseId2, caseId3)))
                .thenReturn(Optional.of(progressionResponse));

        final JsonObject payload = getPayload("organisation-units.json");
        when(referenceDataService.getOrganisationUnit(eq(hearingCourtCentreId), any())).thenReturn(payload);

        final JsonEnvelope event = JsonEnvelope.envelopeFrom(
                metadataWithRandomUUID("public.hearing.nces-email-notification-for-application"), notificationPayload);

        // WHEN
        stagingEnforcementAcknowledgmentEventProcessor.processSendNcesMailForNewApplication(event);

        // THEN
        verify(progressionService).getInactiveMigratedCasesByCaseIds(List.of(caseId1, caseId2, caseId3));

        // We expect 4: (Senior Case 1) + (Junior Case 1) + (Senior Case 2) + (Original Request)
        verify(sender, times(4)).sendAsAdmin(envelopeArgumentCaptor.capture());

        List<Envelope<JsonObject>> allEnvelopes = envelopeArgumentCaptor.getAllValues();

        // 1. Assert Garfield Senior (Case 1)
        assertThat(JsonEnvelope.envelopeFrom(allEnvelopes.get(0).metadata(), allEnvelopes.get(0).payload()),
                jsonEnvelope(
                        metadata().withName("result.command.send-migrated-inactive-nces-email-for-application"),
                        payloadIsJson(allOf(
                                withJsonPath("$.masterDefendantId", is(masterDefendantId)),
                                withJsonPath("$.migratedMasterDefendantCourtEmailAndFineAccount.caseId", is(caseId1)),
                                withJsonPath("$.migratedMasterDefendantCourtEmailAndFineAccount.fineAccountNumber", is("12345")),
                                withJsonPath("$.migratedMasterDefendantCourtEmailAndFineAccount.defendantName", is("Garfield Dare")),
                                // Fixed address to match JSON data exactly
                                withJsonPath("$.migratedMasterDefendantCourtEmailAndFineAccount.defendantAddress", is("59 Meadow Lane B1 1AA")),
                                withJsonPath("$.migratedMasterDefendantCourtEmailAndFineAccount.defendantEmail", is("test@gmail.com")),
                                withJsonPath("$.migratedMasterDefendantCourtEmailAndFineAccount.defendantDateOfBirth", is("1998-08-23"))
                        ))));

        // 2. Assert Garfield Junior (Case 1) - PROVES THE MULTI-DEFENDANT FIX
        assertThat(JsonEnvelope.envelopeFrom(allEnvelopes.get(1).metadata(), allEnvelopes.get(1).payload()),
                jsonEnvelope(
                        metadata().withName("result.command.send-migrated-inactive-nces-email-for-application"),
                        payloadIsJson(allOf(
                                withJsonPath("$.migratedMasterDefendantCourtEmailAndFineAccount.caseId", is(caseId1)),
                                withJsonPath("$.migratedMasterDefendantCourtEmailAndFineAccount.fineAccountNumber", is("54321")),
                                withJsonPath("$.migratedMasterDefendantCourtEmailAndFineAccount.defendantName", is("Garfield (Junior) Dare")),
                                withJsonPath("$.migratedMasterDefendantCourtEmailAndFineAccount.defendantDateOfBirth", is("2020-01-01")),
                                withJsonPath("$.migratedMasterDefendantCourtEmailAndFineAccount.defendantEmail", is("junior@gmail.com"))
                        ))));

        // 3. Assert Garfield Senior (Case 2)
        assertThat(JsonEnvelope.envelopeFrom(allEnvelopes.get(2).metadata(), allEnvelopes.get(2).payload()),
                jsonEnvelope(
                        metadata().withName("result.command.send-migrated-inactive-nces-email-for-application"),
                        payloadIsJson(allOf(
                                withJsonPath("$.migratedMasterDefendantCourtEmailAndFineAccount.caseId", is(caseId2)),
                                withJsonPath("$.migratedMasterDefendantCourtEmailAndFineAccount.fineAccountNumber", is("67890")),
                                withJsonPath("$.migratedMasterDefendantCourtEmailAndFineAccount.defendantName", is("Garfield Dare"))
                        ))));

        // 4. Assert Original Request Envelope
        assertThat(allEnvelopes.get(3).metadata().name(), is("result.command.send-nces-email-for-application"));
    }

    private JsonArrayBuilder createCaseIds(String... caseIds) {
        final JsonArrayBuilder builder = createArrayBuilder();
        Arrays.stream(caseIds).forEach(builder::add);
        return builder;
    }

    private static JsonObject getPayload(final String path) {
        String request = null;
        try {
            request = Resources.toString(
                    Resources.getResource(path),
                    Charset.defaultCharset()
            );
        } catch (final Exception e) {
            fail("Error consuming file from location " + path);
        }
        final JsonReader reader = Json.createReader(new StringReader(request));
        return reader.readObject();
    }
}
