import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@ExperimentalUnsignedTypes
class CPUTest {

    lateinit var cpu: CPU
    lateinit var memory: Memory

    @BeforeTest
    fun setup() {
        memory = Memory()
        cpu = CPU(memory)
        cpu.stackPointer = 0x100u
    }
    // =============================================
    // LD nn,n => Load into register the value at PC
    // =============================================

    @Test
    fun `LD B, n`() {
        memory.set(0x100u, 0x06u) // LD B, n
        memory.set(0x101u, 0x11u) // n

        val cycleCount = cpu.fetch()
        assertEquals(0x11u, cpu.BC.left) // B loaded nn
        assertEquals(0x102u, cpu.programCounter) // forward 2 steps
        assertEquals(8, cycleCount)
    }

    @Test
    fun `LD C, n`() {
        memory.set(0x100u, 0x0Eu) // LD C, n
        memory.set(0x101u, 0x11u) // n

        val cycleCount = cpu.fetch()
        assertEquals(0x11u, cpu.BC.right) // C loaded nn
        assertEquals(0x102u, cpu.programCounter) // forward 2 steps
        assertEquals(8, cycleCount)
    }

    @Test
    fun `LD D, n`() {
        memory.set(0x100u, 0x16u) // LD D, n
        memory.set(0x101u, 0x11u) // n

        val cycleCount = cpu.fetch()
        assertEquals(0x11u, cpu.DE.left) // B loaded nn
        assertEquals(0x102u, cpu.programCounter) // forward 2 steps
        assertEquals(8, cycleCount)
    }

    @Test
    fun `LD E, n`() {
        memory.set(0x100u, 0x1Eu) // LD E, n
        memory.set(0x101u, 0x11u) // n

        val cycleCount = cpu.fetch()
        assertEquals(0x11u, cpu.DE.right) // C loaded nn
        assertEquals(0x102u, cpu.programCounter) // forward 2 steps
        assertEquals(8, cycleCount)
    }

    @Test
    fun `LD H, n`() {
        memory.set(0x100u, 0x26u) // LD H, n
        memory.set(0x101u, 0x11u) // n

        val cycleCount = cpu.fetch()
        assertEquals(0x11u, cpu.HL.left) // B loaded nn
        assertEquals(0x102u, cpu.programCounter) // forward 2 steps
        assertEquals(8, cycleCount)
    }

    @Test
    fun `LD L, n`() {
        memory.set(0x100u, 0x2Eu) // LD L, n
        memory.set(0x101u, 0x11u) // n

        val cycleCount = cpu.fetch()
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

        val cycleCount = cpu.fetch()
        assertEquals(0x11u, cpu.AF.left)
        assertEquals(0x101u, cpu.programCounter) // forward 1 step
        assertEquals(4, cycleCount)
    }

    @Test
    fun `LD A, B`() {
        memory.set(0x100u, 0x78u) // LD A, B
        cpu.BC.left = 0x11u

        val cycleCount = cpu.fetch()
        assertEquals(0x11u, cpu.AF.left)
        assertEquals(0x101u, cpu.programCounter) // forward 1 step
        assertEquals(4, cycleCount)
    }

    @Test
    fun `LD A, C`() {
        memory.set(0x100u, 0x79u) // LD A, C
        cpu.BC.right = 0x11u

        val cycleCount = cpu.fetch()
        assertEquals(0x11u, cpu.AF.left)
        assertEquals(0x101u, cpu.programCounter) // forward 1 step
        assertEquals(4, cycleCount)
    }

    @Test
    fun `LD A, D`() {
        memory.set(0x100u, 0x7Au) // LD A, D
        cpu.DE.left = 0x11u

        val cycleCount = cpu.fetch()
        assertEquals(0x11u, cpu.AF.left)
        assertEquals(0x101u, cpu.programCounter) // forward 1 step
        assertEquals(4, cycleCount)
    }

    @Test
    fun `LD A, E`() {
        memory.set(0x100u, 0x7Bu) // LD A, E
        cpu.DE.right = 0x11u

        val cycleCount = cpu.fetch()
        assertEquals(0x11u, cpu.AF.left)
        assertEquals(0x101u, cpu.programCounter) // forward 1 step
        assertEquals(4, cycleCount)
    }

