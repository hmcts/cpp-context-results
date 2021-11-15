package uk.gov.moj.cpp.results.domain.aggregate;

import static java.util.Objects.isNull;
import static java.util.stream.Stream.empty;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;

import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.justice.results.courts.DefendantTrackingStatusUpdated;
import uk.gov.justice.results.courts.TrackingStatus;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DefendantAggregate implements Aggregate {

    private static final long serialVersionUID = -7886712539441184596L;

    private static final String ELECTRONIC_MONITORING_ACTIVATE_RESULT_GROUP = "ELMON";
    private static final String ELECTRONIC_MONITORING_DEACTIVATE_RESULT_GROUP = "ELMONEND";

    private static final String WARRANT_OF_ARREST_ON_RESULT_GROUP = "Warrants of arrest";
    private static final String WARRANT_OF_ARREST_OFF_RESULT_GROUP = "WOAEXTEND";

    private List<TrackingStatus> offenceTrackingStatus = new ArrayList<>();

    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(DefendantTrackingStatusUpdated.class).apply(this::defendantTrackingStatusUpdatedEvent),
                otherwiseDoNothing());
    }

    public Stream<Object> updateDefendantTrackingStatus(final UUID defendantId, final List<Offence> offences) {

        final List<TrackingStatus> trackingList = new ArrayList<>();

        buildElectronicMonitoringTracking(offences, trackingList, ELECTRONIC_MONITORING_ACTIVATE_RESULT_GROUP);
        buildElectronicMonitoringTracking(offences, trackingList, ELECTRONIC_MONITORING_DEACTIVATE_RESULT_GROUP);

        buildWarrantOfArrestTracking(offences, trackingList, WARRANT_OF_ARREST_ON_RESULT_GROUP);
        buildWarrantOfArrestTracking(offences, trackingList, WARRANT_OF_ARREST_OFF_RESULT_GROUP);

        if (!trackingList.isEmpty()) {
            final DefendantTrackingStatusUpdated defendantTrackingStatusUpdated = DefendantTrackingStatusUpdated.defendantTrackingStatusUpdated()
                    .withDefendantId(defendantId)
                    .withTrackingStatus(trackingList)
                    .build();

            return apply(Stream.of(defendantTrackingStatusUpdated));
        }

        return apply(empty());
    }

    /**
     * Populates the {@link List}&lt;{@link TrackingStatus}&gt; for {@code Electronic Monitoring} results.
     * <br><br>
     * Compares the {@code OrderedDate} of the {@code JudicialResults} and if the {@code offenceTrackingStatus}
     * does not have a newer result (i.e. new hearing resulted or the latest hearing amended and re-shared), adds {@code TrackingStatus} to the {@code trackedList}.
     * <br>
     * Will activate/deactivate {@code Electronic Monitoring}
     * depending on the resultDefinitionGroup parameter provided.
     *
     * @param offences              - the list of offences shared in the result
     * @param trackedList           - the trackedList to be populated
     * @param resultDefinitionGroup - the resultDefinitionGroup to be compared against the JudicialResult's resultDefinitionGroup
     *                              ({@code "ELMON"} to Activate, {@code "ELMONEND"} to Deactivate)
     */
    private void buildElectronicMonitoringTracking(final List<Offence> offences, final List<TrackingStatus> trackedList, final String resultDefinitionGroup) {
        for (final Offence offence : offences) {
            final Optional<JudicialResult> jr = findJudicialResultBelongingToGroup(resultDefinitionGroup, offence);

            if (jr.isPresent()) {

                final ZonedDateTime emLastModifiedTime = jr.get().getOrderedDate().atStartOfDay(ZoneOffset.UTC);

                final boolean isLatestEMResult = offenceTrackingStatus.stream()
                        .noneMatch(existingStatus -> existingStatus.getOffenceId().equals(offence.getId()) &&
                                (!isNull(existingStatus.getEmLastModifiedTime()) && existingStatus.getEmLastModifiedTime().isAfter(emLastModifiedTime)));

                if (isLatestEMResult) {
                    trackedList.add(new TrackingStatus(emLastModifiedTime, resultDefinitionGroup.equals(ELECTRONIC_MONITORING_ACTIVATE_RESULT_GROUP), offence.getId(), null, null));
                }
            }
        }
    }

    /**
     * Populates the {@link List}&lt;{@link TrackingStatus}&gt; for {@code Warrant of Arrest} results.
     * <br><br>
     * Compares the {@code OrderedDate} of the {@code JudicialResults} and if the {@code offenceTrackingStatus}
     * does not have a newer result (i.e. new hearing resulted or the latest hearing amended and re-shared), adds {@code TrackingStatus} to the {@code trackedList}.
     * <br>
     * Will toggle {@code Warrant of Arrest} on/off
     * depending on the resultDefinitionGroup parameter provided.
     *
     * @param offences              - the list of offences shared in the result
     * @param trackedList           - the trackedList to be populated
     * @param resultDefinitionGroup - the resultDefinitionGroup to be compared against the JudicialResult's resultDefinitionGroup
     *                              ({@code "Warrants of arrest"} for ON, {@code "WOAEXTEND"} for OFF)
     */
    private void buildWarrantOfArrestTracking(final List<Offence> offences, final List<TrackingStatus> trackedList, final String resultDefinitionGroup) {
        for (final Offence offence : offences) {
            final Optional<JudicialResult> jr = findJudicialResultBelongingToGroup(resultDefinitionGroup, offence);

            if (jr.isPresent()) {

                final ZonedDateTime woaLastModifiedTime = jr.get().getOrderedDate().atStartOfDay(ZoneOffset.UTC);

                final boolean isLatestWoAResult = offenceTrackingStatus.stream()
                        .noneMatch(existingStatus -> existingStatus.getOffenceId().equals(offence.getId()) &&
                                (!isNull(existingStatus.getWoaLastModifiedTime()) && existingStatus.getWoaLastModifiedTime().isAfter(woaLastModifiedTime)));

                if (isLatestWoAResult) {
                    trackedList.add(new TrackingStatus(null, null, offence.getId(), woaLastModifiedTime, resultDefinitionGroup.equals(WARRANT_OF_ARREST_ON_RESULT_GROUP)));
                }
            }
        }
    }

    private Optional<JudicialResult> findJudicialResultBelongingToGroup(final String resultGroup, final Offence offence) {
        return Optional.ofNullable(offence.getJudicialResults()).map(Collection::stream).orElseGet(Stream::empty).filter(judicialResult -> {
                    if (Objects.nonNull(judicialResult.getResultDefinitionGroup())) {
                        return Arrays.stream(judicialResult.getResultDefinitionGroup().toLowerCase().replace(" ", "").split(",")).collect(Collectors.toList()).contains(resultGroup.toLowerCase().replace(" ", ""));
                    }
                    return false;
                }
        ).findFirst();
    }

    private void defendantTrackingStatusUpdatedEvent(final DefendantTrackingStatusUpdated defendantTrackingStatusUpdated) {
        this.offenceTrackingStatus.addAll(defendantTrackingStatusUpdated.getTrackingStatus());
    }

}
