package com.matth.scaleconnect.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [WeighInRecord::class], version = 1, exportSchema = false)
abstract class ScaleDatabase : RoomDatabase() {
    abstract fun weighInDao(): WeighInDao

    companion object {
        @Volatile
        private var instance: ScaleDatabase? = null

        fun getInstance(context: Context): ScaleDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ScaleDatabase::class.java,
                    "scaleconnect.db"
                ).build().also { instance = it }
            }
    }
}
