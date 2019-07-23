package uk.gov.moj.cpp.results.it.utils;

import static org.junit.Assert.assertThat;

import uk.gov.moj.cpp.results.test.matchers.BeanMatcher;

import java.time.LocalDateTime;
import java.util.function.Supplier;

@SuppressWarnings({"squid:S2925"})
public class Queries {

    public static <T> void pollForMatch(final long timeoutMillis, final long pollPeriodMillies, Supplier<T> query, final BeanMatcher<T> resultMatcher) {

        final LocalDateTime expiryTime = LocalDateTime.now().plusSeconds(timeoutMillis);

        boolean matched = false;

        T latestValue = null;
        for (; !matched && LocalDateTime.now().isBefore(expiryTime); ) {
            try {
                Thread.sleep(pollPeriodMillies);
            } catch (Exception ex) {
            }
            matched = resultMatcher.matches(latestValue = query.get());
        }

        if (!matched) {
            assertThat(latestValue, resultMatcher);
        }
    }
}