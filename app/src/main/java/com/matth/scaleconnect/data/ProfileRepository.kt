package com.matth.scaleconnect.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.profileDataStore by preferencesDataStore(name = "profile_slots")

const val MAX_PROFILE_SLOTS = 4

class ProfileRepository(private val context: Context) {

    private fun createdKey(slot: Int) = booleanPreferencesKey("slot${slot}_created")
    private fun nameKey(slot: Int) = stringPreferencesKey("slot${slot}_name")
    private fun ageKey(slot: Int) = intPreferencesKey("slot${slot}_age")
    private fun heightKey(slot: Int) = intPreferencesKey("slot${slot}_height")
    private fun maleKey(slot: Int) = booleanPreferencesKey("slot${slot}_male")
    private fun goalKey(slot: Int) = intPreferencesKey("slot${slot}_goal")
    private val activeSlotKey = intPreferencesKey("active_slot_id")
    private val backgroundEnabledKey = booleanPreferencesKey("background_enabled")
    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val weightUnitKey = stringPreferencesKey("weight_unit")
    private val heightUnitKey = stringPreferencesKey("height_unit")

    /** Only slots the user has explicitly added - the app starts with none. */
    val profileSlots: Flow<List<ProfileSlot>> = context.profileDataStore.data.map { prefs ->
        (1..MAX_PROFILE_SLOTS).filter { prefs[createdKey(it)] == true }.map { slot ->
            ProfileSlot(
                id = slot,
                name = prefs[nameKey(slot)] ?: "Profile $slot",
                age = prefs[ageKey(slot)] ?: 30,
                heightCm = prefs[heightKey(slot)] ?: 170,
                isMale = prefs[maleKey(slot)] ?: true,
                goalWeightLb = prefs[goalKey(slot)] ?: 150,
            )
        }
    }

    /** 0 means "no active profile selected" (e.g. nothing has been added yet). */
    val activeSlotId: Flow<Int> = context.profileDataStore.data.map { prefs ->
        prefs[activeSlotKey] ?: 0
    }

    suspend fun setActiveSlot(slotId: Int) {
        context.profileDataStore.edit { it[activeSlotKey] = slotId }
    }

    /** Creates a profile in the next free slot (1..MAX_PROFILE_SLOTS). Returns the new
     * slot id, or null if all slots are already in use. Auto-selects it as active if
     * it's the first profile created. */
    suspend fun addProfile(name: String): Int? {
        var newSlotId: Int? = null
        context.profileDataStore.edit { prefs ->
            val freeSlot = (1..MAX_PROFILE_SLOTS).firstOrNull { prefs[createdKey(it)] != true }
                ?: return@edit
            prefs[createdKey(freeSlot)] = true
            prefs[nameKey(freeSlot)] = name
            prefs[ageKey(freeSlot)] = 30
            prefs[heightKey(freeSlot)] = 170
            prefs[maleKey(freeSlot)] = true
            prefs[goalKey(freeSlot)] = 150
            if ((prefs[activeSlotKey] ?: 0) == 0) {
                prefs[activeSlotKey] = freeSlot
            }
            newSlotId = freeSlot
        }
        return newSlotId
    }

    suspend fun removeProfile(slotId: Int) {
        context.profileDataStore.edit { prefs ->
            prefs[createdKey(slotId)] = false
            if (prefs[activeSlotKey] == slotId) {
                val remaining = (1..MAX_PROFILE_SLOTS).firstOrNull {
                    it != slotId && prefs[createdKey(it)] == true
                }
                prefs[activeSlotKey] = remaining ?: 0
            }
        }
    }

    suspend fun updateProfile(slotId: Int, age: Int, heightCm: Int, isMale: Boolean, goalWeightLb: Int) {
        context.profileDataStore.edit { prefs ->
            prefs[ageKey(slotId)] = age
            prefs[heightKey(slotId)] = heightCm
            prefs[maleKey(slotId)] = isMale
            prefs[goalKey(slotId)] = goalWeightLb
        }
    }

    val backgroundEnabled: Flow<Boolean> = context.profileDataStore.data.map { prefs ->
        prefs[backgroundEnabledKey] ?: true
    }

    suspend fun setBackgroundEnabled(enabled: Boolean) {
        context.profileDataStore.edit { it[backgroundEnabledKey] = enabled }
    }

    val themeMode: Flow<ThemeMode> = context.profileDataStore.data.map { prefs ->
        prefs[themeModeKey]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.profileDataStore.edit { it[themeModeKey] = mode.name }
    }

    val weightUnit: Flow<WeightUnit> = context.profileDataStore.data.map { prefs ->
        prefs[weightUnitKey]?.let { runCatching { WeightUnit.valueOf(it) }.getOrNull() } ?: WeightUnit.LB
    }

    suspend fun setWeightUnit(unit: WeightUnit) {
        context.profileDataStore.edit { it[weightUnitKey] = unit.name }
    }

    val heightUnit: Flow<HeightUnit> = context.profileDataStore.data.map { prefs ->
        prefs[heightUnitKey]?.let { runCatching { HeightUnit.valueOf(it) }.getOrNull() } ?: HeightUnit.CM
    }

    suspend fun setHeightUnit(unit: HeightUnit) {
        context.profileDataStore.edit { it[heightUnitKey] = unit.name }
    }
}
