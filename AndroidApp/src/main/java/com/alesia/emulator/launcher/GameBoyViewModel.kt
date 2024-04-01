package com.alesia.emulator.launcher

import Alesia
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.alesia.emulator.AndroidFileParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalStdlibApi::class, ExperimentalUnsignedTypes::class)
class GameBoyViewModel(private val fileParser: AndroidFileParser, private val alesia: Alesia): ViewModel() {

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