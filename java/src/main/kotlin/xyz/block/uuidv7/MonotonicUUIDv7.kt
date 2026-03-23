package xyz.block.uuidv7

import java.security.SecureRandom
import java.util.UUID
import java.util.concurrent.ThreadLocalRandom
import java.util.function.LongSupplier

/**
 * Utility object for generating monotonic UUID v7 identifiers.
 *
 * This implementation ensures that UUIDs generated within the same millisecond are strictly
 * ordered by incrementing a counter. This provides guaranteed sequential ordering, making it
 * ideal for database primary keys and scenarios requiring chronological order guarantees.
 *
 * UUID v7 is a time-ordered UUID format that encodes a Unix timestamp in milliseconds
 * in the most significant 48 bits, making UUIDs naturally sortable by creation time.
 * This implementation follows RFC 9562 with monotonic counter support.
 *
 * All generation methods are synchronized to ensure strict ordering across threads.
 * If the counter overflows within a millisecond (after 4096 UUIDs), the method will
 * block until the next millisecond to maintain uniqueness.
 */
public object MonotonicUUIDv7 {

    private const val COUNTER_MAX = 0xFFF // 12 bits = 4095
    private val SECURE_RANDOM = SecureRandom()

    // Monotonicity state (guarded by object lock in synchronized methods)
    private var lastTimestamp = 0L
    private var counter = 0

    /**
     * Resets internal monotonic state. Package-private for testing.
     */
    @Synchronized
    internal fun resetState() {
        lastTimestamp = 0L
        counter = 0
    }

    /**
     * Generates a new monotonic UUID v7 using the current system time.
     *
     * This method ensures that UUIDs generated within the same millisecond are strictly
     * ordered by incrementing a counter. If the counter overflows within a millisecond,
     * the method will block until the next millisecond to maintain uniqueness.
     *
     * This method is synchronized and best suited for database primary keys and scenarios
     * requiring guaranteed sequential ordering.
     *
     * @return a new UUID v7 instance
     */
    @JvmStatic
    @Synchronized
    public fun generate(): UUID = generate(System::currentTimeMillis)

    /**
     * Generates a new monotonic UUID v7 using a custom clock source.
     *
     * This method ensures that UUIDs generated within the same millisecond are strictly
     * ordered by incrementing a counter. Useful for testing monotonic behavior with
     * controlled clock sources.
     *
     * This method is synchronized and best suited for database primary keys and scenarios
     * requiring guaranteed sequential ordering.
     *
     * @param clock a supplier that returns the current time in milliseconds since Unix epoch
     * @return a new UUID v7 instance
     */
    @JvmStatic
    @Synchronized
    public fun generate(clock: LongSupplier): UUID {
        var timestamp = clock.asLong
        val counterValue: Int

        if (timestamp <= lastTimestamp) {
            // Same millisecond or clock went backward - clamp and increment counter
            timestamp = lastTimestamp
            counter = (counter + 1) and COUNTER_MAX

            if (counter == 0) {
                // Counter overflow - wait for next millisecond to maintain uniqueness
                do {
                    timestamp = clock.asLong
                } while (timestamp <= lastTimestamp)

                // New millisecond - update state and start with random counter value
                lastTimestamp = timestamp
                counter = SECURE_RANDOM.nextInt(COUNTER_MAX + 1)
            }
            counterValue = counter
        } else {
            // New millisecond - start with random counter value for unpredictability
            counter = SECURE_RANDOM.nextInt(COUNTER_MAX + 1)
            counterValue = counter
            lastTimestamp = timestamp
        }

        val randB = ThreadLocalRandom.current().nextLong()

        return UUIDv7.build(timestamp, counterValue, randB)
    }
}
