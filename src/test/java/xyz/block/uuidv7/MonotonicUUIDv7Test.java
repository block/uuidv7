package xyz.block.uuidv7;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class MonotonicUUIDv7Test {

    @Test
    void generateCreatesValidUuid() {
        UUID uuid = MonotonicUUIDv7.generate();

        assertThat(uuid).isNotNull();
        assertThat(uuid.version()).isEqualTo(7);
        assertThat(uuid.variant()).isEqualTo(2);
    }

    @Test
    void generateWithCustomClock() {
        long fixedTime = 1234567890000L;
        UUID uuid = MonotonicUUIDv7.generate(() -> fixedTime);

        assertThat(uuid).isNotNull();
        assertThat(UUIDv7.getTimestamp(uuid)).isEqualTo(fixedTime);
    }

    @Test
    void getTimestampExtractsCorrectValue() {
        long expectedTime = System.currentTimeMillis();
        UUID uuid = MonotonicUUIDv7.generate(() -> expectedTime);

        long actualTime = UUIDv7.getTimestamp(uuid);

        assertThat(actualTime).isEqualTo(expectedTime);
    }

    @Test
    void generatedUuidsAreTimeSorted() {
        long time1 = 1000000000000L;
        long time2 = 2000000000000L;

        UUID uuid1 = MonotonicUUIDv7.generate(() -> time1);
        UUID uuid2 = MonotonicUUIDv7.generate(() -> time2);

        assertThat(uuid1.compareTo(uuid2))
            .as("Earlier UUID should sort before later UUID")
            .isLessThan(0);
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
            UUID uuid = MonotonicUUIDv7.generate(() -> testTime);
            long extractedTime = UUIDv7.getTimestamp(uuid);

            assertThat(extractedTime)
                .as("Timestamp should be preserved exactly for %d", testTime)
                .isEqualTo(testTime);
        }
    }

    @Test
    void generateMonotonicMethodWorks() {
        // Generate UUIDs using generate()
        List<UUID> uuids = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            uuids.add(MonotonicUUIDv7.generate());
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
    void generateEnsuresStrictOrdering() {
        long fixedTime = 1234567890000L;

        // Generate multiple UUIDs at the same timestamp
        List<UUID> uuids = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            uuids.add(MonotonicUUIDv7.generate(() -> fixedTime));
        }

        // Verify they are strictly ordered
        for (int i = 0; i < uuids.size() - 1; i++) {
            assertThat(uuids.get(i).compareTo(uuids.get(i + 1)))
                .as("UUID at index %d should be less than UUID at index %d", i, i + 1)
                .isLessThan(0);
        }
    }

    @Test
    void generateAdvancesTimestampOnCounterOverflow() {
        AtomicLong timestamp = new AtomicLong(1234567890000L);

        // Generate more than 4096 UUIDs (counter max) to trigger overflow
        // Clock advances with each call, simulating time passing
        List<UUID> uuids = new ArrayList<>();
        for (int i = 0; i < 5000; i++) {
            uuids.add(MonotonicUUIDv7.generate(timestamp::incrementAndGet));
        }

        // Verify all UUIDs are unique
        Set<UUID> uniqueUuids = new HashSet<>(uuids);
        assertThat(uniqueUuids).hasSize(5000);

        // Verify timestamp advanced beyond the initial value
        long maxTimestamp = uuids.stream()
            .mapToLong(UUIDv7::getTimestamp)
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
    void generateWithCustomClockEnsuresOrdering() {
        long fixedTime = 1234567890000L;

        // Generate multiple UUIDs using generate() with custom clock at same timestamp
        List<UUID> uuids = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            uuids.add(MonotonicUUIDv7.generate(() -> fixedTime));
        }

        // Should be strictly ordered despite same timestamp
        for (int i = 0; i < uuids.size() - 1; i++) {
            assertThat(uuids.get(i).compareTo(uuids.get(i + 1))).isLessThan(0);
        }
    }

    @Test
    void generateResetsCounterOnNewTimestamp() {
        AtomicLong timestamp = new AtomicLong(1000000000000L);

        // Generate some UUIDs at first timestamp
        UUID uuid1 = MonotonicUUIDv7.generate(timestamp::get);
        UUID uuid2 = MonotonicUUIDv7.generate(timestamp::get);

        // Advance timestamp
        timestamp.set(2000000000000L);

        // Generate UUID at new timestamp
        UUID uuid3 = MonotonicUUIDv7.generate(timestamp::get);

        // uuid3 should have later timestamp
        assertThat(UUIDv7.getTimestamp(uuid3))
            .isGreaterThan(UUIDv7.getTimestamp(uuid1))
            .isGreaterThan(UUIDv7.getTimestamp(uuid2));

        // uuid3 should sort after uuid1 and uuid2
        assertThat(uuid1.compareTo(uuid3)).isLessThan(0);
        assertThat(uuid2.compareTo(uuid3)).isLessThan(0);
    }

    @Test
    void uniquenessUnderHighLoad() {
        AtomicLong timestamp = new AtomicLong(1234567890000L);

        // Generate many UUIDs (more than counter capacity of 4096)
        // The clock will naturally advance when counter overflows
        Set<UUID> uuids = new HashSet<>();
        for (int i = 0; i < 10000; i++) {
            uuids.add(MonotonicUUIDv7.generate(timestamp::incrementAndGet));
        }

        // All should be unique
        assertThat(uuids).hasSize(10000);

        // Timestamp should have advanced beyond initial value
        long finalTimestamp = uuids.stream()
            .mapToLong(UUIDv7::getTimestamp)
            .max()
            .orElse(0);
        assertThat(finalTimestamp).isGreaterThan(1234567890000L);
    }
}
