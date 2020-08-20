package uk.gov.moj.cpp.results.it;

import static com.jayway.restassured.RestAssured.given;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.moj.cpp.results.it.utils.WireMockStubUtils.setupAsAuthorisedUser;
import static uk.gov.moj.cpp.results.it.utils.WireMockStubUtils.setupAsSystemUser;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.function.Consumer;

import javax.jms.MessageConsumer;

import org.apache.http.HttpStatus;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jayway.restassured.builder.RequestSpecBuilder;
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.response.Header;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.RequestSpecification;

import uk.gov.justice.services.test.utils.core.http.ResponseData;
import uk.gov.justice.services.test.utils.core.rest.RestClient;
import uk.gov.moj.cpp.results.it.utils.QueueUtil;

final class TestUtilities {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestUtilities.class);

    public static final UUID USER_ID_VALUE = randomUUID();
    public static final UUID USER_ID_VALUE_AS_ADMIN = randomUUID();
    public static final Header CPP_UID_HEADER = new Header(USER_ID, USER_ID_VALUE.toString());
    public static final Header CPP_UID_HEADER_AS_ADMIN = new Header(USER_ID, USER_ID_VALUE_AS_ADMIN.toString());
    public static final String ENDPOINT_PROPERTIES_FILE = "endpoint.properties";
    public static final Properties ENDPOINT_PROPERTIES = new Properties();
    public static final RequestSpecification requestSpec;
    public static final String baseUri;
    public static final RestClient restClient = new RestClient();

    static {
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try (final InputStream stream = loader.getResourceAsStream(ENDPOINT_PROPERTIES_FILE)) {
            ENDPOINT_PROPERTIES.load(stream);
            final String baseUriProp = System.getProperty("INTEGRATION_HOST_KEY");
            baseUri = isNotEmpty(baseUriProp) ? format("http://%s:8080", baseUriProp) : ENDPOINT_PROPERTIES.getProperty("base-uri");
            requestSpec = new RequestSpecBuilder().setBaseUri(baseUri).build();
            setupAsAuthorisedUser(USER_ID_VALUE);
            setupAsSystemUser(USER_ID_VALUE_AS_ADMIN);
        } catch (final IOException e) {
            LOGGER.warn("Error reading properties from {}", ENDPOINT_PROPERTIES_FILE, e);
            throw new ExceptionInInitializerError(e);
        }
    }

    public static class EventListener {

        private MessageConsumer messageConsumer;
        private String eventType;
        private List<Matcher<Object>> matchers = new ArrayList<>();

        public EventListener(final String eventType) {
            this.eventType = eventType;
            this.messageConsumer = QueueUtil.publicEvents.createConsumer(eventType);
        }

        private boolean matches(String json) {
            return matchers.stream().allMatch(m -> m.matches(json));
        }

        public void expectNoneWithin(long timeout) {

            JsonPath message = QueueUtil.retrieveMessage(messageConsumer, timeout);

            while (message != null && !matches(message.prettify())) {
                message = QueueUtil.retrieveMessage(messageConsumer);
            }
            if (message != null) {
                fail("expected no messages");
            }
        }

        public JsonPath waitFor() {

            JsonPath message = QueueUtil.retrieveMessage(messageConsumer, 30000);

            while (message != null && !matches(message.prettify())) {
                message = QueueUtil.retrieveMessage(messageConsumer);
            }
            if (message == null) {
                fail("Expected '" + eventType + "' message to emit on the public.event topic.");
            }
            return message;
        }

        public EventListener withFilter(Matcher<Object> matcher) {
            this.matchers.add(matcher);
            return this;
        }
    }

    public static EventListener listenFor(String mediaType) {
        return new EventListener(mediaType);
    }

    public static class JsonUtil {
        public static String toJsonString(final Object o) throws JsonProcessingException {
            final ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            return mapper.writeValueAsString(o);
        }
    }

    public static class CommandBuilder {
        private RequestSpecification requestSpec;
        private String endpoint;
        private String type;
        private String payloadAsString;
        private Object[] arguments = new Object[0];

        public CommandBuilder(RequestSpecification requestSpec, String endpoint) {
            this.requestSpec = requestSpec;
            this.endpoint = endpoint;
        }

        public CommandBuilder withArgs(Object... args) {
            this.arguments = args;
            return this;
        }

        public CommandBuilder ofType(final String type) {
            this.type = type;
            return this;
        }

        public CommandBuilder withPayload(final String payload) {
            this.payloadAsString = payload;
            return this;
        }


        public CommandBuilder withPayload(final Object payload) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                mapper.registerModule(new JavaTimeModule());
                mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
                mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
                mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

                this.payloadAsString = JsonUtil.toJsonString(payload);

                System.out.println("Command Payload: ");
                System.out.println(this.payloadAsString);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            return this;
        }

        public void executeSuccessfully() {

            String url = MessageFormat.format(ENDPOINT_PROPERTIES.getProperty(endpoint), arguments);
            System.out.println("Command Url: ");
            System.out.println(url);

            Response writeResponse = given().spec(requestSpec).and()
                    .contentType(type)
                    .accept(type)
                    .body(ofNullable(this.payloadAsString).orElse(""))
                    .header(CPP_UID_HEADER).when()
                    .post(url)
                    .then().extract().response();
            assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));
        }
    }

    public static CommandBuilder makeCommand(final String endpoint) {
        return makeCommand(requestSpec, endpoint);
    }

    public static CommandBuilder makeCommand(final RequestSpecification requestSpec, final String endpoint) {
        return new CommandBuilder(requestSpec, endpoint);
    }

    public static String getUrl(final String endpoint, final Object... args) {
        return baseUri + "/" + MessageFormat.format(ENDPOINT_PROPERTIES.getProperty(endpoint), args);
    }

    public static <T> T with(final T object, final Consumer<T> consumer) {
        consumer.accept(object);
        return object;
    }

    public static Matcher<ResponseData> print() {
        return new BaseMatcher<ResponseData>() {
            @Override
            public boolean matches(Object o) {
                if (o instanceof ResponseData) {
                    ResponseData responseData = (ResponseData) o;
                    System.out.println(responseData.getPayload());
                }
                return true;
            }

            @Override
            public void describeTo(Description description) {
            }
        };
    }
}
