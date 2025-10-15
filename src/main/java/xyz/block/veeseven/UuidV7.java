package xyz.block.veeseven;

import java.security.SecureRandom;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.LongSupplier;

/**
 * Utility class for generating and working with UUID v7 identifiers.
 * <p>
 * UUID v7 is a time-ordered UUID format that encodes a Unix timestamp in milliseconds
 * in the most significant 48 bits, making UUIDs naturally sortable by creation time.
 * This implementation follows RFC 9562 with optional monotonic counter support.
 * <p>
 * Non-monotonic methods use ThreadLocalRandom for maximum performance with no synchronization.
 * Monotonic methods use a synchronized counter to ensure strict ordering within the same millisecond.
 */
public final class UuidV7 {

    // Counter occupies 12 bits (rand_a in RFC 9562)
    private static final int COUNTER_BITS = 12;
    private static final int COUNTER_MAX = (1 << COUNTER_BITS) - 1; // 0xFFF

    // Thread-safe random source for initial counter values in monotonic mode
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // Monotonicity state (guarded by class lock in monotonic methods)
    private static long monotonicGeneratorLastTimestamp = 0L;
    private static int monotonicGeneratorCounter = 0;

    private UuidV7() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Generates a new UUID v7 using the current system time in non-monotonic mode.
     * <p>
     * Uses ThreadLocalRandom for maximum performance with no synchronization overhead.
     * UUIDs generated in the same millisecond may not be strictly ordered, but uniqueness
     * is guaranteed through random bits.
     *
     * @return a new UUID v7 instance
     */
    public static UUID generate() {
        return generateNonMonotonic(System::currentTimeMillis);
    }

    /**
     * Generates a new UUID v7 using a custom clock source in non-monotonic mode.
     * <p>
     * Uses ThreadLocalRandom for maximum performance with no synchronization overhead.
     * Useful for testing or specialized use cases where you need control over the timestamp.
     *
     * @param clock a supplier that returns the current time in milliseconds since Unix epoch
     * @return a new UUID v7 instance
     */
    public static UUID generate(LongSupplier clock) {
        return generateNonMonotonic(clock);
    }

    /**
     * Non-monotonic implementation - no synchronization for maximum performance.
     */
    private static UUID generateNonMonotonic(LongSupplier clock) {
        long timestamp = clock.getAsLong();

        // Use random bits for counter field (rand_a)
        int counterValue = ThreadLocalRandom.current().nextInt(COUNTER_MAX + 1);

        // Generate random bytes for the least significant bits (rand_b)
        byte[] randomBytes = new byte[8];
        ThreadLocalRandom.current().nextBytes(randomBytes);

        return buildUuid(timestamp, counterValue, randomBytes);
    }

    /**
     * Generates a new UUID v7 using the current system time with monotonic ordering.
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
    public static synchronized UUID generateMonotonic() {
        return generateMonotonicImpl(System::currentTimeMillis);
    }

    /**
     * Generates a new UUID v7 using a custom clock source with monotonic ordering.
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
    public static synchronized UUID generateMonotonic(LongSupplier clock) {
        return generateMonotonicImpl(clock);
    }

    /**
     * Monotonic implementation - synchronized to ensure ordering.
     * This method must only be called from synchronized methods.
     */
    private static UUID generateMonotonicImpl(LongSupplier clock) {
        long timestamp = clock.getAsLong();
        int counterValue;

        if (timestamp == monotonicGeneratorLastTimestamp) {
            // Same millisecond - increment counter
            monotonicGeneratorCounter = (monotonicGeneratorCounter + 1) & COUNTER_MAX;

            if (monotonicGeneratorCounter == 0) {
                // Counter overflow - wait for next millisecond to maintain uniqueness
                do {
                    timestamp = clock.getAsLong();
                } while (timestamp == monotonicGeneratorLastTimestamp);

                // New millisecond - start with random counter value
                monotonicGeneratorCounter = SECURE_RANDOM.nextInt(COUNTER_MAX + 1);
            }
            counterValue = monotonicGeneratorCounter;
        } else {
            // New millisecond - start with random counter value for unpredictability
            monotonicGeneratorCounter = SECURE_RANDOM.nextInt(COUNTER_MAX + 1);
            counterValue = monotonicGeneratorCounter;
            monotonicGeneratorLastTimestamp = timestamp;
        }

        // Generate random bytes for the least significant bits (rand_b)
        byte[] randomBytes = new byte[8];
        ThreadLocalRandom.current().nextBytes(randomBytes);

        return buildUuid(timestamp, counterValue, randomBytes);
    }

    /**
     * Builds a UUID v7 from timestamp, counter, and random bytes.
     */
    private static UUID buildUuid(long timestamp, int counterValue, byte[] randomBytes) {
        // Layout of UUID v7 (RFC 9562):
        //
        // Most significant 64 bits:
        //   48 bits: unix_ts_ms (timestamp in milliseconds)
        //   4 bits:  ver (version = 7)
        //   12 bits: rand_a (counter for monotonicity or random)
        //
        // Least significant 64 bits:
        //   2 bits:  var (variant = 10)
        //   62 bits: rand_b (random data)

        long mostSigBits = (timestamp << 16) | counterValue;

        // Set version to 7 (0111 in bits 48-51)
        mostSigBits = (mostSigBits & 0xFFFFFFFFFFFF0FFFL) | 0x0000000000007000L;

        // Build least significant bits from random bytes
        long leastSigBits = 0L;
        for (int i = 0; i < 8; i++) {
            leastSigBits = (leastSigBits << 8) | (randomBytes[i] & 0xFFL);
        }

        // Set variant to 10 (RFC 4122) in bits 64-65
        leastSigBits = (leastSigBits & 0x3FFFFFFFFFFFFFFFL) | 0x8000000000000000L;

        return new UUID(mostSigBits, leastSigBits);
    }

    /**
     * Extracts the timestamp component from a UUID v7.
     *
     * @param uuid the UUID v7 to extract the timestamp from
     * @return the timestamp in milliseconds since Unix epoch
     * @throws IllegalArgumentException if uuid is null or not a v7 UUID
     */
    public static long getTimestamp(UUID uuid) {
        if (uuid == null) {
            throw new IllegalArgumentException("UUID cannot be null");
        }

        if (uuid.version() != 7) {
            throw new IllegalArgumentException("UUID is not version 7 (got version " + uuid.version() + ")");
        }

        // Extract 48-bit timestamp from most significant bits
        return uuid.getMostSignificantBits() >>> 16;
    }
}
