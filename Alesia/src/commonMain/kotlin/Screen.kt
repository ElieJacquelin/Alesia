import memory.Memory
import rendering.LcdControlRegister
import rendering.PixelFetcher

@OptIn(ExperimentalUnsignedTypes::class)
class Screen (val memory: Memory, val controlRegister: LcdControlRegister = LcdControlRegister(memory), backgroundFifo: ArrayDeque<Pixel> = ArrayDeque(), objectFifo: ArrayDeque<Pixel> = ArrayDeque(), pixelFetcher: PixelFetcher = PixelFetcher(controlRegister, memory)) {
    internal var state: State = State.OAMScan(SharedState(0, 0,  List(160) { ArrayList() }, backgroundFifo, objectFifo, pixelFetcher))

    internal sealed class State {
        abstract val sharedState: SharedState

        data class Disabled(override val sharedState: SharedState): State()
        data class HorizontalBlank(override val sharedState: SharedState, val dotCountInStep: Int = 0): State()
        data class VerticalBlank(override val sharedState: SharedState): State()
        data class OAMScan(override val sharedState: SharedState, val spritesOnTheCurrentLine: MutableList<Object> = mutableListOf()): State()
        data class DrawPixels(override val sharedState: SharedState, val currentXScanLine: Int = 0, val spriteFetchingState: SpriteFetchingState = SpriteFetchingState(), val spritesOnTheCurrentLine: MutableList<Object>): State()
    }

    // Performance: The fields are var to avoid having to do copy() between each step
    internal data class SharedState(var currentLineDotCount: Int, var currentLine: Int, var frame: List<ArrayList<Pixel>>, var backgroundFifo: ArrayDeque<Pixel>, var objectFifo: ArrayDeque<Pixel>, var pixelFetcher: PixelFetcher)

    internal data class SpriteFetchingState(val fetcherAdvancementDotCount: Int = 0)

    var frameUpdateListener: FrameUpdateListener? = null
    interface FrameUpdateListener {
        fun onFrameUpdate(frame: Array<Array<Pixel>>)
    }

    private fun disablePPU() {
        this.state = State.Disabled(this.state.sharedState)
        // Set STAT mode to HBLANK
        memory.set(0xFF41u, memory.get(0xFF41u)and 0b1111_1100u)
        // Reset LY counter
        updateLYCounter(0u)
    }

    private fun enablePPU() {
        controlRegister.setDisplay(true)
        // Start a new frame
        state.sharedState.currentLine = 0
        state.sharedState.currentLineDotCount = 0
        state.sharedState.frame = List(160) { ArrayList() }
        this.state = State.OAMScan(state.sharedState)

        // Reset LY counter
        updateLYCounter(0u)
    }

    // Each tick represents 1 dot
    fun tick() {
        // LCD off check
        if (memory.get(0xFF40u) and 0b1000_0000u == 0u.toUByte()) {
            disablePPU()
        }

        when (state) {
            is State.OAMScan -> oamScan(state as State.OAMScan)
            is State.DrawPixels -> drawingPixel(state as State.DrawPixels)
            is State.HorizontalBlank -> horizontalBlankState(state as State.HorizontalBlank)
            is State.VerticalBlank -> verticalBlankState(state as State.VerticalBlank)
            is State.Disabled -> {
                if(controlRegister.getDisplay()) {
                    enablePPU()
                }
            }
        }
    }

