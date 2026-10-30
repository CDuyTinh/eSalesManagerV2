package com.tinhcd.myesalesmanager.domain.model

/** How a report picks its period. */
enum class PeriodKind { DAY, MONTH, RANGE }

/**
 * The legacy manager's report menu. The first six run on the same database
 * functions as the rep's SFA reports; the last three wait on other phases.
 */
enum class ReportKind(val code: String, val title: String, val period: PeriodKind, val ready: Boolean = true) {
    ACTIVITIES("activities", "Hoạt động trong ngày", PeriodKind.DAY),
    SALES("sales", "Doanh số tháng (MTD)", PeriodKind.MONTH),
    ORDERS("orders", "Báo cáo đơn hàng", PeriodKind.RANGE),
    SKU("sku", "Sản lượng SKU", PeriodKind.RANGE),
    TRADE("trade", "Đăng ký chương trình", PeriodKind.RANGE),
    WORK("work", "Kết quả công việc", PeriodKind.RANGE),
    TRAINING("training", "Kết quả huấn luyện", PeriodKind.RANGE),
    DAILY_TRACKING("tracking", "Theo dõi bán hàng trong ngày", PeriodKind.DAY, ready = false),
    MAP("map", "Bản đồ vị trí nhân viên", PeriodKind.DAY, ready = false),
    ;

    companion object {
        fun of(code: String?): ReportKind = entries.firstOrNull { it.code == code } ?: ACTIVITIES
    }
}

/** A report's window, already resolved to ISO dates. */
data class ReportPeriod(val from: String, val to: String)

sealed interface ReportData

// --- Hoạt động trong ngày -------------------------------------------------------

data class ActivitySummary(
    val planned: Int,
    val visited: Int,
    val unplanned: Int,
    val strike: Int,
    val nonStrike: Int,
    val closed: Int,
    val orderAmount: Long,
)

data class ActivityMember(
    val code: String,
    val name: String,
    val planned: Int,
    val visited: Int,
    val strike: Int,
    val orderAmount: Long,
    val firstCheckIn: String?,
    val lastCheckOut: String?,
)

data class ActivityRow(
    val salesperson: StaffBrief?,
    val customerCode: String,
    val customerName: String,
    val planned: Boolean,
    val status: String,
    val checkInAt: String?,
    val checkOutAt: String?,
    val minutes: Int?,
    val orderAmount: Long,
)

data class ActivitiesReport(
    val summary: ActivitySummary,
    val members: List<ActivityMember>,
    val rows: List<ActivityRow>,
) : ReportData

// --- Doanh số tháng ---------------------------------------------------------------

data class SalesMember(val code: String, val name: String, val orders: Int, val revenue: Long, val target: Long?)
data class SalesCustomer(val code: String, val name: String, val orders: Int, val revenue: Long)
data class SalesProduct(val code: String, val name: String, val baseUom: String, val baseQty: Long, val revenue: Long)

data class SalesReport(
    val revenue: Long,
    val orderCount: Int,
    val target: Long?,
    val members: List<SalesMember>,
    val customers: List<SalesCustomer>,
    val products: List<SalesProduct>,
) : ReportData

// --- Đơn hàng -------------------------------------------------------------------

data class OrderRow(
    val orderNo: String,
    val orderDate: String,
    val salesperson: StaffBrief?,
    val customerCode: String,
    val customerName: String,
    val status: String,
    val lineCount: Int,
    val totalAmount: Long,
)

data class OrdersReport(
    val total: Int,
    val totalAmount: Long,
    val counts: Map<String, Int>,
    val items: List<OrderRow>,
) : ReportData

// --- SKU ------------------------------------------------------------------------

data class SkuRow(
    val code: String,
    val name: String,
    val uom: String,
    val qtyConfirmed: Int,
    val qtyPending: Int,
    val qtyTotal: Int,
    val amountTotal: Long,
)

data class SkuReport(val items: List<SkuRow>) : ReportData

// --- Đăng ký chương trình -------------------------------------------------------------

data class TradeRow(
    val kind: String,
    val salesperson: StaffBrief?,
    val registeredAt: String,
    val status: String,
    val portion: Int,
    val customerCode: String,
    val customerName: String,
    val programCode: String,
    val programName: String,
    val levelName: String?,
)

data class TradeReport(
    val displayCount: Int,
    val loyaltyCount: Int,
    val customerCount: Int,
    val items: List<TradeRow>,
) : ReportData

// --- Kết quả công việc ---------------------------------------------------------------

data class WorkRow(
    val salesperson: StaffBrief?,
    val content: String,
    val categoryName: String,
    val status: String,
    val fromAt: String,
    val toAt: String,
    val objectCount: Int,
    val outcome: String?,
)

data class WorkReport(
    val completed: Int,
    val cancelled: Int,
    val total: Int,
    val items: List<WorkRow>,
) : ReportData

// --- Kết quả huấn luyện ---------------------------------------------------------------

/** One rep's coaching in the period. [score] is the Đánh giá average, 0–100. */
data class TrainingMember(
    val code: String,
    val name: String,
    val plans: Int,
    val trained: Int,
    val untrained: Int,
    val score: Int?,
)

data class TrainingRow(
    val planId: String,
    val trainingId: String?,
    val content: String,
    val fromAt: String,
    val toAt: String,
    val rep: StaffBrief,
    /** completed, in_progress or not_started. */
    val status: String,
    val currentStep: Int?,
    val result: String?,
    val outlets: Int,
    val score: Int?,
)

data class TrainingReport(
    val plans: Int,
    val trained: Int,
    val untrained: Int,
    val outlets: Int,
    val score: Int?,
    val members: List<TrainingMember>,
    val items: List<TrainingRow>,
) : ReportData
