package rendering

import memory.Memory
import Pixel
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@ExperimentalStdlibApi
@ExperimentalUnsignedTypes

class PixelFetcherTest {

    lateinit var pixelFetcher: PixelFetcher
    lateinit var lcdControlRegister: LcdControlRegister
    lateinit var memory: Memory

    @BeforeTest
    fun init() {
        memory = Memory()
        lcdControlRegister = LcdControlRegister(memory)
        pixelFetcher = PixelFetcher(lcdControlRegister, memory)
    }

    private fun buildSharedState(currentTileMapOffset: UInt = 0u, currentLine: Int = 0, backgroundFifo: ArrayDeque<Pixel> = ArrayDeque()): PixelFetcher.SharedState {
        return PixelFetcher.SharedState(currentTileMapOffset, currentLine, backgroundFifo)
    }

    @Test
    fun `Get tile step - first dot`() {
        // GIVEN the PixelFetcher is on the get tile step on the first dot
        val sharedState = buildSharedState()
        pixelFetcher.state = PixelFetcher.State.GetTile(sharedState,0)

        // WHEN the machine ticks
        pixelFetcher.tick()

        // THEN the state remains on GetTile but with the dot count increased
        assertEquals(PixelFetcher.State.GetTile(sharedState, 1), pixelFetcher.state)
    }

    @Test
    fun `Get tile step - 9C00 background`() {
        // GIVEN the PixelFetcher is on the get tile step on the second dot
        val sharedState = buildSharedState()
        pixelFetcher.state = PixelFetcher.State.GetTile(sharedState, 1)
        // AND the BG tile map is set to 0x9C00
        lcdControlRegister.setBgTileMap(true)
        // AND the tile ID to be fetched is set to 2
        memory.set(0x9C00u, 0x02u)

        // WHEN the machine ticks
        pixelFetcher.tick()

        // THEN the new state is the get tile low data step for the correct tile ID
        assertEquals(PixelFetcher.State.GetTileDataLow(sharedState, 0x02u, 0), pixelFetcher.state)
    }

    @Test
    fun `Get tile step - 9800 background`() {
        // GIVEN the PixelFetcher is on the get tile step on the second dot
        val sharedState = buildSharedState()
        pixelFetcher.state = PixelFetcher.State.GetTile(sharedState, 1)
        // AND the BG tile map is set to 0x9800
        lcdControlRegister.setBgTileMap(false)
        // AND the tile ID to be fetched is set to 2
        memory.set(0x9800u, 0x02u)

        // WHEN the machine ticks
        pixelFetcher.tick()

        // THEN the new state is the get tile low data step for the correct tile ID
        assertEquals(PixelFetcher.State.GetTileDataLow(sharedState,0x02u, 0), pixelFetcher.state)
    }

    @Test
    fun `Get tile step - Line 1 - Tile on same row`() {
        // GIVEN the PixelFetcher is on the get tile step on the second dot for the second line
        val sharedState = buildSharedState(currentLine = 1)
        pixelFetcher.state = PixelFetcher.State.GetTile(sharedState, 1)
        // AND the BG tile map is set to 0x9800
        lcdControlRegister.setBgTileMap(false)
        // AND the tile ID to be fetched is set to 2
        memory.set(0x9800u, 0x02u)

        // WHEN the machine ticks
        pixelFetcher.tick()

        // THEN the new state is the get tile low data step for the correct tile ID which is the same tile as the first line
        assertEquals(PixelFetcher.State.GetTileDataLow(sharedState,0x02u, 0), pixelFetcher.state)
    }

