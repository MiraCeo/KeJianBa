package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sign

/** A quartic superellipse: gently bowed sides, with no straight-to-arc joins. */
internal object ForumAvatarShape : Shape {
    // Cache the normalized contour; resizing only scales its cubic control points.
    internal val curves = buildAvatarContour()

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path()
        if (size.width > 0f && size.height > 0f) {
            val first = curves.first().start
            path.moveTo(first.x * size.width, first.y * size.height)
            curves.forEach { curve ->
                path.cubicTo(
                    curve.control1.x * size.width, curve.control1.y * size.height,
                    curve.control2.x * size.width, curve.control2.y * size.height,
                    curve.end.x * size.width, curve.end.y * size.height,
                )
            }
            path.close()
        }
        return Outline.Generic(path)
    }
}

internal data class ForumAvatarCurve(
    val start: Offset,
    val control1: Offset,
    val control2: Offset,
    val end: Offset,
)

private fun buildAvatarContour(): List<ForumAvatarCurve> {
    val count = 32
    val step = 2.0 * PI / count
    val points = (0 until count).map { index ->
        val angle = index * step
        val c = cos(angle)
        val s = sin(angle)
        val sum = c.pow(4) + s.pow(4)
        val radius = sum.pow(-0.25)
        val radiusDerivative = radius * c * s * (c * c - s * s) / sum
        // Polar parameterization avoids the infinite endpoint derivatives of sqrt(cos(t)).
        val position = Offset(((1.0 + radius * c) / 2.0).toFloat(), ((1.0 + radius * s) / 2.0).toFloat())
        val tangent = Offset(
            ((radiusDerivative * c - radius * s) / 2.0).toFloat(),
            ((radiusDerivative * s + radius * c) / 2.0).toFloat(),
        )
        position to tangent
    }
    val handleScale = (step / 3.0).toFloat()
    // Limit the shared tangents to adjacent coordinate spans. At the nearly flat
    // side midpoints, unrestricted Hermite handles can overshoot the image bounds.
    val handles = points.mapIndexed { index, (position, tangent) ->
        val previousDelta = position - points[(index + count - 1) % count].first
        val nextDelta = points[(index + 1) % count].first - position
        val handle = tangent * handleScale
        fun limit(value: Float, previous: Float, next: Float): Float =
            sign(value) * minOf(abs(value), abs(previous), abs(next))
        Offset(limit(handle.x, previousDelta.x, nextDelta.x), limit(handle.y, previousDelta.y, nextDelta.y))
    }
    return List(count) { index ->
        val next = (index + 1) % count
        val start = points[index].first
        val end = points[next].first
        ForumAvatarCurve(start, start + handles[index], end - handles[next], end)
    }
}
