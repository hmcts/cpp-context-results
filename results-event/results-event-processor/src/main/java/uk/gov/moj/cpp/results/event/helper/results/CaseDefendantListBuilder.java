package uk.gov.moj.cpp.results.event.helper.results;

import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static uk.gov.justice.core.courts.AssociatedIndividual.associatedIndividual;
import static uk.gov.justice.core.courts.CaseDefendant.caseDefendant;
import static uk.gov.justice.core.courts.Individual.individual;
import static uk.gov.justice.core.courts.IndividualDefendant.individualDefendant;
import static uk.gov.moj.cpp.results.event.helper.results.CommonMethods.getPresentAtHearing;

import uk.gov.justice.core.courts.AssociatedPerson;
import uk.gov.justice.core.courts.AttendanceDay;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.moj.cpp.results.event.helper.ReferenceCache;

import javax.inject.Inject;
import javax.json.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;



public class CaseDefendantListBuilder {

    private static final String POLICE_ASN_DEFAULT_VALUE = "0800PP0100000000001H";

    ReferenceCache referenceCache;

    @Inject
    public CaseDefendantListBuilder(final ReferenceCache referenceCache) {
        this.referenceCache = referenceCache;
    }

    public List<uk.gov.justice.core.courts.CaseDefendant> buildDefendantList(final List<Defendant> defendants, final Hearing hearing) {
        final List<uk.gov.justice.core.courts.CaseDefendant> defendantList = new ArrayList<>();

        for (final Defendant defendant : defendants) {

            final List<AttendanceDay> attendanceDays = null != hearing.getDefendantAttendance() ? new uk.gov.moj.cpp.results.event.helper.results.AttendanceDay().buildAttendance(hearing.getDefendantAttendance(), defendant.getId()) : emptyList();

            final uk.gov.justice.core.courts.CaseDefendant.Builder builder = caseDefendant()
                    .withDefendantId(defendant.getId())
                    .withProsecutorReference(getProsecutorReference(defendant.getProsecutionAuthorityReference(), defendant.getPersonDefendant()))
                    .withPncId(defendant.getPncId())
                    .withAttendanceDays(attendanceDays)
                    .withJudicialResults(defendant.getDefendantCaseJudicialResults())
                    .withOffences(new OffenceDetails().buildOffences(defendant, hearing.getDefendantJudicialResults()))
                    .withAssociatedPerson(buildAssociatedDefendant(defendant.getAssociatedPersons()));

            final String presentAtHearing = getPresentAtHearing(attendanceDays, hearing, defendant);
            addCorporateDefendant(defendant, builder, presentAtHearing);
            addIndividualDefendant(defendant, builder, presentAtHearing);
            defendantList.add(builder.build());
        }

        return defendantList;
    }


    private List<uk.gov.justice.core.courts.AssociatedIndividual> buildAssociatedDefendant(final List<AssociatedPerson> associatedPersons) {
        final List<uk.gov.justice.core.courts.AssociatedIndividual> resultList = new ArrayList<>();
        if (null != associatedPersons) {
            associatedPersons.stream().forEach(a -> resultList.add(associatedIndividual().withRole(a.getRole()).withPerson(buildIndividual(a.getPerson())).build()));
        }
        return resultList;
    }


    private void addIndividualDefendant(final Defendant defendant, final uk.gov.justice.core.courts.CaseDefendant.Builder builder, final String presentAtHearing) {
        if (null != defendant.getPersonDefendant()) {
            builder.withIndividualDefendant(buildIndividualDefendant(defendant.getPersonDefendant(), presentAtHearing));
        }
    }

    private void addCorporateDefendant(final Defendant defendant, final uk.gov.justice.core.courts.CaseDefendant.Builder builder, final String presentAtHearing) {
        if (null != defendant.getDefenceOrganisation()) {
            builder.withCorporateDefendant(new OrganisationDetails().buildCorporateDefendant(defendant.getDefenceOrganisation(), presentAtHearing));
        }
    }

    private String getProsecutorReference(final String prosecutionAuthorityReference, final PersonDefendant personDefendant) {
        if (isNotEmpty(prosecutionAuthorityReference)) {
            return prosecutionAuthorityReference;
        } else if (null != personDefendant && null != personDefendant.getArrestSummonsNumber()) {
            return personDefendant.getArrestSummonsNumber();
        }
        return POLICE_ASN_DEFAULT_VALUE;
    }

    private uk.gov.justice.core.courts.Individual buildIndividual(final Person personDetails) {
        return individual()
                .withFirstName(personDetails.getFirstName())
                .withLastName(personDetails.getLastName())
                .withNationality(findIsoCodeFromNationality(personDetails))
                .withMiddleName(personDetails.getMiddleName())
                .withContact(personDetails.getContact())
                .withAddress(personDetails.getAddress())
                .withTitle(personDetails.getTitle())
                .withGender(personDetails.getGender())
                .withDateOfBirth(personDetails.getDateOfBirth())
                .build();
    }

    private String findIsoCodeFromNationality(final Person personDetails) {
        Optional<String> isoCode = empty();

        if (null != personDetails.getNationalityId()) {
            final Optional<JsonObject> nationalityResult = referenceCache.getNationalityById(personDetails.getNationalityId());
            if (nationalityResult.isPresent()) {
                final JsonObject jsonObject = nationalityResult.get();
                isoCode = of("isoCode").filter(jsonObject::containsKey).map(jsonObject::getString);
            }
        } else if (null != personDetails.getNationalityCode()) {
            isoCode = ofNullable(personDetails.getNationalityCode());
        }
        return isoCode.orElse(null);
    }

    private uk.gov.justice.core.courts.IndividualDefendant buildIndividualDefendant(final PersonDefendant personDefendant, final String presentAtHearing) {
        final uk.gov.justice.core.courts.IndividualDefendant.Builder individualDefendant = individualDefendant()
                .withBailStatus(personDefendant.getBailStatus())
                .withPresentAtHearing(presentAtHearing)
                .withPerson(buildIndividual(personDefendant.getPersonDetails()));
        if (null != personDefendant.getBailConditions()) {
            individualDefendant.withBailConditions(personDefendant.getBailConditions());
        }
        if (null != personDefendant.getBailReasons()) {
            individualDefendant.withReasonForBailConditionsOrCustody(personDefendant.getBailReasons());
        }
        return individualDefendant.build();

    }
}
