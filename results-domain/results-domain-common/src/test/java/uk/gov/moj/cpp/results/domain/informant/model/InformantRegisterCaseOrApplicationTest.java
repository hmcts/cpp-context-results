package uk.gov.moj.cpp.results.domain.informant.model;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

public class InformantRegisterCaseOrApplicationTest {

    @Test
    public void builder_setsAllFields() {
        final InformantRegisterOffence offence = InformantRegisterOffence.informantRegisterOffence()
                .withOffenceCode("AB001").withOffenceTitle("Theft").withOrderIndex(1).build();

        final InformantRegisterCaseOrApplication coa = InformantRegisterCaseOrApplication.informantRegisterCaseOrApplication()
                .withCaseOrApplicationReference("URN-001")
                .withArrestSummonsNumber("ASN-001")
                .withApplicationParticulars("particulars")
                .withOffences(singletonList(offence))
                .build();

        assertThat(coa.getCaseOrApplicationReference(), is("URN-001"));
        assertThat(coa.getArrestSummonsNumber(), is("ASN-001"));
        assertThat(coa.getApplicationParticulars(), is("particulars"));
        assertThat(coa.getOffences(), hasSize(1));
        assertThat(coa.getOffences().get(0).getOffenceCode(), is("AB001"));
    }

    @Test
    public void jacksonDeserialization_roundtrip() throws Exception {
        final ObjectMapper mapper = new ObjectMapper();
        final String json = "{\"caseOrApplicationReference\":\"URN-001\",\"offences\":[" +
                "{\"offenceCode\":\"AB001\",\"offenceTitle\":\"Theft\",\"orderIndex\":1}]}";

        final InformantRegisterCaseOrApplication coa = mapper.readValue(json, InformantRegisterCaseOrApplication.class);

        assertThat(coa.getCaseOrApplicationReference(), is("URN-001"));
        assertThat(coa.getOffences(), hasSize(1));
    }
}
