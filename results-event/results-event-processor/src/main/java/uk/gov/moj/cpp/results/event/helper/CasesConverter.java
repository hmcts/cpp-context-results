package uk.gov.moj.cpp.results.event.helper;

import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.core.courts.CaseDetails.caseDetails;
import static uk.gov.moj.cpp.results.event.helper.results.CommonMethods.checkURNValidity;
import static uk.gov.moj.cpp.results.event.helper.results.CommonMethods.getUrn;

import org.apache.commons.collections.CollectionUtils;
import uk.gov.justice.core.courts.CaseDefendant;
import uk.gov.justice.core.courts.CaseDetails;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.CourtOrder;
import uk.gov.justice.core.courts.CourtOrderOffence;
import uk.gov.justice.core.courts.DefendantCase;
import uk.gov.justice.core.courts.OffenceDetails;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.domains.results.shareresults.PublicHearingResulted;
import uk.gov.moj.cpp.results.event.helper.results.CaseDefendantListBuilder;

import uk.gov.moj.cpp.results.event.service.ReferenceDataService;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class CasesConverter implements Converter<PublicHearingResulted, List<CaseDetails>> {

    private final ReferenceCache referenceCache;
    private final ReferenceDataService referenceDataService;

    @Inject
    public CasesConverter(final ReferenceCache referenceCache, final ReferenceDataService referenceDataService) {
        this.referenceCache = referenceCache;
        this.referenceDataService = referenceDataService;
    }

    @Override
    public List<CaseDetails> convert(final PublicHearingResulted source) {
        final Stream<ProsecutionCase> prosecutionCaseStream = ofNullable(source.getHearing().getProsecutionCases()).map(Collection::stream).orElseGet(Stream::empty);
        final List<CaseDetails> caseDetailsList = new ArrayList<>();

        final List<CaseDetails> prosecutionCaseCaseDetails = prosecutionCaseStream.map(prosecutionCase ->
        {
            final String originatingOrganisation = prosecutionCase.getOriginatingOrganisation();
            final String prosecutionAuthorityCode = prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityCode();
            final boolean isPoliceProsecutor = referenceDataService.getPoliceFlag(originatingOrganisation, prosecutionAuthorityCode);
            final boolean isURNValid = checkURNValidity(prosecutionCase.getProsecutionCaseIdentifier().getCaseURN());

            return caseDetails()
                    .withCaseId(prosecutionCase.getId())
                    .withUrn(getUrn(prosecutionCase.getProsecutionCaseIdentifier(), isPoliceProsecutor, isURNValid))
                    .withDefendants(new CaseDefendantListBuilder(referenceCache).buildDefendantList(prosecutionCase.getDefendants(), source.getHearing(), isPoliceProsecutor))
                    .withProsecutionAuthorityCode(prosecutionAuthorityCode)
                    .withOriginatingOrganisation(originatingOrganisation)
                    .build();
        }).collect(toList());

        ofNullable(prosecutionCaseCaseDetails).filter(CollectionUtils::isNotEmpty).ifPresent(caseDetailsList::addAll);

        final Supplier<Stream<CourtApplication>> streamSupplier = () -> ofNullable(source.getHearing().getCourtApplications()).map(Collection::stream).orElseGet(Stream::empty);

        final List<CaseDetails> applicationCaseDetails = streamSupplier.get().filter(this::doesApplicationOrApplicationCaseHasJudicialResults)
                .map(courtApplication ->
                        ofNullable(courtApplication.getCourtApplicationCases()).map(Collection::stream).orElseGet(Stream::empty)
                                .map(courtApplicationCase -> {
                                            final String prosecutionAuthorityCode = courtApplicationCase.getProsecutionCaseIdentifier().getProsecutionAuthorityCode();
                                            final boolean isPoliceProsecutor = referenceDataService.getPoliceFlag(null, prosecutionAuthorityCode);
                                            final boolean isURNValid = checkURNValidity(courtApplicationCase.getProsecutionCaseIdentifier().getCaseURN());
                                            return buildCaseDetails(source, courtApplication, isPoliceProsecutor, isURNValid).apply(courtApplicationCase);
                                        }
                                ).collect(toList())
                ).flatMap(List::stream).collect(toList());

        ofNullable(applicationCaseDetails).filter(CollectionUtils::isNotEmpty).ifPresent(caseDetailsList::addAll);

        final List<CaseDetails> applicationCaseDetailsFromCourtOrder = streamSupplier.get().filter(courtApplication -> !isNull(courtApplication.getCourtOrder())).map(courtApplication -> {
            final CourtOrder courtOrder = courtApplication.getCourtOrder();
            return courtOrder.getCourtOrderOffences().stream().filter(this::hasJudicialResultsForCourtOrderOffence)
                    .map(courtOrderOffence -> {
                                final String prosecutionAuthorityCode = courtOrderOffence.getProsecutionCaseIdentifier().getProsecutionAuthorityCode();
                                final boolean isPoliceProsecutor = referenceDataService.getPoliceFlag(null, prosecutionAuthorityCode);
                                final boolean isURNValid = checkURNValidity(courtOrderOffence.getProsecutionCaseIdentifier().getCaseURN());
                                return buildCaseDetailsFromCourtOrder(source, courtApplication, isPoliceProsecutor, isURNValid).apply(courtOrderOffence);
                            }
                    ).collect(toList());
        }).flatMap(List::stream).collect(toList());

        ofNullable(applicationCaseDetailsFromCourtOrder).filter(CollectionUtils::isNotEmpty).ifPresent(caseDetailsList::addAll);

        return this.mergeCaseDetailsForMatchingCaseAndDefendants(caseDetailsList);

    }

    private  boolean hasJudicialResultsForCourtOrderOffence(final CourtOrderOffence co) {
        return (null != co.getOffence().getJudicialResults()) && (!co.getOffence().getJudicialResults().isEmpty());
    }

    @SuppressWarnings({"squid:S4034"})
    private List<CaseDetails> mergeCaseDetailsForMatchingCaseAndDefendants(final List<CaseDetails> originalCaseDetails) {
        final List<CaseDetails> mergedCaseDetails = new ArrayList<>();
        originalCaseDetails.forEach(caseDetails -> {
            final Optional<CaseDetails> matchedCaseDetail = mergedCaseDetails.stream().filter(caseDetails1 -> caseDetails1.getCaseId().equals(caseDetails.getCaseId())).findFirst();
            if (matchedCaseDetail.isPresent()) {
                caseDetails.getDefendants().forEach(caseDefendant -> {
                    final Stream<CaseDefendant> mergedCaseDefendantStream = matchedCaseDetail.get().getDefendants().stream();
                    final Optional<CaseDefendant> matchedDefendant = mergedCaseDefendantStream.filter(caseDefendant1 -> caseDefendant1.getDefendantId().equals(caseDefendant.getDefendantId())).findFirst();
                    if (matchedDefendant.isPresent()) {
                        final List<OffenceDetails> uniqueOffences = ofNullable(caseDefendant.getOffences()).orElse(new ArrayList<>()).stream().filter(od -> !matchedDefendant.get().getOffences().stream().anyMatch(existing -> existing.getId().equals(od.getId()))).collect(Collectors.toList());
                        matchedDefendant.get().getOffences().addAll(uniqueOffences);
                    } else {
                        matchedCaseDetail.get().getDefendants().add(caseDefendant);
                    }
                });
            } else {
                mergedCaseDetails.add(caseDetails);
            }
        });
        return mergedCaseDetails;
    }

    private Function<CourtApplicationCase, CaseDetails> buildCaseDetails(PublicHearingResulted source, CourtApplication courtApplication, final boolean isPoliceProsecutor, final boolean isURNValid) {
        return courtApplicationCase ->
                caseDetails()
                        .withCaseId(courtApplicationCase.getProsecutionCaseId())
                        .withUrn(getUrn(courtApplicationCase.getProsecutionCaseIdentifier(), isPoliceProsecutor, isURNValid))
                        .withProsecutionAuthorityCode(courtApplicationCase.getProsecutionCaseIdentifier().getProsecutionAuthorityCode())
                        .withDefendants(new CaseDefendantListBuilder(referenceCache).buildDefendantList(courtApplicationCase, courtApplication, source.getHearing(), isPoliceProsecutor))
                        .build();
    }

    @SuppressWarnings({"squid:S3358", "squid:S1125", "squid:S3776"})
    private Function<CourtOrderOffence, CaseDetails> buildCaseDetailsFromCourtOrder(PublicHearingResulted source, CourtApplication courtApplication, final boolean isPoliceProsecutor, final boolean isURNValid) {
        return courtOrderOffence -> {
            final DefendantCase defendantCase = courtApplication.getSubject().getMasterDefendant() == null ? null : courtApplication.getSubject().getMasterDefendant().getDefendantCase() == null ? null :
                    courtApplication.getSubject().getMasterDefendant().getDefendantCase().isEmpty() == true ? null : courtApplication.getSubject().getMasterDefendant().getDefendantCase().get(0);
            final UUID caseId = defendantCase == null ? courtOrderOffence.getProsecutionCaseId() : defendantCase.getCaseId();
            final String urn = defendantCase == null ? getUrn(courtOrderOffence.getProsecutionCaseIdentifier(), isPoliceProsecutor, isURNValid) : defendantCase.getCaseReference();
            final String prosecutorAuthorityCode = defendantCase == null ? courtOrderOffence.getProsecutionCaseIdentifier().getProsecutionAuthorityCode() : courtApplication.getApplicant().getProsecutingAuthority() != null ?
                    courtApplication.getApplicant().getProsecutingAuthority().getProsecutionAuthorityCode() : courtOrderOffence.getProsecutionCaseIdentifier().getProsecutionAuthorityCode();

           return caseDetails()
                    .withCaseId(caseId)
                    .withUrn(urn)
                    .withProsecutionAuthorityCode(prosecutorAuthorityCode)
                    .withDefendants(new CaseDefendantListBuilder(referenceCache).buildDefendantList(courtOrderOffence, courtApplication, source.getHearing(), isPoliceProsecutor))
                    .build();
        };
    }

    private boolean doesApplicationOrApplicationCaseHasJudicialResults(final CourtApplication courtApplication) {
        return isNotEmpty(courtApplication.getJudicialResults()) ||
                ofNullable(courtApplication.getCourtApplicationCases()).map(Collection::stream).orElseGet(Stream::empty).
                        map(courtApplicationCase -> Optional.ofNullable(courtApplicationCase.getOffences()))
                        .flatMap(offences -> offences.orElseGet(Collections::emptyList).stream())
                        .anyMatch(courtApplicationOffence -> isNotEmpty(courtApplicationOffence.getJudicialResults()));
    }

}
