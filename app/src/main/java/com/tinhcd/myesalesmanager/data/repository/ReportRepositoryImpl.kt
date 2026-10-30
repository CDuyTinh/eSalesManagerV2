package com.tinhcd.myesalesmanager.data.repository

import com.tinhcd.myesalesmanager.data.remote.dto.StaffBriefDto
import com.tinhcd.myesalesmanager.data.remote.http.PostgrestException
import com.tinhcd.myesalesmanager.data.remote.http.orThrow
import com.tinhcd.myesalesmanager.data.remote.service.ReportService
import com.tinhcd.myesalesmanager.domain.AppError
import com.tinhcd.myesalesmanager.domain.DataResult
import com.tinhcd.myesalesmanager.domain.model.ActivitiesReport
import com.tinhcd.myesalesmanager.domain.model.ActivityMember
import com.tinhcd.myesalesmanager.domain.model.ActivityRow
import com.tinhcd.myesalesmanager.domain.model.ActivitySummary
import com.tinhcd.myesalesmanager.domain.model.OrderRow
import com.tinhcd.myesalesmanager.domain.model.OrdersReport
import com.tinhcd.myesalesmanager.domain.model.ReportData
import com.tinhcd.myesalesmanager.domain.model.ReportKind
import com.tinhcd.myesalesmanager.domain.model.ReportPeriod
import com.tinhcd.myesalesmanager.domain.model.SalesCustomer
import com.tinhcd.myesalesmanager.domain.model.SalesMember
import com.tinhcd.myesalesmanager.domain.model.SalesProduct
import com.tinhcd.myesalesmanager.domain.model.SalesReport
import com.tinhcd.myesalesmanager.domain.model.SkuReport
import com.tinhcd.myesalesmanager.domain.model.SkuRow
import com.tinhcd.myesalesmanager.domain.model.StaffBrief
import com.tinhcd.myesalesmanager.domain.model.TradeReport
import com.tinhcd.myesalesmanager.domain.model.TradeRow
import com.tinhcd.myesalesmanager.domain.model.WorkReport
import com.tinhcd.myesalesmanager.domain.model.WorkRow
import com.tinhcd.myesalesmanager.domain.repository.ReportRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportRepositoryImpl @Inject constructor(
    private val service: ReportService,
) : ReportRepository {

    override suspend fun load(kind: ReportKind, period: ReportPeriod, salespersonId: String?): DataResult<ReportData> = try {
        DataResult.Success(fetch(kind, period, salespersonId))
    } catch (e: PostgrestException) {
        DataResult.Failure(AppError.Server(e.status, e.message))
    } catch (e: Exception) {
        DataResult.Failure(e.toAppError())
    }

    private suspend fun fetch(kind: ReportKind, p: ReportPeriod, sp: String?): ReportData = when (kind) {
        ReportKind.ACTIVITIES -> service.activities(p.from, sp).orThrow().let { d ->
            ActivitiesReport(
                summary = with(d.summary) {
                    ActivitySummary(planned, visited, unplanned, strike, nonStrike, closed, orderAmount)
                },
                members = d.members.map {
                    ActivityMember(it.code, it.name, it.planned, it.visited, it.strike, it.orderAmount, it.firstCheckIn, it.lastCheckOut)
                },
                rows = d.rows.map {
                    ActivityRow(
                        it.salesperson?.toDomain(), it.customerCode, it.customerName, it.planned,
                        it.status, it.checkInAt, it.checkOutAt, it.minutes, it.orderAmount,
                    )
                },
            )
        }

        ReportKind.SALES -> service.sales(p.from, sp).orThrow().let { d ->
            SalesReport(
                revenue = d.revenue,
                orderCount = d.orderCount,
                target = d.target,
                members = d.members.map { SalesMember(it.code, it.name, it.orders, it.revenue, it.target) },
                customers = d.customers.map { SalesCustomer(it.code, it.name, it.orders, it.revenue) },
                products = d.products.map { SalesProduct(it.code, it.name, it.baseUom, it.baseQty, it.revenue) },
            )
        }

        ReportKind.ORDERS -> service.orders(p.from, p.to, sp).orThrow().let { d ->
            OrdersReport(
                total = d.total,
                totalAmount = d.totalAmount,
                counts = d.counts,
                items = d.items.map {
                    OrderRow(it.orderNo, it.orderDate, it.salesperson?.toDomain(), it.customerCode,
                        it.customerName, it.status, it.lineCount, it.totalAmount)
                },
            )
        }

        ReportKind.SKU -> SkuReport(
            service.sku(p.from, p.to, sp).orThrow().items.map {
                SkuRow(it.code, it.name, it.uom, it.qtyConfirmed, it.qtyPending, it.qtyTotal, it.amountTotal)
            },
        )

        ReportKind.TRADE -> service.trade(p.from, p.to, sp).orThrow().let { d ->
            TradeReport(
                displayCount = d.displayCount,
                loyaltyCount = d.loyaltyCount,
                customerCount = d.customerCount,
                items = d.items.map {
                    TradeRow(it.kind, it.salesperson?.toDomain(), it.registeredAt, it.status, it.portion,
                        it.customerCode, it.customerName, it.programCode, it.programName, it.levelName)
                },
            )
        }

        ReportKind.WORK -> service.work(p.from, p.to, sp).orThrow().let { d ->
            WorkReport(
                completed = d.completed,
                cancelled = d.cancelled,
                total = d.total,
                items = d.items.map {
                    WorkRow(it.salesperson?.toDomain(), it.content, it.categoryName, it.status,
                        it.fromAt, it.toAt, it.objectCount, it.outcome)
                },
            )
        }

        ReportKind.TRAINING, ReportKind.DAILY_TRACKING, ReportKind.MAP ->
            throw IllegalStateException("Báo cáo này chưa có")
    }
}

private fun StaffBriefDto.toDomain() = StaffBrief(id, code, name)