    @Test
    fun `LD A, H`() {
        memory.set(0x100u, 0x7Cu) // LD A, H
        cpu.HL.left = 0x11u

        val cycleCount = cpu.fetch()
        assertEquals(0x11u, cpu.AF.left)
        assertEquals(0x101u, cpu.programCounter) // forward 1 step
        assertEquals(4, cycleCount)
    }

    @Test
    fun `LD A, L`() {
        memory.set(0x100u, 0x7Du) // LD A, L
        cpu.HL.right = 0x11u

        val cycleCount = cpu.fetch()
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

        val cycleCount = cpu.fetch()
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

        val cycleCount = cpu.fetch()
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

        val cycleCount = cpu.fetch()
        assertEquals(0x13u, cpu.AF.left)
        assertEquals(0x101u, cpu.programCounter) // forward 1 step
        assertEquals(8, cycleCount)
    }

    @Test
    fun `LD A, (nn)`() {
        memory.set(0x100u, 0xFAu) // LD A, (nn)
        memory.set(0x101u, 0x12u)
        memory.set(0x102u, 0x11u)
        memory.set(0x1211u, 0x13u)

        val cycleCount = cpu.fetch()
        assertEquals(0x13u, cpu.AF.left)
        assertEquals(0x103u, cpu.programCounter) // forward 3 steps
        assertEquals(16, cycleCount)
    }

    @Test
    fun `LD A, #`() {
        memory.set(0x100u, 0x3Eu) // LD A, #
        memory.set(0x101u, 0x12u)

        val cycleCount = cpu.fetch()
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

        val cycleCount = cpu.fetch()
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

        val cycleCount = cpu.fetch()
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

        val cycleCount = cpu.fetch()
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

        val cycleCount = cpu.fetch()
        assertEquals(0x13u, memory.get(0x1211u))
        assertEquals(0x101u, cpu.programCounter) // forward 1 step
        assertEquals(8, cycleCount)
    }

    @Test
    fun `LD (HL), H`() {
        memory.set(0x100u, 0x74u) // LD (HL), H
        cpu.HL.left = 0x12u
        cpu.HL.right = 0x11u

        val cycleCount = cpu.fetch()
        assertEquals(0x12u, memory.get(0x1211u))
        assertEquals(0x101u, cpu.programCounter) // forward 1 step
        assertEquals(8, cycleCount)
    }

    @Test
    fun `LD (HL), L`() {
        memory.set(0x100u, 0x75u) // LD (HL), L
        cpu.HL.left = 0x12u
        cpu.HL.right = 0x11u

        val cycleCount = cpu.fetch()
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

        val cycleCount = cpu.fetch()
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

        val cycleCount = cpu.fetch()
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

        val cycleCount = cpu.fetch()
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

        val cycleCount = cpu.fetch()
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

        val cycleCount = cpu.fetch()
        assertEquals(0x13u, memory.get(0x1211u))
        assertEquals(0x103u, cpu.programCounter) // forward 3 steps
        assertEquals(16, cycleCount)
    }

    @Test
    fun `LD A,($FF00+C)`() {
        memory.set(0x100u, 0xF2u)
        cpu.BC.right = 0x12u
        memory.set(0xFF12u, 0x13u)

        val cycleCount = cpu.fetch()
        assertEquals(0x13u, cpu.AF.left)
        assertEquals(0x101u, cpu.programCounter) // forward 1 steps
        assertEquals(8, cycleCount)
    }

    @Test
    fun `LD ($FF00+C), A`() {
        memory.set(0x100u, 0xE2u)
        cpu.AF.left = 0x13u
        cpu.BC.right = 0x12u

        val cycleCount = cpu.fetch()
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

        val cycleCount = cpu.fetch()
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


        val cycleCount = cpu.fetch()
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

        val cycleCount = cpu.fetch()
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

        val cycleCount = cpu.fetch()
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

        val cycleCount = cpu.fetch()
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

        val cycleCount = cpu.fetch()
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

        val cycleCount = cpu.fetch()
        assertEquals(0x102u, cpu.programCounter) // forward 2 steps
        assertEquals(12, cycleCount)
        assertEquals(0x13u, memory.get(0xFF11u))
    }

