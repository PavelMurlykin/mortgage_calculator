package com.example.mortgagecalculator.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.pow

data class PaymentScheduleItem(
    val monthName: String,
    val year: Int,
    val payment: Double,
    val principal: Double,
    val interest: Double,
    val remainingBalance: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScheduleScreen(
    loanAmount: Double,
    interestRate: Double,
    years: Int,
    isAnnuity: Boolean,
    onBack: () -> Unit
) {
    val schedule = calculateSchedule(loanAmount, interestRate, years, isAnnuity)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("График платежей", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Закрыть", color = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Summary Card - Simplified
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = String.format("%,.0f", if (schedule.isNotEmpty()) schedule.first().payment else 0.0),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("Первый платеж", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Table Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Месяц", modifier = Modifier.weight(1.5f), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Платёж", modifier = Modifier.weight(1f), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Остаток", modifier = Modifier.weight(1.5f), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                val groupedByYear = schedule.groupBy { it.year }
                groupedByYear.forEach { (year, yearItems) ->
                    item {
                        Text(
                            text = year.toString(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                    items(yearItems) { item ->
                        ScheduleRow(item)
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun ScheduleRow(item: PaymentScheduleItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1.5f)) {
            Text(item.monthName, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
        Text(
            text = String.format("%,.0f", item.payment),
            modifier = Modifier.weight(1f),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = String.format("%,.0f", item.remainingBalance),
            modifier = Modifier.weight(1.5f),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
    }
}

fun calculateSchedule(
    loanAmount: Double,
    interestRate: Double,
    years: Int,
    isAnnuity: Boolean
): List<PaymentScheduleItem> {
    val schedule = mutableListOf<PaymentScheduleItem>()
    val months = years * 12
    val monthlyRate = interestRate / 100 / 12
    var remainingBalance = loanAmount
    
    val calendar = Calendar.getInstance()
    val monthFormat = SimpleDateFormat("LLLL", Locale("ru"))

    if (isAnnuity) {
        val payment = if (monthlyRate == 0.0) loanAmount / months
        else loanAmount * (monthlyRate * (1 + monthlyRate).pow(months)) / ((1 + monthlyRate).pow(months) - 1)
        
        for (i in 1..months) {
            val interest = remainingBalance * monthlyRate
            val principal = payment - interest
            remainingBalance = (remainingBalance - principal).coerceAtLeast(0.0)
            
            schedule.add(
                PaymentScheduleItem(
                    monthName = monthFormat.format(calendar.time).replaceFirstChar { it.uppercase() },
                    year = calendar.get(Calendar.YEAR),
                    payment = payment,
                    principal = principal,
                    interest = interest,
                    remainingBalance = remainingBalance
                )
            )
            calendar.add(Calendar.MONTH, 1)
        }
    } else {
        val principalPart = loanAmount / months
        for (i in 1..months) {
            val interest = remainingBalance * monthlyRate
            val payment = principalPart + interest
            remainingBalance = (remainingBalance - principalPart).coerceAtLeast(0.0)
            
            schedule.add(
                PaymentScheduleItem(
                    monthName = monthFormat.format(calendar.time).replaceFirstChar { it.uppercase() },
                    year = calendar.get(Calendar.YEAR),
                    payment = payment,
                    principal = principalPart,
                    interest = interest,
                    remainingBalance = remainingBalance
                )
            )
            calendar.add(Calendar.MONTH, 1)
        }
    }
    return schedule
}
