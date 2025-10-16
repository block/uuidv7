package xyz.block.uuidv7;

import java.security.SecureRandom;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.LongSupplier;

/**
 * Utility class for generating monotonic UUID v7 identifiers.
 * <p>
 * This implementation ensures that UUIDs generated within the same millisecond are strictly
 * ordered by incrementing a counter. This provides guaranteed sequential ordering, making it
 * ideal for database primary keys and scenarios requiring chronological order guarantees.
 * <p>
 * UUID v7 is a time-ordered UUID format that encodes a Unix timestamp in milliseconds
 * in the most significant 48 bits, making UUIDs naturally sortable by creation time.
 * This implementation follows RFC 9562 with monotonic counter support.
 * <p>
 * All generation methods are synchronized to ensure strict ordering across threads.
 * If the counter overflows within a millisecond (after 4096 UUIDs), the method will
 * block until the next millisecond to maintain uniqueness.
 */
public final class MonotonicUUIDv7 {

    // Counter occupies 12 bits (rand_a in RFC 9562)
    private static final int COUNTER_BITS = 12;
    private static final int COUNTER_MAX = (1 << COUNTER_BITS) - 1; // 0xFFF

    // Thread-safe random source for initial counter values
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // Monotonicity state (guarded by class lock in synchronized methods)
    private static long lastTimestamp = 0L;
    private static int counter = 0;

    private MonotonicUUIDv7() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Generates a new monotonic UUID v7 using the current system time.
     * <p>
     * This method ensures that UUIDs generated within the same millisecond are strictly
     * ordered by incrementing a counter. If the counter overflows within a millisecond,
     * the method will block until the next millisecond to maintain uniqueness.
     * <p>
     * This method is synchronized and best suited for database primary keys and scenarios
     * requiring guaranteed sequential ordering.
     *
     * @return a new UUID v7 instance
     */
    public static synchronized UUID generate() {
        return generateImpl(System::currentTimeMillis);
    }

    /**
     * Generates a new monotonic UUID v7 using a custom clock source.
     * <p>
     * This method ensures that UUIDs generated within the same millisecond are strictly
     * ordered by incrementing a counter. Useful for testing monotonic behavior with
     * controlled clock sources.
     * <p>
     * This method is synchronized and best suited for database primary keys and scenarios
     * requiring guaranteed sequential ordering.
     *
     * @param clock a supplier that returns the current time in milliseconds since Unix epoch
     * @return a new UUID v7 instance
     */
    public static synchronized UUID generate(LongSupplier clock) {
        return generateImpl(clock);
    }

    /**
     * Monotonic implementation - synchronized to ensure ordering.
     * This method must only be called from synchronized methods.
     */
    private static UUID generateImpl(LongSupplier clock) {
        long timestamp = clock.getAsLong();
        int counterValue;

        if (timestamp == lastTimestamp) {
            // Same millisecond - increment counter
            counter = (counter + 1) & COUNTER_MAX;

            if (counter == 0) {
                // Counter overflow - wait for next millisecond to maintain uniqueness
                do {
                    timestamp = clock.getAsLong();
                } while (timestamp == lastTimestamp);

                // New millisecond - start with random counter value
                counter = SECURE_RANDOM.nextInt(COUNTER_MAX + 1);
            }
            counterValue = counter;
        } else {
            // New millisecond - start with random counter value for unpredictability
            counter = SECURE_RANDOM.nextInt(COUNTER_MAX + 1);
            counterValue = counter;
            lastTimestamp = timestamp;
        }

        // Generate random bytes for the least significant bits (rand_b)
        byte[] randomBytes = new byte[8];
        ThreadLocalRandom.current().nextBytes(randomBytes);

        return UUIDv7.buildUuid(timestamp, counterValue, randomBytes);
    }
}
