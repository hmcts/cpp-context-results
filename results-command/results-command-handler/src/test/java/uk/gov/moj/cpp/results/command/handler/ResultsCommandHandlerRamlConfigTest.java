package uk.gov.moj.cpp.results.command.handler;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.io.FileUtils.readLines;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

import uk.gov.justice.services.core.annotation.Handles;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ResultsCommandHandlerRamlConfigTest {

    private static final String PATH_TO_RAML = "src/raml/results-command-handler.messaging.raml";
    private static final String CONTEXT_NAME = "results";
    private static final String CONTENT_TYPE_PREFIX = "application/vnd.";

    private Map<String, String> handlerMethodsToHandlerNames;

    @BeforeEach
    public void setup() {
        handlerMethodsToHandlerNames = handlerMethodsToHandlerNames(ResultsCommandHandler.class, InformantRegisterHandler.class, NcesDocumentNotificationCommandHandler.class);
    }

    @Test
    public void testActionNameAndHandleNameAreSame() throws Exception {
        final List<String> ramlActionNames = readLines(new File(PATH_TO_RAML)).stream()
                .filter(action -> !action.isEmpty())
                .filter(line -> line.contains(CONTENT_TYPE_PREFIX) && line.contains(CONTEXT_NAME))
                .map(line -> line.replaceAll("(application/vnd\\.)|(\\+json:)","").trim())
                .collect(toList());

        final Collection<String> allHandlerNames = handlerMethodsToHandlerNames.values();

        assertThat(allHandlerNames, containsInAnyOrder(ramlActionNames.toArray()));
    }

    private Map<String, String> handlerMethodsToHandlerNames(final Class<?>... classes) {
        return stream(classes)
                .flatMap(c -> stream(c.getMethods()))
                .filter(method -> method.getAnnotation(Handles.class) != null)
                .collect(toMap(Method::getName, method -> method.getAnnotation(Handles.class).value()));
    }
}
