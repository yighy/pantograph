package com.yighy.pantograph.drawing

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yighy.pantograph.ui.theme.MotionTokens
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * How to use the tool, in a line. Read off the state as well as the tool where the same tool
 * works two ways: lasso fill with the straight line takes its sides one drag at a time.
 */
internal fun PinnableTool.hint(state: DrawingState): String = when (this) {
    PinnableTool.Fill -> "Press the button to fill the area under the cursor. Drag its chip to tune the fill"
    PinnableTool.Gradient -> "Hold the button and draw: the gradient runs from where you start to where you let go"
    PinnableTool.LassoFill -> when {
        state.drawingMode.isPolygonFill ->
            "Hold the button: each drag of your drawing finger pulls out a side, and lifting it sets a corner"
        state.drawingMode.isEraser -> "Hold the button and trace a shape to erase it"
        else -> "Hold the button and trace a shape to fill it with the brush colour"
    }
    PinnableTool.Lazy -> "The brush trails the cursor on a string, for smoother lines. Drag its chip to set the length"
    PinnableTool.Anchor -> "Hold the button: every line starts from the same point, which the finger on the button moves"
    PinnableTool.Chain -> "With the straight line, hold the button: each drag of your drawing finger adds a side, and lifting it sets a corner"
    PinnableTool.Fine -> "Draw sensitivity applies to aiming too, for precise placement"
    PinnableTool.Loupe -> "Magnifies the canvas under the brush. Drag its chip to set the zoom"
    PinnableTool.Fullscreen -> "Tap the Full chip or press Back to bring the controls back"
    PinnableTool.Lasso -> "Hold the button and move the cursor to outline an area, then release"
    PinnableTool.Rect -> "Hold the button and move the cursor to frame an area, then release"
    PinnableTool.Wand -> "Press the button to select the area under the cursor. Drag its chip to tune it"
    PinnableTool.ColorSelect -> "Press the button to select everything of the colour under the cursor"
    PinnableTool.Path -> "Hold the button to drop a point, steer, then release. Hold an existing point to move it"
}

/**
 * A line on how to use the tool just turned on, under the chip that names it. It goes at the
 * first press of the pen - by then the tool is being used, and the hint would only be in the
 * way of what it describes - or with the tool itself, and a tap on it sends it away too.
 *
 * Only for a tool turned on while the screen is open: the ones already on when a project
 * opens are known already, or they would not have been left on.
 */
@Composable
internal fun ToolHint(viewModel: DrawingViewModel, enabled: Boolean) {
    var shown by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(viewModel, enabled) {
        shown = null
        if (!enabled) return@LaunchedEffect
        var before: List<String>? = null
        viewModel.uiState
            .map { state -> PinnableTool.entries.filter { it.isActive(state) }.map { it.hint(state) } }
            .distinctUntilChanged()
            .collect { now ->
                val previous = before
                before = now
                // The first value is what was already on.
                if (previous == null) return@collect
                val added = now.lastOrNull { it !in previous }
                when {
                    added != null -> shown = added
                    shown !in now -> shown = null
                }
            }
    }
    LaunchedEffect(viewModel) {
        viewModel.penPresses.collect { shown = null }
    }

    // Held over so the exit has something to fade, as the chips do.
    var last by remember { mutableStateOf("") }
    shown?.let { last = it }

    AnimatedVisibility(
        visible = shown != null,
        enter = fadeIn(MotionTokens.expressiveEnter) + expandVertically(MotionTokens.panelTransition),
        exit = fadeOut(MotionTokens.expressiveExit) + shrinkVertically(MotionTokens.panelTransition)
    ) {
        Surface(
            onClick = { shown = null },
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            tonalElevation = 6.dp,
            shadowElevation = 2.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            modifier = Modifier.padding(top = 8.dp).widthIn(max = 240.dp)
        ) {
            Text(
                last,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}
