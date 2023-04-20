import io.mockk.*
import memory.Memory
import rendering.LcdControlRegister
import rendering.PixelFetcher
import kotlin.test.*

@ExperimentalStdlibApi
@ExperimentalUnsignedTypes


internal class ScreenTest {

    lateinit var memory: Memory
    lateinit var screen: Screen
    lateinit var lcdControlRegister: LcdControlRegister

    @BeforeTest
    fun setUp() {
        memory = Memory()
        lcdControlRegister = LcdControlRegister(memory)
        screen = Screen(memory, lcdControlRegister)
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
        objectFifo: ArrayDeque<Pixel> = ArrayDeque(),
        pixelFetcher: PixelFetcher = PixelFetcher(LcdControlRegister(memory), memory)
    ): Screen.SharedState {
        return Screen.SharedState(currentLineDotCount, currentLine, frame, backgroundFifo, objectFifo, pixelFetcher)
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
        assertEquals((0xFAu).toInt(), screen.OAM[0].yPos)
        assertEquals((0xFBu).toInt(), screen.OAM[0].xPos)
        assertEquals(0xFCu, screen.OAM[0].tileIndex)
        assertEquals(0xFDu, screen.OAM[0].attributesFlags)
    }

    @Test
    fun `OAM Scan finds sprite in the current line - Before scanline 16x16`() {
        // Given the first sprite is at position 0
        val sprite = Object(0,0,0xFFu, 0xFFu, 0xFE00u)
        Object.storeObjectInMemory(sprite, memory)
        // And the sprite size is set to 16x16
        lcdControlRegister.setSpriteSizeEnabled(true)
        // And the current scanline is 0
        // And the current state is OAM Scan for the very first dot
        val sharedState = buildSharedState(currentLine = 0, currentLineDotCount = 0)
        screen.state = Screen.State.OAMScan(sharedState)

        // When a new dot is being processed
        screen.tick()

        // Then the sprite isn't in the current sprite list as it is above the scanline
        assertEquals(Screen.State.OAMScan(sharedState.copy(currentLineDotCount = 1), spritesOnTheCurrentLine = emptyList()), screen.state)
    }

    @Test
    fun `OAM Scan finds sprite in the current line - Before scanline 16x8`() {
        // Given the first sprite is at position 8
        val sprite = Object(8,0,0xFFu, 0xFFu, 0xFE00u)
        Object.storeObjectInMemory(sprite, memory)
        // And the sprite size is set to 16x8
        lcdControlRegister.setSpriteSizeEnabled(false)
        // And the current scanline is 0
        // And the current state is OAM Scan for the very first dot
        val sharedState = buildSharedState(currentLine = 0, currentLineDotCount = 0)
        screen.state = Screen.State.OAMScan(sharedState)

        // When a new dot is being processed
        screen.tick()

        // Then the sprite isn't in the current sprite list as it is above the scanline
        assertEquals(Screen.State.OAMScan(sharedState.copy(currentLineDotCount = 1), spritesOnTheCurrentLine = emptyList()), screen.state)
    }

    @Test
    fun `OAM Scan finds sprite in the current line - Within scanline 16x16 - last line`() {
        // Given the first sprite is at position 1
        val sprite = Object(1, 0, 0xFFu, 0xFFu, 0xFE00u)
        Object.storeObjectInMemory(sprite, memory)
        // And the sprite size is set to 16x16
        lcdControlRegister.setSpriteSizeEnabled(true)
        // And the current scanline is 0
        // And the current state is OAM Scan for the very first dot
        val sharedState = buildSharedState(currentLine = 0, currentLineDotCount = 0)
        screen.state = Screen.State.OAMScan(sharedState)

        // When a new dot is being processed
        screen.tick()

        // Then the sprite is in the current sprite list as the last line of the sprite overlaps the current line
        assertEquals(Screen.State.OAMScan(sharedState.copy(currentLineDotCount = 1), spritesOnTheCurrentLine = listOf(Object.ObjectFromMemoryAddress(0xFE00u, memory))), screen.state)
    }

    @Test
    fun `OAM Scan finds sprite in the current line - Within scanline 16x8 - last line`() {
        // Given the first sprite is at position 9
        val sprite = Object(9, 0, 0xFFu, 0xFFu, 0xFE00u)
        Object.storeObjectInMemory(sprite, memory)
        // And the sprite size is set to 16x8
        lcdControlRegister.setSpriteSizeEnabled(false)
        // And the current scanline is 0
        // And the current state is OAM Scan for the very first dot
        val sharedState = buildSharedState(currentLine = 0, currentLineDotCount = 0)
        screen.state = Screen.State.OAMScan(sharedState)

        // When a new dot is being processed
        screen.tick()

        // Then the sprite is in the current sprite list as the last line of the sprite overlaps the current line
        assertEquals(Screen.State.OAMScan(sharedState.copy(currentLineDotCount = 1), spritesOnTheCurrentLine = listOf(Object.ObjectFromMemoryAddress(0xFE00u, memory))), screen.state)
    }

