package com.example.mortgagecalculator.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.mortgagecalculator.data.MortgageEntity
import com.example.mortgagecalculator.ui.MortgageViewModel
import kotlin.math.pow

@Composable
fun SavedCalculationsScreen(viewModel: MortgageViewModel, navController: NavController) {
    val calculations by viewModel.savedCalculations.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Список",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(calculations) { calculation ->
                CalculationItem(calculation, onDelete = { viewModel.deleteCalculation(calculation.id) }, onClick = {
                    val loan = calculation.propertyValue - calculation.downPayment
                    navController.navigate("schedule/${loan}/${calculation.interestRate}/${calculation.termYears}/${calculation.isAnnuity}")
                })
            }
        }
    }
}

@Composable
fun CalculationItem(calculation: MortgageEntity, onDelete: () -> Unit, onClick: () -> Unit) {
    val loanAmount = calculation.propertyValue - calculation.downPayment
    val monthlyRate = calculation.interestRate / 100 / 12
    val months = calculation.termYears * 12
    val payment = if (calculation.isAnnuity) {
        if (monthlyRate == 0.0) loanAmount / months
        else loanAmount * (monthlyRate * (1 + monthlyRate).pow(months)) / ((1 + monthlyRate).pow(months) - 1)
    } else {
        (loanAmount / months) + (loanAmount * monthlyRate) // First payment
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatMillions(calculation.propertyValue),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = String.format("%.2f %%", calculation.interestRate),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "${calculation.termYears} лет",
                        fontSize = 16.sp,
                        color = Color.Gray
                    )
                }
            }
            
            VerticalDivider(modifier = Modifier.height(40.dp).padding(horizontal = 8.dp))
            
            Column(horizontalAlignment = Alignment.End) {
                Text("График", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                Text(
                    text = String.format("%,.0f", payment),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.LightGray)
            }
        }
    }
}

fun formatMillions(value: Double): String {
    return if (value >= 1_000_000) {
        String.format("%.1f млн", value / 1_000_000)
    } else {
        String.format("%,.0f", value)
    }
}
