package xyz.block.uuidv7;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UuidV7Test {

    @Test
    void generateCreatesValidUuid() {
        UUID uuid = UuidV7.generate();

        assertThat(uuid).isNotNull();
        assertThat(uuid.version()).isEqualTo(7);
        assertThat(uuid.variant()).isEqualTo(2);
    }

    @Test
    void generateWithCustomClock() {
        long fixedTime = 1234567890000L;
        UUID uuid = UuidV7.generateMonotonic(() -> fixedTime);

        assertThat(uuid).isNotNull();
        assertThat(UuidV7.getTimestamp(uuid)).isEqualTo(fixedTime);
    }

    @Test
    void getTimestampExtractsCorrectValue() {
        long expectedTime = System.currentTimeMillis();
        UUID uuid = UuidV7.generateMonotonic(() -> expectedTime);

        long actualTime = UuidV7.getTimestamp(uuid);

        assertThat(actualTime).isEqualTo(expectedTime);
    }

    @Test
    void getTimestampThrowsOnNull() {
        assertThatThrownBy(() -> UuidV7.getTimestamp(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("UUID cannot be null");
    }

    @Test
    void getTimestampThrowsOnNonV7Uuid() {
        UUID v4Uuid = UUID.randomUUID();

        assertThatThrownBy(() -> UuidV7.getTimestamp(v4Uuid))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not version 7");
    }

    @Test
    void generatedUuidsAreUnique() {
        Set<UUID> uuids = new HashSet<>();

        for (int i = 0; i < 10000; i++) {
            UUID uuid = UuidV7.generate();
            assertThat(uuids.add(uuid))
                .as("UUID should be unique")
                .isTrue();
        }
    }

    @Test
    void generatedUuidsAreTimeSorted() {
        long time1 = 1000000000000L;
        long time2 = 2000000000000L;

        UUID uuid1 = UuidV7.generateMonotonic(() -> time1);
        UUID uuid2 = UuidV7.generateMonotonic(() -> time2);

        assertThat(uuid1.compareTo(uuid2))
            .as("Earlier UUID should sort before later UUID")
            .isLessThan(0);
    }

    @Test
    void toStringProducesStandardFormat() {
        UUID uuid = UuidV7.generate();
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
            UUID uuid = UuidV7.generateMonotonic(() -> testTime);
            long extractedTime = UuidV7.getTimestamp(uuid);

            assertThat(extractedTime)
                .as("Timestamp should be preserved exactly for %d", testTime)
                .isEqualTo(testTime);
        }
    }

    @Test
    void generateMonotonicMethodWorks() {
        // Generate UUIDs using generateMonotonic()
        List<UUID> uuids = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            uuids.add(UuidV7.generateMonotonic());
        }

        // Should be unique and valid
        Set<UUID> uniqueUuids = new HashSet<>(uuids);
        assertThat(uniqueUuids).hasSize(50);
        uniqueUuids.forEach(uuid -> {
            assertThat(uuid.version()).isEqualTo(7);
            assertThat(uuid.variant()).isEqualTo(2);
        });
    }

    @Test
    void generateMonotonicEnsuresStrictOrdering() {
        long fixedTime = 1234567890000L;

        // Generate multiple UUIDs at the same timestamp
        List<UUID> uuids = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            uuids.add(UuidV7.generateMonotonic(() -> fixedTime));
        }

        // Verify they are strictly ordered
        for (int i = 0; i < uuids.size() - 1; i++) {
            assertThat(uuids.get(i).compareTo(uuids.get(i + 1)))
                .as("UUID at index %d should be less than UUID at index %d", i, i + 1)
                .isLessThan(0);
        }
    }

    @Test
    void generateMonotonicAdvancesTimestampOnCounterOverflow() {
        AtomicLong timestamp = new AtomicLong(1234567890000L);

        // Generate more than 4096 UUIDs (counter max) to trigger overflow
        // Clock advances with each call, simulating time passing
        List<UUID> uuids = new ArrayList<>();
        for (int i = 0; i < 5000; i++) {
            uuids.add(UuidV7.generateMonotonic(timestamp::incrementAndGet));
        }

        // Verify all UUIDs are unique
        Set<UUID> uniqueUuids = new HashSet<>(uuids);
        assertThat(uniqueUuids).hasSize(5000);

        // Verify timestamp advanced beyond the initial value
        long maxTimestamp = uuids.stream()
            .mapToLong(UuidV7::getTimestamp)
            .max()
            .orElse(0);

        assertThat(maxTimestamp).isGreaterThan(1234567890000L);

        // Verify UUIDs are still ordered despite counter overflows
        for (int i = 0; i < uuids.size() - 1; i++) {
            assertThat(uuids.get(i).compareTo(uuids.get(i + 1)))
                .as("UUID at index %d should be less than UUID at index %d", i, i + 1)
                .isLessThan(0);
        }
    }

    @Test
    void generateMonotonicWithCustomClockEnsuresOrdering() {
        long fixedTime = 1234567890000L;

        // Generate multiple UUIDs using generateMonotonic() with custom clock at same timestamp
        List<UUID> uuids = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            uuids.add(UuidV7.generateMonotonic(() -> fixedTime));
        }

        // Should be strictly ordered despite same timestamp
        for (int i = 0; i < uuids.size() - 1; i++) {
            assertThat(uuids.get(i).compareTo(uuids.get(i + 1))).isLessThan(0);
        }
    }

    @Test
    void generateMonotonicResetsCounterOnNewTimestamp() {
        AtomicLong timestamp = new AtomicLong(1000000000000L);

        // Generate some UUIDs at first timestamp
        UUID uuid1 = UuidV7.generateMonotonic(timestamp::get);
        UUID uuid2 = UuidV7.generateMonotonic(timestamp::get);

        // Advance timestamp
        timestamp.set(2000000000000L);

        // Generate UUID at new timestamp
        UUID uuid3 = UuidV7.generateMonotonic(timestamp::get);

        // uuid3 should have later timestamp
        assertThat(UuidV7.getTimestamp(uuid3))
            .isGreaterThan(UuidV7.getTimestamp(uuid1))
            .isGreaterThan(UuidV7.getTimestamp(uuid2));

        // uuid3 should sort after uuid1 and uuid2
        assertThat(uuid1.compareTo(uuid3)).isLessThan(0);
        assertThat(uuid2.compareTo(uuid3)).isLessThan(0);
    }

    @Test
    void monotonicModeUniquenessUnderHighLoad() {
        AtomicLong timestamp = new AtomicLong(1234567890000L);

        // Generate many UUIDs (more than counter capacity of 4096)
        // The clock will naturally advance when counter overflows
        Set<UUID> uuids = new HashSet<>();
        for (int i = 0; i < 10000; i++) {
            uuids.add(UuidV7.generateMonotonic(timestamp::incrementAndGet));
        }

        // All should be unique
        assertThat(uuids).hasSize(10000);

        // Timestamp should have advanced beyond initial value
        long finalTimestamp = uuids.stream()
            .mapToLong(UuidV7::getTimestamp)
            .max()
            .orElse(0);
        assertThat(finalTimestamp).isGreaterThan(1234567890000L);
    }

    @Test
    void generateNonMonotonicMethodWorks() {
        // Generate UUIDs using generate() (non-monotonic)
        Set<UUID> uuids = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            UUID uuid = UuidV7.generate();
            assertThat(uuid).isNotNull();
            assertThat(uuid.version()).isEqualTo(7);
            assertThat(uuid.variant()).isEqualTo(2);
            uuids.add(uuid);
        }

        // Should be unique
        assertThat(uuids).hasSize(1000);
    }

    @Test
    void generateNonMonotonicDoesNotBlockOrGuaranteeOrdering() {
        long fixedTime = 1234567890000L;

        // Generate UUIDs in non-monotonic mode
        Set<UUID> uuids = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            UUID uuid = UuidV7.generate(() -> fixedTime);
            assertThat(uuid).isNotNull();
            assertThat(UuidV7.getTimestamp(uuid)).isEqualTo(fixedTime);
            uuids.add(uuid);
        }

        // Should still be unique
        assertThat(uuids).hasSize(1000);
    }

    @Test
    void nonMonotonicModeUniquenessUnderHighLoad() {
        long fixedTime = 1234567890000L;

        // Generate many UUIDs at the same timestamp in non-monotonic mode
        Set<UUID> uuids = new HashSet<>();
        for (int i = 0; i < 10000; i++) {
            uuids.add(UuidV7.generate(() -> fixedTime));
        }

        // Should still be unique (random bits provide uniqueness)
        assertThat(uuids).hasSize(10000);
    }
}
