package com.dev.help

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.service.quicksettings.TileService
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.dev.help.ui.DashboardScreen
import com.dev.help.ui.NavRoute
import com.dev.help.ui.ShortcutItem
import com.dev.help.ui.SpecialAccessScreen
import com.dev.help.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val shortcutAction = intent?.getStringExtra("shortcut_action")
        if (shortcutAction != null) {
            launchDirectly(shortcutAction)
            return
        }

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                DevQuickApp(onPinShortcut = { item -> pinShortcut(item) })
            }
        }
    }

    private fun launchDirectly(action: String) {
        val intent = if (action.startsWith("pkg:")) {
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + action.substring(4))
            )
        } else if (action == "wireless_debugging") {
            wirelessDebuggingIntent()
        } else {
            Intent(action)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { startActivity(intent) }
            .onFailure {
                startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        finish()
    }

    private fun pinShortcut(item: ShortcutItem) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val shortcutManager = getSystemService(ShortcutManager::class.java)
        if (shortcutManager != null && shortcutManager.isRequestPinShortcutSupported) {
            val intent = Intent(this, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                putExtra("shortcut_action", item.action)
            }

            val shortcut = ShortcutInfo.Builder(this, item.id)
                .setShortLabel(item.title)
                .setIcon(Icon.createWithResource(this, R.mipmap.ic_launcher))
                .setIntent(intent)
                .build()

            shortcutManager.requestPinShortcut(shortcut, null)
        }
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun DevQuickApp(onPinShortcut: (ShortcutItem) -> Unit) {
    val context = LocalContext.current
    val backStack = rememberNavBackStack(NavRoute.Dashboard)
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>()

    NavDisplay(
        backStack = backStack,
        sceneStrategy = listDetailStrategy,
        onBack = { if (backStack.size > 1) backStack.removeAt(backStack.size - 1) }
    ) { key ->
        when (key) {
            is NavRoute.Dashboard -> NavEntry(
                key = key,
                metadata = ListDetailSceneStrategy.listPane()
            ) {
                DashboardScreen(
                    onItemClick = { route ->
                        if (route is NavRoute.SpecialAccess) {
                            backStack.add(route)
                        } else {
                            launchSettings(context, route)
                        }
                    },
                    onPinClick = { item -> onPinShortcut(item) }
                )
            }

            is NavRoute.SpecialAccess -> NavEntry(
                key = key,
                metadata = ListDetailSceneStrategy.detailPane()
            ) {
                SpecialAccessScreen(
                    onPinClick = { item -> onPinShortcut(item) },
                    onItemClick = { action ->
                        val intent = Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        runCatching { context.startActivity(intent) }
                    }
                )
            }

            is NavRoute.WirelessDebugging,
            is NavRoute.WirelessQrScanner,
            is NavRoute.DeveloperOptions,
            is NavRoute.UsbDebugging,
            is NavRoute.WifiSettings,
            is NavRoute.AppInfo,
            is NavRoute.ManageApps,
            is NavRoute.AccessibilitySettings -> NavEntry(
                key = key,
                metadata = ListDetailSceneStrategy.detailPane()
            ) {
            }
            else -> NavEntry(key) { }
        }
    }
}

private fun launchSettings(context: Context, key: NavRoute) {
    val intent = when (key) {
        NavRoute.WirelessQrScanner -> wirelessDebuggingIntent()
        NavRoute.WirelessDebugging -> wirelessDebuggingIntent()
        NavRoute.DeveloperOptions -> Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
        NavRoute.UsbDebugging -> {
            openUsbDebugging(context)
            null
        }
        NavRoute.WifiSettings -> Intent(Settings.ACTION_WIFI_SETTINGS)
        NavRoute.AppInfo -> Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        )
        NavRoute.ManageApps -> Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)
        NavRoute.SpecialAccess -> {
            openSpecialAccess(context)
            null
        }
        NavRoute.AccessibilitySettings -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        NavRoute.Dashboard -> null
        else -> {
            Intent()
        }
    }

    intent?.let {
        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(it) }
            .onFailure {
                context.startActivity(
                    Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
    }
}

private fun openUsbDebugging(context: Context) {
    context.startFirstAvailable(
        listOf(
            settingsActivity("com.android.settings.Settings\$DevelopmentSettingsDashboardActivity"),
            settingsActivity("com.android.settings.DevelopmentSettings"),
            Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
        )
    )
}

private fun openSpecialAccess(context: Context) {
    if (!context.isAccessibilityHelperEnabled()) {
        Toast.makeText(
            context,
            "Enable Dev Help Scanner Helper, then tap again",
            Toast.LENGTH_LONG
        ).show()
        context.startActivity(
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        return
    }

    DevHelpAccessibilityService.requestSpecialAccess(context)
    context.startFirstAvailable(specialAccessIntents())
}

private fun Context.startFirstAvailable(intents: List<Intent>) {
    for (intent in intents) {
        val opened = runCatching {
            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.isSuccess

        if (opened) return
    }

    startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

private fun settingsActivity(className: String): Intent =
    Intent().setComponent(ComponentName("com.android.settings", className))

private fun specialAccessIntents(): List<Intent> =
    listOf(
        Intent(ACTION_MANAGE_SPECIAL_APP_ACCESSES),
        Intent(ACTION_SETTINGS_MANAGE_SPECIAL_APP_ACCESSES),
        settingsActivity("com.android.settings.Settings\$SpecialAccessSettingsActivity"),
        settingsActivity("com.android.settings.Settings\$ManageApplicationsActivity"),
        Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)
    )

private fun Context.isAccessibilityHelperEnabled(): Boolean {
    val enabledServices = Settings.Secure.getString(
        contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ).orEmpty()
    val serviceName = "$packageName/${DevHelpAccessibilityService::class.java.name}"
    return enabledServices.split(':').any { it.equals(serviceName, ignoreCase = true) }
}

private fun wirelessDebuggingIntent(): Intent =
    Intent(TileService.ACTION_QS_TILE_PREFERENCES)
        .setPackage("com.android.settings")
        .putExtra(
            Intent.EXTRA_COMPONENT_NAME,
            ComponentName(
                "com.android.settings",
                "com.android.settings.development.qstile.DevelopmentTiles${'$'}WirelessDebugging"
            )
        )

private const val ACTION_MANAGE_SPECIAL_APP_ACCESSES =
    "android.intent.action.MANAGE_SPECIAL_APP_ACCESSES"
private const val ACTION_SETTINGS_MANAGE_SPECIAL_APP_ACCESSES =
    "android.settings.MANAGE_SPECIAL_APP_ACCESSES"
