package com.matth.scaleconnect.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matth.scaleconnect.ble.ConnectionState
import com.matth.scaleconnect.data.HeightUnit
import com.matth.scaleconnect.data.MAX_PROFILE_SLOTS
import com.matth.scaleconnect.data.ProfileSlot
import com.matth.scaleconnect.data.ThemeMode
import com.matth.scaleconnect.data.WeightUnit
import com.matth.scaleconnect.data.cmToFeetInches
import com.matth.scaleconnect.data.feetInchesToCm
import com.matth.scaleconnect.data.kgToLb
import com.matth.scaleconnect.data.lbToKg
import com.matth.scaleconnect.ui.components.SegmentedControl
import com.matth.scaleconnect.ui.theme.ScaleTheme

@Composable
fun ProfileScreen(
    slots: List<ProfileSlot>,
    activeSlot: ProfileSlot?,
    connectionState: ConnectionState,
    deviceName: String?,
    backgroundEnabled: Boolean,
    themeMode: ThemeMode,
    weightUnit: WeightUnit,
    heightUnit: HeightUnit,
    onSelectSlot: (Int) -> Unit,
    onAddProfile: (name: String) -> Unit,
    onRemoveProfile: (Int) -> Unit,
    onUpdateProfile: (slotId: Int, age: Int, heightCm: Int, isMale: Boolean, goalWeightLb: Int) -> Unit,
    onManage: () -> Unit,
    onSetBackgroundEnabled: (Boolean) -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetWeightUnit: (WeightUnit) -> Unit,
    onSetHeightUnit: (HeightUnit) -> Unit,
) {
    val extended = ScaleTheme.extendedColors
    var editingAge by remember { mutableStateOf(false) }
    var editingHeight by remember { mutableStateOf(false) }
    var editingGoal by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var confirmRemoveSlot by remember { mutableStateOf<ProfileSlot?>(null) }

    fun formatHeight(cm: Int): String = when (heightUnit) {
        HeightUnit.CM -> "$cm cm"
        HeightUnit.FT_IN -> {
            val (ft, inch) = cm.cmToFeetInches()
            "$ft' $inch\""
        }
    }

    fun formatGoal(lb: Int): String = when (weightUnit) {
        WeightUnit.LB -> "$lb lb"
        WeightUnit.KG -> "%.1f kg".format(lb.toDouble().lbToKg())
    }

    if (slots.isEmpty()) {
        EmptyProfilesState(onAddClick = { showAddDialog = true })
        if (showAddDialog) {
            AddProfileDialog(onDismiss = { showAddDialog = false }) {
                onAddProfile(it)
                showAddDialog = false
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            activeSlot?.let { slot ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            slot.name.take(1).uppercase(),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(slot.name, style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Slot ${slot.id} · ${formatHeight(slot.heightCm)} · ${slot.age} yrs",
                            fontSize = 13.sp,
                            color = extended.muted,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }

        item {
            SectionLabel("Scale user slot")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(slots) { slot ->
                    val active = activeSlot?.id == slot.id
                    Box {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(76.dp)
                                .background(
                                    if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.11f) else MaterialTheme.colorScheme.surface,
                                    RoundedCornerShape(16.dp),
                                )
                                .border(
                                    if (active) 1.5.dp else 1.dp,
                                    if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    RoundedCornerShape(16.dp),
                                )
                                .clickable { onSelectSlot(slot.id) }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (active) MaterialTheme.colorScheme.primary else extended.surface2),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    slot.name.take(1).uppercase(),
                                    color = if (active) MaterialTheme.colorScheme.onPrimary else extended.faint,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                slot.name,
                                fontSize = 12.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (active) MaterialTheme.colorScheme.onSurface else extended.muted,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(2.dp)
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(extended.faint)
                                .clickable { confirmRemoveSlot = slot },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("×", color = MaterialTheme.colorScheme.surface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                if (slots.size < MAX_PROFILE_SLOTS) {
                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(76.dp)
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                                .clickable { showAddDialog = true }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                        ) {
                            Box(
                                modifier = Modifier.size(38.dp).clip(CircleShape).background(extended.surface2),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("+", color = extended.muted, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("Add", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = extended.muted)
                        }
                    }
                }
            }
        }

        item {
            SectionLabel("About you")
            Card {
                activeSlot?.let { slot ->
                    ChevronRow("Age", "${slot.age} yrs") { editingAge = true }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    ChevronRow("Height", formatHeight(slot.heightCm)) { editingHeight = true }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Sex", fontSize = 15.sp, modifier = Modifier.padding(bottom = 11.dp))
                        SegmentedControl(
                            options = listOf(true, false),
                            selected = slot.isMale,
                            onSelect = { onUpdateProfile(slot.id, slot.age, slot.heightCm, it, slot.goalWeightLb) },
                            label = { if (it) "Male" else "Female" },
                        )
                    }
                }
            }
        }

        item {
            SectionLabel("Units & goal")
            Card {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Weight units", fontSize = 15.sp)
                    SegmentedControl(
                        options = WeightUnit.entries,
                        selected = weightUnit,
                        onSelect = onSetWeightUnit,
                        label = { if (it == WeightUnit.LB) "lb" else "kg" },
                        fillWidth = false,
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Height units", fontSize = 15.sp)
                    SegmentedControl(
                        options = HeightUnit.entries,
                        selected = heightUnit,
                        onSelect = onSetHeightUnit,
                        label = { if (it == HeightUnit.CM) "cm" else "ft/in" },
                        fillWidth = false,
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                activeSlot?.let { slot ->
                    ChevronRow("Goal weight", formatGoal(slot.goalWeightLb)) { editingGoal = true }
                }
            }
        }

        item {
            SectionLabel("Connected device")
            Card {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(15.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(extended.goodSoft),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(extended.good))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(deviceName ?: "Scale", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(
                            when (connectionState) {
                                ConnectionState.READY -> "Connected"
                                ConnectionState.DISCONNECTED -> "Disconnected"
                                else -> "Connecting…"
                            },
                            fontSize = 12.5.sp,
                            color = extended.muted,
                        )
                    }
                    Text(
                        "Manage",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.clickable(onClick = onManage),
                    )
                }
            }
        }

        item {
            SectionLabel("Appearance")
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Theme", fontSize = 15.sp, modifier = Modifier.padding(bottom = 11.dp))
                    SegmentedControl(
                        options = ThemeMode.entries,
                        selected = themeMode,
                        onSelect = onSetThemeMode,
                        label = {
                            when (it) {
                                ThemeMode.SYSTEM -> "System"
                                ThemeMode.LIGHT -> "Light"
                                ThemeMode.DARK -> "Dark"
                            }
                        },
                    )
                }
            }
        }

        item {
            SectionLabel("Background")
            Card {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 15.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto-connect in background", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        Text(
                            "Reconnects automatically when you step on the scale, even with the app closed",
                            fontSize = 12.sp,
                            color = extended.muted,
                        )
                    }
                    Switch(checked = backgroundEnabled, onCheckedChange = onSetBackgroundEnabled)
                }
            }
        }
    }

    activeSlot?.let { slot ->
        if (editingAge) {
            NumberEditDialog("Age", slot.age, onDismiss = { editingAge = false }) {
                onUpdateProfile(slot.id, it, slot.heightCm, slot.isMale, slot.goalWeightLb)
                editingAge = false
            }
        }
        if (editingHeight) {
            when (heightUnit) {
                HeightUnit.CM -> NumberEditDialog("Height (cm)", slot.heightCm, onDismiss = { editingHeight = false }) {
                    onUpdateProfile(slot.id, slot.age, it, slot.isMale, slot.goalWeightLb)
                    editingHeight = false
                }
                HeightUnit.FT_IN -> {
                    val (ft, inch) = slot.heightCm.cmToFeetInches()
                    FeetInchesEditDialog(ft, inch, onDismiss = { editingHeight = false }) { newFt, newInch ->
                        onUpdateProfile(slot.id, slot.age, feetInchesToCm(newFt, newInch), slot.isMale, slot.goalWeightLb)
                        editingHeight = false
                    }
                }
            }
        }
        if (editingGoal) {
            when (weightUnit) {
                WeightUnit.LB -> NumberEditDialog("Goal weight (lb)", slot.goalWeightLb, onDismiss = { editingGoal = false }) {
                    onUpdateProfile(slot.id, slot.age, slot.heightCm, slot.isMale, it)
                    editingGoal = false
                }
                WeightUnit.KG -> NumberEditDialog(
                    "Goal weight (kg)",
                    Math.round(slot.goalWeightLb.toDouble().lbToKg()).toInt(),
                    onDismiss = { editingGoal = false },
                ) {
                    onUpdateProfile(slot.id, slot.age, slot.heightCm, slot.isMale, Math.round(it.toDouble().kgToLb()).toInt())
                    editingGoal = false
                }
            }
        }
    }

    if (showAddDialog) {
        AddProfileDialog(onDismiss = { showAddDialog = false }) {
            onAddProfile(it)
            showAddDialog = false
        }
    }

    confirmRemoveSlot?.let { slot ->
        AlertDialog(
            onDismissRequest = { confirmRemoveSlot = null },
            title = { Text("Remove ${slot.name}?") },
            text = { Text("This deletes the profile from this device. Their weigh-in history stays saved.") },
            confirmButton = {
                TextButton(onClick = { onRemoveProfile(slot.id); confirmRemoveSlot = null }) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { confirmRemoveSlot = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun EmptyProfilesState(onAddClick: () -> Unit) {
    val extended = ScaleTheme.extendedColors
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("No profiles yet", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            "Add a profile with your age, height and sex so the scale can compute accurate body-composition metrics.",
            fontSize = 14.sp,
            color = extended.muted,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onAddClick) { Text("Add Profile") }
    }
}

@Composable
private fun AddProfileDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add profile") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                placeholder = { Text("Name") },
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim()) },
                enabled = name.isNotBlank(),
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun SectionLabel(text: String) {
    val extended = ScaleTheme.extendedColors
    Text(
        text = text.uppercase(),
        fontSize = 11.5.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = extended.faint,
        modifier = Modifier.padding(start = 4.dp, bottom = 9.dp),
    )
}

@Composable
private fun Card(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp)),
        content = content,
    )
}

