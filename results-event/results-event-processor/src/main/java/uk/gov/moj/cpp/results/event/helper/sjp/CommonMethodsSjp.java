package uk.gov.moj.cpp.results.event.helper.sjp;

import static uk.gov.justice.core.courts.ContactNumber.contactNumber;

import uk.gov.justice.core.courts.ContactNumber;
import uk.gov.justice.sjp.results.BasePersonDetail;

public class CommonMethodsSjp {

    private  CommonMethodsSjp(){}

    public static ContactNumber buildContactNumber(final BasePersonDetail basePersonDetails) {
        return contactNumber()
                //.withFax(basePersonDetails.) //Not Available
                .withHome(basePersonDetails.getTelephoneNumberHome())
                .withMobile(basePersonDetails.getTelephoneNumberMobile())
                .withPrimaryEmail(basePersonDetails.getEmailAddress1())
                .withSecondaryEmail(basePersonDetails.getEmailAddress2())
                .withWork(basePersonDetails.getTelephoneNumberBusiness())
                .build();
    }
}
