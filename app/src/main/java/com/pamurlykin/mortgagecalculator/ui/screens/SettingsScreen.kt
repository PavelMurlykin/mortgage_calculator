package com.pamurlykin.mortgagecalculator.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pamurlykin.mortgagecalculator.data.CalculationType
import com.pamurlykin.mortgagecalculator.ui.MortgageViewModel

@Composable
fun SettingsScreen(mortgageViewModel: MortgageViewModel) {
    val stepChangeAmount by mortgageViewModel.stepChangeAmount.collectAsState()
    val defaultIsAnnuity by mortgageViewModel.defaultIsAnnuity.collectAsState()
    val stepPercent by mortgageViewModel.stepPercent.collectAsState()
    val stepInterestRate by mortgageViewModel.stepInterestRate.collectAsState()
    val stepMonthlyPayment by mortgageViewModel.stepMonthlyPayment.collectAsState()
    val calculationType by mortgageViewModel.calculationType.collectAsState()
    
    val isCalculationExpanded by mortgageViewModel.isCalculationGroupExpanded.collectAsState()
    val isModifiersExpanded by mortgageViewModel.isModifiersGroupExpanded.collectAsState()
    val isAdditionalExpanded by mortgageViewModel.isAdditionalGroupExpanded.collectAsState()

    val showDiscountOption by mortgageViewModel.showDiscountOption.collectAsState()

    var stepAmountText by remember(stepChangeAmount) { 
        mutableStateOf(String.format("%.0f", stepChangeAmount)) 
    }
    var stepPaymentText by remember(stepMonthlyPayment) { 
        mutableStateOf(String.format("%.0f", stepMonthlyPayment)) 
    }

    val calculationDescription = if (calculationType == CalculationType.MONTHLY_PAYMENT) {
        "Стандартный расчет ежемесячного платежа для ипотеченого кредита."
    } else {
        "Позволяет оценить максимальную стоимость объекта для ваших финансовых возможностей."
    }

    val paymentDescription = if (defaultIsAnnuity) {
        "Платеж остаётся неизменным до конца срока кредитования. И сумма на погашение тела кредита, и процентная часть всегда разные"
    } else {
        "Сумма платежа уменьшается к концу срока. Тело кредита гасится равными долями, а проценты начисляются на остаток долга"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Настройки",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Группа 1: Расчет
        SettingsGroup(
            title = "Расчет",
            expanded = isCalculationExpanded,
            onExpandChange = { mortgageViewModel.updateCalculationGroupExpanded(it) }
        ) {
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
                        onClick = { mortgageViewModel.updateCalculationType(CalculationType.MONTHLY_PAYMENT) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsOptionRow(
                        text = "Стоимость объекта",
                        selected = calculationType == CalculationType.PROPERTY_VALUE,
                        onClick = { mortgageViewModel.updateCalculationType(CalculationType.PROPERTY_VALUE) }
                    )
                }
            }
            Text(
                text = calculationDescription,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Payment Type
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
                        onClick = { mortgageViewModel.updateDefaultPaymentType(true) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsOptionRow(
                        text = "Дифференцированный",
                        selected = !defaultIsAnnuity,
                        onClick = { mortgageViewModel.updateDefaultPaymentType(false) }
                    )
                }
            }
            Text(
                text = paymentDescription,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Группа 2: Дополнительные параметры
        SettingsGroup(
            title = "Дополнительные параметры",
            expanded = isAdditionalExpanded,
            onExpandChange = { mortgageViewModel.updateAdditionalGroupExpanded(it) }
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Скидка/Удорожание",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Позволяет скорректировать стоимость объекта в меньшую (скидка) или большую (удорожание) сторону.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = showDiscountOption,
                            onCheckedChange = { mortgageViewModel.updateShowDiscountOption(it) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Группа 3: Модификаторы
        SettingsGroup(
            title = "Модификаторы",
            expanded = isModifiersExpanded,
            onExpandChange = { mortgageViewModel.updateModifiersGroupExpanded(it) }
        ) {
            // Step for Property/Downpayment
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Шаг изменения суммы",
                        fontSize = 16.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = "Относится к стоимости объекта и первоначальному взносу",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    OutlinedTextField(
                        value = stepAmountText,
                        onValueChange = { newValue ->
                            if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                                stepAmountText = newValue
                                newValue.toDoubleOrNull()?.let { amount ->
                                    mortgageViewModel.updateStepChangeAmount(amount)
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

            // Step for Monthly Payment
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Шаг изменения платежа",
                        fontSize = 16.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = "Относится к ежемесячному платежу для типа расчета \"Стоимость объекта\"",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    OutlinedTextField(
                        value = stepPaymentText,
                        onValueChange = { newValue ->
                            if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                                stepPaymentText = newValue
                                newValue.toDoubleOrNull()?.let { amount ->
                                    mortgageViewModel.updateStepMonthlyPayment(amount)
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
                title = "Шаг изменения взноса и ставки",
                currentStep = stepPercent,
                onStepSelected = { newStep ->
                    mortgageViewModel.updateStepPercent(newStep)
                    mortgageViewModel.updateStepInterestRate(newStep)
                }
            )
        }
    }
}

@Composable
fun SettingsGroup(
    title: String, 
    expanded: Boolean, 
    onExpandChange: (Boolean) -> Unit, 
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onExpandChange(!expanded) }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (expanded) "Свернуть" else "Развернуть",
                tint = MaterialTheme.colorScheme.primary
            )
        }
        
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column {
                content()
            }
        }
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
            val stepOptions = listOf(1.0, 0.1, 0.01)
            Row(Modifier.selectableGroup()) {
                stepOptions.forEach { option ->
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
                        Text(
                            text = String.format("%.2f%%", option), 
                            modifier = Modifier.padding(start = 4.dp), 
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsOptionRow(text: String, description: String? = null, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = text, fontSize = 16.sp)
            if (description != null) {
                Text(text = description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (selected) {
            Text("✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
    }
}
