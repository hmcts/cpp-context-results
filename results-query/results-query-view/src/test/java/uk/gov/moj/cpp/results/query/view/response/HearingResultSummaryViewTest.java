package uk.gov.moj.cpp.results.query.view.response;

import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.PAST_LOCAL_DATE;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.randomEnum;

import uk.gov.moj.cpp.domains.results.result.ResultLevel;

import java.time.LocalDate;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hamcrest.core.IsNot;
import org.junit.jupiter.api.Test;

public class HearingResultSummaryViewTest {

    private static final LocalDate FROM_DATE = PAST_LOCAL_DATE.next();
    private static final UUID PERSON_ID_1 = randomUUID();
    private static final UUID HEARING_ID_1 = randomUUID();
    private static final UUID PERSON_ID_2 = randomUUID();
    private static final UUID HEARING_ID_2 = randomUUID();
    private static final String[] HEARING_TYPES = {"PTPH", "SENTENCE", "TRIAL"};
    private static final String HEARING_TYPE =
                    HEARING_TYPES[new Random().nextInt(HEARING_TYPES.length)];
    private static final LocalDate HEARING_DATE = PAST_LOCAL_DATE.next();
    private static final String FIRST_NAME = STRING.next();
    private static final String LAST_NAME = STRING.next();

    private static final UUID HEARING_RESULT_ID = randomUUID();
    private static final UUID OFFENCE_ID = randomUUID();
    private static final UUID CASE_ID = randomUUID();
    private static final String CASE_URN_1_1 = STRING.next();
    private static final String CASE_URN_1_2 = STRING.next();
    private static final String CASE_URN_2_1 = STRING.next();
    private static final String CASE_URN_2_2 = STRING.next();
    private static final ResultLevel RESULT_LEVEL = randomEnum(ResultLevel.class).next();
    private static final String RESULT_LABEL = STRING.next();
    private static final UUID courtCentreId = randomUUID();

    private static final String RESULT_PROMPT_LABEL = STRING.next();
    private static final String RESULT_PROMPT_VALUE = STRING.next();

    @Test
    public void shouldCreateNewObjectWithSameValuesIfBuilderDoesNotOverwriteAnyFields() throws Exception {
        final HearingResultSummaryView hrsv = createHearingResultSummaryView();
        final HearingResultSummaryView actualHrsv = hrsv.builder().build();

        assertThat(actualHrsv, is(IsNot.not(hrsv)));
        assertThat(actualHrsv.getDefendant().getFirstName(), is(hrsv.getDefendant().getFirstName()));
        assertThat(actualHrsv.getDefendant().getLastName(), is(hrsv.getDefendant().getLastName()));
        assertThat(actualHrsv.getUrns(), is(hrsv.getUrns()));
        assertThat(actualHrsv.getHearingType(), is(hrsv.getHearingType()));
        assertThat(actualHrsv.getHearingId(), is(hrsv.getHearingId()));
        assertThat(actualHrsv.getHearingDate(), is(hrsv.getHearingDate()));

    }

    @Test
    public void shouldCreateNewObjectWithSameValuesExceptUrnsFollowingUpdate() throws Exception {
        final List<String> urns = Stream.of("urn1", "urn2").collect(Collectors.toList());

        final HearingResultSummaryView hrsv = createHearingResultSummaryView();
        final HearingResultSummaryView actualHrsv = hrsv.builder().urns(urns).build();

        assertThat(actualHrsv, is(IsNot.not(hrsv)));
        assertThat(actualHrsv.getDefendant().getFirstName(), is(hrsv.getDefendant().getFirstName()));
        assertThat(actualHrsv.getDefendant().getLastName(), is(hrsv.getDefendant().getLastName()));
        assertThat(actualHrsv.getUrns(), is(urns));
        assertThat(actualHrsv.getHearingType(), is(hrsv.getHearingType()));
        assertThat(actualHrsv.getHearingId(), is(hrsv.getHearingId()));
        assertThat(actualHrsv.getHearingDate(), is(hrsv.getHearingDate()));

    }

    private HearingResultSummaryView createHearingResultSummaryView() {
        final DefendantView defendantView = new DefendantView(PERSON_ID_1, FIRST_NAME, LAST_NAME);
        return new HearingResultSummaryView(HEARING_ID_1, HEARING_TYPE, HEARING_DATE, emptyList(), defendantView, courtCentreId);
    }


}