    @Test
    fun `Get tile step - Line 8 - Tile on next row`() {
        // GIVEN the PixelFetcher is on the get tile step on the second dot for the 8th line
        val sharedState = buildSharedState(currentLine = 8)
        pixelFetcher.state = PixelFetcher.State.GetTile(sharedState, 1)
        // AND the BG tile map is set to 0x9800
        lcdControlRegister.setBgTileMap(false)
        // AND the tile ID to be fetched is set to 2 (0x9800 + 0x20 * (currentLine / 8))
        memory.set(0x9820u, 0x02u)

        // WHEN the machine ticks
        pixelFetcher.tick()

        // THEN the new state is the get tile low data step for the correct tile ID which is on the next row
        assertEquals(PixelFetcher.State.GetTileDataLow(sharedState,0x02u, 0), pixelFetcher.state)
    }

    @Test
    fun `Get tile step - Line 8 - Scroll X and Y`() {
        // GIVEN the PixelFetcher is on the get tile step on the second dot for the 8th line
        val sharedState = buildSharedState(currentLine = 8)
        pixelFetcher.state = PixelFetcher.State.GetTile(sharedState, 1)
        // AND the BG tile map is set to 0x9800
        lcdControlRegister.setBgTileMap(false)
        // AND the Scroll X is set to 16 (2 tiles)
        memory.set(0xFF43u, 16u)
        // AND the Scroll Y is set to 24 (3 tiles)
        memory.set(0xFF42u, 24u)
        // AND the tile ID to be fetched is set to 2 (0x9800 + 2 (SCX/8) + 0x20 * ((currentLine / 8) + 3 (SCY/8))
        memory.set(0x9882u, 0x02u)

        // WHEN the machine ticks
        pixelFetcher.tick()

        // THEN the new state is the get tile low data step for the correct tile ID which is on the next row
        assertEquals(PixelFetcher.State.GetTileDataLow(sharedState,0x02u, 0), pixelFetcher.state)
    }

    @Test
    fun `Get tile step - Line 8 - Scroll X and Y- Wrap around`() {
        // GIVEN the PixelFetcher is on the get tile step on the second dot for the 16th line and 4th tile
        val sharedState = buildSharedState(currentLine = 16, currentTileMapOffset = 4u)
        pixelFetcher.state = PixelFetcher.State.GetTile(sharedState, 1)
        // AND the BG tile map is set to 0x9800
        lcdControlRegister.setBgTileMap(false)
        // AND the Scroll X is set to 240 (30 tiles)
        memory.set(0xFF43u, 240u)
        // AND the Scroll Y is set to 248 (31 tiles)
        memory.set(0xFF42u, 248u)
        // AND the tile ID to be fetched is set to 2 (0x9800 + 2 (SCX/8 % 32) + 0x20 * ((currentLine / 8) + 31 (SCY/8) % 32)
        memory.set(0x9822u, 0x02u)

        // WHEN the machine ticks
        pixelFetcher.tick()

        // THEN the new state is the get tile low data step for the correct tile ID which is on the next row
        assertEquals(PixelFetcher.State.GetTileDataLow(sharedState,0x02u, 0), pixelFetcher.state)
    }

    @Test
    fun `Get tile step - Before Window`() {
        // GIVEN the PixelFetcher is on the get tile step on the second dot for the 8th line
        val sharedState = buildSharedState(currentLine = 8)
        pixelFetcher.state = PixelFetcher.State.GetTile(sharedState, 1)
        // AND the window is enabled
        lcdControlRegister.setWindowEnabled(true)
        // AND the windowX is set to 11
        val windowX = memory.set(0xFF4Bu, 11u)
        // AND the window Y is set to 16
        val windowY = memory.set(0xFF4Au, 16u)
        // AND the BG tile map is set to 0x9800
        lcdControlRegister.setWindowTileMap(false)
        // AND the tile ID to be fetched is set to 2 (0x9800 + 0x20 * (currentLine / 8))
        memory.set(0x9820u, 0x02u)
        // AND the tile ID of the window to be fetched is set to 3 (0x9800 + (WinX - 7) + 0x20 * ((currentLine - WinY) / 8)
        memory.set(0x9800u, 0x03u)

        // WHEN the machine ticks
        pixelFetcher.tick()

        // THEN the new state is the get tile low data step for the correct tile ID which is on from the background
        assertEquals(PixelFetcher.State.GetTileDataLow(sharedState,0x02u, 0), pixelFetcher.state)
    }

