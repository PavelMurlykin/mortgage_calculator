package com.pamurlykin.mortgagecalculator.data

import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GracePeriodMigrationTest {
    @Test
    fun migrationPreservesOldCalculationAndNewFieldsSurviveReopen() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "grace-period-migration-test"
        context.deleteDatabase(name)
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context).name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(4) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL("""
                            CREATE TABLE calculations (
                                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                propertyValue REAL NOT NULL, downPayment REAL NOT NULL,
                                termYears INTEGER NOT NULL, interestRate REAL NOT NULL,
                                isAnnuity INTEGER NOT NULL, title TEXT NOT NULL,
                                timestamp INTEGER NOT NULL, discountAmount REAL NOT NULL,
                                isMarkup INTEGER NOT NULL, showDiscount INTEGER NOT NULL,
                                calculatedPayment REAL NOT NULL, finalPropertyValue REAL NOT NULL
                            )
                        """.trimIndent())
                        db.execSQL("INSERT INTO calculations VALUES (1, 6600000, 1320000, 30, 12, 1, 'Existing', 1, 0, 0, 0, 50000, 6600000)")
                    }
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                }).build()
        )
        helper.writableDatabase
        helper.close()
        var database = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(AppDatabase.MIGRATION_4_5).build()
        try {
            val old = database.mortgageDao().getAllCalculations().first().single()
            assertEquals("Existing", old.title)
            assertEquals(360, old.termMonths)
            assertFalse(old.showGracePeriod)
            assertEquals(0, old.gracePeriodMonths)
            assertEquals(1_320_000.0, old.downPayment, 0.0)
            database.mortgageDao().insertCalculation(old.copy(
                id = 0, title = "Grace", termYears = 2, termMonths = 25,
                showGracePeriod = true, gracePeriodMonths = 13, graceInterestRate = 1.25,
                isTermYearsLocked = false, isGracePeriodYearsLocked = false, timestamp = 2
            ))
            database.close()
            database = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
            val saved = database.mortgageDao().getAllCalculations().first()
            assertEquals(2, saved.size)
            assertEquals(25, saved.first().termMonths)
            assertEquals(13, saved.first().gracePeriodMonths)
            assertEquals(1.25, saved.first().graceInterestRate, 0.0)
            assertTrue(saved.first().showGracePeriod)
            assertFalse(saved.first().isTermYearsLocked)
            assertFalse(saved.first().isGracePeriodYearsLocked)
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }
}
