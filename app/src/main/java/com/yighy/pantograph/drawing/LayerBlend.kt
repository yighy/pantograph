package com.yighy.pantograph.drawing

import android.graphics.BlendMode
import android.graphics.Paint
import com.yighy.pantograph.data.LayerEntity

/**
 * How a layer mixes with the layers under it. The few that colouring over a line drawing
 * reaches for: Multiply to shade, Screen and Add to light, Overlay for contrast, Darken and
 * Lighten to keep the stronger of the two.
 */
enum class LayerBlend(val label: String, val mode: BlendMode?) {
    Normal("Normal", null),
    Multiply("Multiply", BlendMode.MULTIPLY),
    Screen("Screen", BlendMode.SCREEN),
    Overlay("Overlay", BlendMode.OVERLAY),
    Add("Add", BlendMode.PLUS),
    Darken("Darken", BlendMode.DARKEN),
    Lighten("Lighten", BlendMode.LIGHTEN);

    companion object {
        /** Unknown names, from a newer build, read as Normal. */
        fun of(name: String?): LayerBlend = entries.firstOrNull { it.name == name } ?: Normal
    }
}

val LayerEntity.blend: LayerBlend get() = LayerBlend.of(blendMode)

/**
 * The paint a layer is laid down with, wherever the drawing is put together - on screen, in an
 * export, a thumbnail, a timelapse frame, a merge: its opacity and its blend. One place, so the
 * picture that leaves the app is the one on the canvas.
 */
fun LayerEntity.compositePaint(filter: Boolean = true): Paint = Paint().apply {
    alpha = (opacity * 255).toInt()
    isFilterBitmap = filter
    blendMode = blend.mode
}
