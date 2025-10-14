package xyz.block.veeseven

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class UuidV7ExtensionsTest {
    
    @Test
    fun `timestamp extension property returns correct value`() {
        val expectedTime = 1234567890000L
        val uuid = UuidV7.generate { expectedTime }
        
        assertThat(uuid.timestamp).isEqualTo(expectedTime)
    }
    
    @Test
    fun `timestamp extension works with generated UUIDs`() {
        val beforeTime = System.currentTimeMillis()
        val uuid = UuidV7.generate()
        val afterTime = System.currentTimeMillis()
        
        assertThat(uuid.timestamp).isBetween(beforeTime, afterTime)
    }
    
    @Test
    fun `Kotlin API feels idiomatic`() {
        val uuid = UuidV7.generate { 9999999999L }
        val timestamp = uuid.timestamp
        
        assertThat(timestamp).isEqualTo(9999999999L)
    }
}
