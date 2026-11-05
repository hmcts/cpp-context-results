package uk.gov.moj.cpp.results.domain.aggregate.utils;

import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static uk.gov.moj.cpp.results.domain.event.OldAccountDetails.oldAccountDetails;

import uk.gov.moj.cpp.results.domain.event.OldAccountDetails;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

public class OldAccountDetailsWrapperTest {

    @Test
    public void shouldGetGobAccountsNullWhenNoOldAccountCorrelations() {
        final OldAccountDetailsWrapper oldAccountDetailsWrapper = new OldAccountDetailsWrapper(emptyList());

        assertThat(oldAccountDetailsWrapper.getOldGobAccounts(), is(nullValue()));
    }

    @Test
    public void shouldUniqueGetGobAccounts() {
        final List<OldAccountDetails> oldAccountCorrelations = List.of(oldAccountDetails().withAccountCorrelationId(randomUUID()).withGobAccountNumber("AC1").build(),
                oldAccountDetails().withAccountCorrelationId(randomUUID()).withGobAccountNumber("AC1").build());

        final OldAccountDetailsWrapper oldAccountDetailsWrapper = new OldAccountDetailsWrapper(oldAccountCorrelations);

        assertThat(oldAccountDetailsWrapper.getOldGobAccounts(), is("AC1"));
    }

    @Test
    public void shouldMultipleGetGobAccounts() {
        final List<OldAccountDetails> oldAccountCorrelations = List.of(oldAccountDetails().withAccountCorrelationId(randomUUID()).withGobAccountNumber("AC1").build(),
                oldAccountDetails().withAccountCorrelationId(randomUUID()).withGobAccountNumber("AC2").build());

        final OldAccountDetailsWrapper oldAccountDetailsWrapper = new OldAccountDetailsWrapper(oldAccountCorrelations);

        assertThat(oldAccountDetailsWrapper.getOldGobAccounts(), is("AC1,AC2"));
    }

    @Test
    public void shouldUniqueGetDivCodes() {
        final List<OldAccountDetails> oldAccountCorrelations = List.of(oldAccountDetails().withAccountCorrelationId(randomUUID()).withDivisionCode("DIV1").build(),
                oldAccountDetails().withAccountCorrelationId(randomUUID()).withDivisionCode("DIV1").build());

        final OldAccountDetailsWrapper oldAccountDetailsWrapper = new OldAccountDetailsWrapper(oldAccountCorrelations);

        assertThat(oldAccountDetailsWrapper.getOldDivisionCodes(), is("DIV1"));
    }

    @Test
    public void shouldMultipleGetDivCodes() {
        final List<OldAccountDetails> oldAccountCorrelations = List.of(oldAccountDetails().withAccountCorrelationId(randomUUID()).withDivisionCode("DIV1").build(),
                oldAccountDetails().withAccountCorrelationId(randomUUID()).withDivisionCode("DIV2").build());

        final OldAccountDetailsWrapper oldAccountDetailsWrapper = new OldAccountDetailsWrapper(oldAccountCorrelations);

        assertThat(oldAccountDetailsWrapper.getOldDivisionCodes(), is("DIV1,DIV2"));
    }

    @Test
    public void shouldGetRecentAccountCorrelation() {
        final UUID accountCorrelationId1 = randomUUID();
        final UUID accountCorrelationId2 = randomUUID();
        final List<OldAccountDetails> oldAccountCorrelations = List.of(oldAccountDetails()
                        .withAccountCorrelationId(accountCorrelationId1)
                        .withCreatedTime(ZonedDateTime.now().minusHours(1)).build(),
                oldAccountDetails()
                        .withAccountCorrelationId(accountCorrelationId2)
                        .withCreatedTime(ZonedDateTime.now()).build());

        final OldAccountDetailsWrapper oldAccountDetailsWrapper = new OldAccountDetailsWrapper(oldAccountCorrelations);

        assertThat(oldAccountDetailsWrapper.getRecentAccountCorrelationId(), is(accountCorrelationId2));
    }

}