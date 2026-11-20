package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        MessageEntity::class,
        SeenPacketEntity::class,
        PendingRelayEntity::class,
        RelayLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ResilientDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao

    companion object {
        @Volatile
        private var INSTANCE: ResilientDatabase? = null

        fun getInstance(context: Context): ResilientDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ResilientDatabase::class.java,
                    "resilient_connect.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
