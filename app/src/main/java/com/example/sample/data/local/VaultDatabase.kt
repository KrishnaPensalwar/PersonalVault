package com.example.sample.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.sample.storage.data.VaultDocumentDao
import com.example.sample.storage.data.VaultDocumentEntity

@Database(entities = [VaultDocumentEntity::class], version = 1, exportSchema = false)
abstract class VaultDatabase : RoomDatabase(){
    abstract fun vaultDocumentDao(): VaultDocumentDao

    companion object{
        @Volatile
        private var INSTANCE: VaultDatabase? = null

        fun getDatabase(context: Context): VaultDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    VaultDatabase::class.java,
                    "personal_vault_database"
                ).build().also { database ->
                    INSTANCE = database
                }
            }
        }
    }
}