package com.tinhcd.myesalesmanager.domain.repository

import com.tinhcd.myesalesmanager.domain.DataResult
import com.tinhcd.myesalesmanager.domain.model.OrdersReport
import com.tinhcd.myesalesmanager.domain.model.ReportData
import com.tinhcd.myesalesmanager.domain.model.ReportKind
import com.tinhcd.myesalesmanager.domain.model.ReportPeriod

interface ReportRepository {
    /**
     * @param period for a DAY report both ends are the day; for a MONTH report
     *   [ReportPeriod.from] is the first of the month.
     * @param salespersonId null reports the whole team.
     * @param supervisorId with no rep, one SS's reps (AM / RM).
     * @param branchIds with no rep, only these distributors' reps (a region, an area or one NPP).
     */
    suspend fun load(kind: ReportKind, period: ReportPeriod, salespersonId: String?, supervisorId: String? = null, branchIds: List<String>? = null): DataResult<ReportData>

    /** The next page of the orders report, from [offset] (the orders already shown). */
    suspend fun moreOrders(
        period: ReportPeriod,
        salespersonId: String?,
        supervisorId: String?,
        branchIds: List<String>?,
        offset: Int,
    ): DataResult<OrdersReport>
}
