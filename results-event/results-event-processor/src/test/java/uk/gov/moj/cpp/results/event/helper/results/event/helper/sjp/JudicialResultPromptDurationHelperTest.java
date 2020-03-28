package uk.gov.moj.cpp.results.event.helper.results.event.helper.sjp;


import static com.google.common.collect.ImmutableList.of;
import static java.time.ZoneOffset.UTC;
import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static junit.framework.TestCase.assertNull;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static uk.gov.justice.core.courts.JudicialResultPrompt.judicialResultPrompt;

import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.JudicialResultPrompt;
import uk.gov.justice.core.courts.JudicialResultPromptDurationElement;
import uk.gov.moj.cpp.results.event.helper.resultdefinition.ResultDefinition;
import uk.gov.moj.cpp.results.event.helper.sjp.JudicialResultPromptDurationHelper;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runners.Parameterized;

public class JudicialResultPromptDurationHelperTest {

    final ZonedDateTime sjpHearingSession = ZonedDateTime.of(LocalDate.now(),LocalTime.of(12,12,12), ZoneId.from(UTC));

    private ResultDefinition resultDefinition;
    private List<JudicialResultPrompt> judicialResultPromptList;

    @Parameterized.Parameter(0)
    public String value;
    @Parameterized.Parameter(1)
    public Integer durationSequence;
    @Parameterized.Parameter(2)
    public Boolean lifeDuration;
    @Parameterized.Parameter(3)
    public String expectedPrimaryUnit;
    @Parameterized.Parameter(4)
    public Integer expectedPrimaryValue;
    @Parameterized.Parameter(5)
    public String expectedSecondaryUnit;
    @Parameterized.Parameter(6)
    public Integer expectedSecondaryValue;

    @Parameterized.Parameters(name = "Duration Value: {0} Duration Sequence: {1} lifeDuration: {2} expectedPrimaryUnit: {3} expectedPrimaryValue: {4} expectedSecondaryUnit: {5} expectedSecondaryValue: {6}")
    public static Collection<Object[]> testData() {
        return asList( new Object[][] {
                {"5 Years",2,true,"Y",5,null,null},
                {"32 months", 1, false, "M", 32, null, null},
                {"5 Years", 4, false, null, null, null, null},
                {"5 Years", null, false, null, null, null, null},
                {"5    Years", 2, false, null, null, "Y", 5},
                {"32         MonThs", 2, false, null, null, "M", 32},
                {"MonThs", 2, false, null, null, null, null},
                {"MonThs", 1, true, "L", 1, null, null},
                {"MonThs", 1, false, null, null, null, null},
                {"MonThs", 1000, false, null, null, null, null}
        });
    }

    @Before
    public void init(){
        resultDefinition = ResultDefinition.resultDefinition().setId(UUID.randomUUID()).setLifeDuration(lifeDuration);

    }

    @Test
    public void testPrimaryAndSecondaryDurationAndValues() {
        judicialResultPromptList = of(judicialResultPrompt().withValue(value).withDurationSequence(durationSequence).build());
        final Optional<JudicialResultPromptDurationElement> resultPromptDurationElement = new JudicialResultPromptDurationHelper().populate(judicialResultPromptList, sjpHearingSession, resultDefinition);

        assertThat(resultPromptDurationElement.map(JudicialResultPromptDurationElement::getPrimaryDurationUnit).orElse(null), is(expectedPrimaryUnit));
        assertThat(resultPromptDurationElement.map(JudicialResultPromptDurationElement::getPrimaryDurationValue).orElse(null), is(expectedPrimaryValue));
        assertThat(resultPromptDurationElement.map(JudicialResultPromptDurationElement::getSecondaryDurationUnit).orElse(null), is(expectedSecondaryUnit));
        assertThat(resultPromptDurationElement.map(JudicialResultPromptDurationElement::getSecondaryDurationValue).orElse(null), is(expectedSecondaryValue));
    }

    @Test
    public void whenJudicialResultPromptListIsNullThenResultPromptDurationElementShouldBeEmpty() {
        judicialResultPromptList = null;
        final Optional<JudicialResultPromptDurationElement> resultPromptDurationElement = new JudicialResultPromptDurationHelper().populate(judicialResultPromptList, sjpHearingSession, resultDefinition);
        assertThat(resultPromptDurationElement, is(empty()));

    }
}
