package com.tollscan.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tollscan.app.ui.screens.DetailScreen
import com.tollscan.app.ui.screens.ExportScreen
import com.tollscan.app.ui.screens.HomeScreen
import com.tollscan.app.ui.screens.ScanScreen

object Routes {
    const val HOME = "home"
    const val SCAN = "scan"
    const val DETAIL = "detail/{receiptId}"
    const val EXPORT = "export"

    fun detail(id: Long) = "detail/$id"
}

@Composable
fun TollScanNavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onScanClick = { navController.navigate(Routes.SCAN) },
                onReceiptClick = { id -> navController.navigate(Routes.detail(id)) },
                onExportClick = { navController.navigate(Routes.EXPORT) }
            )
        }
        composable(Routes.SCAN) {
            ScanScreen(
                onDone = { navController.popBackStack() },
                onCancel = { navController.popBackStack() }
            )
        }
        composable(
            Routes.DETAIL,
            arguments = listOf(navArgument("receiptId") { type = NavType.LongType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("receiptId") ?: 0L
            DetailScreen(receiptId = id, onBack = { navController.popBackStack() })
        }
        composable(Routes.EXPORT) {
            ExportScreen(onBack = { navController.popBackStack() })
        }
    }
}
