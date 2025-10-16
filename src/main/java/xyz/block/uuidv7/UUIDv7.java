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
        return generate(System::currentTimeMillis);
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
        ThreadLocalRandom random = ThreadLocalRandom.current();

        int randA = random.nextInt(4096);
        long randB = random.nextLong();

        return build(timestamp, randA, randB);
    }

    /**
     * Builds a UUID v7 from timestamp and random components.
     * Package-private to allow use by {@link MonotonicUUIDv7}.
     *
     * @param timestamp the timestamp in milliseconds since Unix epoch
     * @param randA the random or counter value for bits 52-63 (12 bits)
     * @param randB the random value for bits 66-127 (62 bits, variant will be set)
     * @return a new UUID v7 instance
     */
    static UUID build(long timestamp, int randA, long randB) {
        long mostSigBits = (timestamp << 16) | (randA & 0xFFFL);
        long leastSigBits = randB;

        // Set version to 7 (0111 in bits 48-51)
        mostSigBits = (mostSigBits & 0xFFFFFFFFFFFF0FFFL) | 0x0000000000007000L;

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
