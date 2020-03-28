package uk.gov.moj.cpp.results.event.helper.results;

import static uk.gov.justice.core.courts.OrganisationDetails.organisationDetails;

import uk.gov.justice.core.courts.Organisation;

public class OrganisationDetails {

    public uk.gov.justice.core.courts.OrganisationDetails buildCorporateDefendant(final Organisation defenceOrganisation, final String presentAtHearing) {
        return organisationDetails()
                .withName(defenceOrganisation.getName())
                .withIncorporationNumber(defenceOrganisation.getIncorporationNumber())
                .withContact(defenceOrganisation.getContact())
                .withAddress(defenceOrganisation.getAddress())
                .withPresentAtHearing(presentAtHearing)
                .withRegisteredCharityNumber(defenceOrganisation.getRegisteredCharityNumber())
                .build();
    }
}