    @Test
    fun `OAM Scan finds sprite in the current line - Within scanline 16x16 - first line`() {
        // Given the first sprite is at position 16
        val sprite = Object(16, 0, 0xFFu, 0xFFu, 0xFE00u)
        Object.storeObjectInMemory(sprite, memory)
        // And the sprite size is set to 16x16
        lcdControlRegister.setSpriteSizeEnabled(true)
        // And the current scanline is 0
        // And the current state is OAM Scan for the very first dot
        val sharedState = buildSharedState(currentLine = 0, currentLineDotCount = 0)
        screen.state = Screen.State.OAMScan(sharedState)

        // When a new dot is being processed
        screen.tick()

        // Then the sprite is in the current sprite list as the first line of the sprite overlaps the current line
        assertEquals(Screen.State.OAMScan(sharedState.copy(currentLineDotCount = 1), spritesOnTheCurrentLine = listOf(Object.ObjectFromMemoryAddress(0xFE00u, memory))), screen.state)
    }

    @Test
    fun `OAM Scan finds sprite in the current line - Within scanline 16x8 - first line`() {
        // Given the first sprite is at position 16
        val sprite = Object(16, 0, 0xFFu, 0xFFu, 0xFE00u)
        Object.storeObjectInMemory(sprite, memory)
        // And the sprite size is set to 16x8
        lcdControlRegister.setSpriteSizeEnabled(false)
        // And the current scanline is 0
        // And the current state is OAM Scan for the very first dot
        val sharedState = buildSharedState(currentLine = 0, currentLineDotCount = 0)
        screen.state = Screen.State.OAMScan(sharedState)

        // When a new dot is being processed
        screen.tick()

        // Then the sprite is in the current sprite list as the first line of the sprite overlaps the current line
        assertEquals(Screen.State.OAMScan(sharedState.copy(currentLineDotCount = 1), spritesOnTheCurrentLine = listOf(Object.ObjectFromMemoryAddress(0xFE00u, memory))), screen.state)
    }
    @Test
    fun `OAM Scan finds sprite in the current line - After scanline`() {
        // Given the first sprite is at position 17
        val sprite = Object(17, 0, 0xFFu, 0xFFu, 0xFE00u)
        Object.storeObjectInMemory(sprite, memory)
        // And the current scanline is 0
        // And the current state is OAM Scan for the very first dot
        val sharedState = buildSharedState(currentLine = 0, currentLineDotCount = 0)
        screen.state = Screen.State.OAMScan(sharedState)

        // When a new dot is being processed
        screen.tick()

        // Then the sprite is not in the sprite list as the sprite is below the current scan line
        assertEquals(Screen.State.OAMScan(sharedState.copy(currentLineDotCount = 1), spritesOnTheCurrentLine = emptyList()), screen.state)
    }

    @Test
    fun `OAM Scan finds sprite in the current line - Within scanline 16x16 - multiple sprite`() {
        // Given the first sprite is at position 1
        val sprite = Object(1, 0, 0xFFu, 0xFFu, 0xFE00u)
        Object.storeObjectInMemory(sprite, memory)
        // Given the second sprite is at position 10
        val sprite2 = Object(10, 0, 0xFAu, 0xFFu, 0xFE04u)
        Object.storeObjectInMemory(sprite2, memory)
        // And the sprite size is set to 16x16
        lcdControlRegister.setSpriteSizeEnabled(true)
        // And the current scanline is 0
        // And the current state is OAM Scan for the very first dot
        val sharedState = buildSharedState(currentLine = 0, currentLineDotCount = 0)
        screen.state = Screen.State.OAMScan(sharedState)

        // When a new dot is being processed
        screen.tick()

        // Then both sprites are in the list
        assertEquals(Screen.State.OAMScan(sharedState.copy(currentLineDotCount = 1), spritesOnTheCurrentLine = listOf(Object.ObjectFromMemoryAddress(0xFE00u, memory), Object.ObjectFromMemoryAddress(0xFE04u, memory))), screen.state)
    }

