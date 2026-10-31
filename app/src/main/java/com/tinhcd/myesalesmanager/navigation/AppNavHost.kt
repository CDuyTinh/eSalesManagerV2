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
import com.tinhcd.myesalesmanager.domain.model.ReportKind
import com.tinhcd.myesalesmanager.domain.model.WorkPlanStatus
import com.tinhcd.myesalesmanager.feature.report.ARG_REPORT
import com.tinhcd.myesalesmanager.feature.report.ReportScreen
import com.tinhcd.myesalesmanager.domain.model.AppNotification
import com.tinhcd.myesalesmanager.domain.model.EdacStep
import com.tinhcd.myesalesmanager.domain.model.NotificationKind
import com.tinhcd.myesalesmanager.domain.model.TradeKind
import com.tinhcd.myesalesmanager.feature.personal.MyLeaveScreen
import com.tinhcd.myesalesmanager.feature.prepare.ARG_CUSTOMER_EDIT_ID
import com.tinhcd.myesalesmanager.feature.prepare.EditCustomerScreen
import com.tinhcd.myesalesmanager.feature.prepare.SalesFocusScreen
import com.tinhcd.myesalesmanager.feature.prepare.StockScreen
import com.tinhcd.myesalesmanager.feature.prepare.TeamCustomersScreen
import com.tinhcd.myesalesmanager.feature.personal.NotificationsScreen
import com.tinhcd.myesalesmanager.feature.personal.PersonalTarget
import com.tinhcd.myesalesmanager.feature.personal.WorkDayScreen
import com.tinhcd.myesalesmanager.feature.personal.WorkNotesScreen
import com.tinhcd.myesalesmanager.feature.edac.ARG_EDAC_CUSTOMER
import com.tinhcd.myesalesmanager.feature.edac.ARG_EDAC_STEP
import com.tinhcd.myesalesmanager.feature.edac.ARG_TRAINING_ID
import com.tinhcd.myesalesmanager.feature.edac.EdacFinishScreen
import com.tinhcd.myesalesmanager.feature.edac.EdacHomeScreen
import com.tinhcd.myesalesmanager.feature.edac.EdacResultScreen
import com.tinhcd.myesalesmanager.feature.edac.EdacStepScreen
import com.tinhcd.myesalesmanager.feature.edac.EdacTrainingScreen
import com.tinhcd.myesalesmanager.feature.route.ARG_ROUTE_ID
import com.tinhcd.myesalesmanager.feature.route.RouteDetailScreen
import com.tinhcd.myesalesmanager.feature.route.RoutesScreen
import com.tinhcd.myesalesmanager.feature.workwith.ARG_VISIT_ID
import com.tinhcd.myesalesmanager.feature.workwith.WorkWithRouteScreen
import com.tinhcd.myesalesmanager.feature.workwith.WorkWithVisitScreen
import com.tinhcd.myesalesmanager.feature.workwith.ARG_QTY_MODE
import com.tinhcd.myesalesmanager.feature.workwith.ARG_SURVEY_ID
import com.tinhcd.myesalesmanager.feature.workwith.CompetitorSurveyScreen
import com.tinhcd.myesalesmanager.feature.workwith.InStoreStep
import com.tinhcd.myesalesmanager.feature.workwith.MarketInfoScreen
import com.tinhcd.myesalesmanager.feature.workwith.PosmCheckScreen
import com.tinhcd.myesalesmanager.feature.workwith.ProductQtyScreen
import com.tinhcd.myesalesmanager.feature.workwith.QtyMode
import com.tinhcd.myesalesmanager.feature.auth.LoginScreen
import com.tinhcd.myesalesmanager.feature.shell.MainShell
import com.tinhcd.myesalesmanager.feature.workplan.ARG_PLAN_ID
import com.tinhcd.myesalesmanager.feature.workplan.ARG_PLAN_STATUS
import com.tinhcd.myesalesmanager.feature.workplan.AssignWorkPlanScreen
import com.tinhcd.myesalesmanager.feature.workplan.TaskTarget
import com.tinhcd.myesalesmanager.feature.workplan.TeamPlansScreen
import com.tinhcd.myesalesmanager.feature.workplan.WorkPlanDetailScreen

