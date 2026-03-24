package com.pamurlykin.mortgagecalculator.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pamurlykin.mortgagecalculator.data.AppDatabase
import com.pamurlykin.mortgagecalculator.data.CalculationType
import com.pamurlykin.mortgagecalculator.data.MortgageEntity
import com.pamurlykin.mortgagecalculator.data.SettingsManager
import com.pamurlykin.mortgagecalculator.ui.screens.formatYearsLabel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
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
    
    val isCalculationGroupExpanded = settingsManager.isCalculationGroupExpanded.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val isModifiersGroupExpanded = settingsManager.isModifiersGroupExpanded.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val isAdditionalGroupExpanded = settingsManager.isAdditionalGroupExpanded.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val showDiscountOption = settingsManager.showDiscountOption.stateIn(viewModelScope, SharingStarted.Eagerly, false)

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
    
    // Discount/Markup inputs
    val discountAmount = MutableStateFlow(0.0)
    val isMarkup = MutableStateFlow(false)
    val discountPercent = MutableStateFlow(0.0)
    val isDiscountPercentLocked = MutableStateFlow(false)

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
            
            settingsManager.discountAmount.first().let { value -> 
                discountAmount.value = value
                if (propertyValue.value > 0) discountPercent.value = (value / propertyValue.value * 100.0)
            }
            settingsManager.isMarkup.first().let { value -> isMarkup.value = value }
            settingsManager.isDiscountPercentLocked.first().let { value -> isDiscountPercentLocked.value = value }

            combine(
                propertyValue, 
                downPayment, 
                downPaymentPercent,
                termYears, 
                interestRate, 
                isAnnuity, 
                isDownPaymentPercentLocked,
                manualMonthlyPayment,
                discountAmount,
                isMarkup,
                isDiscountPercentLocked
            ) { arguments ->
                settingsManager.saveInputs(
                    arguments[0] as Double,
                    arguments[1] as Double,
                    arguments[2] as Double,
                    arguments[3] as Int,
                    arguments[4] as Double,
                    arguments[5] as Boolean,
                    arguments[6] as Boolean,
                    arguments[7] as Double,
                    arguments[8] as Double,
                    arguments[9] as Boolean,
                    arguments[10] as Boolean
                )
            }.collect()
        }
    }

    val finalPropertyValue = combine(propertyValue, discountAmount, isMarkup, showDiscountOption, calculationType) { prop, disc, markup, show, type ->
        if (!show || type != CalculationType.MONTHLY_PAYMENT) return@combine prop
        if (markup) prop + disc else (prop - disc).coerceAtLeast(0.0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0.0)

    // Calculation Outputs
    val calculatedMonthlyPayment = combine(finalPropertyValue, downPayment, termMonths, interestRate, isAnnuity) { property, down, months, rate, annuity ->
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
        
        val loanFromPayment = calculateLoanFromPayment(paymentAmount, monthsCount, annualRate, isAnnuityType)
        val propertyFromPayment = loanFromPayment + downPaymentAmount
        val propertyFromPercent = if (downPaymentPercentage > 0) (downPaymentAmount / (downPaymentPercentage / 100.0)) else Double.MAX_VALUE
        
        minOf(propertyFromPayment, propertyFromPercent).coerceAtLeast(downPaymentAmount)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0.0)

    val currentLoanAmount = combine(propertyValue, downPayment, calculatedPropertyValue, calculationType, finalPropertyValue) { property, down, calculatedProperty, type, finalProp ->
        val currentProperty = if (type == CalculationType.MONTHLY_PAYMENT) finalProp else calculatedProperty
        (currentProperty - down).coerceAtLeast(0.0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0.0)

    // Actions with Ranges
    fun updatePropertyValue(newValue: Double) {
        val validatedValue = newValue.coerceAtLeast(1.0)
        val oldPropertyValue = propertyValue.value
        propertyValue.value = validatedValue
        
        // Update down payment
        if (isDownPaymentPercentLocked.value) {
            val percentage = if (oldPropertyValue > 0) downPayment.value / oldPropertyValue else 0.0
            downPayment.value = (validatedValue * percentage).coerceIn(0.0, validatedValue)
            downPaymentPercent.value = percentage * 100.0
        } else {
            if (downPayment.value > validatedValue) downPayment.value = validatedValue
            downPaymentPercent.value = if (validatedValue > 0) (downPayment.value / validatedValue * 100.0) else 0.0
        }

        // Update discount
        if (isDiscountPercentLocked.value) {
            val percentage = if (oldPropertyValue > 0) discountAmount.value / oldPropertyValue else 0.0
            discountAmount.value = (validatedValue * percentage).coerceAtLeast(0.0)
            discountPercent.value = percentage * 100.0
        } else {
            discountPercent.value = if (validatedValue > 0) (discountAmount.value / validatedValue * 100.0) else 0.0
        }
    }

    fun updateDownPayment(newValue: Double) {
        if (calculationType.value == CalculationType.MONTHLY_PAYMENT) {
            downPayment.value = newValue.coerceIn(0.0, finalPropertyValue.value)
            downPaymentPercent.value = if (finalPropertyValue.value > 0) (downPayment.value / finalPropertyValue.value * 100.0) else 0.0
            isDownPaymentPercentLocked.value = false
        } else {
            downPayment.value = newValue.coerceAtLeast(0.0)
        }
    }

    fun updateDownPaymentPercent(newPercentage: Double) {
        val validatedPercentage = newPercentage.coerceIn(0.0, 100.0)
        if (calculationType.value == CalculationType.MONTHLY_PAYMENT) {
            downPayment.value = finalPropertyValue.value * (validatedPercentage / 100.0)
            downPaymentPercent.value = validatedPercentage
            isDownPaymentPercentLocked.value = true
        } else {
            downPaymentPercent.value = validatedPercentage
        }
    }

    fun updateDiscountAmount(newValue: Double) {
        discountAmount.value = newValue.coerceAtLeast(0.0)
        discountPercent.value = if (propertyValue.value > 0) (discountAmount.value / propertyValue.value * 100.0) else 0.0
        isDiscountPercentLocked.value = false
    }

    fun updateDiscountPercent(newPercentage: Double) {
        val validatedPercentage = newPercentage.coerceAtLeast(0.0)
        discountAmount.value = propertyValue.value * (validatedPercentage / 100.0)
        discountPercent.value = validatedPercentage
        isDiscountPercentLocked.value = true
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
        discountAmount.value = calculation.discountAmount
        isMarkup.value = calculation.isMarkup
        viewModelScope.launch { settingsManager.updateShowDiscountOption(calculation.showDiscount) }
        
        if (calculation.propertyValue > 0) {
            discountPercent.value = (calculation.discountAmount / calculation.propertyValue * 100.0)
        }
        
        val finalProp = if (calculation.showDiscount) {
            if (calculation.isMarkup) calculation.propertyValue + calculation.discountAmount 
            else (calculation.propertyValue - calculation.discountAmount).coerceAtLeast(0.0)
        } else calculation.propertyValue
        
        if (finalProp > 0) {
            downPaymentPercent.value = (calculation.downPayment / finalProp * 100.0)
        }
    }

    fun saveCalculation() {
        viewModelScope.launch {
            val isMonthlyType = calculationType.value == CalculationType.MONTHLY_PAYMENT
            val currentProperty = propertyValue.value
            
            mortgageDao.insertCalculation(
                MortgageEntity(
                    propertyValue = currentProperty,
                    downPayment = downPayment.value,
                    termYears = termYears.value,
                    interestRate = interestRate.value,
                    isAnnuity = isAnnuity.value,
                    title = "Сохраненный расчет",
                    discountAmount = discountAmount.value,
                    isMarkup = isMarkup.value,
                    showDiscount = showDiscountOption.value
                )
            )
        }
    }

    fun updateCalculationTitle(id: Int, newTitle: String) {
        viewModelScope.launch {
            mortgageDao.updateTitle(id, newTitle)
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
    fun updateCalculationGroupExpanded(expanded: Boolean) {
        viewModelScope.launch { settingsManager.updateCalculationGroupExpanded(expanded) }
    }
    fun updateModifiersGroupExpanded(expanded: Boolean) {
        viewModelScope.launch { settingsManager.updateModifiersGroupExpanded(expanded) }
    }
    fun updateAdditionalGroupExpanded(expanded: Boolean) {
        viewModelScope.launch { settingsManager.updateAdditionalGroupExpanded(expanded) }
    }
    fun updateShowDiscountOption(show: Boolean) {
        viewModelScope.launch { settingsManager.updateShowDiscountOption(show) }
    }
}
