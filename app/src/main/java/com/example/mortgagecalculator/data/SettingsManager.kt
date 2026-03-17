package com.example.mortgagecalculator.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

enum class CalculationType {
    MONTHLY_PAYMENT, PROPERTY_VALUE
}

class SettingsManager(private val context: Context) {
    companion object {
        val STEP_CHANGE = doublePreferencesKey("step_change")
        val DEFAULT_IS_ANNUITY = booleanPreferencesKey("default_is_annuity")
        
        val STEP_PERCENT = doublePreferencesKey("step_percent")
        val STEP_RATE = doublePreferencesKey("step_rate")
        val STEP_PAYMENT = doublePreferencesKey("step_payment")

        val CALCULATION_TYPE = stringPreferencesKey("calculation_type")

        // Input persistence
        val PROPERTY_VALUE = doublePreferencesKey("property_value")
        val DOWN_PAYMENT = doublePreferencesKey("down_payment")
        val DOWN_PAYMENT_PERCENT = doublePreferencesKey("down_payment_percent")
        val TERM_YEARS = intPreferencesKey("term_years")
        val INTEREST_RATE = doublePreferencesKey("interest_rate")
        val IS_ANNUITY = booleanPreferencesKey("is_annuity")
        val IS_DOWN_PAYMENT_PERCENT_LOCKED = booleanPreferencesKey("is_down_payment_percent_locked")
        val MANUAL_MONTHLY_PAYMENT = doublePreferencesKey("manual_monthly_payment")
    }

    val stepChange: Flow<Double> = context.dataStore.data.map { it[STEP_CHANGE] ?: 100000.0 }
    val defaultIsAnnuity: Flow<Boolean> = context.dataStore.data.map { it[DEFAULT_IS_ANNUITY] ?: true }
    
    val stepPercent: Flow<Double> = context.dataStore.data.map { it[STEP_PERCENT] ?: 0.1 }
    val stepRate: Flow<Double> = context.dataStore.data.map { it[STEP_RATE] ?: 0.1 }
    val stepPayment: Flow<Double> = context.dataStore.data.map { it[STEP_PAYMENT] ?: 10000.0 }

    val calculationType: Flow<CalculationType> = context.dataStore.data.map { 
        val name = it[CALCULATION_TYPE] ?: CalculationType.MONTHLY_PAYMENT.name
        CalculationType.valueOf(name)
    }

    // Input state flows
    val propertyValue: Flow<Double> = context.dataStore.data.map { it[PROPERTY_VALUE] ?: 6600000.0 }
    val downPayment: Flow<Double> = context.dataStore.data.map { it[DOWN_PAYMENT] ?: 1320000.0 }
    val downPaymentPercent: Flow<Double> = context.dataStore.data.map { it[DOWN_PAYMENT_PERCENT] ?: 20.0 }
    val termYears: Flow<Int> = context.dataStore.data.map { it[TERM_YEARS] ?: 30 }
    val interestRate: Flow<Double> = context.dataStore.data.map { it[INTEREST_RATE] ?: 12.0 }
    val isAnnuity: Flow<Boolean> = context.dataStore.data.map { it[IS_ANNUITY] ?: true }
    val isDownPaymentPercentLocked: Flow<Boolean> = context.dataStore.data.map { it[IS_DOWN_PAYMENT_PERCENT_LOCKED] ?: false }
    val manualMonthlyPayment: Flow<Double> = context.dataStore.data.map { it[MANUAL_MONTHLY_PAYMENT] ?: 50000.0 }

    suspend fun updateStepChange(step: Double) { context.dataStore.edit { it[STEP_CHANGE] = step } }
    suspend fun updateDefaultPaymentType(isAnnuity: Boolean) { context.dataStore.edit { it[DEFAULT_IS_ANNUITY] = isAnnuity } }
    
    suspend fun updateStepPercent(step: Double) { context.dataStore.edit { it[STEP_PERCENT] = step } }
    suspend fun updateStepRate(step: Double) { context.dataStore.edit { it[STEP_RATE] = step } }
    suspend fun updateStepPayment(step: Double) { context.dataStore.edit { it[STEP_PAYMENT] = step } }

    suspend fun updateCalculationType(type: CalculationType) {
        context.dataStore.edit { it[CALCULATION_TYPE] = type.name }
    }

    suspend fun saveInputs(property: Double, down: Double, downPercent: Double, term: Int, rate: Double, annuity: Boolean, percentLocked: Boolean, manualPayment: Double) {
        context.dataStore.edit {
            it[PROPERTY_VALUE] = property
            it[DOWN_PAYMENT] = down
            it[DOWN_PAYMENT_PERCENT] = downPercent
            it[TERM_YEARS] = term
            it[INTEREST_RATE] = rate
            it[IS_ANNUITY] = annuity
            it[IS_DOWN_PAYMENT_PERCENT_LOCKED] = percentLocked
            it[MANUAL_MONTHLY_PAYMENT] = manualPayment
        }
    }
}
