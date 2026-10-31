package com.playlog.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.playlog.app.ui.theme.Accent
import com.playlog.app.ui.theme.GemBrush
import com.playlog.app.ui.theme.MintWhite
import com.playlog.app.ui.theme.Sage
import kotlinx.coroutines.delay
import java.util.Locale

/**
 * The Playlog rating gem — a faceted diamond filled from left to right.
 * Tap the left half for a half point, the right half for a whole point.
 * Re-tapping the current value clears it.
 */
@Composable
fun GemRating(
    value: Double?,
    modifier: Modifier = Modifier,
    gemSize: Dp = 26.dp,
    spacing: Dp = 7.dp,
    onRate: ((Double?) -> Unit)? = null,
) {
    val haptics = LocalHapticFeedback.current
    Row(
        modifier = modifier.semantics {
            contentDescription = value?.let {
                "Rating ${String.format(Locale.US, "%.1f", it)} out of 5"
            } ?: "Not rated"
        },
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (index in 0 until 5) {
            val current = value ?: 0.0
            val filled = current >= index + 1.0
            val half = !filled && current >= index + 0.5
            val fraction = when {
                filled -> 1f
                half -> 0.5f
                else -> 0f
            }

            Gem(
                fraction = fraction,
                size = gemSize,
                index = index,
                active = fraction > 0f,
                modifier = Modifier.then(
                    if (onRate != null) {
                        Modifier.pointerInput(index, value) {
                            detectTapGestures { offset ->
                                val picked = if (offset.x < size.width / 2f) {
                                    index + 0.5
                                } else {
                                    index + 1.0
                                }
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onRate(if (value == picked) null else picked)
                            }
                        }
                    } else {
                        Modifier
                    },
                ),
            )
        }
    }
}

@Composable
private fun Gem(
    fraction: Float,
    size: Dp,
    index: Int,
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(active) {
        if (active) {
            scale.snapTo(0.70f)
            delay(index * 38L)
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.48f, stiffness = 520f),
            )
        } else {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 700f),
            )
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val gem = Path().apply {
                moveTo(w * 0.5f, 0f)
                lineTo(w, h * 0.40f)
                lineTo(w * 0.5f, h)
                lineTo(0f, h * 0.40f)
                close()
            }

            if (active) {
                drawPath(gem, Accent.copy(alpha = 0.12f))
            }

            drawPath(gem, Color.White.copy(alpha = if (active) 0.05f else 0.025f))

            if (fraction > 0f) {
                clipRect(right = w * fraction) {
                    drawPath(gem, GemBrush)
                    // top facet
                    val leftFacet = Path().apply {
                        moveTo(0f, h * 0.40f)
                        lineTo(w * 0.5f, 0f)
                        lineTo(w * 0.5f, h * 0.52f)
                        close()
                    }
                    drawPath(leftFacet, Color.White.copy(alpha = 0.22f))
                    val rightFacet = Path().apply {
                        moveTo(w * 0.5f, 0f)
                        lineTo(w, h * 0.40f)
                        lineTo(w * 0.5f, h * 0.52f)
                        close()
                    }
                    drawPath(rightFacet, Color.Black.copy(alpha = 0.10f))
                    val lowerFacet = Path().apply {
                        moveTo(0f, h * 0.40f)
                        lineTo(w * 0.5f, h * 0.52f)
                        lineTo(w, h * 0.40f)
                        lineTo(w * 0.5f, h)
                        close()
                    }
                    drawPath(lowerFacet, Color.White.copy(alpha = 0.06f))
                }
            }

            drawPath(
                gem,
                color = if (active) MintWhite.copy(alpha = 0.55f) else Sage.copy(alpha = 0.38f),
                style = Stroke(width = 1.dp.toPx()),
            )

            if (fraction == 0.5f) {
                drawLine(
                    color = MintWhite.copy(alpha = 0.45f),
                    start = Offset(w * 0.5f, h * 0.06f),
                    end = Offset(w * 0.5f, h * 0.94f),
                    strokeWidth = 1.dp.toPx(),
                )
            }
        }
    }
}

/** Compact gem row with the numeric score set in serif. */
@Composable
fun GemScore(
    value: Double?,
    modifier: Modifier = Modifier,
    gemSize: Dp = 15.dp,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        GemRating(value = value, gemSize = gemSize, spacing = 3.dp)
        Spacer(Modifier.width(7.dp))
        Text(
            text = value?.let { String.format(Locale.US, "%.1f", it) } ?: "—",
            color = if (value != null) MintWhite else Sage,
            style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
        )
    }
}

/** Horizontal gem bar for the profile spread. */
@Composable
fun GemBar(
    value: Double,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
) {
    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val track = Path().apply {
                moveTo(w * 0.5f, 0f)
                lineTo(w, h * 0.40f)
                lineTo(w * 0.5f, h)
                lineTo(0f, h * 0.40f)
                close()
            }
            drawPath(track, Color.White.copy(alpha = 0.05f))
            clipRect(right = size.width * value.coerceIn(0.0, 1.0).toFloat()) {
                drawPath(
                    path = track,
                    brush = Brush.linearGradient(listOf(Accent, Color(0xFF00D4A0))),
                    alpha = 0.9f,
                )
            }
        }
    }
}
