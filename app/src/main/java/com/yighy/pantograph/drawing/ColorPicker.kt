package com.yighy.pantograph.drawing

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.dp
import com.yighy.pantograph.ui.theme.MotionTokens

/** The colour field's side. With the hue bar beside it, the left half of the colour panel. */
internal val ColorFieldSize = 140.dp
private val HueBarWidth = 28.dp
private val FieldGap = 8.dp

/**
 * The left half of the colour panel: the saturation-brightness field with a vertical hue bar
 * beside it, or the three sliders in the same width. The swatches, the comparison and the
 * buttons sit to its right (see ColorPickerContent), so the panel spends the width of the
 * screen instead of a narrow column centred in it.
 */
@Composable
fun HSBPickerView(
    initialColor: Color,
    isSliderMode: Boolean,
    onColorChanged: (Color) -> Unit
) {
    // Local state to keep HSB values stable and independent of RGB conversions
    var hue by remember { mutableFloatStateOf(0f) }
    var saturation by remember { mutableFloatStateOf(0f) }
    var brightness by remember { mutableFloatStateOf(0f) }

    // Update local HSB only when initialColor changes from OUTSIDE (like History or Layer click)
    LaunchedEffect(initialColor) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(initialColor.toArgb(), hsv)
        // Only update if the difference is significant to avoid slider jumping
        if (Color.hsv(hsv[0], hsv[1], hsv[2]).toArgb() != Color.hsv(hue, saturation, brightness).toArgb()) {
            hue = hsv[0]
            saturation = hsv[1]
            brightness = hsv[2]
        }
    }

    // The two modes are different heights, so the swap needs both a cross-fade and a size
    // transform - a bare if/else snapped the whole panel to its new height.
    AnimatedContent(
        targetState = isSliderMode,
        transitionSpec = {
            (fadeIn(MotionTokens.expressiveEnter) togetherWith fadeOut(MotionTokens.expressiveExit))
                .using(SizeTransform(clip = false) { _, _ -> MotionTokens.panelTransition })
        },
        label = "colorPickerMode"
    ) { sliderMode ->
        if (!sliderMode) {
            Row(horizontalArrangement = Arrangement.spacedBy(FieldGap)) {
                SaturationValueBox(
                    hue = hue,
                    saturation = saturation,
                    value = brightness,
                    modifier = Modifier.size(ColorFieldSize),
                    onValueChange = { s, v ->
                        saturation = s
                        brightness = v
                        onColorChanged(Color.hsv(hue, s, v))
                    }
                )
                VerticalHueBar(
                    hue = hue,
                    modifier = Modifier.width(HueBarWidth).height(ColorFieldSize),
                    onHueChange = { h ->
                        hue = h
                        onColorChanged(Color.hsv(h, saturation, brightness))
                    }
                )
            }
        } else {
            Box(modifier = Modifier.width(ColorFieldSize + FieldGap + HueBarWidth)) {
                HSBSlidersView(
                    hue = hue,
                    saturation = saturation,
                    value = brightness,
                    onValueChange = { h, s, v ->
                        hue = h
                        saturation = s
                        brightness = v
                        onColorChanged(Color.hsv(h, s, v))
                    }
                )
            }
        }
    }
}

/**
 * Hue top to bottom, red round to red, with a ring across it at the current hue. Tapped or
 * dragged anywhere along it; standing upright beside the field, it is as tall as the field
 * and the hue gets the same travel for the thumb the field's brightness does.
 */
