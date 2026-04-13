package com.karbrusha.bluetoothlowenergy.presentation

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.karbrusha.bluetoothlowenergy.presentation.components.BleDeviceDetailScreen
import com.karbrusha.bluetoothlowenergy.presentation.components.BleLiveDataScreen
import com.karbrusha.bluetoothlowenergy.presentation.components.BluetoothScreen
import com.karbrusha.bluetoothlowenergy.presentation.components.BleScanConnectScreen
import com.karbrusha.bluetoothlowenergy.presentation.components.SavedScreen
import com.karbrusha.bluetoothlowenergy.presentation.navigation.AppRoutes
import com.karbrusha.bluetoothlowenergy.ui.theme.BluetoothLowEnergyTheme
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val bluetoothManager by lazy {
        applicationContext.getSystemService(BluetoothManager::class.java)
    }
    private val bluetoothAdapter by lazy {
        bluetoothManager?.adapter
    }
    private val isBluetoothEnabled: Boolean
        get() = bluetoothAdapter?.isEnabled == true

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val enableBluetoothLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {

            }
        val permissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions())
            { perms ->
                val canEnableBluetooth = perms[Manifest.permission.BLUETOOTH_CONNECT] == true

                if (canEnableBluetooth && !isBluetoothEnabled) {
                    enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                }
            }
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        )
        setContent {
            BluetoothLowEnergyTheme {
                val classicViewModel = hiltViewModel<BluetoothViewmodel>()
                val bleViewModel = hiltViewModel<BleScanConnectViewModel>()

                val classicState by classicViewModel.state.collectAsState()
                val bleState by bleViewModel.uiState.collectAsState()
                val isBluetoothEnabled by classicViewModel.isBluetoothEnabled.collectAsState()

                // Show dialog whenever Bluetooth is turned off
                if (!isBluetoothEnabled) {
                    AlertDialog(
                        onDismissRequest = {},
                        title = { Text("Bluetooth is Off") },
                        text = { Text("Bluetooth has been turned off. Please enable it to continue using this app.") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    enableBluetoothLauncher.launch(
                                        Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                                    )
                                }
                            ) {
                                Text("Turn On")
                            }
                        },
                        dismissButton = null,
                    )
                }

                val navController = rememberNavController()
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = backStackEntry?.destination
                val isBleDetail = currentDestination?.route?.startsWith(AppRoutes.BleDetail) == true

                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing),
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = { Text(text = "DEVICE CONNECT") },
                            navigationIcon = {
                                if (isBleDetail) {
                                    IconButton(onClick = { navController.popBackStack() }) {
                                        Icon(
                                            imageVector = Icons.Default.ChevronLeft,
                                            contentDescription = "Back",
                                        )
                                    }
                                }
                            },
                            actions = {
                                IconButton(onClick = { /* TODO settings */ }) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Settings",
                                    )
                                }
                            },
                        )
                    },
                    bottomBar = {
                        val items = remember {
                            listOf(
                                BottomNavItem(
                                    route = AppRoutes.Classic,
                                    label = "Classic",
                                    icon = Icons.Default.Waves,
                                ),
                                BottomNavItem(
                                    route = AppRoutes.Ble,
                                    label = "BLE",
                                    icon = Icons.Default.Bluetooth,
                                ),
                                BottomNavItem(
                                    route = AppRoutes.Saved,
                                    label = "Saved",
                                    icon = Icons.Default.Bookmark,
                                ),
                            )
                        }

                        NavigationBar(
                            modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                        ) {
                            items.forEach { item ->
                                val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        navController.navigate(item.route) {
                                            popUpTo(navController.graph.startDestinationId) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = item.label,
                                        )
                                    },
                                    label = { Text(text = item.label) },
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = AppRoutes.Classic,
                        modifier = Modifier
                            .padding(innerPadding)
                            .padding(horizontal = 12.dp),
                    ) {
                        composable(AppRoutes.Classic) {
                            BluetoothScreen(
                                onStopScan = classicViewModel::stopScan,
                                onStartScan = classicViewModel::startScan,
                                onClear = classicViewModel::clearScanResults,
                                state = classicState,
                            )
                        }

                        composable(AppRoutes.Ble) {
                            BleScanConnectScreen(
                                state = bleState,
                                onStartScan = bleViewModel::startScan,
                                onStopScan = bleViewModel::stopScan,
                                onClear = bleViewModel::clearScanResults,
                                onConnect = bleViewModel::connect,
                                onDisconnect = bleViewModel::disconnect,
                                onReadCharacteristic = bleViewModel::readCharacteristic,
                                onWriteCharacteristicHex = bleViewModel::writeCharacteristicHex,
                                onOpenDetails = { device ->
                                    navController.navigate(AppRoutes.bleDetailRoute(device.address))
                                },
                            )
                        }

                        composable(AppRoutes.Saved) {
                            SavedScreen()
                        }

                        composable(
                            route = "${AppRoutes.BleDetail}/{${AppRoutes.ArgDeviceAddress}}",
                            arguments = listOf(
                                navArgument(AppRoutes.ArgDeviceAddress) { type = NavType.StringType },
                            ),
                        ) { entry ->
                            val address = entry.arguments?.getString(AppRoutes.ArgDeviceAddress).orEmpty()
                            val device =
                                bleState.bleScannedDevices.firstOrNull { it.device.address == address }?.device
                                    ?: bleState.gattConnectionState.connectedDevice
                                    ?: return@composable

                            BleDeviceDetailScreen(
                                device = device,
                                gattConnectionState = bleState.gattConnectionState,
                                gattServices = bleState.gattServices,
                                characteristicValues = bleState.characteristicValues,
                                notifyingCharacteristics = bleState.gattConnectionState.notifyingCharacteristics,
                                onBack = { navController.popBackStack() },
                                onConnect = bleViewModel::connect,
                                onDisconnect = bleViewModel::disconnect,
                                onReadCharacteristic = bleViewModel::readCharacteristic,
                                onWriteCharacteristicHex = bleViewModel::writeCharacteristicHex,
                                onSetNotificationsEnabled = bleViewModel::setNotificationsEnabled,
                                onOpenLiveData = {
                                    navController.navigate(AppRoutes.bleLiveDataRoute(device.address))
                                },
                            )
                        }

                        composable(
                            route = "${AppRoutes.BleLiveData}/{${AppRoutes.ArgDeviceAddress}}",
                            arguments = listOf(
                                navArgument(AppRoutes.ArgDeviceAddress) { type = NavType.StringType },
                            ),
                        ) { entry ->
                            val address = entry.arguments?.getString(AppRoutes.ArgDeviceAddress).orEmpty()
                            val device =
                                bleState.bleScannedDevices.firstOrNull { it.device.address == address }?.device
                                    ?: bleState.gattConnectionState.connectedDevice
                                    ?: return@composable

                            BleLiveDataScreen(
                                deviceName = device.name ?: "Unknown Device",
                                gattConnectionState = bleState.gattConnectionState,
                                services = bleState.gattServices,
                                characteristicValues = bleState.characteristicValues,
                                notifyingCharacteristics = bleState.gattConnectionState.notifyingCharacteristics,
                                onSetNotificationsEnabled = bleViewModel::setNotificationsEnabled,
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)
