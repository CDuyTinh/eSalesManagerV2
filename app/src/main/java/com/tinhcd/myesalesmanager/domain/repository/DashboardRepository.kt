package com.tinhcd.myesalesmanager.domain.repository

import com.tinhcd.myesalesmanager.domain.DataResult
import com.tinhcd.myesalesmanager.domain.model.TeamDashboard

interface DashboardRepository {
    /** @param date ISO yyyy-MM-dd. @param salespersonId null for the whole team. @param supervisorId one SS's reps (AM / RM). */
    suspend fun overview(date: String, salespersonId: String?, supervisorId: String? = null): DataResult<TeamDashboard>
}
