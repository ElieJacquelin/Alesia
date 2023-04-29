import okio.FileSystem
import okio.Path
import okio.buffer

@OptIn(ExperimentalUnsignedTypes::class)
class JvmFileParser(val romPath: Path, val savePath: Path): FileParser {

    private fun loadFile(fileSystem: FileSystem, path: Path): UByteArray {
        val metadata = fileSystem.metadataOrNull(path) ?: return UByteArray(0)
        val romSize = metadata.size
        val rom = ByteArray(romSize!!.toInt())
        val buffer = fileSystem.source(path).buffer()
        buffer.readFully(rom)

        buffer.close()
        return rom.toUByteArray()
    }

    override fun loadRom(): UByteArray {
        return loadFile(FileSystem.SYSTEM, romPath)
    }

    override fun loadSave(): UByteArray? {
        val save = loadFile(FileSystem.SYSTEM, savePath)
        if(save.isEmpty()) {
            return null
        }
        return save
    }

    override fun writeSave(save: UByteArray) {
        val saveFile = FileSystem.SYSTEM.openReadWrite(savePath, mustCreate = false, mustExist = false)
        saveFile.resize(0L) // Delete any existing data
        // Write save data to file
        saveFile.write(0L, save.toByteArray(), 0, save.size)
    }
}