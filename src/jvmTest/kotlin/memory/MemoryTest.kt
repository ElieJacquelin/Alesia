package memory

import CPU
import io.mockk.*
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@ExperimentalStdlibApi
@ExperimentalUnsignedTypes

internal class MemoryTest {

    lateinit var memory: Memory
    lateinit var cpu: CPU

    @BeforeTest
    fun setUp() {
        memory = Memory()
        memory.loadRom(UByteArray(0x8000)) // Default MBC0 ROM
        cpu = mockk()
        memory.cpu = cpu
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
        every { cpu.resetDiv() } just runs
        // Given some data is already store in Div
        memory.incrementDiv()

        // When a value is written ot 0xFF04
        memory.set(0xFF04u, 0x21u)

        // Then DIV is reset
        assertEquals(0x00u, memory.get(0xFF04u))
        // And the CPU internal counter is reset
        verify { cpu.resetDiv() }
    }

    @Test
    fun `Load ROM`() {
        // Given a ROM is ready to be loaded
        val rom = UByteArray(0x200) { 0x12u }
        rom[0x0147] = 0x00u // And MBC is MBC0
        rom[0x0149] = 0x00u // And RAM is not available

        // When loading the ROM
        memory.loadRom(rom)

        // Then the rom is loaded starting from address 0
        assertEquals(0x12u, memory.get(0x0000u))
        assertEquals(0x12u, memory.get(0x0001u))
    }

    @Test
    fun `Writing onto ROM is disabled`() {
        // Given the bypass is not set
        val memory = Memory(disableWritingToRom = true)
        memory.loadRom(UByteArray(0x2000))

        // When trying to write a value onto a ROM address
        memory.set(0x1234u, 0x12u)

        // Then the value is ignored
        assertEquals(0u, memory.get(0x1234u))
    }

    @Test
    fun `Allow bypassing ROM write restriction`() {
        // Given the bypass is set
        val memory = Memory(disableWritingToRom = false)
        memory.loadRom(UByteArray(0x2000))

        // When trying to write a value onto a ROM address
        memory.set(0x1234u, 0x12u)

        // Then the value is set
        assertEquals(0x12u, memory.get(0x1234u))
    }
}