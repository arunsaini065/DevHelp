package com.dev.help.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface NavRoute : NavKey {
    @Serializable
    data object Dashboard : NavRoute

    @Serializable
    data object WirelessDebugging : NavRoute

    @Serializable
    data object WirelessQrScanner : NavRoute

    @Serializable
    data object DeveloperOptions : NavRoute

    @Serializable
    data object UsbDebugging : NavRoute

    @Serializable
    data object WifiSettings : NavRoute

    @Serializable
    data object AppInfo : NavRoute

    @Serializable
    data object ManageApps : NavRoute

    @Serializable
    data object SpecialAccess : NavRoute

    @Serializable
    data object AccessibilitySettings : NavRoute
}
