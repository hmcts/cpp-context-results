package uk.gov.moj.cpp.results.event.helper.sjp;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static uk.gov.moj.cpp.results.event.helper.TestTemplate.getCaseDefendant;

import uk.gov.justice.core.courts.OrganisationDetails;
import uk.gov.justice.sjp.results.CaseDefendant;

import org.junit.Test;

public class OrganisationDetailsSjpTest {

    @Test
    public void testBuildCorporateDefendant() {

        CaseDefendant caseDefendant = getCaseDefendant();
        OrganisationDetails organisationDetails =  new OrganisationDetailsSjp().buildCorporateDefendant(caseDefendant);
        assertThat(organisationDetails.getIncorporationNumber(), is(caseDefendant.getCorporateDefendant().getPncIdentifier()));
        assertThat(organisationDetails.getPresentAtHearing(), is("N"));
        assertThat(organisationDetails.getAddress(), is(caseDefendant.getCorporateDefendant().getAddress()));

    }
}
