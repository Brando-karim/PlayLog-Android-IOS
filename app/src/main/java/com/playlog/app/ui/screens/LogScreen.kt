package com.playlog.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.playlog.app.PlaylogViewModel
import com.playlog.app.R
import com.playlog.app.data.Completion
import com.playlog.app.data.Game
import com.playlog.app.data.PlayStatus
import com.playlog.app.data.formatHoursValue
import com.playlog.app.data.parseTags
import com.playlog.app.ui.components.BrandLoader
import com.playlog.app.ui.components.Cover
import com.playlog.app.ui.components.FilterPill
import com.playlog.app.ui.components.GemRating
import com.playlog.app.ui.components.GlossyPillButton
import com.playlog.app.ui.components.Kicker
import com.playlog.app.ui.components.LedgerPanel
import com.playlog.app.ui.components.PlaylogField
import com.playlog.app.ui.components.ScreenHeader
import com.playlog.app.ui.components.TagRow
import com.playlog.app.ui.components.dayLabel
import com.playlog.app.ui.theme.Accent
import com.playlog.app.ui.theme.GlassBorder
import com.playlog.app.ui.theme.JungleInk
import com.playlog.app.ui.theme.MintWhite
import com.playlog.app.ui.theme.ReadingStyle
import com.playlog.app.ui.theme.Sage

