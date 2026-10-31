package com.tinhcd.myesalesmanager.data.repository

import com.tinhcd.myesalesmanager.data.remote.dto.RouteActionDto
import com.tinhcd.myesalesmanager.data.remote.http.PostgrestException
import com.tinhcd.myesalesmanager.data.remote.http.orThrow
import com.tinhcd.myesalesmanager.data.remote.service.RouteService
import com.tinhcd.myesalesmanager.domain.AppError
import com.tinhcd.myesalesmanager.domain.DataResult
import com.tinhcd.myesalesmanager.domain.model.RouteCandidate
import com.tinhcd.myesalesmanager.domain.model.RouteDetail
import com.tinhcd.myesalesmanager.domain.model.RouteHistoryEntry
import com.tinhcd.myesalesmanager.domain.model.RouteOutlet
import com.tinhcd.myesalesmanager.domain.model.SalesRoute
import com.tinhcd.myesalesmanager.domain.model.StaffBrief
import com.tinhcd.myesalesmanager.domain.repository.RouteRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RouteRepositoryImpl @Inject constructor(
    private val service: RouteService,
) : RouteRepository {

    override suspend fun routes() = call {
        service.routes().orThrow().map {
            SalesRoute(
                it.id, it.code, it.name, it.isActive,
                it.salesperson?.let { s -> StaffBrief(s.id, s.code, s.name) },
                it.customerCount, it.perWeekday,
            )
        }
    }

    override suspend fun detail(routeId: String) = call {
        val d = service.detail(routeId).orThrow()
        RouteDetail(
            id = d.id,
            code = d.code,
            name = d.name,
            rep = d.salesperson?.let { StaffBrief(it.id, it.code, it.name) },
            outlets = d.customers.map {
                RouteOutlet(
                    customerId = it.customerId, code = it.code, name = it.name, address = it.address,
                    lat = it.lat, lng = it.lng, visitOrder = it.visitOrder,
                    weekdays = it.weekdays, weeks = it.weeks, lastVisit = it.lastVisit, lastOrder = it.lastOrder,
                )
            },
            history = d.history.map { RouteHistoryEntry(it.at, it.text, it.actor) },
        )
    }

    override suspend fun candidates(routeId: String, query: String?) = call {
        service.candidates(routeId, query?.trim()?.takeIf { it.isNotEmpty() }).orThrow().map {
            RouteCandidate(it.customerId, it.code, it.name, it.address, it.onRoutes)
        }
    }

    override suspend fun setOutlet(routeId: String, customerId: String, weekdays: List<Int>, weeks: List<Int>) = call {
        service.act(
            RouteActionDto("set", routeId, customerId = customerId, weekdays = weekdays, weeks = weeks),
        ).orThrow()
        Unit
    }

    override suspend fun removeOutlet(routeId: String, customerId: String) = call {
        service.act(RouteActionDto("remove", routeId, customerId = customerId)).orThrow()
        Unit
    }

    override suspend fun reorder(routeId: String, customerIds: List<String>) = call {
        service.act(RouteActionDto("reorder", routeId, customerIds = customerIds)).orThrow()
        Unit
    }

    override suspend fun assign(routeId: String, salespersonId: String, note: String?) = call {
        service.act(
            RouteActionDto(
                "assign", routeId, salespersonId = salespersonId,
                note = note?.trim()?.takeIf { it.isNotEmpty() },
            ),
        ).orThrow()
        Unit
    }

    private inline fun <T> call(block: () -> T): DataResult<T> = try {
        DataResult.Success(block())
    } catch (e: PostgrestException) {
        DataResult.Failure(
            if (e.status == 409) AppError.Rule("server", e.message) else AppError.Server(e.status, e.message),
        )
    } catch (e: Exception) {
        DataResult.Failure(e.toAppError())
    }
}
