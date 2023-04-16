import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.Joypad
import kotlinx.coroutines.delay
import memory.Memory
import okio.FileSystem
import okio.Path
import okio.buffer
import java.time.Instant.now


@ExperimentalStdlibApi
@ExperimentalUnsignedTypes
class Alesia: Screen.FrameUpdateListener {
    var frameBitmap by mutableStateOf(ByteArray(160 * 144 * 4))

    val memory = Memory()
    val cpu = CPU(memory)
    val screen = Screen(memory).apply { frameUpdateListener = this@Alesia }

    var shouldSleep = false
    var clockStart = now()

    init {

    }

    suspend fun runRom(fileSystem: FileSystem, path: Path) {
        val rom = loadRom(fileSystem, path)
        memory.loadRom(rom)

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
            if (shouldSleep) {
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

    private fun loadRom(fileSystem: FileSystem, path: Path): UByteArray {

        val romSize = fileSystem.metadata(path).size
        val rom = ByteArray(romSize!!.toInt())
        val buffer = fileSystem.source(path).buffer()
        buffer.readFully(rom)

        buffer.close()
        return rom.toUByteArray()
    }


    fun handleKeyEvent(key: Joypad.Key, pressed: Boolean) {
        memory.handleKey(key, pressed)
    }

    override fun onFrameUpdate(frame: Array<Array<Pixel>>) {
        // Transform frame to Array of RGB-888x for rendering
        val frameRgb = ByteArray(160 * 144 * 4)
        frame.flatten().forEachIndexed { index, pixel ->
            val pixelIndex = index * 4
            val (red, green, blue) = when (pixel.colorId) {
                ColorID.ZERO -> Triple(0b11111111, 0b11111111, 0b11111111) // White
                ColorID.ONE -> Triple(0b10101001, 0b10101001, 0b10101001) // Light grey
                ColorID.TWO -> Triple(0b01010100, 0b01010100, 0b01010100) // Dark grey
                ColorID.THREE -> Triple(0, 0, 0) // Black
            }

            frameRgb[pixelIndex] = red.toByte()
            frameRgb[pixelIndex + 1] = green.toByte()
            frameRgb[pixelIndex + 2] = blue.toByte()
            frameRgb[pixelIndex + 3] = 0xff.toByte() // Ignore Alpha
        }

        this.frameBitmap = frameRgb
        shouldSleep = true
    }
}
