package uk.gov.moj.cpp.results.domain.aggregate;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.frequency;
import static java.util.Collections.nCopies;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.results.test.TestUtilities.payloadAsString;
import static uk.gov.moj.cpp.results.test.TestUtilities.stringToJsonObject;

import uk.gov.justice.hearing.courts.HearingFinancialResultRequest;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.results.domain.event.NewApplicationResults;
import uk.gov.moj.cpp.results.domain.event.NewOffenceByResult;
import uk.gov.moj.cpp.results.test.matchers.JsonMatcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.JsonObject;

import org.hamcrest.Matchers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for defining, executing and asserting test steps related to Hearing Financial Result Aggregates.
 */
class HearingFinancialResultAggregateTestSteps {
    private static final Logger logger = LoggerFactory.getLogger(HearingFinancialResultsAggregateNCESTest.class.getName());
    private static final JsonObjectToObjectConverter jsonToObjectConverter = new JsonObjectToObjectConverter(new ObjectMapperProducer().objectMapper());
    private static final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(new ObjectMapperProducer().objectMapper());
    private static final String APPLICATION_RESULT = "NEXH --";

    /**
     * Builder for defining a scenario of result tracked steps.
     * Each scenario consists of a sequence of result tracked steps, a step is started by newResultTrackedStep and finished by finishStep.
     */
    static class Scenario {
        List<StepDef> resultTrackedSteps = new ArrayList<>();

        static Scenario newScenario() {
            return new Scenario();
        }

        Scenario newStep(StepDef step) {
            resultTrackedSteps.add(step);
            return this;
        }
    }

    /**
     * Base class for defining a step in a scenario.
     */
    abstract static class StepDef {
        Map<String, List<String>> expectedEventPayloads = new HashMap<>();
        Map<String, String> expectedEventPayloadRegExs = new HashMap<>();
        List<String> expectedEventNames = new ArrayList<>();
        List<String> notExpectedEventNames = new ArrayList<>();
        private final Comparison defaultComparison = Comparison.comparison();
        Map<String, Comparison> comparisonMap = new HashMap<>();
        String name;

        abstract Map<String, List<Object>> execute(final HearingFinancialResultsAggregate aggregate);

        void assertExpectedEvents(final Map<String, List<Object>> actualEvents) {

            expectedEventPayloads.forEach((key, expectedPayloadFiles) -> {
                final List<Object> actualEventPayloads = actualEvents.get(key);
                if (actualEventPayloads == null) {
                    fail("Expected event not found: " + key);
                }

                expectedPayloadFiles.forEach(expectedEventPayloadFile -> {
                    StringBuilder mismatchErrors = new StringBuilder();
                    boolean matchFound = false;
                    short attempt = 0;
                    for (Object actualEvent : actualEventPayloads) {
                        final String actualEventPayload = objectToJsonObjectConverter.convert(actualEvent).toString();
                        try {
                            if (comparisonMap.isEmpty()) {
                                logger.warn("<WARNING> USING DEFAULT COMPARISON FOR ALL EXPECTED EVENT PAYLOADS, ADD COMPARISON FOR EACH EXPECTED EVENT PAYLOAD TO AVOID THIS WARNING.");
                            }
                            final Comparison comparator = comparisonMap.getOrDefault(expectedEventPayloadFile, defaultComparison);
                            final String expectedEventPayload = payloadAsString(expectedEventPayloadFile, comparator.parameters);
                            if (expectedEventPayload.isEmpty()) {
                                fail("Expected event payload is empty: " + expectedEventPayloadFile);
                            }
                            assertThat(actualEventPayload, JsonMatcher.matchesJson(expectedEventPayload, comparator.pathsExcluded));
                            matchFound = true;
                            break;
                        } catch (AssertionError e) {
                            mismatchErrors.append(format("\n**MATCHING ATTEMPT %d **\n", ++attempt)).append(e.getMessage()).append("\n\n");
                        }
                    }
                    if (!matchFound) {
                        throw new AssertionError(format("Expected event payload(%s) not matched: %s", expectedEventPayloadFile, mismatchErrors));
                    }
                });
            });

            expectedEventPayloadRegExs.forEach((key, regexPattern) -> {
                final List<Object> actualEventPayloads = actualEvents.get(key);
                if (actualEventPayloads == null) {
                    fail("Expected event not found: " + key);
                }
                final List<String> actualJsonPayloads = actualEventPayloads.stream().map(objectToJsonObjectConverter::convert).map(Object::toString).toList();
                assertThat(actualJsonPayloads, hasItem(Matchers.matchesPattern(regexPattern)));
            });

            if (!expectedEventNames.isEmpty()) {
                List<String> actualEventNames = actualEvents.entrySet()
                        .stream()
                        .flatMap(entry -> nCopies(entry.getValue().size(), entry.getKey()).stream())
                        .toList();
                if (expectedEventNames.size() != actualEventNames.size()) {
                    fail(String.format("Expected event count does not match actual event count, Expected: %d Actual: %d", expectedEventNames.size(), actualEventNames.size()));
                }

                for (String actualEventName : actualEventNames) {
                    if (frequency(actualEventNames, actualEventName) != frequency(expectedEventNames, actualEventName)) {
                        fail("Expected event count does not match actual event count for event-name: " + actualEventName);
                    }
                }
            }

            expectedEventNames.forEach(expectedEvent -> {
                if (!actualEvents.containsKey(expectedEvent)) {
                    fail("Expected event not found: " + expectedEvent);
                }
            });

            notExpectedEventNames.forEach(notExpectedEvent -> {
                if (actualEvents.containsKey(notExpectedEvent)) {
                    fail("Not expected event found: " + notExpectedEvent);
                }
            });
        }

