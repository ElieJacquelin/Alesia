@file:OptIn(ExperimentalStdlibApi::class)

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.CanvasBasedWindow
import compose.Gameboy
import io.Joypad
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.configureWebResources

@ExperimentalStdlibApi
@ExperimentalUnsignedTypes
@ExperimentalComposeUiApi
@ExperimentalResourceApi
fun main()  {
    configureWebResources {
        // same as default - this is not necessary to add here. It's here to show this feature
        resourcePathMapping { path -> "./$path" }
    }

    CanvasBasedWindow("Alesia", canvasElementId = "alesiaCanvas") {
        App()
    }
}

@OptIn(ExperimentalUnsignedTypes::class, ExperimentalStdlibApi::class)
@Composable
fun App() {
    val emulatorScope = rememberCoroutineScope { Dispatchers.Default }
    val fileParser = remember { WebFileParser() }
    val alesia = remember { Alesia(fileParser) }
    val frame = alesia.frameBitmap.collectAsState(ByteArray(160 * 144 * 4))
    val counter by alesia.fpsCounter.collectAsState()

    MaterialTheme() {
        var isRunning by remember { mutableStateOf(false) }
        val focusRequester = remember { FocusRequester() }

        Column(Modifier.fillMaxSize().background(Color.LightGray), Arrangement.spacedBy(5.dp)) {
            Button(modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .onPreviewKeyEvent { true }, // Disable keyboard actions which would trigger onClick
                enabled = !isRunning,
                onClick = {
                    isRunning = true

                    emulatorScope.launch {
                        fileParser.openFilePickerAndLoadRom()
                        alesia.runRom()
                    }
                    focusRequester.requestFocus()
                }) {
                Text("Start")
            }
            // Somehow updating the frame object does not trigger a recomposition, so we use that FPS counter to force it
            Text("${counter} FPS")

            Gameboy(modifier = Modifier
                .onKeyEvent {
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
                    } else if (it.key == Key.Spacebar) {
                        alesia.triggerSpeedMode(pressed)
                        return@onKeyEvent true
                    } else {
                        // Unknown key, abort here
                        return@onKeyEvent true
                    }
                    alesia.handleKeyEvent(key, pressed)
                    true
                }
                .focusable(true).focusRequester(focusRequester).focusTarget(),frame.value)
        }
    }
}

