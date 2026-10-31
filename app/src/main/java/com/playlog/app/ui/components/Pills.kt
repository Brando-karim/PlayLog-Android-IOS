package com.playlog.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.playlog.app.data.PlayStatus
import com.playlog.app.ui.theme.Accent
import com.playlog.app.ui.theme.Danger
import com.playlog.app.ui.theme.GlassBorder
import com.playlog.app.ui.theme.MintWhite
import com.playlog.app.ui.theme.PaleMint
import com.playlog.app.ui.theme.PillShape
import com.playlog.app.ui.theme.Sage
import com.playlog.app.ui.theme.Teal

/** Quiet status capsule. One accent, with a single restrained tint for drops. */
@Composable
fun StatusPill(status: PlayStatus, modifier: Modifier = Modifier) {
    val color = when (status) {
        PlayStatus.PLAYING -> Accent
        PlayStatus.PLAYED -> PaleMint
        PlayStatus.BACKLOG -> Sage
        PlayStatus.DROPPED -> Danger
    }
    Row(
        modifier = modifier
            .clip(PillShape)
            .background(color.copy(alpha = 0.09f))
            .border(1.dp, color.copy(alpha = 0.24f), PillShape)
            .padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(5.dp)
                .background(color, CircleShape),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = status.label.uppercase(),
            color = color,
            fontSize = 9.sp,
            letterSpacing = 1.0.sp,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
        )
    }
}

/** Filter / sort capsule used across Library, Discover and Log. */
@Composable
fun FilterPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(PillShape)
            .background(if (selected) Accent.copy(alpha = 0.13f) else Color.White.copy(alpha = 0.035f))
            .border(
                1.dp,
                if (selected) Accent.copy(alpha = 0.44f) else GlassBorder,
                PillShape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (selected) Accent else Sage,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
        )
    }
}

/** Small mint label, e.g. REPLAY or PRIVATE. */
@Composable
fun AccentBadge(
    text: String,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
) {
    Row(
        modifier = modifier
            .clip(PillShape)
            .background(Accent.copy(alpha = 0.10f))
            .border(1.dp, Accent.copy(alpha = 0.22f), PillShape)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = Accent,
                modifier = Modifier.size(10.dp),
            )
            Spacer(Modifier.width(4.dp))
        }
        Text(
            text = text.uppercase(),
            color = Accent,
            fontSize = 9.sp,
            letterSpacing = 1.0.sp,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
        )
    }
}

/** Neutral micro-label on glass. */
@Composable
fun MetaBadge(
    text: String,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
) {
    Row(
        modifier = modifier
            .clip(PillShape)
            .background(Color.White.copy(alpha = 0.035f))
            .border(1.dp, GlassBorder, PillShape)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = MintWhite.copy(alpha = 0.7f),
                modifier = Modifier.size(10.dp),
            )
            Spacer(Modifier.width(4.dp))
        }
        Text(
            text = text.uppercase(),
            color = Sage,
            fontSize = 9.sp,
            letterSpacing = 0.9.sp,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
        )
    }
}

/** Rounded count chip used beside live / offline data state. */
@Composable
fun CountBadge(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Teal,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(5.dp))
            .background(color.copy(alpha = 0.10f))
            .border(1.dp, color.copy(alpha = 0.22f), RoundedCornerShape(5.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = text.uppercase(),
            color = color,
            fontSize = 8.sp,
            letterSpacing = 1.0.sp,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
        )
    }
}
