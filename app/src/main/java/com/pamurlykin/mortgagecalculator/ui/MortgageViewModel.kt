package com.pamurlykin.mortgagecalculator.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pamurlykin.mortgagecalculator.data.AppDatabase
import com.pamurlykin.mortgagecalculator.data.CalculationType
import com.pamurlykin.mortgagecalculator.data.MortgageEntity
import com.pamurlykin.mortgagecalculator.data.SettingsManager
import com.pamurlykin.mortgagecalculator.util.MortgageCalculator
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MortgageViewModel(application: Application) : AndroidViewModel(application) {
    private val appDatabase = AppDatabase.getDatabase(application)
    private val mortgageDao = appDatabase.mortgageDao()
    private val settingsManager = SettingsManager(application)

    // Saved Calculations
    val savedCalculations: StateFlow<List<MortgageEntity>> = mortgageDao.getAllCalculations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Settings
    val stepChangeAmount: StateFlow<Double> = settingsManager.stepChangeAmount.stateIn(viewModelScope, SharingStarted.Eagerly, 100000.0)
    val defaultIsAnnuity: StateFlow<Boolean> = settingsManager.defaultIsAnnuity.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val stepPercent: StateFlow<Double> = settingsManager.stepPercent.stateIn(viewModelScope, SharingStarted.Eagerly, 0.1)
    val stepInterestRate: StateFlow<Double> = settingsManager.stepInterestRate.stateIn(viewModelScope, SharingStarted.Eagerly, 0.1)
    val stepMonthlyPayment: StateFlow<Double> = settingsManager.stepMonthlyPayment.stateIn(viewModelScope, SharingStarted.Eagerly, 10000.0)
    val calculationType: StateFlow<CalculationType> = settingsManager.calculationType.stateIn(viewModelScope, SharingStarted.Eagerly, CalculationType.MONTHLY_PAYMENT)
    
    val isCalculationGroupExpanded: StateFlow<Boolean> = settingsManager.isCalculationGroupExpanded.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val isModifiersGroupExpanded: StateFlow<Boolean> = settingsManager.isModifiersGroupExpanded.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val isAdditionalGroupExpanded: StateFlow<Boolean> = settingsManager.isAdditionalGroupExpanded.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val showDiscountOption: StateFlow<Boolean> = settingsManager.showDiscountOption.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // Calculation Inputs
    val propertyValue = MutableStateFlow<Double>(6600000.0)
    val downPayment = MutableStateFlow<Double>(1320000.0)
    val downPaymentPercent = MutableStateFlow<Double>(20.0)
    val termMonths = MutableStateFlow<Int>(360)
    val termYears = MutableStateFlow<Int>(30)
    val interestRate = MutableStateFlow<Double>(12.0)
    val isAnnuity = MutableStateFlow<Boolean>(true)
    val isDownPaymentPercentLocked = MutableStateFlow<Boolean>(false)
    val isTermYearsLocked = MutableStateFlow<Boolean>(true)
    val manualMonthlyPayment = MutableStateFlow<Double>(50000.0)
    
    // Discount/Markup inputs
    val discountAmount = MutableStateFlow<Double>(0.0)
    val isMarkup = MutableStateFlow<Boolean>(false)
    val discountPercent = MutableStateFlow<Double>(0.0)
    val isDiscountPercentLocked = MutableStateFlow<Boolean>(false)

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
                listOf(propertyValue, downPayment, downPaymentPercent, termYears, interestRate, 
                isAnnuity, isDownPaymentPercentLocked, manualMonthlyPayment, 
                discountAmount, isMarkup, isDiscountPercentLocked)
            ) { args ->
                settingsManager.saveInputs(
                    args[0] as Double, args[1] as Double, args[2] as Double, args[3] as Int,
                    args[4] as Double, args[5] as Boolean, args[6] as Boolean, args[7] as Double,
                    args[8] as Double, args[9] as Boolean, args[10] as Boolean
                )
            }.collect()
        }
    }

    val finalPropertyValue: StateFlow<Double> = combine(
        propertyValue, 
        discountAmount, 
        isMarkup, 
        showDiscountOption, 
        calculationType
    ) { prop, disc, markup, show, type ->
        MortgageCalculator.calculateFinalPropertyValue(prop, disc, markup, show && type == CalculationType.MONTHLY_PAYMENT)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0.0)

    val calculatedMonthlyPayment: StateFlow<Double> = combine(
        finalPropertyValue, 
        downPayment, 
        termMonths, 
        interestRate, 
        isAnnuity
    ) { property, down, months, rate, annuity ->
        MortgageCalculator.calculateMonthlyPayment(property - down, rate, months, annuity)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0.0)

    val calculatedPropertyValue: StateFlow<Double> = combine(
        listOf(manualMonthlyPayment, interestRate, termMonths, isAnnuity, downPayment, downPaymentPercent, isDownPaymentPercentLocked)
    ) { args ->
        val payment = args[0] as Double
        val rate = args[1] as Double
        val months = args[2] as Int
        val annuity = args[3] as Boolean
        val down = args[4] as Double
        val percent = args[5] as Double
        val isPercentLocked = args[6] as Boolean
        
        val loanAmount = MortgageCalculator.calculateLoanAmount(payment, rate, months, annuity)
        if (isPercentLocked && percent < 100.0) {
            loanAmount / (1 - percent / 100.0)
        } else {
            loanAmount + down
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0.0)

    val currentLoanAmount: StateFlow<Double> = combine(
        listOf(propertyValue, downPayment, finalPropertyValue, calculationType, calculatedPropertyValue, manualMonthlyPayment, interestRate, termMonths, isAnnuity)
    ) { args ->
        val type = args[3] as CalculationType
        
        if (type == CalculationType.MONTHLY_PAYMENT) {
            val finalProp = args[2] as Double
            val down = args[1] as Double
            (finalProp - down).coerceAtLeast(0.0)
        } else {
            val payment = args[5] as Double
            val rate = args[6] as Double
            val months = args[7] as Int
            val annuity = args[8] as Boolean
            MortgageCalculator.calculateLoanAmount(payment, rate, months, annuity)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0.0)

    // Actions
    fun updatePropertyValue(newValue: Double) {
        val validatedValue = newValue.coerceAtLeast(1.0)
        val oldPropertyValue = propertyValue.value
        propertyValue.value = validatedValue
        
        if (isDownPaymentPercentLocked.value) {
            val percentage = if (oldPropertyValue > 0) downPayment.value / oldPropertyValue else 0.0
            downPayment.value = (validatedValue * percentage).coerceIn(0.0, validatedValue)
            downPaymentPercent.value = percentage * 100.0
        } else {
            if (downPayment.value > validatedValue) downPayment.value = validatedValue
            downPaymentPercent.value = if (validatedValue > 0) (downPayment.value / validatedValue * 100.0) else 0.0
        }

        if (isDiscountPercentLocked.value) {
            val percentage = if (oldPropertyValue > 0) discountAmount.value / oldPropertyValue else 0.0
            discountAmount.value = (validatedValue * percentage).coerceAtLeast(0.0)
            discountPercent.value = percentage * 100.0
        } else {
            discountPercent.value = if (validatedValue > 0) (discountAmount.value / validatedValue * 100.0) else 0.0
        }
    }

    fun updateDownPayment(newValue: Double) {
        downPayment.value = newValue.coerceAtLeast(0.0)
        if (calculationType.value == CalculationType.MONTHLY_PAYMENT) {
            val currentProp = finalPropertyValue.value
            if (currentProp > 0) downPaymentPercent.value = (downPayment.value / currentProp * 100.0)
        }
        isDownPaymentPercentLocked.value = false
    }

    fun updateDownPaymentPercent(newPercentage: Double) {
        val validatedPercentage = newPercentage.coerceIn(0.0, 100.0)
        downPaymentPercent.value = validatedPercentage
        if (calculationType.value == CalculationType.MONTHLY_PAYMENT) {
            downPayment.value = finalPropertyValue.value * (validatedPercentage / 100.0)
        }
        isDownPaymentPercentLocked.value = true
    }

    fun updateDiscountAmount(newValue: Double) {
        discountAmount.value = newValue.coerceAtLeast(0.0)
        if (propertyValue.value > 0) discountPercent.value = (discountAmount.value / propertyValue.value * 100.0)
        isDiscountPercentLocked.value = false
    }

    fun updateDiscountPercent(newPercentage: Double) {
        discountPercent.value = newPercentage.coerceAtLeast(0.0)
        discountAmount.value = propertyValue.value * (newPercentage / 100.0)
        isDiscountPercentLocked.value = true
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

    fun updateInterestRate(newRate: Double) { interestRate.value = newRate.coerceIn(0.0, 100.0) }
    fun updateManualMonthlyPayment(newAmount: Double) { manualMonthlyPayment.value = newAmount.coerceAtLeast(0.0) }
    fun updateCalculationType(newType: CalculationType) { viewModelScope.launch { settingsManager.updateCalculationType(newType) } }

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
    }

    fun saveCalculation() {
        viewModelScope.launch {
            mortgageDao.insertCalculation(
                MortgageEntity(
                    propertyValue = propertyValue.value,
                    downPayment = downPayment.value,
                    termYears = termYears.value,
                    interestRate = interestRate.value,
                    isAnnuity = isAnnuity.value,
                    title = "Сохраненный расчет",
                    discountAmount = discountAmount.value,
                    isMarkup = isMarkup.value,
                    showDiscount = showDiscountOption.value,
                    calculatedPayment = calculatedMonthlyPayment.value,
                    finalPropertyValue = finalPropertyValue.value
                )
            )
        }
    }

    fun updateCalculationTitle(id: Int, newTitle: String) { viewModelScope.launch { mortgageDao.updateTitle(id, newTitle) } }
    fun deleteSavedCalculation(calculationId: Int) { viewModelScope.launch { mortgageDao.deleteCalculation(calculationId) } }

    // Settings
    fun updateStepChangeAmount(newStep: Double) { viewModelScope.launch { settingsManager.updateStepChangeAmount(newStep) } }
    fun updateDefaultPaymentType(isAnnuity: Boolean) { viewModelScope.launch { settingsManager.updateDefaultPaymentType(isAnnuity) } }
    fun updateStepPercent(newStep: Double) { viewModelScope.launch { settingsManager.updateStepPercent(newStep) } }
    fun updateStepInterestRate(newStep: Double) { viewModelScope.launch { settingsManager.updateStepInterestRate(newStep) } }
    fun updateStepMonthlyPayment(newStep: Double) { viewModelScope.launch { settingsManager.updateStepMonthlyPayment(newStep) } }
    fun updateCalculationGroupExpanded(expanded: Boolean) { viewModelScope.launch { settingsManager.updateCalculationGroupExpanded(expanded) } }
    fun updateModifiersGroupExpanded(expanded: Boolean) { viewModelScope.launch { settingsManager.updateModifiersGroupExpanded(expanded) } }
    fun updateAdditionalGroupExpanded(expanded: Boolean) { viewModelScope.launch { settingsManager.updateAdditionalGroupExpanded(expanded) } }
    fun updateShowDiscountOption(show: Boolean) { viewModelScope.launch { settingsManager.updateShowDiscountOption(show) } }
}
