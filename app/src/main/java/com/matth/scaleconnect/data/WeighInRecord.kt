package com.matth.scaleconnect.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weigh_ins")
data class WeighInRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val slotId: Int,
    val timestampMillis: Long,
    val weightLb: Double,
    val bodyFatPercent: Double,
    val hydrationPercent: Double,
    val musclePercent: Double,
    val bonePercent: Double,
    val bmrKcal: Int,
)
