package com.matth.scaleconnect

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.matth.scaleconnect.ble.ConnectionState
import com.matth.scaleconnect.data.ThemeMode
import com.matth.scaleconnect.service.ScaleConnectionService
import com.matth.scaleconnect.ui.components.BottomNavBar
import com.matth.scaleconnect.ui.components.NavDestination
import com.matth.scaleconnect.ui.dashboard.DashboardScreen
import com.matth.scaleconnect.ui.history.HistoryScreen
import com.matth.scaleconnect.ui.profile.ProfileScreen
import com.matth.scaleconnect.ui.theme.ScaleConnectTheme

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: ScaleViewModel

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) {
            viewModel.startScan()
            if (viewModel.backgroundEnabled.value) startBackgroundService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            viewModel = viewModel()
            val backgroundEnabled by viewModel.backgroundEnabled.collectAsState()

            LaunchedEffect(backgroundEnabled) {
                if (backgroundEnabled) {
                    if (hasRequiredPermissions()) {
                        startBackgroundService()
                    } else {
                        requestPermissionsAndScan()
                    }
                } else {
                    stopService(Intent(this@MainActivity, ScaleConnectionService::class.java))
                }
            }

            val themeMode by viewModel.themeMode.collectAsState()
            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            val view = LocalView.current
            SideEffect {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }

            ScaleConnectTheme(darkTheme = darkTheme) {
                ScaleConnectApp(viewModel = viewModel, onRequestScan = { requestPermissionsAndScan() })
            }
        }
    }

    private fun requestPermissionsAndScan() {
        permissionLauncher.launch(requiredPermissions())
    }

    private fun startBackgroundService() {
        ContextCompat.startForegroundService(this, Intent(this, ScaleConnectionService::class.java))
    }

    private fun requiredPermissions(): Array<String> = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.BLUETOOTH_SCAN)
            add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    private fun hasRequiredPermissions(): Boolean = requiredPermissions().all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }
}

@Composable
fun ScaleConnectApp(viewModel: ScaleViewModel, onRequestScan: () -> Unit) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            BottomNavBar(
                current = NavDestination.entries.find { it.route == currentRoute } ?: NavDestination.Dashboard,
                onSelect = { dest ->
                    navController.navigate(dest.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        },
    ) { padding ->
        AppNavHost(navController, viewModel, onRequestScan, Modifier.padding(padding))
    }
}

@Composable
private fun AppNavHost(
    navController: NavHostController,
    viewModel: ScaleViewModel,
    onRequestScan: () -> Unit,
    modifier: Modifier,
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val deviceName by viewModel.deviceName.collectAsState()
    val reading by viewModel.reading.collectAsState()
    val activeSlot by viewModel.activeSlot.collectAsState()
    val profileSlots by viewModel.profileSlots.collectAsState()
    val history by viewModel.history.collectAsState()
    val backgroundEnabled by viewModel.backgroundEnabled.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val weightUnit by viewModel.weightUnit.collectAsState()
    val heightUnit by viewModel.heightUnit.collectAsState()

    NavHost(
        navController = navController,
        startDestination = NavDestination.Dashboard.route,
        modifier = modifier,
    ) {
        composable(NavDestination.Dashboard.route) {
            DashboardScreen(
                connectionState = connectionState,
                deviceName = deviceName,
                reading = reading,
                activeSlot = activeSlot,
                profileSlots = profileSlots,
                history = history,
                weightUnit = weightUnit,
                onConnectClick = onRequestScan,
                onSelectSlot = viewModel::selectSlot,
            )
        }
        composable(NavDestination.History.route) {
            HistoryScreen(history = history, weightUnit = weightUnit)
        }
        composable(NavDestination.Profile.route) {
            ProfileScreen(
                slots = profileSlots,
                activeSlot = activeSlot,
                connectionState = connectionState,
                deviceName = deviceName,
                backgroundEnabled = backgroundEnabled,
                themeMode = themeMode,
                weightUnit = weightUnit,
                heightUnit = heightUnit,
                onSelectSlot = viewModel::selectSlot,
                onAddProfile = viewModel::addProfile,
                onRemoveProfile = viewModel::removeProfile,
                onUpdateProfile = viewModel::updateProfile,
                onManage = {
                    if (connectionState == ConnectionState.DISCONNECTED) onRequestScan() else viewModel.syncActiveProfile()
                },
                onSetBackgroundEnabled = viewModel::setBackgroundEnabled,
                onSetThemeMode = viewModel::setThemeMode,
                onSetWeightUnit = viewModel::setWeightUnit,
                onSetHeightUnit = viewModel::setHeightUnit,
            )
        }
    }
}
