package uk.gov.moj.cpp.results.event.helper.sjp;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.core.courts.JudicialResultPrompt.judicialResultPrompt;

import uk.gov.justice.sjp.results.Prompts;
import uk.gov.moj.cpp.results.event.helper.PenaltyPoint;
import uk.gov.moj.cpp.results.event.helper.resultdefinition.ResultDefinition;

import java.math.BigDecimal;
import java.util.List;

public class JudicialResultPrompt {

    public List<uk.gov.justice.core.courts.JudicialResultPrompt> buildJudicialResultPrompt(final ResultDefinition resultDefinition, final List<Prompts> prompts) {

        if (isNotEmpty(prompts)) {
            return prompts.stream()
                    .map(prompt -> {
                                final uk.gov.moj.cpp.results.event.helper.resultdefinition.Prompt promptDefinition = resultDefinition.getPrompts().stream().filter(
                                        promptDef -> promptDef.getId().equals(prompt.getId()))
                                        .findFirst().orElseThrow(() -> new RuntimeException(String.format("no prompt definition found for prompt id: %s value: %s ", prompt.getId(), prompt.getValue())));

                                final ResultAmountSterling resultAmountSterling = new ResultAmountSterling(resultDefinition, promptDefinition, prompt);

                                return judicialResultPrompt()
                                        .withCourtExtract(ofNullable(promptDefinition.getCourtExtract()).orElse("N"))
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
                    .collect(toList());
        }
        return emptyList();
    }

}
