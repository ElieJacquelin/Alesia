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
    val emulatorScope = rememberCoroutineScope()
    val alesia = remember { Alesia(WebFileParser()) }

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

            Gameboy(alesia)
        }
    }
}

