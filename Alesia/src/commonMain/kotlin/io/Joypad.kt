package io

class Joypad {

    enum class Key {
        A, B, Start, Select, Up, Down, Left, Right
    }

    internal var aKeyPressed = false
    internal var bKeyPressed = false
    internal var startKeyPressed = false
    internal var selectKeyPressed = false
    internal var upKeyPressed = false
    internal var downKeyPressed = false
    internal var leftKeyPressed = false
    internal var rightKeyPressed = false

    fun handleKey(key: Key, pressed: Boolean) {
        when (key) {
            Key.A -> aKeyPressed = pressed
            Key.B -> bKeyPressed = pressed
            Key.Start -> startKeyPressed = pressed
            Key.Select -> selectKeyPressed = pressed
            Key.Up -> upKeyPressed = pressed
            Key.Down -> downKeyPressed = pressed
            Key.Left -> leftKeyPressed = pressed
            Key.Right -> rightKeyPressed = pressed
        }
    }

    fun generateJoypadValue(joypadControl: UByte): UByte {
        val isProbingActionButton = ((joypadControl.toUInt() shr 5) and 1u) == 0u
        val isProbingDirectionButton = ((joypadControl.toUInt() shr 4) and 1u) == 0u
        if(isProbingActionButton){
            return joypadControl or (if(startKeyPressed) 0u else 0b1000u) or (if(selectKeyPressed) 0u else 0b100u) or (if(bKeyPressed) 0u else 0b10u) or (if(aKeyPressed) 0u else 1u)
        } else if (isProbingDirectionButton) {
            return joypadControl or (if(downKeyPressed) 0u else 0b1000u) or (if(upKeyPressed) 0u else 0b100u) or (if(leftKeyPressed) 0u else 0b10u) or (if(rightKeyPressed) 0u else 1u)
        }
        return 0xFFu // Release all buttons by default
    }
}