package xyz.block.uuidv7

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import xyz.block.uuidv7.UUIDv7.timestamp
import java.util.UUID

class UUIDv7Test {

    @Test
    fun generateCreatesValidUuid() {
        val uuid = UUIDv7.generate()

        assertThat(uuid).isNotNull()
        assertThat(uuid.version()).isEqualTo(7)
        assertThat(uuid.variant()).isEqualTo(2)
    }

    @Test
    fun generateWithCustomClock() {
        val fixedTime = 1234567890000L
        val uuid = UUIDv7.generate { fixedTime }

        assertThat(uuid).isNotNull()
        assertThat(uuid.timestamp).isEqualTo(fixedTime)
    }

    @Test
    fun getTimestampExtractsCorrectValue() {
        val expectedTime = System.currentTimeMillis()
        val uuid = UUIDv7.generate { expectedTime }

        val actualTime = uuid.timestamp

        assertThat(actualTime).isEqualTo(expectedTime)
    }

    @Test
    fun getTimestampThrowsOnNonV7Uuid() {
        val v4Uuid = UUID.randomUUID()

        assertThatThrownBy { v4Uuid.timestamp }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("not version 7")
    }

    @Test
    fun generatedUuidsAreUnique() {
        val uuids = mutableSetOf<UUID>()

        for (i in 0 until 10000) {
            val uuid = UUIDv7.generate()
            assertThat(uuids.add(uuid))
                .`as`("UUID should be unique")
                .isTrue()
        }
    }

    @Test
    fun toStringProducesStandardFormat() {
        val uuid = UUIDv7.generate()
        val str = uuid.toString()

        assertThat(str).matches("[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}")
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
            val uuid = UUIDv7.generate { testTime }
            val extractedTime = uuid.timestamp

            assertThat(extractedTime)
                .`as`("Timestamp should be preserved exactly for %d", testTime)
                .isEqualTo(testTime)
        }
    }

    @Test
    fun generateMethodWorks() {
        // Generate UUIDs using generate()
        val uuids = mutableSetOf<UUID>()
        for (i in 0 until 1000) {
            val uuid = UUIDv7.generate()
            assertThat(uuid).isNotNull()
            assertThat(uuid.version()).isEqualTo(7)
            assertThat(uuid.variant()).isEqualTo(2)
            uuids.add(uuid)
        }

        // Should be unique
        assertThat(uuids).hasSize(1000)
    }

    @Test
    fun generateDoesNotBlockOrGuaranteeOrdering() {
        val fixedTime = 1234567890000L

        // Generate UUIDs
        val uuids = mutableSetOf<UUID>()
        for (i in 0 until 1000) {
            val uuid = UUIDv7.generate { fixedTime }
            assertThat(uuid).isNotNull()
            assertThat(uuid.timestamp).isEqualTo(fixedTime)
            uuids.add(uuid)
        }

        // Should still be unique
        assertThat(uuids).hasSize(1000)
    }

    @Test
    fun uniquenessUnderHighLoad() {
        val fixedTime = 1234567890000L

        // Generate many UUIDs at the same timestamp
        val uuids = mutableSetOf<UUID>()
        for (i in 0 until 10000) {
            uuids.add(UUIDv7.generate { fixedTime })
        }

        // Should still be unique (random bits provide uniqueness)
        assertThat(uuids).hasSize(10000)
    }
}
