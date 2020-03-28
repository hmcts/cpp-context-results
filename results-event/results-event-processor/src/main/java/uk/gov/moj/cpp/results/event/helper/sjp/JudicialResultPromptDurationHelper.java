package uk.gov.moj.cpp.results.event.helper.sjp;


import static java.lang.Integer.parseInt;
import static java.lang.String.valueOf;
import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.upperCase;
import static uk.gov.justice.core.courts.JudicialResultPromptDurationElement.judicialResultPromptDurationElement;
import static uk.gov.moj.cpp.results.event.helper.sjp.DurationDateHelper.populateStartAndEndDates;

import uk.gov.justice.core.courts.JudicialResultPromptDurationElement;
import uk.gov.moj.cpp.results.event.helper.resultdefinition.ResultDefinition;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.tuple.Pair;

public class JudicialResultPromptDurationHelper {

    private static final int PRIMARY_DURATION_TYPE = 1;
    private static final int SECONDARY_DURATION_TYPE = 2;
    private static final String DURATION_UNIT = "L";
    private static final Integer DURATION_VALUE = 1;
    private static final int MAX_VALUE = 999;


    private BiFunction<List<uk.gov.justice.core.courts.JudicialResultPrompt>, Integer, Optional<Pair<String, Integer>>> getValue = (judicialResultPrompts, durationType) -> {
        final Optional<uk.gov.justice.core.courts.JudicialResultPrompt> primaryJudicialResultPromptOptional = getJudicialResultPromptDurationElement(judicialResultPrompts, durationType);
        if (primaryJudicialResultPromptOptional.isPresent()) {
            return splitDuration(primaryJudicialResultPromptOptional);
        }
        return empty();
    };


    private Optional<uk.gov.justice.core.courts.JudicialResultPrompt> getJudicialResultPromptDurationElement(final List<uk.gov.justice.core.courts.JudicialResultPrompt> judicialResultPrompts, final Integer durationType) {
        if(nonNull(judicialResultPrompts) && isNotEmpty(judicialResultPrompts)){
            return judicialResultPrompts.stream()
                    .filter(Objects::nonNull)
                    .filter(p -> nonNull(p.getDurationSequence()) && p.getDurationSequence().equals(durationType))
                    .findFirst();
        }
        return empty();
    }


    public Optional<JudicialResultPromptDurationElement> populate(final List<uk.gov.justice.core.courts.JudicialResultPrompt> judicialResultPrompts, final ZonedDateTime dateAndTimeOfSession, final ResultDefinition resultDefinition) {

        final Optional<JudicialResultPromptDurationElement> primaryJudicialPrompt = populatePrimaryDuration(judicialResultPrompts, dateAndTimeOfSession, resultDefinition);

        if (primaryJudicialPrompt.isPresent()) {
            return primaryJudicialPrompt;
        }

        final Optional<JudicialResultPromptDurationElement> secondaryJudicialPrompt = populateSecondaryDuration(judicialResultPrompts);
        if (secondaryJudicialPrompt.isPresent()) {
            return secondaryJudicialPrompt;
        }

        return Optional.empty();
    }

    private Optional<JudicialResultPromptDurationElement> populatePrimaryDuration(final List<uk.gov.justice.core.courts.JudicialResultPrompt> judicialResultPrompts, final ZonedDateTime dateAndTimeOfSession, final uk.gov.moj.cpp.results.event.helper.resultdefinition.ResultDefinition resultDefinition) {

        if (BooleanUtils.isTrue(resultDefinition.getLifeDuration())) {
            return of(judicialResultPromptDurationElement().withPrimaryDurationUnit(DURATION_UNIT).withPrimaryDurationValue(DURATION_VALUE).build());
        }

        final Optional<Pair<String, Integer>> primaryValue = getValue.apply(judicialResultPrompts, PRIMARY_DURATION_TYPE);
        if (primaryValue.isPresent() ) {
            final Pair<String, Integer> resultPair = primaryValue.get();
            final String unit = resultPair.getKey();
            final int value = resultPair.getValue();
            final JudicialResultPromptDurationElement.Builder builder = judicialResultPromptDurationElement();
            if (value <= MAX_VALUE) {
                builder.withPrimaryDurationValue(value).withPrimaryDurationUnit(unit);
            }else {
                populateStartAndEndDates(builder, dateAndTimeOfSession, primaryValue.get());
            }
            return of(builder.build());

        }
        return Optional.empty();
    }

    private Optional<JudicialResultPromptDurationElement> populateSecondaryDuration(final List<uk.gov.justice.core.courts.JudicialResultPrompt> judicialResultPrompts) {

        final Optional<Pair<String, Integer>> secondaryValue = getValue.apply(judicialResultPrompts, SECONDARY_DURATION_TYPE);

        if (secondaryValue.isPresent()) {
            final Pair<String, Integer> resultPair = secondaryValue.get();
            final String unit = resultPair.getKey();
            final int value = resultPair.getValue();
            return of(judicialResultPromptDurationElement().withSecondaryDurationValue(value).withSecondaryDurationUnit(unit).build());
        }
        return empty();
    }



    private Optional<Pair<String, Integer>> splitDuration(final Optional<uk.gov.justice.core.courts.JudicialResultPrompt> judicialResultPromptOptional) {
        final String[] splitDuration = judicialResultPromptOptional.map(uk.gov.justice.core.courts.JudicialResultPrompt::getValue).map(s -> s.split("\\s+")).orElse(null);
        if (splitDuration != null && splitDuration.length == 2) {
            final int value = parseInt(splitDuration[0]);
            final String unit = upperCase(valueOf(splitDuration[1].charAt(0)));
            return of(Pair.of(unit, value));
        }
        return empty();
    }


}
