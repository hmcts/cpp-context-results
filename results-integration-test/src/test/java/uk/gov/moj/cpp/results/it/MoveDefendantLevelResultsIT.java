package uk.gov.moj.cpp.results.it;

import static java.time.LocalDate.of;
import static uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.closeMessageConsumers;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.createMessageConsumers;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.getSummariesByDate;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.hearingResultsHaveBeenSharedV2;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.verifyPublicEventPoliceResultsGenerated;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.whenPrisonAdminTriesToViewResultsForThePerson;
import static uk.gov.moj.cpp.results.it.steps.data.factory.HearingResultDataFactory.getUserId;
import static uk.gov.moj.cpp.results.it.utils.ReferenceDataServiceStub.stubBailStatuses;
import static uk.gov.moj.cpp.results.it.utils.ReferenceDataServiceStub.stubCountryNationalities;
import static uk.gov.moj.cpp.results.it.utils.ReferenceDataServiceStub.stubGetOrganisationUnit;
import static uk.gov.moj.cpp.results.it.utils.ReferenceDataServiceStub.stubJudicialResults;
import static uk.gov.moj.cpp.results.it.utils.ReferenceDataServiceStub.stubModeOfTrialReasons;
import static uk.gov.moj.cpp.results.it.utils.ReferenceDataServiceStub.stubSpiOutFlag;
import static uk.gov.moj.cpp.results.it.utils.WireMockStubUtils.setupUserAsPrisonAdminGroup;
import static uk.gov.moj.cpp.results.test.TestTemplates.basicShareResultsV2TemplateWithTwoOffences;

import uk.gov.moj.cpp.domains.results.shareresults.PublicHearingResulted;

import java.time.LocalDate;

import javax.jms.JMSException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MoveDefendantLevelResultsIT {

    @AfterEach
    public void teardown() throws JMSException {
        closeMessageConsumers();
    }

    @BeforeEach
    public void setUp() {
        setupUserAsPrisonAdminGroup(getUserId());
        stubCountryNationalities();
        stubGetOrganisationUnit();
        stubJudicialResults();
        stubSpiOutFlag(true, false);
        stubBailStatuses();
        stubModeOfTrialReasons();
        createMessageConsumers();
    }

    @Test
    public void shouldMoveJudicialResultsToOffenceLevelAndRaisePublicEvent() throws JMSException {

        final PublicHearingResulted resultsMessage = basicShareResultsV2TemplateWithTwoOffences(MAGISTRATES);

        hearingResultsHaveBeenSharedV2(resultsMessage);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());

        LocalDate startDate = resultsMessage.getHearing().getHearingDays().get(0).getSittingDay().toLocalDate();
        startDate = of(startDate.getYear(), startDate.getMonth(), startDate.getDayOfMonth() - 1);

        getSummariesByDate(startDate);
        verifyPublicEventPoliceResultsGenerated();
    }
}
