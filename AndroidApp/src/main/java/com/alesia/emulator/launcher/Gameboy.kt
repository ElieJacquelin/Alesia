package compose

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.scale
import java.nio.ByteBuffer


@Composable
fun Gameboy(modifier: Modifier = Modifier, frame: ByteArray) {
    val bitmap = remember { Bitmap.createBitmap(160, 144, Bitmap.Config.ARGB_8888, false)}
    val buffer = ByteBuffer.wrap(frame)
    bitmap.copyPixelsFromBuffer(buffer)

    Canvas(modifier = modifier) {
        scale(2f) {
            drawImage(bitmap.asImageBitmap(), filterQuality = FilterQuality.None)
        }
    }
}