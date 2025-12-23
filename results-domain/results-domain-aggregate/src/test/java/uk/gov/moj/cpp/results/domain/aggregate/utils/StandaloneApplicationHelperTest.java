package uk.gov.moj.cpp.results.domain.aggregate.utils;

import static java.time.LocalDate.now;
import static java.util.Collections.singletonList;
import static java.util.UUID.fromString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.fail;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.AttendanceDay;
import uk.gov.justice.core.courts.AttendanceType;
import uk.gov.justice.core.courts.CaseDefendant;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.DefendantAttendance;
import uk.gov.justice.core.courts.Gender;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.IndividualDefendant;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.OffenceDetails;
import uk.gov.justice.core.courts.OrganisationDetails;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;

import java.io.StringReader;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.Collections;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import com.google.common.io.Resources;
import org.junit.jupiter.api.Test;

public class StandaloneApplicationHelperTest {

    private static final JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(new ObjectMapperProducer().objectMapper());

    private static final String POLICE_ASN_DEFAULT_VALUE = "0800PP0100000000001H";
    public static final int DEFAULT_OFFENCE_SEQ_NUMBER = 1;
    public static final int DEFAULT_OFFENCE_DATE_CODE = 1;

    @Test
    public void givenStandaloneApplication_whenIndividualSubject_shouldBuildDefendantFromSubject() {

        final JsonObject applicationPayload = getPayload("json/application-payload_standalone_person-defendant.json");
        final CourtApplication application = jsonObjectToObjectConverter.convert(applicationPayload, CourtApplication.class);
        final Hearing hearing = Hearing.hearing()
                .withDefendantAttendance(singletonList(DefendantAttendance.defendantAttendance()
                        .withDefendantId(fromString("5a39fe5a-c07f-4b1c-abfe-31c1bff71658"))
                        .withAttendanceDays(singletonList(AttendanceDay.attendanceDay()
                                .withDay(now())
                                .withAttendanceType(AttendanceType.IN_PERSON)
                                .build()))
                        .build()))
                .build();

        final CaseDefendant caseDefendant = StandaloneApplicationHelper.buildDefendantFromSubject(application, hearing);

        final JsonObject subject = applicationPayload.getJsonObject("subject");
        final JsonObject personDetails = subject.getJsonObject("personDetails");
        final JsonObject addressJson = personDetails.getJsonObject("address");
        final JsonObject type = applicationPayload.getJsonObject("type");
        final JsonObject judicialResultJson = applicationPayload.getJsonArray("judicialResults").getJsonObject(0);

        assertThat(caseDefendant.getDefendantId(), is(fromString(subject.getString("id"))));
        assertThat(caseDefendant.getProsecutorReference(), is(POLICE_ASN_DEFAULT_VALUE));
        assertThat(caseDefendant.getAttendanceDays().size(), is(1));
        assertThat(caseDefendant.getAttendanceDays().get(0).getAttendanceType(), is(AttendanceType.IN_PERSON));
        assertThat(caseDefendant.getAttendanceDays().get(0).getDay(), is(LocalDate.now()));

        final IndividualDefendant individualDefendant = caseDefendant.getIndividualDefendant();
        assertThat(individualDefendant, notNullValue());
        assertThat(individualDefendant.getBailStatus().getCode(), is(judicialResultJson.getString("postHearingCustodyStatus")));

        assertThat(individualDefendant.getPerson().getFirstName(), is(personDetails.getString("firstName")));
        assertThat(individualDefendant.getPerson().getLastName(), is(personDetails.getString("lastName")));
        assertThat(individualDefendant.getPerson().getDateOfBirth(), is(LocalDate.parse(personDetails.getString("dateOfBirth"))));
        assertThat(individualDefendant.getPerson().getGender(), is(Gender.valueOf(personDetails.getString("gender"))));

        final Address address = individualDefendant.getPerson().getAddress();
        assertThat(address.getAddress1(), is(addressJson.getString("address1")));
        assertThat(address.getAddress2(), is(addressJson.getString("address2")));
        assertThat(address.getAddress3(), is(addressJson.getString("address3")));
        assertThat(address.getAddress4(), is(addressJson.getString("address4")));
        assertThat(address.getAddress5(), is(addressJson.getString("address5")));
        assertThat(address.getPostcode(), is(addressJson.getString("postcode")));

        assertThat(caseDefendant.getOffences(), hasSize(1));
        final OffenceDetails offence = caseDefendant.getOffences().get(0);

        assertThat(offence.getOffenceSequenceNumber(), is(DEFAULT_OFFENCE_SEQ_NUMBER));
        assertThat(offence.getOffenceCode(), is(type.getString("code")));
        assertThat(offence.getWording(), is(applicationPayload.getString("applicationParticulars")));
        assertThat(offence.getStartDate(), is(LocalDate.parse(applicationPayload.getString("applicationReceivedDate"))));
        assertThat(offence.getFinalDisposal(), is("Y"));
        assertThat(offence.getOffenceDateCode(), is(DEFAULT_OFFENCE_DATE_CODE));

        assertThat(offence.getJudicialResults(), hasSize(1));
        final JudicialResult judicialResult = offence.getJudicialResults().get(0);
        assertThat(judicialResult.getJudicialResultId(), is(fromString(judicialResultJson.getString("judicialResultId"))));
        assertThat(judicialResult.getLabel(), is(judicialResultJson.getString("label")));
        assertThat(judicialResult.getResultText(), is(judicialResultJson.getString("resultText")));
    }

