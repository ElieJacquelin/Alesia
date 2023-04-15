package memory

@OptIn(ExperimentalUnsignedTypes::class)
// Default no memory controller which simply read and write from the main memory
class MBC0(val rom: UByteArray, val disableWritingToRom: Boolean = true) : MBC {

    private val ram = UByteArray(0x2000)

    override fun get(address: UShort): UByte {
        return when(address) {
            in 0x0000u..0x7FFFu -> rom[address.toInt()]
            in 0xA000u..0xBFFFu -> {
                 ram[(address - 0xA000u).toInt()]
            }

            else -> throw Exception("Invalid MBC1 read address: $address")
        }
    }

    override fun set(address: UShort, value: UByte) {
        if (address < 0x8000u && disableWritingToRom) {
            return
        }
        when(address) {
            in 0x0000u..0x7FFFu -> {
                // A significant amount of unit tests writes onto the ROM, we allow an option for those tests to bypass this restriction
                if(!disableWritingToRom) {
                    rom[address.toInt()] = value
                }
            }
            in 0xA000u..0xBFFFu -> {
                ram[(address - 0xA000u).toInt()] = value
            }
        }

    }

}