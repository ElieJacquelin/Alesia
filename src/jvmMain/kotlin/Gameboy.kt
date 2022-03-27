package compose

import Alesia
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.unit.dp
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo

@OptIn(ExperimentalStdlibApi::class, ExperimentalUnsignedTypes::class)
@Composable
fun Gameboy(alesia: Alesia) {
    val frame = alesia.frameBitmap
    val bitmap = remember { Bitmap() }
    val info = ImageInfo(160, 144, ColorType.RGB_888X, ColorAlphaType.OPAQUE)
    bitmap.installPixels(info, frame, info.minRowBytes)

    Canvas(modifier = Modifier.size(width = 160.dp, height = 144.dp)) {
        drawImage(bitmap.asComposeImageBitmap())
    }
}