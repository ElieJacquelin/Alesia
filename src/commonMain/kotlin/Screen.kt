import rendering.LcdControlRegister
import rendering.PixelFetcher

@OptIn(ExperimentalUnsignedTypes::class)
class Screen (val memory: Memory, val controlRegister: LcdControlRegister = LcdControlRegister(memory), backgroundFifo: ArrayDeque<Pixel> = ArrayDeque(), pixelFetcher: PixelFetcher = PixelFetcher(controlRegister, memory)) {
    var tiles = generateTiles()
    var OAM = generateOAM()
    internal var state: State = State.OAMScan(SharedState(0, 0,  List(160) { ArrayList() }, backgroundFifo, pixelFetcher))

    internal sealed class State {
        data class HorizontalBlank(val sharedState: SharedState, val dotCountInStep: Int = 0): State()
        data class VerticalBlank(val sharedState: SharedState): State()
        data class OAMScan(val sharedState: SharedState): State()
        data class DrawPixels(val sharedState: SharedState): State()
    }

    internal data class SharedState(val currentLineDotCount: Int, val currentLine: Int, val frame: List<ArrayList<Pixel>>, val backgroundFifo: ArrayDeque<Pixel>, val pixelFetcher: PixelFetcher)

    var frameUpdateListener: FrameUpdateListener? = null
    interface FrameUpdateListener {
        fun onFrameUpdate(frame: Array<Array<Pixel>>)
    }

    // Each tick represents 1 dot
    fun tick() {
        when (state) {
            is State.OAMScan -> oamScan(state as State.OAMScan)
            is State.DrawPixels -> drawingPixel(state as State.DrawPixels)
            is State.HorizontalBlank -> horizontalBlankState(state as State.HorizontalBlank)
            is State.VerticalBlank -> verticalBlankState(state as State.VerticalBlank)
        }
    }

    private fun oamScan(state: State.OAMScan) {
        val (currentLineDotCount) = state.sharedState
        if (currentLineDotCount == 0) {
            // Start of a new line, we can generate the OAM list
            // It might make sense to generate the OAM progressively in case the memory is being updated
            generateOAM()
            setStatMode(state)
            triggerStatInterruptIfNeeded()
        }
        val newSharedState = state.sharedState.copy(currentLineDotCount = currentLineDotCount + 1)
        // The OAM scan mode is meant to last 80 dots before moving to the next mode
        if (currentLineDotCount >= 79) {
            this.state = State.DrawPixels(newSharedState)
        } else {
            this.state = State.OAMScan(newSharedState)
        }
    }

    private fun drawingPixel(state: State.DrawPixels) {
        val (currentLineDotCount, currentLine, frame, backgroundFifo, pixelFetcher) = state.sharedState
        if (currentLineDotCount == 80) {
            // Resets fifo and fetcher to prepare them for the current line
            backgroundFifo.clear()
            pixelFetcher.reset(currentLine, backgroundFifo)

            setStatMode(state)
        }

        // Advance the fetcher one tick
        pixelFetcher.tick()
        // Start emptying the fifo, one pixel at a time
        val pixel = backgroundFifo.removeFirstOrNull()
        val currentLinePixels = frame[currentLine]
        if (pixel != null) {
            currentLinePixels.add(pixel)
        }

        val newSharedState = state.sharedState.copy(currentLineDotCount = currentLineDotCount + 1)
        if (currentLinePixels.size >= 160) {
            // The full line has been generated, we can move on to the next step
            this.state = State.HorizontalBlank(newSharedState)
            return
        }
        // Continue drawing pixels
        this.state = State.DrawPixels(newSharedState)
    }

    private fun setStatInterrupt() {
        var interrupt = memory.get(0xFF0Fu)
        interrupt = interrupt or (1u shl 1).toUByte()
        memory.set(0xFF0Fu, interrupt)
    }

    private fun setStatMode(state: State) {
        val stat = memory.get(0xFF41u).and(0b1111_1100u) // Reset first 2 bits to facilitate setting the mode after
        val newStat = when (state) {
            is State.OAMScan -> stat.or(0b10u) //Bit 1-0: 10
            is State.DrawPixels -> stat.or(0b11u) //Bit 1-0: 11
            is State.HorizontalBlank -> stat.or(0b00u) //Bit 1-0: 00
            is State.VerticalBlank -> stat.or(0b01u) //Bit 1-0: 01
        }
        memory.set(0xFF41u, newStat)
    }

    private fun triggerStatInterruptIfNeeded() {
        val stat = memory.get(0xFF41u)
        val currentMode = stat.and(0b11u) // First two bits describe the current mode
        when (currentMode.toUInt()) {
            0u -> if(stat.and(0b1000u) > 0u) setStatInterrupt() // HBlank
            1u -> if(stat.and(0b10000u) > 0u) setStatInterrupt() // VBlank
            2u -> if(stat.and(0b100000u) > 0u) setStatInterrupt() // OAM Search
            4u -> {} // Transferring data to LCD doesn't trigger interrupt
        }
    }

    private fun horizontalBlankState(state: State.HorizontalBlank) {
        if (state.dotCountInStep == 0) {
            setStatMode(state)
            // Trigger horizontal blank interrupt on the first dot
            triggerStatInterruptIfNeeded()
        }
        if (state.sharedState.currentLineDotCount == 456) {
            // Reached the end of HBlank, go for the next line or VBlank
            val newLineNumber = state.sharedState.currentLine + 1
            val newSharedState = state.sharedState.copy(currentLineDotCount = 0, currentLine = newLineNumber)
            // Increment LY counter
            memory.set(0xFF44u, newLineNumber.toUByte())
            if (state.sharedState.currentLine == 143) {
                this.state = State.VerticalBlank(newSharedState)
            } else {
                this.state = State.OAMScan(newSharedState)
            }
            return
        }

        // Continue HBlank
        val currentLintDotCount = state.sharedState.currentLineDotCount
        this.state = State.HorizontalBlank(state.sharedState.copy(currentLineDotCount = currentLintDotCount + 1), state.dotCountInStep + 1)
    }

