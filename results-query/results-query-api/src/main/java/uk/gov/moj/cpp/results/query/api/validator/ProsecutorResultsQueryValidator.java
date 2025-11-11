package uk.gov.moj.cpp.results.query.api.validator;

import static java.time.LocalDate.now;
import static java.util.Objects.isNull;
import static uk.gov.justice.services.messaging.JsonObjects.getString;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Optional;

import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProsecutorResultsQueryValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProsecutorResultsQueryValidator.class);
    private static final String FIELD_START_DATE = "startDate";
    private static final String FIELD_END_DATE = "endDate";

    private static final String ERROR_CODE_SHARED_DATE_IN_FUTURE = "SHARED_DATE_IN_FUTURE";
    private static final String ERROR_CODE_SHARED_DATE_RANGE_INVALID = "SHARED_DATE_RANGE_INVALID";

    public void validatePayload(final JsonEnvelope jsonEnvelope) {
        final JsonObject payloadAsJsonObject = jsonEnvelope.payloadAsJsonObject();

        final Optional<String> optionalStartDateAsString = getString(payloadAsJsonObject, FIELD_START_DATE);
        final Optional<String> optionalEndDateAsString = getString(payloadAsJsonObject, FIELD_END_DATE);

        try {
            optionalStartDateAsString.ifPresent(LocalDate::parse);
        } catch (final DateTimeParseException e) {
            LOGGER.error("Start date cannot be parsed", e);
            throw new BadRequestException(ERROR_CODE_SHARED_DATE_RANGE_INVALID);
        }

        try {
            optionalEndDateAsString.ifPresent(LocalDate::parse);
        } catch (final DateTimeParseException e) {
            LOGGER.error("End date cannot be parsed", e);
            throw new BadRequestException(ERROR_CODE_SHARED_DATE_RANGE_INVALID);
        }

        final LocalDate startDate = optionalStartDateAsString.map(LocalDate::parse).orElse(null);
        if (isNull(startDate)) {
            LOGGER.error("Start date not provided");
            throw new BadRequestException(ERROR_CODE_SHARED_DATE_RANGE_INVALID);
        }

        final LocalDate endDate = optionalEndDateAsString.map(LocalDate::parse).orElse(null);

        if (now().isBefore(startDate)) {
            LOGGER.error("Start date is in the future");
            throw new BadRequestException(ERROR_CODE_SHARED_DATE_IN_FUTURE);
        }

        if (isNull(endDate)) {
            return;
        }

        if (now().isBefore(endDate)) {
            LOGGER.error("End date is in the future");
            throw new BadRequestException(ERROR_CODE_SHARED_DATE_IN_FUTURE);
        }

        if (startDate.isAfter(endDate)) {
            LOGGER.error("Start date is after end date");
            throw new BadRequestException(ERROR_CODE_SHARED_DATE_RANGE_INVALID);
        }

        if (startDate.isBefore(LocalDate.now().minusDays(7))) {
            LOGGER.error("Start date more than 7 days in the past");
            throw new BadRequestException(ERROR_CODE_SHARED_DATE_RANGE_INVALID);
        }
    }
}
