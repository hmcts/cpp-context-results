package uk.gov.moj.cpp.results.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.results.persist.HearingRepository;
import uk.gov.moj.cpp.results.persist.HearingResultRepository;
import uk.gov.moj.cpp.results.persist.DefendantRepository;
import uk.gov.moj.cpp.results.persist.VariantDirectoryRepository;
import uk.gov.moj.cpp.results.persist.entity.Hearing;
import uk.gov.moj.cpp.results.persist.entity.HearingResult;
import uk.gov.moj.cpp.results.persist.entity.Defendant;
import uk.gov.moj.cpp.results.persist.entity.VariantDirectory;

import javax.inject.Inject;
import javax.transaction.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

@SuppressWarnings({"unchecked","squid:S1612"})
@ServiceComponent(EVENT_LISTENER)
public class ResultsEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResultsEventListener.class);

    private final DefendantRepository defendantRepository;
    private final HearingRepository hearingRepository;
    private final HearingResultRepository hearingResultRepository;
    private final VariantDirectoryRepository variantDirectoryRepository;

    @Inject
    public ResultsEventListener(final DefendantRepository defendantRepository,
                                final HearingRepository hearingRepository,
                                final HearingResultRepository hearingResultRepository,
                                final VariantDirectoryRepository variantDirectoryRepository) {
        this.defendantRepository = defendantRepository;
        this.hearingRepository = hearingRepository;
        this.hearingResultRepository = hearingResultRepository;
        this.variantDirectoryRepository = variantDirectoryRepository;
    }

    @Transactional
    @Handles("results.hearing-results-added")
    public void hearingResultsAdded(final JsonEnvelope event) {
        final Map<String, Object> hearingResultsMap = HearingResultsConverter.withJsonObject(event.payloadAsJsonObject()).convert();
        saveHearingResults(hearingResultsMap);
    }

    private void saveHearingResults(final Map<String, Object> hearingResultsMap) {

        final Set<Hearing> hearings = getHearingFromJson(hearingResultsMap);

        final Set<Defendant> defendants = getDefendantsFromJson(hearingResultsMap);

        final List<HearingResult> hearingResults = getHearingResultFromJson(hearingResultsMap);

        final List<VariantDirectory> variantDirectories = getVariantDirectories(hearingResultsMap);

        saveHearings(hearings);

        savePersons(defendants);

        saveHearingResults(hearingResults);

        final UUID hearingId = (UUID) hearingResultsMap.get(HearingResultsConverter.HEARING_ID);
        deleteVariantDirectories(hearingId);

        if (!variantDirectories.isEmpty()) {
            saveVariantDirectories(variantDirectories);
        }

        deleteHearingResults(hearingId, defendants, hearingResults);

        deleteDefendants(hearingId, defendants);

        LOGGER.info("Saved ({}) new hearings result(s), ({}) person(s) for (1) hearing(s) with id: [{}]",
                hearingResults.size(), defendants.size(), hearingResultsMap.get("hearingId"));
    }

    private Set<Defendant> getDefendantsFromJson(final Map<String, Object> hearingResultsMap) {
        return (Set<Defendant>) hearingResultsMap.get(HearingResultsConverter.DEFENDANTS);
    }

    private Set<Hearing> getHearingFromJson(final Map<String, Object> hearingResultsMap) {
        return (Set<Hearing>) hearingResultsMap.get(HearingResultsConverter.HEARINGS);
    }

    private List<HearingResult> getHearingResultFromJson(final Map<String, Object> hearingResultsMap) {
        return (List<HearingResult>) hearingResultsMap.get(HearingResultsConverter.HEARING_RESULTS);
    }

    private List<VariantDirectory> getVariantDirectories(final Map<String, Object> hearingResultsMap) {
        return (List<VariantDirectory>) hearingResultsMap.get(HearingResultsConverter.VARIANTS);
    }

    private void savePersons(final Set<Defendant> defendants) {
        defendants.forEach(person -> defendantRepository.saveAndFlush(person));
    }

    private void saveHearings(final Set<Hearing> hearings) {
        hearings.forEach(hearing -> hearingRepository.saveAndFlush(hearing));
    }

    private void saveHearingResults(final List<HearingResult> hearingResults) {
        hearingResults.forEach(hearingResult -> hearingResultRepository.saveAndFlush(hearingResult));
    }

    private void saveVariantDirectories(final List<VariantDirectory> variantDirectories) {
        variantDirectories.forEach(variant -> variantDirectoryRepository.saveAndFlush(variant));
    }

    private void deleteVariantDirectories(UUID hearingId) {

        final Collection<VariantDirectory> currentVariantDirectories = variantDirectoryRepository.findByHearingId(hearingId);
        currentVariantDirectories.forEach(variantDirectoryRepository::remove);
    }

    private void deleteDefendants(UUID hearingId, final Set<Defendant> defendants) {

        final List<Defendant> personList = defendantRepository.findByHearingId(hearingId);

        final List<Defendant> personsTobeDeleted = personList.stream()
                .filter(existingPerson -> defendants.stream().noneMatch(person -> person.getId().equals(existingPerson.getId())))
                .collect(Collectors.toList());

        personsTobeDeleted.forEach(person -> {

            final List<HearingResult> hearingResults = hearingResultRepository.findByHearingIdAndPersonId(hearingId, person.getId());

            hearingResults.forEach(hearingResult -> hearingResultRepository.remove(hearingResult));

        });

        personsTobeDeleted.forEach(person -> defendantRepository.remove(person));
    }

    private void deleteHearingResults(UUID hearingId, final Set<Defendant> defendants, final List<HearingResult> hearingResults) {

        defendants.forEach(person -> {

            final List<HearingResult> existingHearingResults = hearingResultRepository.findByHearingIdAndPersonId(hearingId, person.getId());

            final List<HearingResult> hearingResultTobeDeleted = existingHearingResults.stream()
                    .filter(existingHearingResult -> hearingResults.stream().noneMatch(hearingResult -> hearingResult.getId().equals(existingHearingResult.getId())))
                    .collect(Collectors.toList());

            hearingResultTobeDeleted.forEach(hearingResult -> hearingResultRepository.remove(hearingResult));
        });
    }
}