    @Test
    public void givenStandaloneApplication_whenIndividualSubject_NoFinalResults_shouldBuildDefendantFromSubject() {

        final JsonObject applicationPayload = getPayload("json/application-payload_standalone_person-defendant_no-final-result.json");
        final CourtApplication application = jsonObjectToObjectConverter.convert(applicationPayload, CourtApplication.class);
        final Hearing hearing = Hearing.hearing()
                .withDefendantAttendance(singletonList(DefendantAttendance.defendantAttendance()
                        .withDefendantId(fromString("5a39fe5a-c07f-4b1c-abfe-31c1bff71658"))
                        .withAttendanceDays(singletonList(AttendanceDay.attendanceDay()
                                .withDay(now())
                                .withAttendanceType(AttendanceType.IN_PERSON)
                                .build()))
                        .build()))
                .build();

        final CaseDefendant caseDefendant = StandaloneApplicationHelper.buildDefendantFromSubject(application, hearing);

        final JsonObject subject = applicationPayload.getJsonObject("subject");
        final JsonObject personDetails = subject.getJsonObject("personDetails");
        final JsonObject addressJson = personDetails.getJsonObject("address");
        final JsonObject type = applicationPayload.getJsonObject("type");
        final JsonObject judicialResultJson = applicationPayload.getJsonArray("judicialResults").getJsonObject(0);

        assertThat(caseDefendant.getDefendantId(), is(fromString(subject.getString("id"))));
        assertThat(caseDefendant.getProsecutorReference(), is(POLICE_ASN_DEFAULT_VALUE));
        assertThat(caseDefendant.getAttendanceDays().size(), is(1));
        assertThat(caseDefendant.getAttendanceDays().get(0).getAttendanceType(), is(AttendanceType.IN_PERSON));
        assertThat(caseDefendant.getAttendanceDays().get(0).getDay(), is(LocalDate.now()));

        final IndividualDefendant individualDefendant = caseDefendant.getIndividualDefendant();
        assertThat(individualDefendant, notNullValue());
        assertThat(individualDefendant.getBailStatus().getCode(), is(judicialResultJson.getString("postHearingCustodyStatus")));

        assertThat(individualDefendant.getPerson().getFirstName(), is(personDetails.getString("firstName")));
        assertThat(individualDefendant.getPerson().getLastName(), is(personDetails.getString("lastName")));
        assertThat(individualDefendant.getPerson().getDateOfBirth(), is(LocalDate.parse(personDetails.getString("dateOfBirth"))));
        assertThat(individualDefendant.getPerson().getGender(), is(Gender.valueOf(personDetails.getString("gender"))));

        final Address address = individualDefendant.getPerson().getAddress();
        assertThat(address.getAddress1(), is(addressJson.getString("address1")));
        assertThat(address.getAddress2(), is(addressJson.getString("address2")));
        assertThat(address.getAddress3(), is(addressJson.getString("address3")));
        assertThat(address.getAddress4(), is(addressJson.getString("address4")));
        assertThat(address.getAddress5(), is(addressJson.getString("address5")));
        assertThat(address.getPostcode(), is(addressJson.getString("postcode")));

        assertThat(caseDefendant.getOffences(), hasSize(1));
        final OffenceDetails offence = caseDefendant.getOffences().get(0);

        assertThat(offence.getOffenceSequenceNumber(), is(DEFAULT_OFFENCE_SEQ_NUMBER));
        assertThat(offence.getOffenceCode(), is(type.getString("code")));
        assertThat(offence.getWording(), is(applicationPayload.getString("applicationParticulars")));
        assertThat(offence.getStartDate(), is(LocalDate.parse(applicationPayload.getString("applicationReceivedDate"))));
        assertThat(offence.getFinalDisposal(), is("N"));
        assertThat(offence.getOffenceDateCode(), is(DEFAULT_OFFENCE_DATE_CODE));

        assertThat(offence.getJudicialResults(), hasSize(1));
        final JudicialResult judicialResult = offence.getJudicialResults().get(0);
        assertThat(judicialResult.getJudicialResultId(), is(fromString(judicialResultJson.getString("judicialResultId"))));
        assertThat(judicialResult.getLabel(), is(judicialResultJson.getString("label")));
        assertThat(judicialResult.getResultText(), is(judicialResultJson.getString("resultText")));
    }

