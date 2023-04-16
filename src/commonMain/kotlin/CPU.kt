import memory.Memory

@ExperimentalStdlibApi
class CPU(private val memory: Memory) {
    val frequency = 4194304 // 4.194304 MHz

    var AF = AFRegister() //First bit is accumulator and second bit is the Flag register
    // Flag description: ZNHC0000
    var BC = SplitRegister()
    var DE = SplitRegister()
    var HL = SplitRegister()
    var stackPointer: UShort
    var programCounter: UShort

    var interruptMasterEnabled: Boolean = false
    var halt: Boolean = false

    var divCycleCount = 0
    var timerCycleCount = 0

    init {
        AF.left = 0x01u
        AF.right = 0xB0u
        BC.setBoth(0x0013u)
        DE.setBoth(0x00D8u)
        HL.setBoth(0x014Du)

        stackPointer = 0xFFFEu
        programCounter = 0x100u

        memory.cpu = this
    }


    fun tick(): Int {
        // wait for interruption if CPU has been halted
        if (halt) {
            handleInterrupts()
            updateTimers(4)
            return 4
        }
        val instruction = this.readOp()
        val (cycleCount, actionAfterInstruction) = decodeAndExecute(instruction)
        updateTimers(cycleCount)
        handleInterrupts()
        actionAfterInstruction?.invoke()
        return cycleCount
    }

    private fun updateTimers(cycles: Int) {
        // DIV register
        divCycleCount += cycles
        if (divCycleCount >= 256) {
            // Enough cycles has passed, the div register should be incremented
            memory.incrementDiv()
            divCycleCount -= 256
        }

        // TIMA
        // Timer control determines if the timer is paused and at which frequency it should trigger
        val timerControl = memory.get(0xFF07u)
        val timerEnable = timerControl and 0b0100u
        if (timerEnable > 0u) {
            val timerPeriod = timerControl and 0b11u
            val timerFrequencyCycles = when (timerPeriod.toUInt()) {
                0b00u -> 1024
                0b01u -> 16
                0b10u -> 64
                0b11u -> 256
                else -> throw Exception("Invalid timer frequency")
            }

            timerCycleCount += cycles
            while (timerCycleCount >= timerFrequencyCycles) {
                // Enough cycle has passed, the timer should be incremented
                val tima = memory.get(0xFF05u).toUInt()
                if (tima == 0xFFu) {
                    // tima will overflow, value should be reset to the TMA register
                    memory.set(0xFF05u, memory.get(0xFF06u))
                    // Interrupt timer is set
                    setInterrupt(2)
                } else {
                    memory.set(0xFF05u, (tima + 1u).toUByte())
                }
                timerCycleCount -= timerFrequencyCycles
            }
        }
    }

    fun resetDiv() {
        divCycleCount = 0
        timerCycleCount = 0
    }

    private fun setInterrupt(bit: Int) {
        var interrupt = memory.get(0xFF0Fu)
        interrupt = interrupt or (1u shl bit).toUByte()
        memory.set(0xFF0Fu, interrupt)
    }

    private fun handleInterrupts() {
        //TODO add wait when interrupt master or interrupt enabled isn't set yet

        // Check if VBLANK is enabled and requested
        if (shouldInterrupt(0)) {
            triggerInterrupt(0x40u, 0)
        } else if(shouldInterrupt(1)) { // LCD STAT
            triggerInterrupt(0x48u, 1)
        } else if(shouldInterrupt(2)) { // Timer
            triggerInterrupt(0x50u, 2)
        } else if(shouldInterrupt(3)) { // Serial
            triggerInterrupt(0x58u, 3)
        } else if(shouldInterrupt(4)) { // Joypad
            triggerInterrupt(0x60u, 4)
        }
    }

    private fun shouldInterrupt(bit: Int): Boolean {
        val registersAreSet = memory.get(0xFFFFu) and (1u shl bit).toUByte() > 0u.toUByte() && memory.get(0xFF0Fu) and (1u shl bit).toUByte() > 0u.toUByte()
        if (registersAreSet) halt = false // Disable any pending halt
        return interruptMasterEnabled && registersAreSet
    }

    private fun triggerInterrupt(jumpAddress: UShort, bit: Int) {
        // Disable other interrupts
        interruptMasterEnabled = false
        memory.set(0xFF0Fu, memory.get(0xFF0Fu) and (1u shl bit).toUByte().inv())

        // Store current instruction in the stackpointer
        storeShortToStack(programCounter)
        // Jump to the interruption handler
        programCounter = jumpAddress
        // Reset halt
        halt = false

        // 20 clock cycle have passed handling the different interrupt operations
        updateTimers(20)
    }

