package xyz.block.veeseven;

import java.security.SecureRandom;
import java.util.UUID;
import java.util.function.LongSupplier;

/**
 * Utility class for generating and working with UUID v7 identifiers.
 * <p>
 * UUID v7 is a time-ordered UUID format that encodes a Unix timestamp in milliseconds
 * in the most significant 48 bits, making UUIDs naturally sortable by creation time.
 * This implementation follows RFC 9562.
 */
public final class UuidV7 {
    private static final SecureRandom RANDOM = new SecureRandom();
    
    private UuidV7() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
    
    /**
     * Generates a new UUID v7 using the current system time.
     *
     * @return a new UUID v7 instance
     */
    public static UUID generate() {
        return generate(System::currentTimeMillis);
    }
    
    /**
     * Generates a new UUID v7 using a custom clock source.
     * <p>
     * This is useful for testing or specialized use cases where you need control
     * over the timestamp component of the UUID.
     *
     * @param clock a supplier that returns the current time in milliseconds since Unix epoch
     * @return a new UUID v7 instance
     */
    public static UUID generate(LongSupplier clock) {
        long timestamp = clock.getAsLong();
        
        byte[] randomBytes = new byte[10];
        RANDOM.nextBytes(randomBytes);
        
        long mostSigBits = (timestamp << 16) | ((randomBytes[0] & 0xFFL) << 8) | (randomBytes[1] & 0xFFL);
        
        mostSigBits = (mostSigBits & 0xFFFFFFFFFFFF0FFFL) | 0x0000000000007000L;
        
        long leastSigBits = 0L;
        for (int i = 2; i < 10; i++) {
            leastSigBits = (leastSigBits << 8) | (randomBytes[i] & 0xFFL);
        }
        
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
        
        return uuid.getMostSignificantBits() >>> 16;
    }
}