    @Test
    fun `OAM Scan finds sprite in the current line - Within scanline 16x16 - 10 sprites limit`() {
        // Given 11 sprites are in the same line and next to each other on the X line
        val sprites = mutableListOf<Object>()
        for (i in 0..10) {
            val sprite = Object(1, i, 0xFFu, 0xFFu, (0xFE00u + (0x4u * i.toUInt())).toUShort())
            Object.storeObjectInMemory(sprite, memory)
            sprites.add(sprite)
        }
        val expectedRemovedSprite = sprites.removeLast()

        // And the sprite size is set to 16x16
        lcdControlRegister.setSpriteSizeEnabled(true)
        // And the current scanline is 0
        // And the current state is OAM Scan for the very first dot
        val sharedState = buildSharedState(currentLine = 0, currentLineDotCount = 0)
        screen.state = Screen.State.OAMScan(sharedState)

        // When a new dot is being processed
        screen.tick()

        // Then 10 sprites are in the list as the furthest one is being dismissed
        assertEquals(Screen.State.OAMScan(sharedState.copy(currentLineDotCount = 1), spritesOnTheCurrentLine = sprites), screen.state)
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

        assertEquals(Screen.State.DrawPixels(expectedSharedState, spritesOnTheCurrentLine = emptyList()), screen.state)
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

        screen.state = Screen.State.DrawPixels(originalSharedState, spritesOnTheCurrentLine = emptyList())

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

        screen.state = Screen.State.DrawPixels(originalSharedState, spritesOnTheCurrentLine = emptyList(), currentXScanLine = 1)

        // When the machine ticks
        screen.tick()

        // Then the background fifo has lost its first pixel
        assertEquals(1, backgroundFifo.size)
        assertEquals(Pixel(ColorID.TWO, 0, false), backgroundFifo[0])

        // And the pixel removed is added to the frame at the current line
        assertEquals(2, frame[currentLine].size)
        assertEquals(Pixel(ColorID.THREE, 0, false), frame[currentLine][0]) // Existing pixel
        assertEquals(Pixel(ColorID.ONE, 0, false), frame[currentLine][1]) // New pixel

        // And the state remains on DrawPixels for the next X
        val expectedSharedState = originalSharedState.copy(currentLineDotCount = 91)
        assertEquals(Screen.State.DrawPixels(expectedSharedState, spritesOnTheCurrentLine = emptyList(), currentXScanLine = 2), screen.state)
    }

    @Test
    fun `DrawPixels empties fifo one pixel at a time - BG disabled`() {
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

        // And the background is disabled
        lcdControlRegister.setBgAndWindowEnabled(false)

        screen.state = Screen.State.DrawPixels(originalSharedState, spritesOnTheCurrentLine = emptyList(), currentXScanLine = 1)

        // When the machine ticks
        screen.tick()

        // Then the background fifo has lost its first pixel
        assertEquals(1, backgroundFifo.size)
        assertEquals(Pixel(ColorID.TWO, 0, false), backgroundFifo[0])

        // And the fifo gets a empty pixel due to background being disabled
        assertEquals(2, frame[currentLine].size)
        assertEquals(Pixel(ColorID.THREE, 0, false), frame[currentLine][0]) // Existing pixel
        assertEquals(Pixel(ColorID.ZERO, 0, false), frame[currentLine][1]) // New pixel

        // And the state remains on DrawPixels for the next X
        val expectedSharedState = originalSharedState.copy(currentLineDotCount = 91)
        assertEquals(Screen.State.DrawPixels(expectedSharedState, spritesOnTheCurrentLine = emptyList(), currentXScanLine = 2), screen.state)
    }

    @Test
    fun `DrawPixels always ticks the PixelFetcher if no sprites to be drawn`() {
        val pixelFetcher: PixelFetcher = mockk()
        every { pixelFetcher.reset(any(), any()) } just Runs
        every { pixelFetcher.tick() } just Runs

        // Given the pixels for the line is not full (160 pixels)
        val currentLine = 12
        val frame: List<ArrayList<Pixel>> = List(160) { ArrayList() }
        for (i in 0..158) {
            frame[currentLine].add(Pixel(ColorID.THREE, 0, false))
        }
        // And no sprites should be drawn

        val originalSharedState = buildSharedState(frame = frame, currentLine=currentLine, pixelFetcher = pixelFetcher)

        screen.state = Screen.State.DrawPixels(originalSharedState, spritesOnTheCurrentLine = emptyList(), currentXScanLine = 158)

        // When the machine ticks
        screen.tick()

        // Then the PixelFetcher also ticks
        verify { pixelFetcher.tick() }

        // And the state remains on DrawPixels
        val expectedSharedState = originalSharedState.copy(currentLineDotCount = originalSharedState.currentLineDotCount + 1)
        assertEquals(Screen.State.DrawPixels(expectedSharedState, spritesOnTheCurrentLine = emptyList(), currentXScanLine = 159), screen.state)
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

        screen.state = Screen.State.DrawPixels(originalSharedState, spritesOnTheCurrentLine = emptyList())

        // When the machine ticks
        screen.tick()

        // Then the state moves to horizontal blank
        val expectedSharedState = originalSharedState.copy(currentLineDotCount = originalSharedState.currentLineDotCount + 1)
        assertEquals(Screen.State.HorizontalBlank(expectedSharedState), screen.state)
    }

