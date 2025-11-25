package xyz.block.uuidv7

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class UUIDv7ExtensionsTest {
    
    @Test
    fun `timestamp extension property returns correct value`() {
        val expectedTime = 1234567890000L
        val uuid = UUIDv7.generate { expectedTime }
        
        assertThat(uuid.timestamp).isEqualTo(expectedTime)
    }
    
    @Test
    fun `timestamp extension works with generated UUIDs`() {
        val beforeTime = System.currentTimeMillis()
        val uuid = UUIDv7.generate()
        val afterTime = System.currentTimeMillis()
        
        assertThat(uuid.timestamp).isBetween(beforeTime, afterTime)
    }
    
    @Test
    fun `Kotlin API feels idiomatic`() {
        val uuid = UUIDv7.generate { 9999999999L }
        val timestamp = uuid.timestamp
        
        assertThat(timestamp).isEqualTo(9999999999L)
    }

    @Test
    fun `compactString extension property returns 22 character string`() {
        val uuid = UUIDv7.generate()
        val compactString = uuid.compactString

        assertThat(compactString).hasSize(22)
    }

    @Test
    fun `compactString extension round-trips correctly`() {
        val original = UUIDv7.generate()
        val compactString = original.compactString
        val decoded = UUIDv7.fromCompactString(compactString)

        assertThat(decoded).isEqualTo(original)
    }

    @Test
    fun `compactString only uses alphanumeric characters`() {
        val uuid = UUIDv7.generate()
        val compactString = uuid.compactString

        assertThat(compactString).matches("[0-9A-Za-z]{22}")
    }
}
