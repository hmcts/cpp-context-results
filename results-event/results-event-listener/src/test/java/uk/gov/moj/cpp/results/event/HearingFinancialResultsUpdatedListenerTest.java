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
        
        JsonEnvelope mockEnvelope = mock(JsonEnvelope.class);
        JsonObject mockPayload = mock(JsonObject.class);
        when(mockEnvelope.payloadAsJsonObject()).thenReturn(mockPayload);
        
        HearingFinancialResultsUpdated mockHearingFinancialResultsUpdated = mock(HearingFinancialResultsUpdated.class);
        when(mockHearingFinancialResultsUpdated.getMasterDefendantId()).thenReturn(masterDefendantId);
        when(mockHearingFinancialResultsUpdated.getCorrelationId()).thenReturn(correlationId);
        when(mockHearingFinancialResultsUpdated.getHearingId()).thenReturn(hearingId);
        when(mockHearingFinancialResultsUpdated.getAccountNumber()).thenReturn(accountNumber);
        when(mockHearingFinancialResultsUpdated.getAccountRequestTime()).thenReturn(accountRequestTime);
        
        when(jsonObjectToObjectConverter.convert(mockPayload, HearingFinancialResultsUpdated.class))
                .thenReturn(mockHearingFinancialResultsUpdated);
        
        DefendantGobAccountsEntity existingEntity = new DefendantGobAccountsEntity();
        existingEntity.setMasterDefendantId(masterDefendantId);
        existingEntity.setCorrelationId(correlationId);
        existingEntity.setHearingId(hearingId);
        
        when(defendantGobAccountsRepository.findByMasterDefendantIdAndHearingIdAndCorrelationId(
                masterDefendantId, hearingId, correlationId)).thenReturn(existingEntity);
        
        hearingFinancialResultsUpdatedListener.handleDefendantGobAccounts(mockEnvelope);

        verify(defendantGobAccountsRepository).save(existingEntity);
        
        assert existingEntity.getAccountNumber().equals(accountNumber);
        assert existingEntity.getAccountRequestTime().equals(accountRequestTime);
        assert existingEntity.getUpdatedTime() != null;
    }

    @Test
    public void shouldNotSaveWhenNoExistingEntityFound() throws Exception {
        // Create test data
        UUID masterDefendantId = randomUUID();
        UUID correlationId = randomUUID();
        UUID hearingId = randomUUID();
        String accountNumber = "TEST123";
        ZonedDateTime accountRequestTime = ZonedDateTime.now();
        
        JsonEnvelope mockEnvelope = mock(JsonEnvelope.class);
        JsonObject mockPayload = mock(JsonObject.class);
        when(mockEnvelope.payloadAsJsonObject()).thenReturn(mockPayload);
        
        HearingFinancialResultsUpdated mockHearingFinancialResultsUpdated = mock(HearingFinancialResultsUpdated.class);
        when(mockHearingFinancialResultsUpdated.getMasterDefendantId()).thenReturn(masterDefendantId);
        when(mockHearingFinancialResultsUpdated.getCorrelationId()).thenReturn(correlationId);
        when(mockHearingFinancialResultsUpdated.getHearingId()).thenReturn(hearingId);
        
        when(jsonObjectToObjectConverter.convert(mockPayload, HearingFinancialResultsUpdated.class))
                .thenReturn(mockHearingFinancialResultsUpdated);
        
        when(defendantGobAccountsRepository.findByMasterDefendantIdAndHearingIdAndCorrelationId(
                masterDefendantId, hearingId, correlationId)).thenReturn(null);
        
        hearingFinancialResultsUpdatedListener.handleDefendantGobAccounts(mockEnvelope);
        
        verify(defendantGobAccountsRepository, never()).save(any(DefendantGobAccountsEntity.class));
    }
}