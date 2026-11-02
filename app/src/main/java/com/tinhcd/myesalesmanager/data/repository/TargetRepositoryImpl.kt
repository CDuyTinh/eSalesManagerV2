package com.tinhcd.myesalesmanager.data.repository

import com.tinhcd.myesalesmanager.data.remote.dto.SaveTargetsDto
import com.tinhcd.myesalesmanager.data.remote.dto.TargetAmountDto
import com.tinhcd.myesalesmanager.data.remote.dto.TargetLineDto
import com.tinhcd.myesalesmanager.data.remote.http.PostgrestException
import com.tinhcd.myesalesmanager.data.remote.http.orThrow
import com.tinhcd.myesalesmanager.data.remote.service.TargetService
import com.tinhcd.myesalesmanager.domain.AppError
import com.tinhcd.myesalesmanager.domain.DataResult
import com.tinhcd.myesalesmanager.domain.model.TargetLine
import com.tinhcd.myesalesmanager.domain.model.TargetSheet
import com.tinhcd.myesalesmanager.domain.repository.TargetRepository
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TargetRepositoryImpl @Inject constructor(
    private val service: TargetService,
) : TargetRepository {

    override suspend fun sheet(month: LocalDate, salespersonId: String) = call {
        val d = service.sheet(month.toString(), salespersonId).orThrow()
        TargetSheet(
            month = LocalDate.parse(d.month),
            canEdit = d.canEdit,
            revenueTarget = d.revenueTarget,
            orderTarget = d.orderTarget,
            categories = d.categories.map { it.toDomain() },
            products = d.products.map { it.toDomain() },
        )
    }

    override suspend fun save(
        salespersonId: String,
        month: LocalDate,
        revenueTarget: Long?,
        orderTarget: Int?,
        categories: Map<String, Long>,
        products: Map<String, Long>,
    ) = call {
        service.save(
            SaveTargetsDto(
                salespersonId = salespersonId,
                month = month.toString(),
                revenueTarget = revenueTarget,
                orderTarget = orderTarget,
                categories = categories.map { (id, target) -> TargetAmountDto(id, target) },
                products = products.map { (id, target) -> TargetAmountDto(id, target) },
            ),
        ).orThrow()
        Unit
    }

    private fun TargetLineDto.toDomain() = TargetLine(id, code, name, target)

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
