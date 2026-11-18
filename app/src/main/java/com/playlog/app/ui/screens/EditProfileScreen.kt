package com.playlog.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.playlog.app.PlaylogViewModel
import com.playlog.app.ui.components.GhostPill
import com.playlog.app.ui.components.GlossyPillButton
import com.playlog.app.ui.components.Kicker
import com.playlog.app.ui.components.LedgerPanel
import com.playlog.app.ui.components.PlaylogField
import com.playlog.app.ui.components.ProfileAvatar
import com.playlog.app.ui.components.ScreenHeader
import com.playlog.app.ui.theme.Accent
import com.playlog.app.ui.theme.Danger
import com.playlog.app.ui.theme.MintWhite
import com.playlog.app.ui.theme.Sage
import com.playlog.app.ui.theme.Teal

@Composable
fun EditProfileScreen(
    vm: PlaylogViewModel,
    onBack: () -> Unit,
    onAccount: () -> Unit,
) {
    val snapshot by vm.state.collectAsStateWithLifecycle()
    val account by vm.account.collectAsStateWithLifecycle()
    val edit by vm.profileEdit.collectAsStateWithLifecycle()
    var name by rememberSaveable { mutableStateOf(snapshot.displayName) }
    var pickedUri by remember { mutableStateOf<Uri?>(null) }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) pickedUri = uri
    }

    LaunchedEffect(Unit) { vm.resetProfileEditState() }

    val previewUrl = pickedUri?.toString() ?: snapshot.avatarUrl
    val hasSavedPhoto = !snapshot.avatarUrl.isNullOrBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        ScreenHeader(
            title = "Edit profile",
            kicker = "Your identity",
            onBack = onBack,
        )

        Column(Modifier.padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(14.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ProfileAvatar(
                    name = name.ifBlank { snapshot.displayName },
                    avatarUrl = previewUrl,
                    size = 112.dp,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Profile photo",
                    style = MaterialTheme.typography.titleMedium,
                    color = MintWhite,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Square crop · 512 × 512 WebP",
                    style = MaterialTheme.typography.bodySmall,
                    color = Sage,
                )
            }

            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                GhostPill(
                    text = if (pickedUri != null) "Choose another" else "Choose photo",
                    onClick = {
                        pickImage.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                    enabled = account.signedIn && !edit.busy,
                    modifier = Modifier.weight(1f),
                )
                if (pickedUri != null || hasSavedPhoto) {
                    GhostPill(
                        text = if (pickedUri != null) "Clear" else "Remove photo",
                        onClick = {
                            if (pickedUri != null) {
                                pickedUri = null
                            } else {
                                vm.removeProfilePhoto()
                            }
                        },
                        enabled = !edit.busy,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            if (!account.signedIn) {
                LedgerPanel(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(15.dp),
                ) {
                    Text(
                        text = "Sign in to upload a photo",
                        style = MaterialTheme.typography.titleSmall,
                        color = MintWhite,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Your display name is saved on this device. Sign in to sync your name " +
                            "and photo with the Playlog website.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Sage,
                    )
                    Spacer(Modifier.height(11.dp))
                    GhostPill(text = "Account & sync", onClick = onAccount)
                }
            } else {
                Text(
                    text = "Your photo and display name sync with the Playlog website.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Sage,
                )
            }

            Spacer(Modifier.height(26.dp))
            Kicker("Display name", color = Teal)
            Spacer(Modifier.height(9.dp))
            PlaylogField(
                value = name,
                onValueChange = { if (it.length <= 80) name = it },
                label = "Name",
                placeholder = "How your profile is shown",
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "${name.length}/80",
                style = MaterialTheme.typography.labelSmall,
                color = Sage,
            )

            Spacer(Modifier.height(24.dp))
            GlossyPillButton(
                text = if (edit.busy) "Saving…" else "Save profile",
                onClick = { vm.saveProfile(name, pickedUri) },
                enabled = !edit.busy && name.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            )

            if (edit.error != null) {
                Spacer(Modifier.height(14.dp))
                EditStatusNote(text = edit.error.orEmpty(), color = Danger)
            } else if (edit.message != null) {
                Spacer(Modifier.height(14.dp))
                EditStatusNote(text = edit.message.orEmpty(), color = Accent)
            }

            Spacer(Modifier.height(18.dp))
            Text(
                text = "Your photo is stored in the same Playlog profile images bucket as the web app. " +
                    "The app only uses your own account folder.",
                style = MaterialTheme.typography.bodySmall,
                color = Sage.copy(alpha = 0.8f),
            )
            Spacer(Modifier.height(36.dp))
        }
    }
}

@Composable
private fun EditStatusNote(text: String, color: androidx.compose.ui.graphics.Color) {
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
