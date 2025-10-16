package xyz.block.uuidv7;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.LongSupplier;

/**
 * Utility class for generating and working with UUID v7 identifiers.
 * <p>
 * UUID v7 is a time-ordered UUID format that encodes a Unix timestamp in milliseconds
 * in the most significant 48 bits, making UUIDs naturally sortable by creation time.
 * This implementation follows RFC 9562.
 * <p>
 * This class uses ThreadLocalRandom for maximum performance with no synchronization overhead.
 * UUIDs generated in the same millisecond may not be strictly ordered, but uniqueness
 * is guaranteed through random bits. For monotonic ordering guarantees, use
 * {@link MonotonicUUIDv7} instead.
 */
public final class UUIDv7 {

    // Counter occupies 12 bits (rand_a in RFC 9562)
    static final int COUNTER_BITS = 12;
    static final int COUNTER_MAX = (1 << COUNTER_BITS) - 1; // 0xFFF

    private UUIDv7() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Generates a new UUID v7 using the current system time.
     * <p>
     * Uses ThreadLocalRandom for maximum performance with no synchronization overhead.
     * UUIDs generated in the same millisecond may not be strictly ordered, but uniqueness
     * is guaranteed through random bits.
     *
     * @return a new UUID v7 instance
     */
    public static UUID generate() {
        long timestamp = System.currentTimeMillis();

        // Use random bits for counter field (rand_a)
        int counterValue = ThreadLocalRandom.current().nextInt(COUNTER_MAX + 1);

        // Generate random bytes for the least significant bits (rand_b)
        byte[] randomBytes = new byte[8];
        ThreadLocalRandom.current().nextBytes(randomBytes);

        return buildUuid(timestamp, counterValue, randomBytes);
    }

    /**
     * Generates a new UUID v7 using a custom clock source.
     * <p>
     * Uses ThreadLocalRandom for maximum performance with no synchronization overhead.
     * Useful for testing or specialized use cases where you need control over the timestamp.
     *
     * @param clock a supplier that returns the current time in milliseconds since Unix epoch
     * @return a new UUID v7 instance
     */
    public static UUID generate(LongSupplier clock) {
        long timestamp = clock.getAsLong();

        // Use random bits for counter field (rand_a)
        int counterValue = ThreadLocalRandom.current().nextInt(COUNTER_MAX + 1);

        // Generate random bytes for the least significant bits (rand_b)
        byte[] randomBytes = new byte[8];
        ThreadLocalRandom.current().nextBytes(randomBytes);

        return buildUuid(timestamp, counterValue, randomBytes);
    }

    /**
     * Builds a UUID v7 from timestamp, counter, and random bytes.
     * Package-private to allow use by {@link MonotonicUUIDv7}.
     */
    static UUID buildUuid(long timestamp, int counterValue, byte[] randomBytes) {
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
