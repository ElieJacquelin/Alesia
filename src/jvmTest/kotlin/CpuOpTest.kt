import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@ExperimentalStdlibApi
@ExperimentalUnsignedTypes
class CpuOpTest {

    lateinit var cpu: CPU
    lateinit var memory: Memory

    @BeforeTest
    fun setup() {
        memory = Memory()
        cpu = CPU(memory)
        cpu.stackPointer = 0x100u
    }

    @Test
    fun `F flag lower nibble always 0`() {
        // When a non 0 value is set to F lower nibble
        cpu.AF.right = 0xFFu
        // Then the lower nibble remains 0 and higher nibble get the value
        assertEquals(0xF0u, cpu.AF.right)
    }

    // =============================================
    // LD nn,n => Load into register the value at PC
    // =============================================

    @Test
    fun `LD B, n`() {
        memory.set(0x100u, 0x06u) // LD B, n
        memory.set(0x101u, 0x11u) // n

        val cycleCount = cpu.tick()
        assertEquals(0x11u, cpu.BC.left) // B loaded nn
        assertEquals(0x102u, cpu.programCounter) // forward 2 steps
        assertEquals(8, cycleCount)
    }

    @Test
    fun `LD C, n`() {
        memory.set(0x100u, 0x0Eu) // LD C, n
        memory.set(0x101u, 0x11u) // n

        val cycleCount = cpu.tick()
        assertEquals(0x11u, cpu.BC.right) // C loaded nn
        assertEquals(0x102u, cpu.programCounter) // forward 2 steps
        assertEquals(8, cycleCount)
    }

    @Test
    fun `LD D, n`() {
        memory.set(0x100u, 0x16u) // LD D, n
        memory.set(0x101u, 0x11u) // n

        val cycleCount = cpu.tick()
        assertEquals(0x11u, cpu.DE.left) // B loaded nn
        assertEquals(0x102u, cpu.programCounter) // forward 2 steps
        assertEquals(8, cycleCount)
    }

    @Test
    fun `LD E, n`() {
        memory.set(0x100u, 0x1Eu) // LD E, n
        memory.set(0x101u, 0x11u) // n

        val cycleCount = cpu.tick()
        assertEquals(0x11u, cpu.DE.right) // C loaded nn
        assertEquals(0x102u, cpu.programCounter) // forward 2 steps
        assertEquals(8, cycleCount)
    }

    @Test
    fun `LD H, n`() {
        memory.set(0x100u, 0x26u) // LD H, n
        memory.set(0x101u, 0x11u) // n

        val cycleCount = cpu.tick()
        assertEquals(0x11u, cpu.HL.left) // B loaded nn
        assertEquals(0x102u, cpu.programCounter) // forward 2 steps
        assertEquals(8, cycleCount)
    }

    @Test
    fun `LD L, n`() {
        memory.set(0x100u, 0x2Eu) // LD L, n
        memory.set(0x101u, 0x11u) // n

        val cycleCount = cpu.tick()
        assertEquals(0x11u, cpu.HL.right) // C loaded nn
        assertEquals(0x102u, cpu.programCounter) // forward 2 steps
        assertEquals(8, cycleCount)
    }

    // ============================================
    //LD r1, r2 => Load into r1 the value in r2
    // A register
    // ==============================================
    @Test
    fun `LD A, A`() {
        memory.set(0x100u, 0x7Fu) // LD A, A
        cpu.AF.left = 0x11u

        val cycleCount = cpu.tick()
        assertEquals(0x11u, cpu.AF.left)
        assertEquals(0x101u, cpu.programCounter) // forward 1 step
        assertEquals(4, cycleCount)
    }

    @Test
    fun `LD A, B`() {
        memory.set(0x100u, 0x78u) // LD A, B
        cpu.BC.left = 0x11u

        val cycleCount = cpu.tick()
        assertEquals(0x11u, cpu.AF.left)
        assertEquals(0x101u, cpu.programCounter) // forward 1 step
        assertEquals(4, cycleCount)
    }

    @Test
    fun `LD A, C`() {
        memory.set(0x100u, 0x79u) // LD A, C
        cpu.BC.right = 0x11u

        val cycleCount = cpu.tick()
        assertEquals(0x11u, cpu.AF.left)
        assertEquals(0x101u, cpu.programCounter) // forward 1 step
        assertEquals(4, cycleCount)
    }

    @Test
    fun `LD A, D`() {
        memory.set(0x100u, 0x7Au) // LD A, D
        cpu.DE.left = 0x11u

        val cycleCount = cpu.tick()
        assertEquals(0x11u, cpu.AF.left)
        assertEquals(0x101u, cpu.programCounter) // forward 1 step
        assertEquals(4, cycleCount)
    }

    @Test
    fun `LD A, E`() {
        memory.set(0x100u, 0x7Bu) // LD A, E
        cpu.DE.right = 0x11u

        val cycleCount = cpu.tick()
        assertEquals(0x11u, cpu.AF.left)
        assertEquals(0x101u, cpu.programCounter) // forward 1 step
        assertEquals(4, cycleCount)
    }

    @Test
    fun `LD A, H`() {
        memory.set(0x100u, 0x7Cu) // LD A, H
        cpu.HL.left = 0x11u

        val cycleCount = cpu.tick()
        assertEquals(0x11u, cpu.AF.left)
        assertEquals(0x101u, cpu.programCounter) // forward 1 step
        assertEquals(4, cycleCount)
    }

    @Test
    fun `LD A, L`() {
        memory.set(0x100u, 0x7Du) // LD A, L
        cpu.HL.right = 0x11u

        val cycleCount = cpu.tick()
        assertEquals(0x11u, cpu.AF.left)
        assertEquals(0x101u, cpu.programCounter) // forward 1 step
        assertEquals(4, cycleCount)
    }

    @Test
    fun `LD A, (BC)`() {
        memory.set(0x100u, 0x0Au) // LD A, (BC)
        cpu.BC.left = 0x12u
        cpu.BC.right = 0x11u
        memory.set(0x1211u, 0x13u)

        val cycleCount = cpu.tick()
        assertEquals(0x13u, cpu.AF.left)
        assertEquals(0x101u, cpu.programCounter) // forward 1 step
        assertEquals(8, cycleCount)
    }

    @Test
    fun `LD A, (DE)`() {
        memory.set(0x100u, 0x1Au) // LD A, (DE)
        cpu.DE.left = 0x12u
        cpu.DE.right = 0x11u
        memory.set(0x1211u, 0x13u)

        val cycleCount = cpu.tick()
        assertEquals(0x13u, cpu.AF.left)
        assertEquals(0x101u, cpu.programCounter) // forward 1 step
        assertEquals(8, cycleCount)
    }

    @Test
    fun `LD A, (HL)`() {
        memory.set(0x100u, 0x7Eu) // LD A, (HL)
        cpu.HL.left = 0x12u
        cpu.HL.right = 0x11u
        memory.set(0x1211u, 0x13u)

        val cycleCount = cpu.tick()
        assertEquals(0x13u, cpu.AF.left)
        assertEquals(0x101u, cpu.programCounter) // forward 1 step
        assertEquals(8, cycleCount)
    }

    @Test
    fun `LD A, (nn)`() {
        memory.set(0x100u, 0xFAu) // LD A, (nn)
        memory.set(0x101u, 0x12u)
        memory.set(0x102u, 0x11u)
        memory.set(0x1112u, 0x13u)

        val cycleCount = cpu.tick()
        assertEquals(0x13u, cpu.AF.left)
        assertEquals(0x103u, cpu.programCounter) // forward 3 steps
        assertEquals(16, cycleCount)
    }

    @Test
    fun `LD A, #`() {
        memory.set(0x100u, 0x3Eu) // LD A, #
        memory.set(0x101u, 0x12u)

        val cycleCount = cpu.tick()
        assertEquals(0x12u, cpu.AF.left)
        assertEquals(0x102u, cpu.programCounter) // forward 2 steps
        assertEquals(8, cycleCount)
    }

    @Test
    fun `LD (HL), B`() {
        memory.set(0x100u, 0x70u) // LD (HL), B
        cpu.HL.left = 0x12u
        cpu.HL.right = 0x11u
        cpu.BC.left = 0x13u

        val cycleCount = cpu.tick()
        assertEquals(0x13u, memory.get(0x1211u))
        assertEquals(0x101u, cpu.programCounter) // forward 1 step
        assertEquals(8, cycleCount)
    }

