import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@ExperimentalStdlibApi
@ExperimentalUnsignedTypes

class CpuTimerTest {

    lateinit var cpu: CPU
    lateinit var memory: Memory

    @BeforeTest
    fun setup() {
        memory = Memory()
        cpu = CPU(memory)
        cpu.stackPointer = 0x100u
    }

    @Test
    fun `DEV intermediate cycle counts get updated after every instruction`() {
        // Given no cycles have passed
        cpu.devCycleCount = 4096
        assertEquals(0u, memory.get(0xFF04u))

        // When 4 cycles have spent (LD A,A is 4 cycles)
        memory.set(0x100u, 0x7Fu)
        cpu.tick()

        // Then the current cycle count is decremented by 4
        assertEquals(4092, cpu.devCycleCount)
        // and the dev register is not updated
        assertEquals(0u, memory.get(0xFF04u))
    }

    @Test
    fun `DEV is incremented after 4096 cycles`() {
        // Given 4092 cycles have passed (4 cycles remains)
        cpu.devCycleCount = 4

        // When 4 cycles have spent (LD A,A is 4 cycles)
        memory.set(0x100u, 0x7Fu)
        cpu.tick()

        // Then the dev register is incremented
        assertEquals(0x1u, memory.get(0xFF04u))
        // and the current cycle count is reset
        assertEquals(4096, cpu.devCycleCount)
    }

}