    @Test
    fun `Get tile step - Window`() {
        // GIVEN the PixelFetcher is on the get tile step on the second dot for the 16th line and 4th tile
        val sharedState = buildSharedState(currentLine = 16, currentTileMapOffset = 4u)
        pixelFetcher.state = PixelFetcher.State.GetTile(sharedState, 1)
        // AND the window is enabled
        lcdControlRegister.setWindowEnabled(true)
        // AND the BG tile map is set to 0x9800
        lcdControlRegister.setWindowTileMap(false)
        // AND the windowX is set to 11
        val windowX = memory.set(0xFF4Bu, 11u)
        // AND the window Y is set to 16
        val windowY = memory.set(0xFF4Au, 16u)
        // AND the window tile ID to be fetched is set to 2 (0x9800 + (WinX - 7) + 0x20 * ((currentLine - WinY) / 8)
        memory.set(0x9800u, 0x02u)

        // WHEN the machine ticks
        pixelFetcher.tick()

        // THEN the new state is the get tile low data step for the correct tile ID from the Window
        assertEquals(PixelFetcher.State.GetTileDataLow(sharedState,0x02u, 0), pixelFetcher.state)
    }

    @Test
    fun `Get tile step - After Window`() {
        // GIVEN the PixelFetcher is on the get tile step on the second dot for the 17th line and 4th tile
        val sharedState = buildSharedState(currentLine = 16, currentTileMapOffset = 4u)
        pixelFetcher.state = PixelFetcher.State.GetTile(sharedState, 1)
        // AND the window is enabled
        lcdControlRegister.setWindowEnabled(true)
        // AND the BG tile map is set to 0x9800
        lcdControlRegister.setWindowTileMap(false)
        // AND the windowX is set to 11
        val windowX = memory.set(0xFF4Bu, 11u)
        // AND the window Y is set to 16
        val windowY = memory.set(0xFF4Au, 16u)
        // AND the window tile ID to be fetched is set to 2 (0x9800 + (WinX - 7) + 0x20 * ((currentLine - WinY) / 8)
        memory.set(0x9800u, 0x02u)

        // WHEN the machine ticks
        pixelFetcher.tick()

        // THEN the new state is the get tile low data step for the correct tile ID from the Window
        assertEquals(PixelFetcher.State.GetTileDataLow(sharedState,0x02u, 0), pixelFetcher.state)
    }

    @Test
    fun `Get tile step - Window disabled`() {
        // GIVEN the PixelFetcher is on the get tile step on the second dot for the 16th line and 4th tile
        val sharedState = buildSharedState(currentLine = 16, currentTileMapOffset = 4u)
        pixelFetcher.state = PixelFetcher.State.GetTile(sharedState, 1)
        // AND the window is disabled
        lcdControlRegister.setWindowEnabled(false)
        // AND the BG tile map is set to 0x9800
        lcdControlRegister.setWindowTileMap(false)
        // AND the windowX is set to 11
        val windowX = memory.set(0xFF4Bu, 11u)
        // AND the window Y is set to 16
        val windowY = memory.set(0xFF4Au, 16u)
        // AND the window tile ID to be fetched is set to 2 (0x9800 + (WinX - 7) + 0x20 * ((currentLine - WinY) / 8)
        memory.set(0x9800u, 0x02u)

        // WHEN the machine ticks
        pixelFetcher.tick()

        // THEN the new state is the get tile low data step for the tile that is not the window
        assertNotEquals(PixelFetcher.State.GetTileDataLow(sharedState,0x02u, 0), pixelFetcher.state)
    }