@Composable
private fun ChevronRow(label: String, value: String, onClick: () -> Unit) {
    val extended = ScaleTheme.extendedColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 15.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(value, fontSize = 15.sp, color = extended.muted, fontWeight = FontWeight.SemiBold)
            Text("›", fontSize = 19.sp, color = extended.faint, modifier = Modifier.padding(start = 10.dp))
        }
    }
}

@Composable
private fun FeetInchesEditDialog(
    initialFeet: Int,
    initialInches: Int,
    onDismiss: () -> Unit,
    onConfirm: (feet: Int, inches: Int) -> Unit,
) {
    var feetText by remember { mutableStateOf(initialFeet.toString()) }
    var inchesText by remember { mutableStateOf(initialInches.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Height (ft/in)") },
        text = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = feetText,
                    onValueChange = { feetText = it.filter { c -> c.isDigit() } },
                    label = { Text("ft") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = inchesText,
                    onValueChange = { inchesText = it.filter { c -> c.isDigit() } },
                    label = { Text("in") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val ft = feetText.toIntOrNull()
                val inch = inchesText.toIntOrNull()
                if (ft != null && inch != null) onConfirm(ft, inch) else onDismiss()
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun NumberEditDialog(title: String, initial: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var text by remember { mutableStateOf(initial.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.filter { c -> c.isDigit() } },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { text.toIntOrNull()?.let(onConfirm) ?: onDismiss() }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
