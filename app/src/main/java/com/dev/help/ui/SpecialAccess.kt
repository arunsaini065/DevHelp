package com.dev.help.ui

import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dev.help.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpecialAccessScreen(
    onItemClick: (String) -> Unit,
    onPinClick: (ShortcutItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        ShortcutItem(
            id = "overlay",
            title = "Display Over Apps",
            icon = Icons.Rounded.Layers,
            action = Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            description = "Draw on top of other apps",
            iconRes = R.drawable.ic_security_shortcut
        ),
        ShortcutItem(
            id = "usage_access",
            title = "Usage Access",
            icon = Icons.Rounded.Insights,
            action = Settings.ACTION_USAGE_ACCESS_SETTINGS,
            description = "Track app usage statistics",
            iconRes = R.drawable.ic_security_shortcut
        ),
        ShortcutItem(
            id = "all_files",
            title = "All Files Access",
            icon = Icons.Rounded.FileOpen,
            action = "android.settings.MANAGE_APP_ALL_FILES_ACCESS_PERMISSION",
            description = "Access all files on storage",
            iconRes = R.drawable.ic_security_shortcut
        ),
        ShortcutItem(
            id = "notification_access",
            title = "Notification Access",
            icon = Icons.Rounded.NotificationsActive,
            action = Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS,
            description = "Read and interact with notifications",
            iconRes = R.drawable.ic_security_shortcut
        ),
        ShortcutItem(
            id = "unknown_apps",
            title = "Install Unknown Apps",
            icon = Icons.Rounded.AppRegistration,
            action = Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            description = "Install APKs from this app",
            iconRes = R.drawable.ic_apps_shortcut
        ),
        ShortcutItem(
            id = "battery_optimization",
            title = "Battery Optimization",
            icon = Icons.Rounded.BatterySaver,
            action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS,
            description = "Ignore battery optimizations",
            iconRes = R.drawable.ic_security_shortcut
        ),
        ShortcutItem(
            id = "dnd_access",
            title = "Do Not Disturb Access",
            icon = Icons.Rounded.DoNotDisturbOn,
            action = Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS,
            description = "Manage DND settings",
            iconRes = R.drawable.ic_security_shortcut
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Special App Access") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items) { item ->
                SpecialAccessCard(
                    item = item,
                    onClick = { onItemClick(item.action) },
                    onPinClick = { onPinClick(item) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpecialAccessCard(
    item: ShortcutItem,
    onClick: () -> Unit,
    onPinClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(item.icon, contentDescription = null, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(item.description, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onPinClick) {
                Icon(Icons.Rounded.PushPin, contentDescription = "Pin to Home")
            }
        }
    }
}

data class ShortcutItem(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val action: String,
    val description: String,
    val iconRes: Int? = null
)