object Routes {
    const val LOGIN = "login"
    const val SHELL = "shell"

    const val APPROVAL_LEAVE = "approval/leave"
    const val APPROVAL_CUSTOMER = "approval/customer"
    const val APPROVAL_CUSTOMER_DETAIL = "approval/customer/{$ARG_CUSTOMER_ID}"
    const val APPROVAL_TRADE = "approval/trade/{$ARG_TRADE_KIND}"

    const val TEAM_PLANS = "plans?$ARG_PLAN_STATUS={$ARG_PLAN_STATUS}"
    const val PLAN_DETAIL = "plans/{$ARG_PLAN_ID}"
    const val ASSIGN_PLAN = "plans/assign"

    fun customerDetail(id: String) = "approval/customer/$id"
    fun trade(kind: String) = "approval/trade/$kind"
    fun plans(status: String? = null) = if (status == null) "plans" else "plans?$ARG_PLAN_STATUS=$status"
    fun planDetail(id: String) = "plans/$id"

    fun of(target: ApprovalTarget): String = when (target) {
        ApprovalTarget.Leave -> APPROVAL_LEAVE
        ApprovalTarget.Customer -> APPROVAL_CUSTOMER
        is ApprovalTarget.Trade -> trade(target.kind.code)
        ApprovalTarget.WorkPlan -> plans(WorkPlanStatus.SUBMITTED.code)
    }

    const val REPORT = "reports/{$ARG_REPORT}"
    fun report(kind: ReportKind) = "reports/${kind.code}"

    const val WORK_WITH = "workwith"
    const val WORK_WITH_VISIT = "workwith/visit/{$ARG_VISIT_ID}"
    const val WORK_WITH_QTY = "workwith/visit/{$ARG_VISIT_ID}/qty/{$ARG_QTY_MODE}"
    const val WORK_WITH_POSM = "workwith/visit/{$ARG_VISIT_ID}/posm"
    const val WORK_WITH_MARKET = "workwith/visit/{$ARG_VISIT_ID}/market"
    const val WORK_WITH_SURVEY = "workwith/visit/{$ARG_VISIT_ID}/market/{$ARG_SURVEY_ID}"
    fun inStore(step: InStoreStep, visitId: String) = when (step) {
        InStoreStep.STOCK -> "workwith/visit/$visitId/qty/${QtyMode.COUNT.code}"
        InStoreStep.ORDER -> "workwith/visit/$visitId/qty/${QtyMode.ORDER.code}"
        InStoreStep.POSM -> "workwith/visit/$visitId/posm"
        InStoreStep.MARKET -> "workwith/visit/$visitId/market"
    }
    fun workWithVisit(id: String) = "workwith/visit/$id"

    const val ROUTES = "routes"
    const val ROUTE_DETAIL = "routes/{$ARG_ROUTE_ID}"
    fun routeDetail(id: String) = "routes/$id"

    const val EDAC = "edac"
    const val EDAC_TRAINING = "edac/{$ARG_TRAINING_ID}"
    const val EDAC_STEP = "edac/{$ARG_TRAINING_ID}/step/{$ARG_EDAC_STEP}?$ARG_EDAC_CUSTOMER={$ARG_EDAC_CUSTOMER}"
    const val EDAC_FINISH = "edac/{$ARG_TRAINING_ID}/finish"
    const val EDAC_RESULT = "edac/{$ARG_TRAINING_ID}/result"
    fun edacTraining(id: String) = "edac/$id"
    fun edacStep(id: String, step: EdacStep, customerId: String? = null) =
        if (step == EdacStep.FINISH) "edac/$id/finish"
        else "edac/$id/step/${step.index}" + (customerId?.let { "?$ARG_EDAC_CUSTOMER=$it" } ?: "")
    fun edacResult(id: String) = "edac/$id/result"

