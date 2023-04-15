import memory.Memory
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
        memory.loadRom(UByteArray(0x8000)) // MBC0 ROM
        cpu = CPU(memory)
        cpu.stackPointer = 0x100u
    }

    @Test
    fun `DEV intermediate cycle counts get updated after every instruction`() {
        // Given no cycles have passed
        cpu.divCycleCount = 0
        assertEquals(0u, memory.get(0xFF04u))

        // When 4 cycles have passed (LD A,A is 4 cycles)
        memory.set(0x100u, 0x7Fu)
        cpu.tick()

        // Then the current cycle count is decremented by 4
        assertEquals(4, cpu.divCycleCount)
        // and the div register is not updated
        assertEquals(0u, memory.get(0xFF04u))
    }

    @Test
    fun `DIV is incremented after 256 cycles`() {
        // Given 252 cycles have passed (4 cycles remains)
        cpu.divCycleCount = 252

        // When 4 cycles have passed (LD A,A is 4 cycles)
        memory.set(0x100u, 0x7Fu)
        cpu.tick()

        // Then the div register is incremented
        assertEquals(0x1u, memory.get(0xFF04u))
        // and the current cycle count is reset
        assertEquals(0, cpu.divCycleCount)
    }

    @Test
    fun `DIV register overflows back to 0`() {
        // Given 252 cycles have passed (4 cycles remains)
        cpu.divCycleCount = 252
        // And the DIV register is at maximum count
        cpu.divCycleCount = 0
        memory.set(0xFF04u, 0xFFu)

        // When 4 cycles have passed (LD A,A is 4 cycles)
        memory.set(0x100u, 0x7Fu)
        cpu.tick()

        // Then the div register overflows and go back to 0
        assertEquals(0u, memory.get(0xFF04u))
    }

    @Test
    fun `TimerControl can not disable DIV counting`() {
        // Given the timer control disable the timer
        memory.set(0xFF07u, 0b0000_0011u) // 3rd bit is the timer control

        // When 4 cycles have passed (LD A,A is 4 cycles)
        memory.set(0x100u, 0x7Fu)
        cpu.tick()

        // Then the div register is increased
        assertEquals(4, cpu.divCycleCount)
    }

    @Test
    fun `TimerControl can disable timer counting`() {
        // Given the timer control disable the timer
        memory.set(0xFF07u, 0b0000_0011u) // 3rd bit is the timer control

        // When 4 cycles have passed (LD A,A is 4 cycles)
        memory.set(0x100u, 0x7Fu)
        cpu.tick()

        // Then the timer counter is not increased
        assertEquals(0, cpu.timerCycleCount)
    }

    @Test
    fun `TimerControl can enable timer counting`() {
        // Given the timer control enable the timer
        memory.set(0xFF07u, 0b0000_0111u) // 3rd bit is the timer control

        // When 4 cycles have passed (LD A,A is 4 cycles)
        memory.set(0x100u, 0x7Fu)
        cpu.tick()

        // Then the timer counter is increased
        assertEquals(4, cpu.timerCycleCount)
    }

    @Test
    fun `Timer counter register is increased when the cycle count reach a threshold - 4096 Hz`() {
        // Given the threshold is set to 4096hz (1024 clock cycles)
        memory.set(0xFF07u, 0b0000_0100u)
        // And 1020 cycles have passed
        cpu.timerCycleCount = 1020

        // When 4 cycles have passed (LD A,A is 4 cycles)
        memory.set(0x100u, 0x7Fu)
        cpu.tick()

        // Then the timer counter register is incremented
        assertEquals(1u, memory.get(0xFF05u))
        // and the current count is reset
        assertEquals(0, cpu.timerCycleCount)
    }

    @Test
    fun `Timer counter register is increased when the cycle count reach a threshold - 262144 Hz`() {
        // Given the threshold is set to 262144hz (16 clock cycles)
        memory.set(0xFF07u, 0b0000_0101u)
        // And 12 cycles have passed
        cpu.timerCycleCount = 12

        // When 4 cycles have passed (LD A,A is 4 cycles)
        memory.set(0x100u, 0x7Fu)
        cpu.tick()

        // Then the timer counter register is incremented
        assertEquals(1u, memory.get(0xFF05u))
        // and the current count is reset
        assertEquals(0, cpu.timerCycleCount)
    }

    @Test
    fun `Timer counter register is increased when the cycle count reach a threshold - 65536 Hz`() {
        // Given the threshold is set to 65536 hz (64 clock cycles)
        memory.set(0xFF07u, 0b0000_0110u)
        // And 60 cycles have passed
        cpu.timerCycleCount = 60

        // When 4 cycles have passed (LD A,A is 4 cycles)
        memory.set(0x100u, 0x7Fu)
        cpu.tick()

        // Then the timer counter register is incremented
        assertEquals(1u, memory.get(0xFF05u))
        // and the current count is reset
        assertEquals(0, cpu.timerCycleCount)
    }

    @Test
    fun `Timer counter register is increased when the cycle count reach a threshold - 16384 Hz`() {
        // Given the threshold is set to 16384 hz (256 clock cycles)
        memory.set(0xFF07u, 0b0000_0111u)
        // And 252 cycles have passed
        cpu.timerCycleCount = 252

        // When 4 cycles have passed (LD A,A is 4 cycles)
        memory.set(0x100u, 0x7Fu)
        cpu.tick()

        // Then the timer counter register is incremented
        assertEquals(1u, memory.get(0xFF05u))
        // and the current count is reset
        assertEquals(0, cpu.timerCycleCount)
    }

    @Test
    fun `Timer counter register can be increased more than once`() {
        // Given the threshold is set to 262144hz (16 clock cycles)
        memory.set(0xFF07u, 0b0000_0101u)
        // And 33 cycles have passed
        cpu.timerCycleCount = 33

        // When 4 cycles have passed (LD A,A is 4 cycles)
        memory.set(0x100u, 0x7Fu)
        cpu.tick()

        // Then the timer counter register is incremented twice (37 = 16*2 + 5)
        assertEquals(2u, memory.get(0xFF05u))
        // and the current count is set to the remainder
        assertEquals(5, cpu.timerCycleCount)
    }

    @Test
    fun `Timer counter register overflow`() {
        // Given the threshold is set to 4096hz (1024 clock cycles)
        memory.set(0xFF07u, 0b0000_0100u)
        // And the timer counter is ready to overflow
        memory.set(0xFF05u, 0xFFu)
        // And the timer modulo is set to some value
        memory.set(0xFF06u, 0x12u)
        // And 1020 cycles have passed
        cpu.timerCycleCount = 1020

        // When 4 cycles have passed (LD A,A is 4 cycles)
        memory.set(0x100u, 0x7Fu)
        cpu.tick()

        // Then the timer counter is reset to the timer modulo value
        assertEquals(0x12u, memory.get(0xFF05u))
        // and the current count is reset
        assertEquals(0, cpu.timerCycleCount)
        // and the timer interrupt is enabled
        assertEquals(0b0000_0100u, memory.get(0xFF0Fu))
    }

}