    @Test
    fun `LD (HL), C`() {
        memory.set(0x100u, 0x71u) // LD (HL), C
        cpu.HL.left = 0x12u
        cpu.HL.right = 0x11u
        cpu.BC.right = 0x13u

        val cycleCount = cpu.tick()
        assertEquals(0x13u, memory.get(0x1211u))
        assertEquals(0x101u, cpu.programCounter) // forward 1 step
        assertEquals(8, cycleCount)
    }

    @Test
    fun `LD (HL), D`() {
        memory.set(0x100u, 0x72u) // LD (HL), D
        cpu.HL.left = 0x12u
        cpu.HL.right = 0x11u
        cpu.DE.left = 0x13u

        val cycleCount = cpu.tick()
        assertEquals(0x13u, memory.get(0x1211u))
        assertEquals(0x101u, cpu.programCounter) // forward 1 step
        assertEquals(8, cycleCount)
    }

    @Test
    fun `LD (HL), E`() {
        memory.set(0x100u, 0x73u) // LD (HL), E
        cpu.HL.left = 0x12u
        cpu.HL.right = 0x11u
        cpu.DE.right = 0x13u

        val cycleCount = cpu.tick()
        assertEquals(0x13u, memory.get(0x1211u))
        assertEquals(0x101u, cpu.programCounter) // forward 1 step
        assertEquals(8, cycleCount)
    }

    @Test
    fun `LD (HL), H`() {
        memory.set(0x100u, 0x74u) // LD (HL), H
        cpu.HL.left = 0x12u
        cpu.HL.right = 0x11u

        val cycleCount = cpu.tick()
        assertEquals(0x12u, memory.get(0x1211u))
        assertEquals(0x101u, cpu.programCounter) // forward 1 step
        assertEquals(8, cycleCount)
    }

    @Test
    fun `LD (HL), L`() {
        memory.set(0x100u, 0x75u) // LD (HL), L
        cpu.HL.left = 0x12u
        cpu.HL.right = 0x11u

        val cycleCount = cpu.tick()
        assertEquals(0x11u, memory.get(0x1211u))
        assertEquals(0x101u, cpu.programCounter) // forward 1 step
        assertEquals(8, cycleCount)
    }

    @Test
    fun `LD (HL), n`() {
        memory.set(0x100u, 0x36u) // LD (HL), L
        memory.set(0x101u, 0x13u)
        cpu.HL.left = 0x12u
        cpu.HL.right = 0x11u

        val cycleCount = cpu.tick()
        assertEquals(0x13u, memory.get(0x1211u))
        assertEquals(0x102u, cpu.programCounter) // forward 2 steps
        assertEquals(12, cycleCount)
    }

    // LD n A => Load value of A in address n
    @Test
    fun `LD (BC), A`() {
        memory.set(0x100u, 0x02u)
        cpu.BC.left = 0x12u
        cpu.BC.right = 0x11u
        cpu.AF.left= 0x13u

        val cycleCount = cpu.tick()
        assertEquals(0x13u, memory.get(0x1211u))
        assertEquals(0x101u, cpu.programCounter) // forward 1 step
        assertEquals(8, cycleCount)
    }

    @Test
    fun `LD (DE), A`() {
        memory.set(0x100u, 0x12u)
        cpu.DE.left = 0x12u
        cpu.DE.right = 0x11u
        cpu.AF.left= 0x13u

        val cycleCount = cpu.tick()
        assertEquals(0x13u, memory.get(0x1211u))
        assertEquals(0x101u, cpu.programCounter) // forward 1 step
        assertEquals(8, cycleCount)
    }

    @Test
    fun `LD (HL), A`() {
        memory.set(0x100u, 0x77u)
        cpu.HL.left = 0x12u
        cpu.HL.right = 0x11u
        cpu.AF.left= 0x13u

        val cycleCount = cpu.tick()
        assertEquals(0x13u, memory.get(0x1211u))
        assertEquals(0x101u, cpu.programCounter) // forward 1 step
        assertEquals(8, cycleCount)
    }

    @Test
    fun `LD (nn), A`() {
        memory.set(0x100u, 0xEAu)
        memory.set(0x101u, 0x12u)
        memory.set(0x102u, 0x11u)
        cpu.AF.left= 0x13u

        val cycleCount = cpu.tick()
        assertEquals(0x13u, memory.get(0x1112u))
        assertEquals(0x103u, cpu.programCounter) // forward 3 steps
        assertEquals(16, cycleCount)
    }

    @Test
    fun `LD A,($FF00+C)`() {
        memory.set(0x100u, 0xF2u)
        cpu.BC.right = 0x12u
        memory.set(0xFF12u, 0x13u)

        val cycleCount = cpu.tick()
        assertEquals(0x13u, cpu.AF.left)
        assertEquals(0x101u, cpu.programCounter) // forward 1 steps
        assertEquals(8, cycleCount)
    }

    @Test
    fun `LD ($FF00+C), A`() {
        memory.set(0x100u, 0xE2u)
        cpu.AF.left = 0x13u
        cpu.BC.right = 0x12u

        val cycleCount = cpu.tick()
        assertEquals(0x13u, memory.get(0xFF12u))
        assertEquals(0x101u, cpu.programCounter) // forward 1 steps
        assertEquals(8, cycleCount)
    }

    @Test
    fun `LD A,(HL) - DEC HL`() {
        memory.set(0x100u, 0x3Au)
        cpu.HL.left = 0x12u
        cpu.HL.right = 0x11u
        memory.set(0x1211u, 0x13u)

        val cycleCount = cpu.tick()
        assertEquals(0x13u, cpu.AF.left)
        assertEquals(0x101u, cpu.programCounter) // forward 1 steps
        assertEquals(8, cycleCount)
        assertEquals(0x1210u, cpu.HL.both())
    }

    @Test
    fun `LD A,(HL) - DEC HL | carry`() {
        memory.set(0x100u, 0x3Au)
        cpu.HL.left = 0x03u
        cpu.HL.right = 0x00u
        memory.set(0x0300u, 0x13u)


        val cycleCount = cpu.tick()
        assertEquals(0x13u, cpu.AF.left)
        assertEquals(0x101u, cpu.programCounter) // forward 1 steps
        assertEquals(8, cycleCount)
        assertEquals(0x02FFu, cpu.HL.both())
    }

    @Test
    fun `LD A,(HL) - DEC HL | lower limit`() {
        memory.set(0x100u, 0x3Au)
        cpu.HL.left = 0x00u
        cpu.HL.right = 0x00u
        memory.set(0x0000u, 0x13u)

        val cycleCount = cpu.tick()
        assertEquals(0x13u, cpu.AF.left)
        assertEquals(0x101u, cpu.programCounter) // forward 1 steps
        assertEquals(8, cycleCount)
        assertEquals(0xFFFFu, cpu.HL.both())
    }

    @Test
    fun `LD (HL),A - DEC H`() {
        memory.set(0x100u, 0x32u)
        cpu.HL.left = 0x11u
        cpu.HL.right = 0x12u
        cpu.AF.left = 0x13u

        val cycleCount = cpu.tick()
        assertEquals(0x101u, cpu.programCounter) // forward 1 steps
        assertEquals(8, cycleCount)
        assertEquals(0x13u, memory.get(0x1112u))
        assertEquals(0x1111u, cpu.HL.both())
    }

    @Test
    fun `LD A,(HL) - INC HL`() {
        memory.set(0x100u, 0x2Au)
        cpu.HL.left = 0x12u
        cpu.HL.right = 0x11u
        memory.set(0x1211u, 0x13u)

        val cycleCount = cpu.tick()
        assertEquals(0x13u, cpu.AF.left)
        assertEquals(0x101u, cpu.programCounter) // forward 1 steps
        assertEquals(8, cycleCount)
        assertEquals(0x1212u, cpu.HL.both())
    }

    @Test
    fun `LD (HL),A - INC HL`() {
        memory.set(0x100u, 0x22u)
        cpu.HL.left = 0x11u
        cpu.HL.right = 0x12u
        cpu.AF.left = 0x13u

        val cycleCount = cpu.tick()
        assertEquals(0x101u, cpu.programCounter) // forward 1 steps
        assertEquals(8, cycleCount)
        assertEquals(0x13u, memory.get(0x1112u))
        assertEquals(0x1113u, cpu.HL.both())
    }

