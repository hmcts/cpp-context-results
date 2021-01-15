package uk.gov.moj.cpp.results.event.helper;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static uk.gov.justice.core.courts.CaseDetails.caseDetails;
import static uk.gov.moj.cpp.results.event.helper.results.CommonMethods.getUrn;

import uk.gov.justice.core.courts.CaseDetails;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.domains.results.shareresults.PublicHearingResulted;
import uk.gov.moj.cpp.results.event.helper.results.CaseDefendantListBuilder;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

public class CasesConverter implements Converter<PublicHearingResulted, List<CaseDetails>> {

    private final ReferenceCache referenceCache;

    @Inject
    public CasesConverter(final ReferenceCache referenceCache) {
        this.referenceCache = referenceCache;
    }

    @Override
    public List<CaseDetails> convert(final PublicHearingResulted source) {
        return ofNullable(source.getHearing()).map(hearing ->
                ofNullable(hearing.getProsecutionCases()).map(prosecutionCases ->
                        prosecutionCases.stream().map(prosecutionCase ->
                                caseDetails()
                                        .withCaseId(prosecutionCase.getId())
                                        .withUrn(getUrn(prosecutionCase.getProsecutionCaseIdentifier()))
                                        .withDefendants(new CaseDefendantListBuilder(referenceCache).buildDefendantList(prosecutionCase.getDefendants(), hearing))
                                        .withProsecutionAuthorityCode(prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityCode())
                                        .withOriginatingOrganisation(prosecutionCase.getOriginatingOrganisation())
                                        .build()
                        ).collect(Collectors.toList())
                ).orElse(emptyList())).orElse(emptyList());
    }
}
