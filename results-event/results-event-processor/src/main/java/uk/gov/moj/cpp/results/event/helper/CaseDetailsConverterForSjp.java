package uk.gov.moj.cpp.results.event.helper;

import uk.gov.justice.core.courts.CaseDetails;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.justice.sjp.results.PublicSjpResulted;
import uk.gov.moj.cpp.results.event.helper.sjp.CaseDefendantListBuilderSjp;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class CaseDetailsConverterForSjp implements Converter<PublicSjpResulted, List<CaseDetails>> {

    private final ReferenceCache referenceCache;

    @Inject
    public CaseDetailsConverterForSjp(final ReferenceCache referenceCache) {
        this.referenceCache = referenceCache;
    }

    @Override
    public List<CaseDetails> convert(final PublicSjpResulted source) {

        final List<CaseDetails> caseDetailsList = new ArrayList<>();

        final List<uk.gov.justice.sjp.results.CaseDetails> caseSjpDetails = source.getCases();

        for (final uk.gov.justice.sjp.results.CaseDetails caseSjpDetail : caseSjpDetails) {
            caseDetailsList.add(CaseDetails.caseDetails().withCaseId(caseSjpDetail.getCaseId())
                    .withUrn(caseSjpDetail.getUrn())
                    .withDefendants(new CaseDefendantListBuilderSjp(referenceCache).buildDefendantList(caseSjpDetail, source.getSession().getDateAndTimeOfSession(), source.getSession().getSessionId()))
                    .withProsecutionAuthorityCode(caseSjpDetail.getProsecutionAuthorityCode())
                    .withOriginatingOrganisation(caseSjpDetail.getOriginatingOrganisation())
                    .build());
        }
        return caseDetailsList;
    }


}