package uk.gov.moj.cpp.results.event.helper.results;

import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

import uk.gov.justice.core.courts.Category;
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

    private Predicate<JudicialResult> interimOrWithdrawnResultPredicate = judicialResult -> (nonNull(judicialResult.getCategory())&& judicialResult.getCategory().equals(Category.INTERMEDIARY) || nonNull(judicialResult.getTerminatesOffenceProceedings()) && judicialResult.getTerminatesOffenceProceedings().equals(Boolean.TRUE));
    private Predicate<JudicialResult> notInterimOrNotWithdrawnResultPredicate = judicialResult -> (nonNull(judicialResult.getCategory()) && !judicialResult.getCategory().equals(Category.INTERMEDIARY) || nonNull(judicialResult.getTerminatesOffenceProceedings()) && judicialResult.getTerminatesOffenceProceedings().equals(Boolean.FALSE));
    private Predicate<JudicialResult> notInterimAndNotWithdrawnResultPredicate = judicialResult -> (nonNull(judicialResult.getCategory()) && !judicialResult.getCategory().equals(Category.INTERMEDIARY) && nonNull(judicialResult.getTerminatesOffenceProceedings()) && judicialResult.getTerminatesOffenceProceedings().equals(Boolean.FALSE));

    public List<Offence> buildOffenceAndDefendantJudicialResults(final List<Offence> originalOffences, final List<JudicialResult> defendantJudicialResults, final List<DefendantJudicialResult> hearingLevelResults) {
        final List<Offence> noneMatchUpdatedOffences = noneMatchBuildOffenceAndDefendantJudicialResults(originalOffences, defendantJudicialResults, hearingLevelResults);
      return isNotEmpty(noneMatchUpdatedOffences) ? noneMatchUpdatedOffences : allMatchBuildOffenceAndDefendantJudicialResults(originalOffences, defendantJudicialResults, hearingLevelResults);
    }

    public List<Offence> noneMatchBuildOffenceAndDefendantJudicialResults(final List<Offence> originalOffences, final List<JudicialResult> defendantJudicialResults,final List<DefendantJudicialResult> hearingLevelResults) {
        final List<Offence> noneMatchFilteredOffenceList = originalOffences.stream()
                .filter(Objects::nonNull)
                .filter(offence -> !(nonNull(offence)&& isNotEmpty(offence.getJudicialResults()) &&
                        offence.getJudicialResults()
                        .stream()
                        .filter(Objects::nonNull)
                        .filter(notInterimOrNotWithdrawnResultPredicate)
                        .collect(toList()).isEmpty()))
                .collect(Collectors.toList());
        return addNoneMatchDefendantJudicialResultsAndOffenceJudicialResults(defendantJudicialResults,hearingLevelResults , noneMatchFilteredOffenceList, originalOffences);
    }

    public List<Offence> allMatchBuildOffenceAndDefendantJudicialResults(final List<Offence> originalOffences, final List<JudicialResult> defendantJudicialResults, final List<DefendantJudicialResult> hearingLevelResults) {
        final List<Offence> allMatchFilteredOffenceList = originalOffences.stream()
                .filter(Objects::nonNull)
                .filter(offence ->offence
                        .getJudicialResults()
                        .stream()
                        .filter(Objects::nonNull)
                        .allMatch(interimOrWithdrawnResultPredicate))
                .collect(toList());
        final List<Offence> updatedOffences = addDefendantJudicialResultsAndOffenceJudicialResults(defendantJudicialResults, hearingLevelResults,allMatchFilteredOffenceList, originalOffences);
        return isNotEmpty(updatedOffences) ? updatedOffences : originalOffences;
    }

    private List<Offence> addNoneMatchDefendantJudicialResultsAndOffenceJudicialResults(final List<JudicialResult> defendantJudicialResults, final List<DefendantJudicialResult>  hearingLevelJudicialResults, final List<Offence> filteredOffenceList, final List<Offence> originalOffences) {
        List<JudicialResult> updatedJudicialResults = null;
        if(isNotEmpty(filteredOffenceList)) {
            final Optional<Offence> optionalOffence = filteredOffenceList.stream()
                    .filter(Objects::nonNull).filter(offence -> !(nonNull(offence)&& isNotEmpty(offence.getJudicialResults()) && offence.getJudicialResults().stream()
                            .filter(Objects::nonNull).filter(notInterimOrNotWithdrawnResultPredicate).collect(toList()).isEmpty())).findFirst();
            final Offence offence = optionalOffence.isPresent() ? optionalOffence.get() : null;

            final Offence filteredOffence = getFilteredOffenceForNoneMatch(filteredOffenceList, offence);

            if (filteredOffence == null) {
                final Optional<Offence> originalOptionalOffence = originalOffences.stream().findFirst();
                final Offence originalFirstOffence = originalOptionalOffence.isPresent() ? originalOptionalOffence.get() : null;
                updatedJudicialResults = nonNull(originalFirstOffence) ? originalFirstOffence.getJudicialResults() : emptyList();
            }

            updatedJudicialResults = appendDefendantJudicialResultsToOffence(defendantJudicialResults, hearingLevelJudicialResults, updatedJudicialResults, filteredOffence);
            return isNotEmpty(originalOffences) ? updatedOffencesWithJudicialResultsForNoneMatch(originalOffences, updatedJudicialResults) : originalOffences;
        }
        return emptyList();
    }

    private List<JudicialResult> appendDefendantJudicialResultsToOffence(final List<JudicialResult> defendantJudicialResults,  final List<DefendantJudicialResult> hearingLevelJudicialResults,List<JudicialResult> updatedJudicialResults, final Offence filteredOffence) {
        List<JudicialResult> hearingJudicialResults = new ArrayList<>();
        if(isNotEmpty(hearingLevelJudicialResults)) {
            hearingJudicialResults = hearingLevelJudicialResults.stream().map(DefendantJudicialResult :: getJudicialResult).collect(toList());
        }
        if (nonNull(filteredOffence) && isNotEmpty(filteredOffence.getJudicialResults())) {
            updatedJudicialResults = Stream.of(defendantJudicialResults, filteredOffence.getJudicialResults(), hearingJudicialResults)
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream)
                    .collect(toList());
        }
        return updatedJudicialResults;
    }

    private Offence getFilteredOffenceForNoneMatch(final List<Offence> filteredOffenceList, final Offence offence) {
        final Optional<Offence> optionalFilteredOffence = filteredOffenceList.stream().filter(Objects::nonNull)
                .filter(off -> !(nonNull(offence)&& isNotEmpty(offence.getJudicialResults()) && nonNull(off.getJudicialResults()) && off.getJudicialResults().stream().filter(Objects::nonNull)
                        .filter(notInterimAndNotWithdrawnResultPredicate).collect(toList()).isEmpty())).findFirst();
        return optionalFilteredOffence.isPresent() ? optionalFilteredOffence.get() : offence;
    }

    private List<Offence> addDefendantJudicialResultsAndOffenceJudicialResults(final List<JudicialResult> defendantJudicialResults, final List<DefendantJudicialResult> hearingLevelResults, final List<Offence> filteredOffenceList, final List<Offence> originalOffences) {
        final Optional<Offence> optionalOffence = filteredOffenceList.stream().filter(Objects::nonNull)
                .filter(offence -> !(offence.getJudicialResults().stream().filter(Objects::nonNull)
                        .filter(interimOrWithdrawnResultPredicate).collect(toList()).isEmpty())).findFirst();

        final Offence firstOffence = optionalOffence.isPresent() ? optionalOffence.get() : null;

        List<JudicialResult> updatedJudicialResults = null;
        if(firstOffence == null){
            final Optional<Offence> originalOptionalOffence =   originalOffences.stream().findFirst();
            final Offence originalFirstOffence = originalOptionalOffence.isPresent() ? originalOptionalOffence.get() : null;
            updatedJudicialResults = nonNull(originalFirstOffence) ? originalFirstOffence.getJudicialResults() : emptyList();
        }

        updatedJudicialResults = appendDefendantJudicialResultsToOffence(defendantJudicialResults,hearingLevelResults, updatedJudicialResults, firstOffence);

        return isNotEmpty(originalOffences) ? updatedOffencesWithJudicialResultsForAllMatch(originalOffences, updatedJudicialResults) : originalOffences;

    }

    private List<Offence> updatedOffencesWithJudicialResultsForNoneMatch(final List<Offence> originalOffences, List<JudicialResult> updatedJudicialResults) {
        final List<Offence> updatedOffenceList = new ArrayList<>(originalOffences);
        final Optional<Offence> optionalOffence = updatedOffenceList.stream()
                .filter(Objects::nonNull).filter(offence -> !(nonNull(offence)&& isNotEmpty(offence.getJudicialResults()) && offence.getJudicialResults()
                        .stream()
                        .filter(Objects::nonNull)
                        .filter(notInterimOrNotWithdrawnResultPredicate)
                        .collect(toList()).isEmpty())).findFirst();
       final Offence offence =   optionalOffence.isPresent() ?optionalOffence.get() : null;

        final  Optional<Offence>  optionalFilteredOffence =  updatedOffenceList.stream().filter(Objects::nonNull)
                .filter(offence2 -> !(nonNull(offence2)&& isNotEmpty(offence2.getJudicialResults()) &&offence2.getJudicialResults().stream()
                        .filter(Objects::nonNull).filter(notInterimAndNotWithdrawnResultPredicate).collect(toList()).isEmpty())).findFirst();
        final  Offence filteredOffence =optionalFilteredOffence.isPresent() ? optionalFilteredOffence.get() : offence;

        if (nonNull(filteredOffence) && isNotEmpty(updatedJudicialResults)) {
            filteredOffence.setJudicialResults(updatedJudicialResults);
        }
        return updatedOffenceList;
    }

    private List<Offence> updatedOffencesWithJudicialResultsForAllMatch(final List<Offence> originalOffences, List<JudicialResult> updatedJudicialResults) {
        final List<Offence> updatedOffenceList = new ArrayList<>(originalOffences);
        final Optional<Offence> optionalOffence = updatedOffenceList.stream().filter(Objects::nonNull).filter(offence ->offence
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
