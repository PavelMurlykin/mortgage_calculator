package com.example.mortgagecalculator.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.mortgagecalculator.ui.MortgageViewModel
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculationScreen(viewModel: MortgageViewModel, navController: NavController) {
    val propertyValue by viewModel.propertyValue.collectAsState()
    val downPayment by viewModel.downPayment.collectAsState()
    val termYears by viewModel.termYears.collectAsState()
    val interestRate by viewModel.interestRate.collectAsState()
    val isAnnuity by viewModel.isAnnuity.collectAsState()
    val isPercentLocked by viewModel.isDownPaymentPercentLocked.collectAsState()

    val loanAmount by viewModel.loanAmount.collectAsState()
    val monthlyPayment by viewModel.monthlyPayment.collectAsState()
    val totalInterest by viewModel.totalInterest.collectAsState()
    
    val stepChange by viewModel.stepChange.collectAsState()
    val stepPercent by viewModel.stepPercent.collectAsState()
    val stepRate by viewModel.stepRate.collectAsState()

    val formatSymbols = DecimalFormatSymbols(Locale.getDefault()).apply {
        groupingSeparator = ' '
    }
    val formatter = DecimalFormat("#,###", formatSymbols)
    val decimalFormatter = DecimalFormat("#,##0.00", formatSymbols)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Title aligned with other screens
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Расчет",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { viewModel.saveCalculation() }) {
                Icon(Icons.Default.Save, contentDescription = "Сохранить", tint = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Result Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = decimalFormatter.format(monthlyPayment),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text("Ежемесячный платеж", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = {
                        navController.navigate("schedule/${loanAmount}/${interestRate}/${termYears}/${isAnnuity}")
                    }) {
                        Text("График >", color = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                ResultRow("Сумма кредита", formatter.format(loanAmount))
                ResultRow("Сумма процентов", formatter.format(totalInterest))
                
                // Payment type dropdown
                var expanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Тип платежа", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = { expanded = true }) {
                            Text(if (isAnnuity) "Аннуитетный" else "Дифференцированный")
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Аннуитетный") },
                            onClick = {
                                viewModel.isAnnuity.value = true
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Дифференцированный") },
                            onClick = {
                                viewModel.isAnnuity.value = false
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Input Fields
        InputCard(
            label = "Стоимость объекта",
            value = propertyValue,
            onValueChange = { viewModel.updatePropertyValue(it) },
            step = stepChange,
            isMoney = true
        )

        // Down Payment split block
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Первоначальный взнос", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                
                // Rubles
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NumericField(
                        value = downPayment,
                        onValueChange = { viewModel.updateDownPayment(it) },
                        modifier = Modifier.weight(1f),
                        isMoney = true
                    )
                    if (!isPercentLocked) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Row {
                        IconButton(onClick = { viewModel.updateDownPayment(downPayment - stepChange) }) { 
                            Text("-", fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) 
                        }
                        IconButton(onClick = { viewModel.updateDownPayment(downPayment + stepChange) }) { 
                            Text("+", fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) 
                        }
                    }
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.LightGray.copy(alpha = 0.2f))
                
                // Percentage
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val percent = if (propertyValue > 0) (downPayment / propertyValue * 100) else 0.0
                    NumericField(
                        value = percent,
                        onValueChange = { viewModel.updateDownPaymentPercent(it) },
                        modifier = Modifier.weight(1f),
                        suffix = "%"
                    )
                    if (isPercentLocked) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Row {
                        IconButton(onClick = { viewModel.updateDownPaymentPercent(percent - stepPercent) }) { 
                            Text("-", fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) 
                        }
                        IconButton(onClick = { viewModel.updateDownPaymentPercent(percent + stepPercent) }) { 
                            Text("+", fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) 
                        }
                    }
                }
            }
        }

        InputCard(
            label = "Срок (лет)",
            value = termYears.toDouble(),
            onValueChange = { viewModel.termYears.value = it.toInt().coerceIn(1, 30) },
            step = 1.0,
            range = 1.0..30.0,
            isInteger = true
        )

        InputCard(
            label = "Процентная ставка",
            value = interestRate,
            onValueChange = { viewModel.interestRate.value = it },
            step = stepRate,
            suffix = "%",
            allowEmpty = true
        )
    }
}

@Composable
fun ResultRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun InputCard(
    label: String,
    value: Double,
    onValueChange: (Double) -> Unit,
    step: Double,
    suffix: String = "",
    range: ClosedFloatingPointRange<Double>? = null,
    isMoney: Boolean = false,
    isInteger: Boolean = false,
    allowEmpty: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                NumericField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.weight(1f),
                    isMoney = isMoney,
                    isInteger = isInteger,
                    suffix = suffix,
                    allowEmpty = allowEmpty
                )
                Row {
                    IconButton(onClick = { 
                        val newVal = (value - step).let { if (range != null) it.coerceIn(range) else it.coerceAtLeast(0.0) }
                        onValueChange(newVal)
                    }) {
                        Text("-", fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { 
                        val newVal = (value + step).let { if (range != null) it.coerceIn(range) else it }
                        onValueChange(newVal)
                    }) {
                        Text("+", fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun NumericField(
    value: Double,
    onValueChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
    isMoney: Boolean = false,
    isInteger: Boolean = false,
    suffix: String = "",
    color: Color = MaterialTheme.colorScheme.onSurface,
    allowEmpty: Boolean = false
) {
    val symbols = DecimalFormatSymbols(Locale.getDefault()).apply { groupingSeparator = ' ' }
    val formatter = if (isMoney) DecimalFormat("#,###", symbols) else if (isInteger) DecimalFormat("#", symbols) else DecimalFormat("0.##", symbols)
    
    var textValue by remember(value) { mutableStateOf(if (value == 0.0 && allowEmpty) "" else formatter.format(value)) }

    androidx.compose.foundation.text.BasicTextField(
        value = textValue,
        onValueChange = { input ->
            val cleanInput = input.replace(" ", "").replace(",", ".")
            if (cleanInput.isEmpty()) {
                textValue = ""
                if (allowEmpty) onValueChange(0.0)
            } else if (cleanInput.toDoubleOrNull() != null) {
                textValue = input
                onValueChange(cleanInput.toDouble())
            }
        },
        textStyle = LocalTextStyle.current.copy(
            fontSize = 32.sp, 
            fontWeight = FontWeight.Bold,
            color = color
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
        decorationBox = { innerTextField ->
            Row {
                innerTextField()
                if (suffix.isNotEmpty()) {
                    Text(
                        text = suffix, 
                        fontSize = 32.sp, 
                        fontWeight = FontWeight.Bold, 
                        color = color,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }
    )
}