    @Test
    fun `DrawPixels waits for background fifo to not be empty while having sprites the be drawn on the current X`() {
        val pixelFetcher: PixelFetcher = mockk()
        every { pixelFetcher.reset(any(), any()) } just Runs
        every { pixelFetcher.tick() } just Runs
        every { pixelFetcher.state } returns PixelFetcher.State.GetTile(PixelFetcher.SharedState(0u, 0, ArrayDeque()), 0)

        // Given there is a sprite to be drawn at X 0, Y 20
        val sprite = Object(36, 8, 0xFFu, 0xFFu, 0xFFFFu)
        // And sprites are enabled
        lcdControlRegister.setSpriteEnabled(true)

        // And the background fifo is empty
        val backgroundFifo = ArrayDeque<Pixel>()

        // And we are at line 20 at position 0
        val currentLine = 20
        val frame: List<ArrayList<Pixel>> = List(160) { ArrayList() }


        val originalSharedState = buildSharedState(frame = frame, currentLine=currentLine, pixelFetcher = pixelFetcher, backgroundFifo = backgroundFifo)

        screen.state = Screen.State.DrawPixels(originalSharedState, spritesOnTheCurrentLine = listOf(sprite))

        // When the machine ticks 5 times
        for (i in 0..4) {
            screen.tick()
        }

        // Then the PixelFetcher also ticks 5 times
        verify(exactly = 5) { pixelFetcher.tick() }

        // And the state remains on DrawPixels for the current X, with the sprite remaining to be drawn
        val expectedSharedState = originalSharedState.copy(currentLineDotCount = originalSharedState.currentLineDotCount + 5)
        assertEquals(Screen.State.DrawPixels(expectedSharedState, spritesOnTheCurrentLine = listOf(sprite), currentXScanLine = 0), screen.state)
    }

    @Test
    fun `DrawPixels tick fetcher 2 times for 4 dots while having sprites the be drawn on the current X`() {
        val pixelFetcher: PixelFetcher = mockk()
        every { pixelFetcher.reset(any(), any()) } just Runs
        every { pixelFetcher.tick() } just Runs
        every { pixelFetcher.state } returns PixelFetcher.State.GetTile(PixelFetcher.SharedState(0u, 0, ArrayDeque()), 0)

        // Given there is a sprite to be drawn at X 0, Y 20
        val sprite = Object(36, 8, 0xFFu, 0xFFu, 0xFFFFu)
        // And sprites are enabled
        lcdControlRegister.setSpriteEnabled(true)

        // And the background fifo is not empty
        val backgroundFifo = ArrayDeque<Pixel>()
        backgroundFifo.add(Pixel(ColorID.ONE, 0, false))

        // And we are at line 20 at position 0
        val currentLine = 20
        val frame: List<ArrayList<Pixel>> = List(160) { ArrayList() }


        val originalSharedState = buildSharedState(frame = frame, currentLine=currentLine, pixelFetcher = pixelFetcher, backgroundFifo = backgroundFifo)

        screen.state = Screen.State.DrawPixels(originalSharedState, spritesOnTheCurrentLine = listOf(sprite))

        // When the machine ticks 4 times
        for (i in 0..3) {
            screen.tick()
        }

        // Then the PixelFetcher also ticks 2 times
        verify(exactly = 2) { pixelFetcher.tick() }

        // And the state remains on DrawPixels for the current X, with the sprite remaining to be drawn
        val expectedSharedState = originalSharedState.copy(currentLineDotCount = originalSharedState.currentLineDotCount + 4)
        assertEquals(Screen.State.DrawPixels(expectedSharedState, spritesOnTheCurrentLine = listOf(sprite), currentXScanLine = 0, spriteFetchingState = Screen.SpriteFetchingState(4)), screen.state)
    }

