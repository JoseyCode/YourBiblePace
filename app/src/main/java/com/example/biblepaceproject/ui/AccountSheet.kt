package com.example.biblepaceproject.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSheet(account: AccountState, actions: ReaderActions, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Your progress", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            when {
                !account.configured -> Text(
                    "Cloud backup isn't set up in this build. Your progress is saved on this phone only.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                !account.signedIn -> {
                    Text(
                        "Sign in with Google and your progress is kept safe in your account. Reinstall the app, get a new phone, or come back " +
                            "after a long break: it will all be waiting for you.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Button(onClick = actions.onSignIn, enabled = !account.busy, modifier = Modifier.fillMaxWidth()) {
                        Text(if (account.busy) "Signing in…" else "Sign in with Google")
                    }
                }
                else -> {
                    Text("Signed in as ${account.email ?: "your Google account"}", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        when (account.status) {
                            SyncStatus.Syncing -> "Syncing…"
                            SyncStatus.Synced -> "Everything is backed up."
                            SyncStatus.Failed -> "Not backed up yet."
                            SyncStatus.Idle -> "Waiting to sync."
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = actions.onSyncNow, enabled = account.status != SyncStatus.Syncing && !account.busy) { Text("Sync now") }
                        OutlinedButton(onClick = actions.onSignOut, enabled = !account.busy) { Text("Sign out") }
                    }
                    Text(
                        "Signing out removes your progress from this phone. It stays in your account and returns when you sign back in.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            account.message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
        }
    }
}
