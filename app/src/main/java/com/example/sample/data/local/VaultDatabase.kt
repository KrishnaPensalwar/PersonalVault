package com.example.sample.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.sample.family.data.FamilyMemberDao
import com.example.sample.family.data.FamilyMemberEntity
import com.example.sample.password.data.PasswordDao
import com.example.sample.password.data.PasswordEntity
import com.example.sample.storage.data.VaultDocumentDao
import com.example.sample.storage.data.VaultDocumentEntity

@Database(
    entities = [
        VaultDocumentEntity::class,
        PasswordEntity::class,
        FamilyMemberEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class VaultDatabase : RoomDatabase() {

    abstract fun vaultDocumentDao(): VaultDocumentDao
    abstract fun passwordDao(): PasswordDao
    abstract fun familyMemberDao(): FamilyMemberDao

    companion object {

        @Volatile
        private var INSTANCE: VaultDatabase? = null

        /**
         * Migration from version 1 → 2: adds the `passwords` table.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `passwords` (
                        `id` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `username` TEXT NOT NULL,
                        `password` TEXT NOT NULL,
                        `websiteUrl` TEXT NOT NULL,
                        `notes` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `isFavorite` INTEGER NOT NULL DEFAULT 0,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        /**
         * Migration from version 2 → 3:
         *  - Creates the `family_members` table.
         *  - Adds nullable `familyMemberId` column to `vault_documents`.
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `family_members` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `relationship` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "ALTER TABLE `vault_documents` ADD COLUMN `familyMemberId` TEXT DEFAULT NULL"
                )
            }
        }

        fun getDatabase(context: Context): VaultDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    VaultDatabase::class.java,
                    "personal_vault_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}