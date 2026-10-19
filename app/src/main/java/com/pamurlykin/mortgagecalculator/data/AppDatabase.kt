package com.pamurlykin.mortgagecalculator.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [MortgageEntity::class], version = 5, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mortgageDao(): MortgageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE calculations ADD COLUMN title TEXT NOT NULL DEFAULT ''")
                } catch (e: Exception) {}
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE calculations ADD COLUMN discountAmount REAL NOT NULL DEFAULT 0.0")
                    db.execSQL("ALTER TABLE calculations ADD COLUMN isMarkup INTEGER NOT NULL DEFAULT 0")
                    db.execSQL("ALTER TABLE calculations ADD COLUMN showDiscount INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {}
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE calculations ADD COLUMN calculatedPayment REAL NOT NULL DEFAULT 0.0")
                    db.execSQL("ALTER TABLE calculations ADD COLUMN finalPropertyValue REAL NOT NULL DEFAULT 0.0")
                } catch (e: Exception) {}
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE calculations ADD COLUMN termMonths INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE calculations SET termMonths = termYears * 12")
                db.execSQL("ALTER TABLE calculations ADD COLUMN isTermYearsLocked INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE calculations ADD COLUMN showGracePeriod INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE calculations ADD COLUMN gracePeriodMonths INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE calculations ADD COLUMN graceInterestRate REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE calculations ADD COLUMN isGracePeriodYearsLocked INTEGER NOT NULL DEFAULT 1")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val databaseInstance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mortgage_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = databaseInstance
                databaseInstance
            }
        }
    }
}
