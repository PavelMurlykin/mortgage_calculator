package com.pamurlykin.mortgagecalculator.data

import androidx.room.ColumnInfo
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
    val timestamp: Long = System.currentTimeMillis(),
    val discountAmount: Double = 0.0,
    val isMarkup: Boolean = false,
    val showDiscount: Boolean = false,
    // Cached calculated values for better performance
    val calculatedPayment: Double = 0.0,
    val finalPropertyValue: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val termMonths: Int = termYears * 12,
    @ColumnInfo(defaultValue = "1") val isTermYearsLocked: Boolean = true,
    @ColumnInfo(defaultValue = "0") val showGracePeriod: Boolean = false,
    @ColumnInfo(defaultValue = "0") val gracePeriodMonths: Int = 0,
    @ColumnInfo(defaultValue = "0.0") val graceInterestRate: Double = 0.0,
    @ColumnInfo(defaultValue = "1") val isGracePeriodYearsLocked: Boolean = true
)