    @Test
    fun `LDH (n),A`() {
        memory.set(0x100u, 0xE0u)
        memory.set(0x101u, 0x11u)
        cpu.AF.left = 0x13u

        val cycleCount = cpu.tick()
        assertEquals(0x102u, cpu.programCounter) // forward 2 steps
        assertEquals(12, cycleCount)
        assertEquals(0x13u, memory.get(0xFF11u))
    }

    @Test
    fun `LDH A,(n)`() {
        memory.set(0x100u, 0xF0u)
        memory.set(0x101u, 0x11u)
        memory.set(0xFF11u, 0x13u)

        val cycleCount = cpu.tick()
        assertEquals(0x102u, cpu.programCounter) // forward 2 steps
        assertEquals(12, cycleCount)
        assertEquals(0x13u, cpu.AF.left)
    }

    // =============
    // 16-bit loads
    // ============
    @Test
    fun `LD BC, nn`() {
        memory.set(0x100u, 0x01u)
        memory.set(0x101u, 0x11u)
        memory.set(0x102u, 0x12u)

        val cycleCount = cpu.tick()
        assertEquals(0x103u, cpu.programCounter) // forward 3 steps
        assertEquals(12, cycleCount)
        assertEquals(0x1211u, cpu.BC.both())
    }

    @Test
    fun `LD DE, nn`() {
        memory.set(0x100u, 0x11u)
        memory.set(0x101u, 0x11u)
        memory.set(0x102u, 0x12u)

        val cycleCount = cpu.tick()
        assertEquals(0x103u, cpu.programCounter) // forward 3 steps
        assertEquals(12, cycleCount)
        assertEquals(0x1211u, cpu.DE.both())
    }

    @Test
    fun `LD HL, nn`() {
        memory.set(0x100u, 0x21u)
        memory.set(0x101u, 0x11u)
        memory.set(0x102u, 0x12u)

        val cycleCount = cpu.tick()
        assertEquals(0x103u, cpu.programCounter) // forward 3 steps
        assertEquals(12, cycleCount)
        assertEquals(0x1211u, cpu.HL.both())
    }

    @Test
    fun `LD SP, nn`() {
        memory.set(0x100u, 0x31u)
        memory.set(0x101u, 0x11u)
        memory.set(0x102u, 0x12u)

        val cycleCount = cpu.tick()
        assertEquals(0x103u, cpu.programCounter) // forward 3 steps
        assertEquals(12, cycleCount)
        assertEquals(0x1211u, cpu.stackPointer)
    }

    @Test
    fun `LD SP, HL`() {
        memory.set(0x100u, 0xF9u)
        cpu.HL.setBoth(0x1213u)

        val cycleCount = cpu.tick()
        assertEquals(0x101u, cpu.programCounter) // forward 1 steps
        assertEquals(8, cycleCount)
        assertEquals(0x1213u, cpu.stackPointer)
    }

    @Test
    fun `LD (nn), SP`() {
        memory.set(0x100u, 0x08u)
        memory.set(0x101u, 0x11u)
        memory.set(0x102u, 0x12u)
        cpu.stackPointer = 0x3456u

        val cycleCount = cpu.tick()
        assertEquals(0x103u, cpu.programCounter) // forward 3 steps
        assertEquals(20, cycleCount)
        assertEquals(0x56u, memory.get(0x1211u))
        assertEquals(0x34u, memory.get(0x1212u))
    }

    @Test
    fun `PUSH AF`() {
        memory.set(0x100u, 0xF5u)
        cpu.stackPointer = 0x3456u
        cpu.AF.setBoth(0x1230u)

        val cycleCount = cpu.tick()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(16, cycleCount)
        assertEquals(0x12u, memory.get(0x3455u))
        assertEquals(0x30u, memory.get(0x3454u))
        assertEquals(0x3454u, cpu.stackPointer)
    }

    @Test
    fun `POP AF`() {
        memory.set(0x100u, 0xF1u)
        cpu.stackPointer = 0x3456u
        memory.set(0x3456u, 0x10u)
        memory.set(0x3457u, 0x34u)

        val cycleCount = cpu.tick()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(16, cycleCount)
        assertEquals(0x34u, cpu.AF.left)
        assertEquals(0x10u, cpu.AF.right)
        assertEquals(0x3458u, cpu.stackPointer)
    }

    @Test
    fun `POP AF | F lower nibble always 0`() {
        memory.set(0x100u, 0xF1u)
        cpu.stackPointer = 0x3456u
        memory.set(0x3456u, 0x12u)
        memory.set(0x3457u, 0x34u)

        val cycleCount = cpu.tick()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(16, cycleCount)
        assertEquals(0x34u, cpu.AF.left)
        assertEquals(0x10u, cpu.AF.right)
        assertEquals(0x3458u, cpu.stackPointer)
    }

    @Test
    fun `ADD A, A | no carry`() {
        memory.set(0x100u, 0x87u)
        cpu.AF.left = 0b01000011u

        // 01000011 + 01000011 = 10000110
        // Carry flag false
        // Half carry false

        val cycleCount = cpu.tick()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(4, cycleCount)
        assertEquals(0b10000110u, cpu.AF.left)
        assertEquals(0b00000000u, cpu.AF.right)
    }

    @Test
    fun `ADD A, A | carry`() {
        memory.set(0x100u, 0x87u)
        cpu.AF.left = 0b11000011u

        // 11000011 + 11000011 = 110000110 => 10000110
        // Carry flag true
        // Half carry false

        val cycleCount = cpu.tick()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(4, cycleCount)
        assertEquals(0b10000110u, cpu.AF.left)
        assertEquals(0b00010000u, cpu.AF.right)
    }

    @Test
    fun `ADD A, A | half carry`() {
        memory.set(0x100u, 0x87u)
        cpu.AF.left = 0b01001100u

        // 01001100 + 01001100 = 10011000
        // Carry flag false
        // Half carry true

        val cycleCount = cpu.tick()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(4, cycleCount)
        assertEquals(0b10011000u, cpu.AF.left)
        assertEquals(0b00100000u, cpu.AF.right)
    }

    @Test
    fun `ADD A, A | upper limit`() {
        memory.set(0x100u, 0x87u)
        cpu.AF.left = 0b11111111u

        // 11111111 + 11111111 = 1 1111 1110
        // Carry flag true
        // Half carry true

        val cycleCount = cpu.tick()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(4, cycleCount)
        assertEquals(0b11111110u, cpu.AF.left)
        assertEquals(0b00110000u, cpu.AF.right)
    }

    @Test
    fun `ADD A, B | overflow to 0`() {
        memory.set(0x100u, 0x80u)
        cpu.AF.left = 0b11111111u
        cpu.BC.left = 0b00000001u

        // 11111111 + 00000001 = 1 0000 0000
        // Carry flag true
        // Half carry true
        // Zero flag true

        val cycleCount = cpu.tick()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(4, cycleCount)
        assertEquals(0b0u, cpu.AF.left)
        assertEquals(0b10110000u, cpu.AF.right)
    }

    @Test
    fun `ADC A, A | no carry`() {
        memory.set(0x100u, 0x8Fu)
        cpu.AF.left = 0b01000011u
        cpu.AF.right = 0u

        // 01000011 + 01000011 = 10000110
        // Carry flag false
        // Half carry false

        val cycleCount = cpu.tick()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(4, cycleCount)
        assertEquals(0b10000110u, cpu.AF.left)
        assertEquals(0b00000000u, cpu.AF.right)
    }

    @Test
    fun `ADC A, A | zero`() {
        memory.set(0x100u, 0x8Fu)
        cpu.AF.left = 0b00000000u
        cpu.AF.right = 0u

        val cycleCount = cpu.tick()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(4, cycleCount)
        assertEquals(0b00000000u, cpu.AF.left)
        assertEquals(0b10000000u, cpu.AF.right)
    }

