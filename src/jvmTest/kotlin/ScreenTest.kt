import io.mockk.*
import rendering.LcdControlRegister
import rendering.PixelFetcher
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExperimentalStdlibApi
@ExperimentalUnsignedTypes


internal class ScreenTest {

    lateinit var memory: Memory
    lateinit var screen: Screen

    @BeforeTest
    fun setUp() {
        memory = Memory()
        screen = Screen(memory)
    }

    private fun storeTile(baseAddress: UShort) {
        for(i in baseAddress..(baseAddress+15u).toUShort()) {
            memory.set(i.toUShort(), 0xFFu)
        }
    }


    @Test
    fun `Get first tile data - 8000 method`() {
        // Given a tile has been stored on the first ID
        storeTile(0x8000u)
        // When tiles are generated
        screen.generateTiles()

        validateSprite(0, false)
    }

    @Test
    fun `Get last tile data - 8000 method`() {
        // Given a tile has been stored on the last ID
        storeTile(0x8FF0u)
        // When tiles are generated
        screen.generateTiles()

        validateSprite(255, false)
    }

    @Test
    fun `Get first tile data - 8800 method`() {
        // Given a tile has been stored on the first ID
        storeTile(0x9000u)
        // When tiles are generated
        screen.generateTiles()

        validateSprite(0, true)
    }

    @Test
    fun `Get last tile data - 8800 method`() {
        // Given a tile has been stored on the last ID
        storeTile(0x8FF0u)
        // When tiles are generated
        screen.generateTiles()

        validateSprite(255, true)
    }

    @Test
    fun `Get last tile data - 8800 method - lower boundary`() {
        // Given a tile has been stored before the boundary
        storeTile(0x97F0u)
        // When tiles are generated
        screen.generateTiles()

        validateSprite(127, true)
    }

    @Test
    fun `Get last tile data - 8800 method - higher boundary`() {
        // Given a tile has been stored after the boundary
        storeTile(0x8800u)
        // When tiles are generated
        screen.generateTiles()

        validateSprite(128, true)
    }

    private fun validateSprite(tileId: Int, `8800AddressMode`: Boolean) {
        //Then only one sprite has the ID
        assertEquals(1, screen.tiles.filter { tile -> tile.getTileId(`8800AddressMode`) == tileId }.size)
        val lastTile = screen.tiles.find { tile -> tile.getTileId(`8800AddressMode`) == tileId }!!
        // That sprite has 8 rows
        assertEquals(8, lastTile.pixelsData.size)
        // of 2 bytes each
        assertEquals(2, lastTile.pixelsData[0].size)
        // And all values are set accordingly
        for (row in lastTile.pixelsData) {
            for (pixel in row) {
                assertEquals(0xFFu, pixel)
            }
        }
    }

    private fun buildSharedState(
        currentLineDotCount: Int = 0,
        currentLine: Int = 0,
        frame: List<ArrayList<Pixel>> = List(160) { ArrayList() },
        backgroundFifo: ArrayDeque<Pixel> = ArrayDeque(),
        pixelFetcher: PixelFetcher = PixelFetcher(LcdControlRegister(memory), memory)
    ): Screen.SharedState {
        return Screen.SharedState(currentLineDotCount, currentLine, frame, backgroundFifo, pixelFetcher)
    }

    @Test
    fun `OAM Scan generates the OAM data on the first dot`() {
        // Given the OAM data for the first sprite is stored
        memory.set(0xFE00u, 0xFAu)
        memory.set(0xFE01u, 0xFBu)
        memory.set(0xFE02u, 0xFCu)
        memory.set(0xFE03u, 0xFDu)
        // And the current state is OAM Scan for the very first dot
        screen.state = Screen.State.OAMScan(buildSharedState())

        // When a new dot is being processed
        screen.tick()

        // Then the OAM has 40 items
        assertEquals(40, screen.OAM.size)
        // And the first sprite has the relevant data
        assertEquals(0xFAu, screen.OAM[0].yPos)
        assertEquals(0xFBu, screen.OAM[0].XPos)
        assertEquals(0xFCu, screen.OAM[0].tileIndex)
        assertEquals(0xFDu, screen.OAM[0].attributesFlags)
    }

