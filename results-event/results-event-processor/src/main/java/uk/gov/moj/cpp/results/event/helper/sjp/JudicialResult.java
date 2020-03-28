package uk.gov.moj.cpp.results.event.helper.sjp;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;

import uk.gov.justice.core.courts.Category;
import uk.gov.justice.core.courts.JudicialResultPromptDurationElement;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.sjp.results.BaseResult;
import uk.gov.moj.cpp.results.event.helper.ReferenceCache;
import uk.gov.moj.cpp.results.event.helper.resultdefinition.ResultDefinition;
import uk.gov.moj.cpp.results.event.helper.resultdefinition.ResultDefinitionNotFoundException;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;

public class JudicialResult {

    ReferenceCache referenceCache;

    @Inject
    public JudicialResult(final ReferenceCache referenceCache) {
        this.referenceCache = referenceCache;
    }

    @SuppressWarnings("squid:S1188")
    public List<uk.gov.justice.core.courts.JudicialResult> buildJudicialResults(final List<BaseResult> baseResults, final ZonedDateTime dateAndTimeOfSession, final UUID sessionId) {

        final JsonEnvelope context = envelopeFrom(metadataBuilder().withId(randomUUID()).withName("public.sjp.case-resulted").build(), createObjectBuilder().build());

        return baseResults.stream()
                .map(baseResult -> {
                            final ResultDefinition resultDefinition = this.referenceCache.getResultDefinitionById(context, dateAndTimeOfSession.toLocalDate(), baseResult.getId());
                            if (resultDefinition == null) {
                                throw new ResultDefinitionNotFoundException(format("resultDefinition not found for resultDefinitionId: %s, orderedDate: %s", baseResult.getId(), dateAndTimeOfSession.toLocalDate()));
                            }
                            final List<uk.gov.justice.core.courts.JudicialResultPrompt> judicialResultPrompts = new JudicialResultPrompt().buildJudicialResultPrompt(resultDefinition, baseResult.getPrompts());

                            Optional<JudicialResultPromptDurationElement> judicialResultPromptDurationElement = empty();
                            judicialResultPromptDurationElement = new JudicialResultPromptDurationHelper().populate(judicialResultPrompts, dateAndTimeOfSession, resultDefinition);

                            final uk.gov.justice.core.courts.JudicialResult.Builder builder = uk.gov.justice.core.courts.JudicialResult.judicialResult();

                            builder
                                    .withJudicialResultId(baseResult.getId())
                                    .withJudicialResultTypeId(resultDefinition.getId())
                                    .withCategory(getCategory(resultDefinition))
                                    .withCjsCode(resultDefinition.getCjsCode())
                                    .withIsAdjournmentResult(resultDefinition.isAdjournment())
                                    .withIsAvailableForCourtExtract(resultDefinition.getIsAvailableForCourtExtract())
                                    .withIsConvictedResult(resultDefinition.isConvicted())
                                    .withDurationElement(judicialResultPromptDurationElement.isPresent() ? judicialResultPromptDurationElement.get() : null)
                                    .withIsFinancialResult(resultDefinition.isFinancial())
                                    .withLabel(resultDefinition.getLabel())
                                    .withOrderedHearingId(sessionId)
                                    .withOrderedDate(dateAndTimeOfSession.toLocalDate())
                                    .withRank(isNull(resultDefinition.getRank()) ? BigDecimal.ZERO : new BigDecimal(resultDefinition.getRank()))
                                    .withUsergroups(resultDefinition.getUserGroups())
                                    .withWelshLabel(resultDefinition.getWelshLabel())
                                    .withResultText(new ResultTextHelper().getResultText(resultDefinition, judicialResultPrompts))
                                    .withTerminatesOffenceProceedings(resultDefinition.getTerminatesOffenceProceedings())
                                    .withLifeDuration(resultDefinition.getLifeDuration())
                                    .withPublishedAsAPrompt(resultDefinition.getPublishedAsAPrompt())
                                    .withExcludedFromResults(resultDefinition.getExcludedFromResults())
                                    .withAlwaysPublished(resultDefinition.getAlwaysPublished())
                                    .withUrgent(resultDefinition.getUrgent())
                                    .withD20(resultDefinition.getD20());

                            if (isNotEmpty(judicialResultPrompts)) {
                                builder.withJudicialResultPrompts(judicialResultPrompts);
                            }

                            return builder.build(); 
                        }
                ).collect(Collectors.toList());
    }


    private Category getCategory(final ResultDefinition resultDefinition) {
        Category category = null;

        if (nonNull(resultDefinition) && nonNull(resultDefinition.getCategory())) {

            switch (resultDefinition.getCategory()) {
                case "A":
                    category = Category.ANCILLARY;
                    break;
                case "F":
                    category = Category.FINAL;
                    break;
                case "I":
                    category = Category.INTERMEDIARY;
                    break;
                default:
                    category = null;
            }
        }

        return category;
    }
}
