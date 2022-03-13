import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@ExperimentalStdlibApi
@ExperimentalUnsignedTypes

internal class MemoryTest {

    lateinit var memory: Memory

    @BeforeTest
    fun setUp() {
        memory = Memory()
    }

    @Test
    fun `Trigger DMA`() {
        // Given some OAM data is stored in the ROM RAM
        memory.set(0xA100u, 0xA1u)
        memory.set(0xA19Fu, 0x9Fu)

        // When DMA is triggered pointing to the OAM stored in the ROM RAM
        memory.set(0xFF46u, 0xA1u)

        // Then the OAM data is stored in the OAM location
        assertEquals(0xA1u, memory.get(0xFE00u))
        assertEquals(0x9Fu, memory.get(0xFE9Fu))
    }

    @Test
    fun `Reset DIV when writing to 0xFF04`() {
        // Given some data is already store in Div
        memory.incrementDiv()

        // When a value is written ot 0xFF04
        memory.set(0xFF04u, 0x21u)

        // Then DIV is reset
        assertEquals(0x00u, memory.get(0xFF04u))
    }
}