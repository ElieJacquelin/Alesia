package com.alesia.emulator

import FileParser
import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.FileNotFoundException
import kotlin.contracts.contract

@OptIn(ExperimentalUnsignedTypes::class)
class AndroidFileParser(private val applicationContext: Context): FileParser {
    private var rom: UByteArray? = null
    fun loadRomFromUri(uri: Uri) {
        rom = applicationContext.contentResolver.openInputStream(uri)?.use { inputStream -> inputStream.buffered().readBytes() }?.toUByteArray()
    }
    override fun loadRom(): UByteArray {
        return rom!!
    }

    override fun loadSave(): UByteArray? {
        return try {
            applicationContext.openFileInput(getFileSaveName()).use {
                it.readBytes().toUByteArray()
            }
        } catch (error: FileNotFoundException) {
            null
        }
    }

    override fun writeSave(save: UByteArray) {
        applicationContext.openFileOutput(getFileSaveName(), 0).use {
            it.write(save.toByteArray())
        }
    }

    private fun getFileSaveName(): String {
        val romCopy = rom ?: return ""
        // Get file save from the name stored in the ROM itself
        val romName = String(romCopy.sliceArray(0x134..0x143).toByteArray(), Charsets.US_ASCII)
            .lowercase()
            .filter { it.isLetterOrDigit() } // Remove non-alphanumeric characters (e.g. null characters if the rom title doesn't take all available space)
        return "${romName}.sav"
    }
}