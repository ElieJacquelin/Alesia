import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import compose.Gameboy
import io.Joypad
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import okio.FileSystem
import okio.Path.Companion.toPath

@ExperimentalStdlibApi
@ExperimentalUnsignedTypes
//fun main() {
//    val alesia = Alesia()
//    // Missing 2, 9
//    alesia.runRom(FileSystem.SYSTEM, "C:\\Users\\eliej\\Downloads\\gb\\instr_timing.gb".toPath())
//}

@OptIn(ExperimentalStdlibApi::class)
val alesia = Alesia()

@OptIn(ExperimentalComposeUiApi::class)
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
        state = rememberWindowState(width = 600.dp, height = 600.dp),
        onKeyEvent = {
            val pressed = it.type == KeyEventType.KeyDown
            val key = if (it.key == Key.Z) {
                Joypad.Key.A
            } else if (it.key == Key.X) {
                Joypad.Key.B
            } else if (it.key == Key.Enter) {
                Joypad.Key.Start
            } else if (it.key == Key.ShiftRight) {
                Joypad.Key.Select
            } else if (it.key == Key.DirectionUp) {
                Joypad.Key.Up
            } else if (it.key == Key.DirectionDown) {
                Joypad.Key.Down
            } else if (it.key == Key.DirectionLeft) {
                Joypad.Key.Left
            } else if (it.key == Key.DirectionRight) {
                Joypad.Key.Right
            } else {
                // Unknown key, abort here
                return@Window true
            }
            alesia.handleKeyEvent(key, pressed)
            true
        }
    ) {
        MaterialTheme() {
            var isRunning by remember { mutableStateOf(false) }

            Column(Modifier.fillMaxSize().background(Color.LightGray), Arrangement.spacedBy(5.dp)) {
                Button(modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .onPreviewKeyEvent { true }, // Disable keyboard actions which would trigger onClick
                    enabled = !isRunning,
                    onClick = {
                        isRunning = true
                        emulatorScope.launch(newSingleThreadContext("emulator")) {
                            alesia.runRom(FileSystem.SYSTEM, "C:\\Users\\eliej\\Downloads\\gb\\Tetris.gb".toPath())
                        }
                    }) {
                    Text("Start")
                }

                Gameboy(alesia)
            }
        }
    }
}