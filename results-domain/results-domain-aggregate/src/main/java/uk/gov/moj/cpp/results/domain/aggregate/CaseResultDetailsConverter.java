package uk.gov.moj.cpp.results.domain.aggregate;

import uk.gov.moj.cpp.domains.resultdetails.JudicialResultAmendmentType;
import uk.gov.moj.cpp.results.domain.event.ApplicationResultDetails;
import uk.gov.moj.cpp.results.domain.event.AmendmentType;
import uk.gov.moj.cpp.results.domain.event.OffenceResultDetails;
import uk.gov.moj.cpp.results.domain.event.CaseResultDetails;
import uk.gov.moj.cpp.results.domain.event.DefendantResultDetails;
import uk.gov.moj.cpp.results.domain.event.JudicialResultDetails;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

public class CaseResultDetailsConverter {
    private CaseResultDetailsConverter() {
    }

    public static CaseResultDetails convert(uk.gov.moj.cpp.domains.resultdetails.CaseResultDetails caseResultDetails) {
        if (isNull(caseResultDetails)) {
            return null;
        }

        final List<DefendantResultDetails> defendantResultDetails = caseResultDetails.getDefendantResultDetails().stream()
                .map(CaseResultDetailsConverter::convertDefendantResultDetails)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        final List<ApplicationResultDetails> applicationResultDetails = caseResultDetails.getApplicationResultDetails().stream()
                .map(CaseResultDetailsConverter::convertApplicationResultDetails)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        final CaseResultDetails.Builder caseResultDetailsBuilder = new CaseResultDetails.Builder();

        caseResultDetailsBuilder
                .withCaseId(caseResultDetails.getCaseId())
                .withDefendantResultDetails(defendantResultDetails);

        if (isNotEmpty(applicationResultDetails)) {
            caseResultDetailsBuilder.withApplicationResultDetails(applicationResultDetails);
        }

        return caseResultDetailsBuilder.build();
    }

    private static DefendantResultDetails convertDefendantResultDetails(uk.gov.moj.cpp.domains.resultdetails.DefendantResultDetails defendantResultDetails) {
        final List<OffenceResultDetails> offenceDetailsList = defendantResultDetails.getOffences().stream()
                .map(CaseResultDetailsConverter::convertOffenceResultDetails)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (offenceDetailsList.isEmpty()) {
            return null;
        }

        return DefendantResultDetails.defendantResultDetails()
                .withId(defendantResultDetails.getDefendantId())
                .withDefendantName(defendantResultDetails.getDefendantName())
                .withOffenceResultDetails(offenceDetailsList)
                .build();
    }

    private static OffenceResultDetails convertOffenceResultDetails(uk.gov.moj.cpp.domains.resultdetails.OffenceResultDetails offenceResultDetails) {
        final List<JudicialResultDetails> judicialResultDetailsList = offenceResultDetails.getResults().stream()
                .filter(judicialResultDetails -> judicialResultDetails.getAmendmentType() != JudicialResultAmendmentType.NONE)
                .map(CaseResultDetailsConverter::convertResultDetails)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (judicialResultDetailsList.isEmpty()) {
            return null;
        }

        return OffenceResultDetails.offenceResultDetails()
                .withId(offenceResultDetails.getOffenceId())
                .withOffenceNo(offenceResultDetails.getOffenceNo())
                .withOffenceCount(offenceResultDetails.getOffenceCount())
                .withOffenceTitle(offenceResultDetails.getOffenceTitle())
                .withJudicialResultDetails(judicialResultDetailsList)
                .build();
    }

    private static ApplicationResultDetails convertApplicationResultDetails(uk.gov.moj.cpp.domains.resultdetails.ApplicationResultDetails applicationResultDetails) {
        final List<JudicialResultDetails> judicialResultDetailsList = applicationResultDetails.getResults().stream()
                .filter(judicialResultDetails -> judicialResultDetails.getAmendmentType() != JudicialResultAmendmentType.NONE)
                .map(CaseResultDetailsConverter::convertResultDetails)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        final List<OffenceResultDetails> offenceResultDetails = applicationResultDetails.getCourtOrderOffenceResultDetails().stream()
                .map(CaseResultDetailsConverter::convertOffenceResultDetails)
                .collect(Collectors.toList());

        if (judicialResultDetailsList.isEmpty() && offenceResultDetails.isEmpty()) {
            return null;
        }

        return ApplicationResultDetails.applicationResultDetails()
                .withId(applicationResultDetails.getApplicationId())
                .withApplicationTitle(applicationResultDetails.getApplicationTitle())
                .withJudicialResultDetails(judicialResultDetailsList)
                .withOffenceResultDetails(offenceResultDetails)
                .withApplicationSubjectFirstName(applicationResultDetails.getApplicationSubjectFirstName())
                .withApplicationSubjectLastName(applicationResultDetails.getApplicationSubjectLastName())
                .build();
    }

    private static JudicialResultDetails convertResultDetails(uk.gov.moj.cpp.domains.resultdetails.JudicialResultDetails resultDetails) {
        return JudicialResultDetails.judicialResultDetails()
                .withId(resultDetails.getResultId())
                .withResultTitle(resultDetails.getTitle())
                .withAmendmentType(AmendmentType.valueFor(resultDetails.getAmendmentType().name()).orElse(null))
                .withJudicialResultTypeId(resultDetails.getResultTypeId())
                .build();
    }
}
