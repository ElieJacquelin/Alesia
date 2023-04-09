import io.Joypad

@ExperimentalUnsignedTypes
open class Memory(val joypad: Joypad = Joypad(), val disableWritingToRom: Boolean=  false) {
    private val VRAM = 0x8000u..0x9FFFu
    private val OAM = 0xFE00u..0xFE9Fu

    private var isVRAMLocked = false
    private var isOAMLocked = false
    private val memory = UByteArray(0x10000)
    // Memory map (from Pan Docs)
    // Start  End	Description	                    Notes
    // 0000   3FFF	16 KiB ROM bank 00	            From cartridge, usually a fixed bank
    // 4000	  7FFF	16 KiB ROM Bank 01~NN	        From cartridge, switchable bank via mapper (if any)
    // 8000	  9FFF	8 KiB Video RAM (VRAM)	        In CGB mode, switchable bank 0/1
    // A000	  BFFF	8 KiB External RAM	            From cartridge, switchable bank if any
    // C000	  CFFF	4 KiB Work RAM (WRAM)
    // D000	  DFFF	4 KiB Work RAM (WRAM)	        In CGB mode, switchable bank 1~7
    // E000	  FDFF	Mirror of C000~DDFF (ECHO RAM)	Nintendo says use of this area is prohibited.
    // FE00	  FE9F	Sprite attribute table (OAM)
    // FEA0	  FEFF	Not Usable	                    Nintendo says use of this area is prohibited
    // FF00	  FF7F	I/O Registers
    // FF80	  FFFE	High RAM (HRAM)
    // FFFF	  FFFF	Interrupt Enable register (IE)

    init {
        set(0xFF10u, 0x80u)
        set(0xFF11u, 0xBFu)
        set(0xFF12u, 0xF3u)
        set(0xFF14u, 0xBFu)
        set(0xFF16u, 0x3Fu)
        set(0xFF19u, 0xBFu)
        set(0xFF1Au, 0x7Fu)
        set(0xFF1Bu, 0xFFu)
        set(0xFF1Cu, 0x9Fu)
        set(0xFF1Eu, 0xBFu)
        set(0xFF20u, 0xFFu)
        set(0xFF23u, 0xBFu)
        set(0xFF24u, 0x77u)
        set(0xFF25u, 0xF3u)
        set(0xFF26u, 0xF1u)
        set(0xFF26u, 0xF1u)
        set(0xFF40u, 0x91u)
        set(0xFF47u, 0xFCu)
        set(0xFF48u, 0xFFu)
        set(0xFF49u, 0xFFu)
    }

    fun get(address: UShort, isGPU: Boolean = false): UByte {
        // Handle VRAM being locked
        if(!isGPU && VRAM.contains(address) && isVRAMLocked) {
            return 0xFFu
        }

        if(!isGPU &&OAM.contains(address) && isOAMLocked) {
            return 0xFFu
        }

        if(address == 0xFF00u.toUShort()) {
            val joypadControl = memory[0xFF00u.toInt()] and 0b0011_0000u
            return joypad.generateJoypadValue(joypadControl)
        }
        return memory[address.toInt()]
    }

    fun set(address: UShort, value:UByte, isGPU: Boolean = false) {
        // Prevent writing values onto the ROM itself
        // A significant amount of unit tests writes onto the ROM, we allow an option for those tests to bypass this restriction
        if (address < 0x8000u && !disableWritingToRom) {
            return
        }

        // Prevent writing to VRAM if locked
        // TODO this is causing issue with Tetris, maybe the code is correct but because of timing error this shows up
//        if(!isGPU && VRAM.contains(address) && isVRAMLocked) {
//            return
//        }

        // Prevent writing to OAM if locked (Still allow writing via DMA)
        if(!isGPU && OAM.contains(address) && isOAMLocked) {
            return
        }

        memory[address.toInt()] = value

        if(address == 0xFF46u.toUShort()) {
            // Trigger DMA: store sprites into OAM
            for((index, sourceAddress) in (value * 0x100u..(value * 0x100u) + 0x9Fu).withIndex()) {
                set((0xFE00u + index.toUInt()).toUShort(), get(sourceAddress.toUShort()))
            }
            // TODO add DMA cycle time
        } else if (address == 0xFF04u.toUShort()) {
            // Reset DIV
            memory[address.toInt()] = 0u
        }
    }

    fun incrementDiv() {
        val address = (0xFF04u).toInt()
        memory[address]++
    }

    fun loadRom(rom: UByteArray) {
        // The rom takes the first 32kb of memory
        rom.copyInto(memory)
    }

    fun handleKey(key: Joypad.Key, pressed: Boolean) {
        joypad.handleKey(key, pressed)
    }

    fun lockVRAM() {
        isVRAMLocked = true
    }

    fun unlockVRAM() {
        isVRAMLocked = false
    }

    fun lockOAM() {
        isOAMLocked = true
    }

    fun unlockOAM() {
        isOAMLocked = false
    }
}