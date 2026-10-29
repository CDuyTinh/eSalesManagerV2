package com.tinhcd.myesalesmanager.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tinhcd.myesalesmanager.feature.approval.ARG_CUSTOMER_ID
import com.tinhcd.myesalesmanager.feature.approval.ARG_TRADE_KIND
import com.tinhcd.myesalesmanager.feature.approval.ApprovalTarget
import com.tinhcd.myesalesmanager.feature.approval.CustomerApprovalDetailScreen
import com.tinhcd.myesalesmanager.feature.approval.CustomerApprovalScreen
import com.tinhcd.myesalesmanager.feature.approval.LeaveApprovalScreen
import com.tinhcd.myesalesmanager.feature.approval.TradeApprovalScreen
import com.tinhcd.myesalesmanager.feature.auth.LoginScreen
import com.tinhcd.myesalesmanager.feature.shell.MainShell

object Routes {
    const val LOGIN = "login"
    const val SHELL = "shell"

    const val APPROVAL_LEAVE = "approval/leave"
    const val APPROVAL_CUSTOMER = "approval/customer"
    const val APPROVAL_CUSTOMER_DETAIL = "approval/customer/{$ARG_CUSTOMER_ID}"
    const val APPROVAL_TRADE = "approval/trade/{$ARG_TRADE_KIND}"

    fun customerDetail(id: String) = "approval/customer/$id"
    fun trade(kind: String) = "approval/trade/$kind"

    fun of(target: ApprovalTarget): String = when (target) {
        ApprovalTarget.Leave -> APPROVAL_LEAVE
        ApprovalTarget.Customer -> APPROVAL_CUSTOMER
        is ApprovalTarget.Trade -> trade(target.kind.code)
    }
}

@Composable
fun AppNavHost(startDestination: String) {
    val nav = rememberNavController()
    val back: () -> Unit = { nav.popBackStack() }

    NavHost(navController = nav, startDestination = startDestination) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onSignedIn = {
                    nav.navigate(Routes.SHELL) { popUpTo(Routes.LOGIN) { inclusive = true } }
                },
            )
        }
        composable(Routes.SHELL) {
            MainShell(
                onSignedOut = {
                    nav.navigate(Routes.LOGIN) { popUpTo(Routes.SHELL) { inclusive = true } }
                },
                onOpenApproval = { nav.navigate(Routes.of(it)) },
            )
        }

        composable(Routes.APPROVAL_LEAVE) { LeaveApprovalScreen(onBack = back) }
        composable(Routes.APPROVAL_CUSTOMER) {
            CustomerApprovalScreen(
                onBack = back,
                onOpen = { nav.navigate(Routes.customerDetail(it)) },
            )
        }
        composable(
            Routes.APPROVAL_CUSTOMER_DETAIL,
            arguments = listOf(navArgument(ARG_CUSTOMER_ID) { type = NavType.StringType }),
        ) { CustomerApprovalDetailScreen(onBack = back) }
        composable(
            Routes.APPROVAL_TRADE,
            arguments = listOf(navArgument(ARG_TRADE_KIND) { type = NavType.StringType }),
        ) { TradeApprovalScreen(onBack = back) }
    }
}
