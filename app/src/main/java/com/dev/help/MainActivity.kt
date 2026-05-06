package com.dev.help

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.service.quicksettings.TileService
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
import com.dev.help.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                DevQuickApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun DevQuickApp() {
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
                        launchSettings(context, route)

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
            is NavRoute.SpecialAccess,
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
        NavRoute.SpecialAccess -> Intent("android.settings.MANAGE_SPECIAL_APP_ACCESS")
        NavRoute.AccessibilitySettings -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        NavRoute.Dashboard -> null
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
    context.startActivity(
        Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
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