    private fun decodeAndExecute(instruction: UByte): Pair<Int, (() -> Unit)?> {
        var actionAfterInstruction: (() -> Unit)? = null
        var cycleCount = when (instruction.toUInt()) {
            // =============
            // 8-bit loads
            // ============

            //LD nn,n => Load into register the value at PC
            0x06u -> op({ BC.left = this.readOp() }, 8)
            0x0Eu -> op({ BC.right = this.readOp() }, 8)
            0x16u -> op({ DE.left = this.readOp() }, 8)
            0x1Eu -> op({ DE.right = this.readOp() }, 8)
            0x26u -> op({ HL.left = this.readOp() }, 8)
            0x2Eu -> op({ HL.right = this.readOp() }, 8)
            0x3Eu -> op({ AF.left = readOp() }, 8)

            //LD r1, r2 => Load into r1 the value in r2
            // A register
            0x7Fu -> op({ AF.left = AF.left }, 4)
            0x78u -> op({ AF.left = BC.left }, 4)
            0x79u -> op({ AF.left = BC.right }, 4)
            0x7Au -> op({ AF.left = DE.left }, 4)
            0x7Bu -> op({ AF.left = DE.right }, 4)
            0x7Cu -> op({ AF.left = HL.left }, 4)
            0x7Du -> op({ AF.left = HL.right }, 4)
            0x0Au -> op({ AF.left = memory.get(BC.both()) }, 8)
            0x1Au -> op({ AF.left = memory.get(DE.both()) }, 8)
            0x7Eu -> op({ AF.left = memory.get(HL.both()) }, 8)
            0xFAu -> op({ AF.left = memory.get(readNN()) }, 16)
            // B register
            0x40u -> op({ BC.left = BC.left }, 4)
            0x41u -> op({ BC.left = BC.right }, 4)
            0x42u -> op({ BC.left = DE.left }, 4)
            0x43u -> op({ BC.left = DE.right }, 4)
            0x44u -> op({ BC.left = HL.left }, 4)
            0x45u -> op({ BC.left = HL.right }, 4)
            0x46u -> op({ BC.left = memory.get(HL.both()) }, 8)
            0x47u -> op({ BC.left = AF.left }, 4)
            // C register
            0x48u -> op({ BC.right = BC.left }, 4)
            0x49u -> op({ BC.right = BC.right }, 4)
            0x4Au -> op({ BC.right = DE.left }, 4)
            0x4Bu -> op({ BC.right = DE.right }, 4)
            0x4Cu -> op({ BC.right = HL.left }, 4)
            0x4Du -> op({ BC.right = HL.right }, 4)
            0x4Eu -> op({ BC.right = memory.get(HL.both()) }, 8)
            0x4Fu -> op({ BC.right = AF.left }, 4)
            // D register
            0x50u -> op({ DE.left = BC.left }, 4)
            0x51u -> op({ DE.left = BC.right }, 4)
            0x52u -> op({ DE.left = DE.left }, 4)
            0x53u -> op({ DE.left = DE.right }, 4)
            0x54u -> op({ DE.left = HL.left }, 4)
            0x55u -> op({ DE.left = HL.right }, 4)
            0x56u -> op({ DE.left = memory.get(HL.both()) }, 8)
            0x57u -> op({ DE.left = AF.left }, 4)
            // E register
            0x58u -> op({ DE.right = BC.left }, 4)
            0x59u -> op({ DE.right = BC.right }, 4)
            0x5Au -> op({ DE.right = DE.left }, 4)
            0x5Bu -> op({ DE.right = DE.right }, 4)
            0x5Cu -> op({ DE.right = HL.left }, 4)
            0x5Du -> op({ DE.right = HL.right }, 4)
            0x5Eu -> op({ DE.right = memory.get(HL.both()) }, 8)
            0x5Fu -> op({ DE.right = AF.left }, 4)
            // H register
            0x60u -> op({ HL.left = BC.left }, 4)
            0x61u -> op({ HL.left = BC.right }, 4)
            0x62u -> op({ HL.left = DE.left }, 4)
            0x63u -> op({ HL.left = DE.right }, 4)
            0x64u -> op({ HL.left = HL.left }, 4)
            0x65u -> op({ HL.left = HL.right }, 4)
            0x66u -> op({ HL.left = memory.get(HL.both()) }, 8)
            0x67u -> op({ HL.left = AF.left }, 4)
            // L register
            0x68u -> op({ HL.right = BC.left }, 4)
            0x69u -> op({ HL.right = BC.right }, 4)
            0x6Au -> op({ HL.right = DE.left }, 4)
            0x6Bu -> op({ HL.right = DE.right }, 4)
            0x6Cu -> op({ HL.right = HL.left }, 4)
            0x6Du -> op({ HL.right = HL.right }, 4)
            0x6Eu -> op({ HL.right = memory.get(HL.both()) }, 8)
            0x6Fu -> op({ HL.right = AF.left }, 4)
            // (HL) memory address
            0x70u -> op({ memory.set(HL.both(), BC.left) }, 8)
            0x71u -> op({ memory.set(HL.both(), BC.right) }, 8)
            0x72u -> op({ memory.set(HL.both(), DE.left) }, 8)
            0x73u -> op({ memory.set(HL.both(), DE.right) }, 8)
            0x74u -> op({ memory.set(HL.both(), HL.left) }, 8)
            0x75u -> op({ memory.set(HL.both(), HL.right) }, 8)
            // (HL) loads next value
            0x36u -> op({ memory.set(HL.both(), this.readOp()) }, 12)

            // LD n A => Load value of A in address n
            0x02u -> op({ memory.set(BC.both(), AF.left) }, 8)
            0x12u -> op({ memory.set(DE.both(), AF.left) }, 8)
            0x77u -> op({ memory.set(HL.both(), AF.left) }, 8)
            0xEAu -> op({ memory.set(readNN(), AF.left) }, 16)

            // LD A,($FF00+C)
            0xF2u -> op({ AF.left = memory.get(0xFF00u.plus(BC.right).toUShort()) }, 8)
            // LD ($FF00+C), A
            0xE2u -> op({ memory.set(0xFF00u.plus(BC.right).toUShort(), AF.left) }, 8)
            // LD A,(HL) - DEC H
            0x3Au -> op({
                AF.left = memory.get(HL.both())
                HL.decrement()
            }, 8)
            // LD (HL),A - DEC H
            0x32u -> op({
                memory.set(HL.both(), AF.left)
                HL.decrement()
            }, 8)
            // LD A,(HL) - INC HL
            0x2Au -> op({
                AF.left = memory.get(HL.both())
                HL.increment()
            }, 8)
            // LD (HL),A - INC HL
            0x22u -> op({
                memory.set(HL.both(), AF.left)
                HL.increment()
            }, 8)
            //  LDH (n),A
            0xE0u -> op({ memory.set(0xFF00u.plus(readOp()).toUShort(), AF.left) }, 12)
            // LDH A,(n)
            0xF0u -> op({ AF.left = memory.get(0xFF00u.plus(readOp()).toUShort()) }, 12)

            // =============
            // 16-bit loads
            // ============

            // LD n,nn
            0x01u -> op({ BC.setBoth(readNN()) }, 12)
            0x11u -> op({ DE.setBoth(readNN()) }, 12)
            0x21u -> op({ HL.setBoth(readNN()) }, 12)
            0x31u -> op({ stackPointer = readNN() }, 12)

            // LD SP,HL
            0xF9u -> op({ stackPointer = HL.both() }, 8)
            // LD HL SP,n
            0xF8u -> op({
                val number = readOp().toByte() // Next byte is signed
                val result = stackPointer.toInt() + number
                HL.setBoth((result).toUShort())

                AF.setZeroFlag(false)
                AF.setSubtractFlag(false)
                AF.setHalfCarryFlag((stackPointer.toInt() and 0x000F) + (number.toInt() and 0x000F) > 0X000F)
                AF.setCarryFlag((stackPointer.toInt() and 0x00FF) + (number.toInt() and 0x00FF) > 0X00FF)
            }, 12)
            // LD(nn), SP
            0x08u -> op({
                val address = readNN()
                memory.set(address, stackPointer.getRight())
                memory.set(address.inc(), stackPointer.getLeft())
            }, 20)
            // PUSH AF
            0xF5u -> op({
                memory.set(--stackPointer, AF.left)
                memory.set(--stackPointer, AF.right)
            }, 16)
            // PUSH BC
            0xC5u -> op({
                memory.set(--stackPointer, BC.left)
                memory.set(--stackPointer, BC.right)
            }, 16)
            // PUSH DE
            0xD5u -> op({
                memory.set(--stackPointer, DE.left)
                memory.set(--stackPointer, DE.right)
            }, 16)
            // PUSH HL
            0xE5u -> op({
                memory.set(--stackPointer, HL.left)
                memory.set(--stackPointer, HL.right)
            }, 16)
            // POP AF
            0xF1u -> op({
                AF.right = memory.get(stackPointer++)
                AF.left = memory.get(stackPointer++)
            }, 12)
            // POP BC
            0xC1u -> op({
                BC.right = memory.get(stackPointer++)
                BC.left = memory.get(stackPointer++)
            }, 12)
            // POP DE
            0xD1u -> op({
                DE.right = memory.get(stackPointer++)
                DE.left = memory.get(stackPointer++)
            }, 12)
            // POP HL
            0xE1u -> op({
                HL.right = memory.get(stackPointer++)
                HL.left = memory.get(stackPointer++)
            }, 12)

            // 8 bit ALU
            // ADD A, A
            0x87u -> op({ addOp(AF.left) }, 4)
            // ADD A, B
            0x80u -> op({ addOp(BC.left) }, 4)
            // ADD A, C
            0x81u -> op({ addOp(BC.right) }, 4)
            // ADD A, D
            0x82u -> op({ addOp(DE.left) }, 4)
            // ADD A, E
            0x83u -> op({ addOp(DE.right) }, 4)
            // ADD A, H
            0x84u -> op({ addOp(HL.left) }, 4)
            // ADD A, L
            0x85u -> op({ addOp(HL.right) }, 4)
            // ADD A, (HL)
            0x86u -> op({ addOp(memory.get(HL.both())) }, 8)
            // ADD A, n
            0xC6u -> op({ addOp(readOp()) }, 8)

            // ADC A, A
            0x8Fu -> op({ addCarryOp(AF.left) }, 4)
            // ADC A, B
            0x88u -> op({ addCarryOp(BC.left) }, 4)
            // ADC A, C
            0x89u -> op({ addCarryOp(BC.right) }, 4)
            // ADC A, D
            0x8Au -> op({ addCarryOp(DE.left) }, 4)
            // ADC A, E
            0x8Bu -> op({ addCarryOp(DE.right) }, 4)
            // ADC A, H
            0x8Cu -> op({ addCarryOp(HL.left) }, 4)
            // ADC A, L
            0x8Du -> op({ addCarryOp(HL.right) }, 4)
            // ADC A, (HL)
            0x8Eu -> op({ addCarryOp(memory.get(HL.both())) }, 8)
            // ADC A, n
            0xCEu -> op({ addCarryOp(readOp()) }, 8)

            // SUB A, A
            0x97u -> op({ subOp(AF.left) }, 4)
            // SUB A, B
            0x90u -> op({ subOp(BC.left) }, 4)
            // SUB A, C
            0x91u -> op({ subOp(BC.right) }, 4)
            // SUB A, D
            0x92u -> op({ subOp(DE.left) }, 4)
            // SUB A, E
            0x93u -> op({ subOp(DE.right) }, 4)
            // SUB A, H
            0x94u -> op({ subOp(HL.left) }, 4)
            // SUB A, L
            0x95u -> op({ subOp(HL.right) }, 4)
            // SUB A, (HLsub
            0x96u -> op({ subOp(memory.get(HL.both())) }, 8)
            // SUB A, n
            0xD6u -> op({ subOp(readOp()) }, 8)

            // SUBC A, A
            0x9Fu -> op({ subCarryOp(AF.left) }, 4)
            // SUBC A, B
            0x98u -> op({ subCarryOp(BC.left) }, 4)
            // SUBC A, C
            0x99u -> op({ subCarryOp(BC.right) }, 4)
            // SUBC A, D
            0x9Au -> op({ subCarryOp(DE.left) }, 4)
            // SUBC A, E
            0x9Bu -> op({ subCarryOp(DE.right) }, 4)
            // SUBC A, H
            0x9Cu -> op({ subCarryOp(HL.left) }, 4)
            // SUBC A, L
            0x9Du -> op({ subCarryOp(HL.right) }, 4)
            // SUBC A, (HL)
            0x9Eu -> op({ subCarryOp(memory.get(HL.both())) }, 8)
            // SUBC A, n
            0xDEu -> op({ subCarryOp(readOp()) }, 8)

            // AND A, A
            0xA7u -> op({ andOp(AF.left) }, 4)
            // AND A, B
            0xA0u -> op({ andOp(BC.left) }, 4)
            // AND A, C
            0xA1u -> op({ andOp(BC.right) }, 4)
            // AND A, D
            0xA2u -> op({ andOp(DE.left) }, 4)
            // AND A, E
            0xA3u -> op({ andOp(DE.right) }, 4)
            // AND A, H
            0xA4u -> op({ andOp(HL.left) }, 4)
            // AND A, L
            0xA5u -> op({ andOp(HL.right) }, 4)
            // AND A, (HL)
            0xA6u -> op({ andOp(memory.get(HL.both())) }, 8)
            // AND A, n
            0xE6u -> op({ andOp(readOp()) }, 8)

            // OR A, A
            0xB7u -> op({ orOp(AF.left) }, 4)
            // OR A, B
            0xB0u -> op({ orOp(BC.left) }, 4)
            // OR A, C
            0xB1u -> op({ orOp(BC.right) }, 4)
            // OR A, D
            0xB2u -> op({ orOp(DE.left) }, 4)
            // OR A, E
            0xB3u -> op({ orOp(DE.right) }, 4)
            // OR A, H
            0xB4u -> op({ orOp(HL.left) }, 4)
            // OR A, L
            0xB5u -> op({ orOp(HL.right) }, 4)
            // OR A, (HL)
            0xB6u -> op({ orOp(memory.get(HL.both())) }, 8)
            // OR A, n
            0xF6u -> op({ orOp(readOp()) }, 8)

            // XOR A, A
            0xAFu -> op({ xorOp(AF.left) }, 4)
            // XOR A, B
            0xA8u -> op({ xorOp(BC.left) }, 4)
            // XOR A, C
            0xA9u -> op({ xorOp(BC.right) }, 4)
            // XOR A, D
            0xAAu -> op({ xorOp(DE.left) }, 4)
            // XOR A, E
            0xABu -> op({ xorOp(DE.right) }, 4)
            // XOR A, H
            0xACu -> op({ xorOp(HL.left) }, 4)
            // XOR A, L
            0xADu -> op({ xorOp(HL.right) }, 4)
            // XOR A, (HL)
            0xAEu -> op({ xorOp(memory.get(HL.both())) }, 8)
            // XOR A, n
            0xEEu -> op({ xorOp(readOp()) }, 8)

            // CP A, A
            0xBFu -> op({ cpOp(AF.left) }, 4)
            // CP A, B
            0xB8u -> op({ cpOp(BC.left) }, 4)
            // CP A, C
            0xB9u -> op({ cpOp(BC.right) }, 4)
            // CP A, D
            0xBAu -> op({ cpOp(DE.left) }, 4)
            // CP A, E
            0xBBu -> op({ cpOp(DE.right) }, 4)
            // CP A, H
            0xBCu -> op({ cpOp(HL.left) }, 4)
            // CP A, L
            0xBDu -> op({ cpOp(HL.right) }, 4)
            // CP A, (HL)
            0xBEu -> op({ cpOp(memory.get(HL.both())) }, 8)
            // CP A, n
            0xFEu -> op({ cpOp(readOp()) }, 8)

            // INC A
            0x3Cu -> op({ incOp(AF, true) }, 4)
            // INC B
            0x04u -> op({ incOp(BC, true) }, 4)
            // INC C
            0x0Cu -> op({ incOp(BC, false) }, 4)
            // INC D
            0x14u -> op({ incOp(DE, true) }, 4)
            // INC E
            0x1Cu -> op({ incOp(DE, false) }, 4)
            // INC H
            0x24u -> op({ incOp(HL, true) }, 4)
            // INC L
            0x2Cu -> op({ incOp(HL, false) }, 4)
            // INC (HL)
            0x34u -> op({
                val result = (memory.get(HL.both()) + 1u).toUByte()
                memory.set(HL.both(), result)
                AF.setZeroFlag(result.toUInt() == 0u)
                AF.setNFlag(false)
                AF.setHalfCarryFlag((result and 0x0Fu).toUInt() == 0x00u)
            }, 12)

            // DEC A
            0x3Du -> op({ decOp(AF, true) }, 4)
            // DEC B
            0x05u -> op({ decOp(BC, true) }, 4)
            // DEC C
            0x0Du -> op({ decOp(BC, false) }, 4)
            // DEC D
            0x15u -> op({ decOp(DE, true) }, 4)
            // DEC E
            0x1Du -> op({ decOp(DE, false) }, 4)
            // DEC H
            0x25u -> op({ decOp(HL, true) }, 4)
            // DEC L
            0x2Du -> op({ decOp(HL, false) }, 4)
            // DEC (HL)
            0x35u -> op({
                val result = (memory.get(HL.both()) - 1u).toUByte()
                memory.set(HL.both(), result)
                AF.setZeroFlag(result.toUInt() == 0u)
                AF.setNFlag(true)
                AF.setHalfCarryFlag((result and 0x0Fu).toUInt() == 0x0Fu)
            }, 12)

            // 16 bit arithmetic
            // ADD HL, BC
            0x09u -> op({ add16Op(BC.both()) }, 8)
            // ADD HL, DE
            0x19u -> op({ add16Op(DE.both()) }, 8)
            // ADD HL, HL
            0x29u -> op({ add16Op(HL.both()) }, 8)
            // ADD HL, SP
            0x39u -> op({ add16Op(stackPointer) }, 8)

            // ADD SP, n
            0xE8u -> op({
                val number = readOp().toByte() // Number is signed
                val result = number + stackPointer.toShort()
                AF.setNFlag(false)
                AF.setZeroFlag(false)
                AF.setHalfCarryFlag((stackPointer.toInt() and 0x000F) + (number.toInt() and 0x000F) > 0X000F)
                AF.setCarryFlag((stackPointer.toInt() and 0x00FF) + (number.toInt() and 0x00FF) > 0X00FF)
                stackPointer = result.toUShort()
            }, 16)

            // INC BC
            0x03u -> op({ BC.increment() }, 8)
            // INC DE
            0x13u -> op({ DE.increment() }, 8)
            // INC HL
            0x23u -> op({ HL.increment() }, 8)
            // INC SP
            0x33u -> op({ stackPointer++ }, 8)

            // DEC BC
            0x0Bu -> op({ BC.decrement() }, 8)
            // DEC DE
            0x1Bu -> op({ DE.decrement() }, 8)
            // DEC HL
            0x2Bu -> op({ HL.decrement() }, 8)
            // DEC SP
            0x3Bu -> op({ stackPointer-- }, 8)


            // Miscellaneous
            0xCBu -> {
                when (readOp().toUInt()) {
                    // SWAP A
                    0x37u -> op({ swapOp(AF, true) }, 8)
                    // SWAP B
                    0x30u -> op({ swapOp(BC, true) }, 8)
                    // SWAP C
                    0x31u -> op({ swapOp(BC, false) }, 8)
                    // SWAP D
                    0x32u -> op({ swapOp(DE, true) }, 8)
                    // SWAP E
                    0x33u -> op({ swapOp(DE, false) }, 8)
                    // SWAP H
                    0x34u -> op({ swapOp(HL, true) }, 8)
                    // SWAP L
                    0x35u -> op({ swapOp(HL, false) }, 8)
                    // SWAP (HL)
                    0x36u -> op({
                        AF.resetFlags()
                        val number = memory.get(HL.both())
                        val result = ((number.toUInt() shr 4) + ((number and 0xFu).toUInt() shl 4)).toUByte()
                        memory.set(HL.both(), result)
                        AF.setZeroFlag(result.toUInt() == 0x0u)
                    }, 16)

                    //RLC A
                    0x07u -> op({ RLC(AF, true) }, 8)
                    //RLC B
                    0x00u -> op({ RLC(BC, true) }, 8)
                    //RLC C
                    0x01u -> op({ RLC(BC, false) }, 8)
                    //RLC D
                    0x02u -> op({ RLC(DE, true) }, 8)
                    //RLC E
                    0x03u -> op({ RLC(DE, false) }, 8)
                    //RLC H
                    0x04u -> op({ RLC(HL, true) }, 8)
                    //RLC L
                    0x05u -> op({ RLC(HL, false) }, 8)
                    //RLC (HL)
                    0x06u -> op({
                        val result = memory.get(HL.both()).rotateLeft(1)
                        memory.set(HL.both(), result)

                        AF.setZeroFlag(result.toUInt() == 0u)
                        AF.setNFlag(false)
                        AF.setHalfCarryFlag(false)
                        AF.setCarryFlag(result.toUInt() and 1u == 1u)
                    }, 16)

                    //RL A
                    0x17u -> op({ RL(AF, true) }, 8)
                    //RL B
                    0x10u -> op({ RL(BC, true) }, 8)
                    //RL C
                    0x11u -> op({ RL(BC, false) }, 8)
                    //RL D
                    0x12u -> op({ RL(DE, true) }, 8)
                    //RL E
                    0x13u -> op({ RL(DE, false) }, 8)
                    //RL H
                    0x14u -> op({ RL(HL, true) }, 8)
                    //RL L
                    0x15u -> op({ RL(HL, false) }, 8)
                    //RL (HL)
                    0x16u -> op({
                        val value = memory.get(HL.both())
                        val newCarry = value.toUInt() and 0b1000_0000u
                        val oldCarry = if (AF.getCarryFlag()) 1u else 0u

                        val result = ((value.toUInt() shl 1) + oldCarry).toUByte()
                        memory.set(HL.both(), result)
                        AF.setZeroFlag(result.toUInt() == 0u)
                        AF.setNFlag(false)
                        AF.setHalfCarryFlag(false)
                        AF.setCarryFlag(newCarry != 0u)
                    }, 16)

                    //RRC A
                    0x0Fu -> op({ RRC(AF, true) }, 8)
                    //RRC B
                    0x08u -> op({ RRC(BC, true) }, 8)
                    //RRC C
                    0x09u -> op({ RRC(BC, false) }, 8)
                    //RRC D
                    0x0Au -> op({ RRC(DE, true) }, 8)
                    //RRC E
                    0x0Bu -> op({ RRC(DE, false) }, 8)
                    //RRC H
                    0x0Cu -> op({ RRC(HL, true) }, 8)
                    //RRC L
                    0x0Du -> op({ RRC(HL, false) }, 8)
                    //RRC (HL)
                    0x0Eu -> op({
                        val result = memory.get(HL.both()).rotateRight(1)
                        AF.setZeroFlag(result.toUInt() == 0u)
                        AF.setNFlag(false)
                        AF.setHalfCarryFlag(false)
                        AF.setCarryFlag(result.toUInt() and 0b1000_0000u > 0u)
                        memory.set(HL.both(), result)
                    }, 16)

                    //RR A
                    0x1Fu -> op({ RR(AF, true) }, 8)
                    //RR B
                    0x18u -> op({ RR(BC, true) }, 8)
                    //RR C
                    0x19u -> op({ RR(BC, false) }, 8)
                    //RR D
                    0x1Au -> op({ RR(DE, true) }, 8)
                    //RR E
                    0x1Bu -> op({ RR(DE, false) }, 8)
                    //RR H
                    0x1Cu -> op({ RR(HL, true) }, 8)
                    //RR L
                    0x1Du -> op({ RR(HL, false) }, 8)
                    //RR (HL)
                    0x1Eu -> op({
                        val value = memory.get(HL.both())

                        val newCarry = value.toUInt() and 0b0000_0001u
                        val oldCarry = if (AF.getCarryFlag()) 0b1000_0000u else 0u
                        val result = ((value.toUInt() shr 1) + oldCarry).toUByte()
                        memory.set(HL.both(), result)

                        AF.setZeroFlag(result.toUInt() == 0u)
                        AF.setNFlag(false)
                        AF.setHalfCarryFlag(false)
                        AF.setCarryFlag(newCarry == 1u)
                    }, 16)

                    //SLA A
                    0x27u -> op({ SLA(AF, true) }, 8)
                    //SLA B
                    0x20u -> op({ SLA(BC, true) }, 8)
                    //SLA C
                    0x21u -> op({ SLA(BC, false) }, 8)
                    //SLA D
                    0x22u -> op({ SLA(DE, true) }, 8)
                    //SLA E
                    0x23u -> op({ SLA(DE, false) }, 8)
                    //SLA H
                    0x24u -> op({ SLA(HL, true) }, 8)
                    //SLA L
                    0x25u -> op({ SLA(HL, false) }, 8)
                    //SLA (HL)
                    0x26u -> op({
                        val value = memory.get(HL.both())

                        // Storing most significant bit to be stored in the carry flag
                        val mostSignificant = value and 0b1000_0000u
                        val result = (value.toUInt() shl 1).toUByte()
                        memory.set(HL.both(), result)
                        AF.setZeroFlag(result.toUInt() == 0u)
                        AF.setNFlag(false)
                        AF.setHalfCarryFlag(false)
                        AF.setCarryFlag(mostSignificant >= 1u)
                    }, 16)


                    // SRA A
                    0x2Fu -> op({ SRA(AF, true) }, 8)
                    // SRA B
                    0x28u -> op({ SRA(BC, true) }, 8)
                    // SRA C
                    0x29u -> op({ SRA(BC, false) }, 8)
                    // SRA D
                    0x2Au -> op({ SRA(DE, true) }, 8)
                    // SRA E
                    0x2Bu -> op({ SRA(DE, false) }, 8)
                    // SRA H
                    0x2Cu -> op({ SRA(HL, true) }, 8)
                    // SRA L
                    0x2Du -> op({ SRA(HL, false) }, 8)
                    // SRA (HL)
                    0x2Eu -> op({
                        val value = memory.get(HL.both())

                        // Arithmetic shift, the most significant bit (the sign bit) is kept when shifting
                        // Kotlin does it by itself when using signed int but converting to and from unsigned byte breaks this
                        val mostSignificant = value and 0b1000_0000u
                        val result = (value.toUInt() shr 1 or mostSignificant.toUInt()).toUByte()
                        // Storing previous bit 0 to save it in the carry
                        val leastSignificant = value and 0b0000_0001u
                        memory.set(HL.both(), result)
                        AF.setZeroFlag(result.toUInt() == 0u)
                        AF.setNFlag(false)
                        AF.setHalfCarryFlag(false)
                        AF.setCarryFlag(leastSignificant.toUInt() == 1u)
                    }, 16)

                    // SRL A
                    0x3Fu -> op({ SRL(AF, true) }, 8)
                    // SRL B
                    0x38u -> op({ SRL(BC, true) }, 8)
                    // SRL C
                    0x39u -> op({ SRL(BC, false) }, 8)
                    // SRL D
                    0x3Au -> op({ SRL(DE, true) }, 8)
                    // SRL E
                    0x3Bu -> op({ SRL(DE, false) }, 8)
                    // SRL H
                    0x3Cu -> op({ SRL(HL, true) }, 8)
                    // SRL L
                    0x3Du -> op({ SRL(HL, false) }, 8)
                    // SRL (HL)
                    0x3Eu -> op({
                        val value = memory.get(HL.both())

                        // Logical shift, the most significant bit is always set to 0
                        val result = (value.toUInt() shr 1).toUByte()
                        // Storing previous bit 0 to save it in the carry
                        val leastSignificant = value and 0b0000_0001u
                        memory.set(HL.both(), result)
                        AF.setZeroFlag(result.toUInt() == 0u)
                        AF.setNFlag(false)
                        AF.setHalfCarryFlag(false)
                        AF.setCarryFlag(leastSignificant.toUInt() == 1u)
                    }, 16)

                    // BIT 0 B
                    0x40u -> op({ bit(BC, true, 0) }, 8)
                    // BIT 0 C
                    0x41u -> op({ bit(BC, false, 0) }, 8)
                    // BIT 0 D
                    0x42u -> op({ bit(DE, true, 0) }, 8)
                    // BIT 0 E
                    0x43u -> op({ bit(DE, false, 0) }, 8)
                    // BIT 0 H
                    0x44u -> op({ bit(HL, true, 0) }, 8)
                    // BIT 0 L
                    0x45u -> op({ bit(HL, false, 0) }, 8)
                    // BIT 0 (HL)
                    0x46u -> op({ bitHL(0) }, 12)
                    // BIT 0 A
                    0x47u -> op({ bit(AF, true, 0) }, 8)

                    // BIT 1 B
                    0x48u -> op({ bit(BC, true, 1) }, 8)
                    // BIT 1 C
                    0x49u -> op({ bit(BC, false, 1) }, 8)
                    // BIT 1 D
                    0x4Au -> op({ bit(DE, true, 1) }, 8)
                    // BIT 1 E
                    0x4Bu -> op({ bit(DE, false, 1) }, 8)
                    // BIT 1 H
                    0x4Cu -> op({ bit(HL, true, 1) }, 8)
                    // BIT 1 L
                    0x4Du -> op({ bit(HL, false, 1) }, 8)
                    // BIT 1 (HL)
                    0x4Eu -> op({ bitHL(1) }, 12)
                    // BIT 1 A
                    0x4Fu -> op({ bit(AF, true, 1) }, 8)

                    // BIT 2 B
                    0x50u -> op({ bit(BC, true, 2) }, 8)
                    // BIT 2 C
                    0x51u -> op({ bit(BC, false, 2) }, 8)
                    // BIT 2 D
                    0x52u -> op({ bit(DE, true, 2) }, 8)
                    // BIT 2 E
                    0x53u -> op({ bit(DE, false, 2) }, 8)
                    // BIT 2 H
                    0x54u -> op({ bit(HL, true, 2) }, 8)
                    // BIT 2 L
                    0x55u -> op({ bit(HL, false, 2) }, 8)
                    // BIT 2 (HL)
                    0x56u -> op({ bitHL(2) }, 12)
                    // BIT 2 A
                    0x57u -> op({ bit(AF, true, 2) }, 8)

                    // BIT 3 B
                    0x58u -> op({ bit(BC, true, 3) }, 8)
                    // BIT 3 C
                    0x59u -> op({ bit(BC, false, 3) }, 8)
                    // BIT 3 D
                    0x5Au -> op({ bit(DE, true, 3) }, 8)
                    // BIT 3 E
                    0x5Bu -> op({ bit(DE, false, 3) }, 8)
                    // BIT 3 H
                    0x5Cu -> op({ bit(HL, true, 3) }, 8)
                    // BIT 3 L
                    0x5Du -> op({ bit(HL, false, 3) }, 8)
                    // BIT 3 (HL)
                    0x5Eu -> op({ bitHL(3) }, 12)
                    // BIT 3 A
                    0x5Fu -> op({ bit(AF, true, 3) }, 8)

                    // BIT 4 B
                    0x60u -> op({ bit(BC, true, 4) }, 8)
                    // BIT 4 C
                    0x61u -> op({ bit(BC, false, 4) }, 8)
                    // BIT 4 D
                    0x62u -> op({ bit(DE, true, 4) }, 8)
                    // BIT 4 E
                    0x63u -> op({ bit(DE, false, 4) }, 8)
                    // BIT 4 H
                    0x64u -> op({ bit(HL, true, 4) }, 8)
                    // BIT 4 L
                    0x65u -> op({ bit(HL, false, 4) }, 8)
                    // BIT 4 (HL)
                    0x66u -> op({ bitHL(4) }, 12)
                    // BIT 4 A
                    0x67u -> op({ bit(AF, true, 4) }, 8)

                    // BIT 5 B
                    0x68u -> op({ bit(BC, true, 5) }, 8)
                    // BIT 5 C
                    0x69u -> op({ bit(BC, false, 5) }, 8)
                    // BIT 5 D
                    0x6Au -> op({ bit(DE, true, 5) }, 8)
                    // BIT 5 E
                    0x6Bu -> op({ bit(DE, false, 5) }, 8)
                    // BIT 5 H
                    0x6Cu -> op({ bit(HL, true, 5) }, 8)
                    // BIT 5 L
                    0x6Du -> op({ bit(HL, false, 5) }, 8)
                    // BIT 5 (HL)
                    0x6Eu -> op({ bitHL(5) }, 12)
                    // BIT 5 A
                    0x6Fu -> op({ bit(AF, true, 5) }, 8)

                    // BIT 6 B
                    0x70u -> op({ bit(BC, true, 6) }, 8)
                    // BIT 6 C
                    0x71u -> op({ bit(BC, false, 6) }, 8)
                    // BIT 6 D
                    0x72u -> op({ bit(DE, true, 6) }, 8)
                    // BIT 6 E
                    0x73u -> op({ bit(DE, false, 6) }, 8)
                    // BIT 6 H
                    0x74u -> op({ bit(HL, true, 6) }, 8)
                    // BIT 6 L
                    0x75u -> op({ bit(HL, false, 6) }, 8)
                    // BIT 6 (HL)
                    0x76u -> op({ bitHL(6) }, 12)
                    // BIT 6 A
                    0x77u -> op({ bit(AF, true, 6) }, 8)

                    // BIT 7 B
                    0x78u -> op({ bit(BC, true, 7) }, 8)
                    // BIT 7 C
                    0x79u -> op({ bit(BC, false, 7) }, 8)
                    // BIT 7 D
                    0x7Au -> op({ bit(DE, true, 7) }, 8)
                    // BIT 7 E
                    0x7Bu -> op({ bit(DE, false, 7) }, 8)
                    // BIT 7 H
                    0x7Cu -> op({ bit(HL, true, 7) }, 8)
                    // BIT 7 L
                    0x7Du -> op({ bit(HL, false, 7) }, 8)
                    // BIT 7 (HL)
                    0x7Eu -> op({ bitHL(7) }, 12)
                    // BIT 7 A
                    0x7Fu -> op({ bit(AF, true, 7) }, 8)

                    // SET 0 B
                    0xC0u -> op({ set(BC, true, 0) }, 8)
                    // SET 0 C
                    0xC1u -> op({ set(BC, false, 0) }, 8)
                    // SET 0 D
                    0xC2u -> op({ set(DE, true, 0) }, 8)
                    // SET 0 E
                    0xC3u -> op({ set(DE, false, 0) }, 8)
                    // SET 0 H
                    0xC4u -> op({ set(HL, true, 0) }, 8)
                    // SET 0 L
                    0xC5u -> op({ set(HL, false, 0) }, 8)
                    // SET 0 (HL)
                    0xC6u -> op({ setHL(0) }, 16)
                    // SET 0 A
                    0xC7u -> op({ set(AF, true, 0) }, 8)

                    // SET 1 B
                    0xC8u -> op({ set(BC, true, 1) }, 8)
                    // SET 1 C
                    0xC9u -> op({ set(BC, false, 1) }, 8)
                    // SET 1 D
                    0xCAu -> op({ set(DE, true, 1) }, 8)
                    // SET 1 E
                    0xCBu -> op({ set(DE, false, 1) }, 8)
                    // SET 1 H
                    0xCCu -> op({ set(HL, true, 1) }, 8)
                    // SET 1 L
                    0xCDu -> op({ set(HL, false, 1) }, 8)
                    // SET 1 (HL)
                    0xCEu -> op({ setHL(1) }, 16)
                    // SET 1 A
                    0xCFu -> op({ set(AF, true, 1) }, 8)

                    // SET 2 B
                    0xD0u -> op({ set(BC, true, 2) }, 8)
                    // SET 2 C
                    0xD1u -> op({ set(BC, false, 2) }, 8)
                    // SET 2 D
                    0xD2u -> op({ set(DE, true, 2) }, 8)
                    // SET 2 E
                    0xD3u -> op({ set(DE, false, 2) }, 8)
                    // SET 2 H
                    0xD4u -> op({ set(HL, true, 2) }, 8)
                    // SET 2 L
                    0xD5u -> op({ set(HL, false, 2) }, 8)
                    // SET 2 (HL)
                    0xD6u -> op({ setHL(2) }, 16)
                    // SET 2 A
                    0xD7u -> op({ set(AF, true, 2) }, 8)

                    // SET 3 B
                    0xD8u -> op({ set(BC, true, 3) }, 8)
                    // SET 3 C
                    0xD9u -> op({ set(BC, false, 3) }, 8)
                    // SET 3 D
                    0xDAu -> op({ set(DE, true, 3) }, 8)
                    // SET 3 E
                    0xDBu -> op({ set(DE, false, 3) }, 8)
                    // SET 3 H
                    0xDCu -> op({ set(HL, true, 3) }, 8)
                    // SET 3 L
                    0xDDu -> op({ set(HL, false, 3) }, 8)
                    // SET 3 (HL)
                    0xDEu -> op({ setHL(3) }, 16)
                    // SET 3 A
                    0xDFu -> op({ set(AF, true, 3) }, 8)

                    // SET 4 B
                    0xE0u -> op({ set(BC, true, 4) }, 8)
                    // SET 4 C
                    0xE1u -> op({ set(BC, false, 4) }, 8)
                    // SET 4 D
                    0xE2u -> op({ set(DE, true, 4) }, 8)
                    // SET 4 E
                    0xE3u -> op({ set(DE, false, 4) }, 8)
                    // SET 4 H
                    0xE4u -> op({ set(HL, true, 4) }, 8)
                    // SET 4 L
                    0xE5u -> op({ set(HL, false, 4) }, 8)
                    // SET 4 (HL)
                    0xE6u -> op({ setHL(4) }, 16)
                    // SET 4 A
                    0xE7u -> op({ set(AF, true, 4) }, 8)

                    // SET 5 B
                    0xE8u -> op({ set(BC, true, 5) }, 8)
                    // SET 5 C
                    0xE9u -> op({ set(BC, false, 5) }, 8)
                    // SET 5 D
                    0xEAu -> op({ set(DE, true, 5) }, 8)
                    // SET 5 E
                    0xEBu -> op({ set(DE, false, 5) }, 8)
                    // SET 5 H
                    0xECu -> op({ set(HL, true, 5) }, 8)
                    // SET 5 L
                    0xEDu -> op({ set(HL, false, 5) }, 8)
                    // SET 5 (HL)
                    0xEEu -> op({ setHL(5) }, 16)
                    // SET 5 A
                    0xEFu -> op({ set(AF, true, 5) }, 8)

                    // SET 6 B
                    0xF0u -> op({ set(BC, true, 6) }, 8)
                    // SET 6 C
                    0xF1u -> op({ set(BC, false, 6) }, 8)
                    // SET 6 D
                    0xF2u -> op({ set(DE, true, 6) }, 8)
                    // SET 6 E
                    0xF3u -> op({ set(DE, false, 6) }, 8)
                    // SET 6 H
                    0xF4u -> op({ set(HL, true, 6) }, 8)
                    // SET 6 L
                    0xF5u -> op({ set(HL, false, 6) }, 8)
                    // SET 6 (HL)
                    0xF6u -> op({ setHL(6) }, 16)
                    // SET 6 A
                    0xF7u -> op({ set(AF, true, 6) }, 8)

                    // SET 7 B
                    0xF8u -> op({ set(BC, true, 7) }, 8)
                    // SET 7 C
                    0xF9u -> op({ set(BC, false, 7) }, 8)
                    // SET 7 D
                    0xFAu -> op({ set(DE, true, 7) }, 8)
                    // SET 7 E
                    0xFBu -> op({ set(DE, false, 7) }, 8)
                    // SET 7 H
                    0xFCu -> op({ set(HL, true, 7) }, 8)
                    // SET 7 L
                    0xFDu -> op({ set(HL, false, 7) }, 8)
                    // SET 7 (HL)
                    0xFEu -> op({ setHL(7) }, 16)
                    // SET 7 A
                    0xFFu -> op({ set(AF, true, 7) }, 8)

                    // RES 0 B
                    0x80u -> op({ reset(BC, true, 0) }, 8)
                    // RES 0 C
                    0x81u -> op({ reset(BC, false, 0) }, 8)
                    // RES 0 D
                    0x82u -> op({ reset(DE, true, 0) }, 8)
                    // RES 0 E
                    0x83u -> op({ reset(DE, false, 0) }, 8)
                    // RES 0 H
                    0x84u -> op({ reset(HL, true, 0) }, 8)
                    // RES 0 L
                    0x85u -> op({ reset(HL, false, 0) }, 8)
                    // RES 0 (HL)
                    0x86u -> op({ resetHL(0) }, 16)
                    // RES 0 A
                    0x87u -> op({ reset(AF, true, 0) }, 8)

                    // RES 1 B
                    0x88u -> op({ reset(BC, true, 1) }, 8)
                    // RES 1 C
                    0x89u -> op({ reset(BC, false, 1) }, 8)
                    // RES 1 D
                    0x8Au -> op({ reset(DE, true, 1) }, 8)
                    // RES 1 E
                    0x8Bu -> op({ reset(DE, false, 1) }, 8)
                    // RES 1 H
                    0x8Cu -> op({ reset(HL, true, 1) }, 8)
                    // RES 1 L
                    0x8Du -> op({ reset(HL, false, 1) }, 8)
                    // RES 1 (HL)
                    0x8Eu -> op({ resetHL(1) }, 16)
                    // RES 1 A
                    0x8Fu -> op({ reset(AF, true, 1) }, 8)

                    // RES 2 B
                    0x90u -> op({ reset(BC, true, 2) }, 8)
                    // RES 2 C
                    0x91u -> op({ reset(BC, false, 2) }, 8)
                    // RES 2 D
                    0x92u -> op({ reset(DE, true, 2) }, 8)
                    // RES 2 E
                    0x93u -> op({ reset(DE, false, 2) }, 8)
                    // RES 2 H
                    0x94u -> op({ reset(HL, true, 2) }, 8)
                    // RES 2 L
                    0x95u -> op({ reset(HL, false, 2) }, 8)
                    // RES 2 (HL)
                    0x96u -> op({ resetHL(2) }, 16)
                    // RES 2 A
                    0x97u -> op({ reset(AF, true, 2) }, 8)

                    // RES 3 B
                    0x98u -> op({ reset(BC, true, 3) }, 8)
                    // RES 3 C
                    0x99u -> op({ reset(BC, false, 3) }, 8)
                    // RES 3 D
                    0x9Au -> op({ reset(DE, true, 3) }, 8)
                    // RES 3 E
                    0x9Bu -> op({ reset(DE, false, 3) }, 8)
                    // RES 3 H
                    0x9Cu -> op({ reset(HL, true, 3) }, 8)
                    // RES 3 L
                    0x9Du -> op({ reset(HL, false, 3) }, 8)
                    // RES 3 (HL)
                    0x9Eu -> op({ resetHL(3) }, 16)
                    // RES 3 A
                    0x9Fu -> op({ reset(AF, true, 3) }, 8)

                    // RES 4 B
                    0xA0u -> op({ reset(BC, true, 4) }, 8)
                    // RES 4 C
                    0xA1u -> op({ reset(BC, false, 4) }, 8)
                    // RES 4 D
                    0xA2u -> op({ reset(DE, true, 4) }, 8)
                    // RES 4 E
                    0xA3u -> op({ reset(DE, false, 4) }, 8)
                    // RES 4 H
                    0xA4u -> op({ reset(HL, true, 4) }, 8)
                    // RES 4 L
                    0xA5u -> op({ reset(HL, false, 4) }, 8)
                    // RES 4 (HL)
                    0xA6u -> op({ resetHL(4) }, 16)
                    // RES 4 A
                    0xA7u -> op({ reset(AF, true, 4) }, 8)

                    // RES 5 B
                    0xA8u -> op({ reset(BC, true, 5) }, 8)
                    // RES 5 C
                    0xA9u -> op({ reset(BC, false, 5) }, 8)
                    // RES 5 D
                    0xAAu -> op({ reset(DE, true, 5) }, 8)
                    // RES 5 E
                    0xABu -> op({ reset(DE, false, 5) }, 8)
                    // RES 5 H
                    0xACu -> op({ reset(HL, true, 5) }, 8)
                    // RES 5 L
                    0xADu -> op({ reset(HL, false, 5) }, 8)
                    // RES 5 (HL)
                    0xAEu -> op({ resetHL(5) }, 16)
                    // RES 5 A
                    0xAFu -> op({ reset(AF, true, 5) }, 8)

                    // RES 6 B
                    0xB0u -> op({ reset(BC, true, 6) }, 8)
                    // RES 6 C
                    0xB1u -> op({ reset(BC, false, 6) }, 8)
                    // RES 6 D
                    0xB2u -> op({ reset(DE, true, 6) }, 8)
                    // RES 6 E
                    0xB3u -> op({ reset(DE, false, 6) }, 8)
                    // RES 6 H
                    0xB4u -> op({ reset(HL, true, 6) }, 8)
                    // RES 6 L
                    0xB5u -> op({ reset(HL, false, 6) }, 8)
                    // RES 6 (HL)
                    0xB6u -> op({ resetHL(6) }, 16)
                    // RES 6 A
                    0xB7u -> op({ reset(AF, true, 6) }, 8)

                    // RES 7 B
                    0xB8u -> op({ reset(BC, true, 7) }, 8)
                    // RES 7 C
                    0xB9u -> op({ reset(BC, false, 7) }, 8)
                    // RES 7 D
                    0xBAu -> op({ reset(DE, true, 7) }, 8)
                    // RES 7 E
                    0xBBu -> op({ reset(DE, false, 7) }, 8)
                    // RES 7 H
                    0xBCu -> op({ reset(HL, true, 7) }, 8)
                    // RES 7 L
                    0xBDu -> op({ reset(HL, false, 7) }, 8)
                    // RES 7 (HL)
                    0xBEu -> op({ resetHL(7) }, 16)
                    // RES 7 A
                    0xBFu -> op({ reset(AF, true, 7) }, 8)

                    else -> throw Exception("Unknown OP instruction: $instruction")
                }
            }

            // DAA
            // This operation implementation mostly comes from https://ehaskins.com/2018-01-30%20Z80%20DAA/
            // as it hard to make sense out of it
            0x27u -> op({
                if (!AF.getNFlag()) { // Last operation was a sum
                    var result = AF.left.toUInt()
                    // Check if carry or result is more than 9
                    if (AF.getHalfCarryFlag() || result and 0x0Fu > 0x9u) {
                        // Add correction to get correct binary coded decimal
                        result += 0x6u
                    }
                    // Check for carry flag or if the left digit is also higher than 9 and also need correction
                    if (AF.getCarryFlag() || result > 0x9Fu) {
                        result += 0x60u
                    }
                    if (result > 0xffu) {
                        AF.setCarryFlag(true)
                    }
                    AF.left = result.toUByte()
                } else {
                    // Checking for the flags but not for the value itself for some reason
                    if (AF.getHalfCarryFlag()) {
                        AF.left = (AF.left - 0x6u).toUByte()
                    }
                    if (AF.getCarryFlag()) {
                        AF.left = (AF.left - 0x60u).toUByte()
                    }
                }
                AF.setZeroFlag(AF.left.toUInt() == 0x0u)
                AF.setHalfCarryFlag(false)
            }, 4)

            // CPL A
            0x2Fu -> op({
                AF.left = AF.left.inv()
                AF.setNFlag(true)
                AF.setHalfCarryFlag(true)
            }, 4)
            // CCF
            0x3Fu -> op({
                AF.setNFlag(false)
                AF.setHalfCarryFlag(false)
                AF.setCarryFlag(!AF.getCarryFlag())
            }, 4)
            // SCF
            0x37u -> op({
                AF.setNFlag(false)
                AF.setHalfCarryFlag(false)
                AF.setCarryFlag(true)
            }, 4)
            // NOP
            0x00u -> op({}, 4)
            // HALT
            0x76u -> op({
                halt = true
            }, 4)
            // STOP
            0x10u -> {
                when (readOp().toUInt()) {
                    0x00u -> op({
                        //TODO add sleep until button press
                    }, 4)
                    else -> throw Exception("Unknown OP instruction: $instruction")
                }
            }
            //DI
            0xF3u -> op({
                interruptMasterEnabled = false
            }, 4)
            //EI
            0xFBu -> op({
                actionAfterInstruction = { interruptMasterEnabled = true }
            }, 4)

            // Rotates and shifts
            // RLCA
            0x07u -> op({
                RLC(AF, true)
                AF.setZeroFlag(false)
            }, 4)
            // RLA
            0x17u -> op({
                RL(AF, true)
                AF.setZeroFlag(false)
            }, 4)
            // RRCA
            0x0Fu -> op({
                RRC(AF, true)
                AF.setZeroFlag(false)
            }, 4)
            // RRA
            0x1Fu -> op({
                RR(AF, true)
                AF.setZeroFlag(false)
            }, 4)

            // JP nn
            0xC3u -> op({ jump() }, 16)

            // JP NZ nn
            0xC2u -> opVariableCycleCount({ jump { !AF.getZeroFlag() } })
            // JP Z nn
            0xCAu -> opVariableCycleCount({ jump { AF.getZeroFlag() } })
            // JP NC nn
            0xD2u -> opVariableCycleCount({ jump { !AF.getCarryFlag() } })
            // JP C nn
            0xDAu -> opVariableCycleCount({ jump { AF.getCarryFlag() } })
            // JP (HL)
            0xE9u -> op({
                programCounter = HL.both()
            }, 4)

            // JR n
            0x18u -> op({ jumpRelative() }, 12)
            // JR NZ n
            0x20u -> opVariableCycleCount({ jumpRelative { !AF.getZeroFlag() } })
            // JR Z n
            0x28u -> opVariableCycleCount({ jumpRelative { AF.getZeroFlag() } })
            // JR NC n
            0x30u -> opVariableCycleCount({ jumpRelative { !AF.getCarryFlag() } })
            // JR C n
            0x38u -> opVariableCycleCount({ jumpRelative { AF.getCarryFlag() } })

            // CALL nn
            0xCDu -> op({ call() }, 24)
            // CALL NZ nn
            0xC4u -> opVariableCycleCount({ call {!AF.getZeroFlag() } })
            // CALL Z nn
            0xCCu -> opVariableCycleCount({ call {AF.getZeroFlag() } })
            // CALL NC nn
            0xD4u -> opVariableCycleCount({ call {!AF.getCarryFlag() } })
            // CALL C nn
            0xDCu -> opVariableCycleCount({ call {AF.getCarryFlag() } })

            // RST 00
            0xC7u -> op({ restart(0u) }, 16)
            // RST 08
            0xCFu -> op({ restart(0x08u) }, 16)
            // RST 10
            0xD7u -> op({ restart(0x10u) }, 16)
            // RST 18
            0xDFu -> op({ restart(0x08u) }, 16)
            // RST 20
            0xE7u -> op({ restart(0x08u) }, 16)
            // RST 28
            0xEFu -> op({ restart(0x28u) }, 16)
            // RST 30
            0xF7u -> op({ restart(0x30u) }, 16)
            // RST 38
            0xFFu -> op({ restart(0x38u) }, 16)

            // RET
            0xC9u -> op({ `return`() }, 16)
            // RET NZ
            0xC0u -> opVariableCycleCount({ `return` { !AF.getZeroFlag()} })
            // RET Z
            0xC8u -> opVariableCycleCount({ `return` { AF.getZeroFlag()} })
            // RET NC
            0xD0u -> opVariableCycleCount({ `return` { !AF.getCarryFlag()} })
            // RET C
            0xD8u -> opVariableCycleCount({ `return` { AF.getCarryFlag()} })
            // RETI
            0xD9u -> op({
                `return`()
                interruptMasterEnabled = true
            }, 16)

            else -> throw Exception("Unknown OP instruction: $instruction")
        }
        return Pair(cycleCount, actionAfterInstruction)
    }

