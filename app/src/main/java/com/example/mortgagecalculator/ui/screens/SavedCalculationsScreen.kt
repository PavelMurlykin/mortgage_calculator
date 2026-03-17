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
fun SavedCalculationsScreen(mortgageViewModel: MortgageViewModel, navigationController: NavController) {
    val savedCalculationsList by mortgageViewModel.savedCalculations.collectAsState()

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
            items(savedCalculationsList) { calculationEntity ->
                CalculationHistoryItem(
                    mortgageCalculation = calculationEntity, 
                    onDeleteClick = { 
                        mortgageViewModel.deleteSavedCalculation(calculationEntity.id) 
                    }, 
                    onItemClick = {
                        val loanAmountValue = calculationEntity.propertyValue - calculationEntity.downPayment
                        navigationController.navigate("schedule/${loanAmountValue}/${calculationEntity.interestRate}/${calculationEntity.termYears}/${calculationEntity.isAnnuity}")
                    }
                )
            }
        }
    }
}

@Composable
fun CalculationHistoryItem(
    mortgageCalculation: MortgageEntity, 
    onDeleteClick: () -> Unit, 
    onItemClick: () -> Unit
) {
    val currentLoanAmount = mortgageCalculation.propertyValue - mortgageCalculation.downPayment
    val monthlyInterestRate = mortgageCalculation.interestRate / 100 / 12
    val totalMonthsCount = mortgageCalculation.termYears * 12
    
    val monthlyPaymentValue = if (mortgageCalculation.isAnnuity) {
        if (monthlyInterestRate == 0.0) currentLoanAmount / totalMonthsCount
        else currentLoanAmount * (monthlyInterestRate * (1 + monthlyInterestRate).pow(totalMonthsCount)) / 
                ((1 + monthlyInterestRate).pow(totalMonthsCount) - 1)
    } else {
        (currentLoanAmount / totalMonthsCount) + (currentLoanAmount * monthlyInterestRate) // First payment for differentiated
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        onClick = onItemClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatAmountInMillions(mortgageCalculation.propertyValue),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = String.format("%.2f %%", mortgageCalculation.interestRate),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "${mortgageCalculation.termYears} " + formatYearsLabel(mortgageCalculation.termYears),
                        fontSize = 16.sp,
                        color = Color.Gray
                    )
                }
            }
            
            VerticalDivider(modifier = Modifier.height(40.dp).padding(horizontal = 8.dp))
            
            Column(horizontalAlignment = Alignment.End) {
                Text("График", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                Text(
                    text = String.format("%,.0f", monthlyPaymentValue),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = Color.LightGray)
            }
        }
    }
}

fun formatAmountInMillions(amountValue: Double): String {
    return if (amountValue >= 1_000_000) {
        String.format("%.1f млн", amountValue / 1_000_000)
    } else {
        String.format("%,.0f", amountValue)
    }
}
