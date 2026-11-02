package com.tinhcd.myesalesmanager.domain.model

import java.time.LocalDate

/**
 * One rep's month of targets: the overall revenue and order figures, and the
 * finer ones by product group and by product.
 *
 * @property canEdit an AM or RM, and a month not yet over; anyone else reads.
 */
data class TargetSheet(
    val month: LocalDate,
    val canEdit: Boolean,
    val revenueTarget: Long?,
    val orderTarget: Int?,
    /** Every active group, each with its target or null. */
    val categories: List<TargetLine>,
    /** Every active product, each with its target or null. */
    val products: List<TargetLine>,
)

data class TargetLine(
    val id: String,
    val code: String,
    val name: String,
    val target: Long?,
)
