package uk.gov.moj.cpp.results.event.helper;

import static java.util.Optional.of;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.core.courts.BailStatus;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class BailStatusConverterTest {

    private static final String BAIL_STATUS = "A";

    @InjectMocks
    BailStatusConverter bailStatusConverter;

    @Mock
    private ReferenceCache referenceCache;

    @Mock
    private BailStatus bailStatusMock;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor;

    @Test
    public void convert() {
        when(referenceCache.getBailStatusObjectByCode(any(), any())).thenReturn(of(bailStatusMock));
        final Optional<BailStatus> bailStatus = bailStatusConverter.convert(BAIL_STATUS);

        verify(referenceCache).getBailStatusObjectByCode(envelopeArgumentCaptor.capture(), eq(BAIL_STATUS));

        final JsonEnvelope jsonEnvelope = envelopeArgumentCaptor.getValue();
        assertThat(jsonEnvelope.metadata().name(), is("public.sjp.case-resulted"));
        assertThat(bailStatus, is(of(bailStatusMock)));
    }
}