        StepDef withExpectedEventPayloadEquals(final String eventClassName, final String payload) {
            expectedEventPayloads.computeIfAbsent(eventClassName, k -> new ArrayList<>()).add(payload);
            return this;
        }

        StepDef withExpectedEventPayloadEquals(final String eventClassName, final String payloadFileName, final Comparison comparison) {
            expectedEventPayloads.computeIfAbsent(eventClassName, k -> new ArrayList<>()).add(payloadFileName);
            comparisonMap.put(payloadFileName, comparison);
            return this;
        }

        StepDef withExpectedEventPayloadRegEx(final String eventClassName, final String regex) {
            expectedEventPayloadRegExs.put(eventClassName, regex);
            return this;
        }

        StepDef withExpectedEventNames(final String... eventClassNames) {
            expectedEventNames.addAll(Arrays.stream(eventClassNames).toList());
            return this;
        }

        StepDef withNotExpectedEventNames(final String... eventClassNames) {
            notExpectedEventNames.addAll(Arrays.stream(eventClassNames).toList());
            return this;
        }
    }

    /**
     * Builder for defining a result tracked step. It takes a result tracked event and emits expected events that should be created as a result.
     */
    static class ResultTrackedStep extends StepDef {
        List<NewOffenceByResult> newOffenceByResults = new ArrayList<>();
        NewApplicationResults.Builder newApplicationResults = new NewApplicationResults.Builder();
        Optional<AccountInfo> accountInfo = AccountInfo.defaultAccountInfo();

        String resultTrackedPayload;

        static ResultTrackedStep newResultTrackedStep(final String name) {
            ResultTrackedStep resultTrackedStep = new ResultTrackedStep();
            resultTrackedStep.name = name;
            return resultTrackedStep;
        }

