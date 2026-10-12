package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    data object Splash : Screen("splash", "Splash")
    data object Home : Screen("home", "Home", Icons.Default.Home)
    data object Scanner : Screen("scanner", "Live Scan", Icons.Default.CameraAlt)
    data object RecentCalls : Screen("recent_calls", "Recents", Icons.Default.History)
    data object Search : Screen("search", "Lookup", Icons.Default.Search)
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    data object CallerPopup : Screen("caller_popup", "Caller ID", Icons.Default.Call)

    companion object {
        val bottomNavItems: List<Screen>
            get() = listOf(
                Home,
                Scanner,
                RecentCalls,
                Search,
                Settings
            )
    }
}
