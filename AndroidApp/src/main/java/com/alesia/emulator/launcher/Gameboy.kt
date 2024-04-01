package compose

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import java.nio.ByteBuffer


@Composable
fun Gameboy(modifier: Modifier = Modifier, frame: ByteArray) {
    val bitmap = remember { Bitmap.createBitmap(160, 144, Bitmap.Config.ARGB_8888, false)}
    val buffer = ByteBuffer.wrap(frame)
    bitmap.copyPixelsFromBuffer(buffer)

    Column(modifier.fillMaxSize().background(Color.Black)) {
        Image(bitmap.asImageBitmap(), "Emulator screen", modifier = Modifier.weight(1f).fillMaxWidth(), contentScale = ContentScale.Fit, filterQuality = FilterQuality.None)
    }
}