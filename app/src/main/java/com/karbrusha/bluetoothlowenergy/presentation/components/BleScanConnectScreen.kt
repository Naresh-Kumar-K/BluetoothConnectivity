package com.karbrusha.bluetoothlowenergy.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.karbrusha.bluetoothlowenergy.domain.BleCharacteristicRef
import com.karbrusha.bluetoothlowenergy.domain.BleService
import com.karbrusha.bluetoothlowenergy.domain.BluetoothDeviceDomain
import com.karbrusha.bluetoothlowenergy.domain.GattConnectionStatus
import com.karbrusha.bluetoothlowenergy.presentation.BleScanConnectUiState
import com.karbrusha.bluetoothlowenergy.domain.BleCharacteristic
import com.karbrusha.bluetoothlowenergy.domain.BleCharacteristicProperties
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment

@Composable
fun BleScanConnectScreen(
    modifier: Modifier = Modifier,
    state: BleScanConnectUiState,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onConnect: (BluetoothDeviceDomain) -> Unit,
    onDisconnect: (BluetoothDeviceDomain) -> Unit,
    onReadCharacteristic: (BleCharacteristicRef) -> Unit,
    onWriteCharacteristicHex: (BleCharacteristicRef, String) -> Unit,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Button(
                enabled = !state.isBleScanning,
                onClick = onStartScan,
            ) {
                Text(text = "Start BLE Scan")
            }
            Button(
                enabled = state.isBleScanning,
                onClick = onStopScan,
            ) {
                Text(text = "Stop")
            }
        }

        state.lastErrorMessage?.let { msg ->
            Text(
                text = "Error: $msg",
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.padding(8.dp))
        }

        val connectedDevice = state.gattConnectionState.connectedDevice

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        ) {
            item {
                Text(text = "Discovered Devices", modifier = Modifier.padding(vertical = 8.dp))
                Divider()
            }

            items(state.bleScannedDevices, key = { it.address }) { device ->
                DeviceRow(
                    device = device,
                    isConnected = connectedDevice?.address == device.address,
                    onConnect = onConnect,
                    onDisconnect = onDisconnect,
                )
                Divider()
            }

            if (connectedDevice != null) {
                item {
                    Spacer(modifier = Modifier.padding(vertical = 8.dp))
                    Text(text = "GATT Explorer", modifier = Modifier.padding(vertical = 8.dp))
                    Divider()
                }
                items(state.gattServices, key = { it.uuid }) { service ->
                    ServiceExplorer(
                        service = service,
                        characteristicValues = state.characteristicValues,
                        onReadCharacteristic = onReadCharacteristic,
                        onWriteCharacteristicHex = onWriteCharacteristicHex,
                    )
                    Divider()
                }
            }
        }

        val status = state.gattConnectionState.status
        if (status == GattConnectionStatus.Connecting || status == GattConnectionStatus.Disconnecting) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Connecting...")
            }
        }
    }
}

@Composable
private fun DeviceRow(
    device: BluetoothDeviceDomain,
    isConnected: Boolean,
    onConnect: (BluetoothDeviceDomain) -> Unit,
    onDisconnect: (BluetoothDeviceDomain) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = device.name ?: "No Name")
            Text(text = device.address, modifier = Modifier.padding(top = 2.dp))
        }

        if (isConnected) {
            Button(onClick = { onDisconnect(device) }) {
                Text(text = "Disconnect")
            }
        } else {
            Button(onClick = { onConnect(device) }) {
                Text(text = "Connect")
            }
        }
    }
}

@Composable
private fun ServiceExplorer(
    service: BleService,
    characteristicValues: Map<BleCharacteristicRef, ByteArray>,
    onReadCharacteristic: (BleCharacteristicRef) -> Unit,
    onWriteCharacteristicHex: (BleCharacteristicRef, String) -> Unit,
) {
    var expanded by remember { mutableStateOf(true) }

    Column(modifier = Modifier.padding(vertical = 10.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = "Service: ${service.uuid}")
            Button(onClick = { expanded = !expanded }) {
                Text(text = if (expanded) "Hide" else "Show")
            }
        }

        if (expanded) {
            service.characteristics.forEach { characteristic ->
                val ref = BleCharacteristicRef(
                    serviceUuid = service.uuid,
                    characteristicUuid = characteristic.uuid,
                )

                key(ref) {
                    var writeHex by rememberSaveable { mutableStateOf("") }

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(text = "Char: ${characteristic.uuid}")
                        val props = characteristic.properties
                        Text(
                            text = "Props: " + listOfNotNull(
                                if (props.readable) "R" else null,
                                if (props.writable) "W" else null,
                                if (props.writeWithoutResponse) "NoRsp" else null,
                                if (props.notifiable) "Notify" else null,
                                if (props.indicatable) "Indicate" else null,
                            ).joinToString(separator = ", "),
                        )

                        val lastValue = characteristicValues[ref]
                        Text(text = "Last: ${lastValue?.toHex() ?: "-"}")

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            if (props.readable) {
                                Button(onClick = { onReadCharacteristic(ref) }) {
                                    Text(text = "Read")
                                }
                            }

                            if (props.writable) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.End,
                                ) {
                                    OutlinedTextField(
                                        value = writeHex,
                                        onValueChange = { writeHex = it },
                                        singleLine = true,
                                        label = { Text("Write hex") },
                                        modifier = Modifier.width(160.dp),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(onClick = { onWriteCharacteristicHex(ref, writeHex) }) {
                                        Text(text = "Write")
                                    }
                                }
                            }
                        }
                        Divider()
                    }
                }
            }
        }
    }
}

private fun ByteArray.toHex(): String {
    return joinToString("") { byte -> "%02X".format(byte) }
}

