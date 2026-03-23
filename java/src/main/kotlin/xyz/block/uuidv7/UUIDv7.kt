package xyz.block.uuidv7

import java.math.BigInteger
import java.util.UUID
import java.util.concurrent.ThreadLocalRandom
import java.util.function.LongSupplier
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

/**
 * Utility object for generating and working with UUID v7 identifiers.
 *
 * UUID v7 is a time-ordered [UUID] format that encodes a Unix timestamp in milliseconds
 * in the most significant 48 bits, making UUIDs naturally sortable by creation time.
 * This implementation follows RFC 9562.
 *
 * This class uses [ThreadLocalRandom] for maximum performance with no synchronization overhead.
 * UUIDs generated in the same millisecond may not be strictly ordered, but uniqueness
 * is guaranteed through random bits. For monotonic ordering guarantees, use
 * [MonotonicUUIDv7] instead.
 */
public object UUIDv7 {

    @JvmStatic
    private val BASE62_ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
    @JvmStatic
    private val BASE62_LENGTH = 22
    @JvmStatic
    private val BASE62 = BigInteger.valueOf(62)

    /**
     * Generates a new UUID v7 using the current system time.
     *
     * Uses ThreadLocalRandom for maximum performance with no synchronization overhead.
     * UUIDs generated in the same millisecond may not be strictly ordered, but uniqueness
     * is guaranteed through random bits.
     *
     * @return a new UUID v7 instance
     */
    @OptIn(ExperimentalUuidApi::class)
    @JvmStatic
    public fun generate(): UUID = Uuid.generateV7().toJavaUuid()

    /**
     * Generates a new UUID v7 using a custom clock source.
     *
     * Uses ThreadLocalRandom for maximum performance with no synchronization overhead.
     * Useful for testing or specialized use cases where you need control over the timestamp.
     *
     * @param clock a supplier that returns the current time in milliseconds since Unix epoch
     * @return a new UUID v7 instance
     */
    @JvmStatic
    public fun generate(clock: LongSupplier): UUID {
        val timestamp = clock.asLong
        val random = ThreadLocalRandom.current()

        val randA = random.nextInt(4096)
        val randB = random.nextLong()

        return build(timestamp, randA, randB)
    }

    /**
     * Generates a new UUID v7 as a compact string using the current system time.
     *
     * Equivalent to calling `generate().compactString`.
     * Returns a 22-character Base62 encoded string that preserves time-ordering.
     *
     * @return a 22-character compact string representation of a new UUID v7
     */
    @JvmStatic
    public fun generateCompactString(): String = generate().compactString

    /**
     * Generates a new UUID v7 as a compact string using a custom clock source.
     *
     * Equivalent to calling `generate(clock).compactString`.
     * Returns a 22-character Base62 encoded string that preserves time-ordering.
     *
     * @param clock a supplier that returns the current time in milliseconds since Unix epoch
     * @return a 22-character compact string representation of a new UUID v7
     */
    @JvmStatic
    public fun generateCompactString(clock: LongSupplier): String = generate(clock).compactString