    @Test
    fun `OAM Scan set the STAT mode on the first dot`() {
        // Given the current state is OAM Scan for the very first dot
        screen.state = Screen.State.OAMScan(buildSharedState())

        // When a new dot is being processed
        screen.tick()

        // Then the Stat mode is set to 2
        assertEquals(0b10u, memory.get(0xFF41u).and(0b11u))
    }

    @Test
    fun `OAM Scan triggers STAT interrupt if enabled on the first dot`() {
        // Given the current state is OAM Scan for the very first dot
        screen.state = Screen.State.OAMScan(buildSharedState())
        // And the interrupt for OAM scan is enabled
        memory.set(0xFF41u, 0b0010_0000u)

        // When a new dot is being processed
        screen.tick()

        // Then the Stat interrupt is set
        assertEquals(0b0000_0010u, memory.get(0xFF0Fu))
    }

    @Test
    fun `OAM Scan does not triggers STAT interrupt if disabled on the first dot`() {
        // Given the current state is OAM Scan for the very first dot
        screen.state = Screen.State.OAMScan(buildSharedState())
        // And the interrupt for OAM scan is disabled
        memory.set(0xFF41u, 0b0001_0000u)

        // When a new dot is being processed
        screen.tick()

        // Then the Stat interrupt is not set
        assertEquals(0u, memory.get(0xFF0Fu))
    }

    @Test
    fun `OAM Scan lasts 80 dots`() {
        // Given the OAM scan is at the 79th dot
        val originalSharedState = buildSharedState(currentLineDotCount = 79)
        screen.state = Screen.State.OAMScan(originalSharedState)

        // When a new dot is being processed
        screen.tick()

        // Then the new state is Rendering Pixel for the next dot
        val expectedSharedState = originalSharedState.copy(currentLineDotCount = 80)

        assertEquals(Screen.State.DrawPixels(expectedSharedState), screen.state)
    }

    @Test
    fun `OAM Scan does not switch state if less than 80 dots`() {
        // Given the OAM scan is at any dot count lower than 79
        val originalSharedState = buildSharedState(currentLineDotCount = 12)
        screen.state = Screen.State.OAMScan(originalSharedState)

        // When a new dot is being processed
        screen.tick()

        // Then the state remains OAM Scan but the dot count is incremented
        val expectedSharedState = originalSharedState.copy(currentLineDotCount = 13)
        assertEquals(Screen.State.OAMScan(expectedSharedState), screen.state)
    }

    @Test
    fun `DrawPixels init fifo and fetcher on the first dot`() {
        // Given the current dot count is at 80 (first dot of DrawPixel)
        // And the fifo is not empty
        // And the pixel fetch is not reset
        val backgroundFifo = ArrayDeque<Pixel>()
        backgroundFifo.add(Pixel(ColorID.TWO, 0, false))
        val pixelFetcher: PixelFetcher = mockk()
        every { pixelFetcher.reset(any(), any()) } just Runs
        every { pixelFetcher.tick() } just Runs
        val currentLine = 12
        val originalSharedState = buildSharedState(currentLineDotCount = 80, currentLine = currentLine, backgroundFifo = backgroundFifo, pixelFetcher = pixelFetcher)

        screen.state = Screen.State.DrawPixels(originalSharedState)

        // When the machine ticks
        screen.tick()

        // Then the background fifo is emptied
        assertTrue(backgroundFifo.isEmpty())
        // And the pixel fetcher has been reset for the current line before ticking the machine
        verifyOrder {
            pixelFetcher.reset(currentLine, backgroundFifo)
            pixelFetcher.tick()
        }
    }

