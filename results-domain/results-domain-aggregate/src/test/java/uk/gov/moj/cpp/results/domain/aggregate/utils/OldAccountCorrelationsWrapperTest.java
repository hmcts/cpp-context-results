package uk.gov.moj.cpp.results.domain.aggregate.utils;

import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static uk.gov.moj.cpp.results.domain.event.OldAccountCorrelations.oldAccountCorrelations;

import uk.gov.moj.cpp.results.domain.event.OldAccountCorrelations;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

public class OldAccountCorrelationsWrapperTest {

    @Test
    public void shouldGetGobAccountsNullWhenNoOldAccountCorrelations() {
        final OldAccountCorrelationsWrapper oldAccountCorrelationsWrapper = new OldAccountCorrelationsWrapper(emptyList());

        assertThat(oldAccountCorrelationsWrapper.getOldGobAccounts(), is(nullValue()));
    }

    @Test
    public void shouldUniqueGetGobAccounts() {
        final List<OldAccountCorrelations> oldAccountCorrelations = List.of(oldAccountCorrelations().withAccountCorrelationId(randomUUID()).withGobAccountNumber("AC1").build(),
                oldAccountCorrelations().withAccountCorrelationId(randomUUID()).withGobAccountNumber("AC1").build());

        final OldAccountCorrelationsWrapper oldAccountCorrelationsWrapper = new OldAccountCorrelationsWrapper(oldAccountCorrelations);

        assertThat(oldAccountCorrelationsWrapper.getOldGobAccounts(), is("AC1"));
    }

    @Test
    public void shouldMultipleGetGobAccounts() {
        final List<OldAccountCorrelations> oldAccountCorrelations = List.of(oldAccountCorrelations().withAccountCorrelationId(randomUUID()).withGobAccountNumber("AC1").build(),
                oldAccountCorrelations().withAccountCorrelationId(randomUUID()).withGobAccountNumber("AC2").build());

        final OldAccountCorrelationsWrapper oldAccountCorrelationsWrapper = new OldAccountCorrelationsWrapper(oldAccountCorrelations);

        assertThat(oldAccountCorrelationsWrapper.getOldGobAccounts(), is("AC1,AC2"));
    }

}