    private inline fun `return`(predicate: (() -> Boolean) = {true}): Int {
        if (predicate.invoke()) {
            val jumpAddressLeastSignificant = memory.get(stackPointer++)
            val jumpAddressMostSignificant = memory.get(stackPointer++)
            programCounter =
                (jumpAddressMostSignificant.toUInt() shl 8 or jumpAddressLeastSignificant.toUInt()).toUShort()
            return 20
        }
        return 8
    }

    private inline fun call(predicate: (() -> Boolean) = {true}): Int {
        val jumpAddress = readNN()
        if (predicate.invoke()) {
            storeShortToStack(programCounter)
            programCounter = jumpAddress
            return 24
        }
        return 12
    }

    private inline fun jumpRelative(predicate: (() -> Boolean) = {true}): Int {
        // the next byte is read is considered signed and therefore can be a sum or a subtraction
        val offset = readOp().toByte() // The read should be done first is order to increment the program counter before the jump
        val jumpAddress = (programCounter.toShort() + offset).toUShort()
        if (predicate.invoke()) {
            programCounter = jumpAddress
            return 12
        }
        return 8
    }

    private inline fun jump(predicate: (() -> Boolean) = {true}): Int {
        val jumpAddress = readNN()
        if (predicate.invoke()) {
            programCounter = jumpAddress
            return 16
        }
        return 12
    }

