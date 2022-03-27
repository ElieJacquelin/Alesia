import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import compose.Gameboy
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import okio.FileSystem
import okio.Path.Companion.toPath
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Image

@ExperimentalStdlibApi
@ExperimentalUnsignedTypes
//fun main() {
//    val alesia = Alesia()
//    // Missing 2, 9
//    alesia.runRom(FileSystem.SYSTEM, "C:\\Users\\eliej\\Downloads\\gb\\instr_timing.gb".toPath())
//}

val alesia = Alesia()

@ExperimentalStdlibApi
@ExperimentalUnsignedTypes
fun main() = application {
    val emulatorScope = rememberCoroutineScope()

    Window(
        onCloseRequest = {
            exitApplication()
            emulatorScope.cancel()
        },
        title = "Alesia",
        state = rememberWindowState(width = 300.dp, height = 300.dp)
    ) {
        MaterialTheme {
            Column(Modifier.fillMaxSize(), Arrangement.spacedBy(5.dp)) {
                Button(modifier = Modifier.align(Alignment.CenterHorizontally),
                    onClick = {
                        emulatorScope.launch(newSingleThreadContext("emulator")) {
                            alesia.runRom(FileSystem.SYSTEM, "C:\\Users\\eliej\\Downloads\\gb\\tetris.gb".toPath())
                        }
                    }) {
                    Text("Start")
                }

                Gameboy(alesia)
            }
        }
    }
}