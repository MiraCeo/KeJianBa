package com.huanchengfly.tieba.post.ui.page.main

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.huanchengfly.tieba.post.LocalUISettings
import kotlin.math.PI
import kotlin.math.cos

private const val PlanetFillDurationMillis = 220
private val PlanetCenter = Offset(12f, 12f)
private const val PlanetRadius = 7.4f
private const val PlanetRestTilt = -22f
private val PlanetOutline = Stroke(1.6f)
private val PlanetRingOutline = Stroke(1.3f)
private const val SelectedPlanetRingOutlineWidth = 1.7f
private val PlanetSwingEasing = Easing { fraction ->
    ((1.0 - cos(PI * fraction)) * 0.5).toFloat()
}

/** One shared planet silhouette: selection fills the sphere and hollows the visible ring. */
@Composable
internal fun PlanetNavigationIcon(
    selected: Boolean,
    description: String?,
    modifier: Modifier = Modifier,
) {
    val reduceMotion = LocalUISettings.current.reduceMotion
    val fill = animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = if (reduceMotion) snap() else tween(PlanetFillDurationMillis, easing = FastOutSlowInEasing),
        label = "MainNavigationPlanetFill",
    )
    val rotation = remember { Animatable(0f) }
    var previousSelected by remember { mutableStateOf(selected) }
    LaunchedEffect(selected, reduceMotion) {
        val selectionChanged = previousSelected != selected
        previousSelected = selected
        when {
            reduceMotion || !selectionChanged -> rotation.snapTo(0f)
            else -> rotation.animateTo(0f, planetSwingSpec(rotation.value, selected))
        }
    }
    val geometry = remember { PlanetGeometry() }
    val tint = LocalContentColor.current
    Canvas(modifier.semantics {
        if (description != null) contentDescription = description
    }) {
        val unit = size.minDimension / 24f
        translate((size.width - 24f * unit) / 2f, (size.height - 24f * unit) / 2f) {
            scale(unit, unit, pivot = Offset.Zero) {
                // Rotate the complete planet/ring around the sphere center, not the ring's tip.
                rotate(PlanetRestTilt + rotation.value, pivot = PlanetCenter) {
                    drawPlanet(geometry, tint, fill.value)
                }
            }
        }
    }
}

private class PlanetGeometry {
    private val frontOuter = Rect(1f, 7.6f, 23f, 16.4f)
    private val frontInner = Rect(2.8f, 10f, 21.2f, 14f)
    val frontRing = ringPath(frontOuter, frontInner)
    // A flatter rear arc recedes behind the sphere instead of exposing tiny holes at its sides.
    val rearRing = ringPath(Rect(1f, 10.4f, 23f, 13.6f), Rect(2.8f, 11.55f, 21.2f, 12.45f))
    val sphereOcclusion = Path().apply {
        addOval(Rect(
            PlanetCenter.x - PlanetRadius, PlanetCenter.y - PlanetRadius,
            PlanetCenter.x + PlanetRadius, PlanetCenter.y + PlanetRadius,
        ))
    }
    // Close the tiny inner-ring gaps outside the sphere without changing either outer contour.
    val frontContacts = Path.combine(
        PathOperation.Difference,
        Path().apply { addOval(frontInner) },
        sphereOcclusion,
    )
    // Hollow the full ring band in the selected state. Its outline is drawn outside this
    // opening instead of narrowing it with an inset channel.
    private val frontChannel = Path.combine(
        PathOperation.Intersect,
        frontRing,
        Path().apply { addRect(Rect(0f, PlanetCenter.y, 24f, 24f)) },
    )
    private val rearChannel = Path.combine(
        PathOperation.Difference,
        Path.combine(
            PathOperation.Intersect,
            rearRing,
            Path().apply { addRect(Rect(0f, 0f, 24f, PlanetCenter.y)) },
        ),
        Path().apply {
            // Keep the rear opening behind the sphere and its outline.
            val radius = PlanetRadius + PlanetOutline.width / 2f
            addOval(Rect(
                PlanetCenter.x - radius, PlanetCenter.y - radius,
                PlanetCenter.x + radius, PlanetCenter.y + radius,
            ))
        },
    )
    val selectedChannel = Path.combine(PathOperation.Union, frontChannel, rearChannel)
}

private fun ringPath(outer: Rect, inner: Rect) = Path().apply {
    fillType = PathFillType.EvenOdd
    addOval(outer)
    addOval(inner)
}

private fun DrawScope.drawPlanet(geometry: PlanetGeometry, tint: Color, fill: Float) {
    if (fill <= 0f) {
        drawPlanetBody(geometry, tint, fill)
        return
    }
    clipPath(geometry.selectedChannel, ClipOp.Difference) {
        drawPlanetBody(geometry, tint, fill)
    }
    if (fill < 1f) {
        // Fade the line open using the same progress as the sphere fill, without a saveLayer
        // or a hard-coded background color.
        clipPath(geometry.selectedChannel) {
            drawPlanetBody(geometry, tint.copy(alpha = tint.alpha * (1f - fill)), fill)
        }
    }
}

private fun DrawScope.drawPlanetBody(geometry: PlanetGeometry, tint: Color, fill: Float) {
    // Leave room for the selected ring's outward stroke at the sides.
    val ringLeft = -SelectedPlanetRingOutlineWidth
    val ringRight = 24f + SelectedPlanetRingOutlineWidth
    // The rear half disappears behind the sphere, even when the sphere is hollow.
    clipRect(left = ringLeft, top = 0f, right = ringRight, bottom = 12f) {
        clipPath(geometry.sphereOcclusion, ClipOp.Difference) {
            drawPlanetRing(geometry.rearRing, tint, fill)
        }
    }
    drawCircle(tint, PlanetRadius, PlanetCenter, alpha = fill)
    drawCircle(tint, PlanetRadius, PlanetCenter, style = PlanetOutline)
    clipRect(left = ringLeft, top = 12f, right = ringRight, bottom = 24f) {
        drawPath(geometry.frontContacts, tint)
        drawPlanetRing(geometry.frontRing, tint, fill)
    }
}

private fun DrawScope.drawPlanetRing(ring: Path, tint: Color, fill: Float) {
    drawPath(ring, tint)
    // At full selection the band is clipped out by selectedChannel. Doubling the centered
    // stroke leaves a full-width outline outside the band, without consuming its opening.
    val strokeWidth = PlanetRingOutline.width +
            (SelectedPlanetRingOutlineWidth * 2f - PlanetRingOutline.width) * fill
    drawPath(ring, tint, style = Stroke(strokeWidth))
}

internal fun planetSwingSpec(initialRotation: Float, selected: Boolean) = keyframes {
    durationMillis = MainNavigationFeedbackDurationMillis
    initialRotation at 0 using PlanetSwingEasing
    (if (selected) 15f else 10f) at 100 using PlanetSwingEasing
    // Pass through zero continuously on the way to the smaller opposite tilt.
    -5f at 270 using PlanetSwingEasing
    0f at MainNavigationFeedbackDurationMillis
}