    @Test
    fun `DrawPixels fills sprite Fifo while having sprites the be drawn on the current X`() {
        val pixelFetcher: PixelFetcher = mockk()
        every { pixelFetcher.reset(any(), any()) } just Runs
        every { pixelFetcher.tick() } just Runs
        every { pixelFetcher.state } returns PixelFetcher.State.GetTile(PixelFetcher.SharedState(0u, 0, ArrayDeque()), 0)

        // Given there is a sprite to be drawn at X 0, Y 20
        val sprite = Object(36, 8, 0xFFu, 0x00u, 0xFFFFu)
        //With data for the first line: low: 0000 0110 / high: 0000 0011 gives 0000 0132 Pixel colors
        memory.set(0x8FF0u, 0x06u)
        memory.set(0x8FF1u, 0x03u)
        // And sprites are enabled
        lcdControlRegister.setSpriteEnabled(true)

        // And we are at line 20 at position 0
        val currentLine = 20
        val frame: List<ArrayList<Pixel>> = List(160) { ArrayList() }

        // And the sprite is ready to be read
        val spriteFetchingState = Screen.SpriteFetchingState(fetcherAdvancementDotCount = 5)

        val originalSharedState = buildSharedState(frame = frame, currentLine=currentLine, pixelFetcher = pixelFetcher)

        screen.state = Screen.State.DrawPixels(originalSharedState, spritesOnTheCurrentLine = listOf(sprite), spriteFetchingState = spriteFetchingState)

        // When the machine ticks
        screen.tick()

        // Then the sprite data is added to the object fifo in order
        val expectedObjectFifo = ArrayDeque<Pixel>()

        expectedObjectFifo.add(Pixel(ColorID.ZERO, 0, false))
        expectedObjectFifo.add(Pixel(ColorID.ZERO, 0, false))
        expectedObjectFifo.add(Pixel(ColorID.ZERO, 0, false))
        expectedObjectFifo.add(Pixel(ColorID.ZERO, 0, false))
        expectedObjectFifo.add(Pixel(ColorID.ZERO, 0, false))
        expectedObjectFifo.add(Pixel(ColorID.ONE, 0, false))
        expectedObjectFifo.add(Pixel(ColorID.THREE, 0, false))
        expectedObjectFifo.add(Pixel(ColorID.TWO, 0, false))

        // And the sprite is removed from the list
        val expectedSpriteList = emptyList<Object>()

        // And the state remains on DrawPixels for the current X, with the sprite remaining to be drawn
        val expectedSharedState = originalSharedState.copy(objectFifo = expectedObjectFifo)
        assertEquals(Screen.State.DrawPixels(expectedSharedState, spritesOnTheCurrentLine = expectedSpriteList, currentXScanLine = 0, spriteFetchingState = Screen.SpriteFetchingState()), screen.state)
    }

    @Test
    fun `DrawPixels fills sprite Fifo while having sprites the be drawn on the current X - Horizontal flip`() {
        val pixelFetcher: PixelFetcher = mockk()
        every { pixelFetcher.reset(any(), any()) } just Runs
        every { pixelFetcher.tick() } just Runs
        every { pixelFetcher.state } returns PixelFetcher.State.GetTile(PixelFetcher.SharedState(0u, 0, ArrayDeque()), 0)

        // Given there is a sprite to be drawn at X 0, Y 20
        // With the horizontal bit set
        val sprite = Object(36, 8, 0xFFu, 0b0010_0000u, 0xFFFFu)
        //With data for the first line: low: 0000 0110 / high: 0000 0011 gives 0000 0132 Pixel colors
        memory.set(0x8FF0u, 0x06u)
        memory.set(0x8FF1u, 0x03u)
        // And sprites are enabled
        lcdControlRegister.setSpriteEnabled(true)

        // And we are at line 20 at position 0
        val currentLine = 20
        val frame: List<ArrayList<Pixel>> = List(160) { ArrayList() }

        // And the sprite is ready to be read
        val spriteFetchingState = Screen.SpriteFetchingState(fetcherAdvancementDotCount = 5)

        val originalSharedState = buildSharedState(frame = frame, currentLine=currentLine, pixelFetcher = pixelFetcher)

        screen.state = Screen.State.DrawPixels(originalSharedState, spritesOnTheCurrentLine = listOf(sprite), spriteFetchingState = spriteFetchingState)

        // When the machine ticks
        screen.tick()

        // Then the sprite data is added to the object fifo in reverse order due to horizontal flip
        val expectedObjectFifo = ArrayDeque<Pixel>()

        expectedObjectFifo.add(Pixel(ColorID.TWO, 0, false))
        expectedObjectFifo.add(Pixel(ColorID.THREE, 0, false))
        expectedObjectFifo.add(Pixel(ColorID.ONE, 0, false))
        expectedObjectFifo.add(Pixel(ColorID.ZERO, 0, false))
        expectedObjectFifo.add(Pixel(ColorID.ZERO, 0, false))
        expectedObjectFifo.add(Pixel(ColorID.ZERO, 0, false))
        expectedObjectFifo.add(Pixel(ColorID.ZERO, 0, false))
        expectedObjectFifo.add(Pixel(ColorID.ZERO, 0, false))

        // And the sprite is removed from the list
        val expectedSpriteList = emptyList<Object>()

        // And the state remains on DrawPixels for the current X, with the sprite remaining to be drawn
        val expectedSharedState = originalSharedState.copy(objectFifo = expectedObjectFifo)
        assertEquals(Screen.State.DrawPixels(expectedSharedState, spritesOnTheCurrentLine = expectedSpriteList, currentXScanLine = 0, spriteFetchingState = Screen.SpriteFetchingState()), screen.state)
    }