    @Test
    fun `DrawPixels empties fifo one pixel at a time`() {
        // Given the fifo is not empty
        val backgroundFifo = ArrayDeque<Pixel>()
        backgroundFifo.add(Pixel(ColorID.ONE, 0, false))
        backgroundFifo.add(Pixel(ColorID.TWO, 0, false))

        val pixelFetcher: PixelFetcher = mockk()
        every { pixelFetcher.reset(any(), any()) } just Runs
        every { pixelFetcher.tick() } just Runs

        // And the pixels for the current line is not empty
        val currentLine = 12
        val frame: List<ArrayList<Pixel>> = List(160) { ArrayList() }
        frame[currentLine].add(Pixel(ColorID.THREE, 0, false))

        // And the current dot count is not 80
        val originalSharedState = buildSharedState(currentLineDotCount = 90, frame = frame, currentLine=currentLine, backgroundFifo = backgroundFifo, pixelFetcher = pixelFetcher)

        screen.state = Screen.State.DrawPixels(originalSharedState)

        // When the machine ticks
        screen.tick()

        // Then the background fifo has lost its first pixel
        assertEquals(1, backgroundFifo.size)
        assertEquals(Pixel(ColorID.TWO, 0, false), backgroundFifo[0])

        // And the pixel removed is added to the frame at the current line
        assertEquals(2, frame[currentLine].size)
        assertEquals(Pixel(ColorID.THREE, 0, false), frame[currentLine][0]) // Existing pixel
        assertEquals(Pixel(ColorID.ONE, 0, false), frame[currentLine][1]) // New pixel

        // And the state remains on DrawPixels
        val expectedSharedState = originalSharedState.copy(currentLineDotCount = 91)
        assertEquals(Screen.State.DrawPixels(expectedSharedState), screen.state)
    }

    @Test
    fun `DrawPixels always ticks the PixelFetcher`() {
        val pixelFetcher: PixelFetcher = mockk()
        every { pixelFetcher.reset(any(), any()) } just Runs
        every { pixelFetcher.tick() } just Runs

        // Given the pixels for the line is not full (160 pixels)
        val currentLine = 12
        val frame: List<ArrayList<Pixel>> = List(160) { ArrayList() }
        for (i in 0..158) {
            frame[currentLine].add(Pixel(ColorID.THREE, 0, false))
        }

        val originalSharedState = buildSharedState(frame = frame, currentLine=currentLine, pixelFetcher = pixelFetcher)

        screen.state = Screen.State.DrawPixels(originalSharedState)

        // When the machine ticks
        screen.tick()

        // Then the PixelFetcher also ticks
        verify { pixelFetcher.tick() }

        // And the state remains on DrawPixels
        val expectedSharedState = originalSharedState.copy(currentLineDotCount = originalSharedState.currentLineDotCount + 1)
        assertEquals(Screen.State.DrawPixels(expectedSharedState), screen.state)
    }

    @Test
    fun `DrawPixels moves to Horizontal blank when the current line is full`() {
        val pixelFetcher: PixelFetcher = mockk()
        every { pixelFetcher.reset(any(), any()) } just Runs
        every { pixelFetcher.tick() } just Runs

        // Given the pixels for the line is missing 1 pixel to be full
        val currentLine = 12
        val frame: List<ArrayList<Pixel>> = List(160) { ArrayList() }
        for (i in 0..158) {
            frame[currentLine].add(Pixel(ColorID.THREE, 0, false))
        }
        // And the fifo has a pixel available
        val backgroundFifo = ArrayDeque<Pixel>()
        backgroundFifo.add(Pixel(ColorID.ONE, 0, false))

        val originalSharedState = buildSharedState(frame = frame, currentLine=currentLine, pixelFetcher = pixelFetcher, backgroundFifo = backgroundFifo)

        screen.state = Screen.State.DrawPixels(originalSharedState)

        // When the machine ticks
        screen.tick()

        // Then the state moves to horizontal blank
        val expectedSharedState = originalSharedState.copy(currentLineDotCount = originalSharedState.currentLineDotCount + 1)
        assertEquals(Screen.State.HorizontalBlank(expectedSharedState), screen.state)
    }

