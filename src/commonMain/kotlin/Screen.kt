class Screen {
    val controlRegister: Byte = 0
    val pixels = Array(256) {Array<Pixel>(256) { Pixel(0, 0, false) } }
    val scrollX: Int = 0
    val scrollY: Int = 0
    val windowPosX: Int = 0
    val windowPosY: Int = 0
}


class Pixel(color: Int, palette: Int, backgroundPriority:Boolean) {

}

@ExperimentalUnsignedTypes
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