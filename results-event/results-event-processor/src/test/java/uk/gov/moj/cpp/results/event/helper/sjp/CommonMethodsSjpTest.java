package uk.gov.moj.cpp.results.event.helper.sjp;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static uk.gov.moj.cpp.results.event.helper.TestTemplate.buildBasePersonDetails;
import static uk.gov.moj.cpp.results.event.helper.sjp.CommonMethodsSjp.buildContactNumber;

import uk.gov.justice.core.courts.ContactNumber;
import uk.gov.justice.sjp.results.BasePersonDetail;

import org.junit.Test;

public class CommonMethodsSjpTest {

    @Test
    public void testBuildContactNumber() {
       BasePersonDetail basePersonDetail =  buildBasePersonDetails();
     ContactNumber contactNumber = buildContactNumber(basePersonDetail);

     assertThat(contactNumber.getHome(), is(basePersonDetail.getTelephoneNumberHome()));
     assertThat(contactNumber.getMobile(), is(basePersonDetail.getTelephoneNumberMobile()));
     assertThat(contactNumber.getWork(), is(basePersonDetail.getTelephoneNumberBusiness()));
     assertThat(contactNumber.getPrimaryEmail(), is(basePersonDetail.getEmailAddress1()));
     assertThat(contactNumber.getSecondaryEmail(), is(basePersonDetail.getEmailAddress2()));

    }


}