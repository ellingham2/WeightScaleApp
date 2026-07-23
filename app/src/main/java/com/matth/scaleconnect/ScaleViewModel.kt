package com.matth.scaleconnect

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.matth.scaleconnect.ble.ConnectionState
import com.matth.scaleconnect.ble.LogEntry
import com.matth.scaleconnect.ble.ScaleBleManager
import com.matth.scaleconnect.ble.WeighInResult
import com.matth.scaleconnect.data.ProfileRepository
import com.matth.scaleconnect.data.ProfileSlot
import com.matth.scaleconnect.data.ScaleDatabase
import com.matth.scaleconnect.data.HeightUnit
import com.matth.scaleconnect.data.ThemeMode
import com.matth.scaleconnect.data.WeighInRecord
import com.matth.scaleconnect.data.WeightUnit
import com.matth.scaleconnect.service.ScaleConnectionService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class ScaleViewModel(application: Application) : AndroidViewModel(application) {

    // Shared with ScaleConnectionService - only one BLE connection ever exists.
    private val bleManager = ScaleBleManager.getInstance(application)
    private val db = ScaleDatabase.getInstance(application)
    private val profileRepo = ProfileRepository(application)

    val connectionState: StateFlow<ConnectionState> = bleManager.connectionState
    val deviceName: StateFlow<String?> = bleManager.deviceName
    val log: StateFlow<List<LogEntry>> = bleManager.log
    val reading: StateFlow<WeighInResult> = bleManager.reading

    val profileSlots: StateFlow<List<ProfileSlot>> = profileRepo.profileSlots
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val activeSlotId: StateFlow<Int> = profileRepo.activeSlotId
        .stateIn(viewModelScope, SharingStarted.Eagerly, 1)

    val activeSlot: StateFlow<ProfileSlot?> = combine(profileSlots, activeSlotId) { slots, id ->
        slots.find { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val history: StateFlow<List<WeighInRecord>> = activeSlotId
        .flatMapLatest { slotId -> db.weighInDao().getForSlot(slotId) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val backgroundEnabled: StateFlow<Boolean> = profileRepo.backgroundEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val themeMode: StateFlow<ThemeMode> = profileRepo.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    val weightUnit: StateFlow<WeightUnit> = profileRepo.weightUnit
        .stateIn(viewModelScope, SharingStarted.Eagerly, WeightUnit.LB)

    val heightUnit: StateFlow<HeightUnit> = profileRepo.heightUnit
        .stateIn(viewModelScope, SharingStarted.Eagerly, HeightUnit.CM)

    init {
        // One-time cleanup: earlier builds didn't delete weigh-in rows for removed
        // profiles, so purge any rows left over from a slot that's no longer in use.
        viewModelScope.launch {
            val createdIds = profileRepo.profileSlots.first().map { it.id }.toSet()
            db.weighInDao().getDistinctSlotIds()
                .filter { it !in createdIds }
                .forEach { db.weighInDao().deleteForSlot(it) }
        }
    }

    fun startScan() = bleManager.startScan()

    fun selectSlot(slotId: Int) {
        viewModelScope.launch { profileRepo.setActiveSlot(slotId) }
    }

    fun addProfile(name: String) {
        viewModelScope.launch { profileRepo.addProfile(name) }
    }

    fun removeProfile(slotId: Int) {
        viewModelScope.launch {
            profileRepo.removeProfile(slotId)
            db.weighInDao().deleteForSlot(slotId)
        }
    }

    fun updateProfile(slotId: Int, age: Int, heightCm: Int, isMale: Boolean, goalWeightLb: Int) {
        viewModelScope.launch { profileRepo.updateProfile(slotId, age, heightCm, isMale, goalWeightLb) }
    }

    fun syncActiveProfile() {
        val slot = activeSlot.value ?: return
        bleManager.sendProfileSync(
            userIndex = slot.id,
            isMale = slot.isMale,
            age = slot.age,
            heightCm = slot.heightCm
        )
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { profileRepo.setThemeMode(mode) }
    }

    fun setWeightUnit(unit: WeightUnit) {
        viewModelScope.launch { profileRepo.setWeightUnit(unit) }
    }

    fun setHeightUnit(unit: HeightUnit) {
        viewModelScope.launch { profileRepo.setHeightUnit(unit) }
    }

    fun setBackgroundEnabled(enabled: Boolean) {
        viewModelScope.launch { profileRepo.setBackgroundEnabled(enabled) }
        val context = getApplication<Application>()
        val intent = Intent(context, ScaleConnectionService::class.java)
        if (enabled) {
            ContextCompat.startForegroundService(context, intent)
        } else {
            context.stopService(intent)
        }
    }
}
