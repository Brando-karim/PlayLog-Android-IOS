@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.playlog.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.playlog.app.R

/* ------------------------------------------------------------------
 * Brand typefaces (SIL Open Font License, bundled as variable fonts)
 * ------------------------------------------------------------------ */

private fun serif(weight: FontWeight, opticalSize: Float, italic: Boolean = false): Font {
    val id = if (italic) R.font.source_serif_4_italic else R.font.source_serif_4
    return Font(
        resId = id,
        weight = weight,
        style = if (italic) FontStyle.Italic else FontStyle.Normal,
        variationSettings = FontVariation.Settings(
            FontVariation.Setting("opsz", opticalSize),
            FontVariation.weight(weight.weight),
        ),
    )
}

private fun sans(weight: FontWeight): Font = Font(
    resId = R.font.libre_franklin,
    weight = weight,
    variationSettings = FontVariation.Settings(
        FontVariation.weight(weight.weight),
    ),
)

/** Large editorial display: Source Serif 4 at display optical size. */
val SerifDisplay = FontFamily(
    serif(FontWeight.Normal, 40f),
    serif(FontWeight.Medium, 40f),
    serif(FontWeight.SemiBold, 40f),
    serif(FontWeight.Bold, 40f),
)

/** Reading serif for game titles and review bodies: text optical size. */
val SerifText = FontFamily(
    serif(FontWeight.Normal, 16f),
    serif(FontWeight.Medium, 16f),
    serif(FontWeight.SemiBold, 16f),
    serif(FontWeight.Bold, 16f),
    serif(FontWeight.Normal, 16f, italic = true),
    serif(FontWeight.SemiBold, 16f, italic = true),
)

/** UI sans: Libre Franklin. */
val LibreFranklin = FontFamily(
    sans(FontWeight.Normal),
    sans(FontWeight.Medium),
    sans(FontWeight.SemiBold),
    sans(FontWeight.Bold),
)

private fun display(size: Int, line: Int, tracking: Double, weight: FontWeight = FontWeight.SemiBold) =
    TextStyle(
        fontFamily = SerifDisplay,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = line.sp,
        letterSpacing = tracking.sp,
    )

private fun serifTitle(size: Int, line: Int) = TextStyle(
    fontFamily = SerifText,
    fontWeight = FontWeight.SemiBold,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = (-0.2).sp,
)

private fun ui(size: Int, line: Int, weight: FontWeight = FontWeight.Normal, tracking: Double = 0.0) =
    TextStyle(
        fontFamily = LibreFranklin,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = line.sp,
        letterSpacing = tracking.sp,
    )

val PlaylogTypography = Typography(
    displayLarge = display(46, 48, -2.2),
    displayMedium = display(40, 42, -1.9),
    displaySmall = display(34, 36, -1.5),
    headlineLarge = display(30, 34, -1.1),
    headlineMedium = display(26, 30, -0.8),
    headlineSmall = display(22, 27, -0.5),
    titleLarge = serifTitle(20, 24),
    titleMedium = ui(15, 20, FontWeight.SemiBold),
    titleSmall = ui(13, 18, FontWeight.SemiBold),
    bodyLarge = ui(16, 26),
    bodyMedium = ui(14, 21),
    bodySmall = ui(12, 18),
    labelLarge = ui(13, 16, FontWeight.SemiBold, 0.3),
    labelMedium = ui(11, 14, FontWeight.SemiBold, 1.0),
    labelSmall = ui(10, 13, FontWeight.SemiBold, 1.3),
)

/** Uppercase editorial section kicker (11sp, +0.16em, accent). */
val KickerStyle = TextStyle(
    fontFamily = LibreFranklin,
    fontWeight = FontWeight.SemiBold,
    fontSize = 11.sp,
    lineHeight = 14.sp,
    letterSpacing = 1.8.sp,
)

/** Metadata / micro-label (11sp, +0.08em, sage). */
val MetaStyle = TextStyle(
    fontFamily = LibreFranklin,
    fontWeight = FontWeight.Medium,
    fontSize = 11.sp,
    lineHeight = 15.sp,
    letterSpacing = 0.9.sp,
)

/** Serif pull-quote / review body. */
val ReadingStyle = TextStyle(
    fontFamily = SerifText,
    fontWeight = FontWeight.Normal,
    fontSize = 17.sp,
    lineHeight = 28.sp,
)

/** Serif italic editorial emphasis. */
val ReadingItalicStyle = ReadingStyle.copy(
    fontStyle = FontStyle.Italic,
    fontWeight = FontWeight.Normal,
)