    private fun oamScan(state: State.OAMScan) {
        val (currentLineDotCount, currentLine) = state.sharedState
        if (currentLineDotCount == 0) {
            // Start of a new line, we can generate the OAM list
            val OAM = generateOAM()
            // and find the first 10 sprites to be displayed for that line
            val spritesOnTheScanLine = OAM.filter { sprite ->
                // Keep only sprites that are visible
                sprite.getSpriteLineForCurrentScanLine(currentLine, controlRegister) != -1
            }.sortedBy { sprite -> sprite.xPos //Sort by increasing X position
            }.take(10) // Ignore sprites after 10 consecutive sprites
            .map { sprite ->
                if(controlRegister.getSpriteSizeEnabled()) {
                   sprite.copy(tileIndex = sprite.tileIndex and 0b1111_1110u) // Ignore bit 0 of 16*8 sprites
                } else {
                    sprite
                }
            }.toMutableList()

            setStatMode(state)
            triggerStatInterruptIfNeeded()
            memory.lockOAM()

            state.sharedState.currentLineDotCount = 1
            this.state = State.OAMScan(state.sharedState, spritesOnTheCurrentLine = spritesOnTheScanLine)
            return
        }
        state.sharedState.currentLineDotCount += 1
        // The OAM scan mode is meant to last 80 dots before moving to the next mode
        if (currentLineDotCount == 79) {
            this.state = State.DrawPixels(state.sharedState, spritesOnTheCurrentLine = state.spritesOnTheCurrentLine)
        } else {
            this.state = State.OAMScan(state.sharedState, spritesOnTheCurrentLine = state.spritesOnTheCurrentLine)
        }
    }