    /**
     * Decodes a compact string representation back to a UUID.
     *
     * @param compactString the compact string to decode (must be 22 characters)
     * @return the decoded UUID
     * @throws IllegalArgumentException if compactString is not 22 characters or contains invalid characters
     */
    @JvmStatic
    public fun fromCompactString(compactString: String): UUID {
        require(compactString.length == BASE62_LENGTH) {
            "Compact string must be exactly $BASE62_LENGTH characters (got ${compactString.length})"
        }

        // Convert Base62 to BigInteger
        var value = BigInteger.ZERO

        for (i in 0 until BASE62_LENGTH) {
            val c = compactString[i]
            val digit = BASE62_ALPHABET.indexOf(c)

            require(digit >= 0) { "Invalid compact string character: $c" }

            value = value.multiply(BASE62).add(BigInteger.valueOf(digit.toLong()))
        }

        // Convert BigInteger to UUID
        val bytes = value.toByteArray()

        // Handle cases where BigInteger might have extra padding or be shorter
        var msb = 0L
        var lsb = 0L

        // Read from the end of the byte array (big-endian)
        var offset = bytes.size - 1

        // Read lsb (last 8 bytes)
        for (i in 0 until 8) {
            if (offset < 0) break
            lsb = (lsb shl 8) or (bytes[offset--].toLong() and 0xFF)
        }
        // Reverse the lsb since we read backwards
        lsb = java.lang.Long.reverseBytes(lsb)

        // Read msb (next 8 bytes)
        for (i in 0 until 8) {
            if (offset < 0) break
            msb = (msb shl 8) or (bytes[offset--].toLong() and 0xFF)
        }
        // Reverse the msb since we read backwards
        msb = java.lang.Long.reverseBytes(msb)

        return UUID(msb, lsb)
    }

    /**
     * Builds a UUID v7 from timestamp and random components.
     * Internal to allow use by [MonotonicUUIDv7].
     *
     * @param timestamp the timestamp in milliseconds since Unix epoch
     * @param randA the random or counter value for bits 52-63 (12 bits)
     * @param randB the random value for bits 66-127 (62 bits, variant will be set)
     * @return a new UUID v7 instance
     */
    @JvmStatic
    internal fun build(timestamp: Long, randA: Int, randB: Long): UUID {
        // Defensively mask randA to ensure it fits in 12 bits
        var mostSigBits = (timestamp shl 16) or (randA.toLong() and 0xFFFL)
        var leastSigBits = randB

        // Set version to 7 (0111 in bits 48-51)
        mostSigBits = (mostSigBits and 0xFFFFFFFFFFFF0FFFUL.toLong()) or 0x0000000000007000L

        // Set variant to 10 (RFC 4122) in bits 64-65
        leastSigBits = (leastSigBits and 0x3FFFFFFFFFFFFFFFL) or 0x8000000000000000UL.toLong()

        return UUID(mostSigBits, leastSigBits)
    }

    /**
     * Extension property to extract the timestamp from a UUID v7.
     *
     * @throws IllegalArgumentException if not a v7 UUID
     */
    @JvmStatic
    public val UUID.timestamp: Long
        get() {
            require(version() == 7) { "UUID is not version 7 (got version ${version()})" }
            return mostSignificantBits ushr 16
        }

    /**
     * Extension property to convert a UUID to a compact string representation.
     *
     * The resulting string is exactly 22 characters long and preserves lexicographic
     * ordering for UUID v7 values (time-ordered UUIDs will sort correctly as compact strings).
     * Uses Base62 encoding (0-9, A-Z, a-z).
     */
    @JvmStatic
    @get:JvmName("toCompactString")
    public val UUID.compactString: String
        get() {
            // Convert UUID to BigInteger (unsigned 128-bit value)
            val bytes = ByteArray(17)
            bytes[0] = 0 // Ensure positive (unsigned)

            val msb = mostSignificantBits
            val lsb = leastSignificantBits

            // Fill bytes array in big-endian order
            for (i in 0 until 8) {
                bytes[1 + i] = (msb ushr (56 - i * 8)).toByte()
                bytes[9 + i] = (lsb ushr (56 - i * 8)).toByte()
            }

            var value = BigInteger(bytes)

            // Convert to Base62
            val sb = StringBuilder(BASE62_LENGTH)
            while (value > BigInteger.ZERO) {
                val divmod = value.divideAndRemainder(BASE62)
                sb.append(BASE62_ALPHABET[divmod[1].toInt()])
                value = divmod[0]
            }

            // Pad with leading zeros to ensure fixed length
            while (sb.length < BASE62_LENGTH) {
                sb.append('0')
            }

            // Reverse to get most significant digit first
            return sb.reverse().toString()
        }
}
