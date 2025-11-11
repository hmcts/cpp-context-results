package uk.gov.moj.cpp.results.domain.aggregate;


import uk.gov.moj.cpp.results.domain.event.ApplicationResultDetails;
import uk.gov.moj.cpp.results.domain.event.CaseResultDetails;

import java.util.List;
import static java.util.Objects.nonNull;

public class ResultReshareHelper {

    public static boolean hasResults(final CaseResultDetails caseResultAmendmentDetails) {

        if (nonNull(caseResultAmendmentDetails.getDefendantResultDetails())) {
            final boolean hasDefendantResult = caseResultAmendmentDetails.getDefendantResultDetails().stream()
                    .anyMatch(d -> nonNull(d.getOffenceResultDetails()) && d.getOffenceResultDetails().stream().anyMatch(
                            o -> nonNull(o.getJudicialResultDetails()) && !o.getJudicialResultDetails().isEmpty()
                    ));

            if (hasDefendantResult) {
                return true;
            }
        }

        final List<ApplicationResultDetails> applicationResultDetailsList = caseResultAmendmentDetails.getApplicationResultDetails();

        if (nonNull(applicationResultDetailsList)) {
            final boolean hasCourtOrderOffenceResult = applicationResultDetailsList.stream()
                    .anyMatch(app -> nonNull(app.getOffenceResultDetails()) && app.getOffenceResultDetails().stream().anyMatch(
                            r -> nonNull(r.getJudicialResultDetails()) && !r.getJudicialResultDetails().isEmpty()
                    ));

            if (hasCourtOrderOffenceResult) {
                return true;
            }

            final boolean hasCourtApplicationCasesResults = applicationResultDetailsList.stream()
                    .anyMatch(app -> nonNull(app.getApplicationCasesResultDetails()) && app.getApplicationCasesResultDetails().stream().anyMatch(
                            r -> nonNull(r.getJudicialResultDetails()) && !r.getJudicialResultDetails().isEmpty()
                    ));

            if (hasCourtApplicationCasesResults) {
                return true;
            }

            final boolean hasApplicationResults = applicationResultDetailsList.stream()
                    .anyMatch(app -> nonNull(app.getJudicialResultDetails()) && !app.getJudicialResultDetails().isEmpty());

            if (hasApplicationResults) {
                return true;
            }
        }

        return false;
    }

}
