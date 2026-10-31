package com.tinhcd.myesalesmanager.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class SiteDto(
    @SerialName("site_id") val siteId: String,
    val code: String,
    val name: String,
    val address: String? = null,
)

@Serializable
data class StockItemDto(
    @SerialName("product_code") val productCode: String,
    @SerialName("product_name") val productName: String,
    @SerialName("base_uom") val baseUom: String = "",
    @SerialName("qty_base") val qtyBase: Long = 0,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class SiteStockDto(
    val sites: List<SiteDto> = emptyList(),
    @SerialName("site_id") val siteId: String? = null,
    val items: List<StockItemDto> = emptyList(),
)

@Serializable
data class FocusRepDto(
    val code: String,
    val name: String,
    @SerialName("sold_base_qty") val soldBaseQty: Long = 0,
    val outlets: Int = 0,
)

@Serializable
data class FocusProductDto(
    @SerialName("product_code") val productCode: String,
    @SerialName("product_name") val productName: String,
    @SerialName("base_uom") val baseUom: String = "",
    @SerialName("from_date") val fromDate: String,
    @SerialName("to_date") val toDate: String,
    @SerialName("target_base_qty") val targetBaseQty: Long? = null,
    @SerialName("team_target") val teamTarget: Long? = null,
    val note: String? = null,
    @SerialName("sold_base_qty") val soldBaseQty: Long = 0,
    val outlets: Int = 0,
    val reps: List<FocusRepDto> = emptyList(),
)

@Serializable
data class RunningProgramDto(
    val kind: String,
    val code: String,
    val name: String,
    @SerialName("from_date") val fromDate: String,
    @SerialName("to_date") val toDate: String,
    val registered: Int = 0,
)

@Serializable
data class SalesFocusDto(
    val products: List<FocusProductDto> = emptyList(),
    val programs: List<RunningProgramDto> = emptyList(),
)

@Serializable
data class TeamCustomerDto(
    val id: String,
    val code: String,
    val name: String,
    val address: String? = null,
    val phone: String? = null,
    @SerialName("contact_name") val contactName: String? = null,
    val routes: String? = null,
    @SerialName("edited_at") val editedAt: String? = null,
)

@Serializable
data class NamedOptionDto(val id: String, val name: String)

@Serializable
data class CustomerOptionsDto(
    val classes: List<NamedOptionDto> = emptyList(),
    val channels: List<NamedOptionDto> = emptyList(),
    @SerialName("shop_types") val shopTypes: List<NamedOptionDto> = emptyList(),
)

@Serializable
data class CustomerChangeDto(
    val at: String,
    val actor: String = "",
    /** { field: { old, new } }; values are whatever JSON type the column has. */
    val changes: Map<String, Map<String, JsonElement>> = emptyMap(),
)

@Serializable
data class CustomerEditDto(
    val id: String,
    val code: String,
    val name: String,
    @SerialName("contact_name") val contactName: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    @SerialName("class_id") val classId: String? = null,
    @SerialName("channel_id") val channelId: String? = null,
    @SerialName("shop_type_id") val shopTypeId: String? = null,
    val options: CustomerOptionsDto = CustomerOptionsDto(),
    val history: List<CustomerChangeDto> = emptyList(),
)

@Serializable
data class CustomerUpdateDto(val id: String, val changes: Map<String, JsonElement>)

@Serializable
data class CustomerUpdatedDto(val changed: Int = 0)
