package com.alesia.emulator.launcher

import Alesia
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.alesia.emulator.AndroidFileParser
import io.Joypad
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalStdlibApi::class, ExperimentalUnsignedTypes::class)
class GameBoyViewModel(private val fileParser: AndroidFileParser, private val alesia: Alesia) : ViewModel() {

    private val _uiState = MutableStateFlow(UIState.Initial)
    val uiState = _uiState.asStateFlow()
    val frame = alesia.frameBitmap

    fun onChooseRom() {
        _uiState.value = UIState.ChooseRom
    }

    fun onRomChosen(romUri: Uri) {
        fileParser.loadRomFromUri(romUri)
        _uiState.value = UIState.Running
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                alesia.runRom()
            }
        }
    }

    fun onDirectionPadChanged(directions: Array<Pair<PadDirection, Boolean>>) {
        for (direction in directions) {
            val (padDirection, pressed) = direction
            val key = when (padDirection) {
                PadDirection.Up -> Joypad.Key.Up
                PadDirection.Down -> Joypad.Key.Down
                PadDirection.Left -> Joypad.Key.Left
                PadDirection.Right -> Joypad.Key.Right
            }
            alesia.handleKeyEvent(key, pressed)
        }
    }

    fun onButtonChanged(button: Button, pressed: Boolean) {
        if (button == Button.FAST_FORWARD) {
            alesia.triggerSpeedMode(pressed)
            return
        }
        val key = when (button) {
            Button.A -> Joypad.Key.A
            Button.B -> Joypad.Key.B
            Button.SELECT -> Joypad.Key.Select
            Button.START -> Joypad.Key.Start
            Button.FAST_FORWARD -> throw Throwable("Unexpected fast forward button")
        }
        alesia.handleKeyEvent(key, pressed)
    }

    fun onScreenStopped() {
        // App is being stopped, pause the rom to store the save to disk
        // OnCleared() isn't called when the user backgrounds the app and removes it from recent, so it's not reliable enough
        if(uiState.value == UIState.Running) {
            alesia.pauseRom()
        }
    }

    fun onScreenStarted() {
        // App is being started, we restart the emulator if it being paused
        if(alesia.isPaused) {
            alesia.unPauseRom()
        }
    }

    enum class UIState {
        Initial, ChooseRom, Running
    }

    companion object {

        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                val application = checkNotNull(extras[APPLICATION_KEY])
                val fileParser = AndroidFileParser(application)
                val alesia = Alesia(fileParser)
                return GameBoyViewModel(fileParser, alesia) as T
            }
        }
    }
}