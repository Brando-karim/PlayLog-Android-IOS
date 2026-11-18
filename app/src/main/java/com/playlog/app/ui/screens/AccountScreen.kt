package com.playlog.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.playlog.app.PlaylogViewModel
import com.playlog.app.ui.components.BrandLoader
import com.playlog.app.ui.components.GhostPill
import com.playlog.app.ui.components.GlossyPillButton
import com.playlog.app.ui.components.Hairline
import com.playlog.app.ui.components.Kicker
import com.playlog.app.ui.components.LedgerPanel
import com.playlog.app.ui.components.PlaylogField
import com.playlog.app.ui.components.ProfileAvatar
import com.playlog.app.ui.components.ScreenHeader
import com.playlog.app.ui.theme.Accent
import com.playlog.app.ui.theme.Danger
import com.playlog.app.ui.theme.GlassBorder
import com.playlog.app.ui.theme.MintWhite
import com.playlog.app.ui.theme.Sage
import com.playlog.app.ui.theme.Teal
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class AuthMode { SignIn, Create }

@Composable
fun AccountScreen(
    vm: PlaylogViewModel,
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
) {
    val account by vm.account.collectAsStateWithLifecycle()
    val snapshot by vm.state.collectAsStateWithLifecycle()
    var mode by remember { mutableStateOf(AuthMode.SignIn) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        ScreenHeader(
            title = "Account",
            kicker = "Cloud sync",
            onBack = onBack,
        )

        Column(Modifier.padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(10.dp))
            Kicker("One ledger, every device", color = Teal)
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (account.signedIn) "Your Playlog is linked" else "Sign in to Playlog",
                style = MaterialTheme.typography.displaySmall,
                color = MintWhite,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Use the same account you use on the Playlog website. Your library, diary, " +
                    "reviews, lists, favourites and profile stay in sync between the web and this phone.",
                style = MaterialTheme.typography.bodyMedium,
                color = Sage,
            )
            Spacer(Modifier.height(22.dp))

            if (!vm.cloudConfigured) {
                LedgerPanel(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(17.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(7.dp).background(Danger, CircleShape))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Cloud sync is not configured",
                            style = MaterialTheme.typography.titleMedium,
                            color = MintWhite,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Add SUPABASE_URL and SUPABASE_ANON_KEY to local.properties, then rebuild the app.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Sage,
                    )
                }
                Spacer(Modifier.height(26.dp))
            } else if (account.signedIn) {
                SignedInPanel(
                    displayName = snapshot.displayName,
                    avatarUrl = snapshot.avatarUrl,
                    email = account.email.orEmpty(),
                    busy = account.busy,
                    lastSync = account.lastSync,
                    message = account.message,
                    error = account.error,
                    onEditProfile = onEditProfile,
                    onSync = vm::syncNow,
                    onSignOut = vm::signOut,
                )
            } else {
                LedgerPanel(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(18.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GhostPill(
                            text = "Sign in",
                            onClick = { mode = AuthMode.SignIn },
                            selected = mode == AuthMode.SignIn,
                            modifier = Modifier.weight(1f),
                        )
                        GhostPill(
                            text = "Create account",
                            onClick = { mode = AuthMode.Create },
                            selected = mode == AuthMode.Create,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.height(18.dp))
                    PlaylogField(
                        value = email,
                        onValueChange = { email = it },
                        label = "Email",
                        placeholder = "you@example.com",
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next,
                        ),
                    )
                    Spacer(Modifier.height(10.dp))
                    PlaylogField(
                        value = password,
                        onValueChange = { password = it },
                        label = "Password",
                        placeholder = "Your password",
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                        ),
                    )
                    if (mode == AuthMode.Create) {
                        Spacer(Modifier.height(10.dp))
                        PlaylogField(
                            value = displayName,
                            onValueChange = { displayName = it },
                            label = "Display name",
                            placeholder = "How your profile is shown",
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    GlossyPillButton(
                        text = when {
                            account.busy -> "Please wait…"
                            mode == AuthMode.SignIn -> "Sign in"
                            else -> "Create account"
                        },
                        onClick = {
                            if (mode == AuthMode.SignIn) {
                                vm.signIn(email, password)
                            } else {
                                vm.signUp(email, password, displayName)
                            }
                        },
                        enabled = !account.busy && email.isNotBlank() && password.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (mode == AuthMode.Create) {
                        Spacer(Modifier.height(9.dp))
                        Text(
                            text = "New accounts may need to confirm their email before signing in.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Sage,
                        )
                    }
                }

                if (account.busy) {
                    Spacer(Modifier.height(8.dp))
                    BrandLoader()
                }

                if (account.error != null) {
                    Spacer(Modifier.height(14.dp))
                    StatusNote(text = account.error.orEmpty(), color = Danger)
                } else if (account.message != null) {
                    Spacer(Modifier.height(14.dp))
                    StatusNote(text = account.message.orEmpty(), color = Accent)
                }
            }

            Spacer(Modifier.height(26.dp))
            Kicker("What syncs", color = Teal)
            Spacer(Modifier.height(9.dp))
            LedgerPanel(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 17.dp, vertical = 15.dp),
            ) {
                SyncRow("Library & statuses", "Played, playing, watchlist and dropped")
                SyncRow("Diary & reviews", "Your latest log for each game")
                SyncRow("Game details", "Platform, hours, completion and tags")
                SyncRow("Lists", "Names, descriptions and games")
                SyncRow("Favourites", "Up to four favourite games")
                SyncRow("Profile", "Display name and photo")
            }
            Spacer(Modifier.height(18.dp))
            Text(
                text = "Local-first by default. If you never sign in, everything stays on this device. " +
                    "Signing in merges this device with your web account.",
                style = MaterialTheme.typography.bodySmall,
                color = Sage.copy(alpha = 0.85f),
            )
            Spacer(Modifier.height(36.dp))
        }
    }
}

