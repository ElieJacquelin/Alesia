package rendering

import memory.Memory

class LcdControlRegister(val memory: Memory) {

    private var register: UByte
    get() = memory.get(0xFF40u)
    set(value) = memory.set(0xFF40u, value)

    fun setDisplay(enabled: Boolean) {
        register = if(enabled) {
            register or (1u shl 7).toUByte()
        }  else {
            register and (1u shl 7).toUByte().inv()
        }
    }

    fun getDisplay(): Boolean {
        return (register.toUInt() shr 7) and 1u > 0u
    }

    fun setWindowTileMap(first: Boolean) {
        register = if(first) {
            register or (1u shl 6).toUByte()
        }  else {
            register and (1u shl 6).toUByte().inv()
        }
    }

    fun getWindowTileMap(): Boolean {
        return (register.toUInt() shr 6) and 1u > 0u
    }

    fun setWindowEnabled(enabled: Boolean) {
        register = if(enabled) {
            register or (1u shl 5).toUByte()
        }  else {
            register and (1u shl 5).toUByte().inv()
        }
    }

    fun getWindowEnabled(): Boolean {
        return (register.toUInt() shr 5) and 1u > 0u
    }

    fun setBgAndWindowTileDataAddressingMode(`8000Mode`: Boolean) {
        register = if(`8000Mode`) {
            register or (1u shl 4).toUByte()
        }  else {
            register and (1u shl 4).toUByte().inv()
        }
    }

    fun getBgAndWindowTileDataAddressingMode(): Boolean {
        return (register.toUInt() shr 4) and 1u > 0u
    }

    fun setBgTileMap(first: Boolean) {
        register = if(first) {
            register or (1u shl 3).toUByte()
        }  else {
            register and (1u shl 3).toUByte().inv()
        }
    }

    fun getBgTileMap(): Boolean {
        return (register.toUInt() shr 3) and 1u > 0u
    }

    fun setSpriteSizeEnabled(enabled: Boolean) {
        register = if(enabled) {
            register or (1u shl 2).toUByte()
        }  else {
            register and (1u shl 2).toUByte().inv()
        }
    }

    fun getSpriteSizeEnabled(): Boolean {
        return (register.toUInt() shr 2) and 1u > 0u
    }

    fun setSpriteEnabled(enabled: Boolean) {
        register = if(enabled) {
            register or (1u shl 1).toUByte()
        }  else {
            register and (1u shl 1).toUByte().inv()
        }
    }

    fun getSpriteEnabled(): Boolean {
        return (register.toUInt() shr 1) and 1u > 0u
    }

    fun setBgAndWindowEnabled(enabled: Boolean) {
        register = if(enabled) {
            register or (1u shl 0).toUByte()
        }  else {
            register and (1u shl 0).toUByte().inv()
        }
    }

    fun getBgAndWindowEnabled(): Boolean {
        return (register.toUInt() shr 0) and 1u > 0u
    }

}