    @Test
    fun `DrawPixel set the STAT mode on the first dot`() {
        // Given the current dot count is at 80 (first dot of DrawPixel)
        val originalSharedState = buildSharedState(currentLineDotCount = 80)

        screen.state = Screen.State.DrawPixels(originalSharedState)

        // When a new dot is being processed
        screen.tick()

        // Then the Stat mode is set to 3
        assertEquals(0b11u, memory.get(0xFF41u).and(0b11u))
    }

    @Test
    fun `VBlank set the STAT mode on the first dot`() {
        // Given the current state is VBlank for the very first dot
        screen.state = Screen.State.VerticalBlank(buildSharedState())

        // When a new dot is being processed
        screen.tick()

        // Then the Stat mode is set to 1
        assertEquals(0b01u, memory.get(0xFF41u).and(0b11u))
    }

    @Test
    fun `VBlank triggers STAT interrupt if enabled on the first dot`() {
        // Given the current state is VBlank for the very first dot
        screen.state = Screen.State.VerticalBlank(buildSharedState())
        // And the interrupt for VBlank is enabled
        memory.set(0xFF41u, 0b0001_0000u)

        // When a new dot is being processed
        screen.tick()

        // Then the Stat interrupt request is set
        assertEquals(0b0000_0010u, memory.get(0xFF0Fu).and(0b10u))
    }

    @Test
    fun `VBlank triggers main VBlank interrupt on the first dot`() {
        // Given the current state is VBlank for the very first dot
        screen.state = Screen.State.VerticalBlank(buildSharedState())

        // When a new dot is being processed
        screen.tick()

        // Then the VBlank interrupt request is set
        assertEquals(0b0000_0001u, memory.get(0xFF0Fu).and(1u))
    }

    @Test
    fun `VBlank does not triggers STAT interrupt if disabled on the first dot`() {
        // Given the current state is VBlank for the very first dot
        screen.state = Screen.State.VerticalBlank(buildSharedState())
        // And the interrupt for VBlank is disabled
        memory.set(0xFF41u, 0b0000_1000u)

        // When a new dot is being processed
        screen.tick()

        // Then the Stat interrupt is not set
        assertEquals(0b0000_0000u, memory.get(0xFF0Fu).and(0b10u))
    }

    @Test
    fun `VBlank continues when current line dot count is less than 456`() {
        // Given the current state is VBlank with 455 line dot count
        val originalSharedState = buildSharedState(currentLineDotCount = 455)
        screen.state = Screen.State.VerticalBlank(originalSharedState)

        // When a new dot is being processed
        screen.tick()

        // Then the next state is VBlank with updated dot count
        val expectedSharedState = originalSharedState.copy(currentLineDotCount = originalSharedState.currentLineDotCount + 1)
        assertEquals(Screen.State.VerticalBlank(expectedSharedState), screen.state)
    }

    @Test
    fun `VBlank goes to next line once line dot count is 456`() {
        // Given the current state is VBlank with 456 line dot count and the current line is not 153
        val originalSharedState = buildSharedState(currentLineDotCount = 456, currentLine = 152)
        screen.state = Screen.State.VerticalBlank(originalSharedState)

        // When a new dot is being processed
        screen.tick()

        // Then the next state is VBlank for the next line
        val expectedSharedState = originalSharedState.copy(currentLineDotCount = 0, currentLine = 153)
        assertEquals(Screen.State.VerticalBlank(expectedSharedState), screen.state)
    }