    private fun drawingPixel(state: State.DrawPixels) {
        val (currentLineDotCount, currentLine, frame, backgroundFifo, objectFifo, pixelFetcher) = state.sharedState
        if (currentLineDotCount == 80) {
            // Resets fifo and fetcher to prepare them for the current line
            backgroundFifo.clear()
            objectFifo.clear()
            pixelFetcher.reset(currentLine, backgroundFifo)

            setStatMode(state)
            // Lock VRAM
            memory.lockVRAM()
        }

        val spriteList = state.spritesOnTheCurrentLine
        val spritesOnTheCurrentXCoordinate: List<Object> = spriteList.filter { sprite -> sprite.xPos - 8 == state.currentXScanLine }
        if(controlRegister.getSpriteEnabled() && spritesOnTheCurrentXCoordinate.isNotEmpty()) {
            // Wait for the pixel fetcher to start having enough pixels in the queue
            val (spriteFetchingDotCount) = state.spriteFetchingState
            if (pixelFetcher.state !is PixelFetcher.State.Push && backgroundFifo.isEmpty() && spriteFetchingDotCount == 0) {
                pixelFetcher.tick()
                state.sharedState.currentLineDotCount += 1
                this.state = State.DrawPixels(state.sharedState, spritesOnTheCurrentLine = spriteList, currentXScanLine = state.currentXScanLine)
                return
            }

            // Advance fetcher twice for a total of 3 dots + 1 dots to find sprite address
            if (spriteFetchingDotCount in 0..4) {
                if(spriteFetchingDotCount == 0 || spriteFetchingDotCount == 1) {
                    pixelFetcher.tick()
                    // Sprite abortion may happen here
                }
                state.sharedState.currentLineDotCount += 1
                val newSpriteFetchingState = SpriteFetchingState(fetcherAdvancementDotCount = spriteFetchingDotCount + 1)
                this.state = State.DrawPixels(state.sharedState, currentXScanLine = state.currentXScanLine, spriteFetchingState = newSpriteFetchingState, spritesOnTheCurrentLine = spriteList)
                return
            }
            // Dot count 4 looks for the lower address of the sprite tile, we skip it and handle the whole sprite the next dot
            if (spriteFetchingDotCount == 5) {
                for(spriteToBeDrawn in spritesOnTheCurrentXCoordinate) {
                    val spriteTileAddress = getSpriteTileAddress(spriteToBeDrawn)
                    val spriteLineDataLow = getTileLineData(
                        spriteTileAddress,
                        spriteToBeDrawn.getSpriteLineForCurrentScanLine(currentLine, controlRegister).toUInt()
                    )
                    val spriteLineDataHigh = getTileLineData(
                        (spriteTileAddress + 1u).toUShort(),
                        spriteToBeDrawn.getSpriteLineForCurrentScanLine(currentLine, controlRegister).toUInt()
                    )
                    val backgroundPriority = spriteToBeDrawn.attributesFlags and 0b1000_0000u > 0u
                    val palette = if(spriteToBeDrawn.attributesFlags and 0b0001_0000u > 0u) 1 else 0
                    val spritePixels = getSpritePixels(spriteLineDataLow, spriteLineDataHigh, backgroundPriority, palette)

                    if (spriteToBeDrawn.attributesFlags and 0b0010_0000u > 0u) {
                        // Horizontal flip
                        spritePixels.reverse()
                    }

                    // Start filling object fifo with lowest priority pixel until reaching the needed size
                    while (objectFifo.size < 8) {
                        objectFifo.add(Pixel(ColorID.ZERO,0, 0, false))
                    }

                    // Start adding the sprites pixels to the queue
                    spritePixels.forEachIndexed { i, pixel ->
                        // Replace pixel in fifo with sprite pixel if the pixel in fifo is white or transparent
                        if (objectFifo[i].colorId == ColorID.ZERO) {
                            objectFifo[i] = pixel
                        }
                    }
                    // The sprite has been handled, we can remove it from the list of sprites to be rendered
                    spriteList.remove(spriteToBeDrawn)
                }
                // Done with adding sprite pixels for the current X, we can continue with the pixel rendering
                state.sharedState.currentLineDotCount += 1
                this.state = State.DrawPixels(state.sharedState, spritesOnTheCurrentLine = spriteList, currentXScanLine = state.currentXScanLine)
                return
            }
        }

        // Advance the fetcher one tick
        pixelFetcher.tick()
        // Start emptying the fifo, one pixel at a time from both background and object fifo to keep them in sync
        val pixelBackground = backgroundFifo.removeFirstOrNull()
        val currentLinePixels = frame[currentLine]
        if(pixelBackground != null) {
            val pixelSprite = objectFifo.removeFirstOrNull()
            val pixelToBeDrawn = if(
                controlRegister.getSpriteEnabled() &&
                pixelSprite != null &&
                pixelSprite.colorId != ColorID.ZERO &&
                // Background over object check
                (!pixelSprite.backgroundPriority ||
                pixelBackground.colorId == ColorID.ZERO) // Sprites gets drawn over Zero color background no matter the priority
                ) {
                // Render a sprite pixel
                // Check the object palette
                val obpAddress = if(pixelSprite.palette == 0) 0xFF48u else 0xFF49u
                applyPaletteColorToPixel(pixelSprite, obpAddress.toUShort())
                pixelSprite
            } else if(controlRegister.getBgAndWindowEnabled()) {
                // Render a background or window pixel
                // Check background palette
                applyPaletteColorToPixel(pixelBackground, 0xFF47u)
                pixelBackground
            } else {
                Pixel(ColorID.ZERO, 0, 0, false)
            }
            currentLinePixels.add(pixelToBeDrawn)
        }

        state.sharedState.currentLineDotCount += 1
        if (currentLinePixels.size >= 160) {
            // The full line has been generated, we can move on to the next step
            this.state = State.HorizontalBlank(state.sharedState)
            return
        }
        // Continue drawing pixels and reset sprite fetching state if needed
        this.state = State.DrawPixels(state.sharedState, spritesOnTheCurrentLine = spriteList, currentXScanLine = currentLinePixels.size)
    }

    private fun applyPaletteColorToPixel(pixel: Pixel, paletteAddress: UShort) {
        val palette = memory.get(paletteAddress)
        val colorValue = getColorValueFromPixel(pixel, palette)
        pixel.colorValue = colorValue.toInt()
    }
    private fun getColorValueFromPixel(pixel: Pixel, palette: UByte): UInt {
        return when(pixel.colorId) {
            ColorID.ZERO -> (palette and 0b0000_0011u).toUInt()
            ColorID.ONE -> (palette and 0b0000_1100u).toUInt() shr 2
            ColorID.TWO -> (palette and 0b0011_0000u).toUInt() shr 4
            ColorID.THREE -> (palette and 0b1100_0000u).toUInt() shr 6
        }
    }

    // TODO the next three functions are duplicates from PixelFetcher
    private fun getSpriteTileAddress(sprite: Object): UShort {
        // Each tile takes 16 bytes of memory
        return (0x8000u + (sprite.tileIndex * 16u)).toUShort()
    }

