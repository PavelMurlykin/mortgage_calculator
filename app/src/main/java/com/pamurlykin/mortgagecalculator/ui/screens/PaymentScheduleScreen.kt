package com.pamurlykin.mortgagecalculator.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
    val remainingBalance: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScheduleScreen(
    loanAmount: Double,
    interestRate: Double,
    termYears: Int,
    isAnnuity: Boolean,
    onBack: () -> Unit
) {
    val paymentSchedule = calculatePaymentSchedule(loanAmount, interestRate, termYears, isAnnuity)
    val symbols = DecimalFormatSymbols(Locale.getDefault()).apply { groupingSeparator = ' ' }
    val formatter = DecimalFormat("#,###.00", symbols)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("График платежей") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("№", modifier = Modifier.weight(0.15f), fontWeight = FontWeight.Bold)
                Text("Платеж", modifier = Modifier.weight(0.35f), fontWeight = FontWeight.Bold)
                Text("Остаток", modifier = Modifier.weight(0.5f), fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.End)
            }
            HorizontalDivider()

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(paymentSchedule) { index, paymentItem ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${paymentItem.monthNumber}", modifier = Modifier.weight(0.15f), fontSize = 14.sp)
                        Column(modifier = Modifier.weight(0.35f)) {
                            Text(formatter.format(paymentItem.paymentAmount), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(
                                "П: ${formatter.format(paymentItem.principalAmount)} / %: ${formatter.format(paymentItem.interestAmount)}",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }
                        Text(
                            formatter.format(paymentItem.remainingBalance),
                            modifier = Modifier.weight(0.5f),
                            fontSize = 14.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.End
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = Color.LightGray)
                }
            }
        }
    }
}

private fun calculatePaymentSchedule(
    loanAmount: Double,
    interestRate: Double,
    termYears: Int,
    isAnnuity: Boolean
): List<PaymentItem> {
    val scheduleList = mutableListOf<PaymentItem>()
    if (loanAmount <= 0 || termYears <= 0) return scheduleList

    val monthlyInterestRate = interestRate / 100 / 12
    val totalMonths = termYears * 12
    var remainingBalanceValue = loanAmount

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
            scheduleList.add(
                PaymentItem(
                    monthNumber = month,
                    paymentAmount = annuityPayment,
                    principalAmount = principalPart,
                    interestAmount = interestPart,
                    remainingBalance = remainingBalanceValue.coerceAtLeast(0.0)
                )
            )
        }
    } else {
        val fixedPrincipalPart = loanAmount / totalMonths
        for (month in 1..totalMonths) {
            val interestPart = remainingBalanceValue * monthlyInterestRate
            val currentMonthlyPayment = fixedPrincipalPart + interestPart
            remainingBalanceValue -= fixedPrincipalPart
            scheduleList.add(
                PaymentItem(
                    monthNumber = month,
                    paymentAmount = currentMonthlyPayment,
                    principalAmount = fixedPrincipalPart,
                    interestAmount = interestPart,
                    remainingBalance = remainingBalanceValue.coerceAtLeast(0.0)
                )
            )
        }
    }

    return scheduleList
}
