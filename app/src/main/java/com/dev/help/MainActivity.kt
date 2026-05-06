package com.dev.help

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
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
            is NavRoute.ManageApps -> NavEntry(
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
        NavRoute.WirelessQrScanner -> {
            openWirelessQrScanner(context)
            null
        }
        NavRoute.WirelessDebugging -> wirelessDebuggingIntent()
        NavRoute.DeveloperOptions -> Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
        NavRoute.UsbDebugging -> Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
        NavRoute.WifiSettings -> Intent(Settings.ACTION_WIFI_SETTINGS)
        NavRoute.AppInfo -> Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        )
        NavRoute.ManageApps -> Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)
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

private fun openWirelessQrScanner(context: Context) {
    context.forceEnableScannerHelper()

    if (!context.isScannerHelperEnabled()) {
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

    DevHelpAccessibilityService.requestWirelessQrScan(context)
    runCatching {
        context.startActivity(wirelessDebuggingIntent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.onFailure {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

private fun Context.forceEnableScannerHelper() {
    val serviceName = scannerHelperServiceName()
    val enabledServices = Settings.Secure.getString(
        contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ).orEmpty()

    val services = enabledServices
        .split(':')
        .filter { it.isNotBlank() }
        .toMutableList()

    if (services.none { it.equals(serviceName, ignoreCase = true) }) {
        services.add(serviceName)
    }

    runCatching {
        Settings.Secure.putString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            services.joinToString(":")
        )
        Settings.Secure.putInt(contentResolver, ACCESSIBILITY_ENABLED, 1)
    }
}

private fun Context.isScannerHelperEnabled(): Boolean {
    val enabledServices = Settings.Secure.getString(
        contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ).orEmpty()
    val serviceName = scannerHelperServiceName()
    return enabledServices.split(':').any { it.equals(serviceName, ignoreCase = true) }
}

private fun Context.scannerHelperServiceName(): String =
    "$packageName/${DevHelpAccessibilityService::class.java.name}"

private fun wirelessDebuggingIntent(): Intent =
    Intent(TileService.ACTION_QS_TILE_PREFERENCES)
        .setPackage("com.android.settings")
        .putExtra(
            Intent.EXTRA_COMPONENT_NAME,
            ComponentName(
                "com.android.settings",
                "com.android.settings.development.qstile.DevelopmentTiles\$WirelessDebugging"
            )
        )

private const val ACCESSIBILITY_ENABLED = "accessibility_enabled"
