package uk.gov.moj.cpp.results.event.helper.sjp;

import static java.lang.String.format;
import static java.lang.System.lineSeparator;
import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsLast;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

import uk.gov.justice.core.courts.JudicialResultPrompt;
import uk.gov.moj.cpp.results.event.helper.resultdefinition.ResultDefinition;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

public class ResultTextHelper {


    public String getResultText(final ResultDefinition resultDefinition, final List<JudicialResultPrompt> judicialResultPromptList) {
        final List<JudicialResultPrompt>  judicialResultPromptsBasedOnSequence = getJudicialResultPromptsBasedOnSequence(judicialResultPromptList);
        return constructResultText(resultDefinition.getLabel(),judicialResultPromptsBasedOnSequence);
    }

    private String constructResultText(final String resultDefinitionLabel, final List<JudicialResultPrompt> judicialResultPromptsBasedOnSequence) {
        final StringBuilder resultTextBuilder = new StringBuilder();
            resultTextBuilder.append(format("%s%s", resultDefinitionLabel, lineSeparator()));
            for(final JudicialResultPrompt judicialResultPrompt : judicialResultPromptsBasedOnSequence) {
                resultTextBuilder.append(format("%s %s", judicialResultPrompt.getLabel(), judicialResultPrompt.getValue()));
                resultTextBuilder.append(lineSeparator());
            }
        return resultTextBuilder.toString();
    }

    private List<JudicialResultPrompt> getJudicialResultPromptsBasedOnSequence(final List<JudicialResultPrompt> judicialResultPromptList) {

        return ofNullable(judicialResultPromptList)
                .map(Collection::stream)
                .orElseGet(Stream::empty)
                .sorted(comparing(JudicialResultPrompt::getPromptSequence, nullsLast(naturalOrder())))
                .collect(toList());
    }

    }
