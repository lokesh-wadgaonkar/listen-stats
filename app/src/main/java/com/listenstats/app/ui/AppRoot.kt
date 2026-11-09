package com.listenstats.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

private enum class Dest(val route: String, val label: String, val icon: ImageVector) {
    OVERVIEW("overview", "Overview", Icons.Filled.Home),
    TOP("top", "Top", Icons.Filled.Star),
    HISTORY("history", "History", Icons.Filled.DateRange),
    REWIND("rewind", "Rewind", Icons.Filled.Favorite),
    SETTINGS("settings", "Settings", Icons.Filled.Settings)
}

@Composable
fun AppRoot(vm: MainViewModel) {
    val navController = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val message = vm.message
    val granted = vm.accessGranted

    LaunchedEffect(message) {
        if (message != null) {
            snackbar.showSnackbar(message)
            vm.consumeMessage()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (granted) {
                BottomBar(navController)
            }
        }
    ) { inner ->
        if (!granted) {
            SetupScreen(modifier = Modifier.padding(inner))
        } else {
            NavHost(
                navController = navController,
                startDestination = Dest.OVERVIEW.route,
                modifier = Modifier.padding(inner)
            ) {
                composable(Dest.OVERVIEW.route) { OverviewScreen(vm) }
                composable(Dest.TOP.route) { TopScreen(vm) }
                composable(Dest.HISTORY.route) { HistoryScreen(vm) }
                composable(Dest.REWIND.route) { RewindScreen(vm) }
                composable(Dest.SETTINGS.route) { SettingsScreen(vm) }
            }
        }
    }
}

@Composable
private fun BottomBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val current = backStackEntry?.destination?.route

    NavigationBar {
        for (d in Dest.values()) {
            NavigationBarItem(
                selected = current == d.route,
                onClick = {
                    navController.navigate(d.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(imageVector = d.icon, contentDescription = d.label) },
                label = { Text(d.label) }
            )
        }
    }
}
