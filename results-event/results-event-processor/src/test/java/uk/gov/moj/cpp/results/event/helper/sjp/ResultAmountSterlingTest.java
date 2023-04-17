package uk.gov.moj.cpp.results.event.helper.sjp;

import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import uk.gov.justice.sjp.results.Prompts;
import uk.gov.moj.cpp.results.event.helper.resultdefinition.Prompt;
import uk.gov.moj.cpp.results.event.helper.resultdefinition.ResultDefinition;

import java.util.UUID;

import org.junit.Test;

public class ResultAmountSterlingTest {

    final Prompt promptReferenceData0 =
            Prompt.prompt()
                    .setId(UUID.randomUUID())
                    .setLabel("promptReferenceData0")
                    .setReference("AOBD")
                    .setUserGroups(asList("usergroup0", "usergroup1"));

    final Prompts prompt0 = Prompts.prompts()
            .withValue("10")
            .withId(promptReferenceData0.getId())
            .build();

    final ResultDefinition resultDefinition = ResultDefinition.resultDefinition().setFinancial("Y");


    @Test
    public void shouldReturnTrueWhenPromptReferenceIsAOBDIsPresent(){
        final ResultAmountSterling resultAmountSterling = new ResultAmountSterling(resultDefinition, promptReferenceData0, prompt0);
        boolean result = resultAmountSterling.isPresent();
        assertThat(result, is(true));

    }

    @Test
    public void shouldReturnTrueWhenPromptReferenceIsAOCOMIsPresent(){
        promptReferenceData0.setReference("AOCOM");
        final ResultAmountSterling resultAmountSterling = new ResultAmountSterling(resultDefinition, promptReferenceData0, prompt0);
        boolean result = resultAmountSterling.isPresent();
        assertThat(result, is(true));
    }

    @Test
    public void shouldReturnTrueWhenPromptReferenceIsAOFIsPresent(){
        promptReferenceData0.setReference("AOF");
        final ResultAmountSterling resultAmountSterling = new ResultAmountSterling(resultDefinition, promptReferenceData0, prompt0);
        boolean result = resultAmountSterling.isPresent();
        assertThat(result, is(true));
    }

    @Test
    public void shouldReturnTrueWhenPromptReferenceIsAOSIsPresent(){
        promptReferenceData0.setReference("AOS");
        final ResultAmountSterling resultAmountSterling = new ResultAmountSterling(resultDefinition, promptReferenceData0, prompt0);
        boolean result = resultAmountSterling.isPresent();
        assertThat(result, is(true));
        assertThat(resultAmountSterling.getAmount(), is("+10.00"));
    }

    @Test
    public void shouldReturnFalseWhenPromptReferenceZAOSIsPresent(){
        promptReferenceData0.setReference("ZAOS");
        final ResultAmountSterling resultAmountSterling = new ResultAmountSterling(resultDefinition, promptReferenceData0, prompt0);
        boolean result = resultAmountSterling.isPresent();
        assertThat(result, is(false));
    }

    @Test
    public void shouldReturnTrueWhenPromptReferenceAOCIsPresent(){
        promptReferenceData0.setReference("AOC");
        final ResultAmountSterling resultAmountSterling = new ResultAmountSterling(resultDefinition, promptReferenceData0, prompt0);
        boolean result = resultAmountSterling.isPresent();
        assertThat(result, is(true));
        assertThat(resultAmountSterling.getAmount(), is("+10.00"));
    }

    @Test
    public void shouldReturnFalseWhenPromptReferenceZAOCIsPresent(){
        promptReferenceData0.setReference("ZAOC");
        final ResultAmountSterling resultAmountSterling = new ResultAmountSterling(resultDefinition, promptReferenceData0, prompt0);
        boolean result = resultAmountSterling.isPresent();
        assertThat(result, is(false));
    }

    @Test
    public void shouldReturnFalseWhenPromptReferenceIsPENPTIsPresent(){
        promptReferenceData0.setReference("PENPT");
        final ResultAmountSterling resultAmountSterling = new ResultAmountSterling(resultDefinition, promptReferenceData0, prompt0);
        boolean result = resultAmountSterling.isPresent();
        assertThat(result, is(false));
    }

    @Test
    public void getValueForFinancialPromptWithPostiveValueShouldRetuenPostiveSterlingAmount(){
        final ResultAmountSterling resultAmountSterling = new ResultAmountSterling(resultDefinition, promptReferenceData0, prompt0);
        boolean result = resultAmountSterling.isPresent();
        assertThat(result, is(true));
        assertThat(resultAmountSterling.getAmount(), is("+10.00"));
    }

    @Test
    public void getValueForFinancialPromptWithNegativeValueShouldRetuenNegativeSterlingAmount(){
        final Prompts finalPrompt0 = Prompts.prompts().withValuesFrom(prompt0).withValue("-10").build();
        final ResultAmountSterling resultAmountSterling = new ResultAmountSterling(resultDefinition, promptReferenceData0, finalPrompt0);
        boolean result = resultAmountSterling.isPresent();
        assertThat(result, is(true));
        assertThat(resultAmountSterling.getAmount(), is("-10.00"));
    }

    @Test
    public void getValueForFinancialPromptWithZeroValueShouldRetuenZeroSterlingAmount(){
        final Prompts finalPrompt0 = Prompts.prompts().withValuesFrom(prompt0).withValue("0").build();
        final ResultAmountSterling resultAmountSterling = new ResultAmountSterling(resultDefinition, promptReferenceData0, finalPrompt0);
        boolean result = resultAmountSterling.isPresent();
        assertThat(result, is(true));
        assertThat(resultAmountSterling.getAmount(), is("0.00"));
    }
}
