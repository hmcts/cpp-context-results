package uk.gov.moj.cpp.results.event.helper.sjp;
import static uk.gov.justice.core.courts.OrganisationDetails.organisationDetails;
import static uk.gov.moj.cpp.results.event.helper.sjp.CommonMethodsSjp.buildContactNumber;
import uk.gov.justice.core.courts.OrganisationDetails;
import uk.gov.justice.sjp.results.CorporateDefendant;

public class OrganisationDetailsSjp {

    private static final String PRESENT_AT_HEARING = "N";

    public OrganisationDetails buildCorporateDefendant(final uk.gov.justice.sjp.results.CaseDefendant sjpDefendant) {

        final CorporateDefendant sjpCorporateDefendant = sjpDefendant.getCorporateDefendant();

        return organisationDetails().withName(sjpCorporateDefendant.getOrganisationName())
                .withAddress(sjpCorporateDefendant.getAddress())
                .withContact(buildContactNumber(sjpDefendant.getParentGuardianDetails()))
                .withPresentAtHearing(PRESENT_AT_HEARING)
                .withIncorporationNumber(sjpCorporateDefendant.getPncIdentifier()) //Need to Check again.
                // .withRegisteredCharityNumber(sjpCorporateDefendant) //Not Available.
                .build();
    }


}
