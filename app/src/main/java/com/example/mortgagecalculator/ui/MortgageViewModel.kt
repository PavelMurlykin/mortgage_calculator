package com.example.mortgagecalculator.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mortgagecalculator.data.AppDatabase
import com.example.mortgagecalculator.data.MortgageEntity
import com.example.mortgagecalculator.data.SettingsManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.pow

class MortgageViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val dao = database.mortgageDao()
    private val settingsManager = SettingsManager(application)

    // Settings
    val stepChange = settingsManager.stepChange.stateIn(viewModelScope, SharingStarted.Eagerly, 100000.0)
    val defaultIsAnnuity = settingsManager.defaultIsAnnuity.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    // Calculation Inputs
    var propertyValue = MutableStateFlow(6600000.0)
    var downPayment = MutableStateFlow(1700000.0)
    var termYears = MutableStateFlow(30)
    var interestRate = MutableStateFlow(3.5)
    var isAnnuity = MutableStateFlow(true)

    init {
        viewModelScope.launch {
            defaultIsAnnuity.collect {
                isAnnuity.value = it
            }
        }
    }

    // Calculation Outputs
    val loanAmount = combine(propertyValue, downPayment) { prop, down ->
        (prop - down).coerceAtLeast(0.0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0.0)

    val monthlyPayment = combine(loanAmount, termYears, interestRate, isAnnuity) { loan, years, rate, annuity ->
        if (loan <= 0 || years <= 0) return@combine 0.0
        val monthlyRate = rate / 100 / 12
        val months = years * 12
        if (annuity) {
            if (monthlyRate == 0.0) loan / months
            else loan * (monthlyRate * (1 + monthlyRate).pow(months)) / ((1 + monthlyRate).pow(months) - 1)
        } else {
            // For differentiated, first payment is shown as reference or we can return a range/avg. 
            // Usually, first payment is (Loan / months) + (Loan * monthlyRate)
            (loan / months) + (loan * monthlyRate)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0.0)

    val totalInterest = combine(loanAmount, monthlyPayment, termYears, isAnnuity, interestRate) { loan, payment, years, annuity, rate ->
        if (loan <= 0 || years <= 0) return@combine 0.0
        val months = years * 12
        if (annuity) {
            (payment * months) - loan
        } else {
            var remaining = loan
            var interestSum = 0.0
            val monthlyRate = rate / 100 / 12
            val principalPart = loan / months
            for (i in 1..months) {
                interestSum += remaining * monthlyRate
                remaining -= principalPart
            }
            interestSum
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0.0)

    // Database Actions
    val savedCalculations = dao.getAllCalculations().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun saveCalculation() {
        viewModelScope.launch {
            dao.insertCalculation(
                MortgageEntity(
                    propertyValue = propertyValue.value,
                    downPayment = downPayment.value,
                    termYears = termYears.value,
                    interestRate = interestRate.value,
                    isAnnuity = isAnnuity.value
                )
            )
        }
    }

    fun deleteCalculation(id: Int) {
        viewModelScope.launch {
            dao.deleteCalculation(id)
        }
    }

    // Settings Update
    fun updateStepChange(step: Double) {
        viewModelScope.launch { settingsManager.updateStepChange(step) }
    }

    fun updateDefaultPaymentType(annuity: Boolean) {
        viewModelScope.launch { settingsManager.updateDefaultPaymentType(annuity) }
    }
}