        public Map<String, List<Object>> execute(final HearingFinancialResultsAggregate aggregate) {
            logger.info("Executing ResultTrackedStep: {}", name);
            String accountCorrelationId = "";
            if (accountInfo.isPresent()) {
                accountCorrelationId = accountInfo.get().accountCorrelationId.toString();
            }

            final JsonObject resultTracked = stringToJsonObject(payloadAsString(resultTrackedPayload).replace("{{accountCorrelationId}}", accountCorrelationId));
            HearingFinancialResultRequest applicationResulted = jsonToObjectConverter.convert(resultTracked, HearingFinancialResultRequest.class);

            Map<UUID, String> mockOffenceDates = mock(Map.class);
            when(mockOffenceDates.get(any(UUID.class))).thenReturn("2019-11-28");
            Stream<Object> financialResultStream = aggregate.updateFinancialResults(applicationResulted, "false", "2021-21-21", "2021-21-21", newOffenceByResults, APPLICATION_RESULT, mockOffenceDates);

            if (accountInfo.isPresent()) {
                final AccountInfo accountInformation = accountInfo.get();
                final Stream<Object> updateAccountNumber = aggregate.updateAccountNumber(accountInformation.accountNumber, accountInformation.accountCorrelationId);
                final Stream<Object> applicationEmailAndSend = aggregate.checkApplicationEmailAndSend();
                financialResultStream = Stream.of(financialResultStream, updateAccountNumber, applicationEmailAndSend).flatMap(s -> s);
            }

            return financialResultStream.peek(e -> logEvent(name, e))
                    .collect(Collectors.groupingBy(
                            e -> e.getClass().getSimpleName()
                    ));
        }

        ResultTrackedStep withResultTrackedEvent(final String payload) {
            resultTrackedPayload = payload;
            return this;
        }

        ResultTrackedStep withResultTrackedEvent(final String payload, final Optional<AccountInfo> accountInfo) {
            resultTrackedPayload = payload;
            this.accountInfo = accountInfo;
            return this;
        }
    }

    /**
     * Builder for defining a step that sends an email for a new application.
     */
    static class NcesEmailForNewApplicationStep extends StepDef {
        String applicationType = "REOPEN";

        static NcesEmailForNewApplicationStep newNcesEmailForNewApplicationStep(String name) {
            final NcesEmailForNewApplicationStep step = new NcesEmailForNewApplicationStep();
            step.name = name;
            return step;
        }

        @Override
        public Map<String, List<Object>> execute(final HearingFinancialResultsAggregate aggregate) {
            logger.info("Executing NcesEmailForNewApplicationStep");
            final Stream<Object> eventStream = aggregate.sendNcesEmailForNewApplication(applicationType, "2021-21-21", List.of("caseUrn1", "caseUrn2"), "hearingCourtCentreName", emptyList());
            return eventStream
                    .peek(e -> logEvent("new application", e))
                    .collect(Collectors.groupingBy(
                            e -> e.getClass().getSimpleName()
                    ));
        }
    }

    static class AccountInfo {
        UUID accountCorrelationId;
        String accountNumber;

        static Optional<AccountInfo> defaultAccountInfo() {
            final UUID uuid = UUID.randomUUID();
            return accountInfo(uuid.toString(), uuid + "ACCOUNT");
        }

        static Optional<AccountInfo> emptyAccountInfo() {
            return Optional.empty();
        }

        static Optional<AccountInfo> accountInfo(final String accountCorrelationId, final String accountNumber) {
            final AccountInfo accountInfo = new AccountInfo();
            accountInfo.accountCorrelationId = UUID.fromString(accountCorrelationId);
            accountInfo.accountNumber = accountNumber;
            return Optional.of(accountInfo);
        }
    }

    static class Comparison {
        List<String> pathsExcluded = new ArrayList<>();
        Map<String, String> parameters = new HashMap<>();

        static Comparison comparison() {
            final Comparison comparison = new Comparison();
            comparison.pathsExcluded.add("gobAccountNumber");
            comparison.pathsExcluded.add("oldGobAccountNumber");
            comparison.pathsExcluded.add("materialId");
            comparison.pathsExcluded.add("notificationId");
            return comparison;
        }

        Comparison withPathsExcluded(String... paths) {
            pathsExcluded.clear();
            pathsExcluded.addAll(asList(paths));
            return this;
        }

        Comparison withParam(String key, String value) {
            parameters.put(key, value);
            return this;
        }
    }

    private static void logEvent(final String context, final Object obj) {
        logger.info("[{}] {} = {}", context, obj.getClass().getSimpleName(), objectToJsonObjectConverter.convert(obj));
    }
}
