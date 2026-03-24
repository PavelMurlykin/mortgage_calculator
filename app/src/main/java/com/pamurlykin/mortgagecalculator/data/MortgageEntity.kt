package com.pamurlykin.mortgagecalculator.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calculations")
data class MortgageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val propertyValue: Double,
    val downPayment: Double,
    val termYears: Int,
    val interestRate: Double,
    val isAnnuity: Boolean,
    val title: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
