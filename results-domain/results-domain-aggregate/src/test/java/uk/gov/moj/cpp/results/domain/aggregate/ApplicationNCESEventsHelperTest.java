package uk.gov.moj.cpp.results.domain.aggregate;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static uk.gov.justice.hearing.courts.OffenceResults.offenceResults;
import static uk.gov.justice.hearing.courts.OffenceResultsDetails.offenceResultsDetails;

import uk.gov.justice.hearing.courts.OffenceResults;
import uk.gov.justice.hearing.courts.OffenceResultsDetails;
import uk.gov.moj.cpp.results.domain.event.NewApplicationResults;
import uk.gov.moj.cpp.results.domain.event.OriginalApplicationResults;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

/**
 * Test class for ApplicationNCESEventsHelper.
 * Tests the helper methods for building NCES events for applications.
 */
class ApplicationNCESEventsHelperTest {

    @Test
    void shouldBuildOriginalApplicationResultsFromAggregateWithSingleResult() {
        // Given
        final String applicationTitle = "Test Application";
        final String resultType = "GRANTED";
        final UUID applicationId = randomUUID();
        final UUID offenceId = randomUUID();

        List<OffenceResultsDetails> applicationResultsDetails = List.of(
                offenceResultsDetails()
                        .withApplicationId(applicationId)
                        .withApplicationTitle(applicationTitle)
                        .withApplicationResultType(resultType)
                        .withOffenceId(offenceId)
                        .build()
        );

        // When
        OriginalApplicationResults result = ApplicationNCESEventsHelper.buildOriginalApplicationResultsFromAggregate(applicationResultsDetails);

        // Then
        assertThat("Application title should match", result.getApplicationTitle(), is(applicationTitle));
        assertThat("Should have one result type", result.getApplicationResult(), hasSize(1));
        assertThat("Result type should match", result.getApplicationResult(), contains(resultType));
    }

    @Test
    void shouldBuildOriginalApplicationResultsFromAggregateWithMultipleResults() {
        // Given
        final String applicationTitle = "Test Application";
        final String resultType1 = "GRANTED";
        final String resultType2 = "DENIED";
        final String resultType3 = "ADJOURNED";
        final UUID applicationId = randomUUID();
        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID offenceId3 = randomUUID();

        List<OffenceResultsDetails> applicationResultsDetails = List.of(
                offenceResultsDetails()
                        .withApplicationId(applicationId)
                        .withApplicationTitle(applicationTitle)
                        .withApplicationResultType(resultType1)
                        .withOffenceId(offenceId1)
                        .build(),
                offenceResultsDetails()
                        .withApplicationId(applicationId)
                        .withApplicationTitle(applicationTitle)
                        .withApplicationResultType(resultType2)
                        .withOffenceId(offenceId2)
                        .build(),
                offenceResultsDetails()
                        .withApplicationId(applicationId)
                        .withApplicationTitle(applicationTitle)
                        .withApplicationResultType(resultType3)
                        .withOffenceId(offenceId3)
                        .build()
        );

        // When
        OriginalApplicationResults result = ApplicationNCESEventsHelper.buildOriginalApplicationResultsFromAggregate(applicationResultsDetails);

        // Then
        assertThat("Application title should match", result.getApplicationTitle(), is(applicationTitle));
        assertThat("Should have three result types", result.getApplicationResult(), hasSize(3));
        assertThat("Result types should contain all values", result.getApplicationResult(), contains(resultType1, resultType2, resultType3));
    }

    @Test
    void shouldBuildNewApplicationResultsFromTrackRequestWithValidData() {
        // Given
        final String applicationTitle = "Test Application";
        final String resultType = "GRANTED";
        final UUID applicationId = randomUUID();
        final UUID offenceId = randomUUID();

        List<OffenceResults> applicationResultsDetails = List.of(
                offenceResults()
                        .withApplicationId(applicationId)
                        .withApplicationTitle(applicationTitle)
                        .withApplicationResultType(resultType)
                        .withOffenceId(offenceId)
                        .build()
        );

        // When
        NewApplicationResults result = ApplicationNCESEventsHelper.buildNewApplicationResultsFromTrackRequest(applicationResultsDetails);

        // Then
        assertThat("Application title should match", result.getApplicationTitle(), is(applicationTitle));
        assertThat("Should have one result type", result.getApplicationResult(), hasSize(1));
        assertThat("Result type should match", result.getApplicationResult(), contains(resultType));
    }

