package uk.gov.moj.cpp.results.command.handler;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloperWithEvents;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataOf;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.domains.results.MigratedMasterDefendantCaseDetails;
import uk.gov.moj.cpp.results.domain.aggregate.MigratedInactiveHearingFinancialResultsAggregate;

import java.lang.reflect.Field;
import java.util.UUID;
import java.util.stream.Stream;

import javax.json.Json;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MigratedStagingEnforcementResponseHandlerTest {

    private static final String TEMPLATE_ID = "550e8400-e29b-41d4-a716-446655440000";

    @Spy
    private final Enveloper enveloper = createEnveloperWithEvents();

    @Mock
    private EventSource eventSource;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private EventStream eventStream;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);

    @Captor
    private ArgumentCaptor<UUID> eventSourceArgumentCaptor;

    @Captor
    private ArgumentCaptor<Stream<JsonEnvelope>> eventStreamArgumentCaptor;

    @InjectMocks
    private MigratedStagingEnforcementResponseHandler migratedStagingEnforcementResponseHandler;

    @BeforeEach
    public void setup() throws Exception {
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        
        // Set the ncesEmailNotificationTemplateId field using reflection
        final Field templateIdField = MigratedStagingEnforcementResponseHandler.class.getDeclaredField("ncesEmailNotificationTemplateId");
        templateIdField.setAccessible(true);
        templateIdField.set(migratedStagingEnforcementResponseHandler, TEMPLATE_ID);
    }

    @Test
    public void shouldSendNcesEmailForMigratedApplication() throws EventStreamException {
        final String masterDefendantId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final String applicationType = "STAT_DEC";
        final String listingDate = "28/12/2021";
        final String courtEmail = "court@example.com";
        final String fineAccountNumber = "FINE123";
        final String division = "6";
        final String defendantName = "John Doe";
        final String defendantEmail = "defendant@email.com";
        final String defendantDateOfBirth = "21/10/1978";

        final JsonEnvelope envelope = envelopeFrom(
                metadataOf(randomUUID(), "result.command.send-migrated-inactive-nces-email-for-application"),
                Json.createObjectBuilder()
                        .add("masterDefendantId", masterDefendantId)
                        .add("applicationType", applicationType)
                        .add("listingDate", listingDate)
                        .add("caseUrns", createArrayBuilder().add("URN1").add("URN2").build())
                        .add("hearingCourtCentreName", "Croydon Crown Court")
                        .add("migratedMasterDefendantCourtEmailAndFineAccount", Json.createObjectBuilder()
                                .add("masterDefendantId", masterDefendantId)
                                .add("caseId", caseId)
                                .add("fineAccountNumber", fineAccountNumber)
                                .add("courtEmail", courtEmail)
                                .add("division", division)
                                .add("defendantId", "defendant-id-1")
                                .add("defendantName", defendantName)
                                .add("defendantEmail", defendantEmail)
                                .add("defendantDateOfBirth", defendantDateOfBirth)
                                .add("migrationSourceSystemCaseIdentifier", "CASE123")
                                .build())
                        .build());

        final MigratedInactiveHearingFinancialResultsAggregate spyAggregate = spy(new MigratedInactiveHearingFinancialResultsAggregate());
        when(aggregateService.get(eventStream, MigratedInactiveHearingFinancialResultsAggregate.class)).thenReturn(spyAggregate);

        migratedStagingEnforcementResponseHandler.sendNcesEmailForMigratedApplication(envelope);

        verify(eventSource, times(1)).getStreamById(eventSourceArgumentCaptor.capture());
        verify(eventStream, times(1)).append(eventStreamArgumentCaptor.capture());

        final MigratedMasterDefendantCaseDetails expectedCaseDetails = MigratedMasterDefendantCaseDetails.builder()
                .withMasterDefendantId(masterDefendantId)
                .withCaseId(caseId)
                .withFineAccountNumber(fineAccountNumber)
                .withCourtEmail(courtEmail)
                .withDivision(division)
                .withDefendantId("defendant-id-1")
                .withDefendantName(defendantName)
                .withDefendantAddress("")
                .withOriginalDateOfConviction("")
                .withDefendantEmail(defendantEmail)
                .withDefendantDateOfBirth(defendantDateOfBirth)
                .withDefendantContactNumber(null)
                .withMigrationSourceSystemCaseIdentifier("CASE123")
                .withCaseURN("")
                .build();

        verify(spyAggregate).sendNcesEmailForMigratedApplication(
                eq(applicationType),
                eq("2021-12-28"),
                any(),
                eq("Croydon Crown Court"),
                eq(expectedCaseDetails));
    }

    @Test
    public void shouldProcessMigratedInactiveNcesEmailNotification() throws EventStreamException {
        final String masterDefendantId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final String materialId = randomUUID().toString();
        final String materialUrl = "https://example.com/material";

        final JsonEnvelope envelope = envelopeFrom(
                metadataOf(randomUUID(), "result.command.migrated-inactive-nces-document-notification"),
                Json.createObjectBuilder()
                        .add("masterDefendantId", masterDefendantId)
                        .add("caseId", caseId)
                        .add("materialId", materialId)
                        .add("materialUrl", materialUrl)
                        .build());

        final MigratedInactiveHearingFinancialResultsAggregate spyAggregate = spy(new MigratedInactiveHearingFinancialResultsAggregate());
        when(aggregateService.get(eventStream, MigratedInactiveHearingFinancialResultsAggregate.class)).thenReturn(spyAggregate);

        migratedStagingEnforcementResponseHandler.processMigratedInactiveNcesEmailNotification(envelope);

        verify(eventSource, times(1)).getStreamById(eventSourceArgumentCaptor.capture());
        verify(eventStream, times(1)).append(eventStreamArgumentCaptor.capture());

        verify(spyAggregate).saveMigratedInactiveNcesEmailNotificationDetails(eq(materialUrl), eq(TEMPLATE_ID));
    }
}
