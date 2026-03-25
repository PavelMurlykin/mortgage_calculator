package com.pamurlykin.mortgagecalculator.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import kotlin.math.pow

data class PaymentItem(
    val monthNumber: Int,
    val paymentAmount: Double,
    val principalAmount: Double,
    val interestAmount: Double,
    val remainingBalance: Double,
    val date: Calendar
)

private val RUSSIAN_MONTHS = listOf(
    "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
    "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScheduleScreen(
    loanAmount: Double,
    interestRate: Double,
    termMonths: Int,
    isAnnuity: Boolean,
    onBack: () -> Unit
) {
    val paymentSchedule = remember(loanAmount, interestRate, termMonths, isAnnuity) {
        calculatePaymentSchedule(loanAmount, interestRate, termMonths, isAnnuity)
    }
    
    val groupedSchedule = remember(paymentSchedule) {
        paymentSchedule.groupBy { it.date.get(Calendar.YEAR) }
    }

    val yearsList = remember(groupedSchedule) {
        groupedSchedule.keys.sorted()
    }

    val expandedYears = remember { mutableStateMapOf<Int, Boolean>().apply { 
        if (yearsList.isNotEmpty()) put(yearsList.first(), true) 
    } }

    val symbols = DecimalFormatSymbols(Locale.getDefault()).apply { groupingSeparator = ' ' }
    val formatter = DecimalFormat("#,###", symbols)

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Заголовок с кнопкой назад, как на других экранах
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
            }
            Text(
                text = "График", 
                fontSize = 32.sp, 
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Подзаголовки таблицы
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Месяц",
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Платеж",
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Остаток",
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            yearsList.forEach { year ->
                val months = groupedSchedule[year] ?: emptyList()
                item {
                    YearHeader(
                        year = year,
                        isExpanded = expandedYears[year] ?: false,
                        onToggle = { expandedYears[year] = !(expandedYears[year] ?: false) }
                    )
                }

                if (expandedYears[year] == true) {
                    items(months) { paymentItem ->
                        PaymentRow(paymentItem, formatter)
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YearHeader(year: Int, isExpanded: Boolean, onToggle: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        onClick = onToggle
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$year год",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.primary
            )
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun PaymentRow(paymentItem: PaymentItem, formatter: DecimalFormat) {
    val monthName = RUSSIAN_MONTHS[paymentItem.date.get(Calendar.MONTH)]
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = monthName,
            modifier = Modifier.weight(1f),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            textAlign = TextAlign.Start
        )
        Text(
            text = formatter.format(paymentItem.paymentAmount),
            modifier = Modifier.weight(1f),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Text(
            text = formatter.format(paymentItem.remainingBalance),
            modifier = Modifier.weight(1f),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}

private fun calculatePaymentSchedule(
    loanAmount: Double,
    interestRate: Double,
    totalMonths: Int,
    isAnnuity: Boolean
): List<PaymentItem> {
    val scheduleList = mutableListOf<PaymentItem>()
    if (loanAmount <= 0 || totalMonths <= 0) return scheduleList

    val monthlyInterestRate = interestRate / 100 / 12
    var remainingBalanceValue = loanAmount
    val startDate = Calendar.getInstance()

    if (isAnnuity) {
        val annuityPayment = if (monthlyInterestRate == 0.0) {
            loanAmount / totalMonths
        } else {
            loanAmount * (monthlyInterestRate * (1 + monthlyInterestRate).pow(totalMonths)) /
                    ((1 + monthlyInterestRate).pow(totalMonths) - 1)
        }

        for (month in 1..totalMonths) {
            val interestPart = remainingBalanceValue * monthlyInterestRate
            val principalPart = annuityPayment - interestPart
            remainingBalanceValue -= principalPart
            val paymentDate = (startDate.clone() as Calendar).apply { add(Calendar.MONTH, month) }

            scheduleList.add(
                PaymentItem(
                    monthNumber = month,
                    paymentAmount = annuityPayment,
                    principalAmount = principalPart,
                    interestAmount = interestPart,
                    remainingBalance = remainingBalanceValue.coerceAtLeast(0.0),
                    date = paymentDate
                )
            )
        }
    } else {
        val fixedPrincipalPart = loanAmount / totalMonths
        for (month in 1..totalMonths) {
            val interestPart = remainingBalanceValue * monthlyInterestRate
            val currentMonthlyPayment = fixedPrincipalPart + interestPart
            remainingBalanceValue -= fixedPrincipalPart
            val paymentDate = (startDate.clone() as Calendar).apply { add(Calendar.MONTH, month) }

            scheduleList.add(
                PaymentItem(
                    monthNumber = month,
                    paymentAmount = currentMonthlyPayment,
                    principalAmount = fixedPrincipalPart,
                    interestAmount = interestPart,
                    remainingBalance = remainingBalanceValue.coerceAtLeast(0.0),
                    date = paymentDate
                )
            )
        }
    }
    return scheduleList
}
