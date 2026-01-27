package uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.cases.result;


import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.gov.justice.hearing.courts.HearingFinancialResultRequest.hearingFinancialResultRequest;
import static uk.gov.justice.hearing.courts.OffenceResults.offenceResults;
import static uk.gov.justice.hearing.courts.OffenceResultsDetails.offenceResultsDetails;
import static uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.ResultNotificationRuleInputBuilder.resultNotificationRuleInputBuilder;
import static uk.gov.moj.cpp.results.domain.aggregate.utils.CorrelationItem.correlationItem;
import static uk.gov.moj.cpp.results.domain.aggregate.utils.ResultCategoryType.FINAL;
import static uk.gov.moj.cpp.results.domain.aggregate.utils.ResultCategoryType.INTERMEDIARY;

import uk.gov.justice.hearing.courts.HearingFinancialResultRequest;
import uk.gov.justice.hearing.courts.OffenceResultsDetails;
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.ResultNotificationRule;
import uk.gov.moj.cpp.results.domain.aggregate.utils.ApplicationMetadata;
import uk.gov.moj.cpp.results.domain.event.MarkedAggregateSendEmailWhenAccountReceived;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class SjpReferredReopenApplicationAcceptedNotificationRuleTest {

    private final SjpReferredReopenApplicationAcceptedNotificationRule rule = new SjpReferredReopenApplicationAcceptedNotificationRule();


    @Test
    void shouldGenerateNotificationWhenSjpOffencesResultedInFinalOrOats() {
        final UUID accountCorrelationId = randomUUID();
        final UUID newAccountCorrelationId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID applicationId = randomUUID();
        final UUID offenceId = randomUUID();
        final UUID offence2Id = randomUUID();
        final Map<UUID, ApplicationMetadata> sjpMap = new HashMap<>();
        final Map<UUID, List<OffenceResultsDetails>> applicationOffenceResultsMap = new HashMap<>();
        applicationOffenceResultsMap.put(applicationId, List.of(offenceResultsDetails()
                .withIsFinancial(true)
                .withApplicationType("REOPEN")
                .withImpositionOffenceDetails("Previous Offence details")
                .build()));
        sjpMap.put(offenceId, new ApplicationMetadata(applicationId, "REOPEN"));
        sjpMap.put(offence2Id, new ApplicationMetadata(applicationId, "REOPEN"));

        final HearingFinancialResultRequest request = hearingFinancialResultRequest()
                .withIsSJPHearing(false)
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(offenceResults()
                                .withIsFinancial(true)
                                .withImpositionOffenceDetails("some offence details")
                                .withOffenceId(offenceId)
                                .withOffenceResultsCategory(FINAL.name())
                                .build(),
                        offenceResults()
                                .withIsFinancial(false)
                                .withImpositionOffenceDetails("OATS- some offence details")
                                .withOffenceId(offence2Id)
                                .withOffenceResultsCategory(FINAL.name())
                                .build()))
                .withAccountCorrelationId(newAccountCorrelationId)
                .build();
        final ResultNotificationRule.RuleInput input = resultNotificationRuleInputBuilder()
                .withRequest(request)
                .withPrevSjpReferralOffenceResultsDetails(sjpMap)
                .withPrevApplicationOffenceResultsMap(applicationOffenceResultsMap)
                .withCorrelationItemList(
                        List.of(correlationItem()
                                .withAccountCorrelationId(accountCorrelationId)
                                .withOffenceResultsDetailsList(List.of(offenceResultsDetails().withOffenceId(offenceId).build()))
                                .withAccountNumber("AC123456789")
                                .withHearingId(hearingId)
                                .build()))
                .build();
        assertThat("Rule should apply to cc case result", rule.appliesTo(input));
        final Optional<MarkedAggregateSendEmailWhenAccountReceived> result = rule.apply(input);

        result.ifPresentOrElse(notification -> assertThat("subject should match", notification.getSubject(), is("APPLICATION TO REOPEN GRANTED"))
                , () -> fail("Expected notification to be present"));
    }

    @Test
    void shouldNotGenerateNotificationWhenAllSjpOffencesResultedNotInFinal() {
        final UUID accountCorrelationId = randomUUID();
        final UUID newAccountCorrelationId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID applicationId = randomUUID();
        final UUID offenceId = randomUUID();
        final UUID offence2Id = randomUUID();
        final Map<UUID, ApplicationMetadata> sjpMap = new HashMap<>();
        final Map<UUID, List<OffenceResultsDetails>> applicationOffenceResultsMap = new HashMap<>();
        applicationOffenceResultsMap.put(applicationId, List.of(offenceResultsDetails()
                .withIsFinancial(true)
                .withApplicationType("REOPEN")
                .withImpositionOffenceDetails("Previous Offence details")
                .build()));
        sjpMap.put(offenceId, new ApplicationMetadata(applicationId, "REOPEN"));
        sjpMap.put(offence2Id, new ApplicationMetadata(applicationId, "REOPEN"));

        final HearingFinancialResultRequest request = hearingFinancialResultRequest()
                .withIsSJPHearing(false)
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(offenceResults()
                                .withIsFinancial(true)
                                .withImpositionOffenceDetails("some offence details")
                                .withOffenceId(offenceId)
                                .withOffenceResultsCategory(INTERMEDIARY.name())
                                .build(),
                        offenceResults()
                                .withIsFinancial(false)
                                .withImpositionOffenceDetails("some offence details")
                                .withOffenceId(offence2Id)
                                .withOffenceResultsCategory(INTERMEDIARY.name())
                                .build()))
                .withAccountCorrelationId(newAccountCorrelationId)
                .build();
        final ResultNotificationRule.RuleInput input = resultNotificationRuleInputBuilder()
                .withRequest(request)
                .withPrevSjpReferralOffenceResultsDetails(sjpMap)
                .withPrevApplicationOffenceResultsMap(applicationOffenceResultsMap)
                .withCorrelationItemList(
                        List.of(correlationItem()
                                .withAccountCorrelationId(accountCorrelationId)
                                .withOffenceResultsDetailsList(List.of(offenceResultsDetails().withOffenceId(offenceId).build()))
                                .withAccountNumber("AC123456789")
                                .withHearingId(hearingId)
                                .build()))
                .build();
        assertThat("Rule should not apply to cc case result", !rule.appliesTo(input));
    }

    @Test
    void shouldGenerateNotificationWhenAllSjpOffencesResultedInFinalButOtherOffencesNotInfinal() {
        final UUID accountCorrelationId = randomUUID();
        final UUID newAccountCorrelationId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID applicationId = randomUUID();
        final UUID offenceId = randomUUID();
        final UUID offence2Id = randomUUID();
        final Map<UUID, ApplicationMetadata> sjpMap = new HashMap<>();
        final Map<UUID, List<OffenceResultsDetails>> applicationOffenceResultsMap = new HashMap<>();
        applicationOffenceResultsMap.put(applicationId, List.of(offenceResultsDetails()
                .withIsFinancial(true)
                .withApplicationType("REOPEN")
                .withImpositionOffenceDetails("Previous Offence details")
                .build()));
        sjpMap.put(offenceId, new ApplicationMetadata(applicationId, "REOPEN"));

        final HearingFinancialResultRequest request = hearingFinancialResultRequest()
                .withIsSJPHearing(false)
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(offenceResults()
                                .withIsFinancial(true)
                                .withImpositionOffenceDetails("some offence details")
                                .withOffenceId(offenceId)
                                .withOffenceResultsCategory(FINAL.name())
                                .build(),
                        offenceResults()
                                .withIsFinancial(false)
                                .withImpositionOffenceDetails("some offence details")
                                .withOffenceId(offence2Id)
                                .withOffenceResultsCategory(INTERMEDIARY.name())
                                .build()))
                .withAccountCorrelationId(newAccountCorrelationId)
                .build();
        final ResultNotificationRule.RuleInput input = resultNotificationRuleInputBuilder()
                .withRequest(request)
                .withPrevSjpReferralOffenceResultsDetails(sjpMap)
                .withPrevApplicationOffenceResultsMap(applicationOffenceResultsMap)
                .withCorrelationItemList(
                        List.of(correlationItem()
                                .withAccountCorrelationId(accountCorrelationId)
                                .withOffenceResultsDetailsList(List.of(offenceResultsDetails().withOffenceId(offenceId).build()))
                                .withAccountNumber("AC123456789")
                                .withHearingId(hearingId)
                                .build()))
                .build();
        assertThat("Rule should apply to cc case result", rule.appliesTo(input));
        final Optional<MarkedAggregateSendEmailWhenAccountReceived> result = rule.apply(input);

        result.ifPresentOrElse(notification -> assertThat("subject should match", notification.getSubject(), is("APPLICATION TO REOPEN GRANTED"))
                , () -> fail("Expected notification to be present"));
    }

}