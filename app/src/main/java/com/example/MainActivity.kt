package com.example

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.ui.navigation.AppNavGraph
import com.example.ui.navigation.Screen
import com.example.ui.theme.QuickCallTheme
import com.example.viewmodel.CallerViewModel
import com.example.viewmodel.ScannerViewModel
import com.example.viewmodel.SettingsViewModel
import com.example.viewmodel.UpdateViewModel

class MainActivity : ComponentActivity() {

    private val callerViewModel: CallerViewModel by viewModels()
    private val scannerViewModel: ScannerViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val updateViewModel: UpdateViewModel by viewModels()

    private var activeNavController: NavHostController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Configure 120Hz high refresh rate display mode if supported by hardware
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val currentDisplay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    display
                } else {
                    @Suppress("DEPRECATION")
                    windowManager.defaultDisplay
                }
                val highRefreshMode = currentDisplay?.supportedModes
                    ?.filter { it.refreshRate >= 119f }
                    ?.maxByOrNull { it.refreshRate }

                if (highRefreshMode != null) {
                    val lp = window.attributes
                    lp.preferredDisplayModeId = highRefreshMode.modeId
                    window.attributes = lp
                }
            } catch (_: Exception) {
                // Default display mode fallback
            }
        }

        val action = intent?.action
        val navTarget = intent?.getStringExtra("EXTRA_NAV_TARGET")
        val pendingNumber = intent?.getStringExtra("EXTRA_PHONE_NUMBER")
        val pendingName = intent?.getStringExtra("EXTRA_CALLER_NAME")

        // Outside shortcut / widget direct launch support
        val startDestination = when {
            action == "com.example.ACTION_QUICK_SCAN" || navTarget == "quick_scan" -> Screen.Scanner.route
            action == "com.example.ACTION_T9_DIALER" || navTarget == "search_t9" -> Screen.Search.route
            navTarget == "caller_popup" -> "caller_popup?number=${pendingNumber ?: ""}&name=${pendingName ?: ""}"
            else -> Screen.Splash.route
        }

        setContent {
            QuickCallTheme {
                val navController = rememberNavController()
                activeNavController = navController

                AppNavGraph(
                    navController = navController,
                    callerViewModel = callerViewModel,
                    scannerViewModel = scannerViewModel,
                    settingsViewModel = settingsViewModel,
                    updateViewModel = updateViewModel,
                    startDestination = startDestination,
                    pendingNumber = pendingNumber,
                    pendingName = pendingName
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val action = intent?.action
        val navTarget = intent?.getStringExtra("EXTRA_NAV_TARGET")
        val pendingNumber = intent?.getStringExtra("EXTRA_PHONE_NUMBER")
        val pendingName = intent?.getStringExtra("EXTRA_CALLER_NAME")

        when {
            action == "com.example.ACTION_QUICK_SCAN" || navTarget == "quick_scan" -> {
                activeNavController?.navigate(Screen.Scanner.route) {
                    launchSingleTop = true
                }
            }
            action == "com.example.ACTION_T9_DIALER" || navTarget == "search_t9" -> {
                activeNavController?.navigate(Screen.Search.route) {
                    launchSingleTop = true
                }
            }
            navTarget == "caller_popup" -> {
                activeNavController?.navigate("caller_popup?number=${pendingNumber ?: ""}&name=${pendingName ?: ""}") {
                    launchSingleTop = true
                }
            }
        }
    }
}
