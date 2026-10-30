package com.tinhcd.myesalesmanager.domain.repository

import com.tinhcd.myesalesmanager.domain.DataResult
import com.tinhcd.myesalesmanager.domain.model.ReportData
import com.tinhcd.myesalesmanager.domain.model.ReportKind
import com.tinhcd.myesalesmanager.domain.model.ReportPeriod

interface ReportRepository {
    /**
     * @param period for a DAY report both ends are the day; for a MONTH report
     *   [ReportPeriod.from] is the first of the month.
     * @param salespersonId null reports the whole team.
     */
    suspend fun load(kind: ReportKind, period: ReportPeriod, salespersonId: String?): DataResult<ReportData>
}