    @Test
    fun `Get tile Data low step - first dot`() {
        // GIVEN the PixelFetcher is on the tile data low step on the first dot
        val sharedState = buildSharedState()
        pixelFetcher.state = PixelFetcher.State.GetTileDataLow(sharedState,  0x02u, 0)

        // WHEN the machine ticks
        pixelFetcher.tick()

        // THEN the state stays on Get Tile Data Low but with the dot count increased
        assertEquals(PixelFetcher.State.GetTileDataLow(sharedState,0x02u, 1), pixelFetcher.state)
    }

    @Test
    fun `Get tile Data low step - $8000 mode`() {
        // GIVEN the addressing is using the $8000 mode
        lcdControlRegister.setBgAndWindowTileDataAddressingMode(true)
        // AND the first line is being drawn
        val sharedState = buildSharedState(currentLine = 0)
        // AND the tile data is set
        memory.set(0x8020u, 0x01u)
        // AND the PixelFetcher is on the tile data low step on the second dot
        pixelFetcher.state = PixelFetcher.State.GetTileDataLow(sharedState, 0x02u, 1)

        // WHEN the machine ticks
        pixelFetcher.tick()

        // THEN the new state is the get tile high data step with the expected low data
        assertEquals(PixelFetcher.State.GetTileDataHigh(sharedState, 0x02u, 0x01u, 0), pixelFetcher.state)
    }

    @Test
    fun `Get tile Data low step - $8000 mode - Line 1`() {
        // GIVEN the addressing is using the $8000 mode
        lcdControlRegister.setBgAndWindowTileDataAddressingMode(true)
        // AND the second line is being drawn
        val sharedState = buildSharedState(currentLine = 1)
        // AND the tile data is set for the second line (1 line = 2 bytes)
        memory.set(0x8022u, 0x01u)
        // AND the PixelFetcher is on the tile data low step on the second dot
        pixelFetcher.state = PixelFetcher.State.GetTileDataLow(sharedState, 0x02u, 1)

        // WHEN the machine ticks
        pixelFetcher.tick()

        // THEN the new state is the get tile high data step with the expected low data
        assertEquals(PixelFetcher.State.GetTileDataHigh(sharedState, 0x02u, 0x01u, 0), pixelFetcher.state)
    }

    @Test
    fun `Get tile Data low step - $8000 mode - Line 7`() {
        // GIVEN the addressing is using the $8000 mode
        lcdControlRegister.setBgAndWindowTileDataAddressingMode(true)
        // AND the eight line is being drawn
        val sharedState = buildSharedState(currentLine = 7)
        // AND the tile data is set for the eight line
        memory.set(0x802Eu, 0x01u)
        // AND the PixelFetcher is on the tile data low step on the second dot
        pixelFetcher.state = PixelFetcher.State.GetTileDataLow(sharedState, 0x02u, 1)

        // WHEN the machine ticks
        pixelFetcher.tick()

        // THEN the new state is the get tile high data step with the expected low data
        assertEquals(PixelFetcher.State.GetTileDataHigh(sharedState, 0x02u, 0x01u, 0), pixelFetcher.state)
    }

    @Test
    fun `Get tile Data low step - $8000 mode - Line 8`() {
        // GIVEN the addressing is using the $8000 mode
        lcdControlRegister.setBgAndWindowTileDataAddressingMode(true)
        // AND the ninth line is being drawn
        val sharedState = buildSharedState(currentLine = 8)
        // AND the tile data is set for the first line
        memory.set(0x8020u, 0x01u)
        // AND the PixelFetcher is on the tile data low step on the second dot
        pixelFetcher.state = PixelFetcher.State.GetTileDataLow(sharedState, 0x02u, 1)

        // WHEN the machine ticks
        pixelFetcher.tick()

        // THEN the new state is the get tile high data step with the expected low data
        assertEquals(PixelFetcher.State.GetTileDataHigh(sharedState, 0x02u, 0x01u, 0), pixelFetcher.state)
    }

