package com.karbrusha.bluetoothlowenergy.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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

    // Only pick 4-byte SpO2/pulse packets (Byte0 == 0x81) per 500G protocol
    // Ignore 0x80 (11-byte waveform) and 0x82 (5-byte alarm limits)
    val latestRawValue = characteristicValues.entries
        .mapNotNull { entry ->
            val v = entry.value
            if (v.size == 4 && (v[0].toInt() and 0xFF) == 0x81) v else null
        }
        .lastOrNull()

    // Extract waveform points from latest 11-byte pleth packet (Byte0 == 0x80)
    val waveformPoints = characteristicValues.entries
        .mapNotNull { entry ->
            val v = entry.value
            if (v.size == 11 && (v[0].toInt() and 0xFF) == 0x80) v else null
        }
        .lastOrNull()
        ?.drop(1)  // skip Byte0, keep Bytes 1–10 as waveform values
        ?.map { (it.toInt() and 0x7F) }  // mask bit7 (always 0 per protocol)

    // Stable last-known-good reading — never resets to "--" when probe is briefly off
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
                            value = reading?.spo2?.toString() ?: "--",
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

                    // PI index card
                    reading?.piIndex?.let { pi ->
                        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column {
                                    Text(
                                        text = "PI INDEX",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        text = "Perfusion Index",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Text(
                                    text = if (pi == 0) "--" else "%.1f%%".format(pi / 10.0),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (pi == 0) MaterialTheme.colorScheme.onSurfaceVariant
                                    else MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }

                    // Plethysmography waveform
                    waveformPoints?.let { points ->
                        PlethWaveformCard(points = points)
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
    val piIndex: Int,   // 0–200, 0 = invalid per 500G protocol
    val isValid: Boolean,
    val probeOff: Boolean,
)

/**
 * Parses 500G BLE oximeter 4-byte SpO2/pulse packet:
 *
 *   Byte0: 0x81  (packet type identifier)
 *   Byte1: pulse rate  (25–250 bpm; 255 = invalid)
 *   Byte2: SpO2        (35–100 %; 127 = invalid)
 *   Byte3: PI index    (0–200; 0 = invalid)
 *
 * Per official 500G Bluetooth Protocol document.
 */
fun parseOximeterPacket(bytes: ByteArray): OximeterReading? {
    if (bytes.size != 4) return null
    if ((bytes[0].toInt() and 0xFF) != 0x81) return null  // must be SpO2/pulse packet type

    val pulseRate = bytes[1].toInt() and 0xFF
    val spo2     = bytes[2].toInt() and 0xFF
    val piIndex  = bytes[3].toInt() and 0xFF

    // Per protocol: SpO2=127 and Pulse=255 are invalid sentinel values
    val spo2Valid  = spo2 != 127 && spo2 in 35..100
    val pulseValid = pulseRate != 255 && pulseRate in 25..250

    if (!spo2Valid || !pulseValid) return null

    return OximeterReading(
        spo2      = spo2,
        pulseRate = pulseRate,
        piIndex   = piIndex,   // 0 = invalid per protocol, shown as "--" in UI
        isValid   = true,
        probeOff  = false,
    )
}

@Composable
private fun PlethWaveformCard(points: List<Int>) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "PLETHYSMOGRAPHY",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(10.dp))
            // Bar chart — each point is 0–100 (127 = invalid, shown as 0)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                points.forEach { raw ->
                    val value = if (raw == 127) 0 else raw.coerceIn(0, 100)
                    val fraction = value / 100f
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(fraction.coerceAtLeast(0.02f))
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)),
                    )
                }
            }
        }
    }
}

private fun ByteArray.toHexString(): String =
    joinToString(" ") { "%02X".format(it) }
