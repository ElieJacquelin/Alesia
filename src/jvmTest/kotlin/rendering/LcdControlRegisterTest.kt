package rendering

import Memory
import kotlin.test.*

@OptIn(ExperimentalUnsignedTypes::class)
internal class LcdControlRegisterTest {
    lateinit var memory: Memory
    lateinit var lcdControlRegister: LcdControlRegister

    @BeforeTest
    fun setUp() {
        memory = Memory()
        lcdControlRegister = LcdControlRegister(memory)
    }

    @Test
    fun `Get LCD enabled`() {
        // Given the bit for LCD is enabled
        memory.set(0xFF40u, 0b1100_0000u)

        // When getting the LCD bit
        val display = lcdControlRegister.getDisplay()

        // Display is enabled
        assertTrue(display)
    }

    @Test
    fun `Get LCD disabled`() {
        // Given the bit for LCD is disabled
        memory.set(0xFF40u, 0b0100_0000u)

        // When getting the LCD bit
        val display = lcdControlRegister.getDisplay()

        // Display is disabled
        assertFalse(display)
    }

    @Test
    fun `Set LCD`() {
        // Given the bit for LCD is disabled
        memory.set(0xFF40u, 0b0100_0000u)

        // When setting the LCD bit
        lcdControlRegister.setDisplay(true)

        // Display is enabled
        assertEquals(0b1100_0000u, memory.get(0xFF40u))
    }

    @Test
    fun `Get window tile map area 9C00`() {
        // Given the bit for window tile map is enabled (0x9c00)
        memory.set(0xFF40u, 0b1100_0000u)

        // When getting the window tile map area
        val value = lcdControlRegister.getWindowTileMap()

        // The map is set to 9C00
        assertTrue(value)
    }

    @Test
    fun `Get window tile map area 9800`() {
        // Given the bit for window tile map is disabled (0x9800)
        memory.set(0xFF40u, 0b1000_0000u)

        // When getting the window tile map area
        val value = lcdControlRegister.getWindowTileMap()

        // The map is set to 9800
        assertFalse(value)
    }

    @Test
    fun `Set window tile map area`() {
        // Given the bit for window tile map is disabled (0x9800)
        memory.set(0xFF40u, 0b1000_0000u)

        // When setting the window tile map area
        lcdControlRegister.setWindowTileMap(true)

        // The map is set to 9C00
        assertEquals(0b1100_0000u, memory.get(0xFF40u))
    }

    @Test
    fun `Get window enabled`() {
        // Given the bit for window is enabled
        memory.set(0xFF40u, 0b1010_0000u)

        // When getting the window enabled
        val value = lcdControlRegister.getWindowEnabled()

        // The window is enabled
        assertTrue(value)
    }

    @Test
    fun `Get window disabled`() {
        // Given the bit for window is disabled
        memory.set(0xFF40u, 0b1000_0000u)

        // When getting the window tile map area
        val value = lcdControlRegister.getWindowEnabled()

        // The window is disabled
        assertFalse(value)
    }

    @Test
    fun `Set window enabled`() {
        // Given the bit for window is disabled
        memory.set(0xFF40u, 0b1000_0000u)

        // When setting the window tile map area
        lcdControlRegister.setWindowEnabled(true)

        // The window is enabled
        assertEquals(0b1010_0000u, memory.get(0xFF40u))
    }

    @Test
    fun `Get background tile data area 8000`() {
        // Given the bit for background tile data is enabled (0x8000)
        memory.set(0xFF40u, 0b1001_0000u)

        // When getting the background tile data area
        val value = lcdControlRegister.getBgAndWindowTileDataAddressingMode()

        // The map is set to 8000
        assertTrue(value)
    }

    @Test
    fun `Get background tile data area 8800`() {
        // Given the bit for background tile data is disabled (0x8800)
        memory.set(0xFF40u, 0b1000_0000u)

        // When getting the background tile data area
        val value = lcdControlRegister.getBgAndWindowTileDataAddressingMode()

        // The map is set to 8800
        assertFalse(value)
    }

