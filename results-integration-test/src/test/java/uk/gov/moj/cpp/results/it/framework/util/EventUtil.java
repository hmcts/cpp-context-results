package uk.gov.moj.cpp.results.it.framework.util;

import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.hearingResultsHaveBeenShared;
import static uk.gov.moj.cpp.results.it.steps.data.factory.HearingResultDataFactory.getUserId;
import static uk.gov.moj.cpp.results.it.utils.AuthorisationServiceStub.stubEnableAllCapabilities;
import static uk.gov.moj.cpp.results.it.utils.WireMockStubUtils.setupUserAsPrisonAdminGroup;
import static uk.gov.moj.cpp.results.test.TestTemplates.basicShareResultsTemplate;

import uk.gov.moj.cpp.domains.results.shareresults.PublicHearingResulted;

public class EventUtil {

    public static void shareHearingResults(int numberOfResults) {
        setupUserAsPrisonAdminGroup(getUserId());
        stubEnableAllCapabilities();

        for (int i = 0; i < numberOfResults; i++) {
            PublicHearingResulted resultsMessage = basicShareResultsTemplate();
            hearingResultsHaveBeenShared(resultsMessage);
        }
    }
}
