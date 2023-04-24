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
    var speedMode = false
    private var savePath: Path? = null

    init {

    }

    fun stopRom(fileSystem: FileSystem) {
        if(savePath == null) {
            return
        }
        val saveData = memory.dumpRam()
        if (saveData.isEmpty()) {
            // Prevent saving files for cartridges which does not have battery
            return
        }
        val saveFile = fileSystem.openReadWrite(savePath!!, mustCreate = false, mustExist = false)
        saveFile.resize(0L) // Delete any existing data
        // Write save data to file
        saveFile.write(0L, saveData.toByteArray(), 0, saveData.size)
    }

    suspend fun runRom(fileSystem: FileSystem, romPath: Path, savePath: Path) {
        if (!fileSystem.exists(romPath)) {
            throw Exception("ROM file doesn't exists")
        }
        this.savePath = savePath
        val rom = loadFile(fileSystem, romPath)
        memory.loadRom(rom)
        val savedRam = loadFile(fileSystem, savePath)
        if(savedRam.isNotEmpty()) {
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

    private fun loadFile(fileSystem: FileSystem, path: Path): UByteArray {
        val metadata = fileSystem.metadataOrNull(path) ?: return UByteArray(0)
        val romSize = metadata.size
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

        this.frameBitmap = frameRgb
        shouldSleep = true
    }

    fun triggerSpeedMode(pressed: Boolean) {
        speedMode = pressed
    }
}