    @Test
    public void givenStandaloneApplication_whenCorporateSubject_shouldBuildDefendantFromSubject() {

        final JsonObject applicationPayload = getPayload("json/application-payload_standalone_corporate-defendant.json");
        final CourtApplication application = jsonObjectToObjectConverter.convert(applicationPayload, CourtApplication.class);
        final Hearing hearing = Hearing.hearing()
                .withDefendantAttendance(null)
                .build();

        final CaseDefendant caseDefendant = StandaloneApplicationHelper.buildDefendantFromSubject(application, hearing);

        final JsonObject subject = applicationPayload.getJsonObject("subject");
        final JsonObject organisation = subject.getJsonObject("organisation");
        final JsonObject addressJson = organisation.getJsonObject("address");
        final JsonObject type = applicationPayload.getJsonObject("type");
        final JsonObject judicialResultJson = applicationPayload.getJsonArray("judicialResults").getJsonObject(0);

        assertThat(caseDefendant.getDefendantId(), is(fromString(subject.getString("id"))));
        assertThat(caseDefendant.getProsecutorReference(), is(POLICE_ASN_DEFAULT_VALUE));
        assertThat(caseDefendant.getAttendanceDays(), is(Collections.emptyList()));

        final OrganisationDetails corporateDefendant = caseDefendant.getCorporateDefendant();
        assertThat(corporateDefendant, notNullValue());

        assertThat(corporateDefendant.getName(), is(organisation.getString("name")));

        final Address address = corporateDefendant.getAddress();
        assertThat(address.getAddress1(), is(addressJson.getString("address1")));
        assertThat(address.getAddress2(), is(addressJson.getString("address2")));
        assertThat(address.getAddress3(), is(addressJson.getString("address3")));
        assertThat(address.getAddress4(), is(addressJson.getString("address4")));
        assertThat(address.getAddress5(), is(addressJson.getString("address5")));
        assertThat(address.getPostcode(), is(addressJson.getString("postcode")));

        assertThat(caseDefendant.getOffences(), hasSize(1));
        final OffenceDetails offence = caseDefendant.getOffences().get(0);

        assertThat(offence.getOffenceSequenceNumber(), is(DEFAULT_OFFENCE_SEQ_NUMBER));
        assertThat(offence.getOffenceCode(), is(type.getString("code")));
        assertThat(offence.getWording(), is(applicationPayload.getString("applicationParticulars")));
        assertThat(offence.getStartDate(), is(LocalDate.parse(applicationPayload.getString("applicationReceivedDate"))));
        assertThat(offence.getFinalDisposal(), is("Y"));
        assertThat(offence.getOffenceDateCode(), is(DEFAULT_OFFENCE_DATE_CODE));

        assertThat(offence.getJudicialResults(), hasSize(1));
        final JudicialResult judicialResult = offence.getJudicialResults().get(0);
        assertThat(judicialResult.getJudicialResultId(), is(fromString(judicialResultJson.getString("judicialResultId"))));
        assertThat(judicialResult.getLabel(), is(judicialResultJson.getString("label")));
        assertThat(judicialResult.getResultText(), is(judicialResultJson.getString("resultText")));
    }

    private static JsonObject getPayload(final String path) {
        String request = null;
        try {
            request = Resources.toString(
                    Resources.getResource(path),
                    Charset.defaultCharset()
            );
        } catch (final Exception e) {
            fail("Error consuming file from location " + path);
        }
        final JsonReader reader = Json.createReader(new StringReader(request));
        return reader.readObject();
    }


}