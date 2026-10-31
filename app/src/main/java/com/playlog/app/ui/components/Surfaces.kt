package com.playlog.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.playlog.app.R
import com.playlog.app.ui.theme.Accent
import com.playlog.app.ui.theme.CardRadius
import com.playlog.app.ui.theme.GlassBorder
import com.playlog.app.ui.theme.GlassFill
import com.playlog.app.ui.theme.GlassFillRaised
import com.playlog.app.ui.theme.GlossyBorder
import com.playlog.app.ui.theme.GlossyBrush
import com.playlog.app.ui.theme.InputRadius
import com.playlog.app.ui.theme.JungleInk
import com.playlog.app.ui.theme.KickerStyle
import com.playlog.app.ui.theme.LibreFranklin
import com.playlog.app.ui.theme.MintWhite
import com.playlog.app.ui.theme.MetaStyle
import com.playlog.app.ui.theme.Obsidian
import com.playlog.app.ui.theme.PaleMint
import com.playlog.app.ui.theme.PillShape
import com.playlog.app.ui.theme.RimBrush
import com.playlog.app.ui.theme.Sage
import com.playlog.app.ui.theme.Teal
import java.util.Random
import kotlin.math.roundToInt

/* ------------------------------------------------------------------ */
/* Ambient field                                                       */
/* ------------------------------------------------------------------ */

private fun grainBitmap(size: Int = 140): ImageBitmap {
    val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
    val random = Random(17)
    val pixels = IntArray(size * size) {
        val alpha = random.nextInt(20)
        (alpha shl 24) or 0x00FFFFFF
    }
    bitmap.setPixels(pixels, 0, size, 0, 0, size, size)
    return bitmap.asImageBitmap()
}

/**
 * Deep forest field with two slow-breathing bioluminescent glows and a
 * fine analog grain, drawn behind every screen.
 */
@Composable
fun AmbientBackground(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "ambient")
    val breath by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(24_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breath",
    )
    val grain = remember { grainBitmap() }
    val grainBrush = remember(grain) {
        ShaderBrush(ImageShader(grain, TileMode.Repeated, TileMode.Repeated))
    }

    Canvas(modifier.fillMaxSize()) {
        drawAmbientField(breath)
        drawRect(brush = grainBrush, alpha = 0.55f)
    }
}

private fun DrawScope.drawAmbientField(breath: Float) {
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(Obsidian, Color(0xFF0B140E), Obsidian),
            start = Offset.Zero,
            end = Offset(size.width * 0.75f, size.height),
        ),
    )

    val topCenter = Offset(size.width * (0.05f + 0.06f * breath), -size.height * 0.02f)
    val topRadius = size.width * 1.05f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Teal.copy(alpha = 0.17f + 0.05f * breath), Color.Transparent),
            center = topCenter,
            radius = topRadius,
        ),
        radius = topRadius,
        center = topCenter,
    )

    val lowCenter = Offset(size.width * (1.02f - 0.05f * breath), size.height * (0.72f + 0.03f * breath))
    val lowRadius = size.width * 0.95f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Accent.copy(alpha = 0.07f + 0.03f * breath), Color.Transparent),
            center = lowCenter,
            radius = lowRadius,
        ),
        radius = lowRadius,
        center = lowCenter,
    )
}

/* ------------------------------------------------------------------ */
/* Panels                                                              */
/* ------------------------------------------------------------------ */