    @Test
    fun `ADC A, A | carry`() {
        memory.set(0x100u, 0x8Fu)
        cpu.AF.left = 0b11000011u
        cpu.AF.right = 0u

        // 11000011 + 11000011 = 110000110 => 10000110
        // Carry flag true
        // Half carry false

        val cycleCount = cpu.tick()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(4, cycleCount)
        assertEquals(0b10000110u, cpu.AF.left)
        assertEquals(0b00010000u, cpu.AF.right)
    }

    @Test
    fun `ADC A, A | half carry`() {
        memory.set(0x100u, 0x8Fu)
        cpu.AF.left = 0b01001100u
        cpu.AF.right = 0u

        // 01001100 + 01001100 = 10011000
        // Carry flag false
        // Half carry true

        val cycleCount = cpu.tick()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(4, cycleCount)
        assertEquals(0b10011000u, cpu.AF.left)
        assertEquals(0b00100000u, cpu.AF.right)
    }

    @Test
    fun `ADC A, A | carry flag`() {
        memory.set(0x100u, 0x8Fu)
        cpu.AF.left = 0b01000011u
        cpu.AF.right = 0b00010000u

        // 01000011 + 01000011 + 1 = 10000111
        // Carry flag false
        // Half carry false

        val cycleCount = cpu.tick()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(4, cycleCount)
        assertEquals(0b10000111u, cpu.AF.left)
        assertEquals(0b00000000u, cpu.AF.right)
    }

    @Test
    fun `ADC A, B | Overflow to 0`() {
        memory.set(0x100u, 0x88u)
        cpu.AF.left = 0b11111111u
        cpu.AF.right= 0u
        cpu.BC.left = 1u

        // 11111111 + 00000001 = 100000000 => 00000000
        // Carry flag true
        // Half carry false
        // Zero flag true

        val cycleCount = cpu.tick()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(4, cycleCount)
        assertEquals(0b00000000u, cpu.AF.left)
        assertEquals(0b10010000u, cpu.AF.right)
    }

    @Test
    fun `SUB A, B | no carry`() {
        memory.set(0x100u, 0x90u)
        cpu.AF.left = 0b01000011u
        cpu.BC.left = 0b01000001u

        // 01000011 - 0100001 = 00000010
        // Carry flag false
        // Half carry false

        val cycleCount = cpu.tick()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(4, cycleCount)
        assertEquals(0b00000010u, cpu.AF.left)
        assertEquals(0b01000000u, cpu.AF.right)
    }

    @Test
    fun `SUB A, B | carry`() {
        memory.set(0x100u, 0x90u)
        cpu.AF.left = 0b00000000u
        cpu.BC.left = 0b10000000u

        // 00000000 - 1000000 = (11111.....)10000000
        // Carry flag true
        // Half carry false

        val cycleCount = cpu.tick()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(4, cycleCount)
        assertEquals(0b10000000u, cpu.AF.left)
        assertEquals(0b01010000u, cpu.AF.right)
    }

    @Test
    fun `SUB A, B | half carry`() {
        memory.set(0x100u, 0x90u)
        cpu.AF.left = 0b00010000u
        cpu.BC.left = 0b00001000u

        // 00010000 - 0001000 = 00001000
        // Carry flag false
        // Half carry true

        val cycleCount = cpu.tick()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(4, cycleCount)
        assertEquals(0b00001000u, cpu.AF.left)
        assertEquals(0b01100000u, cpu.AF.right)
    }


    @Test
    fun `SUB A, A | zero`() {
        memory.set(0x100u, 0x97u)
        cpu.AF.left = 0b00100100u

        val cycleCount = cpu.tick()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(4, cycleCount)
        assertEquals(0b00000000u, cpu.AF.left)
        assertEquals(0b11000000u, cpu.AF.right)
    }

    @Test
    fun `SUB A, B | lower limit`() {
        memory.set(0x100u, 0x90u)
        cpu.AF.left = 0b00000000u
        cpu.BC.left = 0b10000000u

        // 00000000 - 1000000 = (1111...) 10000000
        // Carry flag false
        // Half carry true

        val cycleCount = cpu.tick()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(4, cycleCount)
        assertEquals(0b10000000u, cpu.AF.left)
        assertEquals(0b01010000u, cpu.AF.right)
    }

    @Test
    fun `SUBC A, B | no carry`() {
        memory.set(0x100u, 0x98u)
        cpu.AF.left = 0b01000011u
        cpu.AF.right = 0u
        cpu.BC.left = 0b01000001u

        // 01000011 - 0100001 = 00000010
        // Carry flag false
        // Half carry false

        val cycleCount = cpu.tick()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(4, cycleCount)
        assertEquals(0b00000010u, cpu.AF.left)
        assertEquals(0b01000000u, cpu.AF.right)
    }

    @Test
    fun `SUBC A, B | carry`() {
        memory.set(0x100u, 0x98u)
        cpu.AF.left = 0b00000000u
        cpu.AF.right = 0u
        cpu.BC.left = 0b10000000u

        // 00000000 - 1000000 = (11111.....)10000000
        // Carry flag true
        // Half carry false

        val cycleCount = cpu.tick()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(4, cycleCount)
        assertEquals(0b10000000u, cpu.AF.left)
        assertEquals(0b01010000u, cpu.AF.right)
    }

    @Test
    fun `SUBC A, B | half carry`() {
        memory.set(0x100u, 0x98u)
        cpu.AF.left = 0b00010000u
        cpu.AF.right = 0u
        cpu.AF.right = 0u
        cpu.BC.left = 0b00001000u

        // 00010000 - 0001000 = 00001000
        // Carry flag false
        // Half carry true

        val cycleCount = cpu.tick()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(4, cycleCount)
        assertEquals(0b00001000u, cpu.AF.left)
        assertEquals(0b01100000u, cpu.AF.right)
    }


    @Test
    fun `SUBC A, A | zero`() {
        memory.set(0x100u, 0x9Fu)
        cpu.AF.left = 0b00100100u
        cpu.AF.right = 0u

        val cycleCount = cpu.tick()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(4, cycleCount)
        assertEquals(0b00000000u, cpu.AF.left)
        assertEquals(0b11000000u, cpu.AF.right)
    }

    @Test
    fun `SUBC A, B | carry flag`() {
        memory.set(0x100u, 0x98u)
        cpu.AF.left = 0b00100100u
        cpu.BC.left = 0b00100000u
        cpu.AF.right = 0b00010000u

        // 00100100 - 00100000 - 00000001 = 00000100 - 00000001 = 00000011

        val cycleCount = cpu.tick()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(4, cycleCount)
        assertEquals(0b00000011u, cpu.AF.left)
        assertEquals(0b01000000u, cpu.AF.right)
    }

    @Test
    fun `AND A, B`() {
        memory.set(0x100u, 0xA0u)
        cpu.AF.left = 0b00100110u
        cpu.BC.left = 0b00101100u

        val cycleCount = cpu.tick()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(4, cycleCount)
        assertEquals(0b00100100u, cpu.AF.left)
        assertEquals(0b00100000u, cpu.AF.right)
    }

    @Test
    fun `OR A, B`() {
        memory.set(0x100u, 0xB0u)
        cpu.AF.left = 0b00100110u
        cpu.BC.left = 0b00101100u

        val cycleCount = cpu.tick()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(4, cycleCount)
        assertEquals(0b00101110u, cpu.AF.left)
        assertEquals(0b00000000u, cpu.AF.right)
    }

    @Test
    fun `XOR A, B`() {
        memory.set(0x100u, 0xA8u)
        cpu.AF.left = 0b00100110u
        cpu.BC.left = 0b00101100u

        val cycleCount = cpu.tick()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(4, cycleCount)
        assertEquals(0b00001010u, cpu.AF.left)
        assertEquals(0b00000000u, cpu.AF.right)
    }

    @Test
    fun `CP A, A | zero`() {
        memory.set(0x100u, 0xBFu)
        cpu.AF.left = 0b00100100u

        val cycleCount = cpu.tick()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(4, cycleCount)
        assertEquals(0b00100100u, cpu.AF.left)
        assertEquals(0b11000000u, cpu.AF.right)
    }

    @Test
    fun `CP A, B | different`() {
        memory.set(0x100u, 0xB8u)
        cpu.AF.left = 0b00100100u
        cpu.BC.left = 0b00100101u

        val cycleCount = cpu.tick()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(4, cycleCount)
        assertEquals(0b00100100u, cpu.AF.left)
        assertEquals(0b01110000u, cpu.AF.right)
    }

