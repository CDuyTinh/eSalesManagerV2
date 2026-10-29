package com.tinhcd.myesalesmanager.domain.repository

import com.tinhcd.myesalesmanager.domain.DataResult
import com.tinhcd.myesalesmanager.domain.model.TeamDashboard

interface DashboardRepository {
    /** @param date ISO yyyy-MM-dd. @param salespersonId null for the whole team. */
    suspend fun overview(date: String, salespersonId: String?): DataResult<TeamDashboard>
}
