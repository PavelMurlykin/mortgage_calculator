package com.example.mortgagecalculator.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mortgagecalculator.data.CalculationType
import com.example.mortgagecalculator.ui.MortgageViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MortgageViewModel) {
    val stepChange by viewModel.stepChange.collectAsState()
    val defaultIsAnnuity by viewModel.defaultIsAnnuity.collectAsState()
    val stepPercent by viewModel.stepPercent.collectAsState()
    val stepRate by viewModel.stepRate.collectAsState()
    val calculationType by viewModel.calculationType.collectAsState()
    
    var stepText by remember(stepChange) { mutableStateOf(String.format("%.0f", stepChange)) }

    val description = if (defaultIsAnnuity) {
        "Платеж остаётся неизменным до конца срока кредитования. И сумма на погашение тела кредита, и процентная часть всегда разные"
    } else {
        "Сумма платежа уменьшается к концу срока. Тело кредита гасится равными долями, а проценты начисляются на остаток долга"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Настройки",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Шаг изменения суммы",
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                OutlinedTextField(
                    value = stepText,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                            stepText = newValue
                            newValue.toDoubleOrNull()?.let {
                                viewModel.updateStepChange(it)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    suffix = { Text("₽") },
                    singleLine = true
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Step for Percent and Rate
        StepSelectionCard(
            title = "Шаг изменения взноса (%) и ставки (%)",
            currentStep = stepPercent,
            onStepSelected = { 
                viewModel.updateStepPercent(it)
                viewModel.updateStepRate(it)
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Calculation Type
        Text(
            text = "ТИП РАСЧЁТА",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.selectableGroup()) {
                SettingsOptionRow(
                    text = "Ежемесячный платеж",
                    selected = calculationType == CalculationType.MONTHLY_PAYMENT,
                    onClick = { viewModel.updateCalculationType(CalculationType.MONTHLY_PAYMENT) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                SettingsOptionRow(
                    text = "Стоимость объекта",
                    selected = calculationType == CalculationType.PROPERTY_VALUE,
                    onClick = { viewModel.updateCalculationType(CalculationType.PROPERTY_VALUE) }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "ТИП ПЛАТЕЖА ПО УМОЛЧАНИЮ",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.selectableGroup()) {
                SettingsOptionRow(
                    text = "Аннуитетный",
                    selected = defaultIsAnnuity,
                    onClick = { viewModel.updateDefaultPaymentType(true) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                SettingsOptionRow(
                    text = "Дифференцированный",
                    selected = !defaultIsAnnuity,
                    onClick = { viewModel.updateDefaultPaymentType(false) }
                )
            }
        }
        
        Text(
            text = description,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
fun StepSelectionCard(title: String, currentStep: Double, onStepSelected: (Double) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontSize = 16.sp, modifier = Modifier.padding(bottom = 8.dp))
            val options = listOf(1.0, 0.1, 0.01)
            Row(Modifier.selectableGroup()) {
                options.forEach { option ->
                    Row(
                        Modifier
                            .weight(1f)
                            .selectable(
                                selected = (currentStep == option),
                                onClick = { onStepSelected(option) },
                                role = Role.RadioButton
                            )
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = (currentStep == option), onClick = null)
                        Text(text = "$option%", modifier = Modifier.padding(start = 4.dp), fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsOptionRow(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = text, fontSize = 16.sp)
        if (selected) {
            Text("✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
    }
}
