package com.example.documate.core.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.documate.core.domain.model.DocumentTool
import com.example.documate.feature.add_images_to_pdf.presentation.AddImagesToPdfScreen
import com.example.documate.feature.excel_to_pdf.presentation.ExcelToPdfScreen
import com.example.documate.feature.history.presentation.HistoryScreen
import com.example.documate.feature.home.presentation.HomeScreen
import com.example.documate.feature.images_to_pdf.presentation.ImagesToPdfScreen
import com.example.documate.feature.pdf_compressor.presentation.PdfCompressorScreen
import com.example.documate.feature.powerpoint_to_pdf.presentation.PowerPointToPdfScreen
import com.example.documate.feature.settings.presentation.SettingsScreen
import com.example.documate.feature.word_to_pdf.presentation.WordToPdfScreen

@Composable
fun DocuNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Show bottom bar only on the primary tabs
    val showBottomBar = currentRoute in listOf(
        Screen.Home.route,
        Screen.History.route,
        Screen.Settings.route
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    BottomNavItem.entries.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.title
                                )
                            },
                            label = { Text(text = item.title) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToTool = { tool ->
                        when (tool) {
                            DocumentTool.IMAGES_TO_PDF -> navController.navigate(Screen.ImagesToPdf.route)
                            DocumentTool.ADD_IMAGES_TO_PDF -> navController.navigate(Screen.AddImagesToPdf.route)
                            DocumentTool.PDF_COMPRESSION -> navController.navigate(Screen.PdfCompressor.route)
                            DocumentTool.EXCEL_TO_PDF -> navController.navigate(Screen.ExcelToPdf.route)
                            DocumentTool.WORD_TO_PDF -> navController.navigate(Screen.WordToPdf.route)
                            DocumentTool.POWERPOINT_TO_PDF -> navController.navigate(Screen.PowerPointToPdf.route)
                        }
                    },
                    onNavigateToHistory = {
                        navController.navigate(Screen.History.route)
                    }
                )
            }

            composable(Screen.History.route) {
                HistoryScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.ImagesToPdf.route) {
                ImagesToPdfScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.AddImagesToPdf.route) {
                AddImagesToPdfScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.PdfCompressor.route) {
                PdfCompressorScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.ExcelToPdf.route) {
                ExcelToPdfScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.WordToPdf.route) {
                WordToPdfScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.PowerPointToPdf.route) {
                PowerPointToPdfScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
