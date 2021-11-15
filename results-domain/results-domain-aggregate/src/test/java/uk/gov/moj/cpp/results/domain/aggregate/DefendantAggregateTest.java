package uk.gov.moj.cpp.results.domain.aggregate;

import static java.time.LocalDate.now;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.core.courts.JudicialResult.judicialResult;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JudicialResultCategory;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Verdict;
import uk.gov.justice.core.courts.VerdictType;
import uk.gov.justice.results.courts.DefendantTrackingStatusUpdated;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DefendantAggregateTest {

    private final UUID DEFENDANT_ID = randomUUID();
    private final UUID OFFENCE_ID = randomUUID();
    private static final UUID HEARING_ID = randomUUID();
    private static final String RESULT_DEFINITION_GROUP = "Some result definition group";
    private static final String ELECTRONIC_MONITORING_ACTIVATE_RESULT_GROUP = "ELMON";
    private static final String ELECTRONIC_MONITORING_DEACTIVATE_RESULT_GROUP = "ELMONEND";
    private static final String WARRANTS_OF_ARREST_ON_RESULT_GROUP = "Warrants of arrest";
    private static final String WARRANTS_OF_ARREST_OFF_RESULT_GROUP = "WOAEXTEND";
    private static final String WARRANTS_OF_ARREST_ON_RESULT_GROUP_NO_WHITESPACE = "warrantsofarrest";
    private static final String ELECTRONIC_MONITORING_ACTIVATE_RESULT_GROUP_WITH_LOWERCASE = "ELmoN";
    private static final String LABEL = "label";
    private static final String RESULT_TEXT = "resultText";
    private static final String CJS_CODE = "cjsCode";

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssZ");
    private final ZonedDateTime LAST_MODIFIED_TIME_LAST_WEEK = LocalDate.now().minusDays(7).atStartOfDay(ZoneOffset.UTC);
    private final ZonedDateTime LAST_MODIFIED_TIME_TODAY = LocalDate.now().atStartOfDay(ZoneOffset.UTC);


    @InjectMocks
    private DefendantAggregate defendantAggregate;

    @Test
    public void shouldUpdateDefendingTrackingStatusWhenElectronicMonitoringActivated() {

        final StringJoiner joiner = new StringJoiner(",");
        joiner.add(RESULT_DEFINITION_GROUP).add(ELECTRONIC_MONITORING_ACTIVATE_RESULT_GROUP);
        final String resultDefinitionGroups = joiner.toString();

        final List<Offence> offenceList = getOffenceList(buildJudicialResultList(resultDefinitionGroups, LAST_MODIFIED_TIME_LAST_WEEK.toLocalDate()));

        final List<Object> eventStream = defendantAggregate.updateDefendantTrackingStatus(DEFENDANT_ID, offenceList).collect(Collectors.toList());
        assertThat(eventStream.size(), is(1));
        assertThat(((DefendantTrackingStatusUpdated) eventStream.get(0)).getTrackingStatus().get(0).getEmStatus(), is(true));
        assertThat(((DefendantTrackingStatusUpdated) eventStream.get(0)).getTrackingStatus().get(0).getEmLastModifiedTime().format(TIMESTAMP_FORMATTER), is(LAST_MODIFIED_TIME_LAST_WEEK.format(TIMESTAMP_FORMATTER)));
    }

    @Test
    public void shouldUpdateDefendingTrackingStatusWhenElectronicMonitoringDeactivated() {

        final StringJoiner joiner = new StringJoiner(",");
        joiner.add(RESULT_DEFINITION_GROUP).add(ELECTRONIC_MONITORING_DEACTIVATE_RESULT_GROUP);
        final String resultDefinitionGroups = joiner.toString();

        final List<Offence> offenceList = getOffenceList(buildJudicialResultList(resultDefinitionGroups, LAST_MODIFIED_TIME_LAST_WEEK.toLocalDate()));

        final List<Object> eventStream = defendantAggregate.updateDefendantTrackingStatus(DEFENDANT_ID, offenceList).collect(Collectors.toList());
        assertThat(eventStream.size(), is(1));
        assertThat(((DefendantTrackingStatusUpdated) eventStream.get(0)).getTrackingStatus().get(0).getEmStatus(), is(false));
    }

    @Test
    public void shouldUpdateDefendingTrackingStatusWhenElectronicMonitoringResultChanged() {

        final StringJoiner joiner = new StringJoiner(",");
        joiner.add(RESULT_DEFINITION_GROUP).add(ELECTRONIC_MONITORING_ACTIVATE_RESULT_GROUP);
        String resultDefinitionGroups = joiner.toString();

        final List<Offence> offenceList = getOffenceList(buildJudicialResultList(resultDefinitionGroups, LAST_MODIFIED_TIME_LAST_WEEK.toLocalDate()));

        final List<Object> eventStream = defendantAggregate.updateDefendantTrackingStatus(DEFENDANT_ID, offenceList).collect(Collectors.toList());
        assertThat(eventStream.size(), is(1));
        assertThat(((DefendantTrackingStatusUpdated) eventStream.get(0)).getTrackingStatus().get(0).getEmStatus(), is(true));
        assertThat(((DefendantTrackingStatusUpdated) eventStream.get(0)).getTrackingStatus().get(0).getEmLastModifiedTime().format(TIMESTAMP_FORMATTER), is(LAST_MODIFIED_TIME_LAST_WEEK.format(TIMESTAMP_FORMATTER)));

        // a newer result shared to deactivate
        offenceList.get(0).getJudicialResults().get(0).setOrderedDate(LAST_MODIFIED_TIME_TODAY.toLocalDate()).setResultDefinitionGroup(ELECTRONIC_MONITORING_DEACTIVATE_RESULT_GROUP);

        final List<Object> newEventStream = defendantAggregate.updateDefendantTrackingStatus(DEFENDANT_ID, offenceList).collect(Collectors.toList());
        assertThat(newEventStream.size(), is(1));
        assertThat(((DefendantTrackingStatusUpdated) newEventStream.get(0)).getTrackingStatus().get(0).getEmStatus(), is(false));
        assertThat(((DefendantTrackingStatusUpdated) newEventStream.get(0)).getTrackingStatus().get(0).getEmLastModifiedTime().format(TIMESTAMP_FORMATTER), is(LAST_MODIFIED_TIME_TODAY.format(TIMESTAMP_FORMATTER)));

    }

    @Test
    public void shouldNotUpdateDefendingTrackingStatusWhenElectronicMonitoringResultResharedAfterANewerHearing() {

        final StringJoiner joiner = new StringJoiner(",");
        joiner.add(RESULT_DEFINITION_GROUP).add(ELECTRONIC_MONITORING_ACTIVATE_RESULT_GROUP);
        String resultDefinitionGroups = joiner.toString();

        final List<Offence> offenceList = getOffenceList(buildJudicialResultList(resultDefinitionGroups, LAST_MODIFIED_TIME_LAST_WEEK.toLocalDate()));

        final List<Object> eventStream = defendantAggregate.updateDefendantTrackingStatus(DEFENDANT_ID, offenceList).collect(Collectors.toList());
        assertThat(eventStream.size(), is(1));
        assertThat(((DefendantTrackingStatusUpdated) eventStream.get(0)).getTrackingStatus().get(0).getEmStatus(), is(true));
        assertThat(((DefendantTrackingStatusUpdated) eventStream.get(0)).getTrackingStatus().get(0).getEmLastModifiedTime().format(TIMESTAMP_FORMATTER), is(LAST_MODIFIED_TIME_LAST_WEEK.format(TIMESTAMP_FORMATTER)));

        // a newer result shared to deactivate
        offenceList.get(0).getJudicialResults().get(0).setOrderedDate(LAST_MODIFIED_TIME_TODAY.toLocalDate()).setResultDefinitionGroup(ELECTRONIC_MONITORING_DEACTIVATE_RESULT_GROUP);

        final List<Object> newEventStream = defendantAggregate.updateDefendantTrackingStatus(DEFENDANT_ID, offenceList).collect(Collectors.toList());
        assertThat(newEventStream.size(), is(1));
        assertThat(((DefendantTrackingStatusUpdated) newEventStream.get(0)).getTrackingStatus().get(0).getEmStatus(), is(false));
        assertThat(((DefendantTrackingStatusUpdated) newEventStream.get(0)).getTrackingStatus().get(0).getEmLastModifiedTime().format(TIMESTAMP_FORMATTER), is(LAST_MODIFIED_TIME_TODAY.format(TIMESTAMP_FORMATTER)));

        // previous hearing reshared (extend) but has no affect
        offenceList.get(0).getJudicialResults().get(0).setOrderedDate(LAST_MODIFIED_TIME_LAST_WEEK.toLocalDate()).setResultDefinitionGroup(resultDefinitionGroups);
        final List<Object> resharedEventStream = defendantAggregate.updateDefendantTrackingStatus(DEFENDANT_ID, offenceList).collect(Collectors.toList());
        assertThat(resharedEventStream.size(), is(0));

    }

    @Test
    public void shouldNotUpdateDefendingTrackingStatusWhenNoMatchingResultIsShared() {

        final List<Offence> offenceList = getOffenceList(buildJudicialResultList(RESULT_DEFINITION_GROUP, LAST_MODIFIED_TIME_LAST_WEEK.toLocalDate()));

        final List<Object> eventStream = defendantAggregate.updateDefendantTrackingStatus(DEFENDANT_ID, offenceList).collect(Collectors.toList());
        assertThat(eventStream.size(), is(0));
    }

    @Test
    public void shouldUpdateDefendingTrackingStatusWhenWarrantOfArrestExtended() {

        final StringJoiner joiner = new StringJoiner(",");
        joiner.add(RESULT_DEFINITION_GROUP).add(WARRANTS_OF_ARREST_ON_RESULT_GROUP);
        final String resultDefinitionGroups = joiner.toString();

        final List<Offence> offenceList = getOffenceList(buildJudicialResultList(resultDefinitionGroups, LAST_MODIFIED_TIME_LAST_WEEK.toLocalDate()));

        final List<Object> eventStream = defendantAggregate.updateDefendantTrackingStatus(DEFENDANT_ID, offenceList).collect(Collectors.toList());
        assertThat(eventStream.size(), is(1));
        assertThat(((DefendantTrackingStatusUpdated) eventStream.get(0)).getTrackingStatus().get(0).getWoaStatus(), is(true));
        assertThat(((DefendantTrackingStatusUpdated) eventStream.get(0)).getTrackingStatus().get(0).getWoaLastModifiedTime().format(TIMESTAMP_FORMATTER), is(LAST_MODIFIED_TIME_LAST_WEEK.format(TIMESTAMP_FORMATTER)));

        // a newer result shared to extend
        offenceList.get(0).getJudicialResults().get(0).setOrderedDate(LAST_MODIFIED_TIME_TODAY.toLocalDate()).setResultDefinitionGroup(WARRANTS_OF_ARREST_ON_RESULT_GROUP);

        final List<Object> newEventStream = defendantAggregate.updateDefendantTrackingStatus(DEFENDANT_ID, offenceList).collect(Collectors.toList());
        assertThat(newEventStream.size(), is(1));
        assertThat(((DefendantTrackingStatusUpdated) newEventStream.get(0)).getTrackingStatus().get(0).getWoaStatus(), is(true));
        assertThat(((DefendantTrackingStatusUpdated) newEventStream.get(0)).getTrackingStatus().get(0).getWoaLastModifiedTime().format(TIMESTAMP_FORMATTER), is(LAST_MODIFIED_TIME_TODAY.format(TIMESTAMP_FORMATTER)));
    }

    @Test
    public void shouldUpdateDefendingTrackingStatusWhenWarrantOfArrestDeactivated() {

        final StringJoiner joiner = new StringJoiner(",");
        joiner.add(RESULT_DEFINITION_GROUP).add(WARRANTS_OF_ARREST_ON_RESULT_GROUP);
        final String resultDefinitionGroups = joiner.toString();

        final List<Offence> offenceList = getOffenceList(buildJudicialResultList(resultDefinitionGroups, LAST_MODIFIED_TIME_LAST_WEEK.toLocalDate()));

        final List<Object> eventStream = defendantAggregate.updateDefendantTrackingStatus(DEFENDANT_ID, offenceList).collect(Collectors.toList());
        assertThat(eventStream.size(), is(1));
        assertThat(((DefendantTrackingStatusUpdated) eventStream.get(0)).getTrackingStatus().get(0).getWoaStatus(), is(true));
        assertThat(((DefendantTrackingStatusUpdated) eventStream.get(0)).getTrackingStatus().get(0).getWoaLastModifiedTime().format(TIMESTAMP_FORMATTER), is(LAST_MODIFIED_TIME_LAST_WEEK.format(TIMESTAMP_FORMATTER)));

        // a newer result shared to deactivate
        offenceList.get(0).getJudicialResults().get(0).setOrderedDate(LAST_MODIFIED_TIME_TODAY.toLocalDate()).setResultDefinitionGroup(WARRANTS_OF_ARREST_OFF_RESULT_GROUP);

        final List<Object> newEventStream = defendantAggregate.updateDefendantTrackingStatus(DEFENDANT_ID, offenceList).collect(Collectors.toList());
        assertThat(newEventStream.size(), is(1));
        assertThat(((DefendantTrackingStatusUpdated) newEventStream.get(0)).getTrackingStatus().get(0).getWoaStatus(), is(false));
        assertThat(((DefendantTrackingStatusUpdated) newEventStream.get(0)).getTrackingStatus().get(0).getWoaLastModifiedTime().format(TIMESTAMP_FORMATTER), is(LAST_MODIFIED_TIME_TODAY.format(TIMESTAMP_FORMATTER)));
    }

    @Test
    public void shouldUpdateDefendingTrackingStatusWhenBothElectronicMonitoringAndWarrantOfArrestActivated() {

        final StringJoiner joiner = new StringJoiner(",");
        joiner.add(RESULT_DEFINITION_GROUP).add(ELECTRONIC_MONITORING_ACTIVATE_RESULT_GROUP).add(WARRANTS_OF_ARREST_ON_RESULT_GROUP);
        final String resultDefinitionGroups = joiner.toString();

        final List<Offence> offenceList = getOffenceList(buildJudicialResultList(resultDefinitionGroups, LAST_MODIFIED_TIME_LAST_WEEK.toLocalDate()));

        final List<Object> eventStream = defendantAggregate.updateDefendantTrackingStatus(DEFENDANT_ID, offenceList).collect(Collectors.toList());
        assertThat(eventStream.size(), is(1));
        assertThat(((DefendantTrackingStatusUpdated) eventStream.get(0)).getTrackingStatus().get(0).getEmStatus(), is(true));
        assertThat(((DefendantTrackingStatusUpdated) eventStream.get(0)).getTrackingStatus().get(1).getWoaStatus(), is(true));
    }

    @Test
    public void shouldUpdateDefendingTrackingStatusWhenElectronicMonitoringActivatedWithLowerCaseResultDefinition() {

        final StringJoiner joiner = new StringJoiner(",");
        joiner.add(RESULT_DEFINITION_GROUP).add(ELECTRONIC_MONITORING_ACTIVATE_RESULT_GROUP_WITH_LOWERCASE);
        final String resultDefinitionGroups = joiner.toString();

        final List<Offence> offenceList = getOffenceList(buildJudicialResultList(resultDefinitionGroups, LAST_MODIFIED_TIME_LAST_WEEK.toLocalDate()));

        final List<Object> eventStream = defendantAggregate.updateDefendantTrackingStatus(DEFENDANT_ID, offenceList).collect(Collectors.toList());
        assertThat(eventStream.size(), is(1));
        assertThat(((DefendantTrackingStatusUpdated) eventStream.get(0)).getTrackingStatus().get(0).getEmStatus(), is(true));
        assertThat(((DefendantTrackingStatusUpdated) eventStream.get(0)).getTrackingStatus().get(0).getEmLastModifiedTime().format(TIMESTAMP_FORMATTER), is(LAST_MODIFIED_TIME_LAST_WEEK.format(TIMESTAMP_FORMATTER)));
    }

    @Test
    public void shouldUpdateDefendingTrackingStatusWhenWoAExtendedWithoutWhitespacesInResultDefinition() {

        final StringJoiner joiner = new StringJoiner(",");
        joiner.add(RESULT_DEFINITION_GROUP).add(WARRANTS_OF_ARREST_ON_RESULT_GROUP_NO_WHITESPACE);
        final String resultDefinitionGroups = joiner.toString();

        final List<Offence> offenceList = getOffenceList(buildJudicialResultList(resultDefinitionGroups, LAST_MODIFIED_TIME_LAST_WEEK.toLocalDate()));

        final List<Object> eventStream = defendantAggregate.updateDefendantTrackingStatus(DEFENDANT_ID, offenceList).collect(Collectors.toList());
        assertThat(eventStream.size(), is(1));
        assertThat(((DefendantTrackingStatusUpdated) eventStream.get(0)).getTrackingStatus().get(0).getWoaStatus(), is(true));
        assertThat(((DefendantTrackingStatusUpdated) eventStream.get(0)).getTrackingStatus().get(0).getWoaLastModifiedTime().format(TIMESTAMP_FORMATTER), is(LAST_MODIFIED_TIME_LAST_WEEK.format(TIMESTAMP_FORMATTER)));
    }

    private List<Offence> getOffenceList(final List<JudicialResult> judicialResults) {

        final Offence.Builder builder = Offence.offence();
        builder.withId(OFFENCE_ID)
                .withOffenceDefinitionId(randomUUID())
                .withOffenceCode("offenceCode")
                .withOffenceTitle(STRING.next())
                .withWording(STRING.next())
                .withStartDate(now())
                .withEndDate(now())
                .withArrestDate(now())
                .withChargeDate(now())
                .withConvictionDate(now())
                .withEndDate(now())
                .withModeOfTrial("1010")
                .withJudicialResults(judicialResults)
                .withOrderIndex(65)
                .withIsDisposed(true)
                .withCount(434)
                .withPlea(uk.gov.justice.core.courts.Plea.plea().withOffenceId(OFFENCE_ID).withPleaDate(now()).withPleaValue("NOT_GUILTY").build())
                .withProceedingsConcluded(true)
                .withIntroducedAfterInitialProceedings(true)
                .withIsDiscontinued(true)
                .withVerdict(Verdict.verdict()
                        .withVerdictType(VerdictType.verdictType()
                                .withId(randomUUID())
                                .withCategory(STRING.next())
                                .withCategoryType(STRING.next())
                                .withCjsVerdictCode("N")
                                .build())
                        .withOriginatingHearingId(randomUUID())
                        .withOffenceId(OFFENCE_ID)
                        .withVerdictDate(now())
                        .build());

        return singletonList(builder.build());
    }

    private List<JudicialResult> buildJudicialResultList(final String resultDefinitionGroup, final LocalDate orderDate) {
        return singletonList(judicialResult()
                .withJudicialResultId(randomUUID())
                .withCategory(JudicialResultCategory.FINAL)
                .withCjsCode(CJS_CODE)
                .withIsAdjournmentResult(false)
                .withIsAvailableForCourtExtract(false)
                .withIsConvictedResult(false)
                .withIsFinancialResult(false)
                .withLabel(LABEL)
                .withOrderedHearingId(HEARING_ID)
                .withOrderedDate(orderDate)
                .withRank(BigDecimal.ZERO)
                .withWelshLabel(LABEL)
                .withResultText(RESULT_TEXT)
                .withResultDefinitionGroup(resultDefinitionGroup)
                .withLifeDuration(false)
                .withTerminatesOffenceProceedings(Boolean.FALSE)
                .withLifeDuration(false)
                .withPublishedAsAPrompt(false)
                .withExcludedFromResults(false)
                .withAlwaysPublished(false)
                .withUrgent(false)
                .withD20(false)
                .withPublishedForNows(false)
                .withRollUpPrompts(false)
                .withJudicialResultTypeId(randomUUID())
                .build());
    }


}