package memory

import org.junit.Test
import kotlin.test.BeforeTest
import kotlin.test.assertEquals

@OptIn(ExperimentalUnsignedTypes::class)
class MBC1Test {
    lateinit var mbc: MBC1
    var rom: UByteArray = UByteArray(0x8000)

    @BeforeTest
    fun setUp() {
        mbc = MBC1(rom, 0x4000, true)
    }

    private fun setRomBankNumbers(bankNumber: Int) {
        rom = UByteArray(0x4000 * (bankNumber + 1))
        mbc = MBC1(rom, 0x4000, true)
    }

    private fun setValue(value: UByte, bankNumber: Int, relativeAddress: Int) {
        rom[relativeAddress +(bankNumber * 0x4000)] = value
    }

    private fun setBankingMode(bankingMode: MBC1.BankingMode) {
        when (bankingMode) {
            MBC1.BankingMode.Rom -> mbc.set(0x6000u, 0u)
            MBC1.BankingMode.Ram -> mbc.set(0x6000u, 1u)
        }
    }

    @Test
    fun `Read 0x3FFF - ROM mode`() {
        // Given the ROM has some value stored within 0x0000 and 0x3FFF
        rom[0x2000] = 0x12u
        // And the baking mode is set to ROM
        setBankingMode(MBC1.BankingMode.Rom)

        // When reading the value
        val result = mbc.get(0x2000u)

        // Then the stored value is returned
        assertEquals(0x12u, result)
    }

    @Test
    fun `Read 0x3FFF - RAM mode - High bits bank set to 0`() {
        // Given the ROM has some value stored within 0x0000 and 0x3FFF (bank 0)
        rom[0x2000] = 0x12u
        // And the baking mode is set to RAM
        setBankingMode(MBC1.BankingMode.Ram)
        // And upper bits set in the 0x4000-0x5FFF register is 0
        mbc.set(0x4000u, 0u)

        // When reading the value
        val result = mbc.get(0x2000u)

        // Then the stored value is returned from the bank 0
        assertEquals(0x12u, result)
    }

    @Test
    fun `Read 0x3FFF - RAM mode - High bits bank set to 1`() {
        // Given the ROM has enough banks to be indexed with bits 5 and 6
        setRomBankNumbers(97)
        // And the ROM has some value stored within bank 32 at relative address 0x1000
        setValue(0x12u, 32, 0x1000)
        // And the baking mode is set to RAM
        setBankingMode(MBC1.BankingMode.Ram)
        // And upper bits set in the 0x4000-0x5FFF register is 1 to select bank 32
        mbc.set(0x4000u, 1u)

        // When reading the value at address 0x1000
        val result = mbc.get(0x1000u)

        // Then the stored value is returned from the bank 32
        assertEquals(0x12u, result)
    }

    @Test
    fun `Read 0x4000-0x7FFF - bank 0`() {
        // Given the ROM has some value stored within bank 1 at relative address 0x1000
        setValue(0x12u, 1, 0x1000)
        // And the selected bank is set to 0
        mbc.set(0x2000u, 0u)

        // When reading the value at address 0x5000
        val result = mbc.get(0x5000u)

        // Then the stored value is returned from the bank 1 as bank 0 auto-default to 1
        assertEquals(0x12u, result)
    }

    @Test
    fun `Read 0x4000-0x7FFF - bank 1`() {
        // Given the ROM has some value stored within bank 1 at relative address 0x1000
        setValue(0x12u, 1, 0x1000)
        // And the selected bank is set to 1
        mbc.set(0x2000u, 1u)

        // When reading the value at address 0x5000
        val result = mbc.get(0x5000u)

        // Then the stored value is returned from the bank 1
        assertEquals(0x12u, result)
    }

    @Test
    fun `Read 0x4000-0x7FFF - bank 2`() {
        // Given the ROM has 3 banks
        setRomBankNumbers(3)
        // And the ROM has some value stored within bank 2 at relative address 0x1000
        setValue(0x12u, 2, 0x1000)
        // And the selected bank is set to 2
        mbc.set(0x2000u, 2u)

        // When reading the value at address 0x5000
        val result = mbc.get(0x5000u)

        // Then the stored value is returned from the bank 2
        assertEquals(0x12u, result)
    }

    @Test
    fun `Read 0x4000-0x7FFF - bank 43`() {
        // Given the ROM has 63 banks
        setRomBankNumbers(63)
        // And the ROM has some value stored within bank 43 at relative address 0x1000
        setValue(0x12u, 43, 0x1000)
        // And the selected bank is set to 43
        mbc.set(0x2000u, 0b1011u)
        mbc.set(0x4000u, 0b1u)

        // When reading the value at address 0x5000
        val result = mbc.get(0x5000u)

        // Then the stored value is returned from the bank 43
        assertEquals(0x12u, result)
    }

