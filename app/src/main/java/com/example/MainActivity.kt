package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.local.AppDatabase
import com.example.data.remote.RetrofitClient
import com.example.data.repository.CryptoRepository
import com.example.ui.CryptoViewModel
import com.example.ui.CryptoViewModelFactory
import com.example.ui.cryptodetail.CryptoDetailScreen
import com.example.ui.cryptolist.CryptoListScreen
import com.example.ui.favorites.FavoritesScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Room Database & Repository initialization
        val database = AppDatabase.getDatabase(applicationContext)
        val apiService = RetrofitClient.apiService
        val repository = CryptoRepository(apiService, database.cryptoDao())

        // ViewModel initialization
        val viewModel = ViewModelProvider(
            this,
            CryptoViewModelFactory(repository)
        )[CryptoViewModel::class.java]

        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsState()

            MyApplicationTheme(darkTheme = isDarkMode) {
                val navController = rememberNavController()
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route ?: "market"

                Scaffold(
                    topBar = {
                        // Top App Bar - only visible on Main screens for clean visual layout
                        if (currentRoute != "detail") {
                            TopAppBar(
                                title = {
                                    Text(
                                        text = if (currentRoute == "market") "Kryptos Market" else "Watchlist",
                                        fontWeight = FontWeight.ExtraBold,
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                },
                                actions = {
                                    // Manual reload indicator icon
                                    IconButton(onClick = { viewModel.refreshData() }) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Refresh listings"
                                        )
                                    }
                                    // Day/Night switch Button
                                    IconButton(onClick = { viewModel.toggleTheme() }) {
                                        Icon(
                                            imageVector = if (isDarkMode) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                                            contentDescription = "Toggle color scheme theme"
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.background,
                                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                                )
                            )
                        }
                    },
                    bottomBar = {
                        // Dynamically hide NavigationBar on custom details workspace
                        if (currentRoute != "detail") {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 8.dp
                            ) {
                                NavigationBarItem(
                                    selected = currentRoute == "market",
                                    onClick = {
                                        if (currentRoute != "market") {
                                            navController.navigate("market") {
                                                popUpTo("market") { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (currentRoute == "market") Icons.Default.TrendingUp else Icons.Outlined.TrendingUp,
                                            contentDescription = "Markets navigation trigger"
                                        )
                                    },
                                    label = { Text("Market") }
                                )

                                NavigationBarItem(
                                    selected = currentRoute == "favorites",
                                    onClick = {
                                        if (currentRoute != "favorites") {
                                            navController.navigate("favorites") {
                                                popUpTo("market") { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (currentRoute == "favorites") Icons.Default.Star else Icons.Outlined.StarBorder,
                                            contentDescription = "Favorites watchlist trigger"
                                        )
                                    },
                                    label = { Text("Watchlist") }
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "market",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("market") {
                            CryptoListScreen(
                                viewModel = viewModel,
                                onCoinClick = { coin ->
                                    viewModel.selectCoin(coin)
                                    navController.navigate("detail")
                                }
                            )
                        }
                        composable("favorites") {
                            FavoritesScreen(
                                viewModel = viewModel,
                                onCoinClick = { coin ->
                                    viewModel.selectCoin(coin)
                                    navController.navigate("detail")
                                }
                            )
                        }
                        composable("detail") {
                            CryptoDetailScreen(
                                viewModel = viewModel,
                                onBackClick = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
