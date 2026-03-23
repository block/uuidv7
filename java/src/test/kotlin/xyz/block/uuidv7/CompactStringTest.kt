package xyz.block.uuidv7

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import xyz.block.uuidv7.UUIDv7.compactString
import java.util.UUID

class CompactStringTest {

    @Test
    fun generateCompactStringProducesFixedLength() {
        val uuid = UUIDv7.generate()
        val compactString = uuid.compactString

        assertThat(compactString).hasSize(22)
    }

    @Test
    fun fromCompactStringThrowsOnInvalidLength() {
        assertThatThrownBy { UUIDv7.fromCompactString("tooshort") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("must be exactly 22 characters")
    }

    @Test
    fun fromCompactStringThrowsOnInvalidCharacter() {
        assertThatThrownBy { UUIDv7.fromCompactString("invalid@characters1234") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Invalid compact string character")
    }

    @Test
    fun roundTripConversion() {
        val original = UUIDv7.generate()
        val compactString = original.compactString
        val decoded = UUIDv7.fromCompactString(compactString)

        assertThat(decoded).isEqualTo(original)
    }

    @Test
    fun roundTripWithMultipleUuids() {
        for (i in 0 until 1000) {
            val original = UUIDv7.generate()
            val compactString = original.compactString
            val decoded = UUIDv7.fromCompactString(compactString)

            assertThat(decoded).isEqualTo(original)
        }
    }

    @Test
    fun roundTripWithMonotonicUuids() {
        for (i in 0 until 1000) {
            val original = MonotonicUUIDv7.generate()
            val compactString = original.compactString
            val decoded = UUIDv7.fromCompactString(compactString)

            assertThat(decoded).isEqualTo(original)
        }
    }

    @Test
    fun zeroUuidConvertsCorrectly() {
        val zero = UUID(0L, 0L)
        val compactString = zero.compactString

        assertThat(compactString).isEqualTo("0000000000000000000000")
        assertThat(UUIDv7.fromCompactString(compactString)).isEqualTo(zero)
    }

    @Test
    fun maxUuidConvertsCorrectly() {
        val max = UUID(-1L, -1L)
        val compactString = max.compactString

        assertThat(compactString).hasSize(22)
        assertThat(UUIDv7.fromCompactString(compactString)).isEqualTo(max)
    }

    @Test
    fun preservesLexicographicOrderingForTimeOrderedUuids() {
        // Generate UUIDs with increasing timestamps
        val uuids = mutableListOf<UUID>()
        val compactStringStrings = mutableListOf<String>()

        for (ts in 1000000000000L until 1000000001000L step 100) {
            val uuid = UUIDv7.generate { ts }
            uuids.add(uuid)
            compactStringStrings.add(uuid.compactString)
        }

        // UUIDs should be in order
        val sortedUuids = uuids.sorted()
        assertThat(uuids).isEqualTo(sortedUuids)

        // CompactString strings should also be in lexicographic order
        val sortedCompactString = compactStringStrings.sorted()
        assertThat(compactStringStrings).isEqualTo(sortedCompactString)
    }

    @Test
    fun differentUuidsProduceDifferentCompactString() {
        val uuid1 = UUIDv7.generate()
        val uuid2 = UUIDv7.generate()

        val compactString1 = uuid1.compactString
        val compactString2 = uuid2.compactString

        assertThat(compactString1).isNotEqualTo(compactString2)
    }

    @Test
    fun onlyUsesCompactStringAlphabet() {
        for (i in 0 until 100) {
            val uuid = UUIDv7.generate()
            val compactString = uuid.compactString

            assertThat(compactString).matches("[0-9A-Za-z]{22}")
        }
    }

    @Test
    fun knownValueConversion() {
        // Test with a known UUID value
        val known = UUID.fromString("01234567-89ab-7def-8012-3456789abcde")
        val compactString = known.compactString

        // Should round-trip correctly
        val decoded = UUIDv7.fromCompactString(compactString)
        assertThat(decoded).isEqualTo(known)
    }
}
