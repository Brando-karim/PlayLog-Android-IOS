package com.playlog.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.playlog.app.data.Game
import com.playlog.app.ui.theme.CoverRadius
import com.playlog.app.ui.theme.GlassBorder
import com.playlog.app.ui.theme.MintWhite
import com.playlog.app.ui.theme.Sage
import com.playlog.app.ui.theme.SerifDisplay

/** Branded fallback fields, ordered like a shelf of dark first-editions. */
private val coverGradients = listOf(
    Color(0xFF14362A) to Color(0xFF07110C),
    Color(0xFF102F35) to Color(0xFF060F11),
    Color(0xFF2A2920) to Color(0xFF0D0C08),
    Color(0xFF182F1B) to Color(0xFF080F09),
    Color(0xFF252333) to Color(0xFF0B0A10),
    Color(0xFF33301F) to Color(0xFF100E08),
)

fun coverGradientFor(id: Long): List<Color> {
    val pair = coverGradients[(id and 0x7FFFFFFF).toInt() % coverGradients.size]
    return listOf(pair.first, pair.second)
}

/**
 * 3:4 cover plate with a top-42% gloss, hairline rim, and a branded fallback
 * when IGDB has no artwork.
 */
@Composable
fun Cover(
    game: Game,
    modifier: Modifier = Modifier,
    corner: Dp = CoverRadius,
    showSheen: Boolean = true,
) {
    val shape = RoundedCornerShape(corner)
    val gradient = remember(game.id) {
        Brush.linearGradient(
            colors = coverGradientFor(game.id),
            start = androidx.compose.ui.geometry.Offset.Zero,
            end = androidx.compose.ui.geometry.Offset.Infinite,
        )
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(gradient)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.30f),
                        GlassBorder,
                        Color.Black.copy(alpha = 0.22f),
                    ),
                ),
                shape = shape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initialsOf(game.name),
            color = MintWhite.copy(alpha = 0.34f),
            fontFamily = SerifDisplay,
            fontWeight = FontWeight.SemiBold,
            fontSize = (corner.value * 1.5f + 14f).coerceIn(18f, 34f).sp,
        )

        if (!game.coverUrl.isNullOrBlank()) {
            AsyncImage(
                model = game.coverUrl,
                contentDescription = game.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (showSheen) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.White.copy(alpha = 0.18f),
                            0.20f to Color.White.copy(alpha = 0.045f),
                            0.42f to Color.Transparent,
                            0.62f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.26f),
                        ),
                    ),
            )
        }
    }
}

/**
 * Fanned shelf of covers used on list and collection cards. Back cards peek
 * out to the right with a slight rotation, like books pulled forward.
 */
@Composable
fun CoverStack(
    games: List<Game>,
    modifier: Modifier = Modifier,
    coverWidth: Dp = 54.dp,
    count: Int = 3,
    corner: Dp = 8.dp,
) {
    val visible = games.take(count)
    if (visible.isEmpty()) {
        Box(
            modifier
                .width(coverWidth)
                .aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(corner))
                .background(Color.White.copy(alpha = 0.04f))
                .border(1.dp, GlassBorder, RoundedCornerShape(corner)),
        )
        return
    }

    Box(
        modifier
            .width(coverWidth + 15.dp * (visible.size - 1))
            .height(coverWidth * 4 / 3 + 10.dp),
    ) {
        for (index in visible.indices.reversed()) {
            val game = visible[index]
            Cover(
                game = game,
                modifier = Modifier
                    .offset(x = (index * 15).dp, y = (index * 2).dp)
                    .width(coverWidth)
                    .aspectRatio(3f / 4f)
                    .graphicsLayer {
                        rotationZ = -4f + index * 4f
                    }
                    .shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(corner),
                        ambientColor = Color.Black,
                        spotColor = Color.Black,
                    ),
                corner = corner,
                showSheen = index == 0,
            )
        }
    }
}

/** Tiny monogram used in empty states and profile headers. */
@Composable
fun Monogram(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
) {
    Box(
        modifier = modifier
            .width(size)
            .height(size)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF1A4530), Color(0xFF0A1710)),
                ),
            )
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(
                        com.playlog.app.ui.theme.Accent.copy(alpha = 0.7f),
                        GlassBorder,
                    ),
                ),
                androidx.compose.foundation.shape.CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.trim().take(1).uppercase().ifBlank { "P" },
            color = com.playlog.app.ui.theme.PaleMint,
            fontFamily = SerifDisplay,
            fontWeight = FontWeight.SemiBold,
            fontSize = (size.value * 0.42f).sp,
        )
    }
}

/** Kept for older call sites. */
@Composable
fun CoverCaption(game: Game, modifier: Modifier = Modifier) {
    Text(
        text = game.name,
        modifier = modifier,
        color = Sage,
        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
    )
}
