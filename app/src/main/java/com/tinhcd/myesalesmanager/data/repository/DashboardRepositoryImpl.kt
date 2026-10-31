package com.tinhcd.myesalesmanager.data.repository

import com.tinhcd.myesalesmanager.data.remote.dto.SalesPointDto
import com.tinhcd.myesalesmanager.data.remote.http.PostgrestException
import com.tinhcd.myesalesmanager.data.remote.http.orThrow
import com.tinhcd.myesalesmanager.data.remote.service.DashboardService
import com.tinhcd.myesalesmanager.domain.AppError
import com.tinhcd.myesalesmanager.domain.DataResult
import com.tinhcd.myesalesmanager.domain.model.ChartRange
import com.tinhcd.myesalesmanager.domain.model.MemberFigures
import com.tinhcd.myesalesmanager.domain.model.SalesPoint
import com.tinhcd.myesalesmanager.domain.model.TeamDashboard
import com.tinhcd.myesalesmanager.domain.model.TeamMonth
import com.tinhcd.myesalesmanager.domain.model.TeamToday
import com.tinhcd.myesalesmanager.domain.repository.DashboardRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DashboardRepositoryImpl @Inject constructor(
    private val service: DashboardService,
) : DashboardRepository {

    override suspend fun overview(date: String, salespersonId: String?, supervisorId: String?): DataResult<TeamDashboard> = try {
        val d = service.overview(date, salespersonId, supervisorId).orThrow()
        DataResult.Success(
            TeamDashboard(
                date = d.date,
                teamSize = d.teamSize,
                today = with(d.today) {
                    TeamToday(revenue, orderCount, visitDone, visitPlanned, skuPerOrder, activeReps)
                },
                month = with(d.month) {
                    TeamMonth(
                        revenue, revenueTarget, orderCount, orderTarget,
                        visitDone, visitPlanned, skuPerOrder, vpo, newCustomers,
                    )
                },
                members = d.members.map {
                    MemberFigures(
                        it.id, it.code, it.name, it.todayRevenue, it.todayOrders, it.todayVisits,
                        it.todayPlanned, it.monthRevenue, it.monthOrders, it.revenueTarget,
                    )
                },
                charts = mapOf(
                    ChartRange.THIS_WEEK to d.charts.thisWeek.points(),
                    ChartRange.LAST_WEEK to d.charts.lastWeek.points(),
                    ChartRange.THIS_MONTH to d.charts.thisMonth.points(),
                ),
            ),
        )
    } catch (e: PostgrestException) {
        DataResult.Failure(AppError.Server(e.status, e.message))
    } catch (e: Exception) {
        DataResult.Failure(e.toAppError())
    }
}

private fun List<SalesPointDto>?.points() = orEmpty().map { SalesPoint(it.title, it.actual) }
