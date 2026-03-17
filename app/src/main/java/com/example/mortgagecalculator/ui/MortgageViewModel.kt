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

    // Saved Calculations
    val savedCalculations: StateFlow<List<MortgageEntity>> = dao.getAllCalculations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Settings
    val stepChange = settingsManager.stepChange.stateIn(viewModelScope, SharingStarted.Eagerly, 100000.0)
    val defaultIsAnnuity = settingsManager.defaultIsAnnuity.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val stepPercent = settingsManager.stepPercent.stateIn(viewModelScope, SharingStarted.Eagerly, 0.1)
    val stepRate = settingsManager.stepRate.stateIn(viewModelScope, SharingStarted.Eagerly, 0.1)

    // Calculation Inputs (Initialized with defaults, updated from DataStore in init)
    val propertyValue = MutableStateFlow(6600000.0)
    val downPayment = MutableStateFlow(1320000.0)
    val termYears = MutableStateFlow(30)
    val interestRate = MutableStateFlow(12.0)
    val isAnnuity = MutableStateFlow(true)
    val isDownPaymentPercentLocked = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            // Restore state
            settingsManager.propertyValue.first().let { propertyValue.value = it }
            settingsManager.downPayment.first().let { downPayment.value = it }
            settingsManager.termYears.first().let { termYears.value = it }
            settingsManager.interestRate.first().let { interestRate.value = it }
            settingsManager.isAnnuity.first().let { isAnnuity.value = it }
            settingsManager.isDownPaymentPercentLocked.first().let { isDownPaymentPercentLocked.value = it }
            
            // Auto-save changes using combine with array handling for 6+ flows
            combine(
                propertyValue, 
                downPayment, 
                termYears, 
                interestRate, 
                isAnnuity, 
                isDownPaymentPercentLocked
            ) { args ->
                val p = args[0] as Double
                val d = args[1] as Double
                val t = args[2] as Int
                val r = args[3] as Double
                val a = args[4] as Boolean
                val l = args[5] as Boolean
                settingsManager.saveInputs(p, d, t, r, a, l)
            }.collect()
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

    // Actions
    fun updatePropertyValue(value: Double) {
        val oldProp = propertyValue.value
        propertyValue.value = value
        if (isDownPaymentPercentLocked.value) {
            val percent = if (oldProp > 0) downPayment.value / oldProp else 0.0
            downPayment.value = value * percent
        }
    }

    fun updateDownPayment(value: Double) {
        downPayment.value = value
        isDownPaymentPercentLocked.value = false
    }

    fun updateDownPaymentPercent(percent: Double) {
        downPayment.value = propertyValue.value * (percent / 100)
        isDownPaymentPercentLocked.value = true
    }

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
        viewModelScope.launch { dao.deleteCalculation(id) }
    }

    // Settings
    fun updateStepChange(step: Double) { viewModelScope.launch { settingsManager.updateStepChange(step) } }
    fun updateDefaultPaymentType(annuity: Boolean) { viewModelScope.launch { settingsManager.updateDefaultPaymentType(annuity) } }
    fun updateStepPercent(step: Double) { viewModelScope.launch { settingsManager.updateStepPercent(step) } }
    fun updateStepRate(step: Double) { viewModelScope.launch { settingsManager.updateStepRate(step) } }
}
