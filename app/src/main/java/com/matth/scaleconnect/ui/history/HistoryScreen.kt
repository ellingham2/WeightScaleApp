package com.matth.scaleconnect.ui.history

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matth.scaleconnect.data.WeighInRecord
import com.matth.scaleconnect.data.WeightUnit
import com.matth.scaleconnect.data.lbToKg
import com.matth.scaleconnect.ui.components.SegmentedControl
import com.matth.scaleconnect.ui.components.TrendPoint
import com.matth.scaleconnect.ui.components.WeightTrendChart
import com.matth.scaleconnect.ui.theme.ScaleTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

private enum class Range(val label: String, val days: Int?) {
    Week("W", 7), Month("1M", 30), ThreeMonth("3M", 90), Year("1Y", 365), All("All", null)
}

private enum class ChartMetric(val label: String) {
    Weight("Weight"), Fat("Body Fat"), Hydration("Hydration"), Muscle("Muscle"), Bone("Bone"), Bmr("BMR");

    fun valueOf(record: WeighInRecord, weightUnit: WeightUnit): Double = when (this) {
        Weight -> if (weightUnit == WeightUnit.KG) record.weightLb.lbToKg() else record.weightLb
        Fat -> record.bodyFatPercent
        Hydration -> record.hydrationPercent
        Muscle -> record.musclePercent
        Bone -> record.bonePercent
        Bmr -> record.bmrKcal.toDouble()
    }

    fun unitLabel(weightUnit: WeightUnit): String = when (this) {
        Weight -> if (weightUnit == WeightUnit.KG) "kg" else "lb"
        Bmr -> "kcal"
        else -> "%"
    }

    fun format(value: Double, weightUnit: WeightUnit): String {
        val unit = unitLabel(weightUnit)
        return if (this == Bmr) "%,.0f %s".format(value, unit) else "%.1f %s".format(value, unit)
    }
}

private sealed class ChartSelection {
    data class Single(val index: Int) : ChartSelection()
    data class Range(val startIndex: Int, val endIndex: Int) : ChartSelection()
}