@Composable
private fun SignedInPanel(
    displayName: String,
    avatarUrl: String?,
    email: String,
    busy: Boolean,
    lastSync: Long?,
    message: String?,
    error: String?,
    onEditProfile: () -> Unit,
    onSync: () -> Unit,
    onSignOut: () -> Unit,
) {
    LedgerPanel(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProfileAvatar(
                name = displayName.ifBlank { email.substringBefore('@').ifBlank { "Playlog" } },
                avatarUrl = avatarUrl,
                size = 52.dp,
            )
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = displayName.ifBlank { email.ifBlank { "Signed in" } },
                    style = MaterialTheme.typography.titleMedium,
                    color = MintWhite,
                    maxLines = 1,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = email.ifBlank { "Signed in" },
                    style = MaterialTheme.typography.bodySmall,
                    color = Sage,
                    maxLines = 1,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = when {
                        busy -> "Syncing now…"
                        lastSync != null -> "Last sync ${lastSync.syncLabel()}"
                        else -> "Ready to sync"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (busy) Accent else Sage,
                )
            }
            Box(
                Modifier
                    .size(8.dp)
                    .background(if (error == null) Accent else Danger, CircleShape),
            )
        }
        Spacer(Modifier.height(16.dp))
        GhostPill(
            text = "Edit profile",
            onClick = onEditProfile,
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        GlossyPillButton(
            text = if (busy) "Syncing…" else "Sync now",
            onClick = onSync,
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onSignOut, enabled = !busy) {
            Text("Sign out", color = Danger)
        }
        if (error != null) {
            Spacer(Modifier.height(6.dp))
            StatusNote(text = error, color = Danger)
        } else if (message != null) {
            Spacer(Modifier.height(6.dp))
            StatusNote(text = message, color = Accent)
        }
    }
}

@Composable
private fun SyncRow(label: String, detail: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            Modifier
                .padding(top = 5.dp)
                .size(5.dp)
                .background(Accent, CircleShape),
        )
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = MintWhite,
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = Sage,
            )
        }
    }
    Spacer(Modifier.height(3.dp))
}

@Composable
private fun StatusNote(text: String, color: androidx.compose.ui.graphics.Color) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.07f))
            .border(1.dp, color.copy(alpha = 0.22f), RoundedCornerShape(12.dp))
            .padding(horizontal = 13.dp, vertical = 10.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = color,
        )
    }
}

private fun Long.syncLabel(): String =
    Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale.getDefault()))
