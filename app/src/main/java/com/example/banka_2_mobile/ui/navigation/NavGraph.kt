package com.example.banka_2_mobile.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.banka_2_mobile.data.repository.AuthRepository
import com.example.banka_2_mobile.ui.cards.CardsScreen
import com.example.banka_2_mobile.ui.exchange.ExchangeScreen
import com.example.banka_2_mobile.ui.home.HomeScreen
import com.example.banka_2_mobile.ui.login.LoginScreen
import com.example.banka_2_mobile.ui.otp.OtpScreen
import com.example.banka_2_mobile.ui.transactions.TransactionsScreen

object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val TRANSACTIONS = "transactions"
    const val CARDS = "cards"
    const val EXCHANGE = "exchange"
    const val OTP = "otp"
}

private val bottomNavItems = listOf(
    BottomNavItem(
        route = Routes.TRANSACTIONS,
        label = "Transakcije",
        icon = Icons.Filled.Receipt
    ),
    BottomNavItem(
        route = Routes.CARDS,
        label = "Kartice",
        icon = Icons.Filled.CreditCard
    ),
    BottomNavItem(
        route = Routes.HOME,
        label = "Pocetna",
        icon = Icons.Filled.AccountBalanceWallet,
        isCenter = true
    ),
    BottomNavItem(
        route = Routes.EXCHANGE,
        label = "Menjacnica",
        icon = Icons.Filled.TrendingUp
    ),
    BottomNavItem(
        route = Routes.OTP,
        label = "OTP",
        icon = Icons.Filled.Shield
    )
)

// Routes that show the bottom nav bar
private val mainRoutes = setOf(
    Routes.HOME,
    Routes.TRANSACTIONS,
    Routes.CARDS,
    Routes.EXCHANGE,
    Routes.OTP
)

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val authRepository = remember { AuthRepository(context) }

    val startDestination = if (authRepository.isLoggedIn()) Routes.HOME else Routes.LOGIN

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: startDestination
    val showBottomBar = currentRoute in mainRoutes

    val navigateToLogin: () -> Unit = {
        navController.navigate(Routes.LOGIN) {
            popUpTo(0) { inclusive = true }
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(
                    items = bottomNavItems,
                    currentRoute = currentRoute,
                    onItemClick = { route ->
                        if (route != currentRoute) {
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    ) { _ ->
        NavHost(
            navController = navController,
            startDestination = startDestination
        ) {
            composable(Routes.LOGIN) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.HOME) {
                HomeScreen(onLogout = navigateToLogin)
            }

            composable(Routes.TRANSACTIONS) {
                TransactionsScreen(onLogout = navigateToLogin)
            }

            composable(Routes.CARDS) {
                CardsScreen(onLogout = navigateToLogin)
            }

            composable(Routes.EXCHANGE) {
                ExchangeScreen(onLogout = navigateToLogin)
            }

            composable(Routes.OTP) {
                OtpScreen(onLogout = navigateToLogin)
            }
        }
    }
}
