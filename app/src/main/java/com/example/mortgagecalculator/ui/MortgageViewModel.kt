package com.example.mortgagecalculator.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mortgagecalculator.data.AppDatabase
import com.example.mortgagecalculator.data.CalculationType
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
    val stepPayment = settingsManager.stepPayment.stateIn(viewModelScope, SharingStarted.Eagerly, 10000.0)
    val calculationType = settingsManager.calculationType.stateIn(viewModelScope, SharingStarted.Eagerly, CalculationType.MONTHLY_PAYMENT)

    // Calculation Inputs
    val propertyValue = MutableStateFlow(6600000.0)
    val downPayment = MutableStateFlow(1320000.0)
    val downPaymentPercent = MutableStateFlow(20.0)
    val termYears = MutableStateFlow(30)
    val interestRate = MutableStateFlow(12.0)
    val isAnnuity = MutableStateFlow(true)
    val isDownPaymentPercentLocked = MutableStateFlow(false)
    val manualMonthlyPayment = MutableStateFlow(50000.0)

    init {
        viewModelScope.launch {
            settingsManager.propertyValue.first().let { propertyValue.value = it.coerceAtLeast(1.0) }
            settingsManager.downPayment.first().let { downPayment.value = it.coerceAtLeast(0.0) }
            settingsManager.downPaymentPercent.first().let { downPaymentPercent.value = it.coerceIn(0.0, 100.0) }
            settingsManager.termYears.first().let { termYears.value = it.coerceIn(0, 30) }
            settingsManager.interestRate.first().let { interestRate.value = it.coerceIn(0.0, 100.0) }
            settingsManager.isAnnuity.first().let { isAnnuity.value = it }
            settingsManager.isDownPaymentPercentLocked.first().let { isDownPaymentPercentLocked.value = it }
            settingsManager.manualMonthlyPayment.first().let { manualMonthlyPayment.value = it.coerceAtLeast(0.0) }
            
            combine(
                propertyValue, 
                downPayment, 
                downPaymentPercent,
                termYears, 
                interestRate, 
                isAnnuity, 
                isDownPaymentPercentLocked,
                manualMonthlyPayment
            ) { args ->
                val p = args[0] as Double
                val d = args[1] as Double
                val dp = args[2] as Double
                val t = args[3] as Int
                val r = args[4] as Double
                val a = args[5] as Boolean
                val l = args[6] as Boolean
                val m = args[7] as Double
                settingsManager.saveInputs(p, d, dp, t, r, a, l, m)
            }.collect()
        }
    }

    // Calculation Outputs
    val calculatedMonthlyPayment = combine(propertyValue, downPayment, termYears, interestRate, isAnnuity) { prop, down, years, rate, annuity ->
        val loan = (prop - down).coerceAtLeast(0.0)
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

    val calculatedPropertyValue = combine(
        manualMonthlyPayment, 
        termYears, 
        interestRate, 
        downPayment, 
        downPaymentPercent, 
        isAnnuity
    ) { args ->
        val payment = args[0] as Double
        val years = args[1] as Int
        val rate = args[2] as Double
        val down = args[3] as Double
        val downPct = args[4] as Double
        val annuity = args[5] as Boolean

        if (payment <= 0 || years <= 0) return@combine down
        
        // 1. Max loan based on desired payment
        val loanFromPayment = getLoanFromPayment(payment, years, rate, annuity)
        val propFromPayment = loanFromPayment + down
        
        // 2. Max property value based on minimum down payment percent requirement
        // Property = Down / (Pct / 100)
        val propFromPercent = if (downPct > 0) (down / (downPct / 100.0)) else Double.MAX_VALUE
        
        // Result is the smaller of the two constraints
        minOf(propFromPayment, propFromPercent).coerceAtLeast(down)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0.0)

    val loanAmount = combine(propertyValue, downPayment, calculatedPropertyValue, calculationType) { prop, down, calcProp, type ->
        val currentProp = if (type == CalculationType.MONTHLY_PAYMENT) prop else calcProp
        (currentProp - down).coerceAtLeast(0.0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0.0)

    // Actions with Ranges
    fun updatePropertyValue(value: Double) {
        val validatedValue = value.coerceAtLeast(1.0)
        val oldProp = propertyValue.value
        propertyValue.value = validatedValue
        
        if (isDownPaymentPercentLocked.value) {
            val percent = if (oldProp > 0) downPayment.value / oldProp else 0.0
            downPayment.value = (validatedValue * percent).coerceIn(0.0, validatedValue)
            downPaymentPercent.value = percent * 100.0
        } else {
            if (downPayment.value > validatedValue) {
                downPayment.value = validatedValue
            }
            downPaymentPercent.value = if (validatedValue > 0) (downPayment.value / validatedValue * 100.0) else 0.0
        }
    }

    fun updateDownPayment(value: Double) {
        if (calculationType.value == CalculationType.MONTHLY_PAYMENT) {
            downPayment.value = value.coerceIn(0.0, propertyValue.value)
            downPaymentPercent.value = if (propertyValue.value > 0) (downPayment.value / propertyValue.value * 100.0) else 0.0
            isDownPaymentPercentLocked.value = false
        } else {
            // "Property Value" mode: down payment is an independent input
            downPayment.value = value.coerceAtLeast(0.0)
        }
    }

    fun updateDownPaymentPercent(percent: Double) {
        val validatedPercent = percent.coerceIn(0.0, 100.0)
        if (calculationType.value == CalculationType.MONTHLY_PAYMENT) {
            downPayment.value = propertyValue.value * (validatedPercent / 100.0)
            downPaymentPercent.value = validatedPercent
            isDownPaymentPercentLocked.value = true
        } else {
            // "Property Value" mode: percent is an independent input (min down payment constraint)
            downPaymentPercent.value = validatedPercent
        }
    }

    private fun getLoanFromPayment(payment: Double, years: Int, rate: Double, annuity: Boolean): Double {
        if (payment <= 0 || years <= 0) return 0.0
        val monthlyRate = rate / 100 / 12
        val months = years * 12
        return if (annuity) {
            if (monthlyRate == 0.0) payment * months
            else payment * ((1 + monthlyRate).pow(months) - 1) / (monthlyRate * (1 + monthlyRate).pow(months))
        } else {
            payment / (1.0 / months + monthlyRate)
        }
    }

    fun updateTermYears(years: Int) {
        termYears.value = years.coerceIn(0, 30)
    }

    fun updateInterestRate(rate: Double) {
        interestRate.value = rate.coerceIn(0.0, 100.0)
    }

    fun updateManualMonthlyPayment(value: Double) {
        manualMonthlyPayment.value = value.coerceAtLeast(0.0)
    }

    fun updateCalculationType(type: CalculationType) {
        if (type == CalculationType.PROPERTY_VALUE && calculationType.value != type) {
            // Requirement: When switching to "Property Value", set down payment to 20%
            updateDownPaymentPercent(20.0)
        }
        viewModelScope.launch { settingsManager.updateCalculationType(type) }
    }

    fun saveCalculation() {
        viewModelScope.launch {
            val currentProp = if (calculationType.value == CalculationType.MONTHLY_PAYMENT) propertyValue.value else calculatedPropertyValue.value
            dao.insertCalculation(
                MortgageEntity(
                    propertyValue = currentProp,
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
    fun updateStepPayment(step: Double) { viewModelScope.launch { settingsManager.updateStepPayment(step) } }
}
