import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@ExperimentalStdlibApi
@ExperimentalUnsignedTypes

class CpuInterruptTest {

    lateinit var cpu: CPU
    lateinit var memory: Memory

    @BeforeTest
    fun setup() {
        memory = Memory()
        cpu = CPU(memory)
        cpu.stackPointer = 0x100u
    }

    @Test
    fun `Interrupt master disabled`() {
        // Given interrupt master is disabled
        memory.set(0x100u, 0xF3u)
        memory.set(0x101u, 0x00u)
        cpu.tick()

        // And VBLANK interrupt is enabled and requested
        memory.set(0xFFFFu, 0b0000_0001u)
        memory.set(0xFF0Fu, 0b0000_0001u)

        // When the next instruction is executed
        cpu.tick()

        // Then the interrupt is not triggered
        assertEquals(cpu.programCounter, 0x102u)
    }

    @Test
    fun `Trigger VBLANK`() {
        // Given interrupt master is already enabled
        cpu.interruptMasterEnabled = true
        // And VBLANK interrupt is enabled and requested
        memory.set(0xFFFFu, 0b0000_0001u)
        memory.set(0xFF0Fu, 0b0000_0001u)
        // When the next instruction is executed
        cpu.tick()

        // Then the interrupt is triggered
        assertEquals(cpu.programCounter, 0x0040u)
        // And the previous counter is stored onto the stack
        assertEquals(0x01u, memory.get(cpu.stackPointer))
        assertEquals(0x01u, memory.get((cpu.stackPointer + 1u).toUShort()))

    }

    @Test
    fun `Enable interrupt is active on the next op`() {
        // Given interrupt master is disabled
        cpu.interruptMasterEnabled = false
        // And the program is set to EI -> NOP
        memory.set(0x100u, 0xFBu)
        memory.set(0x101u, 0x00u)
        // And VBLANK interrupt is enabled and requested
        memory.set(0xFFFFu, 0b0000_0001u)
        memory.set(0xFF0Fu, 0b0000_0001u)
        // When the next instruction is executed
        cpu.tick()

        // Then the interrupt is not triggered
        assertEquals(cpu.programCounter, 0x0101u)

        // When the second instruction is executed
        cpu.tick()
        // Then the interrupt is triggered
        assertEquals(cpu.programCounter, 0x0040u)
        // And the previous counter is stored onto the stack
        assertEquals(0x02u, memory.get(cpu.stackPointer))
        assertEquals(0x01u, memory.get((cpu.stackPointer + 1u).toUShort()))

    }
}