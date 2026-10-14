package com.yighy.pantograph.drawing

import org.junit.Assert.assertEquals
import org.junit.Test

class DrawingModeToggleTest {

    @Test
    fun eraserKeepsTheShape() {
        assertEquals(DrawingMode.Eraser, DrawingMode.Freehand.withEraser(true))
        assertEquals(DrawingMode.StraightLineEraser, DrawingMode.StraightLine.withEraser(true))
        assertEquals(DrawingMode.LassoErase, DrawingMode.LassoFill.withEraser(true))
        assertEquals(DrawingMode.PolygonErase, DrawingMode.PolygonFill.withEraser(true))
        assertEquals(DrawingMode.PolygonFill, DrawingMode.PolygonErase.withEraser(false))
    }

    @Test
    fun lineKeepsTheEraser() {
        assertEquals(DrawingMode.StraightLine, DrawingMode.Freehand.withLine(true))
        assertEquals(DrawingMode.StraightLineEraser, DrawingMode.Eraser.withLine(true))
        assertEquals(DrawingMode.PolygonFill, DrawingMode.LassoFill.withLine(true))
        assertEquals(DrawingMode.PolygonErase, DrawingMode.LassoErase.withLine(true))
        assertEquals(DrawingMode.LassoErase, DrawingMode.PolygonErase.withLine(false))
    }

    @Test
    fun theLassoFillNeverTogglesOutOfItself() {
        val lasso = listOf(DrawingMode.LassoFill, DrawingMode.LassoErase, DrawingMode.PolygonFill, DrawingMode.PolygonErase)
        for (mode in lasso) {
            assertEquals(true, mode.withLine(!mode.isLine).isLassoFill)
            assertEquals(true, mode.withEraser(!mode.isEraser).isLassoFill)
        }
    }

    @Test
    fun toolsWithoutEitherComeOutAsTheBrush() {
        assertEquals(DrawingMode.Eraser, DrawingMode.BucketFill.withEraser(true))
        assertEquals(DrawingMode.StraightLine, DrawingMode.Gradient.withLine(true))
    }
}
