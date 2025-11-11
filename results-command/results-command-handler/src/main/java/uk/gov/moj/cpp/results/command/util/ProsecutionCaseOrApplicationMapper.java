package uk.gov.moj.cpp.results.command.util;

import static java.util.Arrays.asList;
import static java.util.Objects.nonNull;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.informantRegisterDocument.InformantRegisterCaseOrApplication;
import uk.gov.justice.core.courts.informantRegisterDocument.InformantRegisterDefendant;
import uk.gov.justice.core.courts.informantRegisterDocument.InformantRegisterOffence;
import uk.gov.justice.core.courts.informantRegisterDocument.InformantRegisterResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProsecutionCaseOrApplicationMapper {

    private ProsecutionCaseOrApplicationMapper() {
    }

    public static List<InformantRegisterCaseOrApplication> getProsecutionCasesOrApplication(final InformantRegisterDefendant masterDefendant,
                                                                                            final ProsecutionCase prosecutionCase,
                                                                                            final Defendant defendant) {
        return asList(InformantRegisterCaseOrApplication.informantRegisterCaseOrApplication()
                .withCaseOrApplicationReference(getCaseOrApplicationReference(prosecutionCase))
                .withResults(getResults(masterDefendant.getProsecutionCasesOrApplications()))
                .withOffences(getOffences(masterDefendant.getProsecutionCasesOrApplications()))
                .withArrestSummonsNumber(getArrestSummonsNumber(masterDefendant, defendant))
                .build());
    }

    private static String getCaseOrApplicationReference(final ProsecutionCase prosecutionCase) {
        if (nonNull(prosecutionCase.getProsecutionCaseIdentifier())) {
            if (isNotEmpty(prosecutionCase.getProsecutionCaseIdentifier().getCaseURN())) {
                return prosecutionCase.getProsecutionCaseIdentifier().getCaseURN();
            } else if (isNotEmpty(prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityReference())) {
                return prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityReference();
            }
        }
        return null;
    }

    private static List<InformantRegisterResult> getResults(final List<InformantRegisterCaseOrApplication> informantRegisterCaseOrApplications) {
        final List<InformantRegisterResult> returnList = new ArrayList<>();
        informantRegisterCaseOrApplications.stream().forEach(caseOrApplication -> {
            if (isNotEmpty(caseOrApplication.getResults())) {
                returnList.addAll(caseOrApplication.getResults());
            }
        });

        return isNotEmpty(returnList) ? returnList : null;
    }

    private static List<InformantRegisterOffence> getOffences(final List<InformantRegisterCaseOrApplication> informantRegisterCaseOrApplications) {
        final List<InformantRegisterOffence> returnList = new ArrayList<>();
        informantRegisterCaseOrApplications.stream().forEach(caseOrApplication -> {
            if (isNotEmpty(caseOrApplication.getOffences())) {
                returnList.addAll(caseOrApplication.getOffences());
            }
        });

        return isNotEmpty(returnList) ? returnList : null;
    }

    private static String getArrestSummonsNumber(final InformantRegisterDefendant masterDefendant, final Defendant defendant) {
        if (nonNull(defendant.getPersonDefendant())
                && isNotEmpty(defendant.getPersonDefendant().getArrestSummonsNumber())) {
            return defendant.getPersonDefendant().getArrestSummonsNumber();
        } else {
            final Optional<InformantRegisterCaseOrApplication> caseOrApplication =
                    masterDefendant.getProsecutionCasesOrApplications().stream().filter(o -> isNotEmpty(o.getArrestSummonsNumber())).findFirst();
            if (caseOrApplication.isPresent()) {
                return caseOrApplication.get().getArrestSummonsNumber();
            } else {
                return null;
            }
        }
    }
}