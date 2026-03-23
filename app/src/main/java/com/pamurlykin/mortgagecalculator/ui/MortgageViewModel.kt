package com.pamurlykin.mortgagecalculator.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pamurlykin.mortgagecalculator.data.AppDatabase
import com.pamurlykin.mortgagecalculator.data.CalculationType
import com.pamurlykin.mortgagecalculator.data.MortgageEntity
import com.pamurlykin.mortgagecalculator.data.SettingsManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.pow

class MortgageViewModel(application: Application) : AndroidViewModel(application) {
    private val appDatabase = AppDatabase.getDatabase(application)
    private val mortgageDao = appDatabase.mortgageDao()
    private val settingsManager = SettingsManager(application)

    // Saved Calculations
    val savedCalculations: StateFlow<List<MortgageEntity>> = mortgageDao.getAllCalculations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Settings
    val stepChangeAmount = settingsManager.stepChangeAmount.stateIn(viewModelScope, SharingStarted.Eagerly, 100000.0)
    val defaultIsAnnuity = settingsManager.defaultIsAnnuity.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val stepPercent = settingsManager.stepPercent.stateIn(viewModelScope, SharingStarted.Eagerly, 0.1)
    val stepInterestRate = settingsManager.stepInterestRate.stateIn(viewModelScope, SharingStarted.Eagerly, 0.1)
    val stepMonthlyPayment = settingsManager.stepMonthlyPayment.stateIn(viewModelScope, SharingStarted.Eagerly, 10000.0)
    val calculationType = settingsManager.calculationType.stateIn(viewModelScope, SharingStarted.Eagerly, CalculationType.MONTHLY_PAYMENT)

    // Calculation Inputs
    val propertyValue = MutableStateFlow(6600000.0)
    val downPayment = MutableStateFlow(1320000.0)
    val downPaymentPercent = MutableStateFlow(20.0)
    val termMonths = MutableStateFlow(360)
    val termYears = MutableStateFlow(30)
    val interestRate = MutableStateFlow(12.0)
    val isAnnuity = MutableStateFlow(true)
    val isDownPaymentPercentLocked = MutableStateFlow(false)
    val isTermYearsLocked = MutableStateFlow(true)
    val manualMonthlyPayment = MutableStateFlow(50000.0)

    init {
        viewModelScope.launch {
            settingsManager.propertyValue.first().let { value -> propertyValue.value = value.coerceAtLeast(1.0) }
            settingsManager.downPayment.first().let { value -> downPayment.value = value.coerceAtLeast(0.0) }
            settingsManager.downPaymentPercent.first().let { value -> downPaymentPercent.value = value.coerceIn(0.0, 100.0) }
            settingsManager.termYears.first().let { value -> 
                termYears.value = value.coerceIn(0, 30)
                termMonths.value = value * 12
            }
            settingsManager.interestRate.first().let { value -> interestRate.value = value.coerceIn(0.0, 100.0) }
            settingsManager.isAnnuity.first().let { value -> isAnnuity.value = value }
            settingsManager.isDownPaymentPercentLocked.first().let { value -> isDownPaymentPercentLocked.value = value }
            settingsManager.manualMonthlyPayment.first().let { value -> manualMonthlyPayment.value = value.coerceAtLeast(0.0) }
            
            combine(
                propertyValue, 
                downPayment, 
                downPaymentPercent,
                termYears, 
                interestRate, 
                isAnnuity, 
                isDownPaymentPercentLocked,
                manualMonthlyPayment
            ) { arguments ->
                val currentPropertyValue = arguments[0] as Double
                val currentDownPayment = arguments[1] as Double
                val currentDownPaymentPercent = arguments[2] as Double
                val currentTermYears = arguments[3] as Int
                val currentInterestRate = arguments[4] as Double
                val currentIsAnnuity = arguments[5] as Boolean
                val currentPercentLocked = arguments[6] as Boolean
                val currentManualPayment = arguments[7] as Double
                
                settingsManager.saveInputs(
                    currentPropertyValue, 
                    currentDownPayment, 
                    currentDownPaymentPercent, 
                    currentTermYears, 
                    currentInterestRate, 
                    currentIsAnnuity, 
                    currentPercentLocked, 
                    currentManualPayment
                )
            }.collect()
        }
    }

