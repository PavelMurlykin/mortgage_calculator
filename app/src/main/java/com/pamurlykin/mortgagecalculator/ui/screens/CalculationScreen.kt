package com.pamurlykin.mortgagecalculator.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.pamurlykin.mortgagecalculator.data.CalculationType
import com.pamurlykin.mortgagecalculator.ui.MortgageViewModel
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

fun formatYearsLabel(years: Int): String {
    val mod10 = years % 10
    val mod100 = years % 100
    return when {
        mod10 == 1 && mod100 != 11 -> "год"
        mod10 in 2..4 && (mod100 < 10 || mod100 >= 20) -> "года"
        else -> "лет"
    }
}

class SuffixTransformation(private val suffix: String) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val transformedText = text + AnnotatedString(suffix)
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = offset
            override fun transformedToOriginal(offset: Int): Int {
                if (offset > text.length) return text.length
                return offset
            }
        }
        return TransformedText(transformedText, offsetMapping)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculationScreen(mortgageViewModel: MortgageViewModel, navController: NavController) {
    val propertyValue by mortgageViewModel.propertyValue.collectAsState()
    val downPayment by mortgageViewModel.downPayment.collectAsState()
    val downPaymentPercent by mortgageViewModel.downPaymentPercent.collectAsState()
    val termYears by mortgageViewModel.termYears.collectAsState()
    val interestRate by mortgageViewModel.interestRate.collectAsState()
    val isAnnuity by mortgageViewModel.isAnnuity.collectAsState()
    val isPercentLocked by mortgageViewModel.isDownPaymentPercentLocked.collectAsState()
    val manualMonthlyPayment by mortgageViewModel.manualMonthlyPayment.collectAsState()
    val calculationType by mortgageViewModel.calculationType.collectAsState()

    val calculatedMonthlyPayment by mortgageViewModel.calculatedMonthlyPayment.collectAsState()
    val calculatedPropertyValue by mortgageViewModel.calculatedPropertyValue.collectAsState()
    val currentLoanAmount by mortgageViewModel.currentLoanAmount.collectAsState()
    
    val stepChangeAmount by mortgageViewModel.stepChangeAmount.collectAsState()
    val stepPercent by mortgageViewModel.stepPercent.collectAsState()
    val stepInterestRate by mortgageViewModel.stepInterestRate.collectAsState()
    val stepMonthlyPayment by mortgageViewModel.stepMonthlyPayment.collectAsState()

    val formatSymbols = DecimalFormatSymbols(Locale.getDefault()).apply {
        groupingSeparator = ' '
    }
    val integerFormatter = DecimalFormat("#,###", formatSymbols)
    val decimalFormatter = DecimalFormat("#,##0.00", formatSymbols)

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
            Text(text = "Расчет", fontSize = 32.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = { mortgageViewModel.saveCalculation() }) {
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
                        val isMonthlyType = calculationType == CalculationType.MONTHLY_PAYMENT
                        val resultAmount = if (isMonthlyType) calculatedMonthlyPayment else manualMonthlyPayment
                        val propertyAmount = if (isMonthlyType) propertyValue else calculatedPropertyValue
                        
                        Text(
                            text = if (isMonthlyType) "Ежемесячный платеж" else "Стоимость объекта",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (isMonthlyType) {
                                decimalFormatter.format(resultAmount) + " руб."
                            } else {
                                integerFormatter.format(propertyAmount) + " руб."
                            },
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    TextButton(onClick = {
                        navController.navigate("schedule/${currentLoanAmount}/${interestRate}/${termYears}/${isAnnuity}")
                    }) {
                        Text("График >", color = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                ResultRow("Сумма кредита", integerFormatter.format(currentLoanAmount) + " руб.")
                
                var isPaymentMenuExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Тип платежа", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = { isPaymentMenuExpanded = true }) {
                            Text(if (isAnnuity) "Аннуитетный" else "Дифференцированный")
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }
                    DropdownMenu(expanded = isPaymentMenuExpanded, onDismissRequest = { isPaymentMenuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Аннуитетный") }, 
                            onClick = { 
                                mortgageViewModel.isAnnuity.value = true
                                isPaymentMenuExpanded = false 
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Дифференцированный") }, 
                            onClick = { 
                                mortgageViewModel.isAnnuity.value = false
                                isPaymentMenuExpanded = false 
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Toggle Input Field based on Calculation Type
        if (calculationType == CalculationType.MONTHLY_PAYMENT) {
            InputCard(
                label = "Стоимость объекта",
                value = propertyValue,
                onValueChange = { mortgageViewModel.updatePropertyValue(it) },
                step = stepChangeAmount,
                isMoney = true,
                suffix = " руб.",
                range = 1.0..1000000000.0
            )
        } else {
            InputCard(
                label = "Ежемесячный платеж",
                value = manualMonthlyPayment,
                onValueChange = { mortgageViewModel.updateManualMonthlyPayment(it) },
                step = stepMonthlyPayment,
                isMoney = true,
                suffix = " руб.",
                range = 1.0..10000000.0
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            val currentPropertyValue = if (calculationType == CalculationType.MONTHLY_PAYMENT) propertyValue else calculatedPropertyValue
            
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Первоначальный взнос", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                
                // Rubles input
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NumericField(
                        value = downPayment,
                        onValueChange = { mortgageViewModel.updateDownPayment(it) },
                        modifier = Modifier.weight(1f),
                        isMoney = true,
                        suffix = " руб.",
                        range = if (calculationType == CalculationType.MONTHLY_PAYMENT) 0.0..currentPropertyValue else 0.0..1000000000.0
                    )
                    if (calculationType == CalculationType.MONTHLY_PAYMENT && !isPercentLocked) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Row {
                        IconButton(onClick = { mortgageViewModel.updateDownPayment(downPayment - stepChangeAmount) }) { 
                            Text("-", fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) 
                        }
                        IconButton(onClick = { mortgageViewModel.updateDownPayment(downPayment + stepChangeAmount) }) { 
                            Text("+", fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) 
                        }
                    }
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.LightGray.copy(alpha = 0.2f))
                
                // Percentage input
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val currentDisplayPercent = if (calculationType == CalculationType.MONTHLY_PAYMENT) {
                        if (currentPropertyValue > 0) (downPayment / currentPropertyValue * 100) else 0.0
                    } else {
                        downPaymentPercent
                    }
                    
                    NumericField(
                        value = currentDisplayPercent,
                        onValueChange = { mortgageViewModel.updateDownPaymentPercent(it) },
                        modifier = Modifier.weight(1f),
                        suffix = " %",
                        range = 0.0..100.0
                    )
                    if (calculationType == CalculationType.MONTHLY_PAYMENT && isPercentLocked) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Row {
                        IconButton(onClick = { mortgageViewModel.updateDownPaymentPercent(currentDisplayPercent - stepPercent) }) { 
                            Text("-", fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) 
                        }
                        IconButton(onClick = { mortgageViewModel.updateDownPaymentPercent(currentDisplayPercent + stepPercent) }) { 
                            Text("+", fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) 
                        }
                    }
                }
            }
        }

        InputCard(
            label = "Срок",
            value = termYears.toDouble(),
            onValueChange = { mortgageViewModel.updateTermYears(it.toInt()) },
            step = 1.0,
            range = 0.0..30.0,
            isInteger = true,
            suffix = " " + formatYearsLabel(termYears)
        )

        InputCard(
            label = "Процентная ставка",
            value = interestRate,
            onValueChange = { mortgageViewModel.updateInterestRate(it) },
            step = stepInterestRate,
            suffix = " %",
            allowEmpty = true,
            range = 0.0..100.0
        )
    }
}

@Composable
fun ResultRow(label: String, valueText: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = valueText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
            Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                NumericField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.weight(1f),
                    isMoney = isMoney,
                    isInteger = isInteger,
                    suffix = suffix,
                    allowEmpty = allowEmpty,
                    range = range
                )
                Row {
                    IconButton(onClick = { 
                        val newValue = (value - step).let { if (range != null) it.coerceIn(range) else it.coerceAtLeast(0.0) }
                        onValueChange(newValue)
                    }) {
                        Text("-", fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { 
                        val newValue = (value + step).let { if (range != null) it.coerceIn(range) else it }
                        onValueChange(newValue)
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
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    allowEmpty: Boolean = false,
    range: ClosedFloatingPointRange<Double>? = null
) {
    val formatSymbols = DecimalFormatSymbols(Locale.getDefault()).apply { groupingSeparator = ' ' }
    val formatter = if (isMoney) DecimalFormat("#,###", formatSymbols) else if (isInteger) DecimalFormat("#", formatSymbols) else DecimalFormat("0.##", formatSymbols)
    
    var textFieldValueState by remember(value) {
        val formattedText = if (value == 0.0 && allowEmpty) "" else formatter.format(value)
        mutableStateOf(TextFieldValue(text = formattedText, selection = TextRange(formattedText.length)))
    }

    androidx.compose.foundation.text.BasicTextField(
        value = textFieldValueState,
        onValueChange = { newValue ->
            val cleanedInputText = newValue.text.replace(" ", "").replace(",", ".")
            if (cleanedInputText.isEmpty()) {
                textFieldValueState = newValue.copy(text = "")
                if (allowEmpty) onValueChange(0.0)
            } else if (cleanedInputText.toDoubleOrNull() != null) {
                val inputDoubleValue = cleanedInputText.toDouble()
                val validatedValue = if (range != null) inputDoubleValue.coerceIn(range) else inputDoubleValue
                
                textFieldValueState = newValue
                onValueChange(validatedValue)
            }
        },
        singleLine = true,
        maxLines = 1,
        textStyle = LocalTextStyle.current.copy(
            fontSize = 32.sp, 
            fontWeight = FontWeight.Bold,
            color = textColor,
            textAlign = TextAlign.Start
        ),
        visualTransformation = remember(suffix) { SuffixTransformation(suffix) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier
    )
}
