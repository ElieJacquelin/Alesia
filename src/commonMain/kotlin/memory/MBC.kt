package memory

sealed interface MBC {
    fun get(address: UShort): UByte
    fun set(address: UShort, value:UByte)

}