    fun of(target: TaskTarget): String = when (target) {
        TaskTarget.TEAM_PLANS -> plans()
        TaskTarget.ASSIGN -> ASSIGN_PLAN
        TaskTarget.WORK_WITH -> WORK_WITH
        TaskTarget.ROUTES -> ROUTES
        TaskTarget.EDAC -> EDAC
        TaskTarget.NOTES -> of(PersonalTarget.NOTES)
        TaskTarget.STOCK -> STOCK
        TaskTarget.SALES_FOCUS -> SALES_FOCUS
        TaskTarget.EDIT_CUSTOMERS -> TEAM_CUSTOMERS
    }

    const val STOCK = "prepare/stock"
    const val SALES_FOCUS = "prepare/focus"
    const val TEAM_CUSTOMERS = "customers"
    const val EDIT_CUSTOMER = "customers/{$ARG_CUSTOMER_EDIT_ID}"
    fun editCustomer(id: String) = "customers/$id"

    fun of(target: PersonalTarget): String = when (target) {
        PersonalTarget.WORK_DAY -> "personal/workday"
        PersonalTarget.LEAVE -> "personal/leave"
        PersonalTarget.NOTES -> "personal/notes"
        PersonalTarget.NOTIFICATIONS -> "personal/notifications"
    }

