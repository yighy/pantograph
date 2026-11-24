package com.yighy.pantograph.drawing

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlin.math.roundToInt

private const val SENSITIVITY_MIN = 0.1f
private const val SENSITIVITY_MAX = 1.0f

/**
 * The bottom bar's sensitivity button: it shows the value, a tap opens the panel with its
 * slider, and a drag sideways on the button sets it there and then - right for more, left for
 * less, as the slider in the panel runs. A chip's value is set the same way, with the same dead
 * zone, response and readout over the finger, only up and down. The panel is for a
 * precise number; the drag is for a quick change without taking the eye off the drawing.
 *
 * The drag is watched from around the button rather than inside it, so the button keeps its
 * own press and click. Once the finger has gone past the slop the gesture is consumed, which is
 * what tells the button underneath that this was not a tap.
 */
@Composable
internal fun SensitivityButton(
    viewModel: DrawingViewModel,
    pressed: Boolean,
    onToggle: () -> Unit
) {
    val sensitivity by remember(viewModel) { viewModel.uiState.map { it.cursorSensitivity }.distinctUntilChanged() }.collectAsState(0.6f)
    val isFineCursor by remember(viewModel) { viewModel.uiState.map { it.isFineCursor }.distinctUntilChanged() }.collectAsState(false)
    val gateSensitivity by remember(viewModel) { viewModel.uiState.map { it.satelliteGateSensitivity }.distinctUntilChanged() }.collectAsState(1f)
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current

    var adjusting by remember { mutableStateOf(false) }
    var fingerX by remember { mutableFloatStateOf(0f) }
    var fingerY by remember { mutableFloatStateOf(0f) }
    var topInWindow by remember { mutableFloatStateOf(0f) }
    var leftInWindow by remember { mutableFloatStateOf(0f) }
    var windowWidthPx by remember { mutableIntStateOf(0) }
    var readoutHeightPx by remember { mutableIntStateOf(with(density) { 44.dp.roundToPx() }) }
    var readoutAbove by remember { mutableStateOf(true) }
    var readoutLeft by remember { mutableStateOf(true) }

    val name = if (isFineCursor) "Cursor sensitivity" else "Draw sensitivity"
    fun format(v: Float) = "${"%.1f".format(v)}×"

    val gesture = Modifier.pointerInput(gateSensitivity) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            // Sideways, the way the panel's slider runs. The satellites' dead zone, and a travel
            // of 200dp for the whole range: the button sits towards the middle of the bar, so
            // that is about as far as a thumb gets either way before the edge of the screen.
            // Positions are taken as -x so that GateMath, which raises the value for a smaller
            // position (a finger moving up), raises it for a finger moving right.
            val s = gateSensitivity.coerceIn(0.25f, 4f)
            val deadZonePx = 12.dp.toPx() / s
            val travelPx = 200.dp.toPx() / s
            var anchorPos = -down.position.x
            var anchorValue = viewModel.uiState.value.cursorSensitivity
            var dragging = false
            var inDeadZone = true
            fingerX = down.position.x
            fingerY = down.position.y
            readoutAbove = true
            val gapAbovePx = READOUT_GAP_ABOVE.roundToPx()
            val marginPx = READOUT_MARGIN.roundToPx()
            val hysteresisPx = READOUT_HYSTERESIS.roundToPx()
            val sideHysteresisPx = READOUT_SIDE_HYSTERESIS.roundToPx()
            readoutLeft = ReadoutPlacement.leansLeft(
                (leftInWindow + down.position.x).roundToInt(), windowWidthPx, null, sideHysteresisPx
            )
            try {
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed) break
                    if (!dragging) {
                        // Within the slop it is still the button's tap, left alone.
                        if ((change.position - down.position).getDistance() <= viewConfiguration.touchSlop) continue
                        dragging = true
                        adjusting = true
                    }
                    change.consume()
                    fingerX = change.position.x
                    fingerY = change.position.y
                    readoutAbove = ReadoutPlacement.staysAbove(
                        fingerY = (topInWindow + change.position.y).roundToInt(),
                        height = readoutHeightPx,
                        gapAbove = gapAbovePx,
                        margin = marginPx,
                        wasAbove = readoutAbove,
                        hysteresis = hysteresisPx
                    )
                    readoutLeft = ReadoutPlacement.leansLeft(
                        (leftInWindow + change.position.x).roundToInt(), windowWidthPx, readoutLeft, sideHysteresisPx
                    )
                    val pos = -change.position.x
                    if (GateMath.effectiveDelta(anchorPos - pos, deadZonePx) == 0f) {
                        inDeadZone = true
                    } else if (inDeadZone) {
                        inDeadZone = false
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    val stepped = GateMath.step(
                        anchorValue = anchorValue,
                        anchorPos = anchorPos,
                        currentPos = pos,
                        min = SENSITIVITY_MIN,
                        max = SENSITIVITY_MAX,
                        deadZonePx = deadZonePx,
                        travelPx = travelPx
                    )
                    anchorValue = stepped.anchorValue
                    anchorPos = stepped.anchorPos
                    viewModel.setCursorSensitivity(stepped.value)
                }
            } finally {
                adjusting = false
            }
        }
    }

    // A drag has no screen-reader equivalent, so the same tenth-of-range steps a chip offers.
    val step = (SENSITIVITY_MAX - SENSITIVITY_MIN) / 10f
    val actions = listOf(true, false).map { increase ->
        CustomAccessibilityAction("${if (increase) "Increase" else "Decrease"} ${name.lowercase()}") {
            val now = viewModel.uiState.value.cursorSensitivity
            viewModel.setCursorSensitivity((now + if (increase) step else -step).coerceIn(SENSITIVITY_MIN, SENSITIVITY_MAX))
            true
        }
    }
    val fill = (sensitivity - SENSITIVITY_MIN) / (SENSITIVITY_MAX - SENSITIVITY_MIN)
    val fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)

    Box(
        modifier = Modifier
            .onGloballyPositioned {
                val inWindow = it.positionInWindow()
                topInWindow = inWindow.y
                leftInWindow = inWindow.x
                windowWidthPx = it.findRootCoordinates().size.width
            }
            .then(gesture)
    ) {
        GroupedButton(
            GroupPosition.First,
            onClick = onToggle,
            pressed = pressed || adjusting,
            isToggle = true
        ) {
            // The value itself rather than a settings glyph: the panel behind this holds the one
            // setting, so the button can say what it is set to - and that is the thing worth
            // seeing at a glance while drawing.
            Text(
                format(sensitivity),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.semantics {
                    contentDescription = "$name ${"%.1f".format(sensitivity)} times. Tap for the slider, drag sideways to change it"
                    customActions = actions
                }
            )
        }
        if (adjusting) {
            Popup(
                popupPositionProvider = AboveOrBelowFinger(
                    fingerX = fingerX,
                    fingerY = fingerY,
                    above = readoutAbove,
                    leansLeft = readoutLeft,
                    gapAbovePx = with(density) { READOUT_GAP_ABOVE.roundToPx() },
                    gapBelowPx = with(density) { READOUT_GAP_BELOW.roundToPx() },
                    marginPx = with(density) { READOUT_MARGIN.roundToPx() }
                )
            ) {
                Box(modifier = Modifier.onSizeChanged { readoutHeightPx = it.height }) {
                    ChipReadout(
                        icon = FineIcon,
                        label = name,
                        values = listOf(ChipValue(short = null, text = format(sensitivity), widest = format(SENSITIVITY_MAX), active = true)),
                        fill = fill,
                        fillColor = fillColor
                    )
                }
            }
        }
    }
}
