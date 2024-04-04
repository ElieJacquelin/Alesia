package compose

import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alesia.emulator.launcher.Button
import com.alesia.emulator.launcher.Buttons
import com.alesia.emulator.launcher.DirectionPad
import com.alesia.emulator.launcher.PadDirection
import java.nio.ByteBuffer


@Composable
fun Gameboy(
    modifier: Modifier = Modifier, frame: ByteArray?,
    onDirectionChange: (direction: Array<Pair<PadDirection, Boolean>>) -> Unit,
    onButtonChange: (button: Button, pressed: Boolean) -> Unit
) {
    val bitmap = remember { Bitmap.createBitmap(160, 144, Bitmap.Config.ARGB_8888, false) }
    if (frame != null) {
        val buffer = ByteBuffer.wrap(frame)
        bitmap.copyPixelsFromBuffer(buffer)
    }

    val (buttonsPaddingX, buttonPaddingY) = if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        Pair(40, 60)
    } else {
        Pair(16, 59)
    }
    val modifierImage = if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        Modifier.fillMaxHeight()
    } else {
        Modifier.fillMaxWidth()
    }
    val scaleImage = if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        ContentScale.FillHeight
    } else {
        ContentScale.FillWidth
    }

    Box(modifier.fillMaxSize().background(Color.Black)) {
        Image(
            bitmap.asImageBitmap(),
            "Emulator screen",
            modifier = modifierImage.align(Alignment.TopCenter),
            contentScale = scaleImage,
            filterQuality = FilterQuality.None
        )
        DirectionPad(onDirectionChange,
            modifier = Modifier.alpha(0.6f).align(Alignment.BottomStart)
                .padding(start = buttonsPaddingX.dp, bottom = buttonPaddingY.dp)
        )
        Buttons(onButtonChange,
            modifier = Modifier.alpha(0.6f).align(Alignment.BottomEnd)
                .padding(end = buttonsPaddingX.dp, bottom = buttonPaddingY.dp)
        )
    }
}

@Preview
@Composable
fun PreviewDPad() {
    Gameboy(frame = null, onDirectionChange = { _ -> }, onButtonChange = { _, _ -> })
}

@Preview(device = "spec:width=411dp,height=891dp,dpi=420,isRound=false,chinSize=0dp,orientation=landscape")
@Composable
fun PreviewDPadLandscape() {
    Gameboy(frame = null, onDirectionChange = { _ -> }, onButtonChange = { _, _ -> })
}