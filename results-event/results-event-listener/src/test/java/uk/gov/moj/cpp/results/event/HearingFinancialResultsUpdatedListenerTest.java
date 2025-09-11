package uk.gov.moj.cpp.results.event;

import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.core.courts.HearingFinancialResultsUpdated;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.results.persist.DefendantGobAccountsEntity;
import uk.gov.moj.cpp.results.persist.DefendantGobAccountsRepository;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HearingFinancialResultsUpdatedListenerTest {

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private DefendantGobAccountsRepository defendantGobAccountsRepository;

    @InjectMocks
    private HearingFinancialResultsUpdatedListener hearingFinancialResultsUpdatedListener;

    @Test
    public void shouldHandleDefendantGOBAccounts() throws Exception {
        // Create test data
        UUID masterDefendantId = randomUUID();
        UUID correlationId = randomUUID();
        UUID hearingId = randomUUID();
        String accountNumber = "TEST123";
        ZonedDateTime accountRequestTime = ZonedDateTime.now();
        
        // Create mock JsonEnvelope
        JsonEnvelope mockEnvelope = mock(JsonEnvelope.class);
        JsonObject mockPayload = mock(JsonObject.class);
        when(mockEnvelope.payloadAsJsonObject()).thenReturn(mockPayload);
        
        // Create mock HearingFinancialResultsUpdated
        HearingFinancialResultsUpdated mockHearingFinancialResultsUpdated = mock(HearingFinancialResultsUpdated.class);
        when(mockHearingFinancialResultsUpdated.getMasterDefendantId()).thenReturn(masterDefendantId);
        when(mockHearingFinancialResultsUpdated.getCorrelationId()).thenReturn(correlationId);
        when(mockHearingFinancialResultsUpdated.getHearingId()).thenReturn(hearingId);
        when(mockHearingFinancialResultsUpdated.getAccountNumber()).thenReturn(accountNumber);
        when(mockHearingFinancialResultsUpdated.getAccountRequestTime()).thenReturn(accountRequestTime);
        
        // Mock the converter
        when(jsonObjectToObjectConverter.convert(mockPayload, HearingFinancialResultsUpdated.class))
                .thenReturn(mockHearingFinancialResultsUpdated);
        
        // Mock the repository to return an existing entity
        DefendantGobAccountsEntity existingEntity = new DefendantGobAccountsEntity();
        existingEntity.setId(randomUUID());
        existingEntity.setMasterDefendantId(masterDefendantId);
        existingEntity.setCorrelationId(correlationId);
        existingEntity.setHearingId(hearingId);
        
        when(defendantGobAccountsRepository.findAccountNumberByMasterDefendantIdAndHearingIdAndCorrelationId(
                masterDefendantId, hearingId, correlationId)).thenReturn(existingEntity);
        
        // Call the public method
        hearingFinancialResultsUpdatedListener.handleDefendantGobAccounts(mockEnvelope);
        
        // Verify that the repository save method was called with the existing entity
        verify(defendantGobAccountsRepository).save(existingEntity);
        
        // Verify that the entity was updated with the correct values
        assert existingEntity.getAccountNumber().equals(accountNumber);
        assert existingEntity.getAccountRequestTime().equals(accountRequestTime);
        assert existingEntity.getUpdateTime() != null;
    }

    @Test
    public void shouldNotSaveWhenNoExistingEntityFound() throws Exception {
        // Create test data
        UUID masterDefendantId = randomUUID();
        UUID correlationId = randomUUID();
        UUID hearingId = randomUUID();
        String accountNumber = "TEST123";
        ZonedDateTime accountRequestTime = ZonedDateTime.now();
        
        // Create mock JsonEnvelope
        JsonEnvelope mockEnvelope = mock(JsonEnvelope.class);
        JsonObject mockPayload = mock(JsonObject.class);
        when(mockEnvelope.payloadAsJsonObject()).thenReturn(mockPayload);
        
        // Create mock HearingFinancialResultsUpdated
        HearingFinancialResultsUpdated mockHearingFinancialResultsUpdated = mock(HearingFinancialResultsUpdated.class);
        when(mockHearingFinancialResultsUpdated.getMasterDefendantId()).thenReturn(masterDefendantId);
        when(mockHearingFinancialResultsUpdated.getCorrelationId()).thenReturn(correlationId);
        when(mockHearingFinancialResultsUpdated.getHearingId()).thenReturn(hearingId);
        
        // Mock the converter
        when(jsonObjectToObjectConverter.convert(mockPayload, HearingFinancialResultsUpdated.class))
                .thenReturn(mockHearingFinancialResultsUpdated);
        
        // Mock the repository to return null (no existing entity)
        when(defendantGobAccountsRepository.findAccountNumberByMasterDefendantIdAndHearingIdAndCorrelationId(
                masterDefendantId, hearingId, correlationId)).thenReturn(null);
        
        // Call the public method
        hearingFinancialResultsUpdatedListener.handleDefendantGobAccounts(mockEnvelope);
        
        // Verify that the repository save method was NOT called
        verify(defendantGobAccountsRepository, never()).save(any(DefendantGobAccountsEntity.class));
    }
}