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

import uk.gov.justice.core.courts.HearingFinancialResultsUpdated;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.results.persist.DefendantGobAccountsEntity;
import uk.gov.moj.cpp.results.persist.DefendantGobAccountsRepository;

import java.util.Collections;

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
class HearingFinancialResultsUpdatedListenerTest {

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private DefendantGobAccountsRepository defendantGobAccountsRepository;

    @InjectMocks
    private HearingFinancialResultsUpdatedListener hearingFinancialResultsUpdatedListener;

    @Captor
    private ArgumentCaptor<DefendantGobAccountsEntity> hearingFinancialDetailsEntityArgumentCaptor;

    @BeforeEach
    public void setup() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldHandleEmailToNcesNotificationRequested() {
        HearingFinancialResultsUpdated hearingFinancialResultsUpdated = hearingFinancialResultsUpdated();
        JsonEnvelope jsonEnvelope = createJsonEnvelope(hearingFinancialResultsUpdated);
        hearingFinancialResultsUpdatedListener.handleDefendantGobAccounts(jsonEnvelope);

        verify(defendantGobAccountsRepository).save(this.hearingFinancialDetailsEntityArgumentCaptor.capture());

        assertThat(hearingFinancialDetailsEntityArgumentCaptor.getAllValues(), is(notNullValue()));
        assertThat(hearingFinancialDetailsEntityArgumentCaptor.getAllValues().size(), is(1));
        assertThat(hearingFinancialDetailsEntityArgumentCaptor.getAllValues().get(0).getId(), is(notNullValue()));
        assertThat(hearingFinancialDetailsEntityArgumentCaptor.getAllValues().get(0).getMasterDefendantId(), is(hearingFinancialResultsUpdated.getMasterDefendantId()));
        assertThat(hearingFinancialDetailsEntityArgumentCaptor.getAllValues().get(0).getCorrelationId(), is(hearingFinancialResultsUpdated.getCorrelationId()));
        assertThat(hearingFinancialDetailsEntityArgumentCaptor.getAllValues().get(0).getCaseReferences(), is(hearingFinancialResultsUpdated.getCaseReferences().toString()));
        assertThat(hearingFinancialDetailsEntityArgumentCaptor.getAllValues().get(0).getAccountNumber(), is(hearingFinancialResultsUpdated.getAccountNumber()));
        assertThat(hearingFinancialDetailsEntityArgumentCaptor.getAllValues().get(0).getCreatedDateTime(), is(notNullValue()));

    }

    private HearingFinancialResultsUpdated hearingFinancialResultsUpdated() {
        return HearingFinancialResultsUpdated.hearingFinancialResultsUpdated()
                .withCorrelationId(randomUUID())
                .withMasterDefendantId(randomUUID())
                .withAccountNumber("accountNumber1")
                .withCaseReferences(Collections.singletonList("caseRef1, caseRef2"))
                .withCreatedDateTime(now(UTC))
                .build();
    }

    private JsonEnvelope createJsonEnvelope(HearingFinancialResultsUpdated hearingFinancialResultsUpdated) {
        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withName("results.event.hearing-financial-results-updated")
                .build();

        return envelopeFrom(metadata,
                objectToJsonObjectConverter.convert(hearingFinancialResultsUpdated));
    }
}