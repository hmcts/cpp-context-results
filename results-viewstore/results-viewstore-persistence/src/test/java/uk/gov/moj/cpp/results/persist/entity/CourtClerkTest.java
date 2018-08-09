package uk.gov.moj.cpp.results.persist.entity;

import org.junit.Test;

import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

public class CourtClerkTest {

    private final static CourtClerk COURT_CLERK = CourtClerk.builder()
            .withClerkOfTheCourtId(UUID.randomUUID())
            .withClerkOfTheCourtFirstName(STRING.next())
            .withClerkOfTheCourtLastName(STRING.next())
            .build();

    @Test
    public void shouldBeAbleToOverwriteFieldsFromBuilder() throws Exception {

        final CourtClerk data = CourtClerk.of(COURT_CLERK)
                .withClerkOfTheCourtId(UUID.randomUUID())
                .withClerkOfTheCourtFirstName(STRING.next())
                .withClerkOfTheCourtLastName(STRING.next())
                .build();

        final CourtClerk subject = CourtClerk.of(COURT_CLERK)
                .withClerkOfTheCourtId(data.getClerkOfTheCourtId())
                .withClerkOfTheCourtFirstName(data.getClerkOfTheCourtFirstName())
                .withClerkOfTheCourtLastName(data.getClerkOfTheCourtLastName())
                .build();

        assertThat(subject.getClerkOfTheCourtId(), is(data.getClerkOfTheCourtId()));
        assertThat(subject.getClerkOfTheCourtFirstName(), is(data.getClerkOfTheCourtFirstName()));
        assertThat(subject.getClerkOfTheCourtLastName(), is(data.getClerkOfTheCourtLastName()));
    }
}