package com.playlog.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/* ------------------------------------------------------------------
 * Bioluminescent Editorial Ledger — colour tokens
 * ------------------------------------------------------------------ */

val Obsidian = Color(0xFF050806)
val ForestNight = Color(0xFF0B140E)
val Pine = Color(0xFF101A13)
val Moss = Color(0xFF0B130E)

val MintWhite = Color(0xFFEAFFF2)
val Sage = Color(0xFF8FB5A0)
val PaleMint = Color(0xFFCAFFDA)

val Accent = Color(0xFF3DFF8A)
val Teal = Color(0xFF00D4A0)
val JungleInk = Color(0xFF04110A)
val FocusRing = Color(0xFFB9FFD3)

val GlassBorder = Color(0x29E1FFED)      // rgba(225,255,237,.16)
val GlassRim = Color(0x5CFFFFFF)         // rgba(255,255,255,.36)
val GlassTop = Color(0x2EFFFFFF)         // rgba(255,255,255,.18)
val GlassFillHigh = Color(0x17FFFFFF)    // rgba(255,255,255,.09)
val RoseMuted = Color(0xFFD98B8B)

// Legacy aliases kept so older call sites keep compiling during the rollout.
val Lime = Accent
val Ink = Obsidian
val Surface1 = Pine
val Surface2 = Color(0xFF16221A)
val Muted = Sage
val Magenta = Color(0xFFE0A3B4)
val Sky = Teal
val Amber = Color(0xFFD9C58A)
val Danger = RoseMuted

/* ------------------------------------------------------------------
 * Gradient tokens
 * ------------------------------------------------------------------ */

/** The 155° app field: deep obsidian → forest night → deep obsidian. */
fun fieldBrush(width: Float, height: Float): Brush = Brush.linearGradient(
    colors = listOf(Obsidian, ForestNight, Obsidian),
    start = Offset(0f, 0f),
    end = Offset(width * 0.72f, height),
)

/** Glass panel fill: white top-left, white mid, teal bottom-right (145°). */
val GlassFill = Brush.linearGradient(
    0f to Color(0x16FFFFFF),
    0.52f to Color(0x0AFFFFFF),
    1f to Color(0x0A00D4A0),
)

/** Slightly brighter glass for raised / active containers. */
val GlassFillRaised = Brush.linearGradient(
    0f to Color(0x1FFFFFFF),
    0.5f to Color(0x14FFFFFF),
    1f to Color(0x1200D4A0),
)

/** Card rim: bright overhead light falling off down the panel. */
val RimBrush = Brush.linearGradient(
    0f to Color(0x8CFFFFFF),
    0.18f to Color(0x3DFFFFFF),
    0.45f to GlassBorder,
    1f to Color(0x14E1FFED),
)

val GlossyBrush = Brush.verticalGradient(
    0f to Color(0xFFA4FFC5),
    0.44f to Color(0xFF64FFA0),
    0.52f to Color(0xFF3DFF8A),
    1f to Color(0xFF20DF73),
)

val GlossyBorder = Brush.verticalGradient(
    0f to Color(0xB8FFFFFF),
    0.5f to Color(0x6BE2FFEF),
    1f to Color(0x33E2FFEF),
)

val GemBrush = Brush.linearGradient(
    0f to Color(0xFFB6FFD0),
    0.55f to Accent,
    1f to Teal,
)

val GemEmpty = Color(0x738FB5A0) // rgba(143,181,160,.45)

/* ------------------------------------------------------------------
 * Spacing / radii
 * ------------------------------------------------------------------ */

val ScreenPadding = 20.dp
val SectionGap = 30.dp
val CardRadius = 22.dp
val PanelRadius = 26.dp
val CoverRadius = 10.dp
val InputRadius = 14.dp
val PillRadius = 999.dp

val PillShape = RoundedCornerShape(PillRadius)
val PanelShape = RoundedCornerShape(PanelRadius)
val CardShape = RoundedCornerShape(CardRadius)
