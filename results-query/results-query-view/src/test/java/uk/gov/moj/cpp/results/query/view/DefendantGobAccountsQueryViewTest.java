package uk.gov.moj.cpp.results.query.view;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
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
public class DefendantGobAccountsQueryViewTest {

    @Mock
    private DefendantGobAccountsRepository defendantGobAccountsRepository;

    @InjectMocks
    private DefendantGobAccountsQueryView defendantGobAccountsQueryView;

    @Test
    public void shouldGetDefendantGobAccounts() throws Exception {
        final UUID masterDefendantId = randomUUID();
        final UUID accountCorrelationId = randomUUID();
        final UUID hearingId = randomUUID();
        final String storedCaseReferences = "[\"caseRef1\", \"caseRef2\"]"; // JSON array stored in entity
        final ZonedDateTime accountRequestTime = ZonedDateTime.now();
        
        final DefendantGobAccountsEntity entity = new DefendantGobAccountsEntity(masterDefendantId, accountCorrelationId);
        entity.setHearingId(hearingId);
        entity.setAccountNumber("ACC123456789");
        entity.setCaseReferences(storedCaseReferences);
        entity.setAccountRequestTime(accountRequestTime);

        final Metadata metadata = mock(Metadata.class);
        final JsonEnvelope envelope = mock(JsonEnvelope.class);
        final JsonObject payload = mock(JsonObject.class);

        when(envelope.metadata()).thenReturn(metadata);
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(payload.getString("masterDefendantId")).thenReturn(masterDefendantId.toString());
        when(payload.getString("hearingId")).thenReturn(hearingId.toString());
        when(defendantGobAccountsRepository.findAccountNumberByMasterDefendantIdAndHearingId(masterDefendantId, hearingId)).thenReturn(entity);

        final JsonEnvelope result = defendantGobAccountsQueryView.getDefendantGobAccounts(envelope);

        assertThat(result.metadata(), is(metadata));
        
        final JsonObject resultPayload = result.payloadAsJsonObject();
        assertThat(resultPayload.getString("masterDefendantId"), is(masterDefendantId.toString()));
        assertThat(resultPayload.getString("accountCorrelationId"), is(accountCorrelationId.toString()));
        assertThat(resultPayload.getString("hearingId"), is(hearingId.toString()));
        assertThat(resultPayload.getString("accountNumber"), is("ACC123456789"));
        assertThat(resultPayload.getString("caseReferences"), is(storedCaseReferences));
        assertThat(resultPayload.getString("accountRequestTime"), is(accountRequestTime.toString()));
    }
}
