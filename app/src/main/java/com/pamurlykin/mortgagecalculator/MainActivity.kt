package com.pamurlykin.mortgagecalculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.pamurlykin.mortgagecalculator.ui.MortgageViewModel
import com.pamurlykin.mortgagecalculator.ui.screens.*
import com.pamurlykin.mortgagecalculator.ui.theme.MortgageCalculatorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MortgageCalculatorTheme {
                MainAppScreen()
            }
        }
    }
}

sealed class AppScreen(val route: String, val title: String, val icon: ImageVector) {
    object Settings : AppScreen("settings", "Настройки", Icons.Default.Settings)
    object Calculation : AppScreen("calculation", "Расчет", Icons.Default.Calculate)
    object SavedCalculations : AppScreen("saved", "Список", Icons.AutoMirrored.Filled.List)
}

@Composable
fun MainAppScreen() {
    val navigationController = rememberNavController()
    val mortgageViewModel: MortgageViewModel = viewModel()

    Scaffold(
        bottomBar = {
            val navigationBackStackEntry by navigationController.currentBackStackEntryAsState()
            val currentDestination = navigationBackStackEntry?.destination
            
            val shouldShowBottomBar = currentDestination?.route in listOf(
                AppScreen.Settings.route, 
                AppScreen.Calculation.route, 
                AppScreen.SavedCalculations.route
            )
            
            if (shouldShowBottomBar) {
                NavigationBar {
                    val navigationItems = listOf(
                        AppScreen.Settings, 
                        AppScreen.Calculation, 
                        AppScreen.SavedCalculations
                    )
                    navigationItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = null) },
                            label = { Text(screen.title) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navigationController.navigate(screen.route) {
                                    popUpTo(navigationController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navigationController,
            startDestination = AppScreen.Calculation.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(AppScreen.Settings.route) { 
                SettingsScreen(mortgageViewModel) 
            }
            composable(AppScreen.Calculation.route) { 
                CalculationScreen(mortgageViewModel, navigationController) 
            }
            composable(AppScreen.SavedCalculations.route) { 
                SavedCalculationsScreen(mortgageViewModel, navigationController) 
            }
            composable("schedule/{loanAmount}/{interestRate}/{termMonths}/{isAnnuity}") { backStackEntry ->
                val loanAmount = backStackEntry.arguments?.getString("loanAmount")?.toDoubleOrNull() ?: 0.0
                val interestRate = backStackEntry.arguments?.getString("interestRate")?.toDoubleOrNull() ?: 0.0
                val termMonths = backStackEntry.arguments?.getString("termMonths")?.toIntOrNull() ?: 0
                val isAnnuity = backStackEntry.arguments?.getString("isAnnuity")?.toBoolean() ?: true
                
                PaymentScheduleScreen(
                    loanAmount = loanAmount, 
                    interestRate = interestRate, 
                    termMonths = termMonths,
                    isAnnuity = isAnnuity, 
                    onBack = { navigationController.popBackStack() }
                )
            }
        }
    }
}