    /** Where a bell entry leads: the request to rule on, or the manager's own item. */
    fun of(n: AppNotification): String? = when (n.kind) {
        NotificationKind.TEAM_LEAVE -> APPROVAL_LEAVE
        NotificationKind.TEAM_CUSTOMER -> customerDetail(n.sourceId)
        NotificationKind.TEAM_TRADE -> trade(
            when {
                n.title.startsWith("Đăng ký tích lũy") -> TradeKind.LOYALTY
                n.title.startsWith("Đăng ký POSM") -> TradeKind.POSM
                else -> TradeKind.DISPLAY
            }.code,
        )
        NotificationKind.TEAM_WORK_PLAN, NotificationKind.WORK_PLAN -> planDetail(n.sourceId)
        NotificationKind.LEAVE_RESULT -> of(PersonalTarget.LEAVE)
        NotificationKind.OTHER -> null
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
                onOpenTask = { nav.navigate(Routes.of(it)) },
                onOpenReport = { nav.navigate(Routes.report(it)) },
                onOpenPersonal = { nav.navigate(Routes.of(it)) },
            )
        }

        composable(Routes.STOCK) { StockScreen(onBack = back) }
        composable(Routes.SALES_FOCUS) { SalesFocusScreen(onBack = back) }
        composable(Routes.TEAM_CUSTOMERS) {
            TeamCustomersScreen(onBack = back, onOpen = { nav.navigate(Routes.editCustomer(it)) })
        }
        composable(
            Routes.EDIT_CUSTOMER,
            arguments = listOf(navArgument(ARG_CUSTOMER_EDIT_ID) { type = NavType.StringType }),
        ) { EditCustomerScreen(onBack = back) }

        composable(Routes.of(PersonalTarget.WORK_DAY)) { WorkDayScreen(onBack = back) }
        composable(Routes.of(PersonalTarget.LEAVE)) { MyLeaveScreen(onBack = back) }
        composable(Routes.of(PersonalTarget.NOTES)) { WorkNotesScreen(onBack = back) }
        composable(Routes.of(PersonalTarget.NOTIFICATIONS)) {
            NotificationsScreen(onBack = back, onOpen = { n -> Routes.of(n)?.let { nav.navigate(it) } })
        }

        composable(Routes.WORK_WITH) {
            WorkWithRouteScreen(onBack = back, onOpenVisit = { nav.navigate(Routes.workWithVisit(it)) })
        }
        composable(
            Routes.WORK_WITH_VISIT,
            arguments = listOf(navArgument(ARG_VISIT_ID) { type = NavType.StringType }),
        ) {
            WorkWithVisitScreen(
                onBack = back,
                onOpenStep = { step, visitId -> nav.navigate(Routes.inStore(step, visitId)) },
            )
        }
        composable(
            Routes.WORK_WITH_QTY,
            arguments = listOf(
                navArgument(ARG_VISIT_ID) { type = NavType.StringType },
                navArgument(ARG_QTY_MODE) { type = NavType.StringType },
            ),
        ) { ProductQtyScreen(onBack = back) }
        composable(
            Routes.WORK_WITH_POSM,
            arguments = listOf(navArgument(ARG_VISIT_ID) { type = NavType.StringType }),
        ) { PosmCheckScreen(onBack = back) }
        composable(
            Routes.WORK_WITH_MARKET,
            arguments = listOf(navArgument(ARG_VISIT_ID) { type = NavType.StringType }),
        ) {
            MarketInfoScreen(onBack = back, onOpen = { v, s -> nav.navigate("workwith/visit/$v/market/$s") })
        }
        composable(
            Routes.WORK_WITH_SURVEY,
            arguments = listOf(
                navArgument(ARG_VISIT_ID) { type = NavType.StringType },
                navArgument(ARG_SURVEY_ID) { type = NavType.StringType },
            ),
        ) { CompetitorSurveyScreen(onBack = back) }

        composable(Routes.EDAC) {
            EdacHomeScreen(
                onBack = back,
                onOpenTraining = { nav.navigate(Routes.edacTraining(it)) },
                onOpenResult = { nav.navigate(Routes.edacResult(it)) },
            )
        }
        composable(
            Routes.EDAC_TRAINING,
            arguments = listOf(navArgument(ARG_TRAINING_ID) { type = NavType.StringType }),
        ) {
            EdacTrainingScreen(
                onBack = back,
                onOpenStep = { id, step -> nav.navigate(Routes.edacStep(id, step)) },
                onOpenResult = { nav.navigate(Routes.edacResult(it)) },
            )
        }
        composable(
            Routes.EDAC_STEP,
            arguments = listOf(
                navArgument(ARG_TRAINING_ID) { type = NavType.StringType },
                navArgument(ARG_EDAC_STEP) { type = NavType.StringType },
                navArgument(ARG_EDAC_CUSTOMER) { type = NavType.StringType; nullable = true; defaultValue = null },
            ),
        ) {
            EdacStepScreen(
                onBack = back,
                onOpenOutlet = { id, step, customer -> nav.navigate(Routes.edacStep(id, step, customer)) },
            )
        }
        composable(
            Routes.EDAC_FINISH,
            arguments = listOf(navArgument(ARG_TRAINING_ID) { type = NavType.StringType }),
        ) { EdacFinishScreen(onBack = back, onDone = back) }
        composable(
            Routes.EDAC_RESULT,
            arguments = listOf(navArgument(ARG_TRAINING_ID) { type = NavType.StringType }),
        ) { EdacResultScreen(onBack = back) }

        composable(Routes.ROUTES) {
            RoutesScreen(onBack = back, onOpen = { nav.navigate(Routes.routeDetail(it)) })
        }
        composable(
            Routes.ROUTE_DETAIL,
            arguments = listOf(navArgument(ARG_ROUTE_ID) { type = NavType.StringType }),
        ) { RouteDetailScreen(onBack = back) }

        composable(
            Routes.REPORT,
            arguments = listOf(navArgument(ARG_REPORT) { type = NavType.StringType }),
        ) {
            ReportScreen(
                onBack = back,
                onOpenTraining = { id, finished ->
                    nav.navigate(if (finished) Routes.edacResult(id) else Routes.edacTraining(id))
                },
            )
        }

        composable(
            Routes.TEAM_PLANS,
            arguments = listOf(
                navArgument(ARG_PLAN_STATUS) { type = NavType.StringType; nullable = true; defaultValue = null },
            ),
        ) {
            TeamPlansScreen(
                onBack = back,
                onOpen = { nav.navigate(Routes.planDetail(it)) },
                onAssign = { nav.navigate(Routes.ASSIGN_PLAN) },
            )
        }
        // Declared before PLAN_DETAIL so "plans/assign" is not read as a plan id.
        composable(Routes.ASSIGN_PLAN) {
            AssignWorkPlanScreen(onDone = back, onBack = back)
        }
        composable(
            Routes.PLAN_DETAIL,
            arguments = listOf(navArgument(ARG_PLAN_ID) { type = NavType.StringType }),
        ) { WorkPlanDetailScreen(onBack = back) }

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
