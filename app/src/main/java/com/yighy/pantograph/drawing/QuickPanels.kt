package com.yighy.pantograph.drawing

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Colorize
import androidx.compose.material.icons.rounded.LinearScale
import androidx.compose.material.icons.rounded.Square
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** A history swatch's cell, the touch target round its smaller disc. */
private val SwatchCell = 36.dp

/**
 * The colour panel: the field and hue bar on the left, and on the right what is needed around
 * them - the colour against the one the panel opened on, the recent colours, the eyedropper
 * and the switch to sliders. Side by side, so the panel is half the height the stacked layout
 * was and fills the width it is given instead of leaving it empty either side.
 */
@Composable
fun ColorPickerContent(viewModel: DrawingViewModel, onDismiss: () -> Unit) {
    val selectedColor by remember(viewModel) { viewModel.uiState.map { it.selectedColor }.distinctUntilChanged() }.collectAsState(Color.Black)
    val colorHistory by remember(viewModel) { viewModel.uiState.map { it.colorHistory }.distinctUntilChanged() }.collectAsState(emptyList())
    val isSliderMode by remember(viewModel) { viewModel.uiState.map { it.isColorPickerSliderMode }.distinctUntilChanged() }.collectAsState(false)
    val isEyeDropperActive by remember(viewModel) { viewModel.uiState.map { it.isEyeDropperMode }.distinctUntilChanged() }.collectAsState(false)
    // The colour in hand when the panel opened, to compare against and to go back to.
    val openedWith = remember { viewModel.uiState.value.selectedColor }
    // How many swatches a row of the history holds, from the width it is given. Measured
    // rather than read from constraints: the row below asks its children for their intrinsic
    // height, which a constraints-reading layout cannot answer.
    var swatchColumns by remember { mutableIntStateOf(4) }
    val swatchCellPx = with(LocalDensity.current) { SwatchCell.toPx() }

    // The buttons sit in the bottom right corner of the whole panel, over a space kept free
    // for them at the foot of the right half - so they are in the corner whichever half is
    // the taller, and the panel takes its height from its halves as they animate between
    // the field and the sliders. Sizing the row to its tallest child did the first and broke
    // the second: that measure is the field's or the sliders' final height, so the panel
    // jumped to it.
    Box(modifier = Modifier.padding(4.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HSBPickerView(
                initialColor = selectedColor,
                isSliderMode = isSliderMode,
                onColorChanged = { viewModel.selectColor(it) }
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // New against old. The old half takes you back to it.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .clip(MaterialTheme.shapes.small)
                        .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.small)
                ) {
                    Box(Modifier.weight(1f).fillMaxHeight().background(selectedColor.copy(alpha = 1f)))
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(openedWith.copy(alpha = 1f))
                            .clickable { viewModel.selectColor(openedWith) }
                            .semantics { contentDescription = "Back to the previous colour" }
                    )
                }

                // As many recent colours as two rows of this width hold, all in view.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onSizeChanged { swatchColumns = (it.width / swatchCellPx).toInt().coerceAtLeast(1) }
                ) {
                    val columns = swatchColumns
                    colorHistory.take(columns * 2).chunked(columns).forEach { row ->
                        Row {
                            row.forEach { color ->
                                Box(
                                    modifier = Modifier
                                        .size(SwatchCell)
                                        .clickable { viewModel.selectColor(color) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .border(
                                                width = if (color == selectedColor) 2.dp else 0.5.dp,
                                                color = if (color == selectedColor) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                                shape = CircleShape
                                            )
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(48.dp))
            }
        }
        Row(
            modifier = Modifier.align(Alignment.BottomEnd),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = {
                    viewModel.toggleEyeDropper()
                    onDismiss()
                },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (isEyeDropperActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.Colorize,
                    contentDescription = "Pick colour from canvas",
                    modifier = Modifier.size(20.dp),
                    tint = if (isEyeDropperActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = { viewModel.setColorPickerSliderMode(!isSliderMode) }) {
                Icon(
                    imageVector = if (isSliderMode) Icons.Rounded.Square else Icons.Rounded.LinearScale,
                    contentDescription = if (isSliderMode) "Switch to colour field" else "Switch to sliders",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun GlobalSettingsPanel(viewModel: DrawingViewModel) {
    // Smoothing lives in the Brush Studio only now (see BrushStudio.kt); removed here to
    // keep this quick panel focused on things that aren't brush-specific
    val cursorSensitivity by remember(viewModel) { viewModel.uiState.map { it.cursorSensitivity }.distinctUntilChanged() }.collectAsState(0.6f)
    val isFineCursor by remember(viewModel) { viewModel.uiState.map { it.isFineCursor }.distinctUntilChanged() }.collectAsState(false)

    // Tools' own values are not here any more: each lives on its tool's chip, which is showing
    // exactly while the value matters, and is marked as draggable. What is left is the one
    // setting that belongs to no tool.
    // One line, as the Brush Studio lays out a slider: the name, the track, the value. The
    // label follows the tool rather than staying put: with Fine armed this is no longer a
    // setting about drawing, it is what the cursor does at all times, and this is the only
    // place that says so.
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            if (isFineCursor) "Cursor sensitivity" else "Draw sensitivity",
            style = MaterialTheme.typography.bodyMedium
        )
        Slider(
            value = cursorSensitivity,
            onValueChange = { viewModel.setCursorSensitivity(it) },
            valueRange = 0.1f..1.0f,
            modifier = Modifier.weight(1f)
        )
        Text(
            "${"%.1f".format(cursorSensitivity)}×",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun SettingRow(label: String, valueLabel: String, value: Float, onValueChange: (Float) -> Unit, range: ClosedFloatingPointRange<Float>) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, style = MaterialTheme.typography.labelSmall)
                Text(valueLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            // No height constraint: Slider's own 48dp box is the thumb's touch target,
            // and clamping it to 24dp made the thumb hard to grab vertically.
            Slider(value = value, onValueChange = onValueChange, valueRange = range)
        }
    }
}
