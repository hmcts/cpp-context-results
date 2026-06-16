package uk.gov.moj.cpp.results.domain.informant.model;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

public class InformantRegisterDefendantTest {

    @Test
    public void builder_setsRequiredFields() {
        final InformantRegisterDefendant defendant = InformantRegisterDefendant.informantRegisterDefendant()
                .withName("John Smith")
                .withAddress1("1 High Street")
                .withFirstName("John")
                .withLastName("Smith")
                .build();

        assertThat(defendant.getName(), is("John Smith"));
        assertThat(defendant.getAddress1(), is("1 High Street"));
        assertThat(defendant.getFirstName(), is("John"));
        assertThat(defendant.getLastName(), is("Smith"));
    }

    @Test
    public void builder_optionalFieldsAreNull() {
        final InformantRegisterDefendant defendant = InformantRegisterDefendant.informantRegisterDefendant()
                .withName("John Smith").withAddress1("1 High Street").build();

        assertThat(defendant.getAddress2(), is(nullValue()));
        assertThat(defendant.getDateOfBirth(), is(nullValue()));
        assertThat(defendant.getNationality(), is(nullValue()));
    }

    @Test
    public void builder_withCasesOrApplications_setsCorrectly() {
        final InformantRegisterCaseOrApplication coa = InformantRegisterCaseOrApplication.informantRegisterCaseOrApplication()
                .withCaseOrApplicationReference("URN-001").build();

        final InformantRegisterDefendant defendant = InformantRegisterDefendant.informantRegisterDefendant()
                .withName("John").withAddress1("1 Street")
                .withProsecutionCasesOrApplications(singletonList(coa))
                .build();

        assertThat(defendant.getProsecutionCasesOrApplications(), hasSize(1));
    }

    @Test
    public void jacksonDeserialization_roundtrip() throws Exception {
        final ObjectMapper mapper = new ObjectMapper();
        final String json = "{\"name\":\"John Smith\",\"address1\":\"1 High Street\",\"firstName\":\"John\",\"lastName\":\"Smith\"}";

        final InformantRegisterDefendant defendant = mapper.readValue(json, InformantRegisterDefendant.class);

        assertThat(defendant.getName(), is("John Smith"));
        assertThat(defendant.getFirstName(), is("John"));
    }
}