    private fun getTileLineData(tileAddress: UShort, lineSprite:UInt): UByte {
        // A sprite takes 2 bytes per line, we can skip to the current line by jumping by a multiple of 2
        return memory.get((tileAddress + 2u * lineSprite).toUShort(), isGPU = true)
    }

    private fun getSpritePixels(tileLowData: UByte, tileHighDate: UByte, backgroundPriority: Boolean, palette: Int): Array<Pixel> {
        val pixelRow = Array(8) { Pixel(ColorID.ZERO, 0, 0, false) }
        for (x in 0..7) {
            // Logic is explained here: https://www.huderlem.com/demos/gameboy2bpp.html
            val first = if (tileHighDate and (1u shl (7 - x)).toUByte() >= 1u) {
                0x10u
            } else {
                0u
            }

            val second = if (tileLowData and (1u shl (7 - x)).toUByte() >= 1u) {
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
            pixelRow[x] = Pixel(colorID, 0, palette, backgroundPriority)
        }
        return pixelRow
    }

    private fun setStatInterrupt() {
        var interrupt = memory.get(0xFF0Fu, isGPU = true)
        interrupt = interrupt or (1u shl 1).toUByte()
        memory.set(0xFF0Fu, interrupt, isGPU = true)
    }

    private fun setStatMode(state: State) {
        val stat = memory.get(0xFF41u, isGPU = true).and(0b1111_1100u) // Reset first 2 bits to facilitate setting the mode after
        val newStat = when (state) {
            is State.OAMScan -> stat.or(0b10u) //Bit 1-0: 10
            is State.DrawPixels -> stat.or(0b11u) //Bit 1-0: 11
            is State.HorizontalBlank -> stat.or(0b00u) //Bit 1-0: 00
            is State.VerticalBlank -> stat.or(0b01u) //Bit 1-0: 01
            is State.Disabled -> throw Exception("State Mode set while PPU is disabled")
        }
        memory.set(0xFF41u, newStat, isGPU = true)
    }

    private fun triggerStatInterruptIfNeeded() {
        val stat = memory.get(0xFF41u, isGPU = true)
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
            // Unlock VRAM
            memory.unlockVRAM()
            memory.unlockOAM()
        }
        if (state.sharedState.currentLineDotCount == 455) {
            // Reached the end of HBlank, go for the next line or VBlank
            state.sharedState.currentLine += 1
            state.sharedState.currentLineDotCount = 0
            val newSharedState = state.sharedState
            // Increment LY counter
            updateLYCounter(state.sharedState.currentLine.toUByte())
            if (state.sharedState.currentLine == 143) {
                this.state = State.VerticalBlank(newSharedState)
            } else {
                this.state = State.OAMScan(newSharedState)
            }
            return
        }

        // Continue HBlank
        state.sharedState.currentLineDotCount += 1
        this.state = State.HorizontalBlank(state.sharedState, state.dotCountInStep + 1)
    }

    private fun updateLYCounter(newLineNumber: UByte) {
        // Update actual LY counter
        memory.set(0xFF44u, newLineNumber, isGPU = true)
        // Compare LYC register
        val lyc = memory.get(0xff45u, isGPU = true)

        // Update Stat register accordingly
        val statRegister = memory.get(0xFF41u)
        val newStatRegister = if (lyc == newLineNumber) {
            statRegister or 0b0000_0100u
        } else {
            statRegister and 0b1111_1011u
        }
        memory.set(0xFF41u, newStatRegister, isGPU = true)

        // Trigger Stat interrupt if needed
        if((lyc == newLineNumber) && newStatRegister and 0b0100_0000u > 0u) {
            // LYC=LY interrupt source is enabled
            setStatInterrupt()
        }
    }

    private fun setVBlankInterrupt() {
//        if(controlRegister.getDisplay()) {
            var interrupt = memory.get(0xFF0Fu, isGPU = true)
            interrupt = interrupt or (1u).toUByte()
            memory.set(0xFF0Fu, interrupt, isGPU = true)
//        }
    }

