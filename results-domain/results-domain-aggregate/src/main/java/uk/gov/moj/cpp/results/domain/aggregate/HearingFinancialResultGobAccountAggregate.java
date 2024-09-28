package uk.gov.moj.cpp.results.domain.aggregate;

import static java.util.stream.Stream.builder;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;

import uk.gov.justice.core.courts.CorrelationIdAndMasterdefendantAdded;
import uk.gov.justice.domain.aggregate.Aggregate;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@SuppressWarnings({"PMD.BeanMembersShouldSerialize"})
public class HearingFinancialResultGobAccountAggregate implements Aggregate {

    private static final long serialVersionUID = -8686140987673113042L;
    private UUID masterDefendantId = null;


    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(CorrelationIdAndMasterdefendantAdded.class).apply(this::handleCorrelationIdUpdated),
                otherwiseDoNothing());
    }

    private void handleCorrelationIdUpdated(final CorrelationIdAndMasterdefendantAdded correlationIdAndMasterdefendantAdded) {
        masterDefendantId =  correlationIdAndMasterdefendantAdded.getMasterDefendantId();
    }

    public Stream<Object> addGobAccountDefendantId(final UUID masterDefendantId, final UUID correlationId) {
        final CorrelationIdAndMasterdefendantAdded correlationIdUpdated = CorrelationIdAndMasterdefendantAdded.correlationIdAndMasterdefendantAdded()
                .withMasterDefendantId(masterDefendantId)
                .withCorrelationId(correlationId).build();
        return apply(builder().add(correlationIdUpdated).build());
    }

    public Optional<UUID> getMasterDefendantId(){
        return Optional.ofNullable(masterDefendantId);
    }
}
