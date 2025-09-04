package uk.gov.moj.cpp.results.domain.aggregate.utils;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.justice.hearing.courts.OffenceResults.offenceResults;
import static uk.gov.justice.hearing.courts.OffenceResultsDetails.offenceResultsDetails;
import static uk.gov.moj.cpp.results.domain.aggregate.utils.OffenceResultsResolver.getNewOffenceResultsCaseAmendment;
import static uk.gov.moj.cpp.results.domain.aggregate.utils.OffenceResultsResolver.getOriginalOffenceResultsApplication;

import uk.gov.justice.hearing.courts.OffenceResults;
import uk.gov.justice.hearing.courts.OffenceResultsDetails;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class OffenceResultsResolverTest {

    @ParameterizedTest(name = "{index} => {0}")
    @MethodSource("newApplicationOffences")
    public void givenOffencesOnCaseAndApplication1_whenNewApplicationReceived_shouldResolveOriginalOffenceResults(final String name, final Map<UUID, OffenceResultsDetails> previousCaseOffenceResultsMap,
                                                                                                                  final Map<UUID, List<OffenceResultsDetails>> previousApplicationOffenceResultsMap,
                                                                                                                  final List<OffenceResults> newOffenceResults, final List<UUID> expectedOffenceIds) {

        final List<OffenceResultsDetails> originalOffenceResults = getOriginalOffenceResultsApplication(previousCaseOffenceResultsMap, previousApplicationOffenceResultsMap, newOffenceResults);


        assertThat(originalOffenceResults.size(), is(expectedOffenceIds.size()));
        assertThat(originalOffenceResults.stream().map(OffenceResultsDetails::getOffenceId).collect(Collectors.toList()), is(expectedOffenceIds));
    }

    @Test
    public void givenCaseOffencesNonFine_whenAmendedWithNonFine_shouldResolveOriginalOffenceResults() {
        final UUID offenceId1 = randomUUID();
        final Map<UUID, OffenceResultsDetails> previousCaseOffenceResultsMap = Map.of(offenceId1, offenceResultsDetails().withOffenceId(offenceId1)
                .withIsFinancial(FALSE).withCreatedTime(now().minusHours(3)).build());

        final List<OffenceResults> newOffenceResults = List.of(OffenceResults.offenceResults().withOffenceId(offenceId1).withIsFinancial(FALSE).build());

        final List<OffenceResults> newOffenceResultsCaseAmendment = getNewOffenceResultsCaseAmendment(newOffenceResults, previousCaseOffenceResultsMap);

        assertThat(newOffenceResultsCaseAmendment.isEmpty(), is(true));
    }

    @Test
    public void givenCaseOffencesFine_whenAmendedWithFine_shouldResolveOriginalOffenceResults() {
        final UUID offenceId1 = randomUUID();
        final Map<UUID, OffenceResultsDetails> previousCaseOffenceResultsMap = Map.of(offenceId1, offenceResultsDetails().withOffenceId(offenceId1)
                .withIsFinancial(TRUE).withCreatedTime(now().minusHours(3)).build());

        final List<OffenceResults> newOffenceResults = List.of(OffenceResults.offenceResults().withOffenceId(offenceId1).withIsFinancial(TRUE).build());

        final List<OffenceResults> newOffenceResultsCaseAmendment = getNewOffenceResultsCaseAmendment(newOffenceResults, previousCaseOffenceResultsMap);

        assertThat(newOffenceResultsCaseAmendment.size(), is(1));
    }


    public static Stream<Arguments> newApplicationOffences() {
        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID newApplicationId = randomUUID();

        return Stream.of(

                Arguments.of(
                        "Case-Fine-Fine > App1 Fine-Fine > App2 Fine-Fine",
                        Map.of(offenceId1, offenceResultsDetails().withOffenceId(offenceId1).withIsFinancial(TRUE)
                                        .withCreatedTime(now().minusHours(3)).build(),
                                offenceId2, offenceResultsDetails().withOffenceId(offenceId2).withIsFinancial(TRUE)
                                        .withCreatedTime(now().minusHours(3)).build()),
                        Map.of(randomUUID(), List.of(offenceResultsDetails().withOffenceId(offenceId1).withIsFinancial(TRUE)
                                .withCreatedTime(now().minusHours(2)).build(), offenceResultsDetails().withOffenceId(offenceId2).withIsFinancial(TRUE)
                                .withCreatedTime(now().minusHours(2)).build())),

                        List.of(offenceResults().withOffenceId(offenceId1).withApplicationId(newApplicationId).withApplicationType("type").withIsFinancial(TRUE).build(),
                                offenceResults().withOffenceId(offenceId2).withApplicationId(newApplicationId).withApplicationType("type").withIsFinancial(TRUE).build()),

                        List.of(offenceId1, offenceId2)
                ),
                Arguments.of(
                        "Case-Fine-Fine > App1 Fine-Fine > App2 Fine-Non-fine",
                        Map.of(offenceId1, offenceResultsDetails().withOffenceId(offenceId1).withIsFinancial(TRUE)
                                        .withCreatedTime(now().minusHours(3)).build(),
                                offenceId2, offenceResultsDetails().withOffenceId(offenceId2).withIsFinancial(TRUE)
                                        .withCreatedTime(now().minusHours(3)).build()),
                        Map.of(randomUUID(), List.of(offenceResultsDetails().withOffenceId(offenceId1).withIsFinancial(TRUE)
                                .withCreatedTime(now().minusHours(2)).build(), offenceResultsDetails().withOffenceId(offenceId2).withIsFinancial(TRUE)
                                .withCreatedTime(now().minusHours(2)).build())),

                        List.of(offenceResults().withOffenceId(offenceId1).withApplicationId(newApplicationId).withApplicationType("type").withIsFinancial(TRUE).build(),
                                offenceResults().withOffenceId(offenceId2).withApplicationId(newApplicationId).withApplicationType("type").withIsFinancial(FALSE).build()),

                        List.of(offenceId1, offenceId2)
                ),
                Arguments.of(
                        "Case-Fine-Fine > App1 Non-Fine-Fine > App2 Fine-Fine",
                        Map.of(offenceId1, offenceResultsDetails().withOffenceId(offenceId1).withIsFinancial(TRUE)
                                        .withCreatedTime(now().minusHours(3)).build(),
                                offenceId2, offenceResultsDetails().withOffenceId(offenceId2).withIsFinancial(TRUE)
                                        .withCreatedTime(now().minusHours(3)).build()),
                        Map.of(randomUUID(), List.of(offenceResultsDetails().withOffenceId(offenceId1).withIsFinancial(FALSE)
                                .withCreatedTime(now().minusHours(2)).build(), offenceResultsDetails().withOffenceId(offenceId2).withIsFinancial(FALSE)
                                .withCreatedTime(now().minusHours(2)).build())),

                        List.of(offenceResults().withOffenceId(offenceId1).withApplicationId(newApplicationId).withApplicationType("type").withIsFinancial(TRUE).build(),
                                offenceResults().withOffenceId(offenceId2).withApplicationId(newApplicationId).withApplicationType("type").withIsFinancial(TRUE).build()),

                        List.of(offenceId1, offenceId2)
                ),
                Arguments.of(
                        "Case-Fine-Fine > App1 Non-Fine-Fine > App2 Non-Fine-Fine",
                        Map.of(offenceId1, offenceResultsDetails().withOffenceId(offenceId1).withIsFinancial(TRUE)
                                        .withCreatedTime(now().minusHours(3)).build(),
                                offenceId2, offenceResultsDetails().withOffenceId(offenceId2).withIsFinancial(TRUE)
                                        .withCreatedTime(now().minusHours(3)).build()),
                        Map.of(randomUUID(), List.of(offenceResultsDetails().withOffenceId(offenceId1).withIsFinancial(FALSE)
                                .withCreatedTime(now().minusHours(2)).build(), offenceResultsDetails().withOffenceId(offenceId2).withIsFinancial(FALSE)
                                .withCreatedTime(now().minusHours(2)).build())),

                        List.of(offenceResults().withOffenceId(offenceId1).withApplicationId(newApplicationId).withApplicationType("type").withIsFinancial(FALSE).build(),
                                offenceResults().withOffenceId(offenceId2).withApplicationId(newApplicationId).withApplicationType("type").withIsFinancial(TRUE).build()),

                        List.of(offenceId2)
                )
        );
    }

}