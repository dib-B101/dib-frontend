package com.ssafy.dib.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.ssafy.dib.core.ui.DibMainTab
import com.ssafy.dib.feature.auction.ProductDetailScreen
import com.ssafy.dib.feature.feed.AuctionFeedScreen
import com.ssafy.dib.feature.home.HomeScreen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onProductClick = { productId ->
                    navController.navigate(Screen.ProductDetail.createRoute(productId))
                },
                onTabSelected = { tab ->
                    when (tab) {
                        DibMainTab.Home -> navController.popBackStack(Screen.Home.route, inclusive = false)
                        DibMainTab.Feed -> navController.navigate(Screen.Feed.route) { launchSingleTop = true }
                        else -> Unit
                    }
                }
            )
        }
        composable(Screen.Feed.route) {
            AuctionFeedScreen(
                onProductClick = { productId ->
                    navController.navigate(Screen.ProductDetail.createRoute(productId))
                },
                onTabSelected = { tab ->
                    when (tab) {
                        DibMainTab.Home -> navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = false }
                            launchSingleTop = true
                        }
                        DibMainTab.Feed -> Unit
                        else -> Unit
                    }
                }
            )
        }
        composable(
            route = Screen.ProductDetail.route,
            arguments = listOf(navArgument("productId") { type = NavType.StringType })
        ) { backStackEntry ->
            ProductDetailScreen(
                productId = backStackEntry.arguments?.getString("productId").orEmpty(),
                onBack = navController::navigateUp
            )
        }
    }
}