    @Test
    fun `VBlank goes to new frame once line dot count is 456 and last line is drawn`() {
        // Given the current state is VBlank with 456 line dot count and the current line is 153
        // And the frame buffer is not empty
        val originalSharedState = buildSharedState(currentLineDotCount = 456, currentLine = 153, frame = List(160) { arrayListOf(Pixel(ColorID.ZERO, 0 , false)) })
        screen.state = Screen.State.VerticalBlank(originalSharedState)

        // When a new dot is being processed
        screen.tick()

        // Then the next state is OAM Scan for the first line and empty frame buffer
        val expectedSharedState = originalSharedState.copy(currentLineDotCount = 0, currentLine = 0, frame = List(160) {ArrayList()})
        assertEquals(Screen.State.OAMScan(expectedSharedState), screen.state)
    }

    @Test
    fun `HBlank set the STAT mode on the first dot`() {
        // Given the current state is HBlank for the very first dot
        screen.state = Screen.State.HorizontalBlank(buildSharedState(), 0)

        // When a new dot is being processed
        screen.tick()

        // Then the Stat mode is set to 0
        assertEquals(0b00u, memory.get(0xFF41u).and(0b11u))
    }

    @Test
    fun `HBlank triggers STAT interrupt if enabled on the first dot`() {
        // Given the current state is HBlank for the very first dot
        screen.state = Screen.State.HorizontalBlank(buildSharedState(), 0)
        // And the interrupt for HBlank is enabled
        memory.set(0xFF41u, 0b0000_1000u)

        // When a new dot is being processed
        screen.tick()

        // Then the Stat interrupt is set
        assertEquals(0b0000_0010u, memory.get(0xFF0Fu))
    }

    @Test
    fun `HBlank does not triggers STAT interrupt if disabled on the first dot`() {
        // Given the current state is HBlank for the very first dot
        screen.state = Screen.State.HorizontalBlank(buildSharedState(), 0)
        // And the interrupt for HBlank is disabled
        memory.set(0xFF41u, 0b0001_0000u)

        // When a new dot is being processed
        screen.tick()

        // Then the Stat interrupt is not set
        assertEquals(0b0000_0000u, memory.get(0xFF0Fu))
    }

    @Test
    fun `HBlank continues when current line dot count is less than 456`() {
        // Given the current state is HBlank with 455 line dot count
        val originalSharedState = buildSharedState(currentLineDotCount = 455)
        screen.state = Screen.State.HorizontalBlank(originalSharedState, 203)

        // When a new dot is being processed
        screen.tick()

        // Then the next state is HBlank with updated dot counts
        val expectedSharedState = originalSharedState.copy(currentLineDotCount = originalSharedState.currentLineDotCount + 1)
        assertEquals(Screen.State.HorizontalBlank(expectedSharedState, 204), screen.state)
    }

    @Test
    fun `HBlank goes to next line once line dot count is 456`() {
        // Given the current state is HBlank with 456 line dot count and the current line is not 143
        val originalSharedState = buildSharedState(currentLineDotCount = 456, currentLine = 142)
        screen.state = Screen.State.HorizontalBlank(originalSharedState, 204)

        // When a new dot is being processed
        screen.tick()

        // Then the next state is OAMScan for the next line
        val expectedSharedState = originalSharedState.copy(currentLineDotCount = 0, currentLine = 143)
        assertEquals(Screen.State.OAMScan(expectedSharedState), screen.state)
    }

    @Test
    fun `HBlank goes to VBlank once line dot count is 456 and last line is drawn`() {
        // Given the current state is HBlank with 456 line dot count and the current line is 143
        val originalSharedState = buildSharedState(currentLineDotCount = 456, currentLine = 143)
        screen.state = Screen.State.HorizontalBlank(originalSharedState, 204)

        // When a new dot is being processed
        screen.tick()

        // Then the next state is VBlank for the next line
        val expectedSharedState = originalSharedState.copy(currentLineDotCount = 0, currentLine = 144)
        assertEquals(Screen.State.VerticalBlank(expectedSharedState), screen.state)
    }
}