    private fun setVBlankInterrupt() {
        var interrupt = memory.get(0xFF0Fu)
        interrupt = interrupt or (1u).toUByte()
        memory.set(0xFF0Fu, interrupt)
    }

    private fun verticalBlankState(state: State.VerticalBlank) {
        if (state.sharedState.currentLineDotCount == 0) {
            setStatMode(state)
            // Trigger Vertical blank interrupt on the first dot
            triggerStatInterruptIfNeeded()
            // Also trigger "main" vertical blank interrupt
            setVBlankInterrupt()
        }
        if (state.sharedState.currentLineDotCount == 456) {
            // Reached the end of current line, go for the next line or new frame
            if (state.sharedState.currentLine == 153) {
                var completeFrame: Array<Array<Pixel>> = Array(144) { emptyArray() }
                for (row in 0..143) {
                    completeFrame[row] = state.sharedState.frame[row].toTypedArray()
                }
                frameUpdateListener?.onFrameUpdate(completeFrame)

                // Start a new frame
                this.state = State.OAMScan(state.sharedState.copy(currentLine = 0, currentLineDotCount = 0, frame = List(160) { ArrayList() }))
                // Reset LY counter
                memory.set(0xFF44u, 0u)
            } else {
                val newLineNumber = state.sharedState.currentLine + 1
                this.state = State.VerticalBlank(state.sharedState.copy(currentLine = newLineNumber, currentLineDotCount = 0))
                // Increment LY counter
                memory.set(0xFF44u, newLineNumber.toUByte())
            }
            return
        }

        // Continue VBlank
        val currentLintDotCount = state.sharedState.currentLineDotCount
        this.state = State.VerticalBlank(state.sharedState.copy(currentLineDotCount = currentLintDotCount + 1))
    }

    private fun generateOAM(): Array<Object> {
        val result = mutableListOf<Object>()
        for(objectAddress in 0xFE00u..0xFE9Fu step 4) {
            result.add(Object(objectAddress.toUShort()))
        }
        OAM = result.toTypedArray()
        return OAM
    }

    fun generateTiles(): Array<Tile> {
        val result = mutableListOf<Tile>()
        // Every tile needs 16 bytes with the tile ID being the index
        for((tileId, tileAddress) in (0x8000u..0x97FFu step 16).withIndex()) {
            val data = mutableListOf<Array<UByte>>()
            // A tile is composed of 8 lines of 2 bytes
            for (i in 0u..15u step 2) {
                data.add(arrayOf(memory.get((tileAddress + i).toUShort()), memory.get((tileAddress + i + 1u).toUShort())))
            }
            result.add(Tile(data.toTypedArray(), tileId))
        }
        tiles = result.toTypedArray()
        return tiles
    }

    sealed class Layer {}

    inner class Background(): Layer() {
        fun drawBackground() {
            if(!controlRegister.getBgTileMap()) {
                // 0x9800 tile map is being used
                if(!controlRegister.getBgAndWindowTileDataAddressingMode()) {
                    // 0x8000 mode
                    for(tileId in 0x9800u..0x9BFFu) {

                    }
                }
            }
        }
    }
    class Window(): Layer()
    inner class Object(baseAddress: UShort): Layer() {
        val yPos:UByte = memory.get(baseAddress)
        val XPos:UByte = memory.get((baseAddress+1u).toUShort())
        val tileIndex:UByte = memory.get((baseAddress+2u).toUShort())
        val attributesFlags:UByte = memory.get((baseAddress+3u).toUShort())
    }
}
// 8x8 pixel, 2 bytes per row, 16 bytes total
class Tile(val pixelsData: Array<Array<UByte>>, private val id:Int) {

    val pixels: Array<Array<Pixel>>

    init {
        val pixels = Array(8) {Array(8) { Pixel(ColorID.ZERO, 0, false) } }
        for (x in 0..7) {
            for (y in 0..7) {
                // TODO handle palette and background priority
                pixels[x][y] = Pixel(getColorId(x,y), 0, false)
            }
        }
        this.pixels = pixels
    }

    fun getTileId(`8800AddressMode`: Boolean): Int {
        return if(!`8800AddressMode` || (id in 128..255)) {
            id
        } else {
            return id - 256
        }
    }

    private fun getColorId(x: Int, y: Int):ColorID {
        // Logic is explained here: https://www.huderlem.com/demos/gameboy2bpp.html
        val first = if (pixelsData[x][1] and (1u shl (7 - y)).toUByte() > 1u) {
            0x10u
        } else {
            0u
        }

        val second = if (pixelsData[x][0] and (1u shl (7 - y)).toUByte() > 1u) {
            0x1u
        } else {
            0u
        }

        return when(first + second) {
            0x00u -> ColorID.ZERO
            0x01u -> ColorID.ONE
            0x10u -> ColorID.TWO
            0x11u -> ColorID.THREE
            else -> throw Exception("Invalid color ID: ${first + second}")
        }
    }
}
enum class ColorID {
    ZERO, ONE, TWO, THREE;
}

data class Pixel(val colorId: ColorID, val palette: Int, val backgroundPriority:Boolean) {

}

class Palette(colors: Array<Int>)
