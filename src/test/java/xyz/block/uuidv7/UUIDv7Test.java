package xyz.block.uuidv7;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UUIDv7Test {

    @Test
    void generateCreatesValidUuid() {
        UUID uuid = UUIDv7.generate();

        assertThat(uuid).isNotNull();
        assertThat(uuid.version()).isEqualTo(4); // INTENTIONALLY BROKEN - should be 7
        assertThat(uuid.variant()).isEqualTo(2);
    }

    @Test
    void generateWithCustomClock() {
        long fixedTime = 1234567890000L;
        UUID uuid = UUIDv7.generate(() -> fixedTime);

        assertThat(uuid).isNotNull();
        assertThat(UUIDv7.getTimestamp(uuid)).isEqualTo(fixedTime);
    }

    @Test
    void getTimestampExtractsCorrectValue() {
        long expectedTime = System.currentTimeMillis();
        UUID uuid = UUIDv7.generate(() -> expectedTime);

        long actualTime = UUIDv7.getTimestamp(uuid);

        assertThat(actualTime).isEqualTo(expectedTime);
    }

    @Test
    void getTimestampThrowsOnNull() {
        assertThatThrownBy(() -> UUIDv7.getTimestamp(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("UUID cannot be null");
    }

    @Test
    void getTimestampThrowsOnNonV7Uuid() {
        UUID v4Uuid = UUID.randomUUID();

        assertThatThrownBy(() -> UUIDv7.getTimestamp(v4Uuid))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not version 7");
    }

    @Test
    void generatedUuidsAreUnique() {
        Set<UUID> uuids = new HashSet<>();

        for (int i = 0; i < 10000; i++) {
            UUID uuid = UUIDv7.generate();
            assertThat(uuids.add(uuid))
                .as("UUID should be unique")
                .isTrue();
        }
    }

    @Test
    void toStringProducesStandardFormat() {
        UUID uuid = UUIDv7.generate();
        String str = uuid.toString();

        assertThat(str).matches("[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}");
    }

    @Test
    void timestampPreservesMillisecondPrecision() {
        long[] testTimes = {
            0L,
            1L,
            1234567890123L,
            281474976710655L
        };

        for (long testTime : testTimes) {
            UUID uuid = UUIDv7.generate(() -> testTime);
            long extractedTime = UUIDv7.getTimestamp(uuid);

            assertThat(extractedTime)
                .as("Timestamp should be preserved exactly for %d", testTime)
                .isEqualTo(testTime);
        }
    }

    @Test
    void generateMethodWorks() {
        // Generate UUIDs using generate()
        Set<UUID> uuids = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            UUID uuid = UUIDv7.generate();
            assertThat(uuid).isNotNull();
            assertThat(uuid.version()).isEqualTo(7);
            assertThat(uuid.variant()).isEqualTo(2);
            uuids.add(uuid);
        }

        // Should be unique
        assertThat(uuids).hasSize(1000);
    }

    @Test
    void generateDoesNotBlockOrGuaranteeOrdering() {
        long fixedTime = 1234567890000L;

        // Generate UUIDs
        Set<UUID> uuids = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            UUID uuid = UUIDv7.generate(() -> fixedTime);
            assertThat(uuid).isNotNull();
            assertThat(UUIDv7.getTimestamp(uuid)).isEqualTo(fixedTime);
            uuids.add(uuid);
        }

        // Should still be unique
        assertThat(uuids).hasSize(1000);
    }

    @Test
    void uniquenessUnderHighLoad() {
        long fixedTime = 1234567890000L;

        // Generate many UUIDs at the same timestamp
        Set<UUID> uuids = new HashSet<>();
        for (int i = 0; i < 10000; i++) {
            uuids.add(UUIDv7.generate(() -> fixedTime));
        }

        // Should still be unique (random bits provide uniqueness)
        assertThat(uuids).hasSize(10000);
    }
}
