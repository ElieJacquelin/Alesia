package com.alesia.emulator.launcher

import Alesia
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.alesia.emulator.AndroidFileParser
import compose.Gameboy

@OptIn(ExperimentalStdlibApi::class, ExperimentalUnsignedTypes::class)
@Composable
fun LauncherScreen(fileParser: AndroidFileParser) {
    var isRunning by remember { mutableStateOf(false) }
    val alesia = remember { Alesia(fileParser) }
    val frame = alesia.frameBitmap.collectAsState(ByteArray(160 * 144 * 4))

    Column(Modifier.fillMaxSize().background(Color.LightGray), Arrangement.spacedBy(5.dp)) {
        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val romUri = it.data?.data ?: return@rememberLauncherForActivityResult
            fileParser.loadRomFromUri(romUri)
            isRunning = true
        }
        if(!isRunning) {
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                        .apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            setType("*/*")
                        }
                    launcher.launch(intent)
                }
            ) {
                Text("Load ROM")
            }
        } else {
            LaunchedEffect(true) {
                alesia.runRom()
            }

            Gameboy(frame = frame.value)
        }
    }
}