    @Test
    fun `DrawPixels fills sprite Fifo while having sprites the be drawn on the current X - Vertical flip`() {
        val pixelFetcher: PixelFetcher = mockk()
        every { pixelFetcher.reset(any(), any()) } just Runs
        every { pixelFetcher.tick() } just Runs
        every { pixelFetcher.state } returns PixelFetcher.State.GetTile(PixelFetcher.SharedState(0u, 0, ArrayDeque()), 0)

        // Given there is a sprite to be drawn at X 0, Y 20
        // With the vertical bit set
        val sprite = Object(36, 8, 0xFFu, 0b0100_0000u, 0xFFFFu)
        //With data for the last line: low: 0000 0110 / high: 0000 0011 gives 0000 0132 Pixel colors
        memory.set(0x9000u, 0x06u)
        memory.set(0x9001u, 0x03u)
        // And sprites are enabled
        lcdControlRegister.setSpriteEnabled(true)

        // And we are at line 20 at position 0
        val currentLine = 20
        val frame: List<ArrayList<Pixel>> = List(160) { ArrayList() }

        // And the sprite is ready to be read
        val spriteFetchingState = Screen.SpriteFetchingState(fetcherAdvancementDotCount = 5)

        val originalSharedState = buildSharedState(frame = frame, currentLine=currentLine, pixelFetcher = pixelFetcher)

        screen.state = Screen.State.DrawPixels(originalSharedState, spritesOnTheCurrentLine = listOf(sprite), spriteFetchingState = spriteFetchingState)

        // When the machine ticks
        screen.tick()

        // Then the sprite data is added to the object fifo
        val expectedObjectFifo = ArrayDeque<Pixel>()

        expectedObjectFifo.add(Pixel(ColorID.ZERO, 0, false))
        expectedObjectFifo.add(Pixel(ColorID.ZERO, 0, false))
        expectedObjectFifo.add(Pixel(ColorID.ZERO, 0, false))
        expectedObjectFifo.add(Pixel(ColorID.ZERO, 0, false))
        expectedObjectFifo.add(Pixel(ColorID.ZERO, 0, false))
        expectedObjectFifo.add(Pixel(ColorID.ONE, 0, false))
        expectedObjectFifo.add(Pixel(ColorID.THREE, 0, false))
        expectedObjectFifo.add(Pixel(ColorID.TWO, 0, false))

        // And the sprite is removed from the list
        val expectedSpriteList = emptyList<Object>()

        // And the state remains on DrawPixels for the current X, with the sprite remaining to be drawn
        val expectedSharedState = originalSharedState.copy(objectFifo = expectedObjectFifo)
        assertEquals(Screen.State.DrawPixels(expectedSharedState, spritesOnTheCurrentLine = expectedSpriteList, currentXScanLine = 0, spriteFetchingState = Screen.SpriteFetchingState()), screen.state)
    }

    @Test
    fun `DrawPixels skips adding pixel sprite Fifo if sprites already in the queue`() {
        val pixelFetcher: PixelFetcher = mockk()
        every { pixelFetcher.reset(any(), any()) } just Runs
        every { pixelFetcher.tick() } just Runs
        every { pixelFetcher.state } returns PixelFetcher.State.GetTile(PixelFetcher.SharedState(0u, 0, ArrayDeque()), 0)

        // Given there is a sprite to be drawn at X 0, Y 20
        val sprite = Object(36, 8, 0xFFu, 0x00u, 0xFFFFu)
        //With data for the first line: low: 0000 0110 / high: 0000 0011 gives 0000 0132 Pixel colors
        memory.set(0x8FF0u, 0x06u)
        memory.set(0x8FF1u, 0x03u)
        // And sprites are enabled
        lcdControlRegister.setSpriteEnabled(true)

        // And we are at line 20 at position 0
        val currentLine = 20
        val frame: List<ArrayList<Pixel>> = List(160) { ArrayList() }

        // And the object fifo is not empty for the first 7 pixels
        val objectFifo = ArrayDeque<Pixel>()
        for (i in 0..6) {
            objectFifo.add(Pixel(ColorID.ONE, 0, false))
        }

        // And the sprite is ready to be read
        val spriteFetchingState = Screen.SpriteFetchingState(fetcherAdvancementDotCount = 5)

        val originalSharedState = buildSharedState(frame = frame, currentLine=currentLine, pixelFetcher = pixelFetcher, objectFifo = objectFifo)

        screen.state = Screen.State.DrawPixels(originalSharedState, spritesOnTheCurrentLine = listOf(sprite), spriteFetchingState = spriteFetchingState)

        // When the machine ticks
        screen.tick()

        // Then the sprite data not added as the sprite fifo is not empty except the last pixel
        val expectedObjectFifo = ArrayDeque<Pixel>()

        expectedObjectFifo.add(Pixel(ColorID.ONE, 0, false))
        expectedObjectFifo.add(Pixel(ColorID.ONE, 0, false))
        expectedObjectFifo.add(Pixel(ColorID.ONE, 0, false))
        expectedObjectFifo.add(Pixel(ColorID.ONE, 0, false))
        expectedObjectFifo.add(Pixel(ColorID.ONE, 0, false))
        expectedObjectFifo.add(Pixel(ColorID.ONE, 0, false))
        expectedObjectFifo.add(Pixel(ColorID.ONE, 0, false))
        expectedObjectFifo.add(Pixel(ColorID.TWO, 0, false))

        // And the sprite is removed from the list
        val expectedSpriteList = emptyList<Object>()

        // And the state remains on DrawPixels for the current X, with the sprite remaining to be drawn
        val expectedSharedState = originalSharedState.copy(objectFifo = expectedObjectFifo)
        assertEquals(Screen.State.DrawPixels(expectedSharedState, spritesOnTheCurrentLine = expectedSpriteList, currentXScanLine = 0, spriteFetchingState = Screen.SpriteFetchingState()), screen.state)
    }