@Composable
fun LogScreen(
    vm: PlaylogViewModel,
    gameId: Long,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val snapshot by vm.state.collectAsStateWithLifecycle()
    val cached = snapshot.game(gameId)
    var fetched by remember(gameId) { mutableStateOf<Game?>(null) }
    LaunchedEffect(gameId, cached) {
        if (cached == null && fetched == null) fetched = vm.loadGame(gameId)
    }
    val game = cached ?: fetched

    if (game == null) {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader(title = "Log game", kicker = "New entry", onBack = onBack)
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { BrandLoader() }
        }
        return
    }

    val existing = snapshot.entry(game.id)
    val previous = remember(game.id) {
        snapshot.diary.filter { it.gameId == game.id }.maxByOrNull { it.playedAt }
    }
    var status by remember(game.id) { mutableStateOf(existing?.playStatus ?: PlayStatus.PLAYED) }
    var rating by remember(game.id) { mutableStateOf(existing?.rating) }
    var review by remember(game.id) { mutableStateOf("") }
    var isReplay by remember(game.id) { mutableStateOf(false) }
    var platform by remember(game.id) { mutableStateOf(previous?.platform) }
    var completion by remember(game.id) { mutableStateOf(Completion.from(previous?.completion)) }
    var playtimeText by remember(game.id) {
        mutableStateOf(previous?.playtimeHours?.let { formatHoursValue(it) }.orEmpty())
    }
    var tagsText by remember(game.id) {
        mutableStateOf(previous?.tags?.joinToString(", ").orEmpty())
    }
    val parsedTags = parseTags(tagsText)
    val playedAt = remember { System.currentTimeMillis() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding(),
    ) {
        ScreenHeader(
            title = "Log game",
            kicker = "New entry",
            onBack = onBack,
        )

        Column(Modifier.padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(10.dp))

            LedgerPanel(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(13.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Cover(
                        game = game,
                        modifier = Modifier
                            .width(62.dp)
                            .aspectRatio(3f / 4f),
                        corner = 8.dp,
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = game.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = MintWhite,
                            maxLines = 2,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = listOfNotNull(
                                game.year?.toString(),
                                game.platforms.firstOrNull(),
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.labelSmall,
                            color = Sage,
                            maxLines = 1,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = playedAt.dayLabel(),
                            style = MaterialTheme.typography.labelSmall,
                            color = Accent,
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
            Kicker("Status")
            Spacer(Modifier.height(11.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PlayStatus.entries.forEach { option ->
                    FilterPill(
                        text = option.label,
                        selected = status == option,
                        onClick = { status = option },
                    )
                }
            }

            Spacer(Modifier.height(28.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Kicker("Details")
                Text(
                    text = "Optional",
                    style = MaterialTheme.typography.labelSmall,
                    color = Sage.copy(alpha = 0.7f),
                )
            }
            Spacer(Modifier.height(14.dp))

            DetailLabel("Platform")
            Spacer(Modifier.height(9.dp))
            if (game.platforms.isNotEmpty()) {
                // Keep a previously saved platform visible even when it is not in
                // this game's IGDB platform list, so re-logging does not hide it.
                val options = buildList {
                    platform?.takeIf { saved -> game.platforms.none { it == saved } }?.let { add(it) }
                    addAll(game.platforms.distinct())
                }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    options.forEach { option ->
                        FilterPill(
                            text = option,
                            selected = platform == option,
                            onClick = { platform = if (platform == option) null else option },
                        )
                    }
                }
            } else {
                PlaylogField(
                    value = platform.orEmpty(),
                    onValueChange = { platform = it.take(60).ifBlank { null } },
                    placeholder = "PC, PS5, Switch…",
                )
            }

            Spacer(Modifier.height(22.dp))
            DetailLabel("Completion")
            Spacer(Modifier.height(9.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Completion.entries.forEach { option ->
                    FilterPill(
                        text = option.label,
                        selected = completion == option,
                        onClick = { completion = if (completion == option) null else option },
                    )
                }
            }

            Spacer(Modifier.height(22.dp))
            DetailLabel("Hours played")
            Spacer(Modifier.height(9.dp))
            PlaylogField(
                value = playtimeText,
                onValueChange = { raw ->
                    val cleaned = raw.replace(',', '.').filter { it.isDigit() || it == '.' }
                    if (cleaned.count { it == '.' } <= 1 && cleaned.count { it.isDigit() } <= 5) {
                        playtimeText = cleaned
                    }
                },
                placeholder = "e.g. 12.5",
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next,
                ),
            )

            Spacer(Modifier.height(22.dp))
            DetailLabel("Tags")
            Spacer(Modifier.height(9.dp))
            PlaylogField(
                value = tagsText,
                onValueChange = { tagsText = it.take(200) },
                placeholder = "co-op, comfort, 100% run",
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            )
            if (parsedTags.isNotEmpty()) {
                Spacer(Modifier.height(9.dp))
                TagRow(parsedTags)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Comma separated · up to 8 tags · 24 characters each",
                style = MaterialTheme.typography.bodySmall,
                color = Sage.copy(alpha = 0.8f),
            )

            Spacer(Modifier.height(28.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Kicker("Your rating")
                if (rating != null) {
                    TextButton(onClick = { rating = null }) {
                        Text(
                            text = "Clear",
                            color = Sage,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            GemRating(
                value = rating,
                gemSize = 36.dp,
                spacing = 11.dp,
                onRate = { rating = it },
            )
            Spacer(Modifier.height(9.dp))
            Text(
                text = "Tap the left half of a gem for a half point. Tap the same value again to clear.",
                style = MaterialTheme.typography.bodySmall,
                color = Sage.copy(alpha = 0.8f),
            )

            Spacer(Modifier.height(28.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Kicker("Review")
                Text(
                    text = "Optional",
                    style = MaterialTheme.typography.labelSmall,
                    color = Sage.copy(alpha = 0.7f),
                )
            }
            Spacer(Modifier.height(10.dp))
            PlaylogField(
                value = review,
                onValueChange = { review = it },
                placeholder = "What did you think? No spoilers…",
                singleLine = false,
                minLines = 5,
                maxLines = 9,
                textStyle = ReadingStyle.copy(fontSize = 16.sp, lineHeight = 25.sp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
            )

            Spacer(Modifier.height(18.dp))
            LedgerPanel(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Played before",
                            style = MaterialTheme.typography.titleMedium,
                            color = MintWhite,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Marks this entry as a replay.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Sage,
                        )
                    }
                    Switch(
                        checked = isReplay,
                        onCheckedChange = { isReplay = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = JungleInk,
                            checkedTrackColor = Accent,
                            uncheckedThumbColor = Sage,
                            uncheckedTrackColor = GlassBorder,
                            uncheckedBorderColor = GlassBorder,
                        ),
                    )
                }
            }

            Spacer(Modifier.height(26.dp))
            GlossyPillButton(
                text = "Save entry",
                onClick = {
                    vm.saveLog(
                        game = game,
                        status = status,
                        rating = rating,
                        review = review,
                        isReplay = isReplay,
                        playedAt = playedAt,
                        platform = platform,
                        playtimeHours = playtimeText.toDoubleOrNull()?.takeIf { it in 0.0..9999.9 },
                        completion = completion,
                        tags = parsedTags,
                    )
                    onSaved()
                },
                modifier = Modifier.fillMaxWidth(),
                icon = painterResource(R.drawable.ic_check),
            )

            if (existing != null) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    TextButton(onClick = {
                        vm.setStatus(game, null)
                        onBack()
                    }) {
                        Text(
                            text = "Remove from library",
                            color = Sage,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
            Spacer(Modifier.height(34.dp))
        }
    }
}

@Composable
private fun DetailLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MintWhite.copy(alpha = 0.85f),
    )
}