    private fun readOp() = memory.get(programCounter++)
    private fun readNN():UShort {
        return (readOp().toUInt() or (readOp().toUInt() shl 8)).toUShort()
    }

    private inline fun addOp(number: UByte) {
        val result = AF.left + number
        val carriedBits = result xor AF.left.toUInt() xor number.toUInt()
        AF.left = (result).toUByte()
        AF.setZeroFlag(result.toUByte() == (0u).toUByte())
        AF.setNFlag(false)
        AF.setHalfCarryFlag((carriedBits and 0x10u) != 0u)
        AF.setCarryFlag((carriedBits and 0x100u) != 0u)
    }

    private inline fun subOp(number: UByte) {
        val result = AF.left - number
        val carriedBits = result xor AF.left.toUInt() xor number.toUInt()
        AF.left = (result).toUByte()
        AF.setZeroFlag(result.toUByte() == (0u).toUByte())
        AF.setNFlag(true)
        AF.setHalfCarryFlag((carriedBits and 0x10u) != 0u)
        AF.setCarryFlag((carriedBits and 0x100u) != 0u)
    }

    private inline fun addCarryOp(number: UByte) {
        val carryValue = if (AF.getCarryFlag()) 1u else 0u
        val result = AF.left + number + carryValue
        AF.setZeroFlag(result.toUByte() == (0u).toUByte())
        AF.setNFlag(false)
        AF.setHalfCarryFlag((AF.left and 0x0Fu) + (number and 0x0Fu) + carryValue > 0X0Fu)
        AF.setCarryFlag(result > 0xFFu)
        AF.left = (result).toUByte()
    }

