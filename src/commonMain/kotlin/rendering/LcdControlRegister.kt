package rendering

class LcdControlRegister() {
    private var register: UByte = 0u

    fun setDisplay(enabled: Boolean) {
        register = if(enabled) {
            register or (1u shl 7).toUByte()
        }  else {
            register and (1u shl 7).toUByte().inv()
        }
    }

    fun getDisplay(): Boolean {
        return register.toUInt() shr 7 > 0u
    }

    fun setWindowTileMap(first: Boolean) {
        register = if(first) {
            register or (1u shl 6).toUByte()
        }  else {
            register and (1u shl 6).toUByte().inv()
        }
    }

    fun getWindowTileMap(): Boolean {
        return register.toUInt() shr 6 > 0u
    }

    fun setWindowEnabled(enabled: Boolean) {
        register = if(enabled) {
            register or (1u shl 5).toUByte()
        }  else {
            register and (1u shl 5).toUByte().inv()
        }
    }

    fun getWindowEnabled(): Boolean {
        return register.toUInt() shr 5 > 0u
    }

    fun setBgAndWindowTileDataAddressingMode(`8000Mode`: Boolean) {
        register = if(`8000Mode`) {
            register or (1u shl 4).toUByte()
        }  else {
            register and (1u shl 4).toUByte().inv()
        }
    }

    fun getBgAndWindowTileDataAddressingMode(): Boolean {
        return register.toUInt() shr 4 > 0u
    }

    fun setBgTileMap(first: Boolean) {
        register = if(first) {
            register or (1u shl 3).toUByte()
        }  else {
            register and (1u shl 3).toUByte().inv()
        }
    }

    fun getBgTileMap(): Boolean {
        return register.toUInt() shr 3 > 0u
    }

    fun setSpriteSizeEnabled(enabled: Boolean) {
        register = if(enabled) {
            register or (1u shl 2).toUByte()
        }  else {
            register and (1u shl 2).toUByte().inv()
        }
    }

    fun getSpriteSizeEnabled(): Boolean {
        return register.toUInt() shr 2 > 0u
    }

    fun setSpriteEnabled(enabled: Boolean) {
        register = if(enabled) {
            register or (1u shl 1).toUByte()
        }  else {
            register and (1u shl 1).toUByte().inv()
        }
    }

    fun getSpriteEnabled(): Boolean {
        return register.toUInt() shr 1 > 0u
    }

    fun setBgAndWindowEnabled(enabled: Boolean) {
        register = if(enabled) {
            register or (1u shl 0).toUByte()
        }  else {
            register and (1u shl 0).toUByte().inv()
        }
    }

    fun getBgAndWindowEnabled(): Boolean {
        return register.toUInt() shr 0 > 0u
    }

}