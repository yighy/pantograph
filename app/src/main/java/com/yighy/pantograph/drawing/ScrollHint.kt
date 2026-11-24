package com.yighy.pantograph.drawing

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.yighy.pantograph.ui.theme.MotionTokens

/** Which edge of a scrolling area a [ScrollHint] sits on, and so which way it points. */
enum class ScrollHintEdge { Start, End, Top, Bottom }

/**
 * A small arrow at the edge of a scrolling area that has more past that edge, so a row or a
 * list that is cut off says so instead of passing for complete. Fades in and out with what is
 * left to scroll; drawn over the content, and takes no touches, so it never hides a tap.
 */
@Composable
fun BoxScope.ScrollHint(visible: Boolean, edge: ScrollHintEdge) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = MotionTokens.colorTransitionFloat,
        label = "scrollHint"
    )
    if (alpha == 0f) return
    val (alignment, icon) = when (edge) {
        ScrollHintEdge.Start -> Alignment.CenterStart to Icons.AutoMirrored.Rounded.KeyboardArrowLeft
        ScrollHintEdge.End -> Alignment.CenterEnd to Icons.AutoMirrored.Rounded.KeyboardArrowRight
        ScrollHintEdge.Top -> Alignment.TopCenter to Icons.Rounded.KeyboardArrowUp
        ScrollHintEdge.Bottom -> Alignment.BottomCenter to Icons.Rounded.KeyboardArrowDown
    }
    // On a disc of the panel's own colour, so it reads over whatever it sits on.
    Box(
        modifier = Modifier
            .align(alignment)
            .alpha(alpha)
            .size(20.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
    }
}
