package uk.gov.moj.cpp.results.command.util;

import static java.util.Objects.nonNull;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static uk.gov.moj.cpp.results.command.util.ProsecutionCaseOrApplicationMapper.getProsecutionCasesOrApplication;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.informantRegisterDocument.InformantRegisterDefendant;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class DefendantMapper {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String LINE1 = "LINE1";
    private static final String LINE2 = "LINE2";
    private static final String LINE3 = "LINE3";
    private static final String LINE4 = "LINE4";
    private static final String LINE5 = "LINE5";
    private static final String POST_CODE = "POST_CODE";

    private DefendantMapper() {
    }

    public static List<InformantRegisterDefendant> getDefendants(final InformantRegisterDefendant masterDefendant,
                                                                 final List<ProsecutionCase> prosecutionCases) {
        final List<InformantRegisterDefendant> defendantList = new ArrayList<>();
        prosecutionCases.stream().forEach(prosecutionCase -> {
            if (isNotEmpty(prosecutionCase.getDefendants())) {
                prosecutionCase.getDefendants().stream().forEach(defendant ->
                        defendantList.add(getDefendant(masterDefendant, prosecutionCase, defendant)));
            }
        });
        return defendantList;
    }

    private static InformantRegisterDefendant getDefendant(final InformantRegisterDefendant masterDefendant,
                                                           final ProsecutionCase prosecutionCase,
                                                           final Defendant defendant) {
        return InformantRegisterDefendant.informantRegisterDefendant()
                .withName(getName(defendant))
                .withDateOfBirth(getDateOfBirth(defendant))
                .withNationality(getNationality(defendant))
                .withAddress1(getAddressPart(defendant, LINE1))
                .withAddress2(getAddressPart(defendant, LINE2))
                .withAddress3(getAddressPart(defendant, LINE3))
                .withAddress4(getAddressPart(defendant, LINE4))
                .withAddress5(getAddressPart(defendant, LINE5))
                .withPostCode(getAddressPart(defendant, POST_CODE))
                .withTitle(getTitle(defendant))
                .withFirstName(getFirstName(defendant))
                .withLastName(getLastName(defendant))
                .withResults(masterDefendant.getResults())
                .withProsecutionCasesOrApplications(getProsecutionCasesOrApplication(masterDefendant, prosecutionCase, defendant))
                .build();
    }

    private static String getName(final Defendant defendant) {
        if (nonNull(defendant)) {
            if (isPersonDetailsAvailable(defendant)) {
                return getFullNameOfPerson(defendant.getPersonDefendant());
            } else if (nonNull(defendant.getLegalEntityDefendant()) &&
                    nonNull(defendant.getLegalEntityDefendant().getOrganisation())) {
                return defendant.getLegalEntityDefendant().getOrganisation().getName();
            }
        }
        return null;
    }

    private static String getFullNameOfPerson(final PersonDefendant person) {
        final StringBuilder fullName = new StringBuilder(EMPTY);
        if (isNotEmpty(person.getPersonDetails().getFirstName())) {
            fullName.append(person.getPersonDetails().getFirstName()).append(" ");
        }
        if (isNotEmpty(person.getPersonDetails().getMiddleName())) {
            fullName.append(person.getPersonDetails().getMiddleName()).append(" ");
        }
        if (isNotEmpty(person.getPersonDetails().getLastName())) {
            fullName.append(person.getPersonDetails().getLastName());
        }
        return fullName.toString();
    }

    private static String getDateOfBirth(final Defendant defendant) {
        if (isPersonDetailsAvailable(defendant) && nonNull(defendant.getPersonDefendant().getPersonDetails().getDateOfBirth())) {
            return defendant.getPersonDefendant().getPersonDetails().getDateOfBirth().format(formatter);
        }
        return null;
    }

    private static String getNationality(final Defendant defendant) {
        if (isPersonDetailsAvailable(defendant)) {
            return defendant.getPersonDefendant().getPersonDetails().getNationalityCode();
        }
        return null;
    }

    private static String getAddressPart(final Defendant defendant, final String part) {
        Address address = null;
        if (isAddressAvailable(defendant)) {
            address = defendant.getPersonDefendant().getPersonDetails().getAddress();
        } else if (isLegalEntityDefendantAddressAvailable(defendant)) {
            address = defendant.getLegalEntityDefendant().getOrganisation().getAddress();
        }

        if (nonNull(address)) {
            switch (part) {
                case LINE1:
                    return address.getAddress1();
                case LINE2:
                    return address.getAddress2();
                case LINE3:
                    return address.getAddress3();
                case LINE4:
                    return address.getAddress4();
                case LINE5:
                    return address.getAddress5();
                case POST_CODE:
                    return address.getPostcode();
                default:
                    return null;
            }
        }
        return null;
    }

    private static String getTitle(final Defendant defendant) {
        if (isPersonDetailsAvailable(defendant)) {
            return defendant.getPersonDefendant().getPersonDetails().getTitle();
        }
        return null;
    }

    private static String getFirstName(final Defendant defendant) {
        if (isPersonDetailsAvailable(defendant)) {
            return defendant.getPersonDefendant().getPersonDetails().getFirstName();
        }
        return null;
    }

    private static String getLastName(final Defendant defendant) {
        if (isPersonDetailsAvailable(defendant)) {
            return defendant.getPersonDefendant().getPersonDetails().getLastName();
        }
        return null;
    }

    private static boolean isPersonDetailsAvailable(final Defendant defendant) {
        return nonNull(defendant.getPersonDefendant()) &&
                nonNull(defendant.getPersonDefendant().getPersonDetails());
    }

    private static boolean isAddressAvailable(final Defendant defendant) {
        return nonNull(defendant.getPersonDefendant()) &&
                nonNull(defendant.getPersonDefendant().getPersonDetails()) &&
                nonNull(defendant.getPersonDefendant().getPersonDetails().getAddress());
    }

    private static boolean isLegalEntityDefendantAddressAvailable(final Defendant defendant) {
        return nonNull(defendant.getLegalEntityDefendant()) &&
                nonNull(defendant.getLegalEntityDefendant().getOrganisation()) &&
                nonNull(defendant.getLegalEntityDefendant().getOrganisation().getAddress());
    }
}