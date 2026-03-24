package com.pamurlykin.mortgagecalculator.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

enum class CalculationType {
    MONTHLY_PAYMENT, PROPERTY_VALUE
}

class SettingsManager(private val context: Context) {
    companion object {
        val STEP_CHANGE_AMOUNT = doublePreferencesKey("step_change")
        val DEFAULT_IS_ANNUITY = booleanPreferencesKey("default_is_annuity")
        
        val STEP_PERCENT = doublePreferencesKey("step_percent")
        val STEP_INTEREST_RATE = doublePreferencesKey("step_rate")
        val STEP_MONTHLY_PAYMENT = doublePreferencesKey("step_payment")

        val CALCULATION_TYPE_NAME = stringPreferencesKey("calculation_type")

        // Group expansion states
        val IS_CALCULATION_GROUP_EXPANDED = booleanPreferencesKey("is_calculation_group_expanded")
        val IS_MODIFIERS_GROUP_EXPANDED = booleanPreferencesKey("is_modifiers_group_expanded")
        val IS_ADDITIONAL_GROUP_EXPANDED = booleanPreferencesKey("is_additional_group_expanded")

        // Additional params
        val SHOW_DISCOUNT_OPTION = booleanPreferencesKey("show_discount_option")

        // Input persistence
        val PROPERTY_VALUE_AMOUNT = doublePreferencesKey("property_value")
        val DOWN_PAYMENT_AMOUNT = doublePreferencesKey("down_payment")
        val DOWN_PAYMENT_PERCENTAGE = doublePreferencesKey("down_payment_percent")
        val LOAN_TERM_YEARS = intPreferencesKey("term_years")
        val INTEREST_RATE_VALUE = doublePreferencesKey("interest_rate")
        val IS_ANNUITY_PAYMENT = booleanPreferencesKey("is_annuity")
        val IS_DOWN_PAYMENT_PERCENT_LOCKED = booleanPreferencesKey("is_down_payment_percent_locked")
        val MANUAL_MONTHLY_PAYMENT_AMOUNT = doublePreferencesKey("manual_monthly_payment")
        
        // Discount/Markup inputs persistence
        val DISCOUNT_AMOUNT = doublePreferencesKey("discount_amount")
        val IS_MARKUP = booleanPreferencesKey("is_markup")
        val IS_DISCOUNT_PERCENT_LOCKED = booleanPreferencesKey("is_discount_percent_locked")
    }

    val stepChangeAmount: Flow<Double> = context.settingsDataStore.data.map { preferences ->
        preferences[STEP_CHANGE_AMOUNT] ?: 100000.0
    }
    val defaultIsAnnuity: Flow<Boolean> = context.settingsDataStore.data.map { preferences ->
        preferences[DEFAULT_IS_ANNUITY] ?: true
    }
    
    val stepPercent: Flow<Double> = context.settingsDataStore.data.map { preferences ->
        preferences[STEP_PERCENT] ?: 0.1
    }
    val stepInterestRate: Flow<Double> = context.settingsDataStore.data.map { preferences ->
        preferences[STEP_INTEREST_RATE] ?: 0.1
    }
    val stepMonthlyPayment: Flow<Double> = context.settingsDataStore.data.map { preferences ->
        preferences[STEP_MONTHLY_PAYMENT] ?: 10000.0
    }

    val calculationType: Flow<CalculationType> = context.settingsDataStore.data.map { preferences ->
        val typeName = preferences[CALCULATION_TYPE_NAME] ?: CalculationType.MONTHLY_PAYMENT.name
        CalculationType.valueOf(typeName)
    }

    val isCalculationGroupExpanded: Flow<Boolean> = context.settingsDataStore.data.map { preferences ->
        preferences[IS_CALCULATION_GROUP_EXPANDED] ?: true
    }
    val isModifiersGroupExpanded: Flow<Boolean> = context.settingsDataStore.data.map { preferences ->
        preferences[IS_MODIFIERS_GROUP_EXPANDED] ?: true
    }
    val isAdditionalGroupExpanded: Flow<Boolean> = context.settingsDataStore.data.map { preferences ->
        preferences[IS_ADDITIONAL_GROUP_EXPANDED] ?: false
    }

    val showDiscountOption: Flow<Boolean> = context.settingsDataStore.data.map { preferences ->
        preferences[SHOW_DISCOUNT_OPTION] ?: false
    }

