package uk.gov.moj.cpp.results.event.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.enterprise.inject.Produces;
import javax.naming.Context;
import javax.naming.NameNotFoundException;
import javax.naming.spi.InitialContextFactory;
import javax.naming.NamingException;
import java.util.Hashtable;

import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.jupiter.api.Test;
import uk.gov.justice.services.common.configuration.ServiceContextNameProvider;
import uk.gov.justice.services.common.configuration.ValueProducer;

class EventGridPublisherConfigurationTest {

    @Test
    void disabledStringJndiConfigurationBootsWithTheFrameworkValueProducer() throws Exception {
        final Map<String, String> bindings = new HashMap<>();
        bindings.put("java:global/eventGridPublishingEnabled", "false");
        withConfiguration(bindings, publisher -> {
            assertTrue(publisher.sendHearingResultedEvent(UUID.randomUUID(), "hearing", "resulted"));
            assertTrue(publisher.sendHearingResultedForDayEvent(UUID.randomUUID(), "hearing", "2026-11-04", "resulted-for-day"));
        });
    }

    @Test
    void absentPublishingFlagUsesTheFrameworkDefault() throws Exception {
        withConfiguration(new HashMap<>(), publisher -> {
            final Field flag = EventGridPublisher.class.getDeclaredField("eventGridPublishingEnabled");
            flag.setAccessible(true);
            assertEquals("true", flag.get(publisher));
        });
    }

    private void withConfiguration(final Map<String, String> bindings, final Check check) throws Exception {
        final String previous = System.getProperty(Context.INITIAL_CONTEXT_FACTORY);
        TestInitialContextFactory.bindings = bindings;
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, TestInitialContextFactory.class.getName());
        try (WeldContainer container = new Weld().disableDiscovery()
                .addBeanClasses(EventGridPublisher.class, ValueProducer.class, ContextNameProducer.class)
                .initialize()) {
            check.run(container.select(EventGridPublisher.class).get());
        } finally {
            if (previous == null) {
                System.clearProperty(Context.INITIAL_CONTEXT_FACTORY);
            } else {
                System.setProperty(Context.INITIAL_CONTEXT_FACTORY, previous);
            }
            TestInitialContextFactory.bindings = new HashMap<>();
        }
    }

    public static class ContextNameProducer {
        @Produces
        public ServiceContextNameProvider serviceContextNameProvider() {
            return () -> "results-event-processor";
        }
    }

    public static class TestInitialContextFactory implements InitialContextFactory {
        static Map<String, String> bindings = new HashMap<>();

        @Override
        public Context getInitialContext(final Hashtable<?, ?> environment) throws NamingException {
            final Context context = mock(Context.class);
            when(context.lookup(anyString())).thenAnswer(invocation -> {
                final String name = invocation.getArgument(0);
                if (!bindings.containsKey(name)) {
                    throw new NameNotFoundException(name);
                }
                return bindings.get(name);
            });
            return context;
        }
    }

    private interface Check {
        void run(EventGridPublisher publisher) throws Exception;
    }
}