    @Test
    void shouldBuildNewApplicationResultsFromTrackRequestWithMultipleResults() {
        // Given
        final String applicationTitle = "Test Application";
        final String resultType1 = "GRANTED";
        final String resultType2 = "DENIED";
        final UUID applicationId = randomUUID();
        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();

        List<OffenceResults> applicationResultsDetails = List.of(
                offenceResults()
                        .withApplicationId(applicationId)
                        .withApplicationTitle(applicationTitle)
                        .withApplicationResultType(resultType1)
                        .withOffenceId(offenceId1)
                        .build(),
                offenceResults()
                        .withApplicationId(applicationId)
                        .withApplicationTitle(applicationTitle)
                        .withApplicationResultType(resultType2)
                        .withOffenceId(offenceId2)
                        .build()
        );

        // When
        NewApplicationResults result = ApplicationNCESEventsHelper.buildNewApplicationResultsFromTrackRequest(applicationResultsDetails);

        // Then
        assertThat("Application title should match", result.getApplicationTitle(), is(applicationTitle));
        assertThat("Should have two result types", result.getApplicationResult(), hasSize(2));
        assertThat("Result types should contain both values", result.getApplicationResult(), contains(resultType1, resultType2));
    }

    @Test
    void shouldBuildApplicationResultsFromTrackRequestWithMultipleResults() {
        // Given
        final String applicationTitle = "Test Application";
        final String resultType1 = "GRANTED";
        final String resultType2 = "DENIED";
        final UUID applicationId = randomUUID();
        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();

        List<OffenceResults> applicationResultsDetails = List.of(
                offenceResults()
                        .withApplicationId(applicationId)
                        .withApplicationTitle(applicationTitle)
                        .withApplicationResultType(resultType1)
                        .withOffenceId(offenceId1)
                        .build(),
                offenceResults()
                        .withApplicationId(applicationId)
                        .withApplicationTitle(applicationTitle)
                        .withApplicationResultType(resultType2)
                        .withOffenceId(offenceId2)
                        .build()
        );

        // When
        OriginalApplicationResults result = ApplicationNCESEventsHelper.buildApplicationResultsFromTrackRequest(applicationResultsDetails);

        // Then
        assertThat("Application title should match", result.getApplicationTitle(), is(applicationTitle));
        assertThat("Should have two result types", result.getApplicationResult(), hasSize(2));
        assertThat("Result types should contain both values", result.getApplicationResult(), contains(resultType1, resultType2));
    }

    @Test
    void shouldHandleMixedValidAndInvalidDataInNewApplicationResults() {
        // Given
        final String applicationTitle = "Test Application";
        final String resultType1 = "GRANTED";
        final String resultType2 = "DENIED";
        final UUID applicationId = randomUUID();
        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID offenceId3 = randomUUID();

        List<OffenceResults> applicationResultsDetails = List.of(
                offenceResults()
                        .withApplicationId(applicationId)
                        .withApplicationTitle(applicationTitle)
                        .withApplicationResultType(resultType1)
                        .withOffenceId(offenceId1)
                        .build(),
                offenceResults()
                        .withApplicationId(null) // Invalid - null application ID
                        .withApplicationTitle(applicationTitle)
                        .withApplicationResultType("INVALID")
                        .withOffenceId(offenceId2)
                        .build(),
                offenceResults()
                        .withApplicationId(applicationId)
                        .withApplicationTitle(applicationTitle)
                        .withApplicationResultType(resultType2)
                        .withOffenceId(offenceId3)
                        .build()
        );

        // When
        NewApplicationResults result = ApplicationNCESEventsHelper.buildNewApplicationResultsFromTrackRequest(applicationResultsDetails);

        // Then
        assertThat("Application title should match", result.getApplicationTitle(), is(applicationTitle));
        assertThat("Should have two valid result types", result.getApplicationResult(), hasSize(2));
        assertThat("Result types should contain valid values", result.getApplicationResult(), contains(resultType1, resultType2));
    }
}

