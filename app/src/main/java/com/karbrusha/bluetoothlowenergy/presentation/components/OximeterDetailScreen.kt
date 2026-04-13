package com.karbrusha.bluetoothlowenergy.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.karbrusha.bluetoothlowenergy.domain.BleCharacteristicRef
import com.karbrusha.bluetoothlowenergy.domain.BleService
import com.karbrusha.bluetoothlowenergy.domain.BluetoothDeviceDomain
import com.karbrusha.bluetoothlowenergy.domain.GattConnectionState
import com.karbrusha.bluetoothlowenergy.domain.GattConnectionStatus

@Composable
fun OximeterDetailScreen(
    modifier: Modifier = Modifier,
    device: BluetoothDeviceDomain,
    gattConnectionState: GattConnectionState,
    gattServices: List<BleService>,
    characteristicValues: Map<BleCharacteristicRef, ByteArray>,
    notifyingCharacteristics: Set<BleCharacteristicRef>,
    onConnect: (BluetoothDeviceDomain) -> Unit,
    onDisconnect: (BluetoothDeviceDomain) -> Unit,
    onReadCharacteristic: (BleCharacteristicRef) -> Unit,
    onSetNotificationsEnabled: (BleCharacteristicRef, Boolean) -> Unit,
) {
    val hero = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            MaterialTheme.colorScheme.background,
        ),
    )

    val deviceConnected = gattConnectionState.connectedDevice?.address == device.address
    val status = gattConnectionState.status

    // Auto-subscribe to all notifiable/indicatable characteristics when services are discovered
    LaunchedEffect(gattServices) {
        if (gattServices.isEmpty()) return@LaunchedEffect
        gattServices.forEach { svc ->
            svc.characteristics.forEach { char ->
                if (char.properties.notifiable || char.properties.indicatable) {
                    val ref = BleCharacteristicRef(
                        serviceUuid = svc.uuid,
                        characteristicUuid = char.uuid,
                    )
                    if (!notifyingCharacteristics.contains(ref)) {
                        onSetNotificationsEnabled(ref, true)
                    }
                }
            }
        }
    }

    // Only pick 4-byte packets (SpO2/pulse) — ignore 11-byte waveform packets
    val latestRawValue = characteristicValues.entries
        .mapNotNull { if (it.value.size == 4) it.value else null }
        .lastOrNull()

    // Stable last-known-good reading — never resets to "--" when probe is briefly off
    // or when an 11-byte waveform packet overwrites the map entry
    var stableReading by remember { mutableStateOf<OximeterReading?>(null) }
    val newReading = latestRawValue?.let { parseOximeterPacket(it) }
    if (newReading != null && newReading.isValid) {
        stableReading = newReading
    }
    val reading = stableReading
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(hero)
            .verticalScroll(rememberScrollState()),
    ) {
        // Header
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.WaterDrop,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp),
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "PULSE OXIMETER",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = device.name ?: "Oximeter",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = if (deviceConnected) "● Connected" else "○ Disconnected",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (deviceConnected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }

        when {
            !deviceConnected -> {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Connect to start reading SpO₂ and pulse data.")
                        Spacer(modifier = Modifier.height(12.dp))
                        FilledTonalButton(onClick = { onConnect(device) }) { Text("Connect") }
                    }
                }
            }

            status == GattConnectionStatus.Connecting -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Connecting…")
                }
            }

            else -> {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Main SpO2 + Pulse metrics
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        MetricCard(
                            modifier = Modifier.weight(1f),
                            label = "SpO₂",
                            value = reading?.spo2?.let { "$it" } ?: "--",
                            unit = "%",
                            icon = Icons.Default.WaterDrop,
                            isValid = reading?.isValid == true,
                        )
                        MetricCard(
                            modifier = Modifier.weight(1f),
                            label = "Pulse Rate",
                            value = reading?.pulseRate?.toString() ?: "--",
                            unit = "bpm",
                            icon = Icons.Default.Favorite,
                            isValid = reading?.isValid == true,
                        )
                    }

                    // Raw packet debug card
