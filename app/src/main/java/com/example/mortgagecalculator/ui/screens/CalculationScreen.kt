package com.example.mortgagecalculator.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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

@Composable
fun CalculationScreen(viewModel: MortgageViewModel, navController: NavController) {
    val propertyValue by viewModel.propertyValue.collectAsState()
    val downPayment by viewModel.downPayment.collectAsState()
    val termYears by viewModel.termYears.collectAsState()
    val interestRate by viewModel.interestRate.collectAsState()
    val isAnnuity by viewModel.isAnnuity.collectAsState()

    val loanAmount by viewModel.loanAmount.collectAsState()
    val monthlyPayment by viewModel.monthlyPayment.collectAsState()
    val totalInterest by viewModel.totalInterest.collectAsState()
    val stepChange by viewModel.stepChange.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { viewModel.saveCalculation() }) {
                Text("Сохранить", color = MaterialTheme.colorScheme.primary)
            }
            Text(
                text = "Расчет кредита",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { /* Share action */ }) {
                Icon(androidx.compose.material.icons.Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

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
                            text = String.format("%,.0f", monthlyPayment),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text("Ежемесячный платеж", fontSize = 12.sp, color = Color.Gray)
                    }
                    TextButton(onClick = {
                        navController.navigate("schedule/${loanAmount}/${interestRate}/${termYears}/${isAnnuity}")
                    }) {
                        Text("График >", color = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                ResultRow("Сумма кредита", String.format("%,.0f", loanAmount))
                ResultRow("Сумма процентов", String.format("%,.0f", totalInterest))
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Тип платежа", fontSize = 14.sp, color = Color.Gray)
                    Text(if (isAnnuity) "Аннуитетный" else "Дифференцированный", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Input Fields
        InputCard(
            label = "Стоимость объекта",
            value = propertyValue,
            onValueChange = { viewModel.propertyValue.value = it },
            step = stepChange
        )
        
        InputCard(
            label = "Первоначальный взнос",
            value = downPayment,
            onValueChange = { viewModel.downPayment.value = it },
            step = stepChange,
            percentage = if (propertyValue > 0) (downPayment / propertyValue * 100) else 0.0
        )

        InputCard(
            label = "Срок (лет)",
            value = termYears.toDouble(),
            onValueChange = { viewModel.termYears.value = it.toInt().coerceIn(0, 30) },
            step = 1.0,
            range = 0.0..30.0
        )

        InputCard(
            label = "Процентная ставка",
            value = interestRate,
            onValueChange = { viewModel.interestRate.value = it },
            step = 0.01,
            suffix = "%"
        )
    }
}

@Composable
fun ResultRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 14.sp, color = Color.Gray)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun InputCard(
    label: String,
    value: Double,
    onValueChange: (Double) -> Unit,
    step: Double,
    percentage: Double? = null,
    suffix: String = "",
    range: ClosedFloatingPointRange<Double>? = null
) {
    var textValue by remember(value) { mutableStateOf(String.format("%.2f", value).replace(",", ".")) }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    BasicTextField(
                        value = textValue,
                        onValueChange = {
                            textValue = it
                            it.toDoubleOrNull()?.let { d -> onValueChange(d) }
                        },
                        textStyle = LocalTextStyle.current.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(label, fontSize = 12.sp, color = Color.Gray)
                        if (percentage != null) {
                            Text(String.format(", %.1f %%", percentage), fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onValueChange((value - step).let { if (range != null) it.coerceIn(range) else it.coerceAtLeast(0.0) }) }) {
                        Text("-", fontSize = 24.sp, color = Color.Gray)
                    }
                    IconButton(onClick = { onValueChange((value + step).let { if (range != null) it.coerceIn(range) else it }) }) {
                        Text("+", fontSize = 24.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun BasicTextField(
    value: String,
    onValueChange: (String) -> Unit,
    textStyle: androidx.compose.ui.text.TextStyle,
    keyboardOptions: KeyboardOptions,
    modifier: Modifier
) {
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = textStyle,
        keyboardOptions = keyboardOptions,
        modifier = modifier
    )
}
