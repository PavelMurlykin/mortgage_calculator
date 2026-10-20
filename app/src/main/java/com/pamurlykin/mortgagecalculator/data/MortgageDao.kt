package com.pamurlykin.mortgagecalculator.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MortgageDao {
    @Query("SELECT * FROM calculations ORDER BY timestamp DESC")
    fun getAllCalculations(): Flow<List<MortgageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalculation(mortgageCalculation: MortgageEntity)

    @Query("UPDATE calculations SET title = :title, timestamp = :timestamp WHERE id = :id")
    suspend fun updateTitle(id: Int, title: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM calculations WHERE id = :calculationId")
    suspend fun deleteCalculation(calculationId: Int)
}