    @Test
    fun `Get tile Data low step - $8800 mode - positive`() {
        // GIVEN the addressing is using the $8800 mode
        lcdControlRegister.setBgAndWindowTileDataAddressingMode(false)
        // AND the first line is being drawn
        val sharedState = buildSharedState(currentLine = 0)
        // AND the tile data is set
        memory.set(0x9020u, 0x01u)
        // AND the PixelFetcher is on the tile data low step on the second dot
        pixelFetcher.state = PixelFetcher.State.GetTileDataLow(sharedState, 0x02u, 1)

        // WHEN the machine ticks
        pixelFetcher.tick()

        // THEN the new state is the get tile high data step with the expected low data
        assertEquals(PixelFetcher.State.GetTileDataHigh(sharedState, 0x02u, 0x01u, 0), pixelFetcher.state)
    }

    @Test
    fun `Get tile Data low step - $8800 mode - negative`() {
        // GIVEN the addressing is using the $8800 mode
        lcdControlRegister.setBgAndWindowTileDataAddressingMode(false)
        // AND the first line is being drawn
        val sharedState = buildSharedState(currentLine = 0)
        // AND the tile data is set
        memory.set(0x8810u, 0x01u)
        // AND the PixelFetcher is on the tile data low step on the second dot
        pixelFetcher.state = PixelFetcher.State.GetTileDataLow(sharedState, 0x81u, 1) // index 129

        // WHEN the machine ticks
        pixelFetcher.tick()

        // THEN the new state is the get tile high data step with the expected low data
        assertEquals(PixelFetcher.State.GetTileDataHigh(sharedState, 0x81u, 0x01u, 0), pixelFetcher.state)
    }

    @Test
    fun `Get tile Data low step - $8800 mode - negative - low boundary`() {
        // GIVEN the addressing is using the $8800 mode
        lcdControlRegister.setBgAndWindowTileDataAddressingMode(false)
        // AND the first line is being drawn
        val sharedState = buildSharedState(currentLine = 0)
        // AND the tile data is set
        memory.set(0x8800u, 0x01u)
        // GIVEN the PixelFetcher is on the tile data low step on the second dot
        pixelFetcher.state = PixelFetcher.State.GetTileDataLow(sharedState, 0x80u, 1) // index 128

        // WHEN the machine ticks
        pixelFetcher.tick()

        // THEN the new state is the get tile high data step with the expected low data
        assertEquals(PixelFetcher.State.GetTileDataHigh(sharedState, 0x80u, 0x01u, 0), pixelFetcher.state)
    }

    @Test
    fun `Get tile Data low step - $8800 mode - negative - high boundary`() {
        // GIVEN the addressing is using the $8800 mode
        lcdControlRegister.setBgAndWindowTileDataAddressingMode(false)
        // AND the first line is being drawn
        val sharedState = buildSharedState(currentLine = 0)
        // AND the tile data is set
        memory.set(0x8FF0u, 0x01u)
        // AND the PixelFetcher is on the tile data low step on the second dot
        val newState = PixelFetcher.State.GetTileDataLow(sharedState, 0xFFu, 1) // index 255
        pixelFetcher.state = newState

        // WHEN the machine ticks
        pixelFetcher.tick()

        // THEN the new state is the get tile high data step with the expected low data
        assertEquals(PixelFetcher.State.GetTileDataHigh(sharedState, 0xFFu, 0x01u, 0), pixelFetcher.state)
    }

    @Test
    fun `Get tile Data High step - first dot`() {
        // GIVEN the PixelFetcher is on the tile data high step on the first dot
        pixelFetcher.state = PixelFetcher.State.GetTileDataHigh(buildSharedState() ,0x02u, 0x03u, 0)

        // WHEN the machine ticks
        pixelFetcher.tick()

        // THEN the new state remains on Get Tile Data High but the dot count is incremented
        assertEquals(PixelFetcher.State.GetTileDataHigh(buildSharedState(),0x02u, 0x03u, 1), pixelFetcher.state)
    }

