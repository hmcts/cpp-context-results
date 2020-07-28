package uk.gov.moj.cpp.results.event.helper.sjp;

import static java.util.Optional.ofNullable;
import static uk.gov.justice.core.courts.AssociatedIndividual.associatedIndividual;
import static uk.gov.justice.core.courts.CaseDefendant.caseDefendant;
import static uk.gov.justice.core.courts.Individual.individual;
import static uk.gov.justice.core.courts.IndividualDefendant.individualDefendant;
import static uk.gov.moj.cpp.results.event.helper.sjp.CommonMethodsSjp.buildContactNumber;

import uk.gov.justice.core.courts.AssociatedIndividual;
import uk.gov.justice.core.courts.BailStatus;
import uk.gov.justice.core.courts.Gender;
import uk.gov.justice.core.courts.Individual;
import uk.gov.justice.core.courts.IndividualDefendant;
import uk.gov.justice.sjp.results.BasePersonDetail;
import uk.gov.moj.cpp.results.event.helper.BailStatusConverter;
import uk.gov.moj.cpp.results.event.helper.ReferenceCache;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

public class CaseDefendantListBuilderSjp {

    private static final String PARENT_GUARDIAN = "parentGuardian";
    private static final String PRESENT_AT_HEARING = "N";

    ReferenceCache referenceCache;

    @Inject
    public CaseDefendantListBuilderSjp(final ReferenceCache referenceCache) {
        this.referenceCache = referenceCache;
    }

    public List<uk.gov.justice.core.courts.CaseDefendant> buildDefendantList(final uk.gov.justice.sjp.results.CaseDetails caseSjpDetails, final ZonedDateTime dateAndTimeOfSession, final UUID sessionId) {

        final List<uk.gov.justice.sjp.results.CaseDefendant> caseSjpDefendants = caseSjpDetails.getDefendants();
        final List<uk.gov.justice.core.courts.CaseDefendant> defendantList = new ArrayList<>();

        for (final uk.gov.justice.sjp.results.CaseDefendant sjpDefendant : caseSjpDefendants) {
            final uk.gov.justice.core.courts.CaseDefendant.Builder builder = caseDefendant()
                    .withDefendantId(sjpDefendant.getDefendantId())
                    .withAssociatedPerson(null != sjpDefendant.getParentGuardianDetails() ? buildIndividualList(sjpDefendant.getParentGuardianDetails()) : null)
                    .withOffences(new OffenceDetails(referenceCache).buildOffences(sjpDefendant, dateAndTimeOfSession, sessionId))
                    .withProsecutorReference(sjpDefendant.getProsecutorReference());

            if (null != sjpDefendant.getCorporateDefendant()) {
                builder.withCorporateDefendant(new OrganisationDetailsSjp().buildCorporateDefendant(sjpDefendant));
            }
            if (null != sjpDefendant.getIndividualDefendant()) {
                builder.withIndividualDefendant(buildIndividualDefendant(sjpDefendant.getIndividualDefendant()));
            }

            defendantList.add(builder.build());

        }
        return defendantList;
    }

    private IndividualDefendant buildIndividualDefendant(final uk.gov.justice.sjp.results.IndividualDefendant sjpIndividualDefendant) {
        final BailStatus bailStatus = new BailStatusConverter(referenceCache).convert(sjpIndividualDefendant.getBailStatus()).orElse(null);
        return individualDefendant()
                .withBailStatus(bailStatus)
                .withPresentAtHearing(PRESENT_AT_HEARING)
                .withPerson(buildIndividual(sjpIndividualDefendant.getBasePersonDetails(), sjpIndividualDefendant.getPersonStatedNationality()))
                .build();
    }

    private List<AssociatedIndividual> buildIndividualList(final BasePersonDetail basePersonDetails) {
        final List<AssociatedIndividual> individualList = new ArrayList<>();
        individualList.add(buildAssociatedIndividual(basePersonDetails));
        return individualList;
    }

    private AssociatedIndividual buildAssociatedIndividual(final BasePersonDetail basePersonDetail) {
        return associatedIndividual()
                .withPerson(buildIndividual(basePersonDetail, null))
                .withRole(PARENT_GUARDIAN)
                .build();
    }

    private Individual buildIndividual(final BasePersonDetail basePersonDetails, final String personStatedNationality) {
        return individual()
                .withFirstName(basePersonDetails.getFirstName())
                .withLastName(basePersonDetails.getLastName())
                .withNationality(personStatedNationality)
                .withContact(buildContactNumber(basePersonDetails))
                .withAddress(basePersonDetails.getAddress())
                .withGender(buildGender(basePersonDetails.getGender()))
                .withDateOfBirth(ofNullable(basePersonDetails.getBirthDate()).map(ZonedDateTime::toLocalDate).orElse(null))
                .build();
    }


    private Gender buildGender(final uk.gov.justice.sjp.results.Gender gender) {
        return Gender.valueOf(gender.name());
    }


}
