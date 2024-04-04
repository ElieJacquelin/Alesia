package com.alesia.emulator.launcher

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.alesia.emulator.R
import kotlin.math.atan2


@Composable
fun DirectionPad(onDirectionChange: (direction: Array<Pair<PadDirection, Boolean>>) -> Unit, modifier: Modifier = Modifier) {
    // Get pad dimensions so we can use them for calculating where the user press
    var padHeight by remember { mutableStateOf(0) }
    var padWidth by remember { mutableStateOf(0) }
    var upPressed by remember { mutableStateOf(false) }
    var downPressed by remember { mutableStateOf(false) }
    var leftPressed by remember { mutableStateOf(false) }
    var rightPressed by remember { mutableStateOf(false) }

    Box(modifier = modifier
        .size(152.dp)
        .onGloballyPositioned { layoutCoordinates ->
            padHeight = layoutCoordinates.size.height
            padWidth = layoutCoordinates.size.width
        }
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()

                    val inputChange = event.changes.first()
                    val inputPosition = inputChange.position

                    if(event.type == PointerEventType.Release && !inputChange.pressed) { // If two fingers are pressed and one is released, `pressed` would still be true
                        // User is releasing the all pointers, we release all directions
                        rightPressed = false
                        downPressed = false
                        leftPressed = false
                        upPressed = false

                        onDirectionChange(arrayOf(
                            Pair(PadDirection.Right, rightPressed),
                            Pair(PadDirection.Down, downPressed),
                            Pair(PadDirection.Left, leftPressed),
                            Pair(PadDirection.Up, upPressed)
                        ))
                    }else if (event.type == PointerEventType.Press || event.type == PointerEventType.Move) {
                        // Find the angle from the center of where the user has touched the pad
                        val padCenterX = padWidth / 2
                        val padCenterY = padHeight / 2
                        val anglePositionOfTouch = Math.toDegrees(
                            atan2((inputPosition.y - padCenterY).toDouble(),
                                (inputPosition.x - padCenterX).toDouble())
                        )
                        // Split evenly the pad for each direction and combination of direction: -22,5 +22,5 => Right
                        var newRightPressed = false
                        var newDownPressed = false
                        var newLeftPressed = false
                        var newUpPressed = false
                        when(anglePositionOfTouch) {
                            in -22.5..22.5 -> {
                                newRightPressed = true
                            }
                            in 22.5..67.5 -> {
                                newRightPressed = true
                                newDownPressed = true
                            }
                            in 67.5..112.5 -> {
                                newDownPressed = true
                            }
                            in 112.5..157.5 -> {
                                newDownPressed = true
                                newLeftPressed = true
                            }
                            in -67.5..-22.5 -> {
                                newUpPressed = true
                                newRightPressed = true
                            }
                            in -112.5..-67.5 -> {
                                newUpPressed = true
                            }
                            in -157.5..-112.5 -> {
                                newLeftPressed = true
                                newUpPressed = true
                            }
                            else -> {
                                newLeftPressed = true
                            }
                        }
                        if(newRightPressed != rightPressed || newDownPressed != downPressed || newLeftPressed != leftPressed || newUpPressed != upPressed) {
                            // Update state and notify of change
                            rightPressed = newRightPressed
                            downPressed = newDownPressed
                            leftPressed = newLeftPressed
                            upPressed = newUpPressed

                            onDirectionChange(arrayOf(
                                Pair(PadDirection.Right, rightPressed),
                                Pair(PadDirection.Down, downPressed),
                                Pair(PadDirection.Left, leftPressed),
                                Pair(PadDirection.Up, upPressed)
                            ))
                        }
                    }
                }
            }
        }
    ) {
        SingleDirection(PadDirection.Up, buttonPressed = upPressed, modifier = Modifier.align(Alignment.TopCenter))
        SingleDirection(PadDirection.Down, buttonPressed = downPressed, modifier = Modifier.align(Alignment.BottomCenter))
        SingleDirection(PadDirection.Left, buttonPressed = leftPressed, modifier = Modifier.align(Alignment.CenterStart))
        SingleDirection(PadDirection.Right, buttonPressed = rightPressed, modifier = Modifier.align(Alignment.CenterEnd))
    }
}

@Composable
private fun SingleDirection(direction: PadDirection, modifier: Modifier, buttonPressed: Boolean) {
    val (rotation, offset) = remember {
        when(direction) {
            PadDirection.Up -> Pair(0f, 4)
            PadDirection.Down -> Pair(180f, 4)
            PadDirection.Left -> Pair(-90f, 4)
            PadDirection.Right -> Pair(90f, 4)
        }
    }
    val buttonImage = if(buttonPressed) {
        R.drawable.direction_pressed
    } else {
        R.drawable.direction
    }

    Image(
        painterResource(buttonImage), direction.buttonDescription, modifier = modifier
        .size(71.dp)
        .rotate(rotation)
        .absoluteOffset(y= offset.dp)
    )
}

