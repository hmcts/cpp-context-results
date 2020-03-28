package uk.gov.moj.cpp.results.event.helper.sjp;

import static java.util.Collections.emptyList;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

import uk.gov.justice.sjp.results.Prompts;
import uk.gov.moj.cpp.results.event.helper.PenaltyPoint;
import uk.gov.moj.cpp.results.event.helper.resultdefinition.ResultDefinition;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

public class JudicialResultPrompt {

    public List<uk.gov.justice.core.courts.JudicialResultPrompt> buildJudicialResultPrompt(final ResultDefinition resultDefinition, final List<Prompts> prompts) {

        if(isNotEmpty(prompts)) {
            final List<uk.gov.justice.core.courts.JudicialResultPrompt> promptList = prompts.stream()
                    .map(prompt -> {

                                final uk.gov.moj.cpp.results.event.helper.resultdefinition.Prompt promptDefinition = resultDefinition.getPrompts().stream().filter(
                                        promptDef -> promptDef.getId().equals(prompt.getId()))
                                        .findFirst().orElseThrow(() -> new RuntimeException(String.format("no prompt definition found for prompt id: %s value: %s ", prompt.getId(), prompt.getValue())));

                                ResultAmountSterling resultAmountSterling = new ResultAmountSterling(resultDefinition, promptDefinition, prompt);

                                return uk.gov.justice.core.courts.JudicialResultPrompt.judicialResultPrompt()
                                        .withCourtExtract(promptDefinition.getCourtExtract())
                                        .withLabel(promptDefinition.getLabel())
                                        .withTotalPenaltyPoints(new PenaltyPoint().getPenaltyPointFromResults(promptDefinition, prompt))
                                        .withDurationSequence(promptDefinition.getDurationSequence() == null ? null : promptDefinition.getDurationSequence())
                                        .withPromptReference(promptDefinition.getReference())
                                        .withPromptSequence(promptDefinition.getSequence() == null ? null : BigDecimal.valueOf(promptDefinition.getSequence()))
                                        .withUsergroups(promptDefinition.getUserGroups())
                                        .withValue(resultAmountSterling.isPresent() ? resultAmountSterling.getAmount() : prompt.getValue())
                                        .withIsFinancialImposition(resultAmountSterling.isPresent())
                                        .withWelshLabel(promptDefinition.getWelshLabel())
                                        .withJudicialResultPromptTypeId(promptDefinition.getId())
                                        .build();
                            }
                    )
                    .collect(Collectors.toList());
            return promptList;
        }
        return emptyList();
    }

}
