package com.playlog.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.playlog.app.PlaylogViewModel
import com.playlog.app.R
import com.playlog.app.ui.components.DiaryRow
import com.playlog.app.ui.components.EmptyState
import com.playlog.app.ui.components.Kicker
import com.playlog.app.ui.components.ScreenHeader
import com.playlog.app.ui.components.monthLabel
import com.playlog.app.ui.theme.GlassBorder
import com.playlog.app.ui.theme.Sage

@Composable
fun DiaryScreen(
    vm: PlaylogViewModel,
    onBack: () -> Unit,
    onGame: (Long) -> Unit,
) {
    val snapshot by vm.state.collectAsStateWithLifecycle()
    val grouped = snapshot.diary
        .sortedByDescending { it.playedAt }
        .groupBy { it.playedAt.monthLabel() }
    val reviewCount = snapshot.diary.count { it.review.isNotBlank() }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Diary",
            kicker = "The ledger",
            onBack = onBack,
        )
        Column(Modifier.padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${snapshot.diary.size} entries · $reviewCount reviews",
                style = MaterialTheme.typography.labelSmall,
                color = Sage,
            )
        }

        if (grouped.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(
                    icon = painterResource(R.drawable.ic_book),
                    title = "No diary entries",
                    message = "Every game you log appears here, grouped by month.",
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 34.dp),
            ) {
                grouped.forEach { (month, entries) ->
                    item(key = "header-$month") {
                        Row(
                            modifier = Modifier.padding(bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Kicker(month)
                            Spacer(Modifier.padding(horizontal = 6.dp))
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(1.dp)
                                    .background(GlassBorder),
                            )
                        }
                    }
                    items(entries, key = { it.id }) { entry ->
                        DiaryRow(
                            entry = entry,
                            onClick = { onGame(entry.gameId) },
                            onDelete = { vm.deleteDiary(entry.id) },
                            modifier = Modifier.padding(bottom = 10.dp),
                        )
                    }
                    item(key = "gap-$month") {
                        Spacer(Modifier.height(14.dp))
                    }
                }
            }
        }
    }
}
