package uk.gov.moj.cpp.results.event;

import static java.time.ZonedDateTime.now;
import static java.time.ZoneOffset.UTC;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.hearing.courts.HearingFinancialResultsTracked;
import uk.gov.justice.hearing.courts.HearingFinancialResultRequest;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.results.persist.DefendantGobAccountsEntity;
import uk.gov.moj.cpp.results.persist.DefendantGobAccountsRepository;

import java.util.Arrays;
import java.util.List;

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
class HearingFinancialResultsTrackedListenerTest {

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private DefendantGobAccountsRepository defendantGobAccountsRepository;

    @InjectMocks
    private HearingFinancialResultsTrackedListener hearingFinancialResultsTrackedListener;

    @Captor
    private ArgumentCaptor<DefendantGobAccountsEntity> defendantGobAccountsEntityArgumentCaptor;

    @BeforeEach
    public void setup() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldProcessDefendantGobAccounts() {
        HearingFinancialResultsTracked hearingFinancialResultsTracked = hearingFinancialResultsTracked();
        JsonEnvelope jsonEnvelope = createJsonEnvelope(hearingFinancialResultsTracked);
        hearingFinancialResultsTrackedListener.processDefendantGobAccounts(jsonEnvelope);

        verify(defendantGobAccountsRepository).save(this.defendantGobAccountsEntityArgumentCaptor.capture());

        assertThat(defendantGobAccountsEntityArgumentCaptor.getAllValues(), is(notNullValue()));
        assertThat(defendantGobAccountsEntityArgumentCaptor.getAllValues().size(), is(1));
        
        DefendantGobAccountsEntity capturedEntity = defendantGobAccountsEntityArgumentCaptor.getAllValues().get(0);
        
        assertThat(capturedEntity.getId(), is(notNullValue()));
        assertThat(capturedEntity.getMasterDefendantId(), is(hearingFinancialResultsTracked.getHearingFinancialResultRequest().getMasterDefendantId()));
        assertThat(capturedEntity.getCorrelationId(), is(hearingFinancialResultsTracked.getHearingFinancialResultRequest().getAccountCorrelationId()));
        assertThat(capturedEntity.getHearingId(), is(hearingFinancialResultsTracked.getHearingFinancialResultRequest().getHearingId()));
        assertThat(capturedEntity.getCaseReferences(), is(notNullValue()));
        assertThat(capturedEntity.getCreatedTime(), is(notNullValue()));
        
        // Verify case references are stored as JSON array string
        String storedCaseReferences = capturedEntity.getCaseReferences();
        assertThat(storedCaseReferences.startsWith("["), is(true));
        assertThat(storedCaseReferences.endsWith("]"), is(true));
        assertThat(storedCaseReferences.contains("caseRef1"), is(true));
        assertThat(storedCaseReferences.contains("caseRef2"), is(true));
    }

    @Test
    public void shouldHandleNullCaseReferences() {
        HearingFinancialResultsTracked hearingFinancialResultsTracked = hearingFinancialResultsTrackedWithNullCaseReferences();
        JsonEnvelope jsonEnvelope = createJsonEnvelope(hearingFinancialResultsTracked);
        hearingFinancialResultsTrackedListener.processDefendantGobAccounts(jsonEnvelope);

        verify(defendantGobAccountsRepository).save(this.defendantGobAccountsEntityArgumentCaptor.capture());

        DefendantGobAccountsEntity capturedEntity = defendantGobAccountsEntityArgumentCaptor.getAllValues().get(0);
        
        // When case references are null, should store empty JSON array
        assertThat(capturedEntity.getCaseReferences(), is("[]"));
    }

    private HearingFinancialResultsTracked hearingFinancialResultsTracked() {
        List<String> caseReferences = Arrays.asList("caseRef1", "caseRef2");
        
        HearingFinancialResultRequest request = HearingFinancialResultRequest.hearingFinancialResultRequest()
                .withMasterDefendantId(randomUUID())
                .withAccountCorrelationId(randomUUID())
                .withHearingId(randomUUID())
                .withProsecutionCaseReferences(caseReferences)
                .build();

        return HearingFinancialResultsTracked.hearingFinancialResultsTracked()
                .withHearingFinancialResultRequest(request)
                .withCreatedTime(now(UTC))
                .build();
    }

    private HearingFinancialResultsTracked hearingFinancialResultsTrackedWithNullCaseReferences() {
        HearingFinancialResultRequest request = HearingFinancialResultRequest.hearingFinancialResultRequest()
                .withMasterDefendantId(randomUUID())
                .withAccountCorrelationId(randomUUID())
                .withHearingId(randomUUID())
                .withProsecutionCaseReferences(null)
                .build();

        return HearingFinancialResultsTracked.hearingFinancialResultsTracked()
                .withHearingFinancialResultRequest(request)
                .withCreatedTime(now(UTC))
                .build();
    }

    private JsonEnvelope createJsonEnvelope(HearingFinancialResultsTracked hearingFinancialResultsTracked) {
        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withName("results.event.hearing-financial-results-tracked")
                .build();

        return envelopeFrom(metadata,
                objectToJsonObjectConverter.convert(hearingFinancialResultsTracked));
    }
}
