@ExperimentalUnsignedTypes
class Memory {
    private val memory = UByteArray(0x10000)

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