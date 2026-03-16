package com.example.mortgagecalculator.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {
    companion object {
        val STEP_CHANGE = doublePreferencesKey("step_change")
        val DEFAULT_IS_ANNUITY = booleanPreferencesKey("default_is_annuity")
    }

    val stepChange: Flow<Double> = context.dataStore.data.map { preferences ->
        preferences[STEP_CHANGE] ?: 100000.0
    }

    val defaultIsAnnuity: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[DEFAULT_IS_ANNUITY] ?: true
    }

    suspend fun updateStepChange(step: Double) {
        context.dataStore.edit { preferences ->
            preferences[STEP_CHANGE] = step
        }
    }

    suspend fun updateDefaultPaymentType(isAnnuity: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DEFAULT_IS_ANNUITY] = isAnnuity
        }
    }
}
