package uk.gov.moj.cpp.results.domain.aggregate;


import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class HearingFinancialResultGobAccountAggregateTest {

    @InjectMocks
    private HearingFinancialResultGobAccountAggregate hearingFinancialResultGobAccountAggregate;

    @Test
    public void shouldUpdateMasterDefendantid(){
        final UUID masterDefendantId = randomUUID();
        final UUID correlationId = randomUUID();

        List<Object> events = hearingFinancialResultGobAccountAggregate.addGobAccountDefendantId(masterDefendantId, correlationId).collect(Collectors.toList());
        final Optional<UUID> response = hearingFinancialResultGobAccountAggregate.getMasterDefendantId();

        assertThat(events.size(), is(1));
        assertThat(events.get(0).getClass().toString(), is("class uk.gov.justice.core.courts.CorrelationIdAndMasterdefendantAdded"));
        assertThat(response.get(), is(masterDefendantId));

    }
}
