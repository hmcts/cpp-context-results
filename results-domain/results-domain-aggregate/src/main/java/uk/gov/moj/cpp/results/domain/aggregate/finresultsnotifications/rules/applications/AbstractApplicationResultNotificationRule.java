package uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.applications;

import static java.util.Comparator.comparing;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.gov.moj.cpp.results.domain.aggregate.ImpositionOffenceDetailsBuilder.buildImpositionOffenceDetailsFromAggregate;

import uk.gov.justice.hearing.courts.HearingFinancialResultRequest;
import uk.gov.justice.hearing.courts.OffenceResults;
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
public abstract class AbstractApplicationResultNotificationRule implements ResultNotificationRule {

    protected HearingFinancialResultRequest getFilteredApplicationResults(HearingFinancialResultRequest request) {
        final HearingFinancialResultRequest filtered = HearingFinancialResultRequest.hearingFinancialResultRequest()
                .withValuesFrom(request)
                .withOffenceResults(new ArrayList<>(request.getOffenceResults())).build();

        filtered.getOffenceResults().removeIf(result -> isNull(result.getApplicationType()));
        return filtered;
    }

    protected List<ImpositionOffenceDetails> getApplicationImpositionOffenceDetails(final HearingFinancialResultRequest hearingFinancialResultRequest,
                                                                                    final Map<UUID, String> offenceDateMap,
                                                                                    final Map<UUID, OffenceResultsDetails> caseOffenceResultsDetails,
                                                                                    final Map<UUID, List<OffenceResultsDetails>> prevApplicationOffenceResultsMap) {

        final UUID currentApplicationId = hearingFinancialResultRequest.getOffenceResults().stream().map(OffenceResults::getApplicationId).filter(Objects::nonNull).findFirst().orElse(null);

        return hearingFinancialResultRequest.getOffenceResults().stream()
                .filter(result -> nonNull(result.getApplicationType()))
                .map(offenceFromRequest -> getOldOffenceResultsDetails(offenceFromRequest.getOffenceId(), caseOffenceResultsDetails, prevApplicationOffenceResultsMap, currentApplicationId))
                .filter(Objects::nonNull)
                .filter(OffenceResultsDetails::getIsFinancial)
                .map(offenceResults -> buildImpositionOffenceDetailsFromAggregate(offenceResults, offenceDateMap)).distinct()
                .toList();
    }

    protected OffenceResultsDetails getOldOffenceResultsDetails(final UUID offenceId, final Map<UUID, OffenceResultsDetails> caseOffenceResultsDetails,
                                                              final Map<UUID, List<OffenceResultsDetails>> prevApplicationOffenceResultsMap, final UUID currentApplicationId) {
        final List<OffenceResultsDetails> allOffenceResults = new ArrayList<>();

        //add case level results for that offence
        if (caseOffenceResultsDetails.containsKey(offenceId)) {
            allOffenceResults.add(caseOffenceResultsDetails.get(offenceId));
        }

        //add all previous application level results for that offence
        if (nonNull(currentApplicationId)) {
            prevApplicationOffenceResultsMap.forEach((applicationId, offenceResults) -> {
                if (!currentApplicationId.equals(applicationId)) {
                    allOffenceResults.addAll(offenceResults.stream()
                            .filter(ord -> offenceId.equals(ord.getOffenceId()))
                            .toList());
                }
            });
        }

        allOffenceResults.sort(comparing(OffenceResultsDetails::getCreatedTime).reversed());

        return !allOffenceResults.isEmpty() ? allOffenceResults.get(0) : null;
    }
}