    // Calculation Outputs
    val calculatedMonthlyPayment = combine(propertyValue, downPayment, termMonths, interestRate, isAnnuity) { property, down, months, rate, annuity ->
        val loanAmount = (property - down).coerceAtLeast(0.0)
        if (loanAmount <= 0 || months <= 0) return@combine 0.0
        val monthlyRate = rate / 100 / 12
        if (annuity) {
            if (monthlyRate == 0.0) loanAmount / months
            else loanAmount * (monthlyRate * (1 + monthlyRate).pow(months)) / ((1 + monthlyRate).pow(months) - 1)
        } else {
            (loanAmount / months) + (loanAmount * monthlyRate)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0.0)

    val calculatedPropertyValue = combine(
        manualMonthlyPayment, 
        termMonths, 
        interestRate, 
        downPayment, 
        downPaymentPercent, 
        isAnnuity
    ) { arguments ->
        val paymentAmount = arguments[0] as Double
        val monthsCount = arguments[1] as Int
        val annualRate = arguments[2] as Double
        val downPaymentAmount = arguments[3] as Double
        val downPaymentPercentage = arguments[4] as Double
        val isAnnuityType = arguments[5] as Boolean

        if (paymentAmount <= 0 || monthsCount <= 0) return@combine downPaymentAmount
        
        // 1. Max loan based on desired payment
        val loanFromPayment = calculateLoanFromPayment(paymentAmount, monthsCount, annualRate, isAnnuityType)
        val propertyFromPayment = loanFromPayment + downPaymentAmount
        
        // 2. Max property value based on minimum down payment percent requirement
        val propertyFromPercent = if (downPaymentPercentage > 0) (downPaymentAmount / (downPaymentPercentage / 100.0)) else Double.MAX_VALUE
        
        // Result is the smaller of the two constraints
        minOf(propertyFromPayment, propertyFromPercent).coerceAtLeast(downPaymentAmount)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0.0)

    val currentLoanAmount = combine(propertyValue, downPayment, calculatedPropertyValue, calculationType) { property, down, calculatedProperty, type ->
        val currentProperty = if (type == CalculationType.MONTHLY_PAYMENT) property else calculatedProperty
        (currentProperty - down).coerceAtLeast(0.0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0.0)

    // Actions with Ranges
    fun updatePropertyValue(newValue: Double) {
        val validatedValue = newValue.coerceAtLeast(1.0)
        val oldPropertyValue = propertyValue.value
        propertyValue.value = validatedValue
        
        if (isDownPaymentPercentLocked.value) {
            val percentage = if (oldPropertyValue > 0) downPayment.value / oldPropertyValue else 0.0
            downPayment.value = (validatedValue * percentage).coerceIn(0.0, validatedValue)
            downPaymentPercent.value = percentage * 100.0
        } else {
            if (downPayment.value > validatedValue) {
                downPayment.value = validatedValue
            }
            downPaymentPercent.value = if (validatedValue > 0) (downPayment.value / validatedValue * 100.0) else 0.0
        }
    }

    fun updateDownPayment(newValue: Double) {
        if (calculationType.value == CalculationType.MONTHLY_PAYMENT) {
            downPayment.value = newValue.coerceIn(0.0, propertyValue.value)
            downPaymentPercent.value = if (propertyValue.value > 0) (downPayment.value / propertyValue.value * 100.0) else 0.0
            isDownPaymentPercentLocked.value = false
        } else {
            downPayment.value = newValue.coerceAtLeast(0.0)
        }
    }

    fun updateDownPaymentPercent(newPercentage: Double) {
        val validatedPercentage = newPercentage.coerceIn(0.0, 100.0)
        if (calculationType.value == CalculationType.MONTHLY_PAYMENT) {
            downPayment.value = propertyValue.value * (validatedPercentage / 100.0)
            downPaymentPercent.value = validatedPercentage
            isDownPaymentPercentLocked.value = true
        } else {
            downPaymentPercent.value = validatedPercentage
        }
    }

    private fun calculateLoanFromPayment(monthlyPayment: Double, months: Int, annualRate: Double, isAnnuity: Boolean): Double {
        if (monthlyPayment <= 0 || months <= 0) return 0.0
        val monthlyRate = annualRate / 100 / 12
        return if (isAnnuity) {
            if (monthlyRate == 0.0) monthlyPayment * months
            else monthlyPayment * ((1 + monthlyRate).pow(months) - 1) / (monthlyRate * (1 + monthlyRate).pow(months))
        } else {
            monthlyPayment / (1.0 / months + monthlyRate)
        }
    }

    fun updateTermYears(newYears: Int) {
        val validatedYears = newYears.coerceIn(0, 30)
        termYears.value = validatedYears
        termMonths.value = validatedYears * 12
        isTermYearsLocked.value = true
    }

    fun updateTermMonths(newMonths: Int) {
        val validatedMonths = newMonths.coerceIn(0, 360)
        termMonths.value = validatedMonths
        termYears.value = validatedMonths / 12
        isTermYearsLocked.value = false
    }

    fun updateInterestRate(newRate: Double) {
        interestRate.value = newRate.coerceIn(0.0, 100.0)
    }

    fun updateManualMonthlyPayment(newAmount: Double) {
        manualMonthlyPayment.value = newAmount.coerceAtLeast(0.0)
    }

    fun updateCalculationType(newType: CalculationType) {
        if (newType == CalculationType.PROPERTY_VALUE && calculationType.value != newType) {
            updateDownPaymentPercent(20.0)
        }
        viewModelScope.launch { settingsManager.updateCalculationType(newType) }
    }

    fun loadCalculation(calculation: MortgageEntity) {
        propertyValue.value = calculation.propertyValue
        downPayment.value = calculation.downPayment
        termYears.value = calculation.termYears
        termMonths.value = calculation.termYears * 12
        interestRate.value = calculation.interestRate
        isAnnuity.value = calculation.isAnnuity
        if (calculation.propertyValue > 0) {
            downPaymentPercent.value = (calculation.downPayment / calculation.propertyValue * 100.0)
        }
    }

    fun saveCalculation() {
        viewModelScope.launch {
            val currentProperty = if (calculationType.value == CalculationType.MONTHLY_PAYMENT) propertyValue.value else calculatedPropertyValue.value
            mortgageDao.insertCalculation(
                MortgageEntity(
                    propertyValue = currentProperty,
                    downPayment = downPayment.value,
                    termYears = termYears.value,
                    interestRate = interestRate.value,
                    isAnnuity = isAnnuity.value
                )
            )
        }
    }

    fun deleteSavedCalculation(calculationId: Int) {
        viewModelScope.launch { mortgageDao.deleteCalculation(calculationId) }
    }

    // Settings
    fun updateStepChangeAmount(newStep: Double) { 
        viewModelScope.launch { settingsManager.updateStepChangeAmount(newStep) } 
    }
    
    fun updateDefaultPaymentType(isAnnuity: Boolean) { 
        viewModelScope.launch { settingsManager.updateDefaultPaymentType(isAnnuity) } 
    }
    
    fun updateStepPercent(newStep: Double) { 
        viewModelScope.launch { settingsManager.updateStepPercent(newStep) } 
    }
    
    fun updateStepInterestRate(newStep: Double) { 
        viewModelScope.launch { settingsManager.updateStepInterestRate(newStep) } 
    }
    
    fun updateStepMonthlyPayment(newStep: Double) { 
        viewModelScope.launch { settingsManager.updateStepMonthlyPayment(newStep) } 
    }
}
