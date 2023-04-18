package rendering

import ColorID
import memory.Memory
import Pixel

@ExperimentalUnsignedTypes
class PixelFetcher(private val controlRegister: LcdControlRegister, private val memory: Memory) {

    internal sealed class State {
        data class GetTile(val sharedState: SharedState, val dotCount: Int = 0) : State()
        data class GetTileDataLow(val sharedState: SharedState, val tileID: UByte, val dotCount: Int = 0): State()
        data class GetTileDataHigh(val sharedState: SharedState, val tileID: UByte, val tileLowData: UByte, val dotCount: Int = 0): State()
        data class Push(val sharedState: SharedState, val tileLowData: UByte, val tileHighData: UByte): State()
    }

    internal data class SharedState(val currentTileMapOffset: UInt, val currentLine: Int, val backgroundFiFo: ArrayDeque<Pixel>)

    internal lateinit var state: State

    fun reset(lineNumber: Int, backgroundFiFo: ArrayDeque<Pixel>) {
        state = State.GetTile(SharedState(0u, lineNumber, backgroundFiFo))
    }

    fun tick() {
        when (state) {
            is State.GetTile -> getTileStep(state as State.GetTile)
            is State.GetTileDataLow -> getTileDataLowStep(state as State.GetTileDataLow)
            is State.GetTileDataHigh -> getTileDataHighStep(state as State.GetTileDataHigh)
            is State.Push -> pushToFifoStep(state as State.Push)
        }
    }

    private fun getTileStep(state: State.GetTile) {
        val (currentTileMapOffset) = state.sharedState
        if (state.dotCount >= 1) {
            // Window check
            if(controlRegister.getWindowEnabled()) {
                val windowY = memory.get(0xFF4Au)
                val windowX = memory.get(0xFF4Bu)
                val relativeWindowX = if(windowX > 7u) windowX - 7u else 0u
                if(state.sharedState.currentLine.toUByte() >= windowY &&
                    currentTileMapOffset >= relativeWindowX) {
                    // The current pixel is in the window
                    val mapTile = if(controlRegister.getWindowTileMap()) 0x9C00u else 0x9800u
                    val tileIDx = (currentTileMapOffset - relativeWindowX)
                    val tileIDy = ((state.sharedState.currentLine.toUByte() - windowY) / 8u)
                    val tileID = memory.get((mapTile + tileIDx + (0x20u * tileIDy)).toUShort(), isGPU = true)

                    this.state = State.GetTileDataLow(state.sharedState, tileID)
                    return
                }
            }

            // Background
            val mapTile = if (controlRegister.getBgTileMap()) 0x9C00u else 0x9800u
            val scrollY = memory.get(0xFF42u, isGPU = true) // Number of pixels scroll to the right
            val scrollX = memory.get(0xFF43u, isGPU = true)
            // Divide the scroll register by 8 to find the related tile being displayed
            val tileIDx = (currentTileMapOffset + (scrollX / 8u)) % 32u // Wraps around if reaching the end of the line
            val tileIDy = ((state.sharedState.currentLine / 8).toUByte() + (scrollY / 8u)) % 32u
            val tileID = memory.get((mapTile + tileIDx + (0x20u * tileIDy)).toUShort(), isGPU = true)


            this.state = State.GetTileDataLow(state.sharedState, tileID)
            return
        }
        // Get tile step lasts 2 dots, do nothing for the first dot
        this.state = State.GetTile(state.sharedState, state.dotCount + 1)
    }

    private fun getTileDataLowStep(state: State.GetTileDataLow) {
        val (_, currentLine) = state.sharedState
        if (state.dotCount >= 1) {
            val tileLowData = getTileDataStep(state.tileID, true, currentLine)
            this.state = State.GetTileDataHigh(sharedState = state.sharedState, tileID = state.tileID, tileLowData = tileLowData)
            return
        }
        // Get Tile data low step lasts 2 dots, do nothing for the first dot
        this.state = State.GetTileDataLow(state.sharedState, state.tileID, state.dotCount + 1)
    }

    private fun getTileDataHighStep(state: State.GetTileDataHigh) {
        val (_, currentLine) = state.sharedState
        if (state.dotCount >= 1) {
            val tileHighData = getTileDataStep(state.tileID, false, currentLine)

            this.state = State.Push(state.sharedState, state.tileLowData, tileHighData)
            pushToFifoStep(this.state as State.Push) // This step does an extra push without waiting for the next tick
            return
        }
        // Get Tile data high step lasts 2 dots, do nothing for the first dot
        this.state = State.GetTileDataHigh(state.sharedState, state.tileID, state.tileLowData, state.dotCount + 1)
    }

    private fun getTileDataStep(tileID: UByte, low: Boolean, currentLine: Int): UByte {
        var tileAddress = getTileAddress(tileID)
        if (!low) {
            tileAddress ++
        }
        // The screen renders a matrix of 8x8 sprites, getting the modulo by 8 of the current line being rendered tells you which line of the current sprite is being drawn
        val currentSpriteLine = currentLine.mod(8).toUInt()
        return getTileLineData(tileAddress, currentSpriteLine)
    }

    private fun getTileAddress(tileID: UByte): UShort {
        // Each sprite takes 16 bytes of memory, we have to jump 16 bytes to get to the next sprite
        return if (controlRegister.getBgAndWindowTileDataAddressingMode()) {
            (0x8000u + tileID * 16u).toUShort()
        } else {
            // In $8800 mode the tileID is interpreted as signed
            if(tileID >= 128u) {
                (0x8800u + (tileID - 128u) * 16u).toUShort()
            } else {
                (0x9000u + tileID * 16u).toUShort()
            }
        }
    }

    private fun getTileLineData(tileAddress: UShort, lineSprite:UInt): UByte {
        // A sprite takes 2 bytes per line, we can skip to the current line by jumping by a multiple of 2
        return memory.get((tileAddress + 2u * lineSprite).toUShort(), isGPU = true)
    }

    private fun pushToFifoStep(state: State.Push) {
        val (_, _, backgroundFiFo) = state.sharedState
        if(!backgroundFiFo.isEmpty()) {
            // Stay in the same step until it succeed
            return
        }

        val pixelRow = Array(8) { Pixel(ColorID.ZERO, 0, false) }
        for (x in 0..7) {
            // Logic is explained here: https://www.huderlem.com/demos/gameboy2bpp.html
            val first = if (state.tileHighData and (1u shl (7 - x)).toUByte() >= 1u) {
                0x10u
            } else {
                0u
            }

            val second = if (state.tileLowData and (1u shl (7 - x)).toUByte() >= 1u) {
                0x1u
            } else {
                0u
            }

            val colorID = when(first + second) {
                0x00u -> ColorID.ZERO
                0x01u -> ColorID.ONE
                0x10u -> ColorID.TWO
                0x11u -> ColorID.THREE
                else -> throw Exception("Invalid color ID: ${first + second}")
            }
            pixelRow[x] = Pixel(colorID, 0, false)
        }

        // Push to Fifo
        backgroundFiFo.addAll(pixelRow)
        // Move to the next tile to be rendered
        val newTileMapOffset = if(state.sharedState.currentTileMapOffset == 31u) 0u else state.sharedState.currentTileMapOffset + 1u
        val newSharedState = state.sharedState.copy(currentTileMapOffset = newTileMapOffset)

        this.state = State.GetTile(newSharedState)
    }
}