package com.pamurlykin.mortgagecalculator.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pamurlykin.mortgagecalculator.data.AppDatabase
import com.pamurlykin.mortgagecalculator.data.CalculationType
import com.pamurlykin.mortgagecalculator.data.MortgageEntity
import com.pamurlykin.mortgagecalculator.data.SettingsManager
import com.pamurlykin.mortgagecalculator.util.MortgageCalculator
import com.pamurlykin.mortgagecalculator.util.MortgagePayments
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
    val showGracePeriodOption: StateFlow<Boolean> = settingsManager.showGracePeriodOption.stateIn(viewModelScope, SharingStarted.Eagerly, false)

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
    val gracePeriodMonths = MutableStateFlow(0)
    val gracePeriodYears: StateFlow<Int> = gracePeriodMonths.map { it / 12 }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)
    val graceInterestRate = MutableStateFlow(0.0)
    val isGracePeriodYearsLocked = MutableStateFlow(true)

    private val graceParameters = combine(showGracePeriodOption, gracePeriodMonths, graceInterestRate, termMonths) { show, months, rate, term ->
        Pair(if (show) months.coerceIn(0, term) else 0, rate)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, Pair(0, 0.0))
    
    // Discount/Markup inputs
    val discountAmount = MutableStateFlow<Double>(0.0)
    val isMarkup = MutableStateFlow<Boolean>(false)
    val discountPercent = MutableStateFlow<Double>(0.0)
    val isDiscountPercentLocked = MutableStateFlow<Boolean>(false)

    val finalPropertyValue: StateFlow<Double> = combine(
        propertyValue, 
        discountAmount, 
        isMarkup, 
        showDiscountOption, 
        calculationType
    ) { prop, disc, markup, show, type ->
        MortgageCalculator.calculateFinalPropertyValue(prop, disc, markup, show && type == CalculationType.MONTHLY_PAYMENT)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0.0)

    private val calculatedLoanAmount: StateFlow<Double> = combine(
        manualMonthlyPayment, interestRate, termMonths, isAnnuity, graceParameters
    ) { payment, rate, months, annuity, grace ->
        MortgageCalculator.calculateLoanAmount(payment, rate, months, annuity, grace.first, grace.second)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0.0)

    val calculatedPropertyValue: StateFlow<Double> = combine(
        calculatedLoanAmount, downPayment, downPaymentPercent, isDownPaymentPercentLocked
    ) { loanAmount, down, percent, isPercentLocked ->
        if (isPercentLocked && percent < 100.0) {
            loanAmount / (1 - percent / 100.0)
        } else {
            loanAmount + down
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0.0)

    val currentLoanAmount: StateFlow<Double> = combine(
        finalPropertyValue, downPayment, calculationType, calculatedLoanAmount
    ) { property, down, type, loanAmount ->
        if (type == CalculationType.MONTHLY_PAYMENT) {
            (property - down).coerceAtLeast(0.0)
        } else {
            loanAmount
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0.0)

    private val calculatedPayments: StateFlow<MortgagePayments> = combine(
        currentLoanAmount, interestRate, termMonths, isAnnuity, graceParameters
    ) { loanAmount, rate, months, annuity, grace ->
        MortgageCalculator.calculatePayments(loanAmount, rate, months, annuity, grace.first, grace.second)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), MortgagePayments(0.0))

    val calculatedMonthlyPayment: StateFlow<Double> = calculatedPayments.map { it.monthlyPayment }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0.0)
    val calculatedGraceMonthlyPayment: StateFlow<Double> = calculatedPayments.map { it.graceMonthlyPayment }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0.0)

    init {
        viewModelScope.launch {
            settingsManager.propertyValue.first().let { value -> propertyValue.value = value.coerceAtLeast(1.0) }
            settingsManager.downPayment.first().let { value -> downPayment.value = value.coerceAtLeast(0.0) }
            settingsManager.downPaymentPercent.first().let { value -> downPaymentPercent.value = value.coerceIn(0.0, 100.0) }
            settingsManager.termMonths.first().let { value ->
                termMonths.value = value.coerceIn(0, 360)
                termYears.value = termMonths.value / 12
            }
            isTermYearsLocked.value = settingsManager.isTermYearsLocked.first()
            gracePeriodMonths.value = settingsManager.gracePeriodMonths.first().coerceIn(0, termMonths.value)
            graceInterestRate.value = settingsManager.graceInterestRate.first().coerceIn(0.0, 100.0)
            isGracePeriodYearsLocked.value = settingsManager.isGracePeriodYearsLocked.first()
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

            // Sync down payment when final property value changes
            launch {
                finalPropertyValue.collect { newVal ->
                    if (calculationType.value == CalculationType.MONTHLY_PAYMENT) {
                        if (isDownPaymentPercentLocked.value) {
                            // Percent is locked, update RUB amount
                            val newAmount = newVal * (downPaymentPercent.value / 100.0)
                            if (kotlin.math.abs(downPayment.value - newAmount) > 0.01) {
                                downPayment.value = newAmount
                            }
                        } else {
                            // RUB amount is locked, update percent
                            if (newVal > 0) {
                                val newPercent = (downPayment.value / newVal * 100.0)
                                if (kotlin.math.abs(downPaymentPercent.value - newPercent) > 0.0001) {
                                    downPaymentPercent.value = newPercent
                                }
                            }
                        }
                    }
                }
            }

            combine(
                propertyValue, downPayment, downPaymentPercent, termYears, interestRate, 
                isAnnuity, isDownPaymentPercentLocked, manualMonthlyPayment, 
                discountAmount, isMarkup, isDiscountPercentLocked, termMonths, isTermYearsLocked,
                gracePeriodMonths, graceInterestRate, isGracePeriodYearsLocked
            ) { args ->
                settingsManager.saveInputs(
                    args[0] as Double,
                    args[1] as Double,
                    args[2] as Double,
                    args[3] as Int,
                    args[4] as Double,
                    args[5] as Boolean,
                    args[6] as Boolean,
                    args[7] as Double,
                    args[8] as Double,
                    args[9] as Boolean,
                    args[10] as Boolean,
                    args[11] as Int,
                    args[12] as Boolean,
                    args[13] as Int,
                    args[14] as Double,
                    args[15] as Boolean
                )
            }.collect()
        }
    }

    // Actions
    fun updatePropertyValue(newValue: Double) {
        val validatedValue = newValue.coerceAtLeast(1.0)
        propertyValue.value = validatedValue
        
        // Down payment and Discount logic is now handled in their respective sync collectors or in update actions
        // To maintain consistency, we trigger sync here if needed.
        // Actually, the finalPropertyValue collector handles Down Payment sync based on locks.
        
        // For Discount Amount/Percent sync when Property Value changes:
        if (isDiscountPercentLocked.value) {
            // Percent is locked, update discount amount
            discountAmount.value = validatedValue * (discountPercent.value / 100.0)
        } else {
            // Amount is locked, update discount percent
            if (validatedValue > 0) {
                discountPercent.value = (discountAmount.value / validatedValue * 100.0)
            }
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
        gracePeriodMonths.value = gracePeriodMonths.value.coerceAtMost(termMonths.value)
        isTermYearsLocked.value = true
    }

    fun updateTermMonths(newMonths: Int) {
        val validatedMonths = newMonths.coerceIn(0, 360)
        termMonths.value = validatedMonths
        termYears.value = validatedMonths / 12
        gracePeriodMonths.value = gracePeriodMonths.value.coerceAtMost(validatedMonths)
        isTermYearsLocked.value = false
    }

    fun updateInterestRate(newRate: Double) { interestRate.value = newRate.coerceIn(0.0, 100.0) }
    fun updateGracePeriodYears(newYears: Int) {
        gracePeriodMonths.value = (newYears.coerceIn(0, 30) * 12).coerceAtMost(termMonths.value)
        isGracePeriodYearsLocked.value = true
    }
    fun updateGracePeriodMonths(newMonths: Int) {
        gracePeriodMonths.value = newMonths.coerceIn(0, termMonths.value)
        isGracePeriodYearsLocked.value = false
    }
    fun updateGraceInterestRate(newRate: Double) { graceInterestRate.value = newRate.coerceIn(0.0, 100.0) }
    fun updateManualMonthlyPayment(newAmount: Double) { manualMonthlyPayment.value = newAmount.coerceAtLeast(0.0) }
    fun updateCalculationType(newType: CalculationType) { viewModelScope.launch { settingsManager.updateCalculationType(newType) } }

    fun loadCalculation(calculation: MortgageEntity) {
        propertyValue.value = calculation.propertyValue
        downPayment.value = calculation.downPayment
        val restoredPropertyValue = MortgageCalculator.calculateFinalPropertyValue(
            calculation.propertyValue, calculation.discountAmount, calculation.isMarkup, calculation.showDiscount
        )
        downPaymentPercent.value = if (restoredPropertyValue > 0) calculation.downPayment / restoredPropertyValue * 100 else 0.0
        isDownPaymentPercentLocked.value = false
        termMonths.value = calculation.termMonths.coerceIn(0, 360)
        termYears.value = termMonths.value / 12
        isTermYearsLocked.value = calculation.isTermYearsLocked
        gracePeriodMonths.value = calculation.gracePeriodMonths.coerceIn(0, termMonths.value)
        graceInterestRate.value = calculation.graceInterestRate.coerceIn(0.0, 100.0)
        isGracePeriodYearsLocked.value = calculation.isGracePeriodYearsLocked
        interestRate.value = calculation.interestRate
        isAnnuity.value = calculation.isAnnuity
        discountAmount.value = calculation.discountAmount
        discountPercent.value = if (calculation.propertyValue > 0) calculation.discountAmount / calculation.propertyValue * 100 else 0.0
        isDiscountPercentLocked.value = false
        isMarkup.value = calculation.isMarkup
        viewModelScope.launch {
            settingsManager.updateShowDiscountOption(calculation.showDiscount)
            settingsManager.updateShowGracePeriodOption(calculation.showGracePeriod)
            settingsManager.updateCalculationType(CalculationType.MONTHLY_PAYMENT)
        }
    }

    fun saveCalculation() {
        val finalValue = MortgageCalculator.calculateFinalPropertyValue(
            propertyValue.value, discountAmount.value, isMarkup.value, showDiscountOption.value
        )
        val payments = MortgageCalculator.calculatePayments(
            finalValue - downPayment.value, interestRate.value, termMonths.value, isAnnuity.value,
            if (showGracePeriodOption.value) gracePeriodMonths.value else 0, graceInterestRate.value
        )
        val calculation = MortgageEntity(
            propertyValue = propertyValue.value,
            downPayment = downPayment.value,
            termYears = termYears.value,
            termMonths = termMonths.value,
            isTermYearsLocked = isTermYearsLocked.value,
            interestRate = interestRate.value,
            isAnnuity = isAnnuity.value,
            title = "Сохраненный расчет",
            discountAmount = discountAmount.value,
            isMarkup = isMarkup.value,
            showDiscount = showDiscountOption.value,
            showGracePeriod = showGracePeriodOption.value,
            gracePeriodMonths = gracePeriodMonths.value,
            graceInterestRate = graceInterestRate.value,
            isGracePeriodYearsLocked = isGracePeriodYearsLocked.value,
            calculatedPayment = payments.monthlyPayment,
            finalPropertyValue = finalValue
        )
        viewModelScope.launch {
            mortgageDao.insertCalculation(calculation)
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
    fun updateShowGracePeriodOption(show: Boolean) { viewModelScope.launch { settingsManager.updateShowGracePeriodOption(show) } }
}
