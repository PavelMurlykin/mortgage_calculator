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
    val calculationType = settingsManager.calculationType.stateIn(viewModelScope, SharingStarted.Eagerly, CalculationType.MONTHLY_PAYMENT)

    // Calculation Inputs
    val propertyValue = MutableStateFlow(6600000.0)
    val downPayment = MutableStateFlow(1320000.0)
    val termYears = MutableStateFlow(30)
    val interestRate = MutableStateFlow(12.0)
    val isAnnuity = MutableStateFlow(true)
    val isDownPaymentPercentLocked = MutableStateFlow(false)
    val manualMonthlyPayment = MutableStateFlow(50000.0)

    init {
        viewModelScope.launch {
            settingsManager.propertyValue.first().let { propertyValue.value = it.coerceAtLeast(1.0) }
            settingsManager.downPayment.first().let { downPayment.value = it.coerceIn(0.0, propertyValue.value) }
            settingsManager.termYears.first().let { termYears.value = it.coerceIn(0, 30) }
            settingsManager.interestRate.first().let { interestRate.value = it.coerceIn(0.0, 100.0) }
            settingsManager.isAnnuity.first().let { isAnnuity.value = it }
            settingsManager.isDownPaymentPercentLocked.first().let { isDownPaymentPercentLocked.value = it }
            settingsManager.manualMonthlyPayment.first().let { manualMonthlyPayment.value = it.coerceAtLeast(0.0) }
            
            combine(
                propertyValue, 
                downPayment, 
                termYears, 
                interestRate, 
                isAnnuity, 
                isDownPaymentPercentLocked,
                manualMonthlyPayment
            ) { args ->
                val p = args[0] as Double
                val d = args[1] as Double
                val t = args[2] as Int
                val r = args[3] as Double
                val a = args[4] as Boolean
                val l = args[5] as Boolean
                val m = args[6] as Double
                settingsManager.saveInputs(p, d, t, r, a, l, m)
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

    val calculatedPropertyValue = combine(manualMonthlyPayment, termYears, interestRate, downPayment, isAnnuity) { payment, years, rate, down, annuity ->
        if (payment <= 0 || years <= 0) return@combine down
        val monthlyRate = rate / 100 / 12
        val months = years * 12
        
        val loan = if (annuity) {
            if (monthlyRate == 0.0) payment * months
            else payment * ((1 + monthlyRate).pow(months) - 1) / (monthlyRate * (1 + monthlyRate).pow(months))
        } else {
            // For differentiated, monthlyPayment = (Loan/n) + (Loan * r) = Loan * (1/n + r)
            // So Loan = monthlyPayment / (1/n + r)
            payment / (1.0 / months + monthlyRate)
        }
        (loan + down).coerceAtLeast(down)
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
        } else {
            if (downPayment.value > validatedValue) {
                downPayment.value = validatedValue
            }
        }
    }

    fun updateDownPayment(value: Double) {
        val currentProp = if (calculationType.value == CalculationType.MONTHLY_PAYMENT) propertyValue.value else calculatedPropertyValue.value
        downPayment.value = value.coerceIn(0.0, currentProp)
        isDownPaymentPercentLocked.value = false
    }

    fun updateDownPaymentPercent(percent: Double) {
        val validatedPercent = percent.coerceIn(0.0, 100.0)
        val currentProp = if (calculationType.value == CalculationType.MONTHLY_PAYMENT) propertyValue.value else calculatedPropertyValue.value
        downPayment.value = currentProp * (validatedPercent / 100.0)
        isDownPaymentPercentLocked.value = true
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
}
