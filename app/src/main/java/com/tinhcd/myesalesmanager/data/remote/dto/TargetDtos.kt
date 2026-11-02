package com.tinhcd.myesalesmanager.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Response of `GET /mgr-targets`: one rep's month. */
@Serializable
data class TargetSheetDto(
    val month: String,
    @SerialName("can_edit") val canEdit: Boolean = false,
    @SerialName("revenue_target") val revenueTarget: Long? = null,
    @SerialName("order_target") val orderTarget: Int? = null,
    val categories: List<TargetLineDto> = emptyList(),
    val products: List<TargetLineDto> = emptyList(),
)

/** A product group or product, with its target if it has one. */
@Serializable
data class TargetLineDto(
    val id: String,
    val code: String = "",
    val name: String = "",
    val target: Long? = null,
)

/** Body of `POST /mgr-targets`: the rep's month, whole. */
@Serializable
data class SaveTargetsDto(
    @SerialName("salesperson_id") val salespersonId: String,
    val month: String,
    @SerialName("revenue_target") val revenueTarget: Long?,
    @SerialName("order_target") val orderTarget: Int?,
    val categories: List<TargetAmountDto>,
    val products: List<TargetAmountDto>,
)

@Serializable
data class TargetAmountDto(val id: String, val target: Long)