@Composable
fun VerticalHueBar(hue: Float, modifier: Modifier = Modifier, onHueChange: (Float) -> Unit) {
    val hueColors = remember { List(361) { Color.hsv(it.toFloat().coerceAtMost(359.9f), 1f, 1f) } }
    val currentOnHueChange by rememberUpdatedState(onHueChange)
    fun hueAt(y: Float, height: Int) = (y / height).coerceIn(0f, 1f) * 360f
    Box(
        modifier = modifier
            .semantics {
                contentDescription = "Hue"
                progressBarRangeInfo = ProgressBarRangeInfo(hue, 0f..360f)
                setProgress { target ->
                    currentOnHueChange(target.coerceIn(0f, 360f))
                    true
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { offset -> currentOnHueChange(hueAt(offset.y, size.height)) }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    currentOnHueChange(hueAt(change.position.y, size.height))
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val barWidth = 12.dp.toPx()
            val left = (size.width - barWidth) / 2f
            drawRoundRect(
                brush = Brush.verticalGradient(hueColors),
                topLeft = Offset(left, 0f),
                size = androidx.compose.ui.geometry.Size(barWidth, size.height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f)
            )
            // The thumb: a white ring with a dark edge, so it reads over every hue.
            val y = (hue / 360f) * size.height
            val ring = Offset(size.width / 2f, y.coerceIn(barWidth / 2f, size.height - barWidth / 2f))
            drawCircle(Color.Black.copy(alpha = 0.35f), radius = 9.dp.toPx(), center = ring, style = androidx.compose.ui.graphics.drawscope.Stroke(4.dp.toPx()))
            drawCircle(Color.White, radius = 9.dp.toPx(), center = ring, style = androidx.compose.ui.graphics.drawscope.Stroke(2.5.dp.toPx()))
        }
    }
}

@Composable
fun HSBSlidersView(
    hue: Float,
    saturation: Float,
    value: Float,
    onValueChange: (Float, Float, Float) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column {
            Text("Hue: ${hue.toInt()}\u00B0", style = MaterialTheme.typography.labelSmall)
            HueSlider(hue = hue, onHueChange = { onValueChange(it, saturation, value) })
        }
        
        Column {
            Text("Saturation: ${(saturation * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
            Slider(
                value = saturation,
                onValueChange = { onValueChange(hue, it, value) },
                valueRange = 0f..1f
            )
        }
        
        Column {
            Text("Brightness: ${(value * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
            Slider(
                value = value,
                onValueChange = { onValueChange(hue, saturation, it) },
                valueRange = 0f..1f
            )
        }
    }
}

@Composable
fun SaturationValueBox(
    hue: Float,
    saturation: Float,
    value: Float,
    modifier: Modifier = Modifier,
    onValueChange: (Float, Float) -> Unit
) {
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val s = (offset.x / size.width).coerceIn(0f, 1f)
                    val v = 1f - (offset.y / size.height).coerceIn(0f, 1f)
                    onValueChange(s, v)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val s = (change.position.x / size.width).coerceIn(0f, 1f)
                    val v = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                    onValueChange(s, v)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val saturationBrush = Brush.horizontalGradient(
                colors = listOf(Color.White, Color.hsv(hue, 1f, 1f))
            )
            drawRect(brush = saturationBrush)

            val valueBrush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color.Black)
            )
            drawRect(brush = valueBrush)

            val selectorX = saturation * size.width
            val selectorY = (1f - value) * size.height
            drawCircle(
                color = if (value > 0.5f) Color.Black else Color.White,
                radius = 6.dp.toPx(),
                center = Offset(selectorX, selectorY),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
            )
        }
    }
}

@Composable
fun HueSlider(hue: Float, onHueChange: (Float) -> Unit) {
    val hueColors = remember {
        List(360) { Color.hsv(it.toFloat(), 1f, 1f) }
    }
    // No height on the wrapper: the real Slider sits inside it, and clamping the box to 16dp
    // clamped the thumb's touch area to 16dp with it. The 8dp gradient below is the visual
    // track; the Slider keeps its own 48dp grab area centred on it.
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(Brush.horizontalGradient(hueColors))
        )
        Slider(
            value = hue,
            onValueChange = onHueChange,
            valueRange = 0f..360f,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent
            )
        )
    }
}
