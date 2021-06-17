@ExperimentalUnsignedTypes
class Memory {
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

    fun get(address: UShort): UByte {
        return memory[address.toInt()]
    }

    fun set(address: UShort, value:UByte) {
        memory[address.toInt()] = value
    }
}