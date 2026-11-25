package com.example.documate.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Sealed hierarchy of application destinations for Navigation Compose.
 */
sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object History : Screen("history")
    data object Settings : Screen("settings")

    data object ImagesToPdf : Screen("images_to_pdf")
    data object AddImagesToPdf : Screen("add_images_to_pdf")
    data object PdfCompressor : Screen("pdf_compressor")
    data object ExcelToPdf : Screen("excel_to_pdf")
    data object WordToPdf : Screen("word_to_pdf")
    data object PowerPointToPdf : Screen("powerpoint_to_pdf")

    data object Result : Screen("result/{recordId}") {
        fun createRoute(recordId: String) = "result/$recordId"
    }
}

/**
 * Bottom navigation item descriptors.
 */
enum class BottomNavItem(
    val title: String,
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME(
        title = "Tools",
        route = Screen.Home.route,
        selectedIcon = Icons.Filled.GridView,
        unselectedIcon = Icons.Outlined.GridView
    ),
    HISTORY(
        title = "History",
        route = Screen.History.route,
        selectedIcon = Icons.Filled.History,
        unselectedIcon = Icons.Outlined.History
    ),
    SETTINGS(
        title = "Settings",
        route = Screen.Settings.route,
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )
}
