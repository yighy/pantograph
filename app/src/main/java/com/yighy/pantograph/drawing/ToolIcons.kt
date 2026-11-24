package com.yighy.pantograph.drawing

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/*
 * Glyphs for the tools Material has no picture of - the ones that are Pantograph's own. Drawn
 * on Material's 24-unit grid with its 2-unit rounded stroke, so they sit among the stock icons
 * without looking borrowed or out of place. Black, and tinted like any Icon.
 */

/** Strokes and fills on the 24-unit grid; see [toolIcon]. */
private class IconParts(private val builder: ImageVector.Builder) {
    fun stroke(pathData: String) {
        builder.addPath(
            pathData = addPathNodes(pathData),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
    }

    fun fill(pathData: String, alpha: Float = 1f) {
        builder.addPath(pathData = addPathNodes(pathData), fill = SolidColor(Color.Black), fillAlpha = alpha)
    }
}

private fun toolIcon(name: String, draw: IconParts.() -> Unit): ImageVector {
    val builder = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    )
    IconParts(builder).draw()
    return builder.build()
}

/** The brush on its rope: the lazy radius as a dashed ring round the cursor, the brush at its edge. */
val LazyIcon: ImageVector by lazy {
    toolIcon("Lazy") {
        stroke("M12.00,4.00A8,8 0 0,1 13.94,4.24M16.70,5.53A8,8 0 0,1 18.13,6.86M19.61,9.53A8,8 0 0,1 19.98,11.44M19.61,14.47A8,8 0 0,1 18.78,16.24M16.70,18.47A8,8 0 0,1 15.00,19.42M12.00,20.00A8,8 0 0,1 10.06,19.76M7.30,18.47A8,8 0 0,1 5.87,17.14M4.39,14.47A8,8 0 0,1 4.02,12.56M4.39,9.53A8,8 0 0,1 5.22,7.76M7.30,5.53A8,8 0 0,1 9.00,4.58")
        fill("M10.4,12a1.6,1.6 0 1,0 3.2,0a1.6,1.6 0 1,0 -3.2,0Z")
        stroke("M12,12L17.3,17.3")
        fill("M15.5,17.7a2.2,2.2 0 1,0 4.4,0a2.2,2.2 0 1,0 -4.4,0Z")
    }
}

/** Straight lines end to end, from where the chain began to the cursor pulling the next side. */
val ChainIcon: ImageVector by lazy {
    toolIcon("Chain") {
        stroke("M4,18L9,7L15,14L18.6,8.3")
        fill("M1.8,18a2.2,2.2 0 1,0 4.4,0a2.2,2.2 0 1,0 -4.4,0Z")
        stroke("M17.4,6.4a2.4,2.4 0 1,0 4.8,0a2.4,2.4 0 1,0 -4.8,0Z")
    }
}

/** A fine crosshair, open in the middle where the point being aimed at is. */
val FineIcon: ImageVector by lazy {
    toolIcon("Fine") {
        stroke("M12,3V8M12,16V21M3,12H8M16,12H21")
        fill("M10.7,12a1.3,1.3 0 1,0 2.6,0a1.3,1.3 0 1,0 -2.6,0Z")
    }
}

/** A magnifier over two pixels: the loupe enlarges without smoothing, pixel for pixel. */
val LoupeIcon: ImageVector by lazy {
    toolIcon("Loupe") {
        stroke("M3.5,10a6.5,6.5 0 1,0 13,0a6.5,6.5 0 1,0 -13,0Z")
        stroke("M14.8,14.8L20,20")
        fill("M7,7h3v3h-3Z")
        fill("M10,10h3v3h-3Z")
    }
}

/** A traced shape, filled, with the point where the trace closes. */
val LassoFillIcon: ImageVector by lazy {
    toolIcon("LassoFill") {
        fill("M4.5,9C4.5,4.5 16,2.5 19,7.5C21.5,12 16,17 10.5,16C6,15.2 4.5,12.5 4.5,9Z", alpha = 0.35f)
        stroke("M4.5,9C4.5,4.5 16,2.5 19,7.5C21.5,12 16,17 10.5,16C6,15.2 4.5,12.5 4.5,9Z")
        fill("M8.5,16a2,2 0 1,0 4,0a2,2 0 1,0 -4,0Z")
    }
}

/** A curve bent through the points placed along it. */
val PathIcon: ImageVector by lazy {
    toolIcon("Path") {
        stroke("M3,16Q7.5,4 12,12T21,8")
        fill("M1,16a2,2 0 1,0 4,0a2,2 0 1,0 -4,0Z")
        fill("M10,12a2,2 0 1,0 4,0a2,2 0 1,0 -4,0Z")
        fill("M19,8a2,2 0 1,0 4,0a2,2 0 1,0 -4,0Z")
    }
}
