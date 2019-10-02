package uk.gov.moj.cpp.data.anonymization.generator;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class SecureRandomLongGeneratorTest {

    @Test
    public void shouldReturnRandomLongWithinTheRangeZeroToGivenBounds() {

        final long bound = 100L;
        final Map<Long, Long> distributionCounter = new HashMap<>();

        for (int index = 0; index < 2000; index++) {
            final long result = new SecureRandomLongGenerator().nextLong(bound);

            distributionCounter.compute(result, (key, value) -> {
                if (null == value) {
                    return 1L;
                } else {
                    return value + 1L;
                }
            });

            assertThat(result >= 0L, is(true));
            assertThat(result < bound, is(true));
        }

        assertThat(distributionCounter.size(), is(100));
    }
}
