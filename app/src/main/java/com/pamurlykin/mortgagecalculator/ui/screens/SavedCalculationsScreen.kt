package com.pamurlykin.mortgagecalculator.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.pamurlykin.mortgagecalculator.data.MortgageEntity
import com.pamurlykin.mortgagecalculator.ui.MortgageViewModel
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import kotlin.math.pow

@Composable
fun SavedCalculationsScreen(mortgageViewModel: MortgageViewModel, navController: NavController) {
    val savedCalculations by mortgageViewModel.savedCalculations.collectAsState()

    val formatSymbols = DecimalFormatSymbols(Locale.getDefault()).apply {
        groupingSeparator = ' '
    }
    val integerFormatter = DecimalFormat("#,###", formatSymbols)
    val rateFormatter = DecimalFormat("0.##", formatSymbols)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Список",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        if (savedCalculations.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Нет сохраненных расчетов",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(savedCalculations, key = { it.id }) { calculation ->
                    CalculationCard(
                        calculation = calculation,
                        integerFormatter = integerFormatter,
                        rateFormatter = rateFormatter,
                        onEdit = {
                            mortgageViewModel.loadCalculation(calculation)
                            navController.navigate("calculation") {
                                popUpTo("calculation") { inclusive = true }
                            }
                        },
                        onDelete = { mortgageViewModel.deleteSavedCalculation(calculation.id) },
                        onTitleChange = { newTitle ->
                            mortgageViewModel.updateCalculationTitle(calculation.id, newTitle)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CalculationCard(
    calculation: MortgageEntity,
    integerFormatter: DecimalFormat,
    rateFormatter: DecimalFormat,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTitleChange: (String) -> Unit
) {
    var isEditingTitle by remember { mutableStateOf(false) }
    var titleValue by remember(calculation.title) { 
        mutableStateOf(TextFieldValue(calculation.title, selection = TextRange(calculation.title.length))) 
    }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    val finalProp = if (calculation.showDiscount) {
        if (calculation.isMarkup) calculation.propertyValue + calculation.discountAmount 
        else (calculation.propertyValue - calculation.discountAmount).coerceAtLeast(0.0)
    } else calculation.propertyValue

    val monthlyPayment = calculateMonthlyPayment(
        propertyValue = finalProp,
        downPayment = calculation.downPayment,
        termYears = calculation.termYears,
        interestRate = calculation.interestRate,
        isAnnuity = calculation.isAnnuity
    )

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
                Box(modifier = Modifier.weight(1f)) {
                    if (isEditingTitle) {
                        BasicTextField(
                            value = titleValue,
                            onValueChange = { titleValue = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            textStyle = TextStyle(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                isEditingTitle = false
                                onTitleChange(titleValue.text)
                                focusManager.clearFocus()
                            })
                        )
                        LaunchedEffect(Unit) {
                            focusRequester.requestFocus()
                        }
                    } else {
                        Text(
                            text = calculation.title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isEditingTitle = true }
                        )
                    }
                }
                
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Редактировать",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Удалить",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            val labelFontSize = 15.sp
            val valueFontSize = 15.sp

            DetailRow("Ежемесячный платеж", "${integerFormatter.format(monthlyPayment)} ₽", labelFontSize, valueFontSize)
            DetailRow("Стоимость объекта", "${integerFormatter.format(finalProp)} ₽", labelFontSize, valueFontSize)
            DetailRow("Срок", "${calculation.termYears} ${formatYearsLabel(calculation.termYears)}", labelFontSize, valueFontSize)
            DetailRow("Процентная ставка", "${rateFormatter.format(calculation.interestRate)} %", labelFontSize, valueFontSize)
        }
    }
}

@Composable
fun DetailRow(label: String, value: String, labelSize: androidx.compose.ui.unit.TextUnit, valueSize: androidx.compose.ui.unit.TextUnit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Text(
            text = "$label: ",
            fontSize = labelSize,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = valueSize,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun calculateMonthlyPayment(
    propertyValue: Double,
    downPayment: Double,
    termYears: Int,
    interestRate: Double,
    isAnnuity: Boolean
): Double {
    val loanAmount = (propertyValue - downPayment).coerceAtLeast(0.0)
    val months = termYears * 12
    if (loanAmount <= 0 || months <= 0) return 0.0
    val monthlyRate = interestRate / 100 / 12
    return if (isAnnuity) {
        if (monthlyRate == 0.0) loanAmount / months
        else loanAmount * (monthlyRate * (1 + monthlyRate).pow(months)) / ((1 + monthlyRate).pow(months) - 1)
    } else {
        (loanAmount / months) + (loanAmount * monthlyRate)
    }
}