    @Test
    fun `LDH A,(n)`() {
        memory.set(0x100u, 0xF0u)
        memory.set(0x101u, 0x11u)
        memory.set(0xFF11u, 0x13u)

        val cycleCount = cpu.fetch()
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

        val cycleCount = cpu.fetch()
        assertEquals(0x103u, cpu.programCounter) // forward 3 steps
        assertEquals(12, cycleCount)
        assertEquals(0x1112u, cpu.BC.both())
    }

    @Test
    fun `LD DE, nn`() {
        memory.set(0x100u, 0x11u)
        memory.set(0x101u, 0x11u)
        memory.set(0x102u, 0x12u)

        val cycleCount = cpu.fetch()
        assertEquals(0x103u, cpu.programCounter) // forward 3 steps
        assertEquals(12, cycleCount)
        assertEquals(0x1112u, cpu.DE.both())
    }

    @Test
    fun `LD HL, nn`() {
        memory.set(0x100u, 0x21u)
        memory.set(0x101u, 0x11u)
        memory.set(0x102u, 0x12u)

        val cycleCount = cpu.fetch()
        assertEquals(0x103u, cpu.programCounter) // forward 3 steps
        assertEquals(12, cycleCount)
        assertEquals(0x1112u, cpu.HL.both())
    }

    @Test
    fun `LD SP, nn`() {
        memory.set(0x100u, 0x31u)
        memory.set(0x101u, 0x11u)
        memory.set(0x102u, 0x12u)

        val cycleCount = cpu.fetch()
        assertEquals(0x103u, cpu.programCounter) // forward 3 steps
        assertEquals(12, cycleCount)
        assertEquals(0x1112u, cpu.stackPointer)
    }

    @Test
    fun `LD SP, HL`() {
        memory.set(0x100u, 0xF9u)
        cpu.HL.setBoth(0x1213u)

        val cycleCount = cpu.fetch()
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

        val cycleCount = cpu.fetch()
        assertEquals(0x103u, cpu.programCounter) // forward 3 steps
        assertEquals(20, cycleCount)
        assertEquals(0x34u, memory.get(0x1112u))
        assertEquals(0x56u, memory.get(0x1113u))
    }

    @Test
    fun `PUSH AF`() {
        memory.set(0x100u, 0xF5u)
        cpu.stackPointer = 0x3456u
        cpu.AF.setBoth(0x1234u)

        val cycleCount = cpu.fetch()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(16, cycleCount)
        assertEquals(0x12u, memory.get(0x3455u))
        assertEquals(0x34u, memory.get(0x3454u))
        assertEquals(0x3454u, cpu.stackPointer)
    }

    @Test
    fun `POP AF`() {
        memory.set(0x100u, 0xF1u)
        cpu.stackPointer = 0x3456u
        memory.set(0x3456u, 0x12u)
        memory.set(0x3457u, 0x34u)

        val cycleCount = cpu.fetch()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(16, cycleCount)
        assertEquals(0x34u, cpu.AF.left)
        assertEquals(0x12u, cpu.AF.right)
        assertEquals(0x3458u, cpu.stackPointer)
    }

    @Test
    fun `ADD A, A | no carry`() {
        memory.set(0x100u, 0x87u)
        cpu.AF.left = 0b01000011u

        // 01000011 + 01000011 = 10000110
        // Carry flag false
        // Half carry false

        val cycleCount = cpu.fetch()
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

        val cycleCount = cpu.fetch()
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

        val cycleCount = cpu.fetch()
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

        val cycleCount = cpu.fetch()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(4, cycleCount)
        assertEquals(0b11111110u, cpu.AF.left)
        assertEquals(0b00110000u, cpu.AF.right)
    }

    @Test
    fun `ADC A, A | no carry`() {
        memory.set(0x100u, 0x8Fu)
        cpu.AF.left = 0b01000011u

        // 01000011 + 01000011 = 10000110
        // Carry flag false
        // Half carry false

        val cycleCount = cpu.fetch()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(4, cycleCount)
        assertEquals(0b10000110u, cpu.AF.left)
        assertEquals(0b00000000u, cpu.AF.right)
    }

