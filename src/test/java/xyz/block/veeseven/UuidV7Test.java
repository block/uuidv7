package xyz.block.veeseven;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

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
        UUID uuid = UuidV7.generate(() -> fixedTime);
        
        assertThat(uuid).isNotNull();
        assertThat(UuidV7.getTimestamp(uuid)).isEqualTo(fixedTime);
    }
    
    @Test
    void getTimestampExtractsCorrectValue() {
        long expectedTime = System.currentTimeMillis();
        UUID uuid = UuidV7.generate(() -> expectedTime);
        
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
        
        UUID uuid1 = UuidV7.generate(() -> time1);
        UUID uuid2 = UuidV7.generate(() -> time2);
        
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
            UUID uuid = UuidV7.generate(() -> testTime);
            long extractedTime = UuidV7.getTimestamp(uuid);
            
            assertThat(extractedTime)
                .as("Timestamp should be preserved exactly for %d", testTime)
                .isEqualTo(testTime);
        }
    }
}
