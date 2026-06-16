package uk.gov.moj.cpp.results.domain.informant.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

public class InformantRegisterRecipientTest {

    @Test
    public void builder_setsRequiredFields() {
        final InformantRegisterRecipient recipient = InformantRegisterRecipient.informantRegisterRecipient()
                .withRecipientName("Prosecutor A")
                .withEmailAddress1("a@example.com")
                .withEmailTemplateName("TEMPLATE_1")
                .build();

        assertThat(recipient.getRecipientName(), is("Prosecutor A"));
        assertThat(recipient.getEmailAddress1(), is("a@example.com"));
        assertThat(recipient.getEmailTemplateName(), is("TEMPLATE_1"));
    }

    @Test
    public void builder_emailAddress2IsOptional() {
        final InformantRegisterRecipient recipient = InformantRegisterRecipient.informantRegisterRecipient()
                .withRecipientName("Prosecutor A").withEmailAddress1("a@example.com").build();

        assertThat(recipient.getEmailAddress2(), is(nullValue()));
    }

    @Test
    public void jacksonDeserialization_roundtrip() throws Exception {
        final ObjectMapper mapper = new ObjectMapper();
        final String json = "{\"recipientName\":\"Prosecutor A\",\"emailAddress1\":\"a@example.com\",\"emailTemplateName\":\"T1\"}";

        final InformantRegisterRecipient recipient = mapper.readValue(json, InformantRegisterRecipient.class);

        assertThat(recipient.getRecipientName(), is("Prosecutor A"));
        assertThat(recipient.getEmailAddress1(), is("a@example.com"));
    }
}
