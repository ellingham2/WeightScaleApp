package com.matth.scaleconnect.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WeighInDao {
    @Insert
    suspend fun insert(record: WeighInRecord)

    @Query("SELECT * FROM weigh_ins WHERE slotId = :slotId ORDER BY timestampMillis DESC")
    fun getForSlot(slotId: Int): Flow<List<WeighInRecord>>

    @Query("SELECT * FROM weigh_ins WHERE slotId = :slotId ORDER BY timestampMillis DESC LIMIT 1")
    suspend fun getLatestForSlot(slotId: Int): WeighInRecord?

    @Query("DELETE FROM weigh_ins WHERE slotId = :slotId")
    suspend fun deleteForSlot(slotId: Int)

    @Query("SELECT DISTINCT slotId FROM weigh_ins")
    suspend fun getDistinctSlotIds(): List<Int>
}
