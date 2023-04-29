import io.Joypad
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import memory.Memory
import java.time.Instant.now


@ExperimentalStdlibApi
@ExperimentalUnsignedTypes
class Alesia(val fileParser: FileParser) {
    val memory = Memory()
    val cpu = CPU(memory)
    val screen = Screen(memory)

    var frameBitmap = callbackFlow {
        val frameCallback = object: Screen.FrameUpdateListener {
            override fun onFrameUpdate(frame: Array<Array<Pixel>>) {
                // Transform frame to Array of RGB-888x for rendering
                val frameRgb = ByteArray(160 * 144 * 4)
                frame.flatten().forEachIndexed { index, pixel ->
                    val pixelIndex = index * 4
                    val (red, green, blue) = when (pixel.colorValue) {
                        0 -> Triple(0b11111111, 0b11111111, 0b11111111) // White
                        1 -> Triple(0b10101001, 0b10101001, 0b10101001) // Light grey
                        2 -> Triple(0b01010100, 0b01010100, 0b01010100) // Dark grey
                        3 -> Triple(0, 0, 0) // Black
                        else -> throw Exception("Wrong color value being used")
                    }

                    frameRgb[pixelIndex] = red.toByte()
                    frameRgb[pixelIndex + 1] = green.toByte()
                    frameRgb[pixelIndex + 2] = blue.toByte()
                    frameRgb[pixelIndex + 3] = 0xff.toByte() // Ignore Alpha
                }

                trySendBlocking(frameRgb)
                shouldSleep = true
            }
        }

        screen.frameUpdateListener = frameCallback
        awaitClose { screen.frameUpdateListener = null }
    }

    var shouldSleep = false
    var clockStart = now()
    var speedMode = false

    init {

    }

    fun stopRom() {
        val saveData = memory.dumpRam()
        if (saveData.isEmpty()) {
            // Prevent saving files for cartridges which does not have battery
            return
        }
        fileParser.writeSave(saveData)
    }

    suspend fun runRom() {
        val rom = fileParser.loadRom()
        memory.loadRom(rom)
        val savedRam = fileParser.loadSave()
        if(savedRam != null) {
           memory.loadRam(savedRam)
        }

        while (true) {
            cpu.tick()
            for (i in 0..3) {
                screen.tick()
            }
            //Blargg test output
            if (memory.get(0xff02u) == 0x81u.toUByte()) {
                val code = memory.get(0xff01u).toInt().toChar()
                print("$code")
                memory.set(0xff02u, 0u)
            }
            if (shouldSleep && !speedMode) {
                val clockEnd = now()
                val sleepDuration = 16 -(clockEnd.toEpochMilli() - clockStart.toEpochMilli())
                if ( sleepDuration> 0){
                    delay(sleepDuration)
                }
                clockStart = clockEnd
                shouldSleep = false
            }
        }

    }




    fun handleKeyEvent(key: Joypad.Key, pressed: Boolean) {
        memory.handleKey(key, pressed)
    }

    fun triggerSpeedMode(pressed: Boolean) {
        speedMode = pressed
    }
}
