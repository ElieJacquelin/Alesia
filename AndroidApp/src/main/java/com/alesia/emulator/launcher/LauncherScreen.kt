package com.alesia.emulator.launcher

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import compose.Gameboy

@Composable
fun LauncherScreen(viewModel: GameBoyViewModel = viewModel(factory = GameBoyViewModel.Factory)) {
    val uiState by viewModel.uiState.collectAsState()
    val frame by viewModel.frame.collectAsState(ByteArray(160 * 144 * 4))

    val chooseRomLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val romUri = it.data?.data ?: return@rememberLauncherForActivityResult
        viewModel.onRomChosen(romUri)
    }
    when(uiState) {
        GameBoyViewModel.UIState.Initial, GameBoyViewModel.UIState.ChooseRom ->
            Column(Modifier.fillMaxSize().background(Color.LightGray), Arrangement.spacedBy(5.dp)) {
                Button(
                    onClick = {
                        viewModel.onChooseRom()

                    }
                ) {
                    Text("Load ROM")
                }

                if(uiState == GameBoyViewModel.UIState.ChooseRom) {
                    SideEffect {
                        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                            .apply {
                                addCategory(Intent.CATEGORY_OPENABLE)
                                setType("*/*")
                            }
                        chooseRomLauncher.launch(intent)
                    }
                }
            }

        GameBoyViewModel.UIState.Running -> Gameboy(frame = frame)
    }
}