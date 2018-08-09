package uk.gov.moj.cpp.results.it.steps;

import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.BaseUriProvider.getBaseUri;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Properties;
import java.util.UUID;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.services.test.utils.core.http.ResponseData;

public abstract class AbstractStepDefinitions {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractStepDefinitions.class);
    private static final String ENDPOINT_PROPERTIES_FILE = "endpoint.properties";

    private static final Properties ENDPOINT_PROPERTIES = new Properties();
    private static final ThreadLocal<UUID> USER_CONTEXT = new ThreadLocal<>();

    static final String BASE_URI = getBaseUri();
    static final String PUBLIC_EVENT_TOPIC = "public.event";

    static  {
        readConfig();
    }

    private static void readConfig() {
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try(final InputStream stream = loader.getResourceAsStream(ENDPOINT_PROPERTIES_FILE)) {
            ENDPOINT_PROPERTIES.load(stream);
        } catch (final IOException e) {
            LOGGER.warn("Error reading properties from {}", ENDPOINT_PROPERTIES_FILE, e);
        }
    }

    static String getProperty(final String name, final Object... args) {
        String value = ENDPOINT_PROPERTIES.getProperty(name);
        if (args.length > 0) {
            value = MessageFormat.format(value, args);
        }
        return value;
    }

    static void setLoggedInUser(final UUID userId) {
        USER_CONTEXT.set(userId);
    }

    static UUID getLoggedInUser() {
        return USER_CONTEXT.get();
    }

    static MultivaluedMap<String, Object> getUserHeader(final UUID userId) {
        final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(USER_ID, userId.toString());
        return headers;
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