    @Test
    fun `Set background tile data area`() {
        // Given the bit for background tile data is disabled (0x8800)
        memory.set(0xFF40u, 0b1000_0000u)

        // When setting the background tile data area
        lcdControlRegister.setBgAndWindowTileDataAddressingMode(true)

        // The map is set to 8000
        assertEquals(0b1001_0000u, memory.get(0xFF40u))
    }

    @Test
    fun `Get background tile map area 9C00`() {
        // Given the bit for background tile map is enabled (0x9C00)
        memory.set(0xFF40u, 0b1000_1000u)

        // When getting the background tile map area
        val value = lcdControlRegister.getBgTileMap()

        // The map is set to 9C00
        assertTrue(value)
    }

    @Test
    fun `Get background tile map area 9800`() {
        // Given the bit for background tile map is disabled (0x9800)
        memory.set(0xFF40u, 0b1000_0000u)

        // When getting the background tile data area
        val value = lcdControlRegister.getBgTileMap()

        // The map is set to 9800
        assertFalse(value)
    }

    @Test
    fun `Set background tile map area`() {
        // Given the bit for background tile map is disabled (0x9800)
        memory.set(0xFF40u, 0b1000_0000u)

        // When setting the background tile data area
        lcdControlRegister.setBgTileMap(true)

        // The map is set to 9C00
        assertEquals(0b1000_1000u, memory.get(0xFF40u))
    }

    @Test
    fun `Get object size 16x`() {
        // Given the bit for object size is enabled (16x)
        memory.set(0xFF40u, 0b1000_0100u)

        // When getting the object size
        val value = lcdControlRegister.getSpriteSizeEnabled()

        // The size is set to 16x
        assertTrue(value)
    }

    @Test
    fun `Get object size 8x`() {
        // Given the bit for object size is disabled (8x)
        memory.set(0xFF40u, 0b1000_0000u)

        // When getting the object size
        val value = lcdControlRegister.getSpriteSizeEnabled()

        // The map is set to 8x
        assertFalse(value)
    }

    @Test
    fun `Set object size`() {
        // Given the bit for object size map is disabled (8x)
        memory.set(0xFF40u, 0b1000_0000u)

        // When setting the object size
        lcdControlRegister.setSpriteSizeEnabled(true)

        // The object size is set to 16x
        assertEquals(0b1000_0100u, memory.get(0xFF40u))
    }

    @Test
    fun `Get object enable`() {
        // Given the bit for object is enable
        memory.set(0xFF40u, 0b1000_0010u)

        // When getting the object enable
        val value = lcdControlRegister.getSpriteEnabled()

        // The object is enabled
        assertTrue(value)
    }

    @Test
    fun `Get object disable`() {
        // Given the bit for object is disabled
        memory.set(0xFF40u, 0b1000_0000u)

        // When getting the object enable
        val value = lcdControlRegister.getSpriteEnabled()

        // The object is disabled
        assertFalse(value)
    }

    @Test
    fun `Set object`() {
        // Given the bit for object is disabled
        memory.set(0xFF40u, 0b1000_0000u)

        // When setting the object enabled
        lcdControlRegister.setSpriteEnabled(true)

        // The object is enabled
        assertEquals(0b1000_0010u, memory.get(0xFF40u))
    }

    @Test
    fun `Get background priority enable`() {
        // Given the bit for background priority is enabled
        memory.set(0xFF40u, 0b1000_0001u)

        // When getting the background priority
        val value = lcdControlRegister.getBgAndWindowEnabled()

        // The background is enabled
        assertTrue(value)
    }

    @Test
    fun `Get background priority disable`() {
        // Given the bit for background priority is disabled
        memory.set(0xFF40u, 0b1000_0000u)

        // When getting the object enable
        val value = lcdControlRegister.getBgAndWindowEnabled()

        // The object is disabled
        assertFalse(value)
    }

    @Test
    fun `Set background priority`() {
        // Given the bit for background priority is disabled
        memory.set(0xFF40u, 0b1000_0000u)

        // When setting the object enabled
        lcdControlRegister.setBgAndWindowEnabled(true)

        // The background priority is enabled
        assertEquals(0b1000_0001u, memory.get(0xFF40u))
    }
}