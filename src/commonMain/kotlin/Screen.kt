class Screen {
    val controlRegister: Byte = 0
    val pixels = Array(160) {Array<Pixel>(144) { Pixel(0, 0, false) } }
    val scrollX: Int = 0
    val scrollY: Int = 0
    val windowPosX: Int = 0
    val windowPosY: Int = 0
}
// 8x8 pixel, 2 bytes per row, 16 bytes total
class Tile(private val pixels: Array<Array<UByte>>) {
    fun getColorId(x: Int, y: Int):ColorID {
        // Logic is explained here: https://www.huderlem.com/demos/gameboy2bpp.html
        val first = if (pixels[x][1] and (1u shl (7 - y)).toUByte() > 1u) {
            0x10u
        } else {
            0u
        }

        val second = if (pixels[x][0] and (1u shl (7 - y)).toUByte() > 1u) {
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

class Pixel(colorId: Int, palette: Int, backgroundPriority:Boolean) {

}

class Palette(colors: Array<Int>)

sealed class Layer {}

class Background(tileReferenceMap: Array<Int>): Layer()
class Window(): Layer()
class Objects(tileOrStackedTiles: Array<Tile>): Layer()


class LcdControlRegister() {
    private var register: UByte = 0u

    fun setDisplay(enabled: Boolean) {
        register = if(enabled) {
            register or (1u shl 7).toUByte()
        }  else {
            register and (1u shl 7).toUByte()
        }
    }

    fun getDisplay(): Boolean {
        return register.toUInt() shr 7 > 0u
    }

    fun setWindowTileMap(first: Boolean) {
        register = if(first) {
            register or (1u shl 6).toUByte()
        }  else {
            register and (1u shl 6).toUByte()
        }
    }

    fun getWindowTileMap(): Boolean {
        return register.toUInt() shr 6 > 0u
    }

    fun setWindowEnabled(enabled: Boolean) {
        register = if(enabled) {
            register or (1u shl 5).toUByte()
        }  else {
            register and (1u shl 5).toUByte()
        }
    }

    fun getWindowEnabled(): Boolean {
        return register.toUInt() shr 5 > 0u
    }

    fun setBgAndWindowTileData(`8800Mode`: Boolean) {
        register = if(`8800Mode`) {
            register or (1u shl 4).toUByte()
        }  else {
            register and (1u shl 4).toUByte()
        }
    }

    fun getBgAndWindowTileData(): Boolean {
        return register.toUInt() shr 4 > 0u
    }

    fun setBgTileMap(first: Boolean) {
        register = if(first) {
            register or (1u shl 3).toUByte()
        }  else {
            register and (1u shl 3).toUByte()
        }
    }

    fun getBgTileMap(): Boolean {
        return register.toUInt() shr 3 > 0u
    }

    fun setSpriteSizeEnabled(enabled: Boolean) {
        register = if(enabled) {
            register or (1u shl 2).toUByte()
        }  else {
            register and (1u shl 2).toUByte()
        }
    }

    fun getSpriteSizeEnabled(): Boolean {
        return register.toUInt() shr 2 > 0u
    }

    fun setSpriteEnabled(enabled: Boolean) {
        register = if(enabled) {
            register or (1u shl 1).toUByte()
        }  else {
            register and (1u shl 1).toUByte()
        }
    }

    fun getSpriteEnabled(): Boolean {
        return register.toUInt() shr 1 > 0u
    }

    fun setBgAndWindowEnabled(enabled: Boolean) {
        register = if(enabled) {
            register or (1u shl 0).toUByte()
        }  else {
            register and (1u shl 0).toUByte()
        }
    }

    fun getBgAndWindowEnabled(): Boolean {
        return register.toUInt() shr 0 > 0u
    }

}