    @Test
    fun `ADC A, A | zero`() {
        memory.set(0x100u, 0x8Fu)
        cpu.AF.left = 0b00000000u

        val cycleCount = cpu.fetch()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(4, cycleCount)
        assertEquals(0b00000000u, cpu.AF.left)
        assertEquals(0b10000000u, cpu.AF.right)
    }

    @Test
    fun `ADC A, A | carry`() {
        memory.set(0x100u, 0x8Fu)
        cpu.AF.left = 0b11000011u

        // 11000011 + 11000011 = 110000110 => 10000110
        // Carry flag true
        // Half carry false

        val cycleCount = cpu.fetch()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(4, cycleCount)
        assertEquals(0b10000110u, cpu.AF.left)
        assertEquals(0b00010000u, cpu.AF.right)
    }

    @Test
    fun `ADC A, A | half carry`() {
        memory.set(0x100u, 0x8Fu)
        cpu.AF.left = 0b01001100u

        // 01001100 + 01001100 = 10011000
        // Carry flag false
        // Half carry true

        val cycleCount = cpu.fetch()
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

        val cycleCount = cpu.fetch()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(4, cycleCount)
        assertEquals(0b10000111u, cpu.AF.left)
        assertEquals(0b00000000u, cpu.AF.right)
    }

    @Test
    fun `SUB A, B | no carry`() {
        memory.set(0x100u, 0x90u)
        cpu.AF.left = 0b01000011u
        cpu.BC.left = 0b01000001u

        // 01000011 - 0100001 = 00000010
        // Carry flag false
        // Half carry false

        val cycleCount = cpu.fetch()
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

        val cycleCount = cpu.fetch()
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

        val cycleCount = cpu.fetch()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(4, cycleCount)
        assertEquals(0b00001000u, cpu.AF.left)
        assertEquals(0b01100000u, cpu.AF.right)
    }


    @Test
    fun `SUB A, A | zero`() {
        memory.set(0x100u, 0x97u)
        cpu.AF.left = 0b00100100u

        val cycleCount = cpu.fetch()
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

        val cycleCount = cpu.fetch()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(4, cycleCount)
        assertEquals(0b10000000u, cpu.AF.left)
        assertEquals(0b01010000u, cpu.AF.right)
    }

    @Test
    fun `SUBC A, B | no carry`() {
        memory.set(0x100u, 0x98u)
        cpu.AF.left = 0b01000011u
        cpu.BC.left = 0b01000001u

        // 01000011 - 0100001 = 00000010
        // Carry flag false
        // Half carry false

        val cycleCount = cpu.fetch()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(4, cycleCount)
        assertEquals(0b00000010u, cpu.AF.left)
        assertEquals(0b01000000u, cpu.AF.right)
    }

    @Test
    fun `SUBC A, B | carry`() {
        memory.set(0x100u, 0x98u)
        cpu.AF.left = 0b00000000u
        cpu.BC.left = 0b10000000u

        // 00000000 - 1000000 = (11111.....)10000000
        // Carry flag true
        // Half carry false

        val cycleCount = cpu.fetch()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(4, cycleCount)
        assertEquals(0b10000000u, cpu.AF.left)
        assertEquals(0b01010000u, cpu.AF.right)
    }

    @Test
    fun `SUBC A, B | half carry`() {
        memory.set(0x100u, 0x98u)
        cpu.AF.left = 0b00010000u
        cpu.BC.left = 0b00001000u

        // 00010000 - 0001000 = 00001000
        // Carry flag false
        // Half carry true

        val cycleCount = cpu.fetch()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(4, cycleCount)
        assertEquals(0b00001000u, cpu.AF.left)
        assertEquals(0b01100000u, cpu.AF.right)
    }


