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
import kotlin.math.abs

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
    val finalPropertyValue by mortgageViewModel.finalPropertyValue.collectAsState()
    val downPayment by mortgageViewModel.downPayment.collectAsState()
    val downPaymentPercent by mortgageViewModel.downPaymentPercent.collectAsState()
    val termYears by mortgageViewModel.termYears.collectAsState()
    val termMonths by mortgageViewModel.termMonths.collectAsState()
    val isTermYearsLocked by mortgageViewModel.isTermYearsLocked.collectAsState()
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

    val showDiscountOption by mortgageViewModel.showDiscountOption.collectAsState()
    val discountAmount by mortgageViewModel.discountAmount.collectAsState()
    val isMarkup by mortgageViewModel.isMarkup.collectAsState()
    val discountPercent by mortgageViewModel.discountPercent.collectAsState()
    val isDiscountPercentLocked by mortgageViewModel.isDiscountPercentLocked.collectAsState()

    val formatSymbols = DecimalFormatSymbols(Locale.getDefault()).apply {
        groupingSeparator = ' '
    }
    val integerFormatter = DecimalFormat("#,###", formatSymbols)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Расчет", fontSize = 32.sp, fontWeight = FontWeight.Bold)
            if (calculationType == CalculationType.MONTHLY_PAYMENT) {
                IconButton(onClick = { mortgageViewModel.saveCalculation() }) {
                    Icon(Icons.Default.Save, contentDescription = "Сохраненный расчет", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Result Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        val isMonthlyType = calculationType == CalculationType.MONTHLY_PAYMENT
                        val resultAmount = if (isMonthlyType) calculatedMonthlyPayment else calculatedPropertyValue
                        
                        Text(
                            text = if (isMonthlyType) "Ежемесячный платеж" else "Стоимость объекта",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = integerFormatter.format(resultAmount) + " ₽",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (calculationType == CalculationType.MONTHLY_PAYMENT) {
                        TextButton(onClick = {
                            navController.navigate("schedule/${currentLoanAmount}/${interestRate}/${termMonths}/${isAnnuity}")
                        }) {
                            Text("График >", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                ResultRow("Сумма кредита", integerFormatter.format(currentLoanAmount) + " ₽")
                
                var isPaymentMenuExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
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

        Spacer(modifier = Modifier.height(8.dp))

        if (calculationType == CalculationType.MONTHLY_PAYMENT) {
            MonthlyPaymentFields(
                mortgageViewModel = mortgageViewModel,
                propertyValue = propertyValue,
                stepChangeAmount = stepChangeAmount,
                showDiscountOption = showDiscountOption,
                isMarkup = isMarkup,
                discountAmount = discountAmount,
                isDiscountPercentLocked = isDiscountPercentLocked,
                discountPercent = discountPercent,
                stepPercent = stepPercent,
                finalPropertyValue = finalPropertyValue,
                integerFormatter = integerFormatter
            )
        } else {
            PropertyValueFields(
                mortgageViewModel = mortgageViewModel,
                manualMonthlyPayment = manualMonthlyPayment,
                stepMonthlyPayment = stepMonthlyPayment
            )
        }

        DownPaymentCard(
            mortgageViewModel = mortgageViewModel,
            calculationType = calculationType,
            finalPropertyValue = finalPropertyValue,
            calculatedPropertyValue = calculatedPropertyValue,
            downPayment = downPayment,
            downPaymentPercent = downPaymentPercent,
            isPercentLocked = isPercentLocked,
            stepChangeAmount = stepChangeAmount,
            stepPercent = stepPercent
        )

        TermCard(
            mortgageViewModel = mortgageViewModel,
            termYears = termYears,
            termMonths = termMonths,
            isTermYearsLocked = isTermYearsLocked
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
fun MonthlyPaymentFields(
    mortgageViewModel: MortgageViewModel,
    propertyValue: Double,
    stepChangeAmount: Double,
    showDiscountOption: Boolean,
    isMarkup: Boolean,
    discountAmount: Double,
    isDiscountPercentLocked: Boolean,
    discountPercent: Double,
    stepPercent: Double,
    finalPropertyValue: Double,
    integerFormatter: DecimalFormat
) {
    InputCard(
        label = "Стоимость объекта",
        value = propertyValue,
        onValueChange = { mortgageViewModel.updatePropertyValue(it) },
        step = stepChangeAmount,
        isMoney = true,
        suffix = " ₽",
        range = 1.0..1000000000.0
    )

    if (showDiscountOption) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isMarkup) "Удорожание" else "Скидка",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Switch(
                        checked = isMarkup,
                        onCheckedChange = { mortgageViewModel.isMarkup.value = it }
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NumericField(
                        value = discountAmount,
                        onValueChange = { mortgageViewModel.updateDiscountAmount(it) },
                        modifier = Modifier.weight(1f),
                        isMoney = true,
                        suffix = " ₽"
                    )
                    if (!isDiscountPercentLocked) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Row {
                        IconButton(onClick = { mortgageViewModel.updateDiscountAmount(discountAmount - stepChangeAmount) }) { 
                            Text("-", fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) 
                        }
                        IconButton(onClick = { mortgageViewModel.updateDiscountAmount(discountAmount + stepChangeAmount) }) { 
                            Text("+", fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) 
                        }
                    }
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color.LightGray.copy(alpha = 0.2f))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NumericField(
                        value = discountPercent,
                        onValueChange = { mortgageViewModel.updateDiscountPercent(it) },
                        modifier = Modifier.weight(1f),
                        suffix = " %"
                    )
                    if (isDiscountPercentLocked) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Row {
                        IconButton(onClick = { mortgageViewModel.updateDiscountPercent(discountPercent - stepPercent) }) { 
                            Text("-", fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) 
                        }
                        IconButton(onClick = { mortgageViewModel.updateDiscountPercent(discountPercent + stepPercent) }) { 
                            Text("+", fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) 
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Итоговая стоимость объекта", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = integerFormatter.format(finalPropertyValue) + " ₽",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Blue
                    )
                }
            }
        }
    }
}

@Composable
fun PropertyValueFields(
    mortgageViewModel: MortgageViewModel,
    manualMonthlyPayment: Double,
    stepMonthlyPayment: Double
) {
    InputCard(
        label = "Ежемесячный платеж",
        value = manualMonthlyPayment,
        onValueChange = { mortgageViewModel.updateManualMonthlyPayment(it) },
        step = stepMonthlyPayment,
        isMoney = true,
        suffix = " ₽",
        range = 1.0..10000000.0
    )
}

@Composable
fun DownPaymentCard(
    mortgageViewModel: MortgageViewModel,
    calculationType: CalculationType,
    finalPropertyValue: Double,
    calculatedPropertyValue: Double,
    downPayment: Double,
    downPaymentPercent: Double,
    isPercentLocked: Boolean,
    stepChangeAmount: Double,
    stepPercent: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        val currentPropertyValue = if (calculationType == CalculationType.MONTHLY_PAYMENT) finalPropertyValue else calculatedPropertyValue
        
        Column(modifier = Modifier.padding(8.dp)) {
            Text("Первоначальный взнос", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                NumericField(
                    value = downPayment,
                    onValueChange = { mortgageViewModel.updateDownPayment(it) },
                    modifier = Modifier.weight(1f),
                    isMoney = true,
                    suffix = " ₽",
                    range = if (calculationType == CalculationType.MONTHLY_PAYMENT) 0.0..currentPropertyValue else 0.0..1000000000.0
                )
                if (!isPercentLocked) {
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
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color.LightGray.copy(alpha = 0.2f))
            
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
                if (isPercentLocked) {
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
}

@Composable
fun TermCard(
    mortgageViewModel: MortgageViewModel,
    termYears: Int,
    termMonths: Int,
    isTermYearsLocked: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text("Срок", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                NumericField(
                    value = termYears.toDouble(),
                    onValueChange = { mortgageViewModel.updateTermYears(it.toInt()) },
                    modifier = Modifier.weight(1f),
                    isInteger = true,
                    suffix = " " + formatYearsLabel(termYears)
                )
                if (isTermYearsLocked) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Row {
                    IconButton(onClick = { mortgageViewModel.updateTermYears(termYears - 1) }) { 
                        Text("-", fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) 
                    }
                    IconButton(onClick = { mortgageViewModel.updateTermYears(termYears + 1) }) { 
                        Text("+", fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) 
                    }
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color.LightGray.copy(alpha = 0.2f))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                NumericField(
                    value = termMonths.toDouble(),
                    onValueChange = { mortgageViewModel.updateTermMonths(it.toInt()) },
                    modifier = Modifier.weight(1f),
                    isInteger = true,
                    suffix = " мес."
                )
                if (!isTermYearsLocked) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Row {
                    IconButton(onClick = { mortgageViewModel.updateTermMonths(termMonths - 1) }) { 
                        Text("-", fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) 
                    }
                    IconButton(onClick = { mortgageViewModel.updateTermMonths(termMonths + 1) }) { 
                        Text("+", fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) 
                    }
                }
            }
        }
    }
}

@Composable
fun ResultRow(label: String, valueText: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
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
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
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
    
    var textFieldValueState by remember {
        val formattedText = if (value == 0.0 && allowEmpty) "" else formatter.format(value)
        mutableStateOf(TextFieldValue(text = formattedText, selection = TextRange(formattedText.length)))
    }

    LaunchedEffect(value) {
        val currentDouble = textFieldValueState.text.replace(" ", "").replace(",", ".").toDoubleOrNull() ?: 0.0
        if (abs(currentDouble - value) > 0.0001) {
            val formattedText = if (value == 0.0 && allowEmpty) "" else formatter.format(value)
            textFieldValueState = TextFieldValue(text = formattedText, selection = TextRange(formattedText.length))
        }
    }

    androidx.compose.foundation.text.BasicTextField(
        value = textFieldValueState,
        onValueChange = { newValue ->
            val oldText = textFieldValueState.text
            val newTextRaw = newValue.text
            val cleanedInputText = newTextRaw.replace(" ", "").replace(",", ".")
            
            if (cleanedInputText.isEmpty()) {
                textFieldValueState = newValue.copy(text = "")
                if (allowEmpty) onValueChange(0.0)
                return@BasicTextField
            }
            
            val inputDoubleValue = cleanedInputText.toDoubleOrNull()
            if (inputDoubleValue != null) {
                val validatedValue = if (range != null) inputDoubleValue.coerceIn(range) else inputDoubleValue
                val formattedNewText = formatter.format(validatedValue)
                
                val cursorInNewRaw = newValue.selection.start
                val significantCharsBeforeCursor = newTextRaw.take(cursorInNewRaw).count { it.isDigit() || it == '.' }
                
                var newCursorPos = 0
                var sigFound = 0
                while (newCursorPos < formattedNewText.length && sigFound < significantCharsBeforeCursor) {
                    if (formattedNewText[newCursorPos].isDigit() || formattedNewText[newCursorPos] == '.') {
                        sigFound++
                    }
                    newCursorPos++
                }
                
                if (newCursorPos < formattedNewText.length && formattedNewText[newCursorPos] == ' ') {
                    if (newTextRaw.length >= oldText.length) {
                        newCursorPos++
                    }
                }

                textFieldValueState = TextFieldValue(
                    text = formattedNewText,
                    selection = TextRange(newCursorPos.coerceIn(0, formattedNewText.length))
                )
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
