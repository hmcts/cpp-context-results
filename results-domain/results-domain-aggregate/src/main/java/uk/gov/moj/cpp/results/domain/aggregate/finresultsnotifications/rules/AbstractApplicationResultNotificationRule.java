package uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.gov.moj.cpp.results.domain.aggregate.ImpositionOffenceDetailsBuilder.buildImpositionOffenceDetailsFromAggregate;

import uk.gov.justice.hearing.courts.HearingFinancialResultRequest;
import uk.gov.justice.hearing.courts.OffenceResultsDetails;
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.ResultNotificationRule;
import uk.gov.moj.cpp.results.domain.event.ImpositionOffenceDetails;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Base class for application result notification rules.
 */
abstract class AbstractApplicationResultNotificationRule implements ResultNotificationRule {

    protected HearingFinancialResultRequest getFilteredApplicationResults(HearingFinancialResultRequest request) {
        final HearingFinancialResultRequest filtered = HearingFinancialResultRequest.hearingFinancialResultRequest()
                .withValuesFrom(request)
                .withOffenceResults(new ArrayList<>(request.getOffenceResults())).build();

        filtered.getOffenceResults().removeIf(result -> isNull(result.getApplicationType()));
        return filtered;
    }

    protected List<ImpositionOffenceDetails> getApplicationImpositionOffenceDetails(final HearingFinancialResultRequest hearingFinancialResultRequest,
                                                                                    final Map<UUID, String> offenceDateMap,
                                                                                    final Map<UUID, OffenceResultsDetails> offenceResultsDetails,
                                                                                    final Map<UUID, OffenceResultsDetails> applicationOffenceResultsDetails) {
        List<ImpositionOffenceDetails> impositionOffenceDetailsForApplication = hearingFinancialResultRequest.getOffenceResults().stream()
                .filter(result -> nonNull(result.getApplicationType()))
                .map(offenceFromRequest -> offenceResultsDetails.get(offenceFromRequest.getOffenceId()))
                .filter(Objects::nonNull)
                .filter(OffenceResultsDetails::getIsFinancial)
                .map(offenceResults -> buildImpositionOffenceDetailsFromAggregate(offenceResults, offenceDateMap)).distinct()
                .toList();

        if (impositionOffenceDetailsForApplication.isEmpty()) {
            impositionOffenceDetailsForApplication = hearingFinancialResultRequest.getOffenceResults().stream()
                    .filter(result -> nonNull(result.getApplicationType()))
                    .map(offenceFromRequest -> applicationOffenceResultsDetails.get(offenceFromRequest.getOffenceId()))
                    .filter(Objects::nonNull)
                    .filter(OffenceResultsDetails::getIsParentFlag)
                    .filter(OffenceResultsDetails::getIsFinancial)
                    .map(offenceResults -> buildImpositionOffenceDetailsFromAggregate(offenceResults, offenceDateMap)).distinct()
                    .toList();
        }
        return impositionOffenceDetailsForApplication;
    }
}
