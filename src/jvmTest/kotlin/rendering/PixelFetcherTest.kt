package rendering

import LcdControlRegister
import Memory
import Pixel
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@ExperimentalStdlibApi
@ExperimentalUnsignedTypes

class PixelFetcherTest {

    lateinit var pixelFetcher: PixelFetcher
    lateinit var lcdControlRegister: LcdControlRegister
    lateinit var memory: Memory
    lateinit var backgroundFifo: ArrayDeque<Pixel>

    @BeforeTest
    fun init() {
        lcdControlRegister = LcdControlRegister()
        memory = Memory()
        backgroundFifo = ArrayDeque()
        pixelFetcher = PixelFetcher(lcdControlRegister, memory, backgroundFifo, 0)
    }

    @Test
    fun `Get tile step - 9C00 background`() {
        // GIVEN the PixelFetcher is on the get tile step
        pixelFetcher.state = PixelFetcher.State.GetTile
        // AND the BG tile map is set to 0x9C00
        lcdControlRegister.setBgTileMap(true)
        // AND the tile ID to be fetched is set to 2
        memory.set(0x9C00u, 0x02u)

        // WHEN the machine ticks
        pixelFetcher.tick()

        // THEN the new state is the get tile low data step for the correct tile ID
        assertEquals(PixelFetcher.State.GetTileDataLow(0x02u), pixelFetcher.state)
    }

    @Test
    fun `Get tile step - 9800 background`() {
        // GIVEN the PixelFetcher is on the get tile step
        pixelFetcher.state = PixelFetcher.State.GetTile
        // AND the BG tile map is set to 0x9800
        lcdControlRegister.setBgTileMap(false)
        // AND the tile ID to be fetched is set to 2
        memory.set(0x9800u, 0x02u)

        // WHEN the machine ticks
        pixelFetcher.tick()

        // THEN the new state is the get tile low data step for the correct tile ID
        assertEquals(PixelFetcher.State.GetTileDataLow(0x02u), pixelFetcher.state)
    }

    @Test
    fun `Get tile Data low step - $8000 mode`() {
        // GIVEN the PixelFetcher is on the tile data low step
        pixelFetcher.state = PixelFetcher.State.GetTileDataLow(0x02u)
        // AND the addressing is using the $8000 mode
        lcdControlRegister.setBgAndWindowTileDataAddressingMode(true)
        // AND the first line is being drawn
        pixelFetcher.LY = 0
        // AND the tile data is set
        memory.set(0x8020u, 0x01u)

        // WHEN the machine ticks
        pixelFetcher.tick()

        // THEN the new state is the get tile high data step with the expected low data
        assertEquals(PixelFetcher.State.GetTileDataHigh(0x02u, 0x01u), pixelFetcher.state)
    }

    @Test
    fun `Get tile Data low step - $8000 mode - Line 1`() {
        // GIVEN the PixelFetcher is on the tile data low step
        pixelFetcher.state = PixelFetcher.State.GetTileDataLow(0x02u)
        // AND the addressing is using the $8000 mode
        lcdControlRegister.setBgAndWindowTileDataAddressingMode(true)
        // AND the second line is being drawn
        pixelFetcher.LY = 1
        // AND the tile data is set for the second line (1 line = 2 bytes)
        memory.set(0x8022u, 0x01u)

        // WHEN the machine ticks
        pixelFetcher.tick()

        // THEN the new state is the get tile high data step with the expected low data
        assertEquals(PixelFetcher.State.GetTileDataHigh(0x02u, 0x01u), pixelFetcher.state)
    }

    @Test
    fun `Get tile Data low step - $8000 mode - Line 7`() {
        // GIVEN the PixelFetcher is on the tile data low step
        pixelFetcher.state = PixelFetcher.State.GetTileDataLow(0x02u)
        // AND the addressing is using the $8000 mode
        lcdControlRegister.setBgAndWindowTileDataAddressingMode(true)
        // AND the eight line is being drawn
        pixelFetcher.LY = 7
        // AND the tile data is set for the eight line
        memory.set(0x802Eu, 0x01u)

        // WHEN the machine ticks
        pixelFetcher.tick()

        // THEN the new state is the get tile high data step with the expected low data
        assertEquals(PixelFetcher.State.GetTileDataHigh(0x02u, 0x01u), pixelFetcher.state)
    }

    @Test
    fun `Get tile Data low step - $8000 mode - Line 8`() {
        // GIVEN the PixelFetcher is on the tile data low step
        pixelFetcher.state = PixelFetcher.State.GetTileDataLow(0x02u)
        // AND the addressing is using the $8000 mode
        lcdControlRegister.setBgAndWindowTileDataAddressingMode(true)
        // AND the ninth line is being drawn
        pixelFetcher.LY = 8
        // AND the tile data is set for the first line
        memory.set(0x8020u, 0x01u)

        // WHEN the machine ticks
        pixelFetcher.tick()

        // THEN the new state is the get tile high data step with the expected low data
        assertEquals(PixelFetcher.State.GetTileDataHigh(0x02u, 0x01u), pixelFetcher.state)
    }