    // Input state flows
    val propertyValue: Flow<Double> = context.settingsDataStore.data.map { preferences ->
        preferences[PROPERTY_VALUE_AMOUNT] ?: 6600000.0
    }
    val downPayment: Flow<Double> = context.settingsDataStore.data.map { preferences ->
        preferences[DOWN_PAYMENT_AMOUNT] ?: 1320000.0
    }
    val downPaymentPercent: Flow<Double> = context.settingsDataStore.data.map { preferences ->
        preferences[DOWN_PAYMENT_PERCENTAGE] ?: 20.0
    }
    val termYears: Flow<Int> = context.settingsDataStore.data.map { preferences ->
        preferences[LOAN_TERM_YEARS] ?: 30
    }
    val interestRate: Flow<Double> = context.settingsDataStore.data.map { preferences ->
        preferences[INTEREST_RATE_VALUE] ?: 12.0
    }
    val isAnnuity: Flow<Boolean> = context.settingsDataStore.data.map { preferences ->
        preferences[IS_ANNUITY_PAYMENT] ?: true
    }
    val isDownPaymentPercentLocked: Flow<Boolean> = context.settingsDataStore.data.map { preferences ->
        preferences[IS_DOWN_PAYMENT_PERCENT_LOCKED] ?: false
    }
    val manualMonthlyPayment: Flow<Double> = context.settingsDataStore.data.map { preferences ->
        preferences[MANUAL_MONTHLY_PAYMENT_AMOUNT] ?: 50000.0
    }
    
    val discountAmount: Flow<Double> = context.settingsDataStore.data.map { preferences ->
        preferences[DISCOUNT_AMOUNT] ?: 0.0
    }
    val isMarkup: Flow<Boolean> = context.settingsDataStore.data.map { preferences ->
        preferences[IS_MARKUP] ?: false
    }
    val isDiscountPercentLocked: Flow<Boolean> = context.settingsDataStore.data.map { preferences ->
        preferences[IS_DISCOUNT_PERCENT_LOCKED] ?: false
    }

    suspend fun updateStepChangeAmount(step: Double) {
        context.settingsDataStore.edit { preferences ->
            preferences[STEP_CHANGE_AMOUNT] = step
        }
    }

    suspend fun updateDefaultPaymentType(isAnnuity: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[DEFAULT_IS_ANNUITY] = isAnnuity
        }
    }
    
    suspend fun updateStepPercent(step: Double) {
        context.settingsDataStore.edit { preferences ->
            preferences[STEP_PERCENT] = step
        }
    }

    suspend fun updateStepInterestRate(step: Double) {
        context.settingsDataStore.edit { preferences ->
            preferences[STEP_INTEREST_RATE] = step
        }
    }

    suspend fun updateStepMonthlyPayment(step: Double) {
        context.settingsDataStore.edit { preferences ->
            preferences[STEP_MONTHLY_PAYMENT] = step
        }
    }

    suspend fun updateCalculationType(type: CalculationType) {
        context.settingsDataStore.edit { preferences ->
            preferences[CALCULATION_TYPE_NAME] = type.name
        }
    }

    suspend fun updateCalculationGroupExpanded(expanded: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[IS_CALCULATION_GROUP_EXPANDED] = expanded
        }
    }

    suspend fun updateModifiersGroupExpanded(expanded: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[IS_MODIFIERS_GROUP_EXPANDED] = expanded
        }
    }

    suspend fun updateAdditionalGroupExpanded(expanded: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[IS_ADDITIONAL_GROUP_EXPANDED] = expanded
        }
    }

    suspend fun updateShowDiscountOption(show: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[SHOW_DISCOUNT_OPTION] = show
        }
    }

    suspend fun saveInputs(
        propertyValue: Double,
        downPayment: Double,
        downPaymentPercentage: Double,
        termYears: Int,
        interestRate: Double,
        isAnnuity: Boolean,
        percentLocked: Boolean,
        manualMonthlyPayment: Double,
        discountAmount: Double,
        isMarkup: Boolean,
        discountPercentLocked: Boolean
    ) {
        context.settingsDataStore.edit { preferences ->
            preferences[PROPERTY_VALUE_AMOUNT] = propertyValue
            preferences[DOWN_PAYMENT_AMOUNT] = downPayment
            preferences[DOWN_PAYMENT_PERCENTAGE] = downPaymentPercentage
            preferences[LOAN_TERM_YEARS] = termYears
            preferences[INTEREST_RATE_VALUE] = interestRate
            preferences[IS_ANNUITY_PAYMENT] = isAnnuity
            preferences[IS_DOWN_PAYMENT_PERCENT_LOCKED] = percentLocked
            preferences[MANUAL_MONTHLY_PAYMENT_AMOUNT] = manualMonthlyPayment
            preferences[DISCOUNT_AMOUNT] = discountAmount
            preferences[IS_MARKUP] = isMarkup
            preferences[IS_DISCOUNT_PERCENT_LOCKED] = discountPercentLocked
        }
    }
}