    @Test
    fun `INC A`() {
        memory.set(0x100u, 0x3Cu)
        cpu.AF.left = 0b00100100u
        cpu.AF.right = 0u

        val cycleCount = cpu.tick()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(4, cycleCount)
        assertEquals(0b00100101u, cpu.AF.left)
        assertEquals(0b00000000u, cpu.AF.right)
    }

    @Test
    fun `INC A | Set zero flag`() {
        memory.set(0x100u, 0x3Cu)
        cpu.AF.left = 0b11111111u
        cpu.AF.right = 0b00000000u

        val cycleCount = cpu.tick()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(4, cycleCount)
        assertEquals(0b00000000u, cpu.AF.left)
        // Zero flag is set
        assertEquals(0b10100000u, cpu.AF.right)
    }

    @Test
    fun `INC A | Unset zero flag`() {
        memory.set(0x100u, 0x3Cu)
        cpu.AF.left = 0b00100100u
        cpu.AF.right = 0b10000000u // Zero flag is set

        val cycleCount = cpu.tick()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(4, cycleCount)
        assertEquals(0b00100101u, cpu.AF.left)
        // Zero flag is unset
        assertEquals(0b00000000u, cpu.AF.right)
    }

    @Test
    fun `INC A | Half carry`() {
        memory.set(0x100u, 0x3Cu)
        cpu.AF.left = 0b00101111u
        cpu.AF.right = 0u

        val cycleCount = cpu.tick()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(4, cycleCount)
        assertEquals(0b00110000u, cpu.AF.left)
        assertEquals(0b00100000u, cpu.AF.right)
    }

    @Test
    fun `DEC A`() {
        memory.set(0x100u, 0x3Du)
        cpu.AF.left = 0b00100100u
        cpu.AF.right = 0u

        val cycleCount = cpu.tick()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(4, cycleCount)
        assertEquals(0b00100011u, cpu.AF.left)
        assertEquals(0b00000000u, cpu.AF.right)
    }

    @Test
    fun `DEC A | Half carry`() {
        memory.set(0x100u, 0x3Du)
        cpu.AF.left = 0b00010000u
        cpu.AF.right = 0u

        val cycleCount = cpu.tick()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(4, cycleCount)
        assertEquals(0b00001111u, cpu.AF.left)
        assertEquals(0b00100000u, cpu.AF.right)
    }

    @Test
    fun `DEC A | Unset zero`() {
        memory.set(0x100u, 0x3Du)
        cpu.AF.left = 0b00010000u
        cpu.AF.right = 0b10000000u

        val cycleCount = cpu.tick()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(4, cycleCount)
        assertEquals(0b00001111u, cpu.AF.left)
        assertEquals(0b00100000u, cpu.AF.right)
    }

    @Test
    fun `DEC A | set zero`() {
        memory.set(0x100u, 0x3Du)
        cpu.AF.left = 0b00000001u
        cpu.AF.right = 0b00000000u

        val cycleCount = cpu.tick()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(4, cycleCount)
        assertEquals(0b00000000u, cpu.AF.left)
        assertEquals(0b10000000u, cpu.AF.right)
    }

    @Test
    fun `ADD-16 HL, BC`() {
        memory.set(0x100u, 0x09u)
        cpu.AF.right = 0u
        cpu.HL.setBoth(0x5555u)
        cpu.BC.setBoth(0x2222u)

        // 0x5555 + 0x2222 = 0x7777
        // Carry flag false
        // Half carry false

        val cycleCount = cpu.tick()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(8, cycleCount)
        assertEquals(0x7777u, cpu.HL.both())
        assertEquals(0b00000000u, cpu.AF.right)
    }

    @Test
    fun `ADD-16 HL, BC | Carry`() {
        memory.set(0x100u, 0x09u)
        cpu.HL.setBoth(0xF555u)
        cpu.BC.setBoth(0x2222u)
        cpu.AF.right = 0u

        // 0xF555 + 0x2222 = 0x11777
        // Carry flag true
        // Half carry false

        val cycleCount = cpu.tick()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(8, cycleCount)
        assertEquals(0x1777u, cpu.HL.both())
        assertEquals(0b00010000u, cpu.AF.right)
    }

    @Test
    fun `ADD-16 HL, BC | Half Carry`() {
        memory.set(0x100u, 0x09u)
        cpu.AF.right = 0u
        cpu.HL.setBoth(0x5F55u)
        cpu.BC.setBoth(0x2222u)

        // 0x5F55 + 0x2222 = 0x8177
        // Carry flag false
        // Half carry true

        val cycleCount = cpu.tick()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(8, cycleCount)
        assertEquals(0x8177u, cpu.HL.both())
        assertEquals(0b00100000u, cpu.AF.right)
    }

    @Test
    fun `ADD-16 SP, n`() {
        memory.set(0x100u, 0xE8u)
        memory.set(0x101u, 0x12u)
        cpu.AF.right = 0u
        cpu.stackPointer = 0x1234u

        // 0x1234 + 0x12 = 0x1246
        // Carry flag false
        // Half carry false

        val cycleCount = cpu.tick()
        assertEquals(0x102u, cpu.programCounter)
        assertEquals(16, cycleCount)
        assertEquals(0x1246u, cpu.stackPointer)
        assertEquals(0b00000000u, cpu.AF.right)
    }

    @Test
    fun `ADD-16 SP, n | Half carry`() {
        memory.set(0x100u, 0xE8u)
        memory.set(0x101u, 0x1Fu)
        cpu.stackPointer = 0x1234u
        cpu.AF.right = 0u

        // 0x1234 + 0x1F = 0x1253
        // Carry flag false
        // Half carry true

        val cycleCount = cpu.tick()
        assertEquals(0x102u, cpu.programCounter)
        assertEquals(16, cycleCount)
        assertEquals(0x1253u, cpu.stackPointer)
        assertEquals(0b00100000u, cpu.AF.right)
    }

    @Test
    fun `ADD-16 SP, n | carry`() {
        memory.set(0x100u, 0xE8u)
        memory.set(0x101u, 0xF1u)
        cpu.stackPointer = 0x1234u
        cpu.AF.right = 0u

        // 0x1234 + 0xF1 = 0x1325
        // Carry flag true
        // Half carry false

        val cycleCount = cpu.tick()
        assertEquals(0x102u, cpu.programCounter)
        assertEquals(16, cycleCount)
        assertEquals(0x1325u, cpu.stackPointer)
        assertEquals(0b00010000u, cpu.AF.right)
    }

    @Test
    fun `INC-16 BC`() {
        memory.set(0x100u, 0x03u)
        cpu.BC.setBoth(0x1234u)

        val cycleCount = cpu.tick()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(8, cycleCount)
        assertEquals(0x1235u, cpu.BC.both())
    }

    @Test
    fun `INC-16 BC | Upper limit`() {
        memory.set(0x100u, 0x03u)
        cpu.BC.setBoth(0xFFFFu)

        val cycleCount = cpu.tick()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(8, cycleCount)
        assertEquals(0x0000u, cpu.BC.both())
    }

    @Test
    fun `INC-16 BC | Split register`() {
        memory.set(0x100u, 0x03u)
        cpu.BC.setBoth(0x00FFu)

        val cycleCount = cpu.tick()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(8, cycleCount)
        assertEquals(0x0100u, cpu.BC.both())
    }

    @Test
    fun `DEC-16 BC`() {
        memory.set(0x100u, 0x0Bu)
        cpu.BC.setBoth(0x1234u)

        val cycleCount = cpu.tick()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(8, cycleCount)
        assertEquals(0x1233u, cpu.BC.both())
    }

    @Test
    fun `DEC-16 BC | lower limit`() {
        memory.set(0x100u, 0x0Bu)
        cpu.BC.setBoth(0x0000u)

        val cycleCount = cpu.tick()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(8, cycleCount)
        assertEquals(0xFFFFu, cpu.BC.both())
    }

    @Test
    fun `DEC-16 BC | Split register`() {
        memory.set(0x100u, 0x0Bu)
        cpu.BC.setBoth(0x0100u)

        val cycleCount = cpu.tick()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(8, cycleCount)
        assertEquals(0x00FFu, cpu.BC.both())
    }

