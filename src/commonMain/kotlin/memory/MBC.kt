package memory

@OptIn(ExperimentalUnsignedTypes::class)
sealed interface MBC {

    val hasBattery: Boolean
    var ram: UByteArray
    fun get(address: UShort): UByte
    fun set(address: UShort, value:UByte)

    fun dumpRam(): UByteArray {
        if (!hasBattery) {
            return UByteArray(0)
        }
        return ram
    }

    fun loadRam(ram: UByteArray) {
        if(this.ram.size != ram.size) {
            throw Exception("Wrong RAM size")
        }
        this.ram = ram
    }

}