package com.playlog.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.playlog.app.R
import com.playlog.app.data.Game
import com.playlog.app.data.PlayStatus
import com.playlog.app.ui.theme.Accent
import com.playlog.app.ui.theme.CoverRadius
import com.playlog.app.ui.theme.Danger
import com.playlog.app.ui.theme.GlassBorder
import com.playlog.app.ui.theme.MintWhite
import com.playlog.app.ui.theme.Muted
import com.playlog.app.ui.theme.PaleMint
import com.playlog.app.ui.theme.PillShape
import com.playlog.app.ui.theme.Sage
import com.playlog.app.ui.theme.SerifText
import java.util.Locale

/* ------------------------------------------------------------------ */
/* Helpers                                                             */
/* ------------------------------------------------------------------ */

fun gradientFor(id: Long): List<Color> = coverGradientFor(id)

fun initialsOf(name: String): String =
    name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1).uppercase() }

fun compactCount(value: Int): String = when {
    value >= 1_000_000 -> String.format(Locale.US, "%.1fM", value / 1_000_000.0)
    value >= 1_000 -> String.format(Locale.US, "%.1fK", value / 1_000.0)
    else -> value.toString()
}

fun statusColor(status: PlayStatus?): Color = when (status) {
    PlayStatus.PLAYING -> Accent
    PlayStatus.PLAYED -> PaleMint
    PlayStatus.BACKLOG -> Sage
    PlayStatus.DROPPED -> Danger
    null -> Sage
}

/* ------------------------------------------------------------------ */
/* Covers and tiles                                                    */
/* ------------------------------------------------------------------ */

@Composable
fun PosterImage(
    game: Game,
    modifier: Modifier = Modifier,
    corner: Dp = CoverRadius,
) {
    Cover(game = game, modifier = modifier, corner = corner)
}

/**
 * Editorial game tile: 3:4 plate, serif title, micro-meta, optional status
 * badge over the artwork and a tiny gem score.
 */
@Composable
fun GameTile(
    game: Game,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    badge: String? = null,
    rating: Double? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.978f else 1f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 700f),
        label = "tile-press",
    )

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
    ) {
        Box {
            Cover(
                game = game,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f),
            )
            if (!badge.isNullOrBlank()) {
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(7.dp),
                ) {
                    MetaBadge(badge)
                }
            }
        }
        Spacer(Modifier.height(9.dp))
        Text(
            text = game.name,
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = SerifText,
                fontSize = 15.sp,
                lineHeight = 19.sp,
            ),
            color = MintWhite,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(3.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = subtitle
                    ?: listOfNotNull(game.year?.toString(), game.genres.firstOrNull())
                        .joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = Sage,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (rating != null) {
                Spacer(Modifier.width(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.ic_star),
                        contentDescription = null,
                        tint = Accent,
                        modifier = Modifier.size(9.dp),
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = String.format(Locale.US, "%.1f", rating),
                        style = MaterialTheme.typography.labelSmall,
                        color = PaleMint,
                    )
                }
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(5.dp)
                    .background(Accent, CircleShape),
            )
            Spacer(Modifier.width(9.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MintWhite,
            )
        }
        if (action != null && onAction != null) {
            Row(
                modifier = Modifier
                    .clip(PillShape)
                    .clickable(onClick = onAction)
                    .padding(start = 8.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = action,
                    style = MaterialTheme.typography.labelLarge,
                    color = PaleMint,
                )
                Spacer(Modifier.width(3.dp))
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_right),
                    contentDescription = null,
                    tint = Accent,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/* Ratings                                                             */
/* ------------------------------------------------------------------ */

@Composable
fun StarRating(
    value: Double?,
    modifier: Modifier = Modifier,
    starSize: Dp = 26.dp,
    onRate: ((Double?) -> Unit)? = null,
) {
    GemRating(
        value = value,
        modifier = modifier,
        gemSize = starSize,
        onRate = onRate,
    )
}

/** IGDB aggregate score, rendered as an editorial score line. */
@Composable
fun RatingBadge(score: Double?, count: Int? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(R.drawable.ic_star),
            contentDescription = null,
            tint = Accent,
            modifier = Modifier.size(13.dp),
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = score?.let { String.format(Locale.US, "%.1f", it / 10.0) } ?: "—",
            style = MaterialTheme.typography.titleLarge,
            color = MintWhite,
        )
        Text(
            text = " / 10",
            style = MaterialTheme.typography.labelSmall,
            color = Sage,
        )
        if (count != null && count > 0) {
            Spacer(Modifier.width(7.dp))
            Text(
                text = "· ${compactCount(count)} ratings",
                style = MaterialTheme.typography.labelSmall,
                color = Sage,
            )
        }
    }
}

/* ------------------------------------------------------------------ */
/* Empty state                                                         */
/* ------------------------------------------------------------------ */

@Composable
fun EmptyState(
    icon: Painter,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(Accent.copy(alpha = 0.055f))
                .border(
                    1.dp,
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(Accent.copy(alpha = 0.35f), GlassBorder),
                    ),
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = Accent,
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MintWhite,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(7.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = Sage,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        if (action != null) {
            Spacer(Modifier.height(20.dp))
            action()
        }
    }
}
