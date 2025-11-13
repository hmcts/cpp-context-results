package uk.gov.moj.cpp.results.domain.aggregate.utils;

import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static uk.gov.moj.cpp.results.domain.event.OldAccountCorrelations.oldAccountCorrelations;

import uk.gov.moj.cpp.results.domain.event.OldAccountCorrelations;

import java.time.ZonedDateTime;
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

    @Test
    public void shouldUniqueGetDivCodes() {
        final List<OldAccountCorrelations> oldAccountCorrelations = List.of(oldAccountCorrelations().withAccountCorrelationId(randomUUID()).withDivisionCode("DIV1").build(),
                oldAccountCorrelations().withAccountCorrelationId(randomUUID()).withDivisionCode("DIV1").build());

        final OldAccountCorrelationsWrapper oldAccountCorrelationsWrapper = new OldAccountCorrelationsWrapper(oldAccountCorrelations);

        assertThat(oldAccountCorrelationsWrapper.getOldDivisionCodes(), is("DIV1"));
    }

    @Test
    public void shouldMultipleGetDivCodes() {
        final List<OldAccountCorrelations> oldAccountCorrelations = List.of(oldAccountCorrelations().withAccountCorrelationId(randomUUID()).withDivisionCode("DIV1").build(),
                oldAccountCorrelations().withAccountCorrelationId(randomUUID()).withDivisionCode("DIV2").build());

        final OldAccountCorrelationsWrapper oldAccountCorrelationsWrapper = new OldAccountCorrelationsWrapper(oldAccountCorrelations);

        assertThat(oldAccountCorrelationsWrapper.getOldDivisionCodes(), is("DIV1,DIV2"));
    }

    @Test
    public void shouldGetRecentAccountCorrelation() {
        final UUID accountCorrelationId1 = randomUUID();
        final UUID accountCorrelationId2 = randomUUID();
        final List<OldAccountCorrelations> oldAccountCorrelations = List.of(oldAccountCorrelations()
                        .withAccountCorrelationId(accountCorrelationId1)
                        .withCreatedTime(ZonedDateTime.now().minusHours(1)).build(),
                oldAccountCorrelations()
                        .withAccountCorrelationId(accountCorrelationId2)
                        .withCreatedTime(ZonedDateTime.now()).build());

        final OldAccountCorrelationsWrapper oldAccountCorrelationsWrapper = new OldAccountCorrelationsWrapper(oldAccountCorrelations);

        assertThat(oldAccountCorrelationsWrapper.getRecentAccountCorrelationId(), is(accountCorrelationId2));
    }

}