    @Test
    fun `SWAP A`() {
        memory.set(0x100u, 0xCBu)
        memory.set(0x101u, 0x37u)
        cpu.AF.left = 0x12u

        val cycleCount = cpu.tick()
        assertEquals(0x102u, cpu.programCounter)
        assertEquals(8, cycleCount)
        assertEquals(0x21u, cpu.AF.left)
    }

    @Test
    fun `SWAP (HL)`() {
        memory.set(0x100u, 0xCBu)
        memory.set(0x101u, 0x36u)
        cpu.HL.setBoth(0x1234u)
        memory.set(0x1234u, 0x12u)

        val cycleCount = cpu.tick()
        assertEquals(0x102u, cpu.programCounter)
        assertEquals(16, cycleCount)
        assertEquals(0x21u, memory.get(0x1234u))
    }

    @Test
    fun `CPL A`() {
        memory.set(0x100u, 0x2Fu)
        cpu.AF.left = 0b11001010u

        val cycleCount = cpu.tick()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(4, cycleCount)
        assertEquals(0b00110101u, cpu.AF.left)
    }

    @Test
    fun `CCF`() {
        memory.set(0x100u, 0x3Fu)
        cpu.AF.right = 0b0101_0000u

        val cycleCount = cpu.tick()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(4, cycleCount)
        assertEquals(0b0000_0000u, cpu.AF.right)
    }

    @Test
    fun `SCF`() {
        memory.set(0x100u, 0x37u)
        cpu.AF.right = 0b0100_0000u

        val cycleCount = cpu.tick()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(4, cycleCount)
        assertEquals(0b0001_0000u, cpu.AF.right)
    }

    @Test
    fun `RLCA`() {
        memory.set(0x100u, 0x07u)
        cpu.AF.left = 0b1100_1010u
        cpu.AF.right = 0u

        val cycleCount = cpu.tick()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(4, cycleCount)
        assertEquals(0b1001_0101u, cpu.AF.left)
        assertEquals(0b0001_0000u, cpu.AF.right)
    }

    @Test
    fun `RLCA | Set zero flag`() {
        memory.set(0x100u, 0x07u)
        cpu.AF.left = 0b0000_0000u
        cpu.AF.right = 0u

        val cycleCount = cpu.tick()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(4, cycleCount)
        assertEquals(0b0000_0000u, cpu.AF.left)
        assertEquals(0b1000_0000u, cpu.AF.right)
    }

    @Test
    fun `RLCA | Unset zero flag`() {
        memory.set(0x100u, 0x07u)
        cpu.AF.left = 0b0000_0001u
        cpu.AF.right = 0b1000_0000u

        val cycleCount = cpu.tick()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(4, cycleCount)
        assertEquals(0b0000_0010u, cpu.AF.left)
        assertEquals(0b0000_0000u, cpu.AF.right)
    }

    @Test
    fun `RLA`() {
        memory.set(0x100u, 0x17u)
        cpu.AF.left = 0b0100_1010u
        cpu.AF.right = 0b0001_0000u

        val cycleCount = cpu.tick()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(4, cycleCount)
        assertEquals(0b1001_0101u, cpu.AF.left)
        assertEquals(0b0000_0000u, cpu.AF.right)
    }

    @Test
    fun `RLA | Set zero`() {
        memory.set(0x100u, 0x17u)
        cpu.AF.left = 0b1000_0000u
        cpu.AF.right = 0b0000_0000u

        val cycleCount = cpu.tick()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(4, cycleCount)
        assertEquals(0b0000_0000u, cpu.AF.left)
        assertEquals(0b1001_0000u, cpu.AF.right)
    }

    @Test
    fun `RRCA`() {
        memory.set(0x100u, 0x0Fu)
        cpu.AF.left = 0b0100_1011u
        cpu.AF.right = 0b0000_0000u

        val cycleCount = cpu.tick()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(4, cycleCount)
        assertEquals(0b10100_101u, cpu.AF.left)
        assertEquals(0b0001_0000u, cpu.AF.right)
    }

    @Test
    fun `RRCA | Set zero`() {
        memory.set(0x100u, 0x0Fu)
        cpu.AF.left = 0b0000_0000u
        cpu.AF.right = 0b0000_0000u

        val cycleCount = cpu.tick()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(4, cycleCount)
        assertEquals(0b0000_0000u, cpu.AF.left)
        assertEquals(0b1000_0000u, cpu.AF.right)
    }

    @Test
    fun `RRCA | Unset zero`() {
        memory.set(0x100u, 0x0Fu)
        cpu.AF.left = 0b0001_0000u
        cpu.AF.right = 0b1000_0000u

        val cycleCount = cpu.tick()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(4, cycleCount)
        assertEquals(0b0000_1000u, cpu.AF.left)
        assertEquals(0b0000_0000u, cpu.AF.right)
    }

    @Test
    fun `RRA`() {
        memory.set(0x100u, 0x1Fu)
        cpu.AF.left = 0b0100_1010u
        cpu.AF.right = 0b0001_0000u

        val cycleCount = cpu.tick()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(4, cycleCount)
        assertEquals(0b1010_0101u, cpu.AF.left)
        assertEquals(0b0000_0000u, cpu.AF.right)
    }

    @Test
    fun `RRA | Set zero`() {
        memory.set(0x100u, 0x1Fu)
        cpu.AF.left = 0b0000_0001u
        cpu.AF.right = 0b0000_0000u

        val cycleCount = cpu.tick()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(4, cycleCount)
        assertEquals(0b0000_0000u, cpu.AF.left)
        assertEquals(0b1001_0000u, cpu.AF.right)
    }

    @Test
    fun `RRA | Unset zero`() {
        memory.set(0x100u, 0x1Fu)
        cpu.AF.left = 0b0000_0010u
        cpu.AF.right = 0b1000_0000u

        val cycleCount = cpu.tick()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(4, cycleCount)
        assertEquals(0b0000_0001u, cpu.AF.left)
        assertEquals(0b0000_0000u, cpu.AF.right)
    }

    @Test
    fun `SRA  | MSB 0`() {
        memory.set(0x100u, 0xCBu)
        memory.set(0x101u, 0x2Fu)
        cpu.AF.left = 0b0100_1011u
        cpu.AF.right = 0b0000_0000u

        val cycleCount = cpu.tick()
        assertEquals(0x102u, cpu.programCounter)
        assertEquals(8, cycleCount)
        assertEquals(0b0010_0101u, cpu.AF.left)
        assertEquals(0b0001_0000u, cpu.AF.right)
    }

    @Test
    fun `SRA  | MSB 1`() {
        memory.set(0x100u, 0xCBu)
        memory.set(0x101u, 0x2Fu)
        cpu.AF.left = 0b1100_1010u
        cpu.AF.right = 0b0001_0000u

        val cycleCount = cpu.tick()
        assertEquals(0x102u, cpu.programCounter)
        assertEquals(8, cycleCount)
        assertEquals(0b1110_0101u, cpu.AF.left)
        assertEquals(0b0000_0000u, cpu.AF.right)
    }

    @Test
    fun `SRA  | Set Zero`() {
        memory.set(0x100u, 0xCBu)
        memory.set(0x101u, 0x2Fu)
        cpu.AF.left = 0b0000_0001u
        cpu.AF.right = 0b0000_0000u

        val cycleCount = cpu.tick()
        assertEquals(0x102u, cpu.programCounter)
        assertEquals(8, cycleCount)
        assertEquals(0b0000_0000u, cpu.AF.left)
        assertEquals(0b1001_0000u, cpu.AF.right)
    }

    @Test
    fun `SRA  | Unset Zero`() {
        memory.set(0x100u, 0xCBu)
        memory.set(0x101u, 0x2Fu)
        cpu.AF.left = 0b0000_0010u
        cpu.AF.right = 0b1000_0000u

        val cycleCount = cpu.tick()
        assertEquals(0x102u, cpu.programCounter)
        assertEquals(8, cycleCount)
        assertEquals(0b0000_0001u, cpu.AF.left)
        assertEquals(0b0000_0000u, cpu.AF.right)
    }

    @Test
    fun `SLA  | MSB 0`() {
        memory.set(0x100u, 0xCBu)
        memory.set(0x101u, 0x27u)
        cpu.AF.left = 0b0100_1011u
        cpu.AF.right = 0b0000_0000u

        val cycleCount = cpu.tick()
        assertEquals(0x102u, cpu.programCounter)
        assertEquals(8, cycleCount)
        assertEquals(0b1001_0110u, cpu.AF.left)
        assertEquals(0b0000_0000u, cpu.AF.right)
    }

