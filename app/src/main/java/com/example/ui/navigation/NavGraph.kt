package com.example.ui.navigation

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.example.data.model.ScanSource
import com.example.ui.screens.CallerPopupScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.RecentCallsScreen
import com.example.ui.screens.ScannerScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkGlassCard
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextOnNeon
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.CallerViewModel
import com.example.viewmodel.ScannerViewModel
import com.example.viewmodel.SettingsViewModel

@Composable
fun AppNavGraph(
    navController: NavHostController,
    callerViewModel: CallerViewModel,
    scannerViewModel: ScannerViewModel,
    settingsViewModel: SettingsViewModel,
    startDestination: String = Screen.Splash.route,
    pendingNumber: String? = null,
    pendingName: String? = null
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val shouldShowBottomBar = currentRoute in Screen.bottomNavItems.map { it.route }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (shouldShowBottomBar) {
                QuickCallBottomNavigation(
                    currentRoute = currentRoute,
                    onItemClick = { screen ->
                        if (currentRoute != screen.route) {
                            navController.navigate(screen.route) {
                                popUpTo(Screen.Home.route) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        @OptIn(ExperimentalSharedTransitionApi::class)
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.Splash.route) {
                SplashScreen(
                    onSplashFinished = {
                        if (!pendingNumber.isNullOrBlank()) {
                            navController.navigate("caller_popup?number=$pendingNumber&name=${pendingName ?: ""}") {
                                popUpTo(Screen.Splash.route) { inclusive = true }
                            }
                        } else {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Splash.route) { inclusive = true }
                            }
                        }
                    }
                )
            }

            composable(
                route = Screen.Home.route,
                enterTransition = {
                    fadeIn(animationSpec = tween(250, easing = FastOutSlowInEasing))
                },
                exitTransition = {
                    fadeOut(animationSpec = tween(250, easing = FastOutSlowInEasing))
                }
            ) {
                HomeScreen(
                    callerViewModel = callerViewModel,
                    scannerViewModel = scannerViewModel,
                    onNavigateToScanner = { source ->
                        scannerViewModel.setScanSource(source)
                        navController.navigate(Screen.Scanner.route) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToCallerPopup = { number, name ->
                        navController.navigate("caller_popup?number=$number&name=$name")
                    },
                    onNavigateToRecents = {
                        navController.navigate(Screen.RecentCalls.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(
                route = Screen.Scanner.route,
                enterTransition = {
                    fadeIn(animationSpec = tween(250, easing = FastOutSlowInEasing))
                },
                exitTransition = {
                    fadeOut(animationSpec = tween(250, easing = FastOutSlowInEasing))
                }
            ) {
                ScannerScreen(
                    scannerViewModel = scannerViewModel,
                    onNavigateToCallerPopup = { number, name ->
                        navController.navigate("caller_popup?number=$number&name=$name")
                    }
                )
            }

            composable(Screen.RecentCalls.route) {
                RecentCallsScreen(
                    callerViewModel = callerViewModel,
                    onNavigateToCallerPopup = { number, name ->
                        navController.navigate("caller_popup?number=$number&name=$name")
                    }
                )
            }

            composable(Screen.Search.route) {
                SearchScreen(
                    callerViewModel = callerViewModel,
                    onNavigateToCallerPopup = { number, name ->
                        navController.navigate("caller_popup?number=$number&name=$name")
                    }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    settingsViewModel = settingsViewModel
                )
            }

            composable(
                route = "caller_popup?number={number}&name={name}",
                arguments = listOf(
                    navArgument("number") {
                        type = NavType.StringType
                        defaultValue = "+1 (888) 998-1234"
                    },
                    navArgument("name") {
                        type = NavType.StringType
                        defaultValue = "Unknown Caller"
                    }
                )
            ) { backStackEntry ->
                val number = backStackEntry.arguments?.getString("number") ?: "+1 (888) 998-1234"
                val name = backStackEntry.arguments?.getString("name") ?: "Unknown Caller"

                CallerPopupScreen(
                    callerViewModel = callerViewModel,
                    phoneNumber = number,
                    callerName = name,
                    onDismiss = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

@Composable
private fun QuickCallBottomNavigation(
    currentRoute: String?,
    onItemClick: (Screen) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            color = Color(0xF50A111F),
            tonalElevation = 14.dp,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                Brush.horizontalGradient(
                    listOf(
                        Color(0x3300FF66),
                        Color(0x4000F0FF),
                        Color(0x3300FF66)
                    )
                )
            )
        ) {
            NavigationBar(
                containerColor = Color.Transparent,
                tonalElevation = 0.dp,
                modifier = Modifier
                    .height(66.dp)
                    .testTag("bottom_nav_bar")
            ) {
                Screen.bottomNavItems.forEach { screen ->
                    val isSelected = currentRoute == screen.route
                    val icon = screen.icon ?: return@forEach

                    val iconScale by androidx.compose.animation.core.animateFloatAsState(
                        targetValue = if (isSelected) 1.15f else 1.0f,
                        animationSpec = androidx.compose.animation.core.spring(
                            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                            stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                        ),
                        label = "nav_icon_scale"
                    )

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { onItemClick(screen) },
                        icon = {
                            if (screen == Screen.Scanner) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(42.dp)
                                        .graphicsLayer {
                                            scaleX = iconScale
                                            scaleY = iconScale
                                        }
                                        .background(if (isSelected) NeonGreen else Color(0xFF132238), CircleShape)
                                        .border(1.5.dp, NeonGreen, CircleShape)
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = screen.title,
                                        tint = if (isSelected) TextOnNeon else NeonGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = screen.title,
                                    modifier = Modifier
                                        .size(22.dp)
                                        .graphicsLayer {
                                            scaleX = iconScale
                                            scaleY = iconScale
                                        }
                                )
                            }
                        },
                        label = {
                            Text(
                                text = screen.title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonGreen,
                            selectedTextColor = NeonGreen,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextMuted,
                            indicatorColor = Color(0x2600FF66)
                        )
                    )
                }
            }
        }
    }
}
