package com.listenstats.app.ui

import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.listenstats.app.data.AppEntry
import com.listenstats.app.data.Dates

@Composable
fun SettingsScreen(vm: MainViewModel) {
    var confirmClear by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            vm.exportTo(uri)
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            vm.importFrom(uri)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Dim.screenPadding),
        verticalArrangement = Arrangement.spacedBy(Dim.gap)
    ) {
        ScreenTitle("Settings")

        SectionCard(title = "Apps") {
            if (vm.apps.isEmpty()) {
                Text(
                    text = "Apps appear here after they play something while Listen Stats is " +
                        "running. Poweramp is on by default.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            } else {
                for (a in vm.apps) {
                    AppRow(entry = a, onToggle = { vm.setAppEnabled(a.packageName, it) })
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Sessions without an artist are always ignored.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        SectionCard(title = "Recording") {
            Text(
                text = "Minimum listening time for a play",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = { vm.setMinSeconds(vm.minSeconds - 5) },
                    shape = RoundedCornerShape(50)
                ) {
                    Text("−5 s")
                }
                Spacer(Modifier.width(16.dp))
                Text(
                    text = vm.minSeconds.toString() + " s",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(16.dp))
                OutlinedButton(
                    onClick = { vm.setMinSeconds(vm.minSeconds + 5) },
                    shape = RoundedCornerShape(50)
                ) {
                    Text("+5 s")
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Only time spent actually playing counts. Pauses, buffering and seeking add nothing.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        SectionCard(title = "Appearance") {
            SwitchRow(
                title = "AMOLED black",
                subtitle = "Pure black background. Always uses the dark theme.",
                checked = vm.amoled
            ) { vm.setAmoled(it) }
            SwitchRow(
                title = "Dynamic color",
                subtitle = "Use your wallpaper colors (Android 12 and newer).",
                checked = vm.dynamicColor,
                enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            ) { vm.setDynamicColor(it) }
        }

        SectionCard(title = "Backup") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        exportLauncher.launch("listenstats-backup-" + Dates.todayKey() + ".json")
                    },
                    shape = RoundedCornerShape(50)
                ) {
                    Text("Export")
                }
                FilledTonalButton(
                    onClick = { importLauncher.launch(arrayOf("*/*")) },
                    shape = RoundedCornerShape(50)
                ) {
                    Text("Import")
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Backups are JSON files you choose the location of. Import skips plays " +
                    "that are already saved.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        BackgroundHelpCard()

        SectionCard(
            title = "Data",
            container = MaterialTheme.colorScheme.errorContainer
        ) {
            Button(
                onClick = { confirmClear = true },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text("Delete all listening data")
            }
        }
        Spacer(Modifier.height(Dim.gap))
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Delete all data?") },
            text = {
                Text(
                    "This permanently deletes every saved play on this phone. " +
                        "Export a backup first if you want to keep it."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmClear = false
                        vm.clearAll()
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun AppRow(entry: AppEntry, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = entry.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = entry.enabled, onCheckedChange = onToggle)
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}
