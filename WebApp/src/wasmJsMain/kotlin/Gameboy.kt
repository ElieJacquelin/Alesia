package compose

import Alesia
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.graphics.drawscope.scale
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo

@Composable
fun Gameboy(modifier: Modifier = Modifier, frame: ByteArray) {
    val bitmap = remember { Bitmap() }
    val info = ImageInfo(160, 144, ColorType.RGB_888X, ColorAlphaType.OPAQUE)
    bitmap.installPixels(info, frame, info.minRowBytes)

    Canvas(modifier = modifier.wrapContentSize()) {
        scale(2f) {
            drawImage(
                bitmap.asComposeImageBitmap(),
                filterQuality = FilterQuality.None
            ) // FilterQuality.None allows pixel-perfect scaling with no antialiasing
        }
    }
}