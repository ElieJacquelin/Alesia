package com.alesia.emulator

import FileParser
import android.content.Context
import android.net.Uri

@OptIn(ExperimentalUnsignedTypes::class)
class AndroidFileParser(val applicationContext: Context): FileParser {
    private var rom: UByteArray? = null
    fun loadRomFromUri(uri: Uri) {
        rom = applicationContext.contentResolver.openInputStream(uri)?.use { inputStream -> inputStream.buffered().readBytes() }?.toUByteArray()
    }
    override fun loadRom(): UByteArray {
        return rom!!
    }

    override fun loadSave(): UByteArray? {
        return null
    }

    override fun writeSave(save: UByteArray) {

    }
}