    private inline fun subCarryOp(number: UByte) {
        val carryValue = if (AF.getCarryFlag()) 1u else 0u
        val result = AF.left - number - carryValue
        val carriedBits = result xor AF.left.toUInt() xor number.toUInt()
        AF.setZeroFlag(result.toUByte() == (0u).toUByte())
        AF.setNFlag(true)
        AF.setHalfCarryFlag((carriedBits and 0x10u) != 0u)
        AF.setCarryFlag((carriedBits and 0x100u) != 0u)
        AF.left = (result).toUByte()
    }

    private inline fun andOp(number: UByte) {
        val result = AF.left and number
        AF.left = result
        AF.setZeroFlag(result.toUInt() == 0x0u)
        AF.setNFlag(false)
        AF.setHalfCarryFlag(true)
        AF.setCarryFlag(false)
    }

    private inline fun orOp(number: UByte) {
        val result = AF.left or number
        AF.left = result
        AF.resetFlags()
        AF.setZeroFlag(result.toUInt() == 0x0u)
    }

    private inline fun xorOp(number: UByte) {
        val result = AF.left xor number
        AF.left = result
        AF.resetFlags()
        AF.setZeroFlag(result.toUInt() == 0x0u)
    }

    private inline fun cpOp(number: UByte) {
        val result = AF.left - number
        val carriedBits = result xor AF.left.toUInt() xor number.toUInt()

        AF.setZeroFlag(result == 0u)
        AF.setNFlag(true)
        AF.setHalfCarryFlag((carriedBits and 0x10u) != 0u)
        AF.setCarryFlag((carriedBits and 0x100u) != 0u)
    }

