package com.tinhcd.myesalesmanager.domain.model

/** A distributor warehouse the manager can look into. */
data class Site(val id: String, val code: String, val name: String, val address: String?)

data class StockItem(
    val productCode: String,
    val productName: String,
    val baseUom: String,
    val qtyBase: Long,
    val updatedAt: String?,
)

data class SiteStock(val sites: List<Site>, val siteId: String?, val items: List<StockItem>)

data class FocusRep(val code: String, val name: String, val soldBaseQty: Long, val outlets: Int)

/** A focus product and the team's progress on it. The target is per rep; [teamTarget] sums it. */
data class FocusProduct(
    val productCode: String,
    val productName: String,
    val baseUom: String,
    val fromDate: String,
    val toDate: String,
    val targetBaseQty: Long?,
    val teamTarget: Long?,
    val note: String?,
    val soldBaseQty: Long,
    val outlets: Int,
    val reps: List<FocusRep>,
)

data class RunningProgram(
    /** display or loyalty. */
    val kind: String,
    val code: String,
    val name: String,
    val fromDate: String,
    val toDate: String,
    /** The team's outlets signed up (pending or approved). */
    val registered: Int,
)

data class SalesFocus(val products: List<FocusProduct>, val programs: List<RunningProgram>)

// --- Sửa khách hàng ------------------------------------------------------------

data class TeamCustomer(
    val id: String,
    val code: String,
    val name: String,
    val address: String?,
    val phone: String?,
    val contactName: String?,
    val routes: String?,
    val editedAt: String?,
)

data class NamedOption(val id: String, val name: String)

data class CustomerChange(val at: String, val actor: String, val fields: Map<String, Pair<String?, String?>>)

data class CustomerEdit(
    val id: String,
    val code: String,
    val name: String,
    val contactName: String?,
    val phone: String?,
    val address: String?,
    val lat: Double?,
    val lng: Double?,
    val classId: String?,
    val channelId: String?,
    val shopTypeId: String?,
    val classes: List<NamedOption>,
    val channels: List<NamedOption>,
    val shopTypes: List<NamedOption>,
    val history: List<CustomerChange>,
)
