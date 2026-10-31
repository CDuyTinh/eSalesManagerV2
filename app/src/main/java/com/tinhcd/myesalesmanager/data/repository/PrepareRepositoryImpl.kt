package com.tinhcd.myesalesmanager.data.repository

import com.tinhcd.myesalesmanager.data.remote.dto.CustomerUpdateDto
import com.tinhcd.myesalesmanager.data.remote.http.PostgrestException
import com.tinhcd.myesalesmanager.data.remote.http.orThrow
import com.tinhcd.myesalesmanager.data.remote.service.PrepareService
import com.tinhcd.myesalesmanager.domain.AppError
import com.tinhcd.myesalesmanager.domain.DataResult
import com.tinhcd.myesalesmanager.domain.model.CustomerChange
import com.tinhcd.myesalesmanager.domain.model.CustomerEdit
import com.tinhcd.myesalesmanager.domain.model.FocusProduct
import com.tinhcd.myesalesmanager.domain.model.FocusRep
import com.tinhcd.myesalesmanager.domain.model.NamedOption
import com.tinhcd.myesalesmanager.domain.model.RunningProgram
import com.tinhcd.myesalesmanager.domain.model.SalesFocus
import com.tinhcd.myesalesmanager.domain.model.Site
import com.tinhcd.myesalesmanager.domain.model.SiteStock
import com.tinhcd.myesalesmanager.domain.model.StockItem
import com.tinhcd.myesalesmanager.domain.model.TeamCustomer
import com.tinhcd.myesalesmanager.domain.repository.PrepareRepository
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrepareRepositoryImpl @Inject constructor(
    private val service: PrepareService,
) : PrepareRepository {

    override suspend fun stock(siteId: String?) = call {
        val d = service.stock(siteId).orThrow()
        SiteStock(
            sites = d.sites.map { Site(it.siteId, it.code, it.name, it.address) },
            siteId = d.siteId,
            items = d.items.map { StockItem(it.productCode, it.productName, it.baseUom, it.qtyBase, it.updatedAt) },
        )
    }

    override suspend fun salesFocus(date: String) = call {
        val d = service.salesFocus(date).orThrow()
        SalesFocus(
            products = d.products.map { p ->
                FocusProduct(
                    p.productCode, p.productName, p.baseUom, p.fromDate, p.toDate, p.targetBaseQty, p.teamTarget,
                    p.note, p.soldBaseQty, p.outlets, p.reps.map { FocusRep(it.code, it.name, it.soldBaseQty, it.outlets) },
                )
            },
            programs = d.programs.map { RunningProgram(it.kind, it.code, it.name, it.fromDate, it.toDate, it.registered) },
        )
    }

    override suspend fun customers(repId: String, query: String?) = call {
        service.customers(repId, query?.trim()?.takeIf { it.isNotEmpty() }).orThrow().map {
            TeamCustomer(it.id, it.code, it.name, it.address, it.phone, it.contactName, it.routes, it.editedAt)
        }
    }

    override suspend fun customer(id: String) = call {
        val d = service.customer(id).orThrow()
        CustomerEdit(
            id = d.id, code = d.code, name = d.name, contactName = d.contactName, phone = d.phone,
            address = d.address, lat = d.lat, lng = d.lng,
            classId = d.classId, channelId = d.channelId, shopTypeId = d.shopTypeId,
            classes = d.options.classes.map { NamedOption(it.id, it.name) },
            channels = d.options.channels.map { NamedOption(it.id, it.name) },
            shopTypes = d.options.shopTypes.map { NamedOption(it.id, it.name) },
            history = d.history.map { h ->
                CustomerChange(h.at, h.actor, h.changes.mapValues { (_, v) -> v["old"].text() to v["new"].text() })
            },
        )
    }

    override suspend fun updateCustomer(before: CustomerEdit, after: CustomerEdit) = call {
        val changes = mutableMapOf<String, JsonElement>()
        fun text(key: String, old: String?, new: String?) {
            if (old != new) changes[key] = if (new == null) JsonNull else JsonPrimitive(new)
        }
        text("name", before.name, after.name.trim())
        text("contact_name", before.contactName, after.contactName?.trim()?.ifEmpty { null })
        text("phone", before.phone, after.phone?.trim()?.ifEmpty { null })
        text("address", before.address, after.address?.trim())
        text("class_id", before.classId, after.classId)
        text("channel_id", before.channelId, after.channelId)
        text("shop_type_id", before.shopTypeId, after.shopTypeId)
        // Position goes as a pair or not at all; the server refuses half of one.
        if (before.lat != after.lat || before.lng != after.lng) {
            changes["lat"] = JsonPrimitive(after.lat)
            changes["lng"] = JsonPrimitive(after.lng)
        }
        if (changes.isEmpty()) 0 else service.updateCustomer(CustomerUpdateDto(before.id, changes)).orThrow().changed
    }

    private fun JsonElement?.text(): String? =
        if (this == null || this is JsonNull) null else runCatching { jsonPrimitive.contentOrNull }.getOrNull() ?: toString()

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
