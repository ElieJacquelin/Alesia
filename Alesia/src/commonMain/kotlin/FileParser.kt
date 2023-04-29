@OptIn(ExperimentalUnsignedTypes::class)
interface FileParser {

    fun loadRom(): UByteArray
    fun loadSave(): UByteArray?

    fun writeSave(save: UByteArray)
}