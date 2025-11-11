package uk.gov.moj.cpp.results.it.utils;

import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.moj.cpp.results.test.matchers.BeanMatcher;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@SuppressWarnings({"squid:S2925"})
public class Queries {

    public static <T> void pollForMatch(final long timeoutInSeconds, final long pollPeriodMillis, Supplier<T> query, final BeanMatcher<T> resultMatcher) {

        final LocalDateTime expiryTime = LocalDateTime.now().plusSeconds(timeoutInSeconds);

        boolean matched = false;

        T latestValue = null;
        for (; !matched && LocalDateTime.now().isBefore(expiryTime); ) {
            try {
                TimeUnit.MILLISECONDS.sleep(pollPeriodMillis);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            matched = resultMatcher.matches(latestValue = query.get());
        }

        if (!matched) {
            assertThat(latestValue, resultMatcher);
        }
    }
}