package uk.gov.moj.cpp.results.domain.aggregate;

import static java.util.Collections.emptyMap;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static uk.gov.justice.hearing.courts.HearingFinancialResultRequest.hearingFinancialResultRequest;
import static uk.gov.justice.hearing.courts.OffenceResultsDetails.offenceResultsDetails;
import static uk.gov.moj.cpp.results.domain.aggregate.MarkedAggregateSendEmailEventBuilder.markedAggregateSendEmailEventBuilder;
import static uk.gov.moj.cpp.results.domain.aggregate.utils.CorrelationItem.correlationItem;

import uk.gov.justice.hearing.courts.HearingFinancialResultRequest;
import uk.gov.justice.hearing.courts.OffenceResults;
import uk.gov.moj.cpp.results.domain.event.MarkedAggregateSendEmailWhenAccountReceived;

import java.time.ZonedDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class MarkedAggregateSendEmailEventBuilderTest {

    private static final String OLD_GOB_ACCOUNT_NUMBER = "73663710A";
    private static final String OLD_DIVISION_CODE = "52";

    @Test
    void buildMarkedAggregateWithoutOldsForSpecificCorrelationIdWithEmail_shouldPopulateGobAccountNumberFromOldCorrelation_whenRequestHasItsOwnNewAccountCorrelationId() {
        final UUID offenceId = randomUUID();
        final UUID oldAccountCorrelationId = randomUUID();
        final UUID newAccountCorrelationId = randomUUID();

        final HearingFinancialResultRequest request = hearingFinancialResultRequest()
                .withAccountCorrelationId(newAccountCorrelationId)
                .withProsecutionCaseReferences(List.of("AAC15170749"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(offenceId)
                                .withIsFinancial(true)
                                .build()))
                .build();

        final LinkedList<uk.gov.moj.cpp.results.domain.aggregate.utils.CorrelationItem> correlationItemList = new LinkedList<>(List.of(
                correlationItem()
                        .withAccountCorrelationId(oldAccountCorrelationId)
                        .withHearingId(randomUUID())
                        .withAccountNumber(OLD_GOB_ACCOUNT_NUMBER)
                        .withAccountDivisionCode(OLD_DIVISION_CODE)
                        .withCreatedTime(ZonedDateTime.now().minusDays(1))
                        .withOffenceResultsDetailsList(List.of(
                                offenceResultsDetails()
                                        .withOffenceId(offenceId)
                                        .withIsFinancial(true)
                                        .withCreatedTime(ZonedDateTime.now().minusDays(1))
                                        .build()))
                        .build()));

        final MarkedAggregateSendEmailWhenAccountReceived notification = markedAggregateSendEmailEventBuilder("nces@justice.gov.uk", correlationItemList)
                .buildMarkedAggregateWithoutOldsForSpecificCorrelationIdWithEmail(
                        request,
                        "APPEAL DISMISSED",
                        List.of(),
                        "nces@justice.gov.uk",
                        null,
                        null,
                        null,
                        List.of(),
                        null,
                        null,
                        null,
                        emptyMap());

        assertThat("accountCorrelationId should stay as the request's own new correlation id "
                        + "when one is supplied, with the previously-known account carried separately",
                notification.getAccountCorrelationId(), is(newAccountCorrelationId));
        assertThat("gobAccountNumber should be populated from the previously-known account",
                notification.getOldAccountDetails().get(0).getGobAccountNumber(), is(OLD_GOB_ACCOUNT_NUMBER));
        assertThat("divisionCode should be populated from the previously-known account",
                notification.getOldAccountDetails().get(0).getDivisionCode(), is(OLD_DIVISION_CODE));
        assertThat("oldAccountDetails should reference the previously-known correlation id",
                notification.getOldAccountDetails().get(0).getAccountCorrelationId(), is(oldAccountCorrelationId));
    }

    @Test
    void buildMarkedAggregateWithoutOldsForSpecificCorrelationIdWithEmail_shouldPopulateGobAccountNumberFromOldCorrelation_whenRequestHasNoAccountCorrelationId() {
        final UUID offenceId = randomUUID();
        final UUID oldAccountCorrelationId = randomUUID();

        final HearingFinancialResultRequest request = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("AAC15170749"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(offenceId)
                                .withIsFinancial(true)
                                .build()))
                .build();

        final LinkedList<uk.gov.moj.cpp.results.domain.aggregate.utils.CorrelationItem> correlationItemList = new LinkedList<>(List.of(
                correlationItem()
                        .withAccountCorrelationId(oldAccountCorrelationId)
                        .withHearingId(randomUUID())
                        .withAccountNumber(OLD_GOB_ACCOUNT_NUMBER)
                        .withAccountDivisionCode(OLD_DIVISION_CODE)
                        .withCreatedTime(ZonedDateTime.now().minusDays(1))
                        .withOffenceResultsDetailsList(List.of(
                                offenceResultsDetails()
                                        .withOffenceId(offenceId)
                                        .withIsFinancial(true)
                                        .withCreatedTime(ZonedDateTime.now().minusDays(1))
                                        .build()))
                        .build()));

        final MarkedAggregateSendEmailWhenAccountReceived notification = markedAggregateSendEmailEventBuilder("nces@justice.gov.uk", correlationItemList)
                .buildMarkedAggregateWithoutOldsForSpecificCorrelationIdWithEmail(
                        request,
                        "APPEAL DISMISSED",
                        List.of(),
                        "nces@justice.gov.uk",
                        null,
                        null,
                        null,
                        List.of(),
                        null,
                        null,
                        null,
                        emptyMap());

        assertThat("gobAccountNumber should still be populated from the previously-known account",
                notification.getGobAccountNumber(), is(OLD_GOB_ACCOUNT_NUMBER));
        assertThat("accountCorrelationId should fall back to the previously-known correlation id",
                notification.getAccountCorrelationId(), is(oldAccountCorrelationId));
    }

    @Test
    void buildMarkedAggregateWithoutOldsForSpecificCorrelationIdWithEmail_shouldLeaveGobAccountNumberNull_whenNoPreviousAccountExists() {
        final UUID offenceId = randomUUID();
        final UUID newAccountCorrelationId = randomUUID();

        final HearingFinancialResultRequest request = hearingFinancialResultRequest()
                .withAccountCorrelationId(newAccountCorrelationId)
                .withProsecutionCaseReferences(List.of("AAC15170749"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(offenceId)
                                .withIsFinancial(true)
                                .build()))
                .build();

        final MarkedAggregateSendEmailWhenAccountReceived notification = markedAggregateSendEmailEventBuilder("nces@justice.gov.uk", new LinkedList<>())
                .buildMarkedAggregateWithoutOldsForSpecificCorrelationIdWithEmail(
                        request,
                        "APPEAL DISMISSED",
                        List.of(),
                        "nces@justice.gov.uk",
                        null,
                        null,
                        null,
                        List.of(),
                        null,
                        null,
                        null,
                        emptyMap());

        assertThat(notification.getGobAccountNumber(), is(nullValue()));
    }
}
