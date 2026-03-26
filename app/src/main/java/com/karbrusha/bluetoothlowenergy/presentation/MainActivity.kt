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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.karbrusha.bluetoothlowenergy.domain.BluetoothDevice
import com.karbrusha.bluetoothlowenergy.ui.theme.BluetoothLowEnergyTheme
import dagger.hilt.android.AndroidEntryPoint

//@AndroidEntryPoint
//class MainActivity : ComponentActivity() {
//
//    @Inject lateinit var analyticsService: AnalyticsService
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        analyticsService.logEvent("MainActivity.onCreate")
//        enableEdgeToEdge()
//        setContent {
//            BluetoothLowEnergyTheme {
//                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
//                    Greeting(
//                        name = "Android",
//                        modifier = Modifier.padding(innerPadding)
//                    )
//                }
//            }
//        }
//    }
//}
//
//@Composable
//fun Greeting(name: String, modifier: Modifier = Modifier) {
//    Text(
//        text = "Hello $name!",
//        modifier = modifier
//    )
//}
//
//@Preview(showBackground = true)
//@Composable
//fun GreetingPreview() {
//    BluetoothLowEnergyTheme {
//        Greeting("Android")
//    }
//}


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
                val viewModel = hiltViewModel<BluetoothViewmodel>()
                val state by viewModel.state.collectAsState()
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    BluetoothScreen(
                        modifier = Modifier.padding(innerPadding),
                        onStopScan = viewModel::stopScan,
                        onStartScan = viewModel::startScan,
                        state = state
                    )
                }
            }
        }
    }
}

@Composable
fun BluetoothScreen(
    modifier: Modifier = Modifier,
    viewModel: BluetoothViewmodel = hiltViewModel(),
    state: BluetoothUiState,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
//        BluetoothDeviceList()
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            Button(onClick = onStartScan) {
                Text(text = "Start Scan")
            }

            Button(onClick = onStopScan) {
                Text(text = "Start Scan")
            }
        }
    }
}

@Composable
fun BluetoothDeviceList(
    modifier: Modifier = Modifier,
    pairedDevices: List<BluetoothDevice>,
    scannedDevices: List<BluetoothDevice>,
    onClick: (BluetoothDevice) -> Unit,
) {

    LazyColumn(modifier = modifier) {
        item {
            Text(
                text = "Paired Devices", modifier = Modifier.padding(16.dp),
                fontWeight = FontWeight.Bold
            )
        }
        items(pairedDevices) { device ->
            Text(
                text = device.name ?: "No Name",
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onClick(device) }
                    .padding(16.dp),
            )
        }
        item {
            Text(
                text = "Scanned Device", modifier = Modifier.padding(16.dp),
                fontWeight = FontWeight.Bold
            )
        }
        items(scannedDevices) { device ->
            Text(
                text = device.name ?: "No Name",
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onClick(device) }
                    .padding(16.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BluetoothScreenPreview() {
    BluetoothLowEnergyTheme {
        Text(text = "Not scanning")
    }
}
