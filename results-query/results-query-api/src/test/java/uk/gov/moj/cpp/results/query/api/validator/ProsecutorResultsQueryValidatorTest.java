package uk.gov.moj.cpp.results.query.api.validator;

import static java.time.LocalDate.now;
import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;

import javax.json.JsonObjectBuilder;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class ProsecutorResultsQueryValidatorTest {

    private ProsecutorResultsQueryValidator validator = new ProsecutorResultsQueryValidator();

    private static final String ERROR_CODE_SHARED_DATE_IN_FUTURE = "SHARED_DATE_IN_FUTURE";
    private static final String ERROR_CODE_SHARED_DATE_RANGE_INVALID = "SHARED_DATE_RANGE_INVALID";

    public static Object[][] errorScenarioSpecification() {
        return new Object[][]{
                // start date, end date, error code

                // start date in future
                {now().plusDays(1).toString(), null, ERROR_CODE_SHARED_DATE_IN_FUTURE},

                // end date in future
                {now().toString(), now().plusDays(1).toString(), ERROR_CODE_SHARED_DATE_IN_FUTURE},

                // date range exceeding last 7 days
                {now().minusDays(8).toString(), now().minusDays(5).toString(), ERROR_CODE_SHARED_DATE_RANGE_INVALID},

                // end date before start date
                {now().minusDays(3).toString(), now().minusDays(5).toString(), ERROR_CODE_SHARED_DATE_RANGE_INVALID},

                // invalid start date
                {"dummy", now().minusDays(5).toString(), ERROR_CODE_SHARED_DATE_RANGE_INVALID},

                // invalid end date
                {now().minusDays(5).toString(), "dummy", ERROR_CODE_SHARED_DATE_RANGE_INVALID},
        };
    }

    public static Object[][] successScenarioSpecification() {
        return new Object[][]{
                // start date, end date

                // start date in past
                {now().minusDays(1).toString(), null},

                // start date in past - beyond 7 days when no end date is supplied
                {now().minusDays(40).toString(), null},

                // start and end date in last 7 days
                {now().minusDays(7).toString(), now().minusDays(5).toString()},
        };
    }

    @ParameterizedTest
    @MethodSource("errorScenarioSpecification")
    public void shouldThrowException(final String startDate, final String endDate, final String errorCode) {
        try {
            validator.validatePayload(createPayload(startDate, endDate));
        } catch (final BadRequestException e) {
            assertThat(e.getMessage(), is(errorCode));
            return;
        }
        fail("Should have failed specification");
    }

    @ParameterizedTest
    @MethodSource("successScenarioSpecification")
    public void shouldProcessSuccessfully(final String startDate, final String endDate) {
        try {
            validator.validatePayload(createPayload(startDate, endDate));
        } catch (final Exception e) {
            fail("No exception should be thrown");
        }
    }

    private JsonEnvelope createPayload(final String startDate, final String endDate) {
        final MetadataBuilder metadataBuilder = metadataBuilder().withId(randomUUID()).withName("results.prosecutor-results");
        final JsonObjectBuilder payloadBuilder = createObjectBuilder().add("startDate", startDate);
        if (nonNull(endDate)) {
            payloadBuilder.add("endDate", endDate);
        }

        return envelopeFrom(metadataBuilder, payloadBuilder);
    }
}