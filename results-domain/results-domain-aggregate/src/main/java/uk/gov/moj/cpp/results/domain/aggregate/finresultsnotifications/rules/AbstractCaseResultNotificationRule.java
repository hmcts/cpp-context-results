package uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules;

import static java.util.Objects.nonNull;

import uk.gov.justice.hearing.courts.HearingFinancialResultRequest;
import uk.gov.justice.hearing.courts.OffenceResults;
import uk.gov.justice.hearing.courts.OffenceResultsDetails;
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.ResultNotificationRule;
import uk.gov.moj.cpp.results.domain.event.ImpositionOffenceDetails;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

/**
 * Base class for case result notification rules.
 */
abstract class AbstractCaseResultNotificationRule implements ResultNotificationRule {

    protected ImpositionOffenceDetails buildImpositionOffenceDetailsFromRequest(final OffenceResults offencesFromRequest, final Map<UUID, String> offenceDateMap) {
        return ImpositionOffenceDetails.impositionOffenceDetails()
                .withDetails(offencesFromRequest.getImpositionOffenceDetails())
                .withOffenceDate(offenceDateMap.get(offencesFromRequest.getOffenceId()))
                .withOffenceId(offencesFromRequest.getOffenceId())
                .withTitle(offencesFromRequest.getOffenceTitle())
                .build();
    }

    protected ImpositionOffenceDetails buildImpositionOffenceDetailsFromAggregate(final OffenceResultsDetails offencesFromAggregate, final Map<UUID, String> offenceDateMap) {
        return ImpositionOffenceDetails.impositionOffenceDetails()
                .withDetails(offencesFromAggregate.getImpositionOffenceDetails())
                .withOffenceDate(offenceDateMap.get(offencesFromAggregate.getOffenceId()))
                .withTitle(offencesFromAggregate.getOffenceTitle())
                .build();
    }

    protected HearingFinancialResultRequest getFilteredCaseResults(HearingFinancialResultRequest request) {
        final HearingFinancialResultRequest filtered = HearingFinancialResultRequest.hearingFinancialResultRequest()
                .withValuesFrom(request)
                .withOffenceResults(new ArrayList<>(request.getOffenceResults())).build();

        filtered.getOffenceResults().removeIf(result -> nonNull(result.getApplicationType()));
        return filtered;
    }
}