    private inline fun incOp(register: SplitRegister, left: Boolean) {
        val result = if (left) {
            ++register.left
        } else {
            ++register.right
        }
        AF.setZeroFlag(result.toUInt() == 0u)
        AF.setNFlag(false)
        AF.setHalfCarryFlag((result and 0x0Fu).toUInt() == 0x00u)
    }

    private inline fun decOp(register: SplitRegister, left: Boolean) {
        val result = if (left) {
            --register.left
        } else {
            --register.right
        }
        AF.setZeroFlag(result == (0u).toUByte())
        AF.setNFlag(true)
        AF.setHalfCarryFlag((result and 0x0Fu).toUInt() == 0x0Fu)
    }

    private inline fun add16Op(number: UShort) {
        val result = number + HL.both()
        AF.setNFlag(false)
        AF.setHalfCarryFlag((HL.both() and 0x0FFFu) + (number and 0x0FFFu) > 0X0FFFu)
        AF.setCarryFlag(result > 0xFFFFu)
        HL.setBoth(result.toUShort())
    }

    private inline fun swapOp(register: SplitRegister, left: Boolean) {
        AF.resetFlags()
        if (left) {
            register.left = ((register.left.toUInt() shr 4) + ((register.left and 0xFu).toUInt() shl 4)).toUByte()
            AF.setZeroFlag(register.left.toUInt() == 0x0u)
        } else {
            register.right = ((register.right.toUInt() shr 4) + ((register.right and 0xFu).toUInt() shl 4)).toUByte()
            AF.setZeroFlag(register.right.toUInt() == 0x0u)
        }
    }