    @Test
    fun `Get tile Data low step - $8800 mode - positive`() {
        // GIVEN the PixelFetcher is on the tile data low step
        pixelFetcher.state = PixelFetcher.State.GetTileDataLow(0x02u)
        // AND the addressing is using the $8800 mode
        lcdControlRegister.setBgAndWindowTileDataAddressingMode(false)
        // AND the first line is being drawn
        pixelFetcher.LY = 0
        // AND the tile data is set
        memory.set(0x9020u, 0x01u)

        // WHEN the machine ticks
        pixelFetcher.tick()

        // THEN the new state is the get tile high data step with the expected low data
        assertEquals(PixelFetcher.State.GetTileDataHigh(0x02u, 0x01u), pixelFetcher.state)
    }

    @Test
    fun `Get tile Data low step - $8800 mode - negative`() {
        // GIVEN the PixelFetcher is on the tile data low step
        pixelFetcher.state = PixelFetcher.State.GetTileDataLow(0x81u) // index 129
        // AND the addressing is using the $8800 mode
        lcdControlRegister.setBgAndWindowTileDataAddressingMode(false)
        // AND the first line is being drawn
        pixelFetcher.LY = 0
        // AND the tile data is set
        memory.set(0x8810u, 0x01u)

        // WHEN the machine ticks
        pixelFetcher.tick()

        // THEN the new state is the get tile high data step with the expected low data
        assertEquals(PixelFetcher.State.GetTileDataHigh(0x81u, 0x01u), pixelFetcher.state)
    }

    @Test
    fun `Get tile Data low step - $8800 mode - negative - low boundary`() {
        // GIVEN the PixelFetcher is on the tile data low step
        pixelFetcher.state = PixelFetcher.State.GetTileDataLow(0x80u) // index 128
        // AND the addressing is using the $8800 mode
        lcdControlRegister.setBgAndWindowTileDataAddressingMode(false)
        // AND the first line is being drawn
        pixelFetcher.LY = 0
        // AND the tile data is set
        memory.set(0x8800u, 0x01u)

        // WHEN the machine ticks
        pixelFetcher.tick()

        // THEN the new state is the get tile high data step with the expected low data
        assertEquals(PixelFetcher.State.GetTileDataHigh(0x80u, 0x01u), pixelFetcher.state)
    }

    @Test
    fun `Get tile Data low step - $8800 mode - negative - high boundary`() {
        // GIVEN the PixelFetcher is on the tile data low step
        val newState = PixelFetcher.State.GetTileDataLow(0xFFu) // index 255
        pixelFetcher.state = newState
        // AND the addressing is using the $8800 mode
        lcdControlRegister.setBgAndWindowTileDataAddressingMode(false)
        // AND the first line is being drawn
        pixelFetcher.LY = 0
        // AND the tile data is set
        memory.set(0x8FF0u, 0x01u)

        // WHEN the machine ticks
        pixelFetcher.tick()

        // THEN the new state is the get tile high data step with the expected low data
        assertEquals(PixelFetcher.State.GetTileDataHigh(0xFFu, 0x01u), pixelFetcher.state)
    }

    @Test
    fun `Get tile Data High step - $8000 mode`() {
        // GIVEN the PixelFetcher is on the tile data high step
        pixelFetcher.state = PixelFetcher.State.GetTileDataHigh(0x02u, 0x03u)
        // AND the addressing is using the $8000 mode
        lcdControlRegister.setBgAndWindowTileDataAddressingMode(true)
        // AND the first line is being drawn
        pixelFetcher.LY = 0
        // AND the tile data is set
        memory.set(0x8021u, 0x01u)
        // AND the Fifo is not empty
        backgroundFifo.add(Pixel(ColorID.ZERO, 0 , false))

        // WHEN the machine ticks
        pixelFetcher.tick()

        // THEN the new state is the get push high data with the expected parameters
        assertEquals(PixelFetcher.State.Push(0x03u, 0x01u), pixelFetcher.state)
    }

    @Test
    fun `Get push data step - Fifo not empty`() {
        // GIVEN the PixelFetcher is on push data step
        pixelFetcher.state = PixelFetcher.State.Push(0x02u, 0x03u)
        // AND the Fifo is not empty
        backgroundFifo.add(Pixel(ColorID.ZERO, 0 , false))

        // WHEN the machine ticks
        pixelFetcher.tick()

        // THEN the state stays the same
        assertEquals(PixelFetcher.State.Push(0x02u, 0x03u), pixelFetcher.state)
    }

    @Test
    fun `Get push data step - Fifo empty`() {
        // GIVEN the PixelFetcher is on push data step
        // low: 0000 0110 / high: 0000 0011 gives 0000 0132 Pixel colors
        pixelFetcher.state = PixelFetcher.State.Push(0x06u, 0x03u)
        // AND the Fifo is empty
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

        // AND the state is set to get Tile
        assertEquals(PixelFetcher.State.GetTile, pixelFetcher.state)
    }

}