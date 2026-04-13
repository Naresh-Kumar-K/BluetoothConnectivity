package com.karbrusha.bluetoothlowenergy.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.karbrusha.bluetoothlowenergy.domain.BleCharacteristicRef
import com.karbrusha.bluetoothlowenergy.domain.BluetoothDeviceDomain
import com.karbrusha.bluetoothlowenergy.presentation.BleScanConnectUiState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import com.karbrusha.bluetoothlowenergy.domain.BleScannedDevice

@Composable
fun BleScanConnectScreen(
    modifier: Modifier = Modifier,
    state: BleScanConnectUiState,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onClear: () -> Unit,
    onToggleFilter: () -> Unit,
    onConnect: (BluetoothDeviceDomain) -> Unit,
    onDisconnect: (BluetoothDeviceDomain) -> Unit,
    onReadCharacteristic: (BleCharacteristicRef) -> Unit,
    onWriteCharacteristicHex: (BleCharacteristicRef, String) -> Unit,
    onOpenDetails: (BluetoothDeviceDomain) -> Unit,
    onToggleSave: (BluetoothDeviceDomain) -> Unit,
) {
    val bg = MaterialTheme.colorScheme.background
    val hero = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
            bg
        )
    )

    // rememberSaveable survives rapid recompositions during active BLE scanning
//    var showFilterMenu by rememberSaveable { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(hero),
    ) {
        val connectedDevice = state.gattConnectionState.connectedDevice
        val visibleDevices = if (state.filterUnnamed) {
            state.bleScannedDevices.filter { !it.device.name.isNullOrBlank() && it.device.name != "Unknown Device" }
        } else {
            state.bleScannedDevices
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                HeroHeader(
                    isScanning = state.isBleScanning,
                    onStartScan = onStartScan,
                    onStopScan = onStopScan,
                )
            }

            if (connectedDevice != null) {
                item {
                    ConnectedDeviceCard(
                        deviceName = connectedDevice.name ?: "Unknown Device",
                        deviceAddress = connectedDevice.address,
                        isSaved = connectedDevice.address in state.savedAddresses,
                        onOpenDetails = { onOpenDetails(connectedDevice) },
                        onToggleSave = { onToggleSave(connectedDevice) },
                    )
                }
            }

            state.lastErrorMessage?.let { msg ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ),
                    ) {
                        Text(
                            text = msg,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "AVAILABLE DEVICES (${visibleDevices.size})",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilledTonalButton(
                            onClick = onClear,
                            enabled = state.bleScannedDevices.isNotEmpty(),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Clear", fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Box {
                            IconButton(onClick = { showFilterMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Filter options",
                                    tint = if (state.filterUnnamed)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            DropdownMenu(
                                expanded = showFilterMenu,
                                onDismissRequest = { showFilterMenu = false },
                            ) {
                                Text(
                                    text = "Filter",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(
                                        horizontal = 16.dp,
                                        vertical = 8.dp
                                    ),
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            Checkbox(
                                                checked = state.filterUnnamed,
                                                onCheckedChange = null,
                                            )
                                            Text(text = "Named devices only")
                                        }
                                    },
                                    onClick = {
                                        onToggleFilter()
                                        showFilterMenu = false
                                    },
                                )
                            }
                        }
                    }
                }
            }

            items(visibleDevices, key = { it.device.address }) { scanned ->
                // If we're connected to this device, it's already shown as "Connected device" card above.
                if (connectedDevice?.address == scanned.device.address) return@items

                DeviceCard(
                    scanned = scanned,
                    isConnected = connectedDevice?.address == scanned.device.address,
                    isSaved = scanned.device.address in state.savedAddresses,
                    onConnect = onConnect,
                    onDisconnect = onDisconnect,
                    onOpenDetails = onOpenDetails,
                    onToggleSave = onToggleSave,
                )
            }
        }

        FilledTonalButton(
            onClick = { if (state.isBleScanning) onStopScan() else onStartScan() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .height(48.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
            contentPadding = PaddingValues(horizontal = 18.dp),
        ) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(10.dp))
            Text(text = "REFRESH SCAN", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun HeroHeader(
    isScanning: Boolean,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val iconBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
        Box(
            modifier = Modifier
                .size(78.dp)
                .clip(MaterialTheme.shapes.extraLarge)
                .background(iconBg),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Bluetooth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "SCANNING ENVIRONMENT",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = "Discovering Waves",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 4.dp),
        )

        Spacer(modifier = Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            FilledTonalButton(
                onClick = onStartScan,
                enabled = !isScanning,
                modifier = Modifier.weight(1f),
            ) {
                Text(text = "Start scan")
            }
            OutlinedButton(
                onClick = onStopScan,
                enabled = isScanning,
                modifier = Modifier.weight(1f),
            ) {
                Text(text = "Stop")
            }
        }
    }
}

@Composable
private fun ConnectedDeviceCard(
    deviceName: String,
    deviceAddress: String,
    isSaved: Boolean,
    onOpenDetails: () -> Unit,
    onToggleSave: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        onClick = onOpenDetails,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Bluetooth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = deviceName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = deviceAddress,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onToggleSave) {
                    Icon(
                        imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = if (isSaved) "Remove from saved" else "Save device",
                        tint = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Connected",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            OutlinedButton(
                onClick = onOpenDetails,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "Open details")
            }
        }
    }
}

@Composable
private fun DeviceCard(
    scanned: BleScannedDevice,
    isConnected: Boolean,
    isSaved: Boolean,
    onConnect: (BluetoothDeviceDomain) -> Unit,
    onDisconnect: (BluetoothDeviceDomain) -> Unit,
    onOpenDetails: (BluetoothDeviceDomain) -> Unit,
    onToggleSave: (BluetoothDeviceDomain) -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isSaved)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        onClick = { onOpenDetails(scanned.device) },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Bluetooth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    modifier = Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = scanned.device.name ?: "Unknown Device",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = scanned.device.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                IconButton(onClick = { onToggleSave(scanned.device) }) {
                    Icon(
                        imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = if (isSaved) "Remove from saved" else "Save device",
                        tint = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                RssiBars(rssi = scanned.rssi)
                Text(
                    text = scanned.rssi?.let { "${it} dBm" } ?: "-- dBm",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (isSaved) {
                FilledTonalButton(
                    onClick = { onConnect(scanned.device) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = "Quick Connect")
                }
            }
            FilledTonalButton(
                onClick = { if (isConnected) onDisconnect(scanned.device) else onConnect(scanned.device) },
                modifier = Modifier.weight(1f),
            ) {
                Text(text = if (isConnected) "Disconnect" else "Connect")
            }
            OutlinedButton(
                onClick = { onOpenDetails(scanned.device) },
                modifier = Modifier.weight(1f),
            ) {
                Text(text = "Details")
            }
        }
    }
}

@Composable
private fun RssiBars(rssi: Int?) {
    val strength = rssiStrengthLevel(rssi)
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom) {
        repeat(5) { index ->
            val active = index < strength
            val barColor =
                if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height((8 + (index * 5)).dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(barColor),
            )
        }
    }
}

private fun rssiStrengthLevel(rssi: Int?): Int {
    if (rssi == null) return 0
    return when {
        rssi >= -55 -> 5
        rssi >= -65 -> 4
        rssi >= -75 -> 3
        rssi >= -85 -> 2
        rssi >= -95 -> 1
        else -> 0
    }
}

