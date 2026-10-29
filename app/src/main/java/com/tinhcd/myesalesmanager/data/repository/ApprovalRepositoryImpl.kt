package com.tinhcd.myesalesmanager.data.repository

import com.tinhcd.myesalesmanager.data.remote.dto.ApprovalPageDto
import com.tinhcd.myesalesmanager.data.remote.dto.CustomerApprovalDto
import com.tinhcd.myesalesmanager.data.remote.dto.DecisionRequestDto
import com.tinhcd.myesalesmanager.data.remote.dto.LeaveApprovalDto
import com.tinhcd.myesalesmanager.data.remote.dto.StaffBriefDto
import com.tinhcd.myesalesmanager.data.remote.dto.TradeApprovalDto
import com.tinhcd.myesalesmanager.data.remote.http.PostgrestException
import com.tinhcd.myesalesmanager.data.remote.http.orThrow
import com.tinhcd.myesalesmanager.data.remote.service.ApprovalService
import com.tinhcd.myesalesmanager.domain.AppError
import com.tinhcd.myesalesmanager.domain.DataResult
import com.tinhcd.myesalesmanager.domain.model.ApprovalCounts
import com.tinhcd.myesalesmanager.domain.model.ApprovalPage
import com.tinhcd.myesalesmanager.domain.model.ApprovalPhoto
import com.tinhcd.myesalesmanager.domain.model.ApprovalStatus
import com.tinhcd.myesalesmanager.domain.model.CustomerApproval
import com.tinhcd.myesalesmanager.domain.model.CustomerApprovalDetail
import com.tinhcd.myesalesmanager.domain.model.Decision
import com.tinhcd.myesalesmanager.domain.model.LeaveApproval
import com.tinhcd.myesalesmanager.domain.model.RouteOption
import com.tinhcd.myesalesmanager.domain.model.StaffBrief
import com.tinhcd.myesalesmanager.domain.model.TradeApproval
import com.tinhcd.myesalesmanager.domain.model.TradeKind
import com.tinhcd.myesalesmanager.domain.repository.ApprovalRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApprovalRepositoryImpl @Inject constructor(
    private val service: ApprovalService,
) : ApprovalRepository {

    override suspend fun counts(): DataResult<ApprovalCounts> = call {
        service.counts().orThrow().let {
            ApprovalCounts(it.leave, it.customer, it.display, it.loyalty, it.posm)
        }
    }

    override suspend fun leaves(status: ApprovalStatus?) = call {
        service.leaves(status?.code).orThrow().toPage { it.toDomain() }
    }

    override suspend fun customers(status: ApprovalStatus?, query: String?) = call {
        service.customers(status?.code, query?.takeIf { it.isNotBlank() })
            .orThrow().toPage { it.toDomain() }
    }

    override suspend fun customerDetail(id: String) = call {
        val d = service.customerDetail(id).orThrow()
        CustomerApprovalDetail(
            summary = CustomerApproval(
                id = d.id, code = d.code, name = d.name, phone = d.phone, address = d.address,
                status = ApprovalStatus.of(d.status), note = d.note, decisionNote = d.decisionNote,
                createdAt = d.createdAt, photoCount = d.photos.size,
                salesperson = d.salesperson?.toDomain(),
            ),
            contactName = d.contactName,
            lat = d.lat,
            lng = d.lng,
            ward = d.ward,
            district = d.district,
            province = d.province,
            className = d.className,
            channelName = d.channelName,
            shopTypeName = d.shopTypeName,
            photos = d.photos.map { ApprovalPhoto(it.typeName, it.url) },
            routes = d.routes.map { RouteOption(it.id, it.code, it.name, it.alreadyOn) },
        )
    }

    override suspend fun trades(kind: TradeKind, status: ApprovalStatus?) = call {
        service.trades(kind.code, status?.code).orThrow().toPage { it.toDomain() }
    }

    override suspend fun decideLeave(id: String, decision: Decision) = decide("leave", id, decision)

    override suspend fun decideCustomer(id: String, decision: Decision) =
        decide("customer", id, decision)

    override suspend fun decideTrade(kind: TradeKind, id: String, decision: Decision) =
        decide(kind.code, id, decision)

    private suspend fun decide(kind: String, id: String, decision: Decision): DataResult<Unit> {
        if (!decision.isValid) return DataResult.Failure(AppError.Rule("reject_needs_note"))
        return call {
            service.decide(
                DecisionRequestDto(
                    kind = kind,
                    id = id,
                    approve = decision.approve,
                    note = decision.note?.trim()?.takeIf { it.isNotEmpty() },
                    routeId = decision.routeId,
                    approvedQty = decision.approvedQty,
                ),
            ).orThrow()
            Unit
        }
    }

    /**
     * A 409 carries a sentence the database wrote for a person ("đơn này đã được
     * xử lý"), so it is surfaced as a rule message rather than a server fault.
     */
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

private fun <D, T> ApprovalPageDto<D>.toPage(map: (D) -> T) = ApprovalPage(
    counts = counts.mapKeys { ApprovalStatus.of(it.key) },
    items = items.map(map),
)

private fun StaffBriefDto.toDomain() = StaffBrief(id, code, name)

private fun LeaveApprovalDto.toDomain() = LeaveApproval(
    id = id,
    salesperson = salesperson?.toDomain(),
    typeName = typeName,
    isPaid = isPaid,
    fromDate = fromDate,
    toDate = toDate,
    days = days,
    reason = reason,
    status = ApprovalStatus.of(status),
    decisionNote = decisionNote,
    decidedAt = decidedAt,
)

private fun CustomerApprovalDto.toDomain() = CustomerApproval(
    id = id,
    code = code,
    name = name,
    phone = phone,
    address = address,
    status = ApprovalStatus.of(status),
    note = note,
    decisionNote = decisionNote,
    createdAt = createdAt,
    photoCount = photoCount,
    salesperson = salesperson?.toDomain(),
)

private fun TradeApprovalDto.toDomain() = TradeApproval(
    kind = TradeKind.of(kind) ?: TradeKind.DISPLAY,
    id = id,
    status = ApprovalStatus.of(status),
    registeredAt = registeredAt,
    programCode = programCode,
    programName = programName,
    detailName = detailName,
    qty = qty,
    approvedQty = approvedQty,
    reason = reason,
    decisionNote = decisionNote,
    customerCode = customer.code,
    customerName = customer.name,
    customerAddress = customer.address,
    salesperson = salesperson?.toDomain(),
)
