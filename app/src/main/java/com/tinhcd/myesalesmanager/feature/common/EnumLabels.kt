package com.tinhcd.myesalesmanager.feature.common

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.tinhcd.myesalesmanager.R
import com.tinhcd.myesalesmanager.domain.model.ChartRange
import com.tinhcd.myesalesmanager.domain.model.EdacStep
import com.tinhcd.myesalesmanager.domain.model.PosmCondition
import com.tinhcd.myesalesmanager.domain.model.RepeatEvery
import com.tinhcd.myesalesmanager.domain.model.ReportKind
import com.tinhcd.myesalesmanager.domain.model.VisitStatus
import com.tinhcd.myesalesmanager.domain.model.WorkObjectKind
import com.tinhcd.myesalesmanager.domain.model.WorkPlanStatus
import com.tinhcd.myesalesmanager.feature.report.RangePreset
import com.tinhcd.myesalesmanager.feature.workwith.InStoreStep
import com.tinhcd.myesalesmanager.feature.workwith.QtyMode

/*
 * Enum names in the app's language. Each enum keeps its Vietnamese text as
 * vi<Name> for logs and tests; screens call these.
 */

@Composable
fun ChartRange.label(): String = stringResource(
    when (this) {
        ChartRange.THIS_WEEK -> R.string.enum_chart_range_this_week
        ChartRange.LAST_WEEK -> R.string.enum_chart_range_last_week
        ChartRange.THIS_MONTH -> R.string.enum_chart_range_this_month
    },
)

@Composable
fun EdacStep.title(): String = stringResource(
    when (this) {
        EdacStep.PREPARE -> R.string.enum_edac_step_prepare_title
        EdacStep.OBSERVE -> R.string.enum_edac_step_observe_title
        EdacStep.EXPLAIN -> R.string.enum_edac_step_explain_title
        EdacStep.DEMONSTRATE -> R.string.enum_edac_step_demonstrate_title
        EdacStep.APPLY -> R.string.enum_edac_step_apply_title
        EdacStep.EVALUATE -> R.string.enum_edac_step_evaluate_title
        EdacStep.END_OF_DAY -> R.string.enum_edac_step_end_of_day_title
        EdacStep.FINISH -> R.string.enum_edac_step_finish_title
    },
)

@Composable
fun EdacStep.hint(): String = stringResource(
    when (this) {
        EdacStep.PREPARE -> R.string.enum_edac_step_prepare_hint
        EdacStep.OBSERVE -> R.string.enum_edac_step_observe_hint
        EdacStep.EXPLAIN -> R.string.enum_edac_step_explain_hint
        EdacStep.DEMONSTRATE -> R.string.enum_edac_step_demonstrate_hint
        EdacStep.APPLY -> R.string.enum_edac_step_apply_hint
        EdacStep.EVALUATE -> R.string.enum_edac_step_evaluate_hint
        EdacStep.END_OF_DAY -> R.string.enum_edac_step_end_of_day_hint
        EdacStep.FINISH -> R.string.enum_edac_step_finish_hint
    },
)

@Composable
fun PosmCondition.label(): String = stringResource(
    when (this) {
        PosmCondition.USABLE -> R.string.enum_posm_condition_usable
        PosmCondition.REPAIRABLE -> R.string.enum_posm_condition_repairable
        PosmCondition.UNUSABLE -> R.string.enum_posm_condition_unusable
    },
)

/** The title's resource, for code with a Context but no composition (the export). */
@StringRes
fun ReportKind.titleRes(): Int = when (this) {
        ReportKind.ACTIVITIES -> R.string.enum_report_kind_activities_title
        ReportKind.SALES -> R.string.enum_report_kind_sales_title
        ReportKind.ORDERS -> R.string.enum_report_kind_orders_title
        ReportKind.SKU -> R.string.enum_report_kind_sku_title
        ReportKind.TRADE -> R.string.enum_report_kind_trade_title
        ReportKind.WORK -> R.string.enum_report_kind_work_title
        ReportKind.TRAINING -> R.string.enum_report_kind_training_title
        ReportKind.DAILY_TRACKING -> R.string.enum_report_kind_daily_tracking_title
        ReportKind.MAP -> R.string.enum_report_kind_map_title
}

@Composable
fun ReportKind.title(): String = stringResource(titleRes())

@Composable
fun WorkPlanStatus.label(): String = stringResource(
    when (this) {
        WorkPlanStatus.DRAFT -> R.string.enum_work_plan_status_draft
        WorkPlanStatus.SUBMITTED -> R.string.enum_work_plan_status_submitted
        WorkPlanStatus.APPROVED -> R.string.enum_work_plan_status_approved
        WorkPlanStatus.REJECTED -> R.string.enum_work_plan_status_rejected
        WorkPlanStatus.COMPLETED -> R.string.enum_work_plan_status_completed
        WorkPlanStatus.CANCELLED -> R.string.enum_work_plan_status_cancelled
    },
)

@Composable
fun WorkObjectKind.label(): String = stringResource(
    when (this) {
        WorkObjectKind.CUSTOMER -> R.string.enum_work_object_kind_customer
        WorkObjectKind.SALESPERSON -> R.string.enum_work_object_kind_salesperson
        WorkObjectKind.BRANCH -> R.string.enum_work_object_kind_branch
    },
)

@Composable
fun RepeatEvery.label(): String = stringResource(
    when (this) {
        RepeatEvery.DAY -> R.string.enum_repeat_every_day
        RepeatEvery.WEEK -> R.string.enum_repeat_every_week
    },
)

@Composable
fun VisitStatus.label(): String = stringResource(
    when (this) {
        VisitStatus.PLANNED -> R.string.enum_visit_status_planned
        VisitStatus.IN_PROGRESS -> R.string.enum_visit_status_in_progress
        VisitStatus.COMPLETED -> R.string.enum_visit_status_completed
        VisitStatus.NO_ORDER -> R.string.enum_visit_status_no_order
        VisitStatus.CLOSED -> R.string.enum_visit_status_closed
    },
)

@Composable
fun RangePreset.label(): String = stringResource(
    when (this) {
        RangePreset.THIS_MONTH -> R.string.enum_range_preset_this_month
        RangePreset.LAST_MONTH -> R.string.enum_range_preset_last_month
        RangePreset.LAST_7 -> R.string.enum_range_preset_last_7
        RangePreset.LAST_30 -> R.string.enum_range_preset_last_30
    },
)

@Composable
fun QtyMode.title(): String = stringResource(
    when (this) {
        QtyMode.COUNT -> R.string.enum_qty_mode_count_title
        QtyMode.ORDER -> R.string.enum_qty_mode_order_title
    },
)

@Composable
fun InStoreStep.title(): String = stringResource(
    when (this) {
        InStoreStep.STOCK -> R.string.enum_in_store_step_stock_title
        InStoreStep.ORDER -> R.string.enum_in_store_step_order_title
        InStoreStep.POSM -> R.string.enum_in_store_step_posm_title
        InStoreStep.MARKET -> R.string.enum_in_store_step_market_title
        InStoreStep.FEEDBACK -> R.string.enum_in_store_step_feedback_title
    },
)
