package com.tinhcd.myesalesmanager.domain.model

/**
 * The in-store steps of a work-with call: counting the outlet's stock, taking
 * the order for the rep, checking POSM, competitor surveys. Legacy
 * STOCK_OUTLET, TAKE_ORDER, POSM_STATUS, MARKET_INFO.
 */

data class ProductUnit(
    val uomCode: String,
    val uomName: String,
    val conversionRate: Int,
    /** For this outlet's class today; null when the unit is not sold. */
    val price: Long?,
    /** What this visit has counted in this unit so far. */
    val countedQty: Int?,
)

data class VisitProduct(
    val id: String,
    val code: String,
    val name: String,
    val baseUom: String,
    val category: String?,
    /** The outlet's previous count, in base units; null if never counted. */
    val previousBaseQty: Int?,
    /** Largest unit first. */
    val units: List<ProductUnit>,
)

data class VisitProducts(val customerId: String, val previousCountDate: String?, val products: List<VisitProduct>)

data class OrderBrief(val id: String, val orderNo: String, val totalAmount: Long, val discountAmount: Long)

/** What has been done on the call so far. */
data class VisitTasks(
    val stockCountedAt: String?,
    val stockCountLines: Int,
    val orders: List<OrderBrief>,
    val posmChecked: Int,
    val surveysDone: Int,
    /** Minutes the supervisor must stay before checking out; 0 means no limit. */
    val minMinutes: Int,
)

/** A quantity entered against one product in one unit. */
data class QtyLine(val productId: String, val uomCode: String, val qty: Int)

/** What the basket earns, as the server would book it. */
data class PromotionPreview(val totalDiscount: Long, val earned: List<String>)

data class PosmPlaced(
    val programId: String,
    val programCode: String,
    val itemId: String,
    val itemCode: String,
    val itemName: String,
    val unitName: String,
    val placedQty: Int,
    val countedQty: Int?,
    val condition: String?,
    val remark: String?,
    val photoCount: Int,
)

/** usable | repairable | unusable: the legacy's three POSM results. */
enum class PosmCondition(val code: String, val label: String) {
    USABLE("usable", "Dùng tốt"),
    REPAIRABLE("repairable", "Cần sửa"),
    UNUSABLE("unusable", "Hỏng"),
}

data class PosmCheck(
    val visitId: String,
    val item: PosmPlaced,
    val countedQty: Int,
    val condition: PosmCondition,
    val remark: String?,
    val photoPaths: List<String>,
)

data class MarketSurvey(
    /** competitor or market. Only competitor surveys are filled in from this app. */
    val kind: String,
    val id: String,
    val code: String,
    val name: String,
    val toDate: String?,
    val completed: Boolean,
    val itemCount: Int,
)

data class SurveyCriterion(val id: String, val name: String, val hint: String?, val required: Boolean)

data class SurveyPairing(
    val productId: String,
    val productName: String,
    val competitorProductId: String,
    val competitorProduct: String,
    val competitorName: String,
    /** Criterion id → what was answered on this visit. */
    val answers: Map<String, String>,
)

data class CompetitorSurvey(
    val id: String,
    val code: String,
    val name: String,
    val criteria: List<SurveyCriterion>,
    val pairings: List<SurveyPairing>,
)
