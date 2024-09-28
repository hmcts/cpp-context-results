package uk.gov.moj.cpp.results.event.helper.results;

import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;

import uk.gov.justice.core.courts.DefendantJudicialResult;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JudicialResultCategory;
import uk.gov.justice.core.courts.Offence;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class MoveDefendantJudicialResultsHelper {

    public List<Offence> buildOffenceAndDefendantJudicialResults(final List<Offence> originalOffences) {
        return buildOffenceWithCaseDefendantAndDefendantLevelJudicialResults(Optional.empty(), originalOffences, emptyList(), emptyList());
    }

    public List<Offence> buildOffenceWithCaseDefendantAndDefendantLevelJudicialResults(final Optional<UUID> masterDefendantId, final List<Offence> originalOffences,
                                                                                       final List<JudicialResult> caseDefendantJudicialResults, final List<DefendantJudicialResult> defendantJudicialResults) {

        if (isNull(originalOffences) || originalOffences.isEmpty()) {
            return originalOffences;
        }

        List<Offence> offencesWithCaseOrDefendantLevelResults = originalOffences.stream()
                .filter(Objects::nonNull)
                .map(offence -> updateMatchingCaseDefendantJudicialResults(offence, caseDefendantJudicialResults))
                .map(offence -> updateMatchingDefendantJudicialResults(offence, masterDefendantId, defendantJudicialResults))
                .collect(toList());

        //update offences with defendant level JRs that are not associated with any offence, eg. DDCH
        return updateMatchingCaseDefendantJudicialResultsWithNoOffenceId(offencesWithCaseOrDefendantLevelResults, caseDefendantJudicialResults);
    }

    private Offence updateMatchingCaseDefendantJudicialResults(Offence offence, List<JudicialResult> defendantCaseJudicialResults) {
        if (isNull(defendantCaseJudicialResults)) {
            return offence;
        }

        List<JudicialResult> matchingCaseDefendantJudicialResults = defendantCaseJudicialResults.stream()
                .filter(defendantCaseJudicialResult -> nonNull(defendantCaseJudicialResult.getOffenceId())
                        && defendantCaseJudicialResult.getOffenceId().equals(offence.getId()))
                .collect(toList());

        return addJudicialResultsToOffence(offence, matchingCaseDefendantJudicialResults);
    }

    private Offence updateMatchingDefendantJudicialResults(Offence offence, Optional<UUID> masterDefendantId, List<DefendantJudicialResult> defendantJudicialResults) {
        if (isNull(defendantJudicialResults)) {
            return offence;
        }

        List<JudicialResult> matchingDefendantJudicialResults = defendantJudicialResults.stream()
                .filter(djr -> masterDefendantId.isPresent() && masterDefendantId.get().equals(djr.getMasterDefendantId()))
                .map(DefendantJudicialResult::getJudicialResult)
                .filter(judicialResult -> nonNull(judicialResult.getOffenceId()) && judicialResult.getOffenceId().equals(offence.getId()))
                .collect(toList());

        return addJudicialResultsToOffence(offence, matchingDefendantJudicialResults);
    }

    private List<Offence> updateMatchingCaseDefendantJudicialResultsWithNoOffenceId(List<Offence> offencesWithCaseOrDefendantLevelResults,
                                                                                    final List<JudicialResult> caseDefendantJudicialResults) {
        if (isNull(caseDefendantJudicialResults)) {
            return offencesWithCaseOrDefendantLevelResults;
        }
        List<JudicialResult> caseDefendantJudicialResultsNotAssociatedWithOffence = caseDefendantJudicialResults.stream()
                .filter(judicialResult -> isNull(judicialResult.getOffenceId()))
                .collect(toList());

        //Add to the first offence that has JudicialResults with ResultCategory intermediary; if none found add DDCH to first offence
        if (!caseDefendantJudicialResultsNotAssociatedWithOffence.isEmpty()) {
            Optional<Offence> firstOffenceWithInterimResults = offencesWithCaseOrDefendantLevelResults.stream().filter(Objects::nonNull)
                    .filter(offence -> nonNull(offence.getJudicialResults())
                            && offence.getJudicialResults().stream()
                            .anyMatch(ojr -> nonNull(ojr.getCategory()) && ojr.getCategory().equals(JudicialResultCategory.INTERMEDIARY)))
                    .findFirst();

            if (firstOffenceWithInterimResults.isPresent()) {
                offencesWithCaseOrDefendantLevelResults.stream()
                        .filter(Objects::nonNull)
                        .filter(o -> o.getId().equals(firstOffenceWithInterimResults.get().getId()))
                        .forEach(o -> offencesWithCaseOrDefendantLevelResults.set(offencesWithCaseOrDefendantLevelResults.indexOf(o), addJudicialResultsToOffence(o, caseDefendantJudicialResultsNotAssociatedWithOffence)));
            } else {
                offencesWithCaseOrDefendantLevelResults.stream().filter(Objects::nonNull)
                        .findFirst()
                        .ifPresent(o -> offencesWithCaseOrDefendantLevelResults.set(offencesWithCaseOrDefendantLevelResults.indexOf(o), addJudicialResultsToOffence(o, caseDefendantJudicialResultsNotAssociatedWithOffence)));
            }
        }

        return offencesWithCaseOrDefendantLevelResults;
    }

    private Offence addJudicialResultsToOffence(Offence offence, List<JudicialResult> matchingCaseOrDefendantLevelJudicialResults) {
        if (!matchingCaseOrDefendantLevelJudicialResults.isEmpty()) {
            List<JudicialResult> updatedJudicialResultList = new ArrayList<>();

            if (nonNull(offence.getJudicialResults())) {
                updatedJudicialResultList.addAll(offence.getJudicialResults());
                updatedJudicialResultList.addAll(matchingCaseOrDefendantLevelJudicialResults);
            } else {
                updatedJudicialResultList.addAll(matchingCaseOrDefendantLevelJudicialResults);
            }

            return Offence.offence().withValuesFrom(offence).withJudicialResults(updatedJudicialResultList).build();
        }
        return offence;
    }
}
