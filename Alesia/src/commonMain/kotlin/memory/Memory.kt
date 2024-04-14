package memory

import CPU
import io.Joypad


@OptIn(ExperimentalStdlibApi::class)
@ExperimentalUnsignedTypes
open class Memory(val joypad: Joypad = Joypad(), val disableWritingToRom: Boolean = true) {

    // Disable memory lock during rendering stages as the timing is too off
    // Locking memory creates more rendering problems
    private val DISABLE_MEMORY_LOCK = true

    private var isVRAMLocked = false
    private var isOAMLocked = false

    private lateinit var mbc: MBC

    private val vram = UByteArray(0x2000)
    private val wram = UByteArray(0x2000)
    private val oamAndMore = UByteArray(0x200)

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

    var cpu: CPU? = null

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
        // Performance: Find the correct memory space based on the first nibble instead of comparing the whole range
        val memoryIndex = address.and(0xF000u).toUInt()
        return when(memoryIndex) {
            0x0000u, 0x1000u, 0x2000u, 0x3000u, 0x4000u, 0x5000u, 0x6000u, 0x7000u -> mbc.get(address) // Cartridge ROM
            0x8000u, 0x9000u -> { // VRAM
                if(!isGPU && isVRAMLocked && !DISABLE_MEMORY_LOCK) {
                    0xFFu
                } else {
                    vram[((address - 0x8000u).toInt())]
                }
            }
            0xA000u, 0xB000u -> mbc.get(address) // Cartridge RAM
            0xC000u, 0xD000u -> wram[(address - 0xC000u).toInt()] // Work RAM
            0xE000u, 0xF000u -> {
                return if(address <= 0xFDFFu) {
                    wram[(address - 0xE000u).toInt()]// Echo
                } else if(address <= 0xFE9Fu) {
                    if(!isGPU && isOAMLocked && !DISABLE_MEMORY_LOCK) {
                        0xFFu
                    } else {
                        oamAndMore[(address - 0xFE00u).toInt()]
                    }
                } else if(address == 0xFF00u.toUShort()) {
                    val joypadControl = oamAndMore[0x100] and 0b0011_0000u
                    joypad.generateJoypadValue(joypadControl)
                } else {
                    oamAndMore[(address - 0xFE00u).toInt()] // Rest of memory
                }
            }
            else -> throw throw Exception("Invalid address read in memory")

        }
    }

    fun set(address: UShort, value:UByte, isGPU: Boolean = false) {
        when(address) {
            in 0x0000u..0x7FFFu -> mbc.set(address, value) // Cartridge ROM
            in 0x8000u..0x9FFFu -> { // VRAM
                if(!isGPU && isVRAMLocked && !DISABLE_MEMORY_LOCK) {
                    return
                }

                vram[((address - 0x8000u).toInt())] = value
            }
            in 0xA000u..0xBFFFu -> mbc.set(address, value) // Cartridge RAM
            in 0xC000u..0xDFFFu -> wram[(address - 0xC000u).toInt()] = value // Work RAN
            in 0xE000u..0xFDFFu -> {} // Echo, ignore writes
            in 0xFE00u..0xFE9Fu -> { // OAM
                // Prevent writing to OAM if locked (Still allow writing via DMA)
                if(!isGPU && isOAMLocked && !DISABLE_MEMORY_LOCK) {
                    return
                } else {
                    oamAndMore[(address - 0xFE00u).toInt()] = value
                }
            }
            0xFF04u.toUShort() -> { // DIV
                // Reset DIV
                oamAndMore[0x104u.toInt()] = 0u
                cpu?.resetDiv()
            }
            0xFF46u.toUShort() -> {
                // Trigger DMA: store sprites into OAM
                for((index, sourceAddress) in (value * 0x100u..(value * 0x100u) + 0x9Fu).withIndex()) {
                    set((0xFE00u + index.toUInt()).toUShort(), get(sourceAddress.toUShort()))
                }
            }
            in 0xFEA0u..0xFFFFu -> oamAndMore[(address - 0xFE00u).toInt()] = value // Rest of memory
            else -> {
                throw Exception("Invalid address write in memory")
            }
        }
    }

    fun incrementDiv() {
        oamAndMore[0x104u.toInt()]++
    }

    fun loadRom(rom: UByteArray) {
        // Load the RAM amount
        val ramSize = when(rom[0x149].toUInt()) {
            0x00u, 0x01u -> 0x2000
            0x02u -> 0x2000
            0x03u -> 0x8000
            0x04u -> 0x2_0000
            0x05u -> 0x1_0000
            else -> throw Exception("Unsupported RAM")
        }

        // Load the MBC type of the rom
        mbc = when(rom[0x0147].toUInt()) {
            0x00u -> MBC0(rom, disableWritingToRom)
            0x01u, 0x02u -> {
                MBC1( rom, ramSize , false)
            }
            0x03u -> {
                MBC1( rom, ramSize , true)
            }
            0x11u, 0x12u -> {
                MBC3( rom, ramSize, false)
            }
            0x13u -> {
                MBC3( rom, ramSize, true)
            }
            else -> throw Exception("Unsupported MBC")
        }
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

    fun dumpRam(): UByteArray {
        return this.mbc.dumpRam()
    }

    fun loadRam(ram: UByteArray) {
        this.mbc.loadRam(ram)
    }
}