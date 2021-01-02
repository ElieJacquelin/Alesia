@ExperimentalUnsignedTypes
class Memory {
    private val memory = UByteArray(0x10000)

    fun get(address: UShort): UByte {
        return memory[address.toInt()]
    }

    fun set(address: UShort, value:UByte) {
        memory[address.toInt()] = value
    }
}