    @Test
    fun `Read 0xA000-0xBFFF - Ram disabled`() {
        // Given the RAM is disabled
        mbc.set(0x0000u, 0x00u)

        // When reading the value at address 0xA000
        val result = mbc.get(0xA000u)

        // Then the value is 0xFF as the ram is disabled
        assertEquals(0xFFu, result)
    }

    @Test
    fun `Read 0xA000-0xBFFF - Ram Enabled - no value`() {
        // Given the RAM is enabled
        mbc.set(0x0000u, 0x0Au)

        // When reading the value at address 0xA000
        val result = mbc.get(0xA000u)

        // Then the value is 0x00 as the ram has no value
        assertEquals(0x00u, result)
    }

    @Test
    fun `Read 0xA000-0xBFFF - Ram Enabled - ROM mode`() {
        // Given the RAM is enabled
        mbc.set(0x0000u, 0x0Au)
        // and the banking mode is set to ROM
        setBankingMode(MBC1.BankingMode.Rom)
        // and some value is stored within 0xA000-0xBFFF
        mbc.set(0xB000u, 0x12u)

        // When reading the value at the address
        val result = mbc.get(0xB000u)

        // Then the value is the same as the stored
        assertEquals(0x12u, result)
    }

    @Test
    fun `Read 0xA000-0xBFFF - Ram Enabled - RAM mode - Bank 0`() {
        // Given the RAM is enabled
        mbc.set(0x0000u, 0x0Au)
        // and the banking mode is set to RAM
        setBankingMode(MBC1.BankingMode.Ram)
        // and the ram bank selected is 0
        mbc.set(0x4000u, 0u)
        // and some value is stored within 0xA000-0xBFFF
        mbc.set(0xB000u, 0x12u)

        // When reading the value at the address
        val result = mbc.get(0xB000u)

        // Then the value is the same as the stored
        assertEquals(0x12u, result)
    }

    @Test
    fun `Read 0xA000-0xBFFF - Ram Enabled - RAM mode - Bank 1`() {
        // Given the RAM is enabled
        mbc.set(0x0000u, 0x0Au)
        // and the banking mode is set to RAM
        setBankingMode(MBC1.BankingMode.Ram)
        // and the ram bank selected is 1
        mbc.set(0x4000u, 1u)
        // and some value is stored within 0xA000-0xBFFF
        mbc.set(0xB000u, 0x12u)

        // When reading the value at the address
        val result = mbc.get(0xB000u)

        // Then the value is the same as the stored
        assertEquals(0x12u, result)
    }

    @Test
    fun `Read 0xA000-0xBFFF - Ram Enabled - RAM mode - Switch bank`() {
        // Given the RAM is enabled
        mbc.set(0x0000u, 0x0Au)
        // and the banking mode is set to RAM
        setBankingMode(MBC1.BankingMode.Ram)
        // and the ram bank selected is 0
        mbc.set(0x4000u, 0u)
        // and some value is stored within 0xA000-0xBFFF
        mbc.set(0xB000u, 0x12u)

        // When switching to bank 1
        mbc.set(0x4000u, 1u)
        // and reading the value at the address
        val resultBank1 = mbc.get(0xB000u)

        // Then the value is 0 as there is no value stored in that bank
        assertEquals(0x00u, resultBank1)

        // When switching back to bank 0
        mbc.set(0x4000u, 0u)
        // and reading the value at the address
        val resultBank0 = mbc.get(0xB000u)

        // Then the value is the value stored in bank0
        assertEquals(0x12u, resultBank0)
    }

    @Test
    fun `Read 0xA000-0xBFFF - Ram Enabled - RAM mode - Bank too high`() {
        // Given 2 banks of RAM is available
        // and the RAM is enabled
        mbc.set(0x0000u, 0x0Au)
        // and the banking mode is set to RAM
        setBankingMode(MBC1.BankingMode.Ram)
        // and the ram bank selected is 1
        mbc.set(0x4000u, 1u)
        // and some value is stored within 0xA000-0xBFFF
        mbc.set(0xB000u, 0x12u)

        // When switching to a bank higher than what is available
        mbc.set(0x4000u, 3u)
        // and reading the value at the address
        val result = mbc.get(0xB000u)

        // Then the bank switch is ignored, so we stay on bank 1
        assertEquals(0x12u, result)
    }
}