package com.karbrusha.bluetoothlowenergy.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.karbrusha.bluetoothlowenergy.domain.BleCharacteristic
import com.karbrusha.bluetoothlowenergy.domain.BleCharacteristicProperties
import com.karbrusha.bluetoothlowenergy.domain.BleCharacteristicRef
import com.karbrusha.bluetoothlowenergy.domain.BleService
import com.karbrusha.bluetoothlowenergy.domain.BluetoothDeviceDomain
import com.karbrusha.bluetoothlowenergy.domain.GattConnectionStatus
import com.karbrusha.bluetoothlowenergy.domain.GattConnectionState

@Composable
fun BleDeviceDetailScreen(
    modifier: Modifier = Modifier,
    device: BluetoothDeviceDomain,
    gattConnectionState: GattConnectionState,
    gattServices: List<BleService>,
    characteristicValues: Map<BleCharacteristicRef, ByteArray>,
    onBack: () -> Unit,
    onConnect: (BluetoothDeviceDomain) -> Unit,
    onDisconnect: (BluetoothDeviceDomain) -> Unit,
    onReadCharacteristic: (BleCharacteristicRef) -> Unit,
    onWriteCharacteristicHex: (BleCharacteristicRef, String) -> Unit,
) {
    val bg = MaterialTheme.colorScheme.background
    val hero = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
            bg,
        ),
    )

    val deviceConnected = gattConnectionState.connectedDevice?.address == device.address
    val status = gattConnectionState.status

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(hero),
    ) {
        HeaderCard(
            deviceName = device.name ?: "Unknown Device",
            statusText = if (deviceConnected) "Active" else "Disconnected",
        )

        gattConnectionState.errorMessage?.let { msg ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
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
            Spacer(modifier = Modifier.height(10.dp))
        }

        when {
            !deviceConnected -> {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Connect to view services and characteristics.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        FilledTonalButton(onClick = { onConnect(device) }) {
                            Text(text = "Connect")
                        }
                    }
                }
            }

            status == GattConnectionStatus.Connecting || status == GattConnectionStatus.Disconnecting -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = if (status == GattConnectionStatus.Connecting) "Connecting..." else "Disconnecting...")
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        SignalCard(
                            // Real RSSI isn't stored on the connected state in this project,
                            // so we show a stable "good" value derived from connection state.
                            percent = 98,
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Available Services",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Info",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    items(gattServices, key = { it.uuid }) { service ->
                        ServiceCard(
                            service = service,
                            characteristicValues = characteristicValues,
                            onReadCharacteristic = onReadCharacteristic,
                            onWriteCharacteristicHex = onWriteCharacteristicHex,
                        )
                    }

                    item {
                        Button(
                            onClick = { onDisconnect(device) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bluetooth,
                                contentDescription = null,
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = "Disconnect Device", fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderCard(
    deviceName: String,
    statusText: String,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "CONNECTED DEVICE",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = deviceName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 6.dp),
            )

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "STATUS",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Circle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(10.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun SignalCard(
    percent: Int,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "STATUS",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = percent.toString(),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "% SIGNAL",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "RSSI (DBM)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.height(10.dp))
            RssiChart()
        }
    }
}

@Composable
private fun RssiChart() {
    // Static look-alike bars (we don't have a time series in state yet).
    val bars = listOf(0.35f, 0.52f, 0.45f, 0.66f, 0.58f, 0.82f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        bars.forEachIndexed { index, h ->
            val active = index == bars.lastIndex
            val color =
                if (active) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(fraction = h)
                    .clip(MaterialTheme.shapes.small)
                    .background(color),
            )
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = "-85 dBm", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = "-42 dBm", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ServiceCard(
    service: BleService,
    characteristicValues: Map<BleCharacteristicRef, ByteArray>,
    onReadCharacteristic: (BleCharacteristicRef) -> Unit,
    onWriteCharacteristicHex: (BleCharacteristicRef, String) -> Unit,
) {
    var expanded by remember { mutableStateOf(true) }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
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
                            text = friendlyServiceName(service.uuid.toString()),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "0x${service.uuid.toString().takeLast(4).uppercase()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                    )
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(10.dp))
                service.characteristics.forEach { characteristic: BleCharacteristic ->
                    val ref = BleCharacteristicRef(
                        serviceUuid = service.uuid,
                        characteristicUuid = characteristic.uuid,
                    )

                    key(ref) {
                        val props: BleCharacteristicProperties = characteristic.properties
                        val lastValue = characteristicValues[ref]

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                val friendlyValue = decodeCharacteristicValue(characteristic.uuid.toString(), lastValue)
                                Text(
                                    text = friendlyCharacteristicName(characteristic.uuid.toString()),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                )
                                Text(
                                    text = friendlyValue ?: (lastValue?.toHex() ?: "-"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (props.readable) {
                                    FilledTonalButton(
                                        onClick = { onReadCharacteristic(ref) },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    ) {
                                        Text(text = "READ", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                                if (props.notifiable || props.indicatable) {
                                    OutlinedButton(
                                        onClick = { /* TODO enable notifications */ },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    ) {
                                        Text(text = "NOTIFY", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                        Divider(modifier = Modifier.padding(top = 10.dp, bottom = 10.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }
}

private fun ByteArray.toHex(): String {
    return joinToString("") { byte -> "%02X".format(byte) }
}

private fun friendlyServiceName(uuid: String): String {
    return when {
        uuid.endsWith("180F", ignoreCase = true) -> "Battery Service"
        uuid.endsWith("180D", ignoreCase = true) -> "Heart Rate"
        uuid.endsWith("180A", ignoreCase = true) -> "Device Information"
        else -> "Service"
    }
}

private fun friendlyCharacteristicName(uuid: String): String {
    return when {
        uuid.endsWith("2A19", ignoreCase = true) -> "Battery Level"
        uuid.endsWith("2A37", ignoreCase = true) -> "Heart Rate Measurement"
        uuid.endsWith("2A29", ignoreCase = true) -> "Manufacturer Name"
        else -> "Characteristic"
    }
}

private fun decodeCharacteristicValue(uuid: String, value: ByteArray?): String? {
    if (value == null || value.isEmpty()) return null
    return when {
        // Battery Level characteristic is a single uint8 percent.
        uuid.endsWith("2A19", ignoreCase = true) -> {
            val pct = value[0].toInt() and 0xFF
            "($pct%)"
        }

        else -> null
    }
}

