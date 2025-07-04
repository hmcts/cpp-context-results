package uk.gov.moj.cpp.results.domain.aggregate;

import uk.gov.justice.hearing.courts.OffenceResults;
import uk.gov.justice.hearing.courts.OffenceResultsDetails;
import uk.gov.moj.cpp.results.domain.event.ImpositionOffenceDetails;

import java.util.Map;
import java.util.UUID;

public class ImpositionOffenceDetailsBuilder {
    private ImpositionOffenceDetailsBuilder() {
    }

    public static ImpositionOffenceDetails buildImpositionOffenceDetailsFromRequest(final OffenceResults offencesFromRequest, final Map<UUID, String> offenceDateMap) {
        return ImpositionOffenceDetails.impositionOffenceDetails()
                .withOffenceId(offencesFromRequest.getOffenceId())
                .withDetails(offencesFromRequest.getImpositionOffenceDetails())
                .withOffenceDate(offenceDateMap.get(offencesFromRequest.getOffenceId()))
                .withTitle(offencesFromRequest.getOffenceTitle())
                .build();
    }

    public static ImpositionOffenceDetails buildImpositionOffenceDetailsFromAggregate(final OffenceResultsDetails offencesFromAggregate, final Map<UUID, String> offenceDateMap) {
        return ImpositionOffenceDetails.impositionOffenceDetails()
                .withOffenceId(offencesFromAggregate.getOffenceId())
                .withDetails(offencesFromAggregate.getImpositionOffenceDetails())
                .withOffenceDate(offenceDateMap.get(offencesFromAggregate.getOffenceId()))
                .withTitle(offencesFromAggregate.getOffenceTitle())
                .build();
    }
}
