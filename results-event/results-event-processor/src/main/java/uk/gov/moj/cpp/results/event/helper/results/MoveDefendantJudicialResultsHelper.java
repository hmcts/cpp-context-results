package uk.gov.moj.cpp.results.event.helper.results;

import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

import uk.gov.justice.core.courts.JudicialResultCategory;
import uk.gov.justice.core.courts.DefendantJudicialResult;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.Offence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class MoveDefendantJudicialResultsHelper {

    private final Predicate<JudicialResult> interimOrWithdrawnResultPredicate = judicialResult -> (nonNull(judicialResult.getCategory()) && judicialResult.getCategory().equals(JudicialResultCategory.INTERMEDIARY) || nonNull(judicialResult.getTerminatesOffenceProceedings()) && judicialResult.getTerminatesOffenceProceedings().equals(Boolean.TRUE));
    private final Predicate<JudicialResult> notInterimOrNotWithdrawnResultPredicate = judicialResult -> (nonNull(judicialResult.getCategory()) && !judicialResult.getCategory().equals(JudicialResultCategory.INTERMEDIARY) || nonNull(judicialResult.getTerminatesOffenceProceedings()) && judicialResult.getTerminatesOffenceProceedings().equals(Boolean.FALSE));
    private final Predicate<JudicialResult> notInterimAndNotWithdrawnResultPredicate = judicialResult -> (nonNull(judicialResult.getCategory()) && !judicialResult.getCategory().equals(JudicialResultCategory.INTERMEDIARY) && nonNull(judicialResult.getTerminatesOffenceProceedings()) && judicialResult.getTerminatesOffenceProceedings().equals(Boolean.FALSE));

    public List<Offence> buildOffenceAndDefendantJudicialResults(final List<Offence> originalOffences, final List<JudicialResult> defendantCaseJudicialResults, final List<DefendantJudicialResult> defendantJudicialResults) {
        final List<Offence> noneMatchUpdatedOffences = noneMatchBuildOffenceAndDefendantJudicialResults(originalOffences, defendantCaseJudicialResults, defendantJudicialResults);
        return isNotEmpty(noneMatchUpdatedOffences) ? noneMatchUpdatedOffences : allMatchBuildOffenceAndDefendantJudicialResults(originalOffences, defendantCaseJudicialResults, defendantJudicialResults);
    }

    public List<Offence> noneMatchBuildOffenceAndDefendantJudicialResults(final List<Offence> originalOffences, final List<JudicialResult> defendantCaseJudicialResults, final List<DefendantJudicialResult> defendantJudicialResults) {
        final List<Offence> noneMatchFilteredOffenceList = originalOffences.stream()
                .filter(Objects::nonNull)
                .filter(offence -> !hasInterimOrWithdrawnResultsExist(offence))
                .collect(Collectors.toList());

        return addNoneMatchDefendantJudicialResultsAndOffenceJudicialResults(defendantCaseJudicialResults, defendantJudicialResults, noneMatchFilteredOffenceList, originalOffences);
    }

    private boolean hasInterimOrWithdrawnResultsExist(Offence offence) {
        return isNotEmpty(offence.getJudicialResults()) &&
                offence.getJudicialResults()
                        .stream()
                        .filter(Objects::nonNull)
                        .noneMatch(notInterimOrNotWithdrawnResultPredicate);
    }

    public List<Offence> allMatchBuildOffenceAndDefendantJudicialResults(final List<Offence> originalOffences, final List<JudicialResult> defendantCaseJudicialResults, final List<DefendantJudicialResult> defendantJudicialResults) {
        final List<Offence> allMatchFilteredOffenceList = originalOffences.stream()
                .filter(Objects::nonNull)
                .filter(offence -> offence
                        .getJudicialResults()
                        .stream()
                        .filter(Objects::nonNull)
                        .allMatch(interimOrWithdrawnResultPredicate))
                .collect(toList());
        final List<Offence> updatedOffences = addDefendantJudicialResultsAndOffenceJudicialResults(defendantCaseJudicialResults, defendantJudicialResults, allMatchFilteredOffenceList, originalOffences);
        return isNotEmpty(updatedOffences) ? updatedOffences : originalOffences;
    }

    private List<Offence> addNoneMatchDefendantJudicialResultsAndOffenceJudicialResults(final List<JudicialResult> defendantCaseJudicialResults, final List<DefendantJudicialResult> defendantJudicialResults, final List<Offence> filteredOffenceList, final List<Offence> originalOffences) {
        List<JudicialResult> updatedJudicialResults = null;
        if (isNotEmpty(filteredOffenceList)) {

            final Optional<Offence> optionalOffence = filteredOffenceList.stream().findFirst();

            final Offence offence = optionalOffence.orElse(null);

            final Offence filteredOffence = getFilteredOffenceForNoneMatch(filteredOffenceList, offence);

            if (isNull(filteredOffence)) {
                final Optional<Offence> originalOptionalOffence = originalOffences.stream().findFirst();
                final Offence originalFirstOffence = originalOptionalOffence.orElse(null);
                updatedJudicialResults = nonNull(originalFirstOffence) ? originalFirstOffence.getJudicialResults() : emptyList();
            }

            updatedJudicialResults = appendDefendantJudicialResultsToOffence(defendantCaseJudicialResults, defendantJudicialResults, updatedJudicialResults, filteredOffence);

            return isNotEmpty(originalOffences) ? updatedOffencesWithJudicialResultsForNoneMatch(originalOffences, updatedJudicialResults) : originalOffences;
        }
        return emptyList();
    }

    private List<JudicialResult> appendDefendantJudicialResultsToOffence(final List<JudicialResult> defendantCaseJudicialResults, final List<DefendantJudicialResult> defendantJudicialResults, List<JudicialResult> updatedJudicialResults, final Offence filteredOffence) {
        List<JudicialResult> judicialResults = new ArrayList<>();
        if (isNotEmpty(defendantJudicialResults)) {
            judicialResults = defendantJudicialResults.stream().map(DefendantJudicialResult::getJudicialResult).collect(toList());
        }
        if (nonNull(filteredOffence) && isNotEmpty(filteredOffence.getJudicialResults())) {
            updatedJudicialResults = Stream.of(defendantCaseJudicialResults, filteredOffence.getJudicialResults(), judicialResults)
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream)
                    .collect(toList());
        }
        return updatedJudicialResults;
    }

    private Offence getFilteredOffenceForNoneMatch(final List<Offence> filteredOffenceList, final Offence offence) {
        final Optional<Offence> optionalFilteredOffence = filteredOffenceList.stream()
                .filter(Objects::nonNull)
                .filter(off -> !(nonNull(offence) && isNotEmpty(offence.getJudicialResults()) && nonNull(off.getJudicialResults()) && off.getJudicialResults().stream().filter(Objects::nonNull).noneMatch(notInterimAndNotWithdrawnResultPredicate)))
                .findFirst();

        return optionalFilteredOffence.orElse(offence);
    }

    private List<Offence> addDefendantJudicialResultsAndOffenceJudicialResults(final List<JudicialResult> defendantCaseJudicialResults, final List<DefendantJudicialResult> defendantJudicialResults, final List<Offence> filteredOffenceList, final List<Offence> originalOffences) {
        final Optional<Offence> optionalOffence = filteredOffenceList.stream()
                .filter(Objects::nonNull)
                .filter(offence -> offence.getJudicialResults().stream().filter(Objects::nonNull).anyMatch(interimOrWithdrawnResultPredicate))
                .findFirst();

        final Offence firstOffence = optionalOffence.orElse(null);

        List<JudicialResult> updatedJudicialResults = null;

        if (isNull(firstOffence)) {
            final Optional<Offence> originalOptionalOffence = originalOffences.stream().findFirst();
            final Offence originalFirstOffence = originalOptionalOffence.orElse(null);
            updatedJudicialResults = nonNull(originalFirstOffence) ? originalFirstOffence.getJudicialResults() : emptyList();
        }

        updatedJudicialResults = appendDefendantJudicialResultsToOffence(defendantCaseJudicialResults, defendantJudicialResults, updatedJudicialResults, firstOffence);

        return isNotEmpty(originalOffences) ? updatedOffencesWithJudicialResultsForAllMatch(originalOffences, updatedJudicialResults) : originalOffences;
    }

    private List<Offence> updatedOffencesWithJudicialResultsForNoneMatch(final List<Offence> originalOffences, List<JudicialResult> updatedJudicialResults) {
        final List<Offence> updatedOffenceList = new ArrayList<>(originalOffences);
        final Optional<Offence> optionalOffence = updatedOffenceList.stream()
                .filter(Objects::nonNull)
                .filter(offence -> !(isNotEmpty(offence.getJudicialResults()) && offence.getJudicialResults().stream().filter(Objects::nonNull).noneMatch(notInterimOrNotWithdrawnResultPredicate)))
                .findFirst();

        final Offence offence = optionalOffence.orElse(null);

        final Optional<Offence> optionalFilteredOffence = updatedOffenceList.stream().filter(Objects::nonNull)
                .filter(offence2 -> !(isNotEmpty(offence2.getJudicialResults()) && offence2.getJudicialResults().stream().filter(Objects::nonNull).noneMatch(notInterimAndNotWithdrawnResultPredicate)))
                .findFirst();

        final Offence filteredOffence = optionalFilteredOffence.orElse(offence);

        if (nonNull(filteredOffence) && isNotEmpty(updatedJudicialResults)) {
            filteredOffence.setJudicialResults(updatedJudicialResults);
        }
        return updatedOffenceList;
    }

    private List<Offence> updatedOffencesWithJudicialResultsForAllMatch(final List<Offence> originalOffences, List<JudicialResult> updatedJudicialResults) {
        final List<Offence> updatedOffenceList = new ArrayList<>(originalOffences);
        final Optional<Offence> optionalOffence = updatedOffenceList.stream().filter(Objects::nonNull).filter(offence -> offence
                .getJudicialResults()
                .stream()
                .filter(Objects::nonNull)
                .allMatch(interimOrWithdrawnResultPredicate)).findFirst();

        if (optionalOffence.isPresent() && isNotEmpty(updatedJudicialResults)) {
            optionalOffence.get().setJudicialResults(updatedJudicialResults);
        }
        return updatedOffenceList;
    }
}
