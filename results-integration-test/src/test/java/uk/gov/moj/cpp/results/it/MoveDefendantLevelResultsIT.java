package uk.gov.moj.cpp.results.it;

import static java.time.LocalDate.of;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.closeMessageConsumers;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.createMessageConsumers;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.getSummariesByDate;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.hearingResultsHaveBeenShared;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.verifyInPublicTopic;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.verifyPrivateEventsWithPoliceResultGenerated;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.whenPrisonAdminTriesToViewResultsForThePerson;
import static uk.gov.moj.cpp.results.it.steps.data.factory.HearingResultDataFactory.getUserId;
import static uk.gov.moj.cpp.results.it.utils.ReferenceDataServiceStub.stubBailStatuses;
import static uk.gov.moj.cpp.results.it.utils.ReferenceDataServiceStub.stubCountryNationalities;
import static uk.gov.moj.cpp.results.it.utils.ReferenceDataServiceStub.stubGetOrgainsationUnit;
import static uk.gov.moj.cpp.results.it.utils.ReferenceDataServiceStub.stubJudicialResults;
import static uk.gov.moj.cpp.results.it.utils.ReferenceDataServiceStub.stubModeOfTrialReasons;
import static uk.gov.moj.cpp.results.it.utils.ReferenceDataServiceStub.stubSpiOutFlag;
import static uk.gov.moj.cpp.results.it.utils.WireMockStubUtils.setupUserAsPrisonAdminGroup;
import static uk.gov.moj.cpp.results.test.TestTemplates.sharedResultTemplateWithTwoOffences;

import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.moj.cpp.domains.results.shareresults.PublicHearingResulted;

import java.time.LocalDate;

import javax.jms.JMSException;

import org.junit.After;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MoveDefendantLevelResultsIT {

    @After
    public void teardown() throws JMSException {
        closeMessageConsumers();
    }

    @BeforeEach
    public void setUp() {
        setupUserAsPrisonAdminGroup(getUserId());
        stubCountryNationalities();
        stubGetOrgainsationUnit();
        stubJudicialResults();
        stubSpiOutFlag(true, false);
        stubBailStatuses();
        stubModeOfTrialReasons();
        createMessageConsumers();
    }

    @Test
    public void shouldMoveJudicialResultsToOffenceLevelAndRaisePublicEvent() throws JMSException {

        final PublicHearingResulted resultsMessage = sharedResultTemplateWithTwoOffences(JurisdictionType.MAGISTRATES);

        hearingResultsHaveBeenShared(resultsMessage);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());

        LocalDate startDate = resultsMessage.getHearing().getHearingDays().get(0).getSittingDay().toLocalDate();
        startDate = of(startDate.getYear(), startDate.getMonth(), startDate.getDayOfMonth() - 1);

        getSummariesByDate(startDate);
        verifyPrivateEventsWithPoliceResultGenerated();
        verifyInPublicTopic();

    }
}