/**
 * Frosted ledger panel: gradient glass fill, a bright overhead rim, and a
 * soft teal bloom. Optional press feedback when [onClick] is provided.
 */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(CardRadius),
    elevated: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.984f else 1f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 700f),
        label = "panel-press",
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = if (elevated) 18.dp else 8.dp,
                shape = shape,
                ambientColor = Teal.copy(alpha = 0.20f),
                spotColor = Accent.copy(alpha = 0.14f),
            )
            .clip(shape)
            .background(if (elevated) GlassFillRaised else GlassFill)
            .border(1.dp, RimBrush, shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            ),
    ) {
        Column(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}

@Composable
fun LedgerPanel(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    GlassPanel(
        modifier = modifier,
        shape = com.playlog.app.ui.theme.ledgerShape(),
        elevated = true,
        contentPadding = contentPadding,
        onClick = onClick,
        content = content,
    )
}

/* ------------------------------------------------------------------ */
/* Buttons                                                             */
/* ------------------------------------------------------------------ */

/**
 * The signature glossy mint pill. Four-stop vertical gradient, bright top
 * rim, teal bloom, and an inner trailing icon capsule.
 */
@Composable
fun GlossyPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: Painter? = null,
    compact: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 650f),
        label = "cta-press",
    )

    Row(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = 14.dp,
                shape = PillShape,
                ambientColor = Accent.copy(alpha = 0.55f),
                spotColor = Accent.copy(alpha = 0.45f),
            )
            .clip(PillShape)
            .background(GlossyBrush)
            .border(1.dp, GlossyBorder, PillShape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .heightIn(min = if (compact) 42.dp else 52.dp)
            .padding(
                start = if (compact) 16.dp else 22.dp,
                end = if (icon != null) 7.dp else if (compact) 16.dp else 22.dp,
                top = if (compact) 7.dp else 9.dp,
                bottom = if (compact) 7.dp else 9.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            color = JungleInk,
            style = if (compact) {
                MaterialTheme.typography.labelLarge
            } else {
                MaterialTheme.typography.titleMedium
            },
            fontWeight = FontWeight.SemiBold,
        )
        if (icon != null) {
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .size(if (compact) 30.dp else 36.dp)
                    .background(Color.White.copy(alpha = 0.32f), CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.45f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = icon,
                    contentDescription = null,
                    tint = JungleInk,
                    modifier = Modifier.size(if (compact) 15.dp else 17.dp),
                )
            }
        }
    }
}

/** Quiet glass pill for secondary actions and filters. */
@Composable
fun GhostPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    icon: Painter? = null,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 700f),
        label = "ghost-press",
    )
    val shape = PillShape

    Row(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(if (selected) Accent.copy(alpha = 0.13f) else Color.White.copy(alpha = 0.04f))
            .border(
                width = 1.dp,
                brush = if (selected) {
                    Brush.verticalGradient(listOf(Accent.copy(alpha = 0.55f), Accent.copy(alpha = 0.22f)))
                } else {
                    RimBrush
                },
                shape = shape,
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .heightIn(min = 42.dp)
            .padding(horizontal = 15.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = if (selected) Accent else MintWhite.copy(alpha = 0.85f),
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(7.dp))
        }
        Text(
            text = text,
            color = if (selected) Accent else MintWhite.copy(alpha = 0.86f),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
fun CircleIconButton(
    painter: Painter,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MintWhite,
    size: Dp = 42.dp,
    iconSize: Dp = 19.dp,
    accent: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 700f),
        label = "icon-press",
    )
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .size(size)
            .clip(CircleShape)
            .background(if (accent) Accent.copy(alpha = 0.13f) else Color(0xB30A120D))
            .border(
                1.dp,
                if (accent) {
                    Brush.verticalGradient(listOf(Accent.copy(alpha = 0.50f), Accent.copy(alpha = 0.20f)))
                } else {
                    RimBrush
                },
                CircleShape,
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painter,
            contentDescription = contentDescription,
            tint = if (accent) Accent else tint,
            modifier = Modifier.size(iconSize),
        )
    }
}

/* ------------------------------------------------------------------ */
/* Editorial primitives                                                */
/* ------------------------------------------------------------------ */

@Composable
fun Kicker(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Accent,
) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        style = KickerStyle,
        color = color,
        maxLines = 1,
    )
}

@Composable
fun Wordmark(
    modifier: Modifier = Modifier,
    size: Int = 26,
) {
    Text(
        text = buildAnnotatedString {
            withStyle(
                SpanStyle(
                    fontFamily = LibreFranklin,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.9).sp,
                ),
            ) {
                append("playlog")
            }
            withStyle(SpanStyle(color = Accent, fontWeight = FontWeight.Bold)) {
                append(".")
            }
        },
        modifier = modifier,
        color = MintWhite,
        fontSize = size.sp,
    )
}

@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(Color.Transparent, GlassBorder, Color.Transparent),
                ),
            ),
    )
}

