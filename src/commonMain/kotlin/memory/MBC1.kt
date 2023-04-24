package memory

@OptIn(ExperimentalUnsignedTypes::class)
class MBC1(private val rom: UByteArray, ramSize: Int, override val hasBattery: Boolean): MBC {

    private var ramEnabled: Boolean = false
    private var romBankNumber: UByte = 1u
    private var ramBankNumber: UByte = 0u
    private var bankingMode: BankingMode = BankingMode.Rom

    override var ram = UByteArray(ramSize)
    private var totalRamBanks = ramSize / 0x2000

    override fun get(address: UShort): UByte {
        return when (address) {
            in 0x0000u..0x3FFFu -> {
                when(bankingMode) {
                    BankingMode.Rom -> rom[address.toInt()]
                    BankingMode.Ram -> {
                        val newRomBankNumber = romBankNumber and 0b110_0000u // Only use the upper bits set in the 0x4000-0x5FFF register
                        rom[(((newRomBankNumber * 0x4000u) + address) and (rom.size - 1).toUInt()).toInt()]
                    }
                }
            }
            in 0x4000u..0x7FFFu -> rom[(((romBankNumber * 0x4000u) + (address - 0x4000u)) and (rom.size - 1).toUInt()).toInt()]
            in 0xA000u..0xBFFFu -> {
                if (ramEnabled) {
                    when(bankingMode) {
                        BankingMode.Rom -> ram[(address - 0xA000u).toInt()]
                        BankingMode.Ram -> ram[((ramBankNumber * 0x2000u) + (address - 0xA000u)).toInt()]
                    }
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
                val bitMask = 0b11111u.toUByte()
                var selectedBank = value and bitMask
                if(selectedBank == 0u.toUByte()) {
                    selectedBank = 1u
                }

                romBankNumber = selectedBank

            }
            in 0x4000u..0x5FFFu -> {
                // The value represent the bit 5 and 6 of the bank index with the first 4 bits being set with the above register
                val currentRomBankNumber = romBankNumber
                romBankNumber = currentRomBankNumber or ((value and 0b11u).toUInt() shl 5).toUByte()

                val newRamBankNumber = value and 0b11u
                if(newRamBankNumber.toInt() < totalRamBanks) {
                    ramBankNumber = newRamBankNumber
                }
            }
            in 0x6000u..0x7FFFu -> {
                bankingMode = if (value.toUInt() and 1u == 0u) BankingMode.Rom else BankingMode.Ram
            }
            in 0xA000u..0xBFFFu -> {
                if (ramEnabled) {
                    when(bankingMode) {
                        BankingMode.Rom -> ram[(address - 0xA000u).toInt()] = value
                        BankingMode.Ram -> ram[((ramBankNumber * 0x2000u) + (address - 0xA000u)).toInt()] = value
                    }
                }
            }
            else -> throw throw Exception("Invalid MBC1 write address: $address")
        }

    }

    enum class BankingMode {
        Rom, Ram
    }
}