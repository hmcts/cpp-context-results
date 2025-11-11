package uk.gov.moj.cpp.results.event.helper.results;

import static java.util.Optional.of;
import static java.util.UUID.fromString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static uk.gov.justice.core.courts.Address.address;
import static uk.gov.justice.core.courts.AttendanceDay.attendanceDay;
import static uk.gov.justice.core.courts.ContactNumber.contactNumber;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.AttendanceDay;
import uk.gov.justice.core.courts.AttendanceType;
import uk.gov.justice.core.courts.Organisation;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

public class OrganisationDetailsTest {

    private static final Optional<UUID> OPTIONAL_UUID = of(fromString("dddd1111-1e20-4c21-916a-81a6c90239e5"));
    public static final String INCORPORATION_NUMBER = "INC007";
    public static final String NAME = "JohnSmith ";
    public static final String REGISTERED_CHARITY_NUMBER = "CHARITY009";


    @Test
    public void testBuildCorporateDefendant() {

        List<AttendanceDay> attendanceDays = getAttendanceDays();
        Organisation defenceOrganisation = getOrganisation();

        uk.gov.justice.core.courts.OrganisationDetails organisationDetails = new OrganisationDetails().buildCorporateDefendant(defenceOrganisation, "Y");

        assertThat(organisationDetails.getIncorporationNumber(), is(INCORPORATION_NUMBER));
        assertThat(organisationDetails.getName(), is(NAME));
        assertThat(organisationDetails.getPresentAtHearing(), is("Y"));
        assertThat(organisationDetails.getRegisteredCharityNumber(), is(REGISTERED_CHARITY_NUMBER));
        assertContact(defenceOrganisation, organisationDetails);
        assertAddress(defenceOrganisation, organisationDetails);


    }

    private List<AttendanceDay> getAttendanceDays() {
        List<AttendanceDay> attendanceDays = new ArrayList<>();
        attendanceDays.add(attendanceDay().withAttendanceType(AttendanceType.IN_PERSON).withDay(LocalDate.of(2019, 02, 02)).build());
        return attendanceDays;
    }

    private void assertAddress(final Organisation defenceOrganisation, final uk.gov.justice.core.courts.OrganisationDetails organisationDetails) {
        assertThat(organisationDetails.getAddress().getAddress1(), is(defenceOrganisation.getAddress().getAddress1()));
        assertThat(organisationDetails.getAddress().getAddress2(), is(defenceOrganisation.getAddress().getAddress2()));
        assertThat(organisationDetails.getAddress().getAddress3(), is(defenceOrganisation.getAddress().getAddress3()));
        assertThat(organisationDetails.getAddress().getAddress4(), is(defenceOrganisation.getAddress().getAddress4()));
        assertThat(organisationDetails.getAddress().getAddress5(), is(defenceOrganisation.getAddress().getAddress5()));
        assertThat(organisationDetails.getAddress().getPostcode(), is(defenceOrganisation.getAddress().getPostcode()));
    }

    private void assertContact(final Organisation defenceOrganisation, final uk.gov.justice.core.courts.OrganisationDetails organisationDetails) {
        assertThat(organisationDetails.getContact().getHome(), is(defenceOrganisation.getContact().getHome()));
        assertThat(organisationDetails.getContact().getWork(), is(defenceOrganisation.getContact().getWork()));
        assertThat(organisationDetails.getContact().getMobile(), is(defenceOrganisation.getContact().getMobile()));
        assertThat(organisationDetails.getContact().getFax(), is(defenceOrganisation.getContact().getFax()));
        assertThat(organisationDetails.getContact().getPrimaryEmail(), is(defenceOrganisation.getContact().getPrimaryEmail()));
        assertThat(organisationDetails.getContact().getSecondaryEmail(), is(defenceOrganisation.getContact().getSecondaryEmail()));
    }

    private Organisation getOrganisation() {
        return Organisation.organisation()
                .withAddress(buildAddress())
                .withContact(contactNumber().withWork("5555555555")
                        .withMobile("7777777777")
                        .withHome("8888888888")
                        .withFax("9999999999")
                        .withPrimaryEmail("primaryemail@gmail.com")
                        .withSecondaryEmail("secondaryemail@gmail.com").build())
                .withIncorporationNumber(INCORPORATION_NUMBER)
                .withName(NAME)
                .withRegisteredCharityNumber(REGISTERED_CHARITY_NUMBER)
                .build();
    }

    private static Address buildAddress() {
        return address()
                .withAddress1("Fitzalan Place")
                .withAddress2("Cardiff")
                .withAddress3("addressline3")
                .withAddress4("address4")
                .withAddress5("address5")
                .withPostcode("CF24 0RZ")
                .build();
    }
}