    @Test
    fun `SUBC A, A | zero`() {
        memory.set(0x100u, 0x9Fu)
        cpu.AF.left = 0b00100100u

        val cycleCount = cpu.fetch()
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

        val cycleCount = cpu.fetch()
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

        val cycleCount = cpu.fetch()
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

        val cycleCount = cpu.fetch()
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

        val cycleCount = cpu.fetch()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(4, cycleCount)
        assertEquals(0b00001010u, cpu.AF.left)
        assertEquals(0b00000000u, cpu.AF.right)
    }

    @Test
    fun `CP A, A | zero`() {
        memory.set(0x100u, 0xBFu)
        cpu.AF.left = 0b00100100u

        val cycleCount = cpu.fetch()
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

        val cycleCount = cpu.fetch()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(4, cycleCount)
        assertEquals(0b00100100u, cpu.AF.left)
        assertEquals(0b01110000u, cpu.AF.right)
    }

    @Test
    fun `INC A`() {
        memory.set(0x100u, 0x3Cu)
        cpu.AF.left = 0b00100100u

        val cycleCount = cpu.fetch()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(4, cycleCount)
        assertEquals(0b00100101u, cpu.AF.left)
        assertEquals(0b00000000u, cpu.AF.right)
    }

    @Test
    fun `INC A | Half carry`() {
        memory.set(0x100u, 0x3Cu)
        cpu.AF.left = 0b00101111u

        val cycleCount = cpu.fetch()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(4, cycleCount)
        assertEquals(0b00110000u, cpu.AF.left)
        assertEquals(0b00100000u, cpu.AF.right)
    }

    @Test
    fun `DEC A`() {
        memory.set(0x100u, 0x3Du)
        cpu.AF.left = 0b00100100u

        val cycleCount = cpu.fetch()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(4, cycleCount)
        assertEquals(0b00100011u, cpu.AF.left)
        assertEquals(0b00000000u, cpu.AF.right)
    }

    @Test
    fun `DEC A | Half carry`() {
        memory.set(0x100u, 0x3Du)
        cpu.AF.left = 0b00010000u

        val cycleCount = cpu.fetch()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(4, cycleCount)
        assertEquals(0b00001111u, cpu.AF.left)
        assertEquals(0b00100000u, cpu.AF.right)
    }

    @Test
    fun `ADD-16 HL, BC`() {
        memory.set(0x100u, 0x09u)
        cpu.HL.setBoth(0x5555u)
        cpu.BC.setBoth(0x2222u)

        // 0x5555 + 0x2222 = 0x7777
        // Carry flag false
        // Half carry false

        val cycleCount = cpu.fetch()
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

        // 0xF555 + 0x2222 = 0x11777
        // Carry flag true
        // Half carry false

        val cycleCount = cpu.fetch()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(8, cycleCount)
        assertEquals(0x1777u, cpu.HL.both())
        assertEquals(0b00010000u, cpu.AF.right)
    }

    @Test
    fun `ADD-16 HL, BC | Half Carry`() {
        memory.set(0x100u, 0x09u)
        cpu.HL.setBoth(0x5F55u)
        cpu.BC.setBoth(0x2222u)

        // 0x5F55 + 0x2222 = 0x8177
        // Carry flag false
        // Half carry true

        val cycleCount = cpu.fetch()
        assertEquals(0x101u, cpu.programCounter)
        assertEquals(8, cycleCount)
        assertEquals(0x8177u, cpu.HL.both())
        assertEquals(0b00100000u, cpu.AF.right)
    }

    @Test
    fun `ADD-16 SP, n`() {
        memory.set(0x100u, 0xE8u)
        memory.set(0x101u, 0x12u)
        cpu.stackPointer = 0x1234u

        // 0x1234 + 0x12 = 0x1246
        // Carry flag false
        // Half carry false

        val cycleCount = cpu.fetch()
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

        // 0x1234 + 0x1F = 0x1253
        // Carry flag false
        // Half carry true

        val cycleCount = cpu.fetch()
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

        // 0x1234 + 0xF1 = 0x1325
        // Carry flag true
        // Half carry false

        val cycleCount = cpu.fetch()
        assertEquals(0x102u, cpu.programCounter)
        assertEquals(16, cycleCount)
        assertEquals(0x1325u, cpu.stackPointer)
        assertEquals(0b00010000u, cpu.AF.right)
    }
}