    @Test
    fun `Get tile Data High step - $8000 mode`() {
        // GIVEN the addressing is using the $8000 mode
        lcdControlRegister.setBgAndWindowTileDataAddressingMode(true)
        // AND the first line is being drawn
        val sharedState = buildSharedState(currentLine = 0)
        // AND the tile data is set
        memory.set(0x8021u, 0x01u)
        // AND the Fifo is not empty
        sharedState.backgroundFiFo.add(Pixel(ColorID.ZERO, 0 , false))
        // AND the PixelFetcher is on the tile data high step on the second dot
        pixelFetcher.state = PixelFetcher.State.GetTileDataHigh(sharedState, 0x02u, 0x03u, 1)

        // WHEN the machine ticks
        pixelFetcher.tick()

        // THEN the new state is the get push high data with the expected parameters
        assertEquals(PixelFetcher.State.Push(sharedState,0x03u, 0x01u), pixelFetcher.state)
    }

    @Test
    fun `Get push data step - Fifo not empty`() {
        // GIVEN the PixelFetcher is on push data step
        val sharedState = buildSharedState()
        pixelFetcher.state = PixelFetcher.State.Push(sharedState,0x02u, 0x03u)
        // AND the Fifo is not empty
        sharedState.backgroundFiFo.add(Pixel(ColorID.ZERO, 0 , false))

        // WHEN the machine ticks
        pixelFetcher.tick()

        // THEN the state stays the same
        assertEquals(PixelFetcher.State.Push(sharedState, 0x02u, 0x03u), pixelFetcher.state)
    }

    @Test
    fun `Get push data step - Fifo empty`() {
        // GIVEN the PixelFetcher is on push data step
        // low: 0000 0110 / high: 0000 0011 gives 0000 0132 Pixel colors
        val sharedState = buildSharedState()
        pixelFetcher.state = PixelFetcher.State.Push(sharedState, 0x06u, 0x03u)
        // AND the Fifo is empty
        val backgroundFifo = sharedState.backgroundFiFo
        backgroundFifo.clear()

        // WHEN the machine ticks
        pixelFetcher.tick()

        // THEN the pixels are pushed to the background Fifo in the expected order
        assertEquals(Pixel(ColorID.ZERO, 0, false), backgroundFifo.removeFirst())
        assertEquals(Pixel(ColorID.ZERO, 0, false), backgroundFifo.removeFirst())
        assertEquals(Pixel(ColorID.ZERO, 0, false), backgroundFifo.removeFirst())
        assertEquals(Pixel(ColorID.ZERO, 0, false), backgroundFifo.removeFirst())
        assertEquals(Pixel(ColorID.ZERO, 0, false), backgroundFifo.removeFirst())
        assertEquals(Pixel(ColorID.ONE, 0, false), backgroundFifo.removeFirst())
        assertEquals(Pixel(ColorID.THREE, 0, false), backgroundFifo.removeFirst())
        assertEquals(Pixel(ColorID.TWO, 0, false), backgroundFifo.removeFirst())
        assertEquals(0, backgroundFifo.size)

        // AND the state is set to get Tile for the next tile
        val expectedSharedState = sharedState.copy(currentTileMapOffset = 1u)
        assertEquals(PixelFetcher.State.GetTile(expectedSharedState), pixelFetcher.state)
    }

    @Test
    fun `reset() reset the state to GetTile`() {
        // GIVEN the state is on any state
        val sharedState = buildSharedState(1u, 2, ArrayDeque())
        pixelFetcher.state = PixelFetcher.State.Push(sharedState, 0x01u, 0x02u)

        // WHEN the reset function is called
        val newLine = 12
        val newBackgroundFifo = ArrayDeque<Pixel>()
        pixelFetcher.reset(newLine, newBackgroundFifo)

        // THEN the state is set to GetTile for the new line
        val expectedSharedState = buildSharedState(0u, newLine, newBackgroundFifo)
        assertEquals(PixelFetcher.State.GetTile(expectedSharedState, 0), pixelFetcher.state)
    }

}