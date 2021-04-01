package uk.gov.moj.cpp.results.event.helper;

import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;
import static uk.gov.justice.core.courts.CaseDetails.caseDetails;
import static uk.gov.moj.cpp.results.event.helper.results.CommonMethods.getUrn;

import uk.gov.justice.core.courts.CaseDetails;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.CourtOrder;
import uk.gov.justice.core.courts.CourtOrderOffence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.domains.results.shareresults.PublicHearingResulted;
import uk.gov.moj.cpp.results.event.helper.results.CaseDefendantListBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;

public class CasesConverter implements Converter<PublicHearingResulted, List<CaseDetails>> {

    private final ReferenceCache referenceCache;

    @Inject
    public CasesConverter(final ReferenceCache referenceCache) {
        this.referenceCache = referenceCache;
    }

    @Override
    public List<CaseDetails> convert(final PublicHearingResulted source) {

        final Stream<ProsecutionCase> prosecutionCaseStream = ofNullable(source.getHearing().getProsecutionCases()).map(Collection::stream).orElseGet(Stream::empty);

        final List<CaseDetails> caseDetailsList = new ArrayList<>();

        final List<CaseDetails> prosecutionCaseCaseDetails = prosecutionCaseStream.map(prosecutionCase ->
                caseDetails()
                        .withCaseId(prosecutionCase.getId())
                        .withUrn(getUrn(prosecutionCase.getProsecutionCaseIdentifier()))
                        .withDefendants(new CaseDefendantListBuilder(referenceCache).buildDefendantList(prosecutionCase.getDefendants(), source.getHearing()))
                        .withProsecutionAuthorityCode(prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityCode())
                        .withOriginatingOrganisation(prosecutionCase.getOriginatingOrganisation())
                        .build()).collect(Collectors.toList());

        ofNullable(prosecutionCaseCaseDetails).filter(CollectionUtils::isNotEmpty).ifPresent(caseDetailsList::addAll);

        final Supplier<Stream<CourtApplication>> streamSupplier = () -> ofNullable(source.getHearing().getCourtApplications()).map(Collection::stream).orElseGet(Stream::empty);

        final List<CaseDetails> applicationCaseDetails = streamSupplier.get().map(courtApplication ->
                ofNullable(courtApplication.getCourtApplicationCases()).map(Collection::stream).orElseGet(Stream::empty)
                        .filter(this::doesApplicationCaseHasJudicialResults).map(buildCaseDetails(source, courtApplication)
                ).collect(Collectors.toList())
        ).flatMap(List::stream).collect(Collectors.toList());

        ofNullable(applicationCaseDetails).filter(CollectionUtils::isNotEmpty).ifPresent(caseDetailsList::addAll);

        final List<CaseDetails> applicationCaseDetailsFromCourtOrder = streamSupplier.get().filter(courtApplication -> !isNull(courtApplication.getCourtOrder())).map(courtApplication -> {
            final CourtOrder courtOrder = courtApplication.getCourtOrder();
            return courtOrder.getCourtOrderOffences().stream().map(buildCaseDetailsFromCourtOrder(source, courtApplication)).collect(Collectors.toList());
        }).flatMap(List::stream).collect(Collectors.toList());

        ofNullable(applicationCaseDetailsFromCourtOrder).filter(CollectionUtils::isNotEmpty).ifPresent(caseDetailsList::addAll);

        return caseDetailsList;

    }

    private Function<CourtApplicationCase, CaseDetails> buildCaseDetails(PublicHearingResulted source, CourtApplication courtApplication) {
        return courtApplicationCase ->
                caseDetails()
                        .withCaseId(courtApplicationCase.getProsecutionCaseId())
                        .withUrn(defaultIfEmpty(courtApplicationCase.getProsecutionCaseIdentifier().getCaseURN(),
                                courtApplicationCase.getProsecutionCaseIdentifier().getProsecutionAuthorityReference()))
                        .withProsecutionAuthorityCode(courtApplicationCase.getProsecutionCaseIdentifier().getProsecutionAuthorityCode())
                        .withDefendants(new CaseDefendantListBuilder(referenceCache).buildDefendantList(courtApplicationCase, courtApplication, source.getHearing()))
                        .build();
    }

    private Function<CourtOrderOffence, CaseDetails> buildCaseDetailsFromCourtOrder(PublicHearingResulted source, CourtApplication courtApplication) {
        return courtOrderOffence ->
                caseDetails()
                        .withCaseId(courtOrderOffence.getProsecutionCaseId())
                        .withUrn(defaultIfEmpty(courtOrderOffence.getProsecutionCaseIdentifier().getCaseURN(),
                                courtOrderOffence.getProsecutionCaseIdentifier().getProsecutionAuthorityReference()))
                        .withProsecutionAuthorityCode(courtOrderOffence.getProsecutionCaseIdentifier().getProsecutionAuthorityCode())
                        .withDefendants(new CaseDefendantListBuilder(referenceCache).buildDefendantList(courtOrderOffence, courtApplication, source.getHearing()))
                        .build();
    }

    private boolean doesApplicationCaseHasJudicialResults(final CourtApplicationCase courtApplicationCase) {
        return ofNullable(courtApplicationCase.getOffences()).map(Collection::stream).orElseGet(Stream::empty)
                .anyMatch(courtApplicationOffence -> isNotEmpty(courtApplicationOffence.getJudicialResults()));
    }

}