    @Test
    fun `DrawPixels prioritize sprite lower in the OAM`() {
        val pixelFetcher: PixelFetcher = mockk()
        every { pixelFetcher.reset(any(), any()) } just Runs
        every { pixelFetcher.tick() } just Runs
        every { pixelFetcher.state } returns PixelFetcher.State.GetTile(PixelFetcher.SharedState(0u, 0, ArrayDeque()), 0)

        // Given there is 2 sprite to be drawn at X 0, Y 20 with sprite 1 being lower in the base address
        val sprite1 = Object(36, 8, 0xFFu, 0x00u, 0xFFFEu)
        val sprite2 = Object(36, 8, 0xFEu, 0x00u, 0xFFFFu)
        //With data for the first line of sprite 1: low: 0000 0110 / high: 0000 0011 gives 0000 0132 Pixel colors
        memory.set(0x8FF0u, 0x06u)
        memory.set(0x8FF1u, 0x03u)

        //With data for the first line of sprite 2: low: 1111 1111 / high: 1111 1111 gives 3333 3333 Pixel colors
        memory.set(0x8FE0u, 0xFFu)
        memory.set(0x8FE1u, 0xFFu)
        // And sprites are enabled
        lcdControlRegister.setSpriteEnabled(true)

        // And we are at line 20 at position 0
        val currentLine = 20
        val frame: List<ArrayList<Pixel>> = List(160) { ArrayList() }

        // And the sprite is ready to be read
        val spriteFetchingState = Screen.SpriteFetchingState(fetcherAdvancementDotCount = 5)

        val originalSharedState = buildSharedState(frame = frame, currentLine=currentLine, pixelFetcher = pixelFetcher)

        screen.state = Screen.State.DrawPixels(originalSharedState, spritesOnTheCurrentLine = listOf(sprite1, sprite2), spriteFetchingState = spriteFetchingState)

        // When the machine ticks
        screen.tick()

        // Then the sprite data is for sprite 1 which is lower in the OAM address
        // Except for transparent pixel which are replaced by sprite 2
        val expectedObjectFifo = ArrayDeque<Pixel>()

        expectedObjectFifo.add(Pixel(ColorID.THREE, 0, false))
        expectedObjectFifo.add(Pixel(ColorID.THREE, 0, false))
        expectedObjectFifo.add(Pixel(ColorID.THREE, 0, false))
        expectedObjectFifo.add(Pixel(ColorID.THREE, 0, false))
        expectedObjectFifo.add(Pixel(ColorID.THREE, 0, false))
        expectedObjectFifo.add(Pixel(ColorID.ONE, 0, false))
        expectedObjectFifo.add(Pixel(ColorID.THREE, 0, false))
        expectedObjectFifo.add(Pixel(ColorID.TWO, 0, false))

        // And the sprite is removed from the list
        val expectedSpriteList = emptyList<Object>()

        // And the state remains on DrawPixels for the current X, with the sprite remaining to be drawn
        val expectedSharedState = originalSharedState.copy(objectFifo = expectedObjectFifo)
        assertEquals(Screen.State.DrawPixels(expectedSharedState, spritesOnTheCurrentLine = expectedSpriteList, currentXScanLine = 0, spriteFetchingState = Screen.SpriteFetchingState()), screen.state)
    }

