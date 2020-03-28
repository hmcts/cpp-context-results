package uk.gov.moj.cpp.results.event.helper.results.event.helper.sjp;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.justice.core.courts.JudicialResultPromptDurationElement;
import uk.gov.moj.cpp.results.event.helper.sjp.DurationDateHelper;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Objects;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class DurationDateHelperTest {

    private Pair<String, Integer> values;

    private JudicialResultPromptDurationElement.Builder builder;

    @Parameterized.Parameter(0)
    public String unit;

    @Parameterized.Parameter(1)
    public Integer value;

    @Parameterized.Parameter(2)
    public Object expectedStartDate;

    @Parameterized.Parameter(3)
    public Object expectedEndDate;

    @Parameterized.Parameters(name = "Duration Unit: {0} Duration Value: {1} expectedStartDate: {2} expectedEndDate: {3}")
    public static Collection<Object[]> testData() {

        return asList(new Object[][]{
                {"H", 1200, "2019-09-13", "2019-11-01"},
                {"W", 1000, "2019-09-13", "2038-11-11"},
                {"M", 1000, "2019-09-13", "2103-01-12"},
                {"Y", 1000, "2019-09-13", "3019-09-12"},
                {"T", 1000, "2019-09-13", "2019-09-13"},
                {"D", 1000, "2019-09-13", "2022-06-08"},
                {"X", 1000, "2019-09-13", "2019-09-13"},

        });
    }

    @Before
    public void init() {
        this.values = Pair.of(unit, value);
        this.builder = new JudicialResultPromptDurationElement.Builder();
    }

    @Test
    public void testExpectedStartAndExpectedEndDates() {

        final LocalDateTime localDateTime = LocalDateTime.of(2019,9,13,15,22,53);
        final ZonedDateTime dateAndTimeOfSession = ZonedDateTime.of(localDateTime, ZoneId.of("UTC"));

        DurationDateHelper.populateStartAndEndDates(builder, dateAndTimeOfSession, values);

        final JudicialResultPromptDurationElement actualJudicialResultPromptDurationElement = builder.build();

        assertThat(Objects.toString(actualJudicialResultPromptDurationElement.getDurationStartDate()), is(Objects.toString(expectedStartDate)));
        assertThat(Objects.toString(actualJudicialResultPromptDurationElement.getDurationEndDate()), is(Objects.toString(expectedEndDate)));
    }

}