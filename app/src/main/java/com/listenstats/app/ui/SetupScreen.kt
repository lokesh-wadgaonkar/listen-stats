package com.listenstats.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/** Shown until notification access is granted. */
@Composable
fun SetupScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Dim.screenPadding),
        verticalArrangement = Arrangement.spacedBy(Dim.gap)
    ) {
        Text(
            text = "Listen Stats",
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Your personal listening stats, kept entirely on this phone. " +
                "No accounts, no internet, no ads.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))

        InfoCard(
            title = "1. Allow notification access",
            body = "Android only lets an app see what your music player is playing if it has " +
                "notification access. Listen Stats uses it to read the track title, artist and " +
                "play/pause state of your media sessions (for example Poweramp). Notification " +
                "text itself is never read or stored.",
            container = MaterialTheme.colorScheme.primaryContainer,
            onContainer = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Button(
                onClick = { Intents.openNotificationAccess(context) },
                shape = RoundedCornerShape(50)
            ) {
                Text("Open notification access")
            }
        }

        InfoCard(
            title = "Switch greyed out or says \"Restricted setting\"?",
            body = "Apps installed from an APK file (not the Play Store) are blocked from " +
                "notification access on Android 13 and newer until you allow it:\n" +
                "Open App info, tap the three-dot menu at the top right, choose " +
                "\"Allow restricted settings\", then come back and open notification access again.",
            container = MaterialTheme.colorScheme.tertiaryContainer,
            onContainer = MaterialTheme.colorScheme.onTertiaryContainer
        ) {
            FilledTonalButton(
                onClick = { Intents.openAppInfo(context) },
                shape = RoundedCornerShape(50)
            ) {
                Text("Open app info")
            }
        }

        BackgroundHelpCard()
    }
}

/** Explains how to stop Realme / ColorOS from killing the recorder. Also used in Settings. */
@Composable
fun BackgroundHelpCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    InfoCard(
        title = "Keep it running in the background",
        body = "Realme / ColorOS closes background apps aggressively. If it kills Listen Stats, " +
            "plays are not recorded. Do all three:\n\n" +
            "1. Battery: open the app's battery settings and choose \"Allow background activity\" " +
            "or \"Don't optimize\".\n" +
            "2. Auto-launch: in Settings, Apps, Auto launch (or Startup manager), switch " +
            "Listen Stats on.\n" +
            "3. Recent apps: open the recent-apps screen and lock the Listen Stats card " +
            "(pull it down or tap the lock icon).\n\n" +
            "Menu names differ slightly between ColorOS versions.",
        modifier = modifier,
        container = MaterialTheme.colorScheme.secondaryContainer,
        onContainer = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Button(
            onClick = { Intents.openBatterySettings(context) },
            shape = RoundedCornerShape(50)
        ) {
            Text("Open app battery settings")
        }
        FilledTonalButton(
            onClick = { Intents.openBatteryOptimizationList(context) },
            shape = RoundedCornerShape(50)
        ) {
            Text("Battery optimization list")
        }
    }
}

@Composable
private fun InfoCard(
    title: String,
    body: String,
    container: Color,
    onContainer: Color,
    modifier: Modifier = Modifier,
    actions: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = container, contentColor = onContainer)
    ) {
        Column(
            modifier = Modifier.padding(Dim.cardPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = onContainer
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = onContainer
            )
            actions()
        }
    }
}
