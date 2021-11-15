package uk.gov.moj.cpp.results.query.api;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.results.query.view.ResultsQueryView;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ResultsQueryApiMockitoTest {

    @Mock
    private ResultsQueryView resultsQueryView;

    @InjectMocks
    private ResultsQueryApi resultsQueryApi;

    @Test
    public void shouldGetHearingDetailsFromQueryView() throws Exception {

        final JsonEnvelope query = mock(JsonEnvelope.class);
        final JsonEnvelope result = mock(JsonEnvelope.class);

        when(resultsQueryView.getHearingDetails(query)).thenReturn(result);

        assertThat(resultsQueryApi.handleGetHearingDetails(query), is(result));
    }

    @Test
    public void shouldGetHearingDetailsForHearingIdFromQueryView() throws Exception {

        final JsonEnvelope query = mock(JsonEnvelope.class);
        final JsonEnvelope result = mock(JsonEnvelope.class);

        when(resultsQueryView.getHearingDetailsForHearingId(query)).thenReturn(result);

        assertThat(resultsQueryApi.handleGetResultsDetails(query), is(result));
    }

    @Test
    public void shouldGetHearingDetailsInternalFromQueryView() throws Exception {

        final JsonEnvelope query = mock(JsonEnvelope.class);
        final JsonEnvelope result = mock(JsonEnvelope.class);

        when(resultsQueryView.getHearingDetailsInternal(query)).thenReturn(result);

        assertThat(resultsQueryApi.handleGetResultsDetailsInternal(query), is(result));
    }

    @Test
    public void shouldGetResultsSummaryFromQueryView() throws Exception {

        final JsonEnvelope query = mock(JsonEnvelope.class);
        final JsonEnvelope result = mock(JsonEnvelope.class);

        when(resultsQueryView.getResultsSummary(query)).thenReturn(result);

        assertThat(resultsQueryApi.handleGetResultsSummary(query), is(result));
    }

    @Test
    public void shouldGetResultsForDefendantsTrackingStatusFromQueryView() throws Exception {

        final JsonEnvelope query = mock(JsonEnvelope.class);
        final JsonEnvelope result = mock(JsonEnvelope.class);

        when(resultsQueryView.getDefendantsTrackingStatus(query)).thenReturn(result);

        assertThat(resultsQueryApi.handleDefendantsTrackingStatus(query), is(result));
    }
}
