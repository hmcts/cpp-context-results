package uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules;

import static java.util.stream.Collectors.toMap;

import uk.gov.justice.hearing.courts.HearingFinancialResultRequest;
import uk.gov.justice.hearing.courts.OffenceResults;
import uk.gov.justice.hearing.courts.OffenceResultsDetails;
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.ResultNotificationRule;
import uk.gov.moj.cpp.results.domain.aggregate.utils.CorrelationItem;
import uk.gov.moj.cpp.results.domain.event.NewOffenceByResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ResultNotificationRuleInputBuilder {
    private HearingFinancialResultRequest request;
    private String isWrittenOffExists;
    private String originalDateOfOffenceList;
    private String originalDateOfSentenceList;
    private List<NewOffenceByResult> newOffenceResultsFromHearing;
    private String applicationResult;
    private Map<UUID, String> offenceDateMap;
    private String ncesEmail;
    private Map<UUID, OffenceResultsDetails> prevOffenceResultsDetails;
    private Map<UUID, UUID> prevSjpReferralOffenceResultsDetails;
    private Map<UUID, List<OffenceResultsDetails>> prevApplicationResultsDetails;
    private Map<UUID, List<OffenceResultsDetails>> prevApplicationOffenceResultsMap;
    private LinkedList<CorrelationItem> correlationItemList;

    private ResultNotificationRuleInputBuilder() {
    }

    public static ResultNotificationRuleInputBuilder resultNotificationRuleInputBuilder() {
        final ResultNotificationRuleInputBuilder builder = new ResultNotificationRuleInputBuilder();
        builder.isWrittenOffExists = null;
        builder.originalDateOfOffenceList = "2023-01-01";
        builder.originalDateOfSentenceList = "2023-02-01";
        builder.newOffenceResultsFromHearing = List.of();
        builder.applicationResult = null;
        builder.offenceDateMap = Map.of();
        builder.ncesEmail = "nces@test.com";
        builder.prevOffenceResultsDetails = Map.of();
        builder.prevSjpReferralOffenceResultsDetails = Map.of();
        builder.prevApplicationResultsDetails = Map.of();
        builder.prevApplicationOffenceResultsMap = Map.of();
        builder.correlationItemList = new LinkedList<>();
        return builder;
    }

    public ResultNotificationRuleInputBuilder withRequest(final HearingFinancialResultRequest request) {
        this.request = request;
        this.offenceDateMap = request.getOffenceResults().stream().
                collect(toMap(
                        OffenceResults::getOffenceId,
                        offenceResult -> "2023-03-01"
                ));
        return this;
    }

    public ResultNotificationRuleInputBuilder withPrevSjpReferralOffenceResultsDetails(final Map<UUID, UUID> prevSjpReferralOffenceResultsDetails) {
        this.prevSjpReferralOffenceResultsDetails = prevSjpReferralOffenceResultsDetails;
        return this;
    }

    public ResultNotificationRuleInputBuilder withPrevOffenceResultsDetails(final Map<UUID, OffenceResultsDetails> prevOffenceResultsDetails) {
        this.prevOffenceResultsDetails = prevOffenceResultsDetails;
        return this;
    }

    public ResultNotificationRuleInputBuilder withPrevApplicationResultsDetails(final Map<UUID, List<OffenceResultsDetails>> prevApplicationResultsDetails) {
        this.prevApplicationResultsDetails = prevApplicationResultsDetails;
        return this;
    }

    public ResultNotificationRuleInputBuilder withPrevApplicationOffenceResultsMap(final Map<UUID, List<OffenceResultsDetails>> prevApplicationOffenceResultsMap) {
        this.prevApplicationOffenceResultsMap = prevApplicationOffenceResultsMap;
        return this;
    }

    public ResultNotificationRule.RuleInput build() {
        return new ResultNotificationRule.RuleInput(request, isWrittenOffExists, originalDateOfOffenceList, originalDateOfSentenceList,
                newOffenceResultsFromHearing, applicationResult, offenceDateMap, ncesEmail, prevOffenceResultsDetails,
                prevApplicationResultsDetails, prevApplicationOffenceResultsMap, prevSjpReferralOffenceResultsDetails, correlationItemList);
    }

    public ResultNotificationRuleInputBuilder withCorrelationItemList(final List<CorrelationItem> items) {
        this.correlationItemList = new LinkedList<>(items);
        return this;
    }
}