    private inline fun RLC(register: SplitRegister, left: Boolean) {
        val result = if(left) {
            register.left = register.left.rotateLeft(1)
            register.left
        } else {
            register.right = register.right.rotateLeft(1)
            register.right
        }
        AF.setZeroFlag(result.toUInt() == 0u)
        AF.setNFlag(false)
        AF.setHalfCarryFlag(false)
        AF.setCarryFlag(result.toUInt() and 1u == 1u)
    }

    private inline fun RRC(register: SplitRegister, left: Boolean) {
        val result = if(left) {
            register.left = register.left.rotateRight(1)
            register.left
        } else {
            register.right = register.right.rotateRight(1)
            register.right
        }
        AF.setZeroFlag(result.toUInt() == 0u)
        AF.setNFlag(false)
        AF.setHalfCarryFlag(false)
        AF.setCarryFlag(result.toUInt() and 0b1000_0000u > 0u)
    }

    private inline fun RL(register: SplitRegister, left: Boolean) {
        val value = if(left) {
            register.left
        } else {
            register.right
        }

        val newCarry = value.toUInt() and 0b1000_0000u
        val oldCarry = if(AF.getCarryFlag()) 1u else 0u

        val result = ((value.toUInt() shl 1) + oldCarry).toUByte()
        if (left) {
            register.left = result
        } else {
            register.right = result
        }
        AF.setZeroFlag(result.toUInt() == 0u)
        AF.setNFlag(false)
        AF.setHalfCarryFlag(false)
        AF.setCarryFlag(newCarry != 0u)
    }

