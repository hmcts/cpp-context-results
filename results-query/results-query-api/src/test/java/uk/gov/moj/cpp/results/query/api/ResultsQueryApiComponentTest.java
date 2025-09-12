package uk.gov.moj.cpp.results.query.api;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.io.FileUtils.readLines;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static uk.gov.justice.services.core.annotation.Component.QUERY_API;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

import uk.gov.justice.services.core.annotation.Handles;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ResultsQueryApiComponentTest {

    private static final String PATH_TO_RAML = "src/raml/results-query-api.raml";
    private static final String NAME = "name:";
    private static final String RESULTS_API = "name: results.get";
    private static final String INFORMANT_REGISTER_API = "name: results.query.informant";

    private Map<String, String> apiMethodsToHandlerNames;
    private Map<String, String> informantAPIMethodsToHandlerNames;

    @BeforeEach
    public void setup() {
        apiMethodsToHandlerNames = stream(ResultsQueryApi.class.getMethods())
                .filter(method -> method.getAnnotation(Handles.class) != null)
                .collect(toMap(Method::getName, method -> method.getAnnotation(Handles.class).value()));
        informantAPIMethodsToHandlerNames = stream(InformantRegisterRequestApi.class.getMethods())
                .filter(method -> method.getAnnotation(Handles.class) != null)
                .collect(toMap(Method::getName, method -> method.getAnnotation(Handles.class).value()));
    }

    @Test
    public void testActionNameAndHandleNameAreSame() throws IOException {

        assertThat(apiMethodsToHandlerNames.values(), containsInAnyOrder(readLines(new File(PATH_TO_RAML)).stream()
                .filter(action -> !action.isEmpty())
                .filter(line -> line.contains(RESULTS_API))
                .map(line -> line.replaceAll(NAME, "").trim()).toArray()));
    }

    @Test
    public void testActionNameAndHandleNameAreSameForInformantRegisterAPI() throws IOException {

        assertThat(informantAPIMethodsToHandlerNames.values(), containsInAnyOrder(readLines(new File(PATH_TO_RAML)).stream()
                .filter(action -> !action.isEmpty())
                .filter(line -> line.contains(INFORMANT_REGISTER_API))
                .map(line -> line.replaceAll(NAME, "").trim()).toArray()));
    }

    @Test
    public void testHandleNamesPassThroughRequester() {
        apiMethodsToHandlerNames.forEach((key, value) ->
                assertThat(ResultsQueryApi.class, isHandlerClass(QUERY_API)
                        .with(method(key)
                                .thatHandles(value))));
    }

    @Test
    public void testHearingFinancialDetailsQueryApiHandler() {
        assertThat(DefendantGobAccountsQueryApi.class, isHandlerClass(QUERY_API)
                .with(method("getDefendantGobAccounts")
                        .thatHandles("results.query.defendant-gob-accounts")));
    }

}
