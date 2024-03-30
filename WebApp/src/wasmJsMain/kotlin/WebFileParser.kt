import kotlinx.coroutines.await
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import kotlin.js.Promise

@OptIn(ExperimentalUnsignedTypes::class)
class WebFileParser(): FileParser {

    private var loadedRom: UByteArray? = null
    override fun loadRom(): UByteArray {
        return loadedRom!!
    }

    suspend fun openFilePickerAndLoadRom() {
        val romFileHandler = getFileHandlerFromDisk().await<JsAny>()
        val romFile = getFile(romFileHandler).await<JsAny>()
        val buffer = getFileArrayBuffer(romFile).await<JsAny>()
        val byteArray = getIntArray(buffer)
        val result = UByteArray(byteArray.length)
        for (i in 0..<byteArray.length) {
            result[i] = byteArray[i].toUByte()
        }
        loadedRom = result
    }

    override fun loadSave(): UByteArray? {
        return null
    }

    override fun writeSave(save: UByteArray) {

    }
}

fun getIntArray(arrayBuffer: JsAny): Uint8Array = js("new Uint8Array(arrayBuffer)")
fun getFileArrayBuffer(file: JsAny): Promise<JsAny> = js("file.arrayBuffer()")
fun getFile(fileHandle: JsAny): Promise<JsAny> = js("fileHandle[0].getFile()")
fun getFileHandlerFromDisk(): Promise<JsAny> = js("window.showOpenFilePicker()")
