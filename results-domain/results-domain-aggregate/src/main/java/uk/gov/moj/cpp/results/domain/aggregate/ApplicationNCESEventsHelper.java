package uk.gov.moj.cpp.results.domain.aggregate;

import static java.util.Objects.nonNull;

import uk.gov.justice.hearing.courts.OffenceResults;
import uk.gov.justice.hearing.courts.OffenceResultsDetails;
import uk.gov.moj.cpp.results.domain.event.NewApplicationResults;
import uk.gov.moj.cpp.results.domain.event.OriginalApplicationResults;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Helper class for processing NCES events for applications.
 * <p>
 * This class provides methods for building and processing NCES events for applications based on the incoming hearing financial result request.
 * It contains methods for requesting NCES email notifications for rejected, granted, and updated applications, as well as for application amendment events.
 */
public class ApplicationNCESEventsHelper {

    public static OriginalApplicationResults buildOriginalApplicationResultsFromAggregate(final List<OffenceResultsDetails> applicationResultsDetails) {
        List<String> applicationResult = new ArrayList<>();
        applicationResultsDetails.forEach(applicationResultDetail -> {
            if (!applicationResult.contains(applicationResultDetail.getApplicationResultType())) {
                applicationResult.add(applicationResultDetail.getApplicationResultType());
            }
        });
        return OriginalApplicationResults.originalApplicationResults()
                .withApplicationTitle(applicationResultsDetails.get(0).getApplicationTitle())
                .withApplicationResult(applicationResult)
                .build();
    }

    public static NewApplicationResults buildNewApplicationResultsFromTrackRequest(final List<OffenceResults> applicationResultsDetails) {
        List<String> applicationResults = new ArrayList<>();
        NewApplicationResults.Builder newApplicationResults = new NewApplicationResults.Builder();
        applicationResultsDetails.forEach(applicationResultDetail -> {
            if (!applicationResults.contains(applicationResultDetail.getApplicationResultType())
                    && nonNull(applicationResultDetail.getApplicationId())
                    && nonNull(applicationResultDetail.getApplicationTitle())) {
                applicationResults.add(applicationResultDetail.getApplicationResultType());
                newApplicationResults.withApplicationTitle(applicationResultDetail.getApplicationTitle());
            }
        });
        return newApplicationResults.withApplicationResult(applicationResults).build();
    }

    public static OriginalApplicationResults buildApplicationResultsFromTrackRequest(final List<OffenceResults> applicationResultsDetails) {
        List<String> applicationResults = new ArrayList<>();
        OriginalApplicationResults.Builder originalApplicationResults = new OriginalApplicationResults.Builder();
        applicationResultsDetails.forEach(applicationResultDetail -> {
            if (!applicationResults.contains(applicationResultDetail.getApplicationResultType())
                    && nonNull(applicationResultDetail.getApplicationId())
                    && nonNull(applicationResultDetail.getApplicationTitle())) {
                applicationResults.add(applicationResultDetail.getApplicationResultType());
                originalApplicationResults.withApplicationTitle(applicationResultDetail.getApplicationTitle());
            }
        });
        return originalApplicationResults.withApplicationResult(applicationResults).build();
    }
}
