package com.tinhcd.myesalesmanager.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Payloads mirror the SFA app's: the same database functions read them.

@Serializable
data class ProductUnitDto(
    @SerialName("uom_code") val uomCode: String,
    @SerialName("uom_name") val uomName: String = "",
    @SerialName("conversion_rate") val conversionRate: Int = 1,
    val price: Long? = null,
    @SerialName("counted_qty") val countedQty: Int? = null,
)

@Serializable
data class VisitProductDto(
    val id: String,
    val code: String,
    val name: String,
    @SerialName("base_uom") val baseUom: String = "",
    val category: String? = null,
    @SerialName("previous_base_qty") val previousBaseQty: Int? = null,
    val units: List<ProductUnitDto> = emptyList(),
)

@Serializable
data class VisitProductsDto(
    @SerialName("customer_id") val customerId: String,
    @SerialName("previous_count_date") val previousCountDate: String? = null,
    val products: List<VisitProductDto> = emptyList(),
)

@Serializable
data class StockCountRefDto(val at: String, val lines: Int = 0)

@Serializable
data class OrderBriefDto(
    val id: String,
    @SerialName("order_no") val orderNo: String,
    @SerialName("total_amount") val totalAmount: Long = 0,
    @SerialName("discount_amount") val discountAmount: Long = 0,
)

@Serializable
data class VisitTasksDto(
    @SerialName("stock_count") val stockCount: StockCountRefDto? = null,
    val orders: List<OrderBriefDto> = emptyList(),
    @SerialName("posm_checked") val posmChecked: Int = 0,
    @SerialName("surveys_done") val surveysDone: Int = 0,
    @SerialName("min_minutes") val minMinutes: Int = 0,
)

@Serializable
data class QtyLineDto(
    @SerialName("product_id") val productId: String,
    @SerialName("uom_code") val uomCode: String,
    val qty: Int,
)

@Serializable
data class OrderLineDto(
    @SerialName("line_no") val lineNo: Int,
    @SerialName("product_id") val productId: String,
    @SerialName("uom_code") val uomCode: String,
    val qty: Int,
)

@Serializable
data class StockCountDto(
    val id: String,
    @SerialName("visit_id") val visitId: String,
    @SerialName("count_date") val countDate: String,
    @SerialName("client_created_at") val clientCreatedAt: String,
    val lines: List<QtyLineDto>,
)

@Serializable
data class OrderDto(
    val id: String,
    @SerialName("visit_id") val visitId: String,
    @SerialName("order_date") val orderDate: String,
    val note: String? = null,
    @SerialName("client_total_amount") val clientTotalAmount: Long,
    @SerialName("client_created_at") val clientCreatedAt: String,
    val lines: List<OrderLineDto>,
)

@Serializable
data class EarnedDto(
    @SerialName("sequence_name") val sequenceName: String? = null,
    @SerialName("break_name") val breakName: String? = null,
    @SerialName("discount_amount") val discountAmount: Long? = null,
)

@Serializable
data class PromotionPreviewDto(
    @SerialName("total_discount") val totalDiscount: Long = 0,
    val earned: List<EarnedDto> = emptyList(),
)

@Serializable
data class PosmPlacedDto(
    @SerialName("program_id") val programId: String,
    @SerialName("program_code") val programCode: String = "",
    @SerialName("posm_item_id") val itemId: String,
    @SerialName("item_code") val itemCode: String = "",
    @SerialName("item_name") val itemName: String = "",
    @SerialName("unit_name") val unitName: String = "Cái",
    @SerialName("placed_qty") val placedQty: Int = 0,
    @SerialName("counted_qty") val countedQty: Int? = null,
    val condition: String? = null,
    val remark: String? = null,
    @SerialName("photo_count") val photoCount: Int = 0,
)

@Serializable
data class PosmListDto(val placed: List<PosmPlacedDto> = emptyList())

@Serializable
data class PosmCheckDto(
    val id: String,
    @SerialName("visit_id") val visitId: String,
    @SerialName("program_id") val programId: String,
    @SerialName("posm_item_id") val itemId: String,
    @SerialName("counted_qty") val countedQty: Int,
    val condition: String,
    val remark: String? = null,
    @SerialName("client_created_at") val clientCreatedAt: String,
    val photos: List<AuditPhotoDto>,
)

@Serializable
data class MarketSurveyDto(
    val kind: String,
    val id: String,
    val code: String,
    val name: String,
    @SerialName("to_date") val toDate: String? = null,
    @SerialName("is_completed") val isCompleted: Boolean = false,
    @SerialName("item_count") val itemCount: Int = 0,
)

@Serializable
data class MarketSurveysDto(val surveys: List<MarketSurveyDto> = emptyList())

@Serializable
data class SurveyCriterionDto(
    val id: String,
    val name: String,
    val hint: String? = null,
    @SerialName("is_required") val isRequired: Boolean = true,
)

@Serializable
data class SurveyAnswerDto(val content: String = "")

@Serializable
data class SurveyPairingDto(
    @SerialName("product_id") val productId: String,
    @SerialName("product_name") val productName: String,
    @SerialName("competitor_product_id") val competitorProductId: String,
    @SerialName("competitor_product") val competitorProduct: String,
    @SerialName("competitor_name") val competitorName: String,
    val answers: Map<String, SurveyAnswerDto> = emptyMap(),
)

@Serializable
data class CompetitorSurveyDto(
    val id: String,
    val code: String,
    val name: String,
    val criteria: List<SurveyCriterionDto> = emptyList(),
    val items: List<SurveyPairingDto> = emptyList(),
)

@Serializable
data class SurveyAnswerInDto(
    @SerialName("product_id") val productId: String,
    @SerialName("competitor_product_id") val competitorProductId: String,
    @SerialName("criteria_id") val criteriaId: String,
    val content: String,
)

@Serializable
data class SurveySubmitDto(
    @SerialName("visit_id") val visitId: String,
    @SerialName("survey_id") val surveyId: String,
    @SerialName("client_created_at") val clientCreatedAt: String,
    val answers: List<SurveyAnswerInDto>,
)

/** One body for every in-store action; only the field the action names is sent. */
@Serializable
data class InStoreActionDto(
    val action: String,
    @SerialName("visit_id") val visitId: String? = null,
    val lines: List<QtyLineDto>? = null,
    val count: StockCountDto? = null,
    val order: OrderDto? = null,
    val check: PosmCheckDto? = null,
    val survey: SurveySubmitDto? = null,
)