enum class PadDirection(val buttonDescription: String) {
    Up("Up button"), Down("Down button"), Left("Left button"), Right("Right button")
}

@Composable
fun Buttons(onButtonChange: (button: Button, pressed: Boolean) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(40.dp)) {
        SingleButton(Button.FAST_FORWARD,
            modifier = Modifier.align(Alignment.End).padding(end = 13.dp),
            onButtonChange = { pressed -> onButtonChange(Button.FAST_FORWARD, pressed) }
        )
        Row(modifier = Modifier.padding(end = 5.dp),horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SingleButton(Button.SELECT,
                modifier = Modifier,
                onButtonChange = { pressed -> onButtonChange(Button.SELECT, pressed) }
            )
            SingleButton(Button.START,
                modifier = Modifier,
                onButtonChange = { pressed -> onButtonChange(Button.START, pressed) }
            )
        }


        var ABHeight by remember { mutableStateOf(0) }
        var ABWidth by remember { mutableStateOf(0) }
        var aPressed by remember { mutableStateOf(false) }
        var bPressed by remember { mutableStateOf(false) }
        Box(modifier = Modifier
            .size(width = 140.dp, height = 140.dp)
            .onGloballyPositioned { layoutCoordinates ->
                ABHeight = layoutCoordinates.size.height
                ABWidth = layoutCoordinates.size.width
            }
            // As it is possible to press both A and B button and the same time, we handle the input manually
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val inputChange = event.changes.first()
                        if(event.type == PointerEventType.Release && !inputChange.pressed) {
                            // All pointers are released, we release all buttons
                            aPressed = false
                            bPressed = false
                        }  else if (event.type == PointerEventType.Press || event.type == PointerEventType.Move) {
                            val position = inputChange.position
                            if((position.x > (ABWidth / 2 - 60) && position.x < (ABWidth / 2 + 60))
                                && (position.y > (ABHeight / 2 - 60) && position.y < (ABHeight / 2 + 60))) {
                                // The pointer is in a box between A and B button
                                aPressed = true
                                bPressed = true
                            } else if(position.y > ABHeight / 2 && position.x < ABWidth / 2) {
                                // The pointer is on the B button
                                bPressed = true
                                aPressed = false
                            } else if(position.x > ABWidth / 2 && position.y < ABHeight / 2) {
                                // The pointer is on the A button
                                aPressed = true
                                bPressed = false
                            } else {
                                aPressed = false
                                bPressed = false
                            }
                        }
                        onButtonChange(Button.A, aPressed)
                        onButtonChange(Button.B, bPressed)
                        event.changes.first().consume()
                    }
                }
            }
        ) {
            SingleButtonImage(
                Button.A,
                modifier = Modifier.align(Alignment.TopEnd),
                buttonPressed = aPressed
            )
            SingleButtonImage(
                Button.B,
                modifier = Modifier.align(Alignment.BottomStart),
                buttonPressed = bPressed
            )
        }
    }
}

@Composable
fun SingleButton(button: Button, modifier: Modifier, onButtonChange: (pressed: Boolean) -> Unit) {
    var buttonPressed by remember { mutableStateOf(false) }
    fun Modifier.applyButton(): Modifier {
        return this.pointerInput(Unit) {
            detectTapGestures(onPress = {
                buttonPressed = true
                onButtonChange(true)
                tryAwaitRelease()
                buttonPressed = false
                onButtonChange(false)
            })
        }
    }

    SingleButtonImage(button, modifier = modifier.applyButton(), buttonPressed)
}

@Composable
fun SingleButtonImage(button: Button, modifier: Modifier, buttonPressed: Boolean) {

    val buttonImage = when(button) {
        Button.A -> if (buttonPressed) R.drawable.button_a_pressed else R.drawable.button_a
        Button.B -> if (buttonPressed) R.drawable.button_b_pressed else R.drawable.button_b
        Button.FAST_FORWARD -> if (buttonPressed) R.drawable.button_fast_forward_pressed else R.drawable.button_fast_forward
        Button.SELECT -> if(buttonPressed) R.drawable.button_select_pressed else R.drawable.button_select
        Button.START -> if(buttonPressed) R.drawable.button_start_pressed else R.drawable.button_start
    }

    Image(painterResource(buttonImage), button.buttonDescription, modifier = modifier)
}

enum class Button(val buttonDescription: String) {
    A("A button"), B("B button"),
    FAST_FORWARD("Fast Forward button"), SELECT("Select button"),
    START("Start button")
}