package uk.gov.moj.cpp.results.query.api;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.results.query.view.DefendantGobAccountsQueryView;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DefendantGobAccountsQueryApiTest {

    @Mock
    private DefendantGobAccountsQueryView defendantGobAccountsQueryView;

    @InjectMocks
    private DefendantGobAccountsQueryApi defendantGobAccountsQueryApi;

    @Test
    public void shouldGetDefendantGobAccountsFromQueryView() throws Exception {

        final JsonEnvelope query = mock(JsonEnvelope.class);
        final JsonEnvelope result = mock(JsonEnvelope.class);

        when(defendantGobAccountsQueryView.getDefendantGobAccounts(query)).thenReturn(result);

        assertThat(defendantGobAccountsQueryApi.getDefendantGobAccounts(query), is(result));
    }
}
