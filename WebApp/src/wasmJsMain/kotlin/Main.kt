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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.CanvasBasedWindow
import io.Joypad
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.configureWebResources
import org.jetbrains.skiko.currentNanoTime

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
    val alesia = remember { Alesia(WebFileParser()) }
    val frame = alesia.frameBitmap.collectAsState(ByteArray(160 * 144 * 4))
    val counter by alesia.fpsCounter.collectAsState()

    MaterialTheme() {
        var isRunning by remember { mutableStateOf(false) }
        var pressed by remember { mutableStateOf(false) }

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
            Button(onClick = {
                pressed = !pressed
                alesia.handleKeyEvent(Joypad.Key.Start, pressed)
            }) {
                Text ("recompose")
            }
            // Somehow updating the frame object does not trigger a recomposition, so we use that FPS counter to force it
            Text("${counter} FPS")

            Gameboy(frame.value)
        }
    }
}

