package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.huanchengfly.tieba.post.LocalUISettings

/** A theme-aware like icon whose outline and filled states share the same rounded silhouette. */
@Composable
fun AnimatedLikeThumbIcon(
    liked: Boolean,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    inactiveColor: Color = LocalContentColor.current,
    activeColor: Color = MaterialTheme.colorScheme.primary,
) {
    val reduceMotion = LocalUISettings.current.reduceMotion
    val tint by animateColorAsState(
        targetValue = if (liked) activeColor else inactiveColor,
        animationSpec = if (reduceMotion) tween(0) else tween(160, easing = FastOutSlowInEasing),
        label = "LikeThumbColor",
    )
    val fill by animateFloatAsState(
        targetValue = if (liked) 1f else 0f,
        animationSpec = if (reduceMotion) tween(0) else tween(140, easing = FastOutSlowInEasing),
        label = "LikeThumbFill",
    )
    val scale = remember { Animatable(1f) }
    var initialized by remember { mutableStateOf(false) }
    LaunchedEffect(liked, reduceMotion) {
        if (!initialized) {
            initialized = true
            scale.snapTo(1f)
            return@LaunchedEffect
        }
        if (reduceMotion) {
            scale.snapTo(1f)
        } else if (liked) {
            scale.snapTo(0.9f)
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.42f, stiffness = 520f),
            )
        } else {
            scale.snapTo(0.95f)
            scale.animateTo(1f, tween(130, easing = FastOutSlowInEasing))
        }
    }

    val handPath = remember {
        Path().apply {
            moveTo(8.7f, 19.9f)
            lineTo(16.9f, 19.9f)
            cubicTo(18.0f, 19.9f, 18.8f, 19.3f, 19.2f, 18.4f)
            lineTo(21.4f, 13.2f)
            cubicTo(21.6f, 12.8f, 21.7f, 12.3f, 21.7f, 11.9f)
            lineTo(21.7f, 11.2f)
            cubicTo(21.7f, 10.2f, 20.9f, 9.4f, 19.9f, 9.4f)
            lineTo(14.6f, 9.4f)
            lineTo(15.3f, 6.1f)
            cubicTo(15.6f, 4.8f, 14.8f, 3.5f, 13.5f, 3.2f)
            cubicTo(13.1f, 3.1f, 12.8f, 3.3f, 12.6f, 3.6f)
            lineTo(8.8f, 8.9f)
            cubicTo(8.5f, 9.3f, 8.4f, 9.7f, 8.4f, 10.2f)
            lineTo(8.4f, 19.4f)
            cubicTo(8.4f, 19.7f, 8.5f, 19.9f, 8.7f, 19.9f)
            close()
        }
    }
    val cuffPath = remember {
        Path().apply {
            moveTo(3.8f, 9.4f)
            lineTo(6.4f, 9.4f)
            cubicTo(6.9f, 9.4f, 7.2f, 9.8f, 7.2f, 10.3f)
            lineTo(7.2f, 19.2f)
            cubicTo(7.2f, 19.7f, 6.9f, 20.1f, 6.4f, 20.1f)
            lineTo(3.8f, 20.1f)
            cubicTo(3.3f, 20.1f, 3.0f, 19.7f, 3.0f, 19.2f)
            lineTo(3.0f, 10.3f)
            cubicTo(3.0f, 9.8f, 3.3f, 9.4f, 3.8f, 9.4f)
            close()
        }
    }
    val outline = Stroke(width = 1.9f, cap = StrokeCap.Round, join = StrokeJoin.Round)

    Canvas(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .semantics {
                if (contentDescription != null) this.contentDescription = contentDescription
            }
    ) {
        val unit = size.minDimension / 24f
        if (unit <= 0f) return@Canvas
        // Keep a two-physical-pixel opening between the hand and cuff. The path centers are
        // 1.2 viewport units apart, while the two stroke halves consume one full stroke width.
        val cuffShift = outline.width - 1.2f + 2f / unit
        scale(unit, unit, pivot = Offset.Zero) {
            if (fill > 0f) {
                drawPath(handPath, tint.copy(alpha = tint.alpha * fill), style = Fill)
                translate(left = -cuffShift) {
                    drawPath(cuffPath, tint.copy(alpha = tint.alpha * fill), style = Fill)
                }
            }
            drawPath(handPath, tint, style = outline)
            translate(left = -cuffShift) {
                drawPath(cuffPath, tint, style = outline)
            }
        }
    }
}