    private fun verticalBlankState(state: State.VerticalBlank) {
        if (state.sharedState.currentLineDotCount == 0) {
            setStatMode(state)
            // Trigger Vertical blank interrupt on the first dot
            triggerStatInterruptIfNeeded()
            // Also trigger "main" vertical blank interrupt
            setVBlankInterrupt()
        }
        if (state.sharedState.currentLineDotCount == 455) {
            // Reached the end of current line, go for the next line or new frame
            if (state.sharedState.currentLine == 152) {
                var completeFrame: Array<Array<Pixel>> = Array(144) { emptyArray() }
                for (row in 0..143) {
                    completeFrame[row] = state.sharedState.frame[row].toTypedArray()
                }
                frameUpdateListener?.onFrameUpdate(completeFrame)

                // Start a new frame
                state.sharedState.currentLine = 0
                state.sharedState.currentLineDotCount = 0
                state.sharedState.frame = List(160) { ArrayList() }
                this.state = State.OAMScan(state.sharedState)
                // Reset LY counter
                updateLYCounter(0u)
            } else {
                state.sharedState.currentLine += 1
                state.sharedState.currentLineDotCount = 0
                this.state = State.VerticalBlank(state.sharedState)
                // Increment LY counter
                updateLYCounter(state.sharedState.currentLine.toUByte())
            }
            return
        }

        // Continue VBlank
        state.sharedState.currentLineDotCount += 1
        this.state = State.VerticalBlank(state.sharedState)
    }

    private fun generateOAM(): MutableList<Object> {
        val result = mutableListOf<Object>()
        for (objectAddress in 0xFE00u..0xFE9Fu step 4) {
            result.add(Object.ObjectFromMemoryAddress(objectAddress.toUShort(), memory))
        }
        return result
    }


}

data class Object(val yPos: Int, val xPos: Int, val tileIndex:UByte, val attributesFlags:UByte, val baseAddress: UShort){

    fun getSpriteLineForCurrentScanLine(ly: Int, controlRegister: LcdControlRegister): Int {
        val spriteSize = if(controlRegister.getSpriteSizeEnabled()) 16 else 8
        // Current line has to be within the sprite: y pos <= current line <= y pos + (sprite size - 1)
        // However sprites are shifted 16 lines up: Y=0 doesn't show on the screen
       if(yPos - 16 <= ly && ly <= yPos - 16 + (spriteSize - 1)) {
           // Sprite is visible
           if(attributesFlags and 0b0100_0000u >= 1u) {
               // Flip vertically
               return (spriteSize - 1) - (ly - yPos + 16)
           }
           return ly - yPos + 16
       }
        // Sprite is not visible for the current scan line
        return -1
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    companion object {
         fun ObjectFromMemoryAddress(baseAddress: UShort, memory: Memory): Object {
             val yPos: Int = memory.get(baseAddress, isGPU = true).toInt()
             val xPos: Int = memory.get((baseAddress+1u).toUShort(), isGPU = true).toInt()
             val tileIndex:UByte = memory.get((baseAddress+2u).toUShort(), isGPU = true)
             val attributesFlags:UByte = memory.get((baseAddress+3u).toUShort(), isGPU = true)

             return Object(yPos, xPos, tileIndex, attributesFlags, baseAddress)
         }
         fun storeObjectInMemory(obj: Object, memory: Memory) {
             memory.set(obj.baseAddress, obj.yPos.toUByte(), isGPU = true)
             memory.set((obj.baseAddress+1u).toUShort(), obj.xPos.toUByte(), isGPU = true)
             memory.set((obj.baseAddress+2u).toUShort(), obj.tileIndex, isGPU = true)
             memory.set((obj.baseAddress+3u).toUShort(), obj.attributesFlags, isGPU = true)
         }
     }
}

enum class ColorID {
    ZERO, ONE, TWO, THREE;
}

data class Pixel(val colorId: ColorID, var colorValue: Int = 0, val palette: Int, val backgroundPriority:Boolean) {

}

