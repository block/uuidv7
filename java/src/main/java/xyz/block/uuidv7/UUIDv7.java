package xyz.block.uuidv7;

import java.math.BigInteger;
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
     * Generates a new UUID v7 as a compact string using the current system time.
     * <p>
     * Equivalent to calling {@code toCompactString(generate())}.
     * Returns a 22-character Base62 encoded string that preserves time-ordering.
     *
     * @return a 22-character compact string representation of a new UUID v7
     */
    public static String generateCompactString() {
        return toCompactString(generate());
    }

    /**
     * Generates a new UUID v7 as a compact string using a custom clock source.
     * <p>
     * Equivalent to calling {@code toCompactString(generate(clock))}.
     * Returns a 22-character Base62 encoded string that preserves time-ordering.
     *
     * @param clock a supplier that returns the current time in milliseconds since Unix epoch
     * @return a 22-character compact string representation of a new UUID v7
     */
    public static String generateCompactString(LongSupplier clock) {
        return toCompactString(generate(clock));
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
        // Defensively mask randA to ensure it fits in 12 bits
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

    private static final String BASE62_ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int BASE62_LENGTH = 22;
    private static final BigInteger BASE62 = BigInteger.valueOf(62);

    /**
     * Converts a UUID to a compact string representation.
     * <p>
     * The resulting string is exactly 22 characters long and preserves lexicographic
     * ordering for UUID v7 values (time-ordered UUIDs will sort correctly as compact strings).
     * Uses Base62 encoding (0-9, A-Z, a-z).
     *
     * @param uuid the UUID to encode
     * @return a 22-character compact string representation
     * @throws IllegalArgumentException if uuid is null
     */
    public static String toCompactString(UUID uuid) {
        if (uuid == null) {
            throw new IllegalArgumentException("UUID cannot be null");
        }

        // Convert UUID to BigInteger (unsigned 128-bit value)
        byte[] bytes = new byte[17];
        bytes[0] = 0; // Ensure positive (unsigned)

        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();

        // Fill bytes array in big-endian order
        for (int i = 0; i < 8; i++) {
            bytes[1 + i] = (byte) (msb >>> (56 - i * 8));
            bytes[9 + i] = (byte) (lsb >>> (56 - i * 8));
        }

        BigInteger value = new BigInteger(bytes);

        // Convert to Base62
        StringBuilder sb = new StringBuilder(BASE62_LENGTH);
        while (value.compareTo(BigInteger.ZERO) > 0) {
            BigInteger[] divmod = value.divideAndRemainder(BASE62);
            sb.append(BASE62_ALPHABET.charAt(divmod[1].intValue()));
            value = divmod[0];
        }

        // Pad with leading zeros to ensure fixed length
        while (sb.length() < BASE62_LENGTH) {
            sb.append('0');
        }

        // Reverse to get most significant digit first
        return sb.reverse().toString();
    }

    /**
     * Decodes a compact string representation back to a UUID.
     *
     * @param compactString the compact string to decode (must be 22 characters)
     * @return the decoded UUID
     * @throws IllegalArgumentException if compactString is null, not 22 characters, or contains invalid characters
     */
    public static UUID fromCompactString(String compactString) {
        if (compactString == null) {
            throw new IllegalArgumentException("Compact string cannot be null");
        }

        if (compactString.length() != BASE62_LENGTH) {
            throw new IllegalArgumentException("Compact string must be exactly " + BASE62_LENGTH + " characters (got " + compactString.length() + ")");
        }

        // Convert Base62 to BigInteger
        BigInteger value = BigInteger.ZERO;

        for (int i = 0; i < BASE62_LENGTH; i++) {
            char c = compactString.charAt(i);
            int digit = BASE62_ALPHABET.indexOf(c);

            if (digit < 0) {
                throw new IllegalArgumentException("Invalid compact string character: " + c);
            }

            value = value.multiply(BASE62).add(BigInteger.valueOf(digit));
        }

        // Convert BigInteger to UUID
        byte[] bytes = value.toByteArray();

        // Handle cases where BigInteger might have extra padding or be shorter
        long msb = 0;
        long lsb = 0;

        // Read from the end of the byte array (big-endian)
        int offset = bytes.length - 1;

        // Read lsb (last 8 bytes)
        for (int i = 0; i < 8 && offset >= 0; i++) {
            lsb = (lsb << 8) | (bytes[offset--] & 0xFF);
        }
        // Reverse the lsb since we read backwards
        lsb = Long.reverseBytes(lsb);

        // Read msb (next 8 bytes)
        for (int i = 0; i < 8 && offset >= 0; i++) {
            msb = (msb << 8) | (bytes[offset--] & 0xFF);
        }
        // Reverse the msb since we read backwards
        msb = Long.reverseBytes(msb);

        return new UUID(msb, lsb);
    }
}
