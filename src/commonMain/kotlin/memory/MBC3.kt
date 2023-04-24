package memory

@OptIn(ExperimentalUnsignedTypes::class)
class MBC3(private val rom: UByteArray, ramSize: Int, override val hasBattery: Boolean): MBC {

    private var ramEnabled: Boolean = false
    private var romBankNumber: UByte = 1u
    private var ramBankNumber: UByte = 0u

    override var ram = UByteArray(ramSize)
    private var totalRamBanks = ramSize / 0x2000

    override fun get(address: UShort): UByte {
        return when (address) {
            in 0x0000u..0x3FFFu -> {

                        rom[address.toInt()]


            }
            in 0x4000u..0x7FFFu -> rom[(((romBankNumber * 0x4000u) + (address - 0x4000u)) and (rom.size - 1).toUInt()).toInt()]
            in 0xA000u..0xBFFFu -> {
                if (ramEnabled) {
                    ram[((ramBankNumber * 0x2000u) + (address - 0xA000u)).toInt()]

                }
                else 0xFFu
            }
            else -> throw IllegalArgumentException("Invalid MBC1 read address: $address")
        }
    }

    override fun set(address: UShort, value: UByte) {
        when (address) {
            in 0x0000u..0x1FFFu -> {
                // Enable external RAM if the lower nibble of the value being set is A
                ramEnabled = value and 0x0Fu == (0x0Au).toUByte()
            }
            in 0x2000u..0x3FFFu -> {
                val newValue = value and 0b0111_1111u
                val romBank = if(newValue.toUInt() == 0u) 1u else newValue
                romBankNumber = romBank

            }
            in 0x4000u..0x5FFFu -> {
                when(value) {
                    in 0x00u..0x03u -> {
                        ramBankNumber = value
                    }
                    in 0x08u..0x0Cu -> {
                        //TODO RTC
                    }
                }
            }
            in 0x6000u..0x7FFFu -> {
//                bankingMode = if (value.toUInt() and 1u == 0u) BankingMode.Rom else BankingMode.Ram
            }
            in 0xA000u..0xBFFFu -> {
                if (ramEnabled) {

                        ram[((ramBankNumber * 0x2000u) + (address - 0xA000u)).toInt()] = value

                }
            }
            else -> throw throw Exception("Invalid MBC1 write address: $address")
        }

    }
}