@Composable
fun SectionHeading(
    kicker: String,
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(modifier.fillMaxWidth()) {
        Kicker(kicker)
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.headlineSmall,
                color = MintWhite,
            )
            if (action != null && onAction != null) {
                Row(
                    modifier = Modifier
                        .clip(PillShape)
                        .clickable(onClick = onAction)
                        .padding(start = 10.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = action,
                        style = MetaStyle,
                        color = PaleMint,
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_right),
                        contentDescription = null,
                        tint = Accent,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))
    }
}

@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    kicker: String? = null,
    onBack: (() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            CircleIconButton(
                painter = painterResource(R.drawable.ic_back),
                contentDescription = "Back",
                onClick = onBack,
            )
            Spacer(Modifier.width(13.dp))
        }
        Column(Modifier.weight(1f)) {
            if (kicker != null) {
                Kicker(kicker)
                Spacer(Modifier.height(3.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MintWhite,
                maxLines = 1,
            )
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, content = trailing)
        }
    }
}

/* ------------------------------------------------------------------ */
/* Inputs                                                              */
/* ------------------------------------------------------------------ */

@Composable
fun PlaylogField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    label: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else 6,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    leadingIcon: Painter? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = placeholder?.let {
            { Text(it, style = textStyle, color = Sage.copy(alpha = 0.7f)) }
        },
        label = label?.let {
            { Text(it, style = MetaStyle, color = Sage) }
        },
        leadingIcon = leadingIcon?.let {
            {
                Icon(
                    painter = it,
                    contentDescription = null,
                    tint = Sage,
                    modifier = Modifier.size(19.dp),
                )
            }
        },
        textStyle = textStyle,
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        shape = RoundedCornerShape(InputRadius),
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = MintWhite,
            unfocusedTextColor = MintWhite,
            focusedContainerColor = Color.White.copy(alpha = 0.055f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.035f),
            cursorColor = Accent,
            focusedBorderColor = Accent.copy(alpha = 0.55f),
            unfocusedBorderColor = GlassBorder,
            focusedPlaceholderColor = Sage.copy(alpha = 0.7f),
            unfocusedPlaceholderColor = Sage.copy(alpha = 0.7f),
            focusedLabelColor = Accent,
            unfocusedLabelColor = Sage,
        ),
    )
}

/* ------------------------------------------------------------------ */
/* Dialogs, loaders, skeletons                                         */
/* ------------------------------------------------------------------ */

@Composable
fun PlaylogDialog(
    onDismissRequest: () -> Unit,
    title: String,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        containerColor = Color(0xFF0C1710),
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(26.dp),
        titleContentColor = MintWhite,
        textContentColor = Sage,
        title = {
            Kicker("Playlog")
            Spacer(Modifier.height(6.dp))
            Text(title, style = MaterialTheme.typography.headlineSmall, color = MintWhite)
        },
        text = {
            Column { content() }
        },
        confirmButton = confirmButton,
        dismissButton = dismissButton,
    )
}

@Composable
fun BrandLoader(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 54.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            color = Accent,
            strokeWidth = 2.dp,
            trackColor = GlassBorder,
            modifier = Modifier.size(32.dp),
        )
    }
}

@Composable
fun SkeletonBlock(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.035f,
        targetValue = 0.105f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "skeleton-alpha",
    )
    Box(modifier.background(Color.White.copy(alpha = alpha), shape))
}

/** Two-up grid of cover skeletons used while the catalogue loads. */
@Composable
fun SkeletonCoverGrid(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(22.dp)) {
        repeat(2) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                repeat(2) {
                    Column(Modifier.weight(1f)) {
                        SkeletonBlock(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(196.dp),
                            shape = RoundedCornerShape(12.dp),
                        )
                        Spacer(Modifier.height(9.dp))
                        SkeletonBlock(
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .height(12.dp),
                            shape = RoundedCornerShape(6.dp),
                        )
                        Spacer(Modifier.height(6.dp))
                        SkeletonBlock(
                            modifier = Modifier
                                .fillMaxWidth(0.45f)
                                .height(9.dp),
                            shape = RoundedCornerShape(5.dp),
                        )
                    }
                }
            }
        }
    }
}

/** Press haptic used by ratings and confirmations. */
@Composable
fun rememberBrandHaptics(): () -> Unit {
    val haptics = LocalHapticFeedback.current
    return remember(haptics) {
        {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
}
