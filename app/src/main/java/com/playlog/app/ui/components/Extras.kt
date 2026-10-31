package com.playlog.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.playlog.app.R
import com.playlog.app.data.DiaryEntry
import com.playlog.app.data.Game
import com.playlog.app.ui.theme.Accent
import com.playlog.app.ui.theme.GlassBorder
import com.playlog.app.ui.theme.MintWhite
import com.playlog.app.ui.theme.PaleMint
import com.playlog.app.ui.theme.Sage
import com.playlog.app.ui.theme.SerifText
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

fun Long.dayLabel(): String =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()))

fun Long.monthLabel(): String =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))

fun Long.todayLabel(): String =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()))

/** Compact ledger stat cell. The passed colour only marks the hairline dot. */
@Composable
fun StatCard(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(15.dp))
            .background(Color.White.copy(alpha = 0.032f))
            .border(1.dp, GlassBorder, RoundedCornerShape(15.dp))
            .padding(horizontal = 13.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(4.dp)
                    .background(accent, CircleShape),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    letterSpacing = 1.0.sp,
                ),
                color = Sage,
                maxLines = 1,
            )
        }
        Spacer(Modifier.height(7.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = MintWhite,
        )
    }
}

/**
 * A diary entry set as a ledger row: small cover plate, serif title, date and
 * score, then a two-line review excerpt.
 */
@Composable
fun DiaryRow(
    entry: DiaryEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onDelete: (() -> Unit)? = null,
) {
    val placeholder = Game(
        id = entry.gameId,
        name = entry.gameName,
        coverUrl = entry.coverUrl,
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(17.dp))
            .background(Color.White.copy(alpha = 0.030f))
            .border(1.dp, GlassBorder, RoundedCornerShape(17.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Cover(
            game = placeholder,
            modifier = Modifier
                .width(42.dp)
                .aspectRatio(3f / 4f),
            corner = 7.dp,
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = entry.gameName,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = SerifText,
                        fontSize = 15.sp,
                        lineHeight = 19.sp,
                    ),
                    color = MintWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (entry.isReplay) {
                    Spacer(Modifier.width(7.dp))
                    AccentBadge("Replay")
                }
            }
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = entry.playedAt.dayLabel(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        letterSpacing = 0.5.sp,
                    ),
                    color = Sage,
                )
                if (entry.rating != null) {
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        painter = painterResource(R.drawable.ic_star),
                        contentDescription = null,
                        tint = Accent,
                        modifier = Modifier.size(9.dp),
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = String.format(Locale.US, "%.1f", entry.rating),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = PaleMint,
                    )
                }
            }
            if (entry.review.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = entry.review,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = SerifText,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                    ),
                    color = Sage,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (onDelete != null) {
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onDelete),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = "Delete entry",
                    tint = Sage.copy(alpha = 0.75f),
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

/** Horizontally scrolling tag capsules; deliberate single-line rhythm. */
@Composable
fun TagRow(tags: List<String>, modifier: Modifier = Modifier) {
    if (tags.isEmpty()) return
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        tags.take(8).forEach { tag ->
            Box(
                Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.035f))
                    .border(1.dp, GlassBorder, CircleShape)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Text(
                    text = tag,
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.4.sp),
                    color = Sage,
                    maxLines = 1,
                )
            }
        }
    }
}
