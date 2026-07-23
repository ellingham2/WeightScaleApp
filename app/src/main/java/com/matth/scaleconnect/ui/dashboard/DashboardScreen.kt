package com.matth.scaleconnect.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matth.scaleconnect.ble.ConnectionState
import com.matth.scaleconnect.ble.WeighInResult
import com.matth.scaleconnect.data.ProfileSlot
import com.matth.scaleconnect.data.WeighInRecord
import com.matth.scaleconnect.data.WeightUnit
import com.matth.scaleconnect.data.lbToKg
import com.matth.scaleconnect.ui.components.MetricRingCard
import com.matth.scaleconnect.ui.theme.ScaleTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun DashboardScreen(
    connectionState: ConnectionState,
    deviceName: String?,
    reading: WeighInResult,
    activeSlot: ProfileSlot?,
    profileSlots: List<ProfileSlot>,
    history: List<WeighInRecord>,
    weightUnit: WeightUnit,
    onConnectClick: () -> Unit,
    onSelectSlot: (Int) -> Unit,
) {
    val extended = ScaleTheme.extendedColors
    val previous = history.firstOrNull()
    var slotMenuExpanded by remember { mutableStateOf(false) }

    fun displayWeight(lb: Double): Double = if (weightUnit == WeightUnit.KG) lb.lbToKg() else lb
    val unitLabel = if (weightUnit == WeightUnit.KG) "kg" else "lb"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusPill(connectionState, deviceName, onConnectClick)
            Box {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        .clickable(enabled = profileSlots.isNotEmpty()) { slotMenuExpanded = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = activeSlot?.name?.take(1)?.uppercase() ?: "?",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                    )
                }
                DropdownMenu(expanded = slotMenuExpanded, onDismissRequest = { slotMenuExpanded = false }) {
                    profileSlots.forEach { slot ->
                        DropdownMenuItem(
                            text = { Text(slot.name + if (slot.id == activeSlot?.id) " (current)" else "") },
                            onClick = {
                                onSelectSlot(slot.id)
                                slotMenuExpanded = false
                            },
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "TODAY · ${SimpleDateFormat("h:mm a", Locale.US).format(Date())}".uppercase(),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = extended.faint,
            )
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 8.dp)) {
                Text(
                    text = reading.weightLb?.let { "%.1f".format(displayWeight(it)) } ?: "--",
                    style = MaterialTheme.typography.displayLarge,
                )
                Text(
                    text = unitLabel,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = extended.muted,
                    modifier = Modifier.padding(start = 6.dp, bottom = 9.dp),
                )
            }
            if (reading.weightLb != null && previous != null) {
                val delta = displayWeight(reading.weightLb) - displayWeight(previous.weightLb)
                Box(
                    modifier = Modifier
                        .padding(top = 14.dp)
                        .background(extended.goodSoft, RoundedCornerShape(999.dp))
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                ) {
                    Text(
                        text = "${if (delta <= 0) "▼" else "▲"} %.1f $unitLabel since last".format(abs(delta)),
                        color = extended.good,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Body composition", style = MaterialTheme.typography.titleMedium)
            Text("5 metrics", fontSize = 12.sp, color = extended.faint, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricRingCard(
                    label = "Body Fat",
                    value = reading.bodyFatPercent ?: 0.0,
                    ringColor = extended.metricFat,
                    modifier = Modifier.weight(1f),
                )
                MetricRingCard(
                    label = "Hydration",
                    value = reading.hydrationPercent ?: 0.0,
                    ringColor = extended.metricHydration,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricRingCard(
                    label = "Muscle",
                    value = reading.musclePercent ?: 0.0,
                    ringColor = extended.metricMuscle,
                    modifier = Modifier.weight(1f),
                )
                MetricRingCard(
                    label = "Bone",
                    value = reading.bonePercent ?: 0.0,
                    ringColor = extended.metricBone,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
                .padding(horizontal = 18.dp, vertical = 15.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("Basal metabolism", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Calories at rest · BMR", fontSize = 12.sp, color = extended.faint)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = reading.bmrKcal?.let { "%,d".format(it) } ?: "--",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = extended.metricBmr,
                )
                Text(
                    text = "KCAL / DAY",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = extended.faint,
                )
            }
        }
    }
}

@Composable
private fun StatusPill(state: ConnectionState, deviceName: String?, onClick: () -> Unit) {
    val extended = ScaleTheme.extendedColors
    val (bg, dotColor, text) = when (state) {
        ConnectionState.READY -> Triple(extended.goodSoft, extended.good, "Connected" + (deviceName?.let { " · $it" } ?: ""))
        ConnectionState.SCANNING, ConnectionState.CONNECTING, ConnectionState.DISCOVERING ->
            Triple(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.primary, "Scanning…")
        ConnectionState.DISCONNECTED -> Triple(MaterialTheme.colorScheme.surface, extended.faint, "Disconnected")
    }
    Row(
        modifier = Modifier
            .background(bg, RoundedCornerShape(999.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(999.dp))
            .clickable(enabled = state == ConnectionState.DISCONNECTED, onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(dotColor, CircleShape),
        )
        Text(
            text = text,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (state == ConnectionState.READY) extended.good else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}
