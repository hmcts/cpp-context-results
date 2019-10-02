package uk.gov.moj.cpp.data.anonymization.generator;

import java.security.SecureRandom;

public class SecureRandomLongGenerator {

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Returns a random long between 0 and bound exclusive (bound - 1).
     *
     * @param bound the upper bound of the random long returned
     * @return the random long value
     */
    public long nextLong(final long bound) {

        long result = secureRandom.nextLong();
        final long maxBound = bound - 1;

        if ((bound & maxBound) == 0L) {
            result &= maxBound;
        } else {

            long next = result >>> 1;
            result = next % bound;

            while (next + maxBound - result < 0L) {
                next = secureRandom.nextLong() >>> 1;
                result = next % bound;
            }
        }

        return result;
    }
}
