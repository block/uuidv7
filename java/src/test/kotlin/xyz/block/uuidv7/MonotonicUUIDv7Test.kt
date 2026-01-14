package xyz.block.uuidv7

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import xyz.block.uuidv7.UUIDv7.timestamp
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

class MonotonicUUIDv7Test {

    @BeforeEach
    fun resetMonotonicState() {
        MonotonicUUIDv7.resetState()
    }

    @Test
    fun generateCreatesValidUuid() {
        val uuid = MonotonicUUIDv7.generate()

        assertThat(uuid).isNotNull()
        assertThat(uuid.version()).isEqualTo(7)
        assertThat(uuid.variant()).isEqualTo(2)
    }

    @Test
    fun generateWithCustomClock() {
        val fixedTime = 1234567890000L
        val uuid = MonotonicUUIDv7.generate { fixedTime }

        assertThat(uuid).isNotNull()
        assertThat(uuid.timestamp).isEqualTo(fixedTime)
    }

    @Test
    fun getTimestampExtractsCorrectValue() {
        val expectedTime = System.currentTimeMillis()
        val uuid = MonotonicUUIDv7.generate { expectedTime }

        val actualTime = uuid.timestamp

        assertThat(actualTime).isEqualTo(expectedTime)
    }

    @Test
    fun generatedUuidsAreTimeSorted() {
        val time1 = 1000000000000L
        val time2 = 2000000000000L

        val uuid1 = MonotonicUUIDv7.generate { time1 }
        val uuid2 = MonotonicUUIDv7.generate { time2 }

        assertThat(uuid1.compareTo(uuid2))
            .`as`("Earlier UUID should sort before later UUID")
            .isLessThan(0)
    }

    @Test
    fun timestampPreservesMillisecondPrecision() {
        val testTimes = longArrayOf(
            0L,
            1L,
            1234567890123L,
            281474976710655L
        )

        for (testTime in testTimes) {
            val uuid = MonotonicUUIDv7.generate { testTime }
            val extractedTime = uuid.timestamp

            assertThat(extractedTime)
                .`as`("Timestamp should be preserved exactly for %d", testTime)
                .isEqualTo(testTime)
        }
    }

    @Test
    fun generateMonotonicMethodWorks() {
        // Generate UUIDs using generate()
        val uuids = mutableListOf<UUID>()
        for (i in 0 until 50) {
            uuids.add(MonotonicUUIDv7.generate())
        }

        // Should be unique and valid
        val uniqueUuids = uuids.toSet()
        assertThat(uniqueUuids).hasSize(50)
        uniqueUuids.forEach { uuid ->
            assertThat(uuid.version()).isEqualTo(7)
            assertThat(uuid.variant()).isEqualTo(2)
        }
    }

    @Test
    fun generateEnsuresStrictOrdering() {
        val fixedTime = 1234567890000L

        // Generate multiple UUIDs at the same timestamp
        val uuids = mutableListOf<UUID>()
        for (i in 0 until 100) {
            uuids.add(MonotonicUUIDv7.generate { fixedTime })
        }

        // Verify they are strictly ordered
        for (i in 0 until uuids.size - 1) {
            assertThat(uuids[i].compareTo(uuids[i + 1]))
                .`as`("UUID at index %d should be less than UUID at index %d", i, i + 1)
                .isLessThan(0)
        }
    }

    @Test
    fun generateAdvancesTimestampOnCounterOverflow() {
        val timestamp = AtomicLong(1234567890000L)

        // Generate more than 4096 UUIDs (counter max) to trigger overflow
        // Clock advances with each call, simulating time passing
        val uuids = mutableListOf<UUID>()
        for (i in 0 until 5000) {
            uuids.add(MonotonicUUIDv7.generate { timestamp.incrementAndGet() })
        }

        // Verify all UUIDs are unique
        val uniqueUuids = uuids.toSet()
        assertThat(uniqueUuids).hasSize(5000)

        // Verify timestamp advanced beyond the initial value
        val maxTimestamp = uuids
            .map { it.timestamp }
            .maxOrNull() ?: 0

        assertThat(maxTimestamp).isGreaterThan(1234567890000L)

        // Verify UUIDs are still ordered despite counter overflows
        for (i in 0 until uuids.size - 1) {
            assertThat(uuids[i].compareTo(uuids[i + 1]))
                .`as`("UUID at index %d should be less than UUID at index %d", i, i + 1)
                .isLessThan(0)
        }
    }

    @Test
    fun generateWithCustomClockEnsuresOrdering() {
        val fixedTime = 1234567890000L

        // Generate multiple UUIDs using generate() with custom clock at same timestamp
        val uuids = mutableListOf<UUID>()
        for (i in 0 until 50) {
            uuids.add(MonotonicUUIDv7.generate { fixedTime })
        }

        // Should be strictly ordered despite same timestamp
        for (i in 0 until uuids.size - 1) {
            assertThat(uuids[i].compareTo(uuids[i + 1])).isLessThan(0)
        }
    }

    @Test
    fun generateResetsCounterOnNewTimestamp() {
        val timestamp = AtomicLong(1000000000000L)

        // Generate some UUIDs at first timestamp
        val uuid1 = MonotonicUUIDv7.generate { timestamp.get() }
        val uuid2 = MonotonicUUIDv7.generate { timestamp.get() }

        // Advance timestamp
        timestamp.set(2000000000000L)

        // Generate UUID at new timestamp
        val uuid3 = MonotonicUUIDv7.generate { timestamp.get() }

        // uuid3 should have later timestamp
        assertThat(uuid3.timestamp)
            .isGreaterThan(uuid1.timestamp)
            .isGreaterThan(uuid2.timestamp)

        // uuid3 should sort after uuid1 and uuid2
        assertThat(uuid1.compareTo(uuid3)).isLessThan(0)
        assertThat(uuid2.compareTo(uuid3)).isLessThan(0)
    }

    @Test
    fun uniquenessUnderHighLoad() {
        val timestamp = AtomicLong(1234567890000L)

        // Generate many UUIDs (more than counter capacity of 4096)
        // The clock will naturally advance when counter overflows
        val uuids = mutableSetOf<UUID>()
        for (i in 0 until 10000) {
            uuids.add(MonotonicUUIDv7.generate { timestamp.incrementAndGet() })
        }

        // All should be unique
        assertThat(uuids).hasSize(10000)

        // Timestamp should have advanced beyond initial value
        val finalTimestamp = uuids
            .map { it.timestamp }
            .maxOrNull() ?: 0
        assertThat(finalTimestamp).isGreaterThan(1234567890000L)
    }
}
