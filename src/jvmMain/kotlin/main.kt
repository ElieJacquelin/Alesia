import okio.FileSystem
import okio.Path.Companion.toPath

@ExperimentalStdlibApi
@ExperimentalUnsignedTypes
fun main() {
    val alesia = Alesia()
    alesia.runRom(FileSystem.SYSTEM, "C:\\Users\\eliej\\Downloads\\02-interrupts.gb".toPath())
}
