package com.example.mortgagecalculator.ui.screens

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
import com.example.mortgagecalculator.data.CalculationType
import com.example.mortgagecalculator.ui.MortgageViewModel
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

fun getYearString(years: Int): String {
    val mod10 = years % 10
    val mod100 = years % 100
    return when {
        mod10 == 1 && mod100 != 11 -> "год"
        mod10 in 2..4 && (mod100 < 10 || mod100 >= 20) -> "года"
        else -> "лет"
    }
}

class SuffixTransformation(val suffix: String) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val result = text + AnnotatedString(suffix)
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = offset
            override fun transformedToOriginal(offset: Int): Int {
                if (offset > text.length) return text.length
                return offset
            }
        }
        return TransformedText(result, mapping)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculationScreen(viewModel: MortgageViewModel, navController: NavController) {
    val propertyValue by viewModel.propertyValue.collectAsState()
    val downPayment by viewModel.downPayment.collectAsState()
    val termYears by viewModel.termYears.collectAsState()
    val interestRate by viewModel.interestRate.collectAsState()
    val isAnnuity by viewModel.isAnnuity.collectAsState()
    val isPercentLocked by viewModel.isDownPaymentPercentLocked.collectAsState()
    val manualMonthlyPayment by viewModel.manualMonthlyPayment.collectAsState()
    val calculationType by viewModel.calculationType.collectAsState()

    val calculatedMonthlyPayment by viewModel.calculatedMonthlyPayment.collectAsState()
    val calculatedPropertyValue by viewModel.calculatedPropertyValue.collectAsState()
    val loanAmount by viewModel.loanAmount.collectAsState()
    
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Расчет", fontSize = 32.sp, fontWeight = FontWeight.Bold)
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
                        val displayPayment = if (calculationType == CalculationType.MONTHLY_PAYMENT) calculatedMonthlyPayment else manualMonthlyPayment
                        val displayProperty = if (calculationType == CalculationType.MONTHLY_PAYMENT) propertyValue else calculatedPropertyValue
                        
                        Text(
                            text = if (calculationType == CalculationType.MONTHLY_PAYMENT) {
                                decimalFormatter.format(displayPayment) + " руб."
                            } else {
                                formatter.format(displayProperty) + " руб."
                            },
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            if (calculationType == CalculationType.MONTHLY_PAYMENT) "Ежемесячный платеж" else "Стоимость объекта",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = {
                        val currentInterestRate = interestRate
                        val currentTermYears = termYears
                        val currentIsAnnuity = isAnnuity
                        navController.navigate("schedule/${loanAmount}/${currentInterestRate}/${currentTermYears}/${currentIsAnnuity}")
                    }) {
                        Text("График >", color = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                ResultRow("Сумма кредита", formatter.format(loanAmount) + " руб.")
                
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
                        DropdownMenuItem(text = { Text("Аннуитетный") }, onClick = { viewModel.isAnnuity.value = true; expanded = false })
                        DropdownMenuItem(text = { Text("Дифференцированный") }, onClick = { viewModel.isAnnuity.value = false; expanded = false })
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
                onValueChange = { viewModel.updatePropertyValue(it) },
                step = stepChange,
                isMoney = true,
                suffix = " руб.",
                range = 1.0..1000000000.0
            )
        } else {
            InputCard(
                label = "Ежемесячный платеж",
                value = manualMonthlyPayment,
                onValueChange = { viewModel.updateManualMonthlyPayment(it) },
                step = stepChange / 10, // Smaller step for payment
                isMoney = true,
                suffix = " руб.",
                range = 1.0..10000000.0
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            val currentProp = if (calculationType == CalculationType.MONTHLY_PAYMENT) propertyValue else calculatedPropertyValue
            
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Первоначальный взнос", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                
                // Rubles
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NumericField(
                        value = downPayment,
                        onValueChange = { viewModel.updateDownPayment(it) },
                        modifier = Modifier.weight(1f),
                        isMoney = true,
                        suffix = " руб.",
                        range = 0.0..currentProp
                    )
                    if (!isPercentLocked) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Row {
                        IconButton(onClick = { viewModel.updateDownPayment(downPayment - stepChange) }) { Text("-", fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        IconButton(onClick = { viewModel.updateDownPayment(downPayment + stepChange) }) { Text("+", fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.LightGray.copy(alpha = 0.2f))
                
                // Percentage
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val percent = if (currentProp > 0) (downPayment / currentProp * 100) else 0.0
                    NumericField(
                        value = percent,
                        onValueChange = { viewModel.updateDownPaymentPercent(it) },
                        modifier = Modifier.weight(1f),
                        suffix = " %",
                        range = 0.0..100.0
                    )
                    if (isPercentLocked) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Row {
                        IconButton(onClick = { viewModel.updateDownPaymentPercent(percent - stepPercent) }) { Text("-", fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        IconButton(onClick = { viewModel.updateDownPaymentPercent(percent + stepPercent) }) { Text("+", fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }
        }

        InputCard(
            label = "Срок",
            value = termYears.toDouble(),
            onValueChange = { viewModel.updateTermYears(it.toInt()) },
            step = 1.0,
            range = 0.0..30.0,
            isInteger = true,
            suffix = " " + getYearString(termYears)
        )

        InputCard(
            label = "Процентная ставка",
            value = interestRate,
            onValueChange = { viewModel.updateInterestRate(it) },
            step = stepRate,
            suffix = " %",
            allowEmpty = true,
            range = 0.0..100.0
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
                    allowEmpty = allowEmpty,
                    range = range
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
    allowEmpty: Boolean = false,
    range: ClosedFloatingPointRange<Double>? = null
) {
    val symbols = DecimalFormatSymbols(Locale.getDefault()).apply { groupingSeparator = ' ' }
    val formatter = if (isMoney) DecimalFormat("#,###", symbols) else if (isInteger) DecimalFormat("#", symbols) else DecimalFormat("0.##", symbols)
    
    var textFieldValue by remember(value) {
        val text = if (value == 0.0 && allowEmpty) "" else formatter.format(value)
        mutableStateOf(TextFieldValue(text = text, selection = TextRange(text.length)))
    }

    androidx.compose.foundation.text.BasicTextField(
        value = textFieldValue,
        onValueChange = { newValue ->
            val cleanInput = newValue.text.replace(" ", "").replace(",", ".")
            if (cleanInput.isEmpty()) {
                textFieldValue = newValue.copy(text = "")
                if (allowEmpty) onValueChange(0.0)
            } else if (cleanInput.toDoubleOrNull() != null) {
                val inputVal = cleanInput.toDouble()
                val restrictedVal = if (range != null) inputVal.coerceIn(range) else inputVal
                
                textFieldValue = newValue
                onValueChange(restrictedVal)
            }
        },
        singleLine = true,
        maxLines = 1,
        textStyle = LocalTextStyle.current.copy(
            fontSize = 32.sp, 
            fontWeight = FontWeight.Bold,
            color = color,
            textAlign = TextAlign.Start
        ),
        visualTransformation = remember(suffix) { SuffixTransformation(suffix) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier
    )
}