//                    latestRawValue?.let { raw ->
//                        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
//                            Column(modifier = Modifier.padding(14.dp)) {
//                                Text(
//                                    text = "RAW PACKET",
//                                    style = MaterialTheme.typography.labelSmall,
//                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
//                                    fontWeight = FontWeight.SemiBold,
//                                )
//                                Spacer(modifier = Modifier.height(6.dp))
//                                Text(
//                                    text = raw.toHexString(),
//                                    style = MaterialTheme.typography.bodyMedium,
//                                    fontWeight = FontWeight.Medium,
//                                    color = MaterialTheme.colorScheme.primary,
//                                )
//                                if (reading != null) {
//                                    Spacer(modifier = Modifier.height(4.dp))
//                                    HorizontalDivider()
//                                    Spacer(modifier = Modifier.height(4.dp))
//                                    Text(
//                                        text = "Flags: 0x${raw[0].toInt().and(0xFF).toString(16).uppercase()}  " +
//                                            "Valid: ${reading.isValid}  " +
//                                            "Probe off: ${reading.probeOff}",
//                                        style = MaterialTheme.typography.bodySmall,
//                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
//                                    )
//                                }
//                            }
//                        }
//                    }

                    // Notify controls for all characteristics
                    NotifyControlsCard(
                        gattServices = gattServices,
                        notifyingCharacteristics = notifyingCharacteristics,
                        onSetNotificationsEnabled = onSetNotificationsEnabled,
                    )

                    Button(
                        onClick = { onDisconnect(device) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                    ) {
                        Icon(Icons.Default.Bluetooth, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Disconnect", fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    unit: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isValid: Boolean = true,
) {
    val valueColor = when {
        !isValid -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.primary
    }
    ElevatedCard(
        modifier = modifier,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = valueColor,
                modifier = Modifier.size(28.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = valueColor,
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NotifyControlsCard(
    gattServices: List<BleService>,
    notifyingCharacteristics: Set<BleCharacteristicRef>,
    onSetNotificationsEnabled: (BleCharacteristicRef, Boolean) -> Unit,
) {
    val notifiableRefs = gattServices.flatMap { svc ->
        svc.characteristics
            .filter { it.properties.notifiable || it.properties.indicatable }
            .map { char -> BleCharacteristicRef(serviceUuid = svc.uuid, characteristicUuid = char.uuid) }
    }
    if (notifiableRefs.isEmpty()) return

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "NOTIFICATIONS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            notifiableRefs.forEach { ref ->
                val isNotifying = notifyingCharacteristics.contains(ref)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = ref.characteristicUuid.takeLast(4).uppercase(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(
                        onClick = { onSetNotificationsEnabled(ref, !isNotifying) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = if (isNotifying) "STOP" else "START",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }
    }
}

// ── Packet parsing ────────────────────────────────────────────────────────────

data class OximeterReading(
    val spo2: Int,
    val pulseRate: Int,
    val isValid: Boolean,
    val probeOff: Boolean,
)

/**
 * Parses proprietary 4-byte oximeter packets (Contec/ChoiceMMed/Berry format):
 *   Byte 0: flags  (bit7=valid, bit6=probe-off, bit0=beep)
 *   Byte 1: SpO2   (0–100 %)
 *   Byte 2: Pulse  low byte
 *   Byte 3: Pulse  high byte (or PI — device-dependent)
 *
 * Also handles standard BLE PLX (2A5E/2A5F) and Heart Rate (2A37) as fallback.
 */
fun parseOximeterPacket(bytes: ByteArray): OximeterReading? {
    if (bytes.size != 4) return null  // only handle 4-byte SpO2/pulse packets

    val flags = bytes[0].toInt() and 0xFF
    val probeOff = (flags and 0x40) != 0
    val isValid = (flags and 0x80) != 0 && !probeOff

    val spo2 = bytes[2].toInt() and 0xFF
    val pulseLo = bytes[1].toInt() and 0xFF
    val pulseHi = bytes[3].toInt() and 0xFF
    val pulse = (pulseHi shl 8) or pulseLo

    // Reject physiologically impossible values
    if (spo2 == 0 || spo2 > 100) return null
    val pulseClamped = if (pulse in 1..300) pulse else if (pulseLo in 1..300) pulseLo else return null

    return OximeterReading(
        spo2 = spo2,
        pulseRate = pulseClamped,
        isValid = isValid,
        probeOff = probeOff,
    )
}

private fun ByteArray.toHexString(): String =
    joinToString(" ") { "%02X".format(it) }