    @Test
    fun `SLA  | MSB 1`() {
        memory.set(0x100u, 0xCBu)
        memory.set(0x101u, 0x27u)
        cpu.AF.left = 0b1100_1010u
        cpu.AF.right = 0b0000_0000u

        val cycleCount = cpu.tick()
        assertEquals(0x102u, cpu.programCounter)
        assertEquals(8, cycleCount)
        assertEquals(0b1001_0100u, cpu.AF.left)
        assertEquals(0b0001_0000u, cpu.AF.right)
    }

    @Test
    fun `SLA  | Set zero`() {
        memory.set(0x100u, 0xCBu)
        memory.set(0x101u, 0x27u)
        cpu.AF.left = 0b1000_0000u
        cpu.AF.right = 0b0000_0000u

        val cycleCount = cpu.tick()
        assertEquals(0x102u, cpu.programCounter)
        assertEquals(8, cycleCount)
        assertEquals(0b0000_0000u, cpu.AF.left)
        assertEquals(0b1001_0000u, cpu.AF.right)
    }

    @Test
    fun `SLA  | Unset zero`() {
        memory.set(0x100u, 0xCBu)
        memory.set(0x101u, 0x27u)
        cpu.AF.left = 0b0010_0000u
        cpu.AF.right = 0b1000_0000u

        val cycleCount = cpu.tick()
        assertEquals(0x102u, cpu.programCounter)
        assertEquals(8, cycleCount)
        assertEquals(0b0100_0000u, cpu.AF.left)
        assertEquals(0b0000_0000u, cpu.AF.right)
    }

    @Test
    fun `SRL`() {
        memory.set(0x100u, 0xCBu)
        memory.set(0x101u, 0x3Fu)
        cpu.AF.left = 0b1100_1011u
        cpu.AF.right = 0b0000_0000u

        val cycleCount = cpu.tick()
        assertEquals(0x102u, cpu.programCounter)
        assertEquals(8, cycleCount)
        assertEquals(0b0110_0101u, cpu.AF.left)
        assertEquals(0b0001_0000u, cpu.AF.right)
    }

    @Test
    fun `SRL | Set zero`() {
        memory.set(0x100u, 0xCBu)
        memory.set(0x101u, 0x3Fu)
        cpu.AF.left = 0b0000_0001u
        cpu.AF.right = 0b0000_0000u

        val cycleCount = cpu.tick()
        assertEquals(0x102u, cpu.programCounter)
        assertEquals(8, cycleCount)
        assertEquals(0b0000_0000u, cpu.AF.left)
        assertEquals(0b1001_0000u, cpu.AF.right)
    }

    @Test
    fun `SRL | Unset zero`() {
        memory.set(0x100u, 0xCBu)
        memory.set(0x101u, 0x3Fu)
        cpu.AF.left = 0b0000_0010u
        cpu.AF.right = 0b1000_0000u

        val cycleCount = cpu.tick()
        assertEquals(0x102u, cpu.programCounter)
        assertEquals(8, cycleCount)
        assertEquals(0b0000_0001u, cpu.AF.left)
        assertEquals(0b0000_0000u, cpu.AF.right)
    }

    @Test
    fun `BIT 0 B | 1`() {
        memory.set(0x100u, 0xCBu)
        memory.set(0x101u, 0x40u)
        cpu.BC.left = 0b0000_0001u
        cpu.AF.right = 0b0000_0000u

        val cycleCount = cpu.tick()
        assertEquals(0x102u, cpu.programCounter)
        assertEquals(8, cycleCount)
        assertEquals(0b0000_0001u, cpu.BC.left)
        assertEquals(0b1010_0000u, cpu.AF.right)
    }

    @Test
    fun `BIT 0 B | 0`() {
        memory.set(0x100u, 0xCBu)
        memory.set(0x101u, 0x40u)
        cpu.BC.left = 0b0000_0000u
        cpu.AF.right = 0b0000_0000u

        val cycleCount = cpu.tick()
        assertEquals(0x102u, cpu.programCounter)
        assertEquals(8, cycleCount)
        assertEquals(0b0000_0000u, cpu.BC.left)
        assertEquals(0b0010_0000u, cpu.AF.right)
    }

    @Test
    fun `BIT 7 A`() {
        memory.set(0x100u, 0xCBu)
        memory.set(0x101u, 0x7Fu)
        cpu.AF.left = 0b1000_0000u
        cpu.AF.right = 0b0000_0000u

        val cycleCount = cpu.tick()
        assertEquals(0x102u, cpu.programCounter)
        assertEquals(8, cycleCount)
        assertEquals(0b1000_0000u, cpu.AF.left)
        assertEquals(0b1010_0000u, cpu.AF.right)
    }

    @Test
    fun `SET 0 B | 1`() {
        memory.set(0x100u, 0xCBu)
        memory.set(0x101u, 0xC0u)
        cpu.BC.left = 0b0000_0001u
        cpu.AF.right = 0b1010_0000u

        val cycleCount = cpu.tick()
        assertEquals(0x102u, cpu.programCounter)
        assertEquals(8, cycleCount)
        assertEquals(0b0000_0001u, cpu.BC.left)
        assertEquals(0b1010_0000u, cpu.AF.right)
    }

    @Test
    fun `SET 0 B | 0`() {
        memory.set(0x100u, 0xCBu)
        memory.set(0x101u, 0xC0u)
        cpu.BC.left = 0b0000_0000u
        cpu.AF.right = 0b0000_0000u

        val cycleCount = cpu.tick()
        assertEquals(0x102u, cpu.programCounter)
        assertEquals(8, cycleCount)
        assertEquals(0b0000_0001u, cpu.BC.left)
        assertEquals(0b0000_0000u, cpu.AF.right)
    }

    @Test
    fun `SET 7 A`() {
        memory.set(0x100u, 0xCBu)
        memory.set(0x101u, 0xFFu)
        cpu.AF.left = 0b0000_0000u

        val cycleCount = cpu.tick()
        assertEquals(0x102u, cpu.programCounter)
        assertEquals(8, cycleCount)
        assertEquals(0b1000_0000u, cpu.AF.left)
    }

    @Test
    fun `RES 0 B | 1`() {
        memory.set(0x100u, 0xCBu)
        memory.set(0x101u, 0x80u)
        cpu.BC.left = 0b0000_0001u
        cpu.AF.right = 0b1010_0000u

        val cycleCount = cpu.tick()
        assertEquals(0x102u, cpu.programCounter)
        assertEquals(8, cycleCount)
        assertEquals(0b0000_0000u, cpu.BC.left)
        assertEquals(0b1010_0000u, cpu.AF.right)
    }

    @Test
    fun `RES 0 B | 0`() {
        memory.set(0x100u, 0xCBu)
        memory.set(0x101u, 0x80u)
        cpu.BC.left = 0b0000_0000u
        cpu.AF.right = 0b0000_0000u

        val cycleCount = cpu.tick()
        assertEquals(0x102u, cpu.programCounter)
        assertEquals(8, cycleCount)
        assertEquals(0b0000_0000u, cpu.BC.left)
        assertEquals(0b0000_0000u, cpu.AF.right)
    }

    @Test
    fun `RES 7 A`() {
        memory.set(0x100u, 0xCBu)
        memory.set(0x101u, 0xBFu)
        cpu.AF.left = 0b1000_0000u

        val cycleCount = cpu.tick()
        assertEquals(0x102u, cpu.programCounter)
        assertEquals(8, cycleCount)
        assertEquals(0b0000_0000u, cpu.AF.left)
    }

    @Test
    fun `JP nn`() {
        memory.set(0x100u, 0xC3u)
        memory.set(0x101u, 0x11u)
        memory.set(0x102u, 0x12u)

        val cycleCount = cpu.tick()
        assertEquals(0x1211u, cpu.programCounter)
        assertEquals(12, cycleCount)
    }

    @Test
    fun `JP NZ nn | Set`() {
        memory.set(0x100u, 0xC2u)
        memory.set(0x101u, 0x11u)
        memory.set(0x102u, 0x12u)
        cpu.AF.right = 0b1000_0000u

        val cycleCount = cpu.tick()
        assertEquals(0x103u, cpu.programCounter)
        assertEquals(12, cycleCount)
    }

