package com.tinhcd.myesalesmanager.domain.repository

import com.tinhcd.myesalesmanager.domain.DataResult
import com.tinhcd.myesalesmanager.domain.model.TargetSheet
import java.time.LocalDate

interface TargetRepository {

    suspend fun sheet(month: LocalDate, salespersonId: String): DataResult<TargetSheet>

    /**
     * Replaces the rep's month. A group or product missing from the maps, or
     * mapped to zero, has no target afterwards.
     */
    suspend fun save(
        salespersonId: String,
        month: LocalDate,
        revenueTarget: Long?,
        orderTarget: Int?,
        categories: Map<String, Long>,
        products: Map<String, Long>,
    ): DataResult<Unit>
}
