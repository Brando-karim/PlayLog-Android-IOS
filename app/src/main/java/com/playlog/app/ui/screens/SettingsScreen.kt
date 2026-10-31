package com.playlog.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.playlog.app.BuildConfig
import com.playlog.app.PlaylogViewModel
import com.playlog.app.ui.components.CountBadge
import com.playlog.app.ui.components.GhostPill
import com.playlog.app.ui.components.Hairline
import com.playlog.app.ui.components.Kicker
import com.playlog.app.ui.components.LedgerPanel
import com.playlog.app.ui.components.PlaylogDialog
import com.playlog.app.ui.components.PlaylogField
import com.playlog.app.ui.components.ScreenHeader
import com.playlog.app.ui.components.Wordmark
import com.playlog.app.ui.theme.Accent
import com.playlog.app.ui.theme.Danger
import com.playlog.app.ui.theme.GlassBorder
import com.playlog.app.ui.theme.MintWhite
import com.playlog.app.ui.theme.Sage
import com.playlog.app.ui.theme.Teal

@Composable
fun SettingsScreen(
    vm: PlaylogViewModel,
    onBack: () -> Unit,
) {
    val snapshot by vm.state.collectAsStateWithLifecycle()
    var editingName by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        ScreenHeader(
            title = "Settings",
            kicker = "Configuration",
            onBack = onBack,
        )

        Column(Modifier.padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(12.dp))

            SettingsSection("Data source") {
                LedgerPanel(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(6.dp)
                                .background(if (vm.igdbConfigured) Accent else Sage, CircleShape),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (vm.igdbConfigured) "IGDB connected" else "Demo catalogue",
                            style = MaterialTheme.typography.titleMedium,
                            color = MintWhite,
                            modifier = Modifier.weight(1f),
                        )
                        CountBadge(
                            text = if (vm.igdbConfigured) "Live" else "Offline",
                            color = if (vm.igdbConfigured) Accent else Sage,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (vm.igdbConfigured) {
                            "Searches and popular games come from the live IGDB catalogue."
                        } else {
                            "The app ships with a small sample catalogue. Add Twitch application credentials to use the full IGDB database."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Sage,
                    )
                    if (!vm.igdbConfigured) {
                        Spacer(Modifier.height(14.dp))
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(com.playlog.app.ui.theme.Obsidian.copy(alpha = 0.55f))
                                .border(1.dp, GlassBorder, RoundedCornerShape(10.dp))
                                .padding(12.dp),
                        ) {
                            Text(
                                text = "local.properties",
                                style = MaterialTheme.typography.labelSmall,
                                color = Accent,
                            )
                            Spacer(Modifier.height(5.dp))
                            Text(
                                text = "IGDB_CLIENT_ID=your_id\nIGDB_CLIENT_SECRET=your_secret",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp,
                                ),
                                color = MintWhite.copy(alpha = 0.85f),
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "Create a free app at dev.twitch.tv/console/apps, then rebuild the APK. Credentials are compiled into BuildConfig — fine for a personal build, but use a backend proxy before sharing publicly.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Sage.copy(alpha = 0.85f),
                        )
                    }
                }
            }

            SettingsSection("Profile") {
                LedgerPanel(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Display name",
                                style = MaterialTheme.typography.titleMedium,
                                color = MintWhite,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = snapshot.displayName,
                                style = MaterialTheme.typography.bodySmall,
                                color = Sage,
                            )
                        }
                        GhostPill(
                            text = "Edit",
                            onClick = { editingName = true },
                            icon = androidx.compose.ui.res.painterResource(com.playlog.app.R.drawable.ic_edit),
                        )
                    }
                }
            }

            SettingsSection("Library") {
                LedgerPanel(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Clear library",
                                style = MaterialTheme.typography.titleMedium,
                                color = MintWhite,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "${snapshot.library.size} games · ${snapshot.diary.size} diary entries",
                                style = MaterialTheme.typography.bodySmall,
                                color = Sage,
                            )
                        }
                        TextButton(onClick = { confirmClear = true }) {
                            Text(
                                text = "Clear",
                                color = Danger,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
            }

            SettingsSection("About") {
                LedgerPanel(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Wordmark(size = 23)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "v${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Sage,
                            modifier = Modifier.padding(bottom = 2.dp),
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "A game diary for logging what you play, rating it, and building lists. An original app — not affiliated with Letterboxd, IGDB, or Twitch.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Sage,
                    )
                    Spacer(Modifier.height(9.dp))
                    Text(
                        text = "Game metadata by IGDB. All personal data is stored locally on this device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Sage,
                    )
                    Spacer(Modifier.height(16.dp))
                    Hairline()
                    Spacer(Modifier.height(14.dp))
                    CreditRow("Typefaces", "Source Serif 4 · Libre Franklin — SIL OFL")
                    CreditRow("Icon pack", "Phosphor Icons — MIT")
                    CreditRow("Built with", "Jetpack Compose · Coil · OkHttp")
                }
            }

            Spacer(Modifier.height(36.dp))
        }
    }

    if (editingName) {
        var name by remember { mutableStateOf(snapshot.displayName) }
        PlaylogDialog(
            onDismissRequest = { editingName = false },
            title = "Display name",
            confirmButton = {
                TextButton(onClick = {
                    vm.setDisplayName(name)
                    editingName = false
                }) { Text("Save", color = Accent) }
            },
            dismissButton = {
                TextButton(onClick = { editingName = false }) { Text("Cancel", color = Sage) }
            },
        ) {
            PlaylogField(
                value = name,
                onValueChange = { name = it },
                label = "Name",
                singleLine = true,
            )
        }
    }

    if (confirmClear) {
        PlaylogDialog(
            onDismissRequest = { confirmClear = false },
            title = "Clear library?",
            confirmButton = {
                TextButton(onClick = {
                    vm.clearLibrary()
                    confirmClear = false
                }) { Text("Clear", color = Danger) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("Cancel", color = Sage) }
            },
        ) {
            Text(
                text = "This deletes your library, diary, and favourites from this device. Lists stay.",
                style = MaterialTheme.typography.bodyMedium,
                color = Sage,
            )
        }
    }
}

@Composable
private fun SettingsSection(
    kicker: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column {
        Kicker(kicker, color = Teal)
        Spacer(Modifier.height(9.dp))
        content()
        Spacer(Modifier.height(26.dp))
    }
}

@Composable
private fun CreditRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, letterSpacing = 1.0.sp),
            color = Sage.copy(alpha = 0.7f),
            modifier = Modifier.width(72.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.3.sp),
            color = MintWhite.copy(alpha = 0.8f),
            modifier = Modifier.weight(1f),
        )
    }
}
