package rendering

import ColorID
import LcdControlRegister
import Memory
import Pixel

@ExperimentalUnsignedTypes
class PixelFetcher(private val controlRegister: LcdControlRegister, private val memory: Memory, private val backgroundFiFo: ArrayDeque<Pixel>, internal var LY: Int) {

    internal sealed class State {
        object GetTile : State()
        data class GetTileDataLow(val tileID: UByte): State()
        data class GetTileDataHigh(val tileID: UByte, val tileLowData: UByte): State()
        data class Push(val tileLowData: UByte, val tileHighData: UByte): State()
    }

    // The pixel fetcher goes through each tile in the tile map and return the next 8 pixel to be displayed
    private var currentTileMapOffset = 0u
    internal var state: State = State.GetTile

    fun tick() {
        when (state) {
            is State.GetTile -> getTileStep()
            is State.GetTileDataLow -> getTileDataLowStep((state as State.GetTileDataLow).tileID)
            is State.GetTileDataHigh -> getTileDataHighStep((state as State.GetTileDataHigh).tileID, (state as State.GetTileDataHigh).tileLowData)
            is State.Push -> pushToFifoStep((state as State.Push).tileLowData, (state as State.Push).tileHighData)
        }
    }

    private fun getTileStep() {
        // TODO handle scrolling
        val mapTile = if (controlRegister.getBgTileMap()) 0x9C00u else 0x9800u
        val tileID =  memory.get((mapTile + currentTileMapOffset).toUShort())

        state = State.GetTileDataLow(tileID)
    }

    private fun getTileDataLowStep(tileID: UByte) {
        val tileLowData = getTileDataStep(tileID, true)
        state = State.GetTileDataHigh(tileID, tileLowData)
    }

    private fun getTileDataHighStep(tileID: UByte, tileLowData: UByte) {
        val tileHighData = getTileDataStep(tileID, false)

        state = State.Push(tileLowData, tileHighData)
        pushToFifoStep(tileLowData, tileHighData) // This step does an extra push without waiting for the next tick
    }

    private fun getTileDataStep(tileID: UByte, low: Boolean): UByte {
        var tileAddress = getTileAddress(tileID)
        if (!low) {
            tileAddress ++
        }
        // The screen renders a matrix of 8x8 sprites, getting the modulo by 8 of the current line being rendered tells you which line of the current sprite is being drawn
        val currentSpriteLine = LY.mod(8).toUInt()
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
        return memory.get((tileAddress + 2u * lineSprite).toUShort())
    }

    private fun pushToFifoStep(tileLowData: UByte, tileHighData: UByte) {
        if(!backgroundFiFo.isEmpty()) {
            // Stay in the same step until it succeed
            return
        }

        val pixelRow = Array(8) { Pixel(ColorID.ZERO, 0, false) }
        for (x in 0..7) {
            // Logic is explained here: https://www.huderlem.com/demos/gameboy2bpp.html
            val first = if (tileHighData and (1u shl (7 - x)).toUByte() >= 1u) {
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
            pixelRow[x] = Pixel(colorID, 0, false)
        }

        // Push to Fifo
        backgroundFiFo.addAll(pixelRow)
        // Move to the next tile to be rendered
        currentTileMapOffset = if(currentTileMapOffset == 31u) 0u else currentTileMapOffset++

        state = State.GetTile
    }
}