package com.example.mortgagecalculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.example.mortgagecalculator.ui.MortgageViewModel
import com.example.mortgagecalculator.ui.screens.*
import com.example.mortgagecalculator.ui.theme.MortgageCalculatorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MortgageCalculatorTheme {
                MainScreen()
            }
        }
    }
}

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Settings : Screen("settings", "Настройки", Icons.Default.Settings)
    object Calculation : Screen("calculation", "Расчет", Icons.Default.Calculate)
    object Saved : Screen("saved", "Список", Icons.Default.List)
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val viewModel: MortgageViewModel = viewModel()

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            
            val showBottomBar = currentDestination?.route in listOf(Screen.Settings.route, Screen.Calculation.route, Screen.Saved.route)
            
            if (showBottomBar) {
                NavigationBar {
                    val items = listOf(Screen.Settings, Screen.Calculation, Screen.Saved)
                    items.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = null) },
                            label = { Text(screen.title) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
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
            navController = navController,
            startDestination = Screen.Calculation.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Settings.route) { SettingsScreen(viewModel) }
            composable(Screen.Calculation.route) { CalculationScreen(viewModel, navController) }
            composable(Screen.Saved.route) { SavedCalculationsScreen(viewModel, navController) }
            composable("schedule/{loan}/{rate}/{years}/{isAnnuity}") { backStackEntry ->
                val loan = backStackEntry.arguments?.getString("loan")?.toDoubleOrNull() ?: 0.0
                val rate = backStackEntry.arguments?.getString("rate")?.toDoubleOrNull() ?: 0.0
                val years = backStackEntry.arguments?.getString("years")?.toIntOrNull() ?: 0
                val isAnnuity = backStackEntry.arguments?.getString("isAnnuity")?.toBoolean() ?: true
                PaymentScheduleScreen(loan, rate, years, isAnnuity, onBack = { navController.popBackStack() })
            }
        }
    }
}
