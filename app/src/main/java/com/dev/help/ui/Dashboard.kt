package com.dev.help.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Accessibility
import androidx.compose.material.icons.rounded.AppSettingsAlt
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onItemClick: (NavRoute) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DevQuick Access", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        val items = listOf(
            DashboardItem(
                title = "Wireless Debugging",
                icon = Icons.Rounded.Wifi,
                key = NavRoute.WirelessDebugging,
                description = "Enable ADB over WiFi"
            ),
            DashboardItem(
                title = "Developer Options",
                icon = Icons.Rounded.Settings,
                key = NavRoute.DeveloperOptions,
                description = "Open system developer settings"
            ),
            DashboardItem(
                title = "USB Debugging",
                icon = Icons.Rounded.Usb,
                key = NavRoute.UsbDebugging,
                description = "Open USB debugging toggle"
            ),
            DashboardItem(
                title = "Wi-Fi Settings",
                icon = Icons.Rounded.Wifi,
                key = NavRoute.WifiSettings,
                description = "Open device network settings"
            ),
            DashboardItem(
                title = "This App Info",
                icon = Icons.Rounded.AppSettingsAlt,
                key = NavRoute.AppInfo,
                description = "Open permissions, storage, and app details"
            ),
            DashboardItem(
                title = "Manage Apps",
                icon = Icons.Rounded.Apps,
                key = NavRoute.ManageApps,
                description = "Open installed apps list"
            ),
            DashboardItem(
                title = "Special Access",
                icon = Icons.Rounded.Security,
                key = NavRoute.SpecialAccess,
                description = "Manage special app permissions"
            ),
            DashboardItem(
                title = "Accessibility Settings",
                icon = Icons.Rounded.Accessibility,
                key = NavRoute.AccessibilitySettings,
                description = "Enable or configure accessibility services"
            )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Shortcuts",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            items(items) { item ->
                ShortcutCard(
                    item = item,
                    onClick = { onItemClick(item.key) }
                )
            }
        }
    }
}

data class DashboardItem(
    val title: String,
    val icon: ImageVector,
    val key: NavRoute,
    val description: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortcutCard(
    item: DashboardItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