    @Test
    fun `DrawPixels mixes background and object fifo`() {
        val pixelFetcher: PixelFetcher = mockk()
        every { pixelFetcher.reset(any(), any()) } just Runs
        every { pixelFetcher.tick() } just Runs
        every { pixelFetcher.state } returns PixelFetcher.State.GetTile(PixelFetcher.SharedState(0u, 0, ArrayDeque()), 0)

        // Given sprites are enabled
        lcdControlRegister.setSpriteEnabled(true)

        // And we are at line 0 at position 0
        val currentLine = 0
        val frame: List<ArrayList<Pixel>> = List(160) { ArrayList() }

        // And there is 8 non-transparent pixels in the background fifo
        val backgroundFifo = ArrayDeque<Pixel>()
        for (i in 0..7) {
            backgroundFifo.add(Pixel(ColorID.ONE, 0, false))
        }

        // And there is 7 non-transparent pixels and 1 transparent pixel in the sprite fifo
        val objectFifo = ArrayDeque<Pixel>()
        for (i in 0..7) {
            objectFifo.add(Pixel(ColorID.TWO, 0, false))
        }
        objectFifo[4] = Pixel(ColorID.ZERO, 0, false)


        val originalSharedState = buildSharedState(frame = frame, currentLine=currentLine, pixelFetcher = pixelFetcher, backgroundFifo = backgroundFifo, objectFifo = objectFifo)

        screen.state = Screen.State.DrawPixels(originalSharedState, spritesOnTheCurrentLine = emptyList())

        // When the machine ticks 8 times
        for (i in 0..7) {
            screen.tick()
        }

        // Then the sprite pixels has priority except for the transparent pixel which should be replaced by the background
        assertEquals(frame[0][0], Pixel(ColorID.TWO, 0 , false))
        assertEquals(frame[0][1], Pixel(ColorID.TWO, 0 , false))
        assertEquals(frame[0][2], Pixel(ColorID.TWO, 0 , false))
        assertEquals(frame[0][3], Pixel(ColorID.TWO, 0 , false))
        assertEquals(frame[0][4], Pixel(ColorID.ONE, 0 , false))
        assertEquals(frame[0][5], Pixel(ColorID.TWO, 0 , false))
        assertEquals(frame[0][6], Pixel(ColorID.TWO, 0 , false))
        assertEquals(frame[0][7], Pixel(ColorID.TWO, 0 , false))


        // And the state remains on DrawPixels for the next X
        assertEquals( Screen.State.DrawPixels(originalSharedState.copy(currentLineDotCount = 8), spritesOnTheCurrentLine = emptyList(), currentXScanLine = 8), screen.state)
    }

    @Test
    fun `DrawPixel set the STAT mode on the first dot`() {
        // Given the current dot count is at 80 (first dot of DrawPixel)
        val originalSharedState = buildSharedState(currentLineDotCount = 80)

        screen.state = Screen.State.DrawPixels(originalSharedState, spritesOnTheCurrentLine = emptyList())

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
        // And LY is updated
        assertEquals(153.toUByte(), memory.get(0xFF44u))
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
        val expectedSharedState = originalSharedState.copy(currentLineDotCount = 0, currentLine = 0, frame = List(160) { ArrayList() })
        assertEquals(Screen.State.OAMScan(expectedSharedState), screen.state)
        // And LY is updated
        assertEquals(0.toUByte(), memory.get(0xFF44u))
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
        // And LY is updated
        assertEquals(143.toUByte(), memory.get(0xFF44u))
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
        // And LY is updated
        assertEquals(144.toUByte(), memory.get(0xFF44u))
    }

    @Test
    fun `LYC=LY Stat register - Set`() {
        // Given the LYC register is set to line 144
        memory.set(0xFF45u, 144u)
        // And the current line is 143
        val originalSharedState = buildSharedState(currentLineDotCount = 456, currentLine = 143)
        screen.state = Screen.State.HorizontalBlank(originalSharedState, 204)

        // When moving to line 144
        screen.tick()

        // Then the LYC=LY State register is set
        val flag = memory.get(0xFF41u) and 0b100u
        assertTrue(flag.toUInt() > 0u)
    }

    @Test
    fun `LYC=LY Stat register - Unset`() {
        // Given the LYC register is set to line 143
        memory.set(0xFF45u, 143u)
        memory.set(0xFF41u, 0b100u)
        // And the current line is 143
        val originalSharedState = buildSharedState(currentLineDotCount = 456, currentLine = 143)
        screen.state = Screen.State.HorizontalBlank(originalSharedState, 204)

        // When moving to line 144
        screen.tick()

        // Then the LYC=LY State register is unset
        val flag = memory.get(0xFF41u) and 0b100u
        assertTrue(flag.toUInt() == 0u)
    }

    @Test
    fun `LYC=LY Stat interrupt`() {
        // Given the LYC register is set to line 144
        memory.set(0xFF45u, 144u)
        // And the LYC=LY interrupt is set
        memory.set(0xFF41u, 0b100_0000u)
        // And the current line is 143
        val originalSharedState = buildSharedState(currentLineDotCount = 456, currentLine = 143)
        screen.state = Screen.State.HorizontalBlank(originalSharedState, 204)

        // When moving to line 144
        screen.tick()

        // Then the stat interrupt is set
        assertEquals(0b0000_0010u, memory.get(0xFF0Fu))
    }

    @Test
    fun `LYC=LY Stat interrupt - Disabled`() {
        // Given the LYC register is set to line 143
        memory.set(0xFF45u, 143u)
        // And the LYC=LY interrupt is disabled
        memory.set(0xFF41u, 0b000_0000u)
        // And the current line is 143
        val originalSharedState = buildSharedState(currentLineDotCount = 456, currentLine = 143)
        screen.state = Screen.State.HorizontalBlank(originalSharedState, 204)

        // When moving to line 144
        screen.tick()

        // Then the stat interrupt is not set
        assertEquals(0b0000_0000u, memory.get(0xFF0Fu))
    }
}