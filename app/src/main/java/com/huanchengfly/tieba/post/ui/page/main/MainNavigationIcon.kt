package com.huanchengfly.tieba.post.ui.page.main

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.huanchengfly.tieba.post.LocalUISettings
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot

/** Reveal the selected glyph from its center instead of shrinking the outline's holes. */
@Composable
internal fun MainNavigationIcon(
    destination: MainDestination,
    selected: Boolean,
    description: String?,
    modifier: Modifier = Modifier,
) {
    if (destination === MainDestination.Feed) {
        PlanetNavigationIcon(selected, description, modifier)
        return
    }
    val reduceMotion = LocalUISettings.current.reduceMotion
    if (destination === MainDestination.Home) {
        // Restore the original forum-entry vector and its own path animation.
        val vector = AnimatedImageVector.animatedVectorResource(destination.iconRes)
        val painter = if (reduceMotion) {
            key(selected) { rememberAnimatedVectorPainter(vector, atEnd = selected) }
        } else {
            rememberAnimatedVectorPainter(vector, atEnd = selected)
        }
        Icon(painter = painter, contentDescription = description, modifier = modifier)
        return
    }
    val isNotification = destination === MainDestination.Notification
    val outline: Painter
    val filled: Painter
    if (isNotification) {
        // Keep the explicit bell vectors from the verified message-icon fix.
        outline = rememberVectorPainter(Icons.Rounded.NotificationsNone)
        filled = rememberVectorPainter(Icons.Rounded.Notifications)
    } else {
        val vector = AnimatedImageVector.animatedVectorResource(destination.iconRes)
        // Fixed endpoints preserve each glyph but do not run its old inward path morph.
        outline = key(destination, false) { rememberAnimatedVectorPainter(vector, atEnd = false) }
        filled = key(destination, true) { rememberAnimatedVectorPainter(vector, atEnd = true) }
    }
    val progress = animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = if (reduceMotion) {
            snap()
        } else {
            tween(MainNavigationFeedbackDurationMillis, easing = FastOutSlowInEasing)
        },
        label = "MainNavigationCenterFill",
    )
    val tint = LocalContentColor.current
    val colorFilter = remember(tint) { ColorFilter.tint(tint) }
    val revealPath = remember { Path() }
    val isFan = destination === MainDestination.Explore
    val swingRotation = if (isNotification) {
        rememberMessageSwingRotation(selected, reduceMotion)
    } else {
        null
    }

    Canvas(modifier.semantics {
        if (description != null) contentDescription = description
    }) {
        val fraction = progress.value
        // Like the fan, read and apply rotation only while drawing, without another graphics layer.
        rotate(swingRotation?.value ?: 0f, pivot = Offset(center.x, size.height * 0.125f)) {
            when {
                fraction <= 0f -> with(outline) { draw(size, colorFilter = colorFilter) }
                fraction >= 1f -> with(filled) { draw(size, colorFilter = colorFilter) }
                else -> {
                    val rotation = if (isFan) 180f * fraction else 0f
                    val radius = hypot(size.width, size.height) * 0.5f * fraction
                    revealPath.reset()
                    revealPath.addOval(Rect(
                        center.x - radius, center.y - radius,
                        center.x + radius, center.y + radius,
                    ))
                    // Complementary clips avoid leaving the outline over the finished filled glyph.
                    clipPath(revealPath, ClipOp.Difference) {
                        rotate(rotation) {
                            with(outline) { draw(size, colorFilter = colorFilter) }
                        }
                    }
                    clipPath(revealPath) {
                        // The fan's filled vector endpoint already includes a 180-degree rotation.
                        rotate(if (isFan) rotation - 180f else 0f) {
                            with(filled) { draw(size, colorFilter = colorFilter) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberMessageSwingRotation(selected: Boolean, reduceMotion: Boolean): State<Float> {
    val rotation = remember { Animatable(0f) }
    var previousSelected by remember { mutableStateOf(selected) }
    LaunchedEffect(selected, reduceMotion) {
        val selectionChanged = previousSelected != selected
        previousSelected = selected
        if (reduceMotion || !selectionChanged) {
            // Do not swing on initial composition or when reduced motion is turned off.
            rotation.snapTo(0f)
        } else {
            val amplitude = if (selected) 20f else 15f
            rotation.animateTo(0f, messageSwingSpec(amplitude, rotation.value))
        }
    }
    return rotation.asState()
}

private val MessageSwingEasing = Easing { fraction ->
    ((1.0 - cos(PI * fraction)) * 0.5).toFloat()
}

internal fun messageSwingSpec(amplitude: Float, initialRotation: Float) = keyframes {
    val duration = MainNavigationFeedbackDurationMillis
    durationMillis = duration
    // Continue from the current angle when interrupted; slow down only at the turning points.
    initialRotation at 0 using MessageSwingEasing
    -amplitude at (duration / 4) using MessageSwingEasing
    // One continuous sweep through zero: a midpoint keyframe would restart the easing there.
    amplitude at (duration * 3 / 4) using MessageSwingEasing
    0f at duration
}
