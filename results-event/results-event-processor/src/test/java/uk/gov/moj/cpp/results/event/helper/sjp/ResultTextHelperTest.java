package uk.gov.moj.cpp.results.event.helper.sjp;

import static com.google.common.collect.ImmutableList.of;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static uk.gov.justice.core.courts.JudicialResultPrompt.judicialResultPrompt;
import static uk.gov.moj.cpp.results.event.helper.TestTemplate.buildResultDefinition;

import uk.gov.justice.core.courts.JudicialResultPrompt;
import uk.gov.moj.cpp.results.event.helper.resultdefinition.ResultDefinition;

import java.math.BigDecimal;
import java.util.List;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

public class ResultTextHelperTest {

    private static final String FIELD_LABEL_VALUE = "label";

    @Test
    public void testResultText_whenJudicialResultPromptSequenceIsNotNull() {
        final ResultDefinition resultDefinition = buildResultDefinition();

        final String resultText = new ResultTextHelper().getResultText(resultDefinition, buildJudicialResultPromptList());

        assertThat(resultDefinition.getLabel(), is(FIELD_LABEL_VALUE));
        assertThat(resultText, is("label\nlabel1 11111\nlabel2 22222\nlabel3 33333\nlabel4 44444\nlabel5 55555\n"));
    }


    @Test
    public void testResultText_whenJudicialResultPromptSequenceIsnNull() {
        final ResultDefinition resultDefinition = buildResultDefinition();
        final List<JudicialResultPrompt> judicialResultPromptList = buildJudicialResultPromptList();
        judicialResultPromptList.get(3).setPromptSequence(null);
        judicialResultPromptList.get(4).setPromptSequence(null);

        final String resultText = new ResultTextHelper().getResultText(resultDefinition, judicialResultPromptList);

        assertThat(resultDefinition.getLabel(), is(FIELD_LABEL_VALUE));
        assertThat(resultText, is("label\nlabel3 33333\nlabel4 44444\nlabel5 55555\nlabel2 22222\nlabel1 11111\n"));
    }

    private ImmutableList<JudicialResultPrompt> buildJudicialResultPromptList() {
        return of(buildJudicialResultPrompt(new BigDecimal(5), "label5", "55555")
                , buildJudicialResultPrompt(new BigDecimal(4), "label4", "44444")
                , buildJudicialResultPrompt(new BigDecimal(3), "label3", "33333")
                , buildJudicialResultPrompt(new BigDecimal(2), "label2", "22222")
                , buildJudicialResultPrompt(new BigDecimal(1), "label1", "11111")
        );
    }

    private uk.gov.justice.core.courts.JudicialResultPrompt buildJudicialResultPrompt(final BigDecimal sequence, final String label, final String value) {
        return judicialResultPrompt()
                .withPromptSequence(sequence)
                .withLabel(label).withValue(value)
                .build();
    }
}