    private inline fun RR(register: SplitRegister, left: Boolean) {
        val value = if(left) {
            register.left
        } else {
            register.right
        }

        val newCarry = value.toUInt() and 0b0000_0001u
        val oldCarry = if(AF.getCarryFlag()) 0b1000_0000u else 0u
        val result = ((value.toUInt() shr 1) + oldCarry).toUByte()
        if (left) {
            register.left = result
        } else {
            register.right = result
        }

        AF.setZeroFlag(result.toUInt() == 0u)
        AF.setNFlag(false)
        AF.setHalfCarryFlag(false)
        AF.setCarryFlag(newCarry == 1u)

    }

    private inline fun SRA(register: SplitRegister, left: Boolean) {
        val value = if(left) {
            register.left
        } else {
            register.right
        }

        // Arithmetic shift, the most significant bit (the sign bit) is kept when shifting
        // Kotlin does it by itself when using signed int but converting to and from unsigned byte breaks this
        val mostSignificant = value and 0b1000_0000u
        val result = (value.toUInt() shr 1 or mostSignificant.toUInt()).toUByte()
        // Storing previous bit 0 to save it in the carry
        val leastSignificant = value and 0b0000_0001u
        if (left) {
            register.left = result
        } else {
            register.right = result
        }
        AF.setZeroFlag(result.toUInt() == 0u)
        AF.setNFlag(false)
        AF.setHalfCarryFlag(false)
        AF.setCarryFlag(leastSignificant.toUInt() == 1u)
    }

    private inline fun SLA(register: SplitRegister, left: Boolean) {
        val value = if(left) {
            register.left
        } else {
            register.right
        }

        // Storing most significant bit to be stored in the carry flag
        val mostSignificant = value and 0b1000_0000u
        val result = (value.toUInt() shl 1).toUByte()
        if (left) {
            register.left = result
        } else {
            register.right = result
        }
        AF.setZeroFlag(result.toUInt() == 0u)
        AF.setNFlag(false)
        AF.setHalfCarryFlag(false)
        AF.setCarryFlag(mostSignificant >= 1u)
    }

    private inline fun SRL(register: SplitRegister, left: Boolean) {
        val value = if(left) {
            register.left
        } else {
            register.right
        }

        // Logical shift, the most significant bit is set to 0
        val result = (value.toUInt() shr 1).toUByte()
        // Storing previous bit 0 to save it in the carry
        val leastSignificant = value and 0b0000_0001u
        if (left) {
            register.left = result
        } else {
            register.right = result
        }
        AF.setZeroFlag(result.toUInt() == 0u)
        AF.setNFlag(false)
        AF.setHalfCarryFlag(false)
        AF.setCarryFlag(leastSignificant.toUInt() == 1u)
    }

    private inline fun bit(value: UByte, bit: Int) {
        if (value.toUInt() shr bit and 0x01u == 1u) {
            AF.setZeroFlag(false)
        } else {
            AF.setZeroFlag(true)
        }
        AF.setNFlag(false)
        AF.setHalfCarryFlag(true)
    }

    private inline fun bit(register: SplitRegister, left: Boolean, bit: Int) {
        val value = if(left) {
            register.left
        } else {
            register.right
        }
        bit(value, bit)
    }

    private inline fun bitHL(bit: Int) {
        val value = memory.get(HL.both())
        bit(value, bit)
    }

    private inline fun set(value: UByte, bit: Int): UByte {
        return value or (1u shl bit).toUByte()
    }

    private inline fun set(register: SplitRegister, left: Boolean, bit: Int) {
        if(left) {
            register.left = set(register.left, bit)
        } else {
            register.right = set(register.right, bit)
        }
    }

    private inline fun setHL(bit: Int) {
         val result = set(memory.get(HL.both()), bit)
        memory.set(HL.both(), result)
    }

    private inline fun reset(value: UByte, bit: Int): UByte {
        return value and (1u shl bit).toUByte().inv()
    }

    private inline fun reset(register: SplitRegister, left: Boolean, bit: Int) {
        if(left) {
            register.left = reset(register.left, bit)
        } else {
            register.right = reset(register.right, bit)
        }
    }

    private inline fun resetHL(bit: Int) {
        val result = reset(memory.get(HL.both()), bit)
        memory.set(HL.both(), result)
    }

    private inline fun restart(newAddress: UShort) {
        storeShortToStack(programCounter)

        programCounter = newAddress
    }

    private inline fun storeShortToStack(short: UShort) {
        memory.set(--stackPointer, (short.toUInt() shr 8 ).toUByte())
        memory.set(--stackPointer, short.toUByte())
    }

        private fun op(command: () -> Unit, cycleCount: Int): Int {
            command()
            return cycleCount
        }
        private fun opVariableCycleCount(command: () -> Int) : Int {
            return command()
        }

        open class SplitRegister {
            var left: UByte = 0u
            open var right: UByte = 0u

            inline fun both(): UShort {
                return (left.toUInt() shl 8 or right.toUInt()).toUShort()
            }

            inline fun setBoth(value: UShort) {
                left = (value.toUInt() shr 8).toUByte()
                right = value.toUByte()
            }

            inline fun increment() {
                val increment = (left.toUInt() shl 8 or right.toUInt()).plus(1u)
                right = increment.toUByte()
                left = (increment shr 8).toUByte()
            }

            inline fun decrement() {
                val decrement = (left.toUInt() shl 8 or right.toUInt()).minus(1u)
                right = decrement.toUByte()
                left = (decrement shr 8).toUByte()
            }

        }

        class AFRegister : SplitRegister() {

            override var right: UByte = 0u
                set(value) {
                    field = (value and 0xF0u) // The lower nibble of the F register should always be 0
                }

            inline fun resetFlags() {
                right = 0x0000u
            }

            inline fun setZeroFlag(flag: Boolean) {
                if (flag) {
                    right = (0b1000_0000u or right.toUInt()).toUByte()
                } else {
                    right = (0b0111_1111u and right.toUInt()).toUByte()
                }
            }

            inline fun setNFlag(flag: Boolean) {
                if (flag) {
                    right = (0b0100_0000u or right.toUInt()).toUByte()
                } else {
                    right = (0b1011_1111u and right.toUInt()).toUByte()
                }
            }

            inline fun setSubtractFlag(flag: Boolean) {
                if (flag) {
                    right = (0b0100_0000u or right.toUInt()).toUByte()
                } else {
                    right = (0b1011_1111u and right.toUInt()).toUByte()
                }
            }

            inline fun setHalfCarryFlag(flag: Boolean) {
                if (flag) {
                    right = (0b0010_0000u or right.toUInt()).toUByte()
                } else {
                    right = (0b1101_1111u and right.toUInt()).toUByte()
                }
            }

            inline fun setCarryFlag(flag: Boolean) {
                if (flag) {
                    right = (0b0001_0000u or right.toUInt()).toUByte()
                } else {
                    right = (0b1110_1111u and right.toUInt()).toUByte()
                }
            }

            inline fun getCarryFlag(): Boolean {
                return 0b0001_0000u and right.toUInt() > 0u
            }

            fun getNFlag(): Boolean {
                return 0b0100_0000u and right.toUInt() > 0u
            }

            fun getHalfCarryFlag(): Boolean {
                return 0b0010_0000u and right.toUInt() > 0u
            }

            fun getZeroFlag(): Boolean {
                return 0b1000_0000u and right.toUInt() > 0u
            }
        }

    }