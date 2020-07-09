package uk.gov.moj.cpp.results.event.helper.sjp;

import static com.google.common.collect.ImmutableList.of;
import static java.math.BigDecimal.valueOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.results.event.helper.TestTemplate.DATE_AND_TIME_OF_SESSION;
import static uk.gov.moj.cpp.results.event.helper.TestTemplate.SESSION_ID;
import static uk.gov.moj.cpp.results.event.helper.TestTemplate.buildResultDefinition;
import static uk.gov.moj.cpp.results.event.helper.TestTemplate.buildResultDefinitionForPrimaryDuration;
import static uk.gov.moj.cpp.results.event.helper.TestTemplate.buildResultDefinitionWithEmptyPrompts;
import static uk.gov.moj.cpp.results.event.helper.TestTemplate.getBaseResults;
import static uk.gov.moj.cpp.results.event.helper.TestTemplate.getBaseResultsWithNoPrompts;

import uk.gov.justice.core.courts.Category;
import uk.gov.justice.sjp.results.BaseResult;
import uk.gov.moj.cpp.results.event.helper.ReferenceCache;

import java.util.List;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class JudicialResultTest {

    private final String EOL = System.getProperty("line.separator");

    @Mock
    private ReferenceCache referenceCache;

    @Test
    public void testBuildJudicialResults() {

        when(referenceCache.getResultDefinitionById(any(),any(), any())).thenReturn(buildResultDefinition());

        List<BaseResult> baseResultList = getBaseResults();

        List<uk.gov.justice.core.courts.JudicialResult> judicialResultList =  new JudicialResult(referenceCache).buildJudicialResults(baseResultList,DATE_AND_TIME_OF_SESSION,SESSION_ID);
        assertResult(baseResultList,judicialResultList);
    }

    @Test
    public void testWhenBaseResultPromptsAreEmptyThenJudicialResultPromptShouldBeNull() {

        when(referenceCache.getResultDefinitionById(any(),any(), any())).thenReturn(buildResultDefinitionWithEmptyPrompts());

        final List<BaseResult> baseResultList = getBaseResultsWithNoPrompts();

        final List<uk.gov.justice.core.courts.JudicialResult> judicialResultList =  new JudicialResult(referenceCache).buildJudicialResults(baseResultList,DATE_AND_TIME_OF_SESSION,SESSION_ID);

        final Optional<uk.gov.justice.core.courts.JudicialResult> jrPrompt = judicialResultList.stream().findFirst();

        assertNotNull(jrPrompt);
        assertNull(jrPrompt.get().getJudicialResultPrompts());
    }

    @Test
    public void testPrimaryDurationInJudicialResults() {

        when(referenceCache.getResultDefinitionById(any(),any(), any())).thenReturn(buildResultDefinitionForPrimaryDuration(false));

        List<BaseResult> baseResultList = getBaseResults();

        List<uk.gov.justice.core.courts.JudicialResult> judicialResultList =  new JudicialResult(referenceCache).buildJudicialResults(baseResultList,DATE_AND_TIME_OF_SESSION,SESSION_ID);
        assertResult(baseResultList,judicialResultList);
    }

    @Test
    public void testPrimaryDurationUnitAndValueInJudicialResult() {

        when(referenceCache.getResultDefinitionById(any(),any(), any())).thenReturn(buildResultDefinitionForPrimaryDuration(true));

        List<BaseResult> baseResultList = getBaseResults();

        List<uk.gov.justice.core.courts.JudicialResult> judicialResultList =  new JudicialResult(referenceCache).buildJudicialResults(baseResultList,DATE_AND_TIME_OF_SESSION,SESSION_ID);
        assertJudicialPromptsWithPrimaryDuration(baseResultList,judicialResultList);
    }


    private void assertResult(List<BaseResult> results, List<uk.gov.justice.core.courts.JudicialResult> judicialResults) {
        assertThat(results.size(), is(judicialResults.size()));
        judicialResults.forEach( judicialResult -> {
            final Optional<BaseResult> baseResultOptional = results.stream().filter(r -> r.getId().equals(judicialResult.getJudicialResultId())).findFirst();
            assertThat(baseResultOptional.isPresent(), is(true));
            assertThat(judicialResult.getCjsCode(), is("cjsCode"));
            assertThat(judicialResult.getLabel(), is("label"));
            assertThat(judicialResult.getCategory(), is(Category.ANCILLARY));
            assertThat(judicialResult.getIsAvailableForCourtExtract(), is(true));
            assertThat(judicialResult.getIsAdjournmentResult(), is(true));
            assertThat(judicialResult.getIsConvictedResult(), is(true));
            assertThat(judicialResult.getIsFinancialResult(), is(true));
            assertThat(judicialResult.getOrderedHearingId(), is(notNullValue()));
            assertThat(judicialResult.getRank(), is(valueOf(1)));
            assertThat(judicialResult.getUsergroups(), is(of("1", "2")));
            assertThat(judicialResult.getWelshLabel(), is("welshLabel"));
            assertThat(judicialResult.getResultText(), is("label"+EOL+"label +10.00"+EOL+"label +10.00"+EOL));
            assertThat(judicialResult.getTerminatesOffenceProceedings(), is(false));
            assertThat(judicialResult.getLifeDuration(), is(false));
            assertThat(judicialResult.getPublishedAsAPrompt(), is(false));
            assertThat(judicialResult.getExcludedFromResults(), is(false));
            assertThat(judicialResult.getAlwaysPublished(), is(false));
            assertThat(judicialResult.getUrgent(), is(false));
            assertThat(judicialResult.getD20(), is(false));
        });

    }


    private void assertJudicialPromptsWithPrimaryDuration(List<BaseResult> results, List<uk.gov.justice.core.courts.JudicialResult> judicialResults) {
        assertThat(results.size(), is(judicialResults.size()));
        judicialResults.forEach( judicialResult -> {
            final Optional<BaseResult> baseResultOptional = results.stream().filter(r -> r.getId().equals(judicialResult.getJudicialResultId())).findFirst();
            assertThat(baseResultOptional.isPresent(), is(true));
            assertThat(judicialResult.getCjsCode(), is("cjsCode"));
            assertThat(judicialResult.getLabel(), is("label"));
            assertThat(judicialResult.getDurationElement().getPrimaryDurationValue(), is(1));
            assertThat(judicialResult.getDurationElement().getPrimaryDurationUnit(), is("L"));
            assertThat(judicialResult.getCategory(), is(Category.ANCILLARY));
            assertThat(judicialResult.getIsAvailableForCourtExtract(), is(true));
            assertThat(judicialResult.getIsAdjournmentResult(), is(true));
            assertThat(judicialResult.getIsConvictedResult(), is(true));
            assertThat(judicialResult.getIsFinancialResult(), is(true));
            assertThat(judicialResult.getOrderedHearingId(), is(notNullValue()));
            assertThat(judicialResult.getRank(), is(valueOf(1)));
            assertThat(judicialResult.getUsergroups(), is(of("1", "2")));
            assertThat(judicialResult.getWelshLabel(), is("welshLabel"));
            assertThat(judicialResult.getResultText(), is("label"+EOL+"label +10.00"+EOL+"label +10.00"+EOL));
            assertThat(judicialResult.getTerminatesOffenceProceedings(), is(false));
            assertThat(judicialResult.getLifeDuration(), is(true));
            assertThat(judicialResult.getPublishedAsAPrompt(), is(false));
            assertThat(judicialResult.getExcludedFromResults(), is(false));
            assertThat(judicialResult.getAlwaysPublished(), is(false));
            assertThat(judicialResult.getUrgent(), is(false));
            assertThat(judicialResult.getD20(), is(false));
        });

    }
}
