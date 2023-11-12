@file:OptIn(ExperimentalStdlibApi::class)

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
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.CanvasBasedWindow
import compose.Gameboy
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.configureWebResources
import org.jetbrains.compose.resources.urlResource


val alesia = Alesia(WebFileParser())
@ExperimentalStdlibApi
@ExperimentalUnsignedTypes
@ExperimentalComposeUiApi
@ExperimentalResourceApi
fun main()  {
    configureWebResources {
        // same as default - this is not necessary to add here. It's here to show this feature
        setResourceFactory { urlResource("./$it") }
    }

    CanvasBasedWindow("Alesia", canvasElementId = "alesiaCanvas") {
        App()
    }

//    Window(
//        onCloseRequest = {
//            emulatorScope.cancel()
//            alesia.stopRom()
//            exitApplication()
//        },
//        title = "Alesia",
//        state = rememberWindowState(width = 600.dp, height = 600.dp),
//        onKeyEvent = {
//            val pressed = it.type == KeyEventType.KeyDown
//            val key = if (it.key == Key.Z) {
//                Joypad.Key.A
//            } else if (it.key == Key.X) {
//                Joypad.Key.B
//            } else if (it.key == Key.Enter) {
//                Joypad.Key.Start
//            } else if (it.key == Key.ShiftRight) {
//                Joypad.Key.Select
//            } else if (it.key == Key.DirectionUp) {
//                Joypad.Key.Up
//            } else if (it.key == Key.DirectionDown) {
//                Joypad.Key.Down
//            } else if (it.key == Key.DirectionLeft) {
//                Joypad.Key.Left
//            } else if (it.key == Key.DirectionRight) {
//                Joypad.Key.Right
//            } else if (it.key == Key.Spacebar) {
//                alesia.triggerSpeedMode(pressed)
//                return@Window true
//            } else {
//                // Unknown key, abort here
//                return@Window true
//            }
//            alesia.handleKeyEvent(key, pressed)
//            true
//        }
//    ) {
}

@Composable
fun App() {
    val emulatorScope = rememberCoroutineScope()

    MaterialTheme() {
        var isRunning by remember { mutableStateOf(false) }

        Column(Modifier.fillMaxSize().background(Color.LightGray), Arrangement.spacedBy(5.dp)) {
            Button(modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .onPreviewKeyEvent { true }, // Disable keyboard actions which would trigger onClick
                enabled = !isRunning,
                onClick = {
                    isRunning = true
                    emulatorScope.launch {
                        alesia.runRom()
                    }
                }) {
                Text("Start")
            }

//            Gameboy(alesia)
        }
    }
}

