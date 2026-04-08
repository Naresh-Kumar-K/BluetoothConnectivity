package com.karbrusha.bluetoothlowenergy.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.karbrusha.bluetoothlowenergy.domain.BleCharacteristicRef
import com.karbrusha.bluetoothlowenergy.domain.BleService
import com.karbrusha.bluetoothlowenergy.domain.GattConnectionState

@Composable
fun BleLiveDataScreen(
    modifier: Modifier = Modifier,
    deviceName: String,
    gattConnectionState: GattConnectionState,
    services: List<BleService>,
    characteristicValues: Map<BleCharacteristicRef, ByteArray>,
    notifyingCharacteristics: Set<BleCharacteristicRef>,
    onSetNotificationsEnabled: (BleCharacteristicRef, Boolean) -> Unit,
) {
    val bg = MaterialTheme.colorScheme.background
    val hero = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
            bg,
        ),
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(hero),
    ) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "LIVE DATA",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = deviceName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                Icon(
                    imageVector = Icons.Default.ShowChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }

        val connected = gattConnectionState.connectedDevice != null
        if (!connected) {
            Text(
                text = "No connected device. Connect first, then enable NOTIFY on a characteristic.",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return
        }

        val notifiableRefs = services.flatMap { svc ->
            svc.characteristics.mapNotNull { ch ->
                if (ch.properties.notifiable || ch.properties.indicatable) {
                    BleCharacteristicRef(serviceUuid = svc.uuid, characteristicUuid = ch.uuid)
                } else null
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Oximeter (auto-detect)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        // Strategy:
                        // 1) Prefer "My Oximeter" frames from the PDF (0x81 prefix).
                        // 2) Else, show any last value from a notifying characteristic.
                        // 3) Else, fall back to any value we have.
                        val decoded = characteristicValues.entries
                            .asSequence()
                            .mapNotNull { (_, bytes) -> bytes.decodeMyOximeterFrameOrNull() }
                            .firstOrNull()
                            ?: characteristicValues.entries
                                .asSequence()
                                .filter { (ref, _) -> notifyingCharacteristics.contains(ref) }
                                .map { (_, bytes) -> "Raw: ${bytes.toHex()}" }
                                .firstOrNull()
                            ?: characteristicValues.entries
                                .asSequence()
                                .map { (_, bytes) -> "Raw: ${bytes.toHex()}" }
                                .firstOrNull()
                        Text(
                            text = decoded ?: "Waiting for data… Enable NOTIFY on PLX (2A5F/2A60) if available.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item {
                Text(
                    text = "NOTIFY SOURCES",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            items(notifiableRefs, key = { it.serviceUuid + "|" + it.characteristicUuid }) { ref ->
                val isNotifying = notifyingCharacteristics.contains(ref)
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Bluetooth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .clip(MaterialTheme.shapes.small)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                                        .padding(6.dp),
                                )
                                Spacer(modifier = Modifier.padding(6.dp))
                                Column {
                                    Text(
                                        text = shortUuid(ref.characteristicUuid),
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        text = "svc ${shortUuid(ref.serviceUuid)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            OutlinedButton(onClick = { onSetNotificationsEnabled(ref, !isNotifying) }) {
                                Text(text = if (isNotifying) "STOP" else "NOTIFY")
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 10.dp))
                        val value = characteristicValues[ref]
                        Text(
                            text = "Last: ${value?.toHex() ?: "-"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

private fun shortUuid(uuid: String): String {
    return uuid.takeLast(4).uppercase()
}

private fun ByteArray.toHex(): String = joinToString("") { "%02X".format(it) }

// Based on the PDF sample code:
// - Notifications frame starts with 0x81 (signed byte -127)
// - content[2] is SpO2 unless it equals 0x7F
// - content[1] is BPM unless it equals 0xFF
private fun ByteArray.decodeMyOximeterFrameOrNull(): String? {
    if (size < 3) return null
    val header = this[0].toInt() and 0xFF
    if (header != 0x81) return null

    val bpmRaw = this[1].toInt() and 0xFF
    val spo2Raw = this[2].toInt() and 0xFF

    val bpm = bpmRaw.takeUnless { it == 0xFF }
    val spo2 = spo2Raw.takeUnless { it == 0x7F }

    if (bpm == null && spo2 == null) return null

    val parts = buildList {
        spo2?.let { add("SpO₂: $it%") }
        bpm?.let { add("BPM: $it") }
    }.joinToString(separator = "   ")

    return parts
}

