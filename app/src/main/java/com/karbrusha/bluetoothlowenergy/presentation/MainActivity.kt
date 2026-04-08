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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.karbrusha.bluetoothlowenergy.presentation.components.BluetoothScreen
import com.karbrusha.bluetoothlowenergy.presentation.components.BleScanConnectScreen
import com.karbrusha.bluetoothlowenergy.ui.theme.BluetoothLowEnergyTheme
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

                var selectedTab by remember { mutableStateOf(0) }

                Scaffold(
                    modifier = Modifier.fillMaxSize().padding(vertical = 50.dp),
                    topBar = {
                        TabRow(selectedTabIndex = selectedTab) {
                            Tab(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                text = { androidx.compose.material3.Text(text = "Classic") },
                            )
                            Tab(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                text = { androidx.compose.material3.Text(text = "BLE") },
                            )
                        }
                    }
                ) { innerPadding ->
                    if (selectedTab == 0) {
                        BluetoothScreen(
                            modifier = Modifier.padding(innerPadding),
                            onStopScan = classicViewModel::stopScan,
                            onStartScan = classicViewModel::startScan,
                            state = classicState
                        )
                    } else {
                        BleScanConnectScreen(
                            modifier = Modifier.padding(innerPadding),
                            state = bleState,
                            onStartScan = bleViewModel::startScan,
                            onStopScan = bleViewModel::stopScan,
                            onConnect = bleViewModel::connect,
                            onDisconnect = bleViewModel::disconnect,
                            onReadCharacteristic = bleViewModel::readCharacteristic,
                            onWriteCharacteristicHex = bleViewModel::writeCharacteristicHex,
                        )
                    }
                }
            }
        }
    }
}
