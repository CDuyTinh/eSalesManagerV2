package com.tinhcd.myesalesmanager.domain.repository

import com.tinhcd.myesalesmanager.domain.DataResult
import com.tinhcd.myesalesmanager.domain.model.RouteCandidate
import com.tinhcd.myesalesmanager.domain.model.RouteDetail
import com.tinhcd.myesalesmanager.domain.model.SalesRoute

/** Lập tuyến (MCP) & phân tuyến for the routes the team runs. */
interface RouteRepository {
    suspend fun routes(): DataResult<List<SalesRoute>>

    suspend fun detail(routeId: String): DataResult<RouteDetail>

    suspend fun candidates(routeId: String, query: String?): DataResult<List<RouteCandidate>>

    /** Adds the outlet to the route, or changes its days; [weeks] empty = every week. */
    suspend fun setOutlet(routeId: String, customerId: String, weekdays: List<Int>, weeks: List<Int>): DataResult<Unit>

    suspend fun removeOutlet(routeId: String, customerId: String): DataResult<Unit>

    /** Hands the route to another rep of the team. */
    suspend fun assign(routeId: String, salespersonId: String, note: String?): DataResult<Unit>
}
