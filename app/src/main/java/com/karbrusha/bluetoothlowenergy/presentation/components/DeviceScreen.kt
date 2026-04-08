package com.karbrusha.bluetoothlowenergy.presentation.components

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.karbrusha.bluetoothlowenergy.domain.BluetoothDevice
import com.karbrusha.bluetoothlowenergy.presentation.BluetoothUiState
import com.karbrusha.bluetoothlowenergy.ui.theme.BluetoothLowEnergyTheme


@Composable
fun BluetoothScreen(
    modifier: Modifier = Modifier,
    state: BluetoothUiState,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
) {
    Column(modifier.fillMaxSize()) {
        BluetoothDeviceList(
            pairedDevices = state.pairedDevices,
            scannedDevices = state.scannedDevices,
            onClick = {},
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 24.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 50.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Button(onClick = onStartScan) {
                Text(text = "Start Scan", fontWeight = FontWeight.Bold)
            }

            Button(onClick = onStopScan) {
                Text(text = "Stop Scan", fontWeight = FontWeight.Bold)
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