@Composable
fun HistoryScreen(history: List<WeighInRecord>, weightUnit: WeightUnit) {
    val extended = ScaleTheme.extendedColors
    var range by remember { mutableStateOf(Range.Month) }
    var metric by remember { mutableStateOf(ChartMetric.Weight) }
    var selectedRecord by remember { mutableStateOf<WeighInRecord?>(null) }
    var chartSelection by remember { mutableStateOf<ChartSelection?>(null) }

    val cutoffMillis = range.days?.let {
        Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -it) }.timeInMillis
    }
    val filtered = if (cutoffMillis != null) {
        history.filter { it.timestampMillis >= cutoffMillis }
    } else {
        history
    }
    val chronological = filtered.sortedBy { it.timestampMillis }
    val chartPoints = chronological.map {
        TrendPoint(SimpleDateFormat("MMM d", Locale.US).format(Date(it.timestampMillis)), metric.valueOf(it, weightUnit))
    }
    val totalDelta = if (chronological.size >= 2) {
        metric.valueOf(chronological.last(), weightUnit) - metric.valueOf(chronological.first(), weightUnit)
    } else {
        null
    }

    // Chart selections are made by index into `chronological`; if the underlying
    // range/data changes, a stale index could point at the wrong weigh-in, so drop it.
    LaunchedEffect(range, chronological.size) { chartSelection = null }

    val selectedIndices: Set<Int> = when (val sel = chartSelection) {
        is ChartSelection.Single -> setOf(sel.index)
        is ChartSelection.Range -> setOf(sel.startIndex, sel.endIndex)
        null -> emptySet()
    }

    val rangeSelectionInfo = (chartSelection as? ChartSelection.Range)?.let { sel ->
        val a = chronological.getOrNull(sel.startIndex)
        val b = chronological.getOrNull(sel.endIndex)
        if (a != null && b != null) {
            val (earlier, later) = if (a.timestampMillis <= b.timestampMillis) a to b else b to a
            Triple(earlier, later, metric.valueOf(later, weightUnit) - metric.valueOf(earlier, weightUnit))
        } else {
            null
        }
    }

    val displayedRecords: List<WeighInRecord> = when (val sel = chartSelection) {
        is ChartSelection.Single -> listOfNotNull(chronological.getOrNull(sel.index))
        is ChartSelection.Range -> rangeSelectionInfo?.let { (earlier, later, _) ->
            filtered.filter { it.timestampMillis in earlier.timestampMillis..later.timestampMillis }
        } ?: filtered
        null -> filtered
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("History", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(22.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(22.dp))
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(metric.label, style = MaterialTheme.typography.titleMedium)
                totalDelta?.let {
                    Box(
                        modifier = Modifier
                            .background(extended.goodSoft, RoundedCornerShape(999.dp))
                            .padding(horizontal = 11.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = "${if (it <= 0) "▼" else "▲"} ${metric.format(abs(it), weightUnit)} this range",
                            color = extended.good,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
            Spacer(Modifier.height(13.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ChartMetric.entries) { m ->
                    MetricChip(label = m.label, selected = m == metric, onClick = { metric = m })
                }
            }
            Spacer(Modifier.height(13.dp))
            SegmentedControl(
                options = Range.entries,
                selected = range,
                onSelect = { range = it },
                label = { it.label },
            )
            Spacer(Modifier.height(16.dp))
            WeightTrendChart(
                points = chartPoints,
                selectedIndices = selectedIndices,
                onPointTap = { idx -> chartSelection = ChartSelection.Single(idx) },
                onRangeSelect = { i1, i2 -> chartSelection = ChartSelection.Range(i1, i2) },
            )
            if (chartSelection != null) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val label = when (val sel = chartSelection) {
                        is ChartSelection.Single -> chronological.getOrNull(sel.index)?.let {
                            "Showing " + SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date(it.timestampMillis))
                        } ?: ""
                        is ChartSelection.Range -> rangeSelectionInfo?.let { (earlier, later, delta) ->
                            val fmt = SimpleDateFormat("MMM d", Locale.US)
                            "${fmt.format(Date(earlier.timestampMillis))} → ${fmt.format(Date(later.timestampMillis))} · " +
                                "${if (delta <= 0) "▼" else "▲"} ${metric.format(abs(delta), weightUnit)}"
                        } ?: ""
                        null -> ""
                    }
                    Text(label, fontSize = 12.5.sp, color = extended.muted, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Clear",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { chartSelection = null },
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Weigh-ins", style = MaterialTheme.typography.titleMedium)
            Text(range.label, fontSize = 12.5.sp, color = extended.faint, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(10.dp))

        if (displayedRecords.isEmpty()) {
            Text("No weigh-ins in this range yet.", color = extended.faint, fontSize = 13.sp)
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(22.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(22.dp)),
            ) {
                items(displayedRecords) { record ->
                    val index = displayedRecords.indexOf(record)
                    val older = displayedRecords.getOrNull(index + 1)
                    WeighInRow(record, older, weightUnit, onClick = { selectedRecord = record })
                    if (index != displayedRecords.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
    }

    selectedRecord?.let { record ->
        WeighInDetailDialog(record, weightUnit, onDismiss = { selectedRecord = null })
    }
}

@Composable
private fun MetricChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val extended = ScaleTheme.extendedColors
    Box(
        modifier = Modifier
            .background(
                if (selected) MaterialTheme.colorScheme.primary else extended.surface2,
                RoundedCornerShape(999.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(
            text = label,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else extended.muted,
        )
    }
}

@Composable
private fun WeighInRow(record: WeighInRecord, older: WeighInRecord?, weightUnit: WeightUnit, onClick: () -> Unit) {
    val extended = ScaleTheme.extendedColors
    val date = Date(record.timestampMillis)
    val displayWeight = if (weightUnit == WeightUnit.KG) record.weightLb.lbToKg() else record.weightLb
    val unitLabel = if (weightUnit == WeightUnit.KG) "kg" else "lb"
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.width(60.dp)) {
            Text(SimpleDateFormat("EEE", Locale.US).format(date), fontSize = 12.sp, color = extended.muted, fontWeight = FontWeight.SemiBold)
            Text(SimpleDateFormat("MMM d", Locale.US).format(date), fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("%.1f".format(displayWeight), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(unitLabel, fontSize = 12.sp, color = extended.faint, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 4.dp))
                older?.let {
                    val delta = displayWeight - (if (weightUnit == WeightUnit.KG) it.weightLb.lbToKg() else it.weightLb)
                    Text(
                        text = "  ${if (delta <= 0) "▼" else "▲"} %.1f".format(abs(delta)),
                        fontSize = 12.sp,
                        color = extended.muted,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Text(
                text = "Fat %.1f%% · Muscle %.1f%% · Water %.1f%%".format(record.bodyFatPercent, record.musclePercent, record.hydrationPercent),
                fontSize = 12.sp,
                color = extended.muted,
            )
        }
        Text("›", fontSize = 19.sp, color = extended.faint)
    }
}

@Composable
private fun WeighInDetailDialog(record: WeighInRecord, weightUnit: WeightUnit, onDismiss: () -> Unit) {
    val extended = ScaleTheme.extendedColors
    val date = Date(record.timestampMillis)
    val weightText = if (weightUnit == WeightUnit.KG) {
        "%.1f kg".format(record.weightLb.lbToKg())
    } else {
        "%.1f lb".format(record.weightLb)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(SimpleDateFormat("EEEE, MMM d · h:mm a", Locale.US).format(date))
        },
        text = {
            Column {
                DetailRow("Weight", weightText, extended)
                DetailRow("Body Fat", "%.1f%%".format(record.bodyFatPercent), extended)
                DetailRow("Hydration", "%.1f%%".format(record.hydrationPercent), extended)
                DetailRow("Muscle", "%.1f%%".format(record.musclePercent), extended)
                DetailRow("Bone", "%.1f%%".format(record.bonePercent), extended)
                DetailRow("BMR", "%,d kcal".format(record.bmrKcal), extended)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

@Composable
private fun DetailRow(label: String, value: String, extended: com.matth.scaleconnect.ui.theme.ExtendedColors) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontSize = 14.sp, color = extended.muted)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}
