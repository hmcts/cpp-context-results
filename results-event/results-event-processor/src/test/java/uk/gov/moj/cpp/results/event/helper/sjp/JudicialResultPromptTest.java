package uk.gov.moj.cpp.results.event.helper.sjp;

import static java.math.BigDecimal.valueOf;
import static java.util.UUID.randomUUID;
import static junit.framework.TestCase.assertNull;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static uk.gov.moj.cpp.results.event.helper.TestTemplate.buildListOfPrompt;
import static uk.gov.moj.cpp.results.event.helper.TestTemplate.buildPromptsWithPromptIdAndValue;
import static uk.gov.moj.cpp.results.event.helper.TestTemplate.buildResultDefinition;
import static uk.gov.moj.cpp.results.event.helper.TestTemplate.buildResultDefinitionWithId;

import uk.gov.justice.sjp.results.Prompts;
import uk.gov.moj.cpp.results.event.helper.resultdefinition.Prompt;
import uk.gov.moj.cpp.results.event.helper.resultdefinition.ResultDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.Test;

public class JudicialResultPromptTest {

    @Test
    public void testBuildJudicialResultPrompt() {

       final  ResultDefinition resultDefinition = buildResultDefinition();
       final  List<Prompts> promptsList = buildListOfPrompt();

        final List<uk.gov.justice.core.courts.JudicialResultPrompt> judicialResultPromptList = new JudicialResultPrompt().buildJudicialResultPrompt(resultDefinition, promptsList);
        final uk.gov.justice.core.courts.JudicialResultPrompt judicialResultPrompt = judicialResultPromptList.get(0);

        assertJudicialResultPrompt(resultDefinition, promptsList, judicialResultPrompt, resultDefinition.getPrompts().get(0));
    }

    @Test
    public void testPenaltyPointsInJudicialResultPrompt(){

        final UUID id = randomUUID();
        final String value = "108.00";
        final String promptReference = "PENPT";
        final ResultDefinition resultDefinition = buildResultDefinitionWithId(id, promptReference);
        final Prompts prompts = buildPromptsWithPromptIdAndValue(id, value);
        final List<Prompts> promptsList = new ArrayList<>();
        promptsList.add(prompts);

        final List<uk.gov.justice.core.courts.JudicialResultPrompt> judicialResultPromptList = new JudicialResultPrompt().buildJudicialResultPrompt(resultDefinition, promptsList);
        final uk.gov.justice.core.courts.JudicialResultPrompt judicialResultPrompt = judicialResultPromptList.get(0);
        assertThat(judicialResultPrompt.getTotalPenaltyPoints().toString() , is(value) ) ;

    }


    @Test
    public void testPenaltyPointsIsNullInJudicialResultPrompt(){

        final UUID id = randomUUID();
        final String value = "108.00";
        final String promptReference = "NONE";
        final ResultDefinition resultDefinition = buildResultDefinitionWithId(id, promptReference);
        final Prompts prompts = buildPromptsWithPromptIdAndValue(id, value);
        final List<Prompts> promptsList = new ArrayList<>();
        promptsList.add(prompts);

        final List<uk.gov.justice.core.courts.JudicialResultPrompt> judicialResultPromptList = new JudicialResultPrompt().buildJudicialResultPrompt(resultDefinition, promptsList);
        final uk.gov.justice.core.courts.JudicialResultPrompt judicialResultPrompt = judicialResultPromptList.get(0);
        assertNull(judicialResultPrompt.getTotalPenaltyPoints()) ;

    }

    private void assertJudicialResultPrompt(final ResultDefinition resultDefinition, final List<Prompts> promptsList, final uk.gov.justice.core.courts.JudicialResultPrompt judicialResultPrompt, final Prompt prompt) {
        assertThat(judicialResultPrompt.getLabel(), is(resultDefinition.getLabel()));
        assertThat(judicialResultPrompt.getCourtExtract(), is(prompt.getCourtExtract()));
        assertThat(judicialResultPrompt.getPromptReference(), is(prompt.getReference()));
        assertThat(judicialResultPrompt.getPromptSequence(), is(valueOf(prompt.getSequence())));
        assertThat(judicialResultPrompt.getValue(), is("+10.00"));
        assertThat(judicialResultPrompt.getIsFinancialImposition(), is(true));
        assertThat(judicialResultPrompt.getWelshLabel(), is(prompt.getWelshLabel()));
        assertThat(judicialResultPrompt.getUsergroups(), is(prompt.getUserGroups()));
        assertThat(judicialResultPrompt.getDurationSequence(), is(prompt.getDurationSequence()));
    }


}

