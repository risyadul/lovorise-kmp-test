package com.lovorise.discover.core.designsystem.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

private val ShimmerBase = Color(0xFFEDEFF3)
private val ShimmerHighlight = Color(0xFFF8F9FB)

/**
 * Skeleton-loading shimmer: a soft highlight sweeping across a neutral block.
 * Apply after a clip() so the sweep follows the placeholder's shape.
 */
fun Modifier.shimmer(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = 1200, easing = LinearEasing)
        ),
        label = "shimmerProgress",
    )
    drawWithCache {
        val width = size.width
        val start = width * progress
        val brush = Brush.linearGradient(
            colors = listOf(ShimmerBase, ShimmerHighlight, ShimmerBase),
            start = Offset(start, 0f),
            end = Offset(start + width, size.height),
        )
        onDrawBehind { drawRect(brush) }
    }
}