    @Test
    fun `JP NZ nn | Reset`() {
        memory.set(0x100u, 0xC2u)
        memory.set(0x101u, 0x11u)
        memory.set(0x102u, 0x12u)
        cpu.AF.right = 0b0000_0000u

        val cycleCount = cpu.tick()
        assertEquals(0x1211u, cpu.programCounter)
        assertEquals(12, cycleCount)
    }

    @Test
    fun `JP (HL)`() {
        memory.set(0x100u, 0xE9u)
        cpu.HL.left = 0x11u
        cpu.HL.right = 0x12u

        val cycleCount = cpu.tick()
        assertEquals(0x1112u, cpu.programCounter)
        assertEquals(12, cycleCount)
    }

    @Test
    fun `JR n | Positive`() {
        memory.set(0x100u, 0x18u)
        memory.set(0x101u, 0x10u)

        val cycleCount = cpu.tick()
        assertEquals(0x112u, cpu.programCounter)
        assertEquals(8, cycleCount)
    }

    @Test
    fun `JR n | Negative`() {
        memory.set(0x100u, 0x18u)
        memory.set(0x101u, 0xF0u)

        val cycleCount = cpu.tick()
        assertEquals(0xF2u, cpu.programCounter)
        assertEquals(8, cycleCount)
    }

    @Test
    fun `CALL nn`() {
        memory.set(0x100u, 0xCDu)
        memory.set(0x101u, 0x11u)
        memory.set(0x102u, 0x12u)
        cpu.stackPointer = 0x3456u

        val cycleCount = cpu.tick()
        assertEquals(0x1211u, cpu.programCounter)
        assertEquals(0x3454u, cpu.stackPointer)
        assertEquals(0x03u, memory.get(cpu.stackPointer))
        assertEquals(0x01u, memory.get((cpu.stackPointer + 1u).toUShort()))
        assertEquals(12, cycleCount)
    }

    @Test
    fun `RST 00`() {
        memory.set(0x100u, 0xC7u)
        cpu.stackPointer = 0x3456u

        val cycleCount = cpu.tick()
        assertEquals(0x0000u, cpu.programCounter)
        assertEquals(0x3454u, cpu.stackPointer)
        assertEquals(0x01u, memory.get(cpu.stackPointer))
        assertEquals(0x01u, memory.get((cpu.stackPointer + 1u).toUShort()))
        assertEquals(32, cycleCount)
    }

    @Test
    fun `RST 38`() {
        memory.set(0x100u, 0xFFu)
        cpu.stackPointer = 0x3456u

        val cycleCount = cpu.tick()
        assertEquals(0x0038u, cpu.programCounter)
        assertEquals(0x3454u, cpu.stackPointer)
        assertEquals(0x01u, memory.get(cpu.stackPointer))
        assertEquals(0x01u, memory.get((cpu.stackPointer + 1u).toUShort()))
        assertEquals(32, cycleCount)
    }

    @Test
    fun `Return`() {
        memory.set(0x100u, 0xC9u)
        cpu.stackPointer = 0x3456u
        memory.set(0x3456u, 0x11u)
        memory.set(0x3457u, 0x12u)

        val cycleCount = cpu.tick()
        assertEquals(0x1211u, cpu.programCounter)
        assertEquals(0x3458u, cpu.stackPointer)
        assertEquals(8, cycleCount)
    }

    @Test
    fun `DAA - Addition - no correction`() {
        memory.set(0x100u, 0x27u)
        cpu.AF.left = 0b0000_0001u
        cpu.AF.right = 0x0u

        val cycleCount = cpu.tick()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(0b000_0001u, cpu.AF.left)
        assertEquals(4, cycleCount)
    }

    @Test
    fun `DAA - Addition - bottom correction`() {
        memory.set(0x100u, 0x27u)
        cpu.AF.left = 0b0000_1100u // last digit is 12 which more than 0x9, should be corrected by 0x6
        cpu.AF.right = 0x0u

        val cycleCount = cpu.tick()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(0b0001_0010u, cpu.AF.left)
        assertEquals(4, cycleCount)
    }

    @Test
    fun `DAA - Addition - Top correction`() {
        memory.set(0x100u, 0x27u)
        cpu.AF.left = 0b1100_0000u // first digit is 12 which more than 0x90, should be corrected by 0x60
        cpu.AF.right = 0x0u

        val cycleCount = cpu.tick()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(0b0010_0000u, cpu.AF.left)
        assertEquals(true, cpu.AF.getCarryFlag())
        assertEquals(4, cycleCount)
    }

    @Test
    fun `DAA - Addition - Full correction`() {
        memory.set(0x100u, 0x27u)
        cpu.AF.left = 0b1100_1100u // Both digits are 12 and both should be corrected
        cpu.AF.right = 0x0u

        val cycleCount = cpu.tick()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(0b0011_0010u, cpu.AF.left)
        assertEquals(true, cpu.AF.getCarryFlag())
        assertEquals(4, cycleCount)
    }

    @Test
    fun `DAA - Addition - Half Carry`() {
        memory.set(0x100u, 0x27u)
        cpu.AF.left = 0b0000_0010u // Last digit is 2 and should not be corrected
        cpu.AF.right = 0b0010_0000u // But there is a half carry which forces the correction

        val cycleCount = cpu.tick()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(0b0000_1000u, cpu.AF.left)
        assertEquals(false, cpu.AF.getHalfCarryFlag())
        assertEquals(4, cycleCount)
    }

    @Test
    fun `DAA - Addition - Carry`() {
        memory.set(0x100u, 0x27u)
        cpu.AF.left = 0b0000_0010u // First digit is 0 and should not be corrected
        cpu.AF.right = 0b0001_0000u // But there is a carry which forces the correction

        val cycleCount = cpu.tick()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(0b0110_0010u, cpu.AF.left)
        assertEquals(true, cpu.AF.getCarryFlag()) // Carry flag does not get reset for some reasons
        assertEquals(4, cycleCount)
    }

    @Test
    fun `DAA - Addition - both carry`() {
        memory.set(0x100u, 0x27u)
        cpu.AF.left = 0b0000_0010u // No digit should be corrected
        cpu.AF.right = 0b0011_0000u // But there is are 2 carry which forces the correction

        val cycleCount = cpu.tick()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(0b0110_1000u, cpu.AF.left)
        assertEquals(true, cpu.AF.getCarryFlag()) // Carry flag does not get reset for some reasons
        assertEquals(false, cpu.AF.getHalfCarryFlag()) // Half Carry flag does get reset
        assertEquals(4, cycleCount)
    }

    @Test
    fun `DAA - subtraction - Half Carry`() {
        memory.set(0x100u, 0x27u)
        cpu.AF.left = 0b0000_1010u // Last digit is 10
        cpu.AF.right = 0b0110_0000u // The half carry asks for a correction

        val cycleCount = cpu.tick()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(0b0000_0100u, cpu.AF.left)
        assertEquals(false, cpu.AF.getHalfCarryFlag())
        assertEquals(4, cycleCount)
    }

    @Test
    fun `DAA - subtraction - Carry`() {
        memory.set(0x100u, 0x27u)
        cpu.AF.left = 0b1010_0000u // First digit is 10
        cpu.AF.right = 0b0101_0000u // The carry asks for a correction

        val cycleCount = cpu.tick()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(0b0100_0000u, cpu.AF.left)
        assertEquals(true, cpu.AF.getCarryFlag()) // Flag should not be reset for some reason
        assertEquals(4, cycleCount)
    }

    @Test
    fun `DAA - subtraction - both carry`() {
        memory.set(0x100u, 0x27u)
        cpu.AF.left = 0b1010_1010u // Both digits are 10
        cpu.AF.right = 0b0111_0000u // And there is are 2 carry which forces the correction

        val cycleCount = cpu.tick()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(0b0100_0100u, cpu.AF.left)
        assertEquals(true, cpu.AF.getCarryFlag()) // Carry flag does not get reset for some reasons
        assertEquals(false, cpu.AF.getHalfCarryFlag()) // Half Carry flag does get reset
        assertEquals(4, cycleCount)
    }
}