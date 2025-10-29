package xyz.block.uuidv7;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CompactStringTest {

    @Test
    void generateCompactStringProducesFixedLength() {
        UUID uuid = UUIDv7.generate();
        String compactString = UUIDv7.toCompactString(uuid);

        assertThat(compactString).hasSize(22);
    }

    @Test
    void toCompactStringThrowsOnNull() {
        assertThatThrownBy(() -> UUIDv7.toCompactString(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("UUID cannot be null");
    }

    @Test
    void fromCompactStringThrowsOnNull() {
        assertThatThrownBy(() -> UUIDv7.fromCompactString(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Compact string cannot be null");
    }

    @Test
    void fromCompactStringThrowsOnInvalidLength() {
        assertThatThrownBy(() -> UUIDv7.fromCompactString("tooshort"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must be exactly 22 characters");
    }

    @Test
    void fromCompactStringThrowsOnInvalidCharacter() {
        assertThatThrownBy(() -> UUIDv7.fromCompactString("invalid@characters1234"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid compact string character");
    }

    @Test
    void roundTripConversion() {
        UUID original = UUIDv7.generate();
        String compactString = UUIDv7.toCompactString(original);
        UUID decoded = UUIDv7.fromCompactString(compactString);

        assertThat(decoded).isEqualTo(original);
    }

    @Test
    void roundTripWithMultipleUuids() {
        for (int i = 0; i < 1000; i++) {
            UUID original = UUIDv7.generate();
            String compactString = UUIDv7.toCompactString(original);
            UUID decoded = UUIDv7.fromCompactString(compactString);

            assertThat(decoded).isEqualTo(original);
        }
    }

    @Test
    void roundTripWithMonotonicUuids() {
        for (int i = 0; i < 1000; i++) {
            UUID original = MonotonicUUIDv7.generate();
            String compactString = UUIDv7.toCompactString(original);
            UUID decoded = UUIDv7.fromCompactString(compactString);

            assertThat(decoded).isEqualTo(original);
        }
    }

    @Test
    void zeroUuidConvertsCorrectly() {
        UUID zero = new UUID(0L, 0L);
        String compactString = UUIDv7.toCompactString(zero);

        assertThat(compactString).isEqualTo("0000000000000000000000");
        assertThat(UUIDv7.fromCompactString(compactString)).isEqualTo(zero);
    }

    @Test
    void maxUuidConvertsCorrectly() {
        UUID max = new UUID(-1L, -1L);
        String compactString = UUIDv7.toCompactString(max);

        assertThat(compactString).hasSize(22);
        assertThat(UUIDv7.fromCompactString(compactString)).isEqualTo(max);
    }

    @Test
    void preservesLexicographicOrderingForTimeOrderedUuids() {
        // Generate UUIDs with increasing timestamps
        List<UUID> uuids = new ArrayList<>();
        List<String> compactStringStrings = new ArrayList<>();

        for (long ts = 1000000000000L; ts < 1000000001000L; ts += 100) {
            final long timestamp = ts;
            UUID uuid = UUIDv7.generate(() -> timestamp);
            uuids.add(uuid);
            compactStringStrings.add(UUIDv7.toCompactString(uuid));
        }

        // UUIDs should be in order
        List<UUID> sortedUuids = new ArrayList<>(uuids);
        Collections.sort(sortedUuids);
        assertThat(uuids).isEqualTo(sortedUuids);

        // CompactString strings should also be in lexicographic order
        List<String> sortedCompactString = new ArrayList<>(compactStringStrings);
        Collections.sort(sortedCompactString);
        assertThat(compactStringStrings).isEqualTo(sortedCompactString);
    }

    @Test
    void differentUuidsProduceDifferentCompactString() {
        UUID uuid1 = UUIDv7.generate();
        UUID uuid2 = UUIDv7.generate();

        String compactString_1 = UUIDv7.toCompactString(uuid1);
        String compactString_2 = UUIDv7.toCompactString(uuid2);

        assertThat(compactString_1).isNotEqualTo(compactString_2);
    }

    @Test
    void onlyUsesCompactStringAlphabet() {
        for (int i = 0; i < 100; i++) {
            UUID uuid = UUIDv7.generate();
            String compactString = UUIDv7.toCompactString(uuid);

            assertThat(compactString).matches("[0-9A-Za-z]{22}");
        }
    }

    @Test
    void knownValueConversion() {
        // Test with a known UUID value
        UUID known = UUID.fromString("01234567-89ab-7def-8012-3456789abcde");
        String compactString = UUIDv7.toCompactString(known);

        // Should round-trip correctly
        UUID decoded = UUIDv7.fromCompactString(compactString);
        assertThat(decoded).isEqualTo(known);
    }
}
