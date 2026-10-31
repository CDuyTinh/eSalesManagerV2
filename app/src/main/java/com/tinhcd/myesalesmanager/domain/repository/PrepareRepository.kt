package com.tinhcd.myesalesmanager.domain.repository

import com.tinhcd.myesalesmanager.domain.DataResult
import com.tinhcd.myesalesmanager.domain.model.CustomerEdit
import com.tinhcd.myesalesmanager.domain.model.SalesFocus
import com.tinhcd.myesalesmanager.domain.model.SiteStock
import com.tinhcd.myesalesmanager.domain.model.TeamCustomer

/** Chuẩn bị (stock, sales focus) and correcting the team's outlets. */
interface PrepareRepository {
    suspend fun stock(siteId: String?): DataResult<SiteStock>

    suspend fun salesFocus(date: String): DataResult<SalesFocus>

    suspend fun customers(repId: String, query: String?): DataResult<List<TeamCustomer>>

    suspend fun customer(id: String): DataResult<CustomerEdit>

    /**
     * Sends only the fields that differ from [before]; a blank optional field
     * clears it. @return how many fields the server changed.
     */
    suspend fun updateCustomer(before: CustomerEdit, after: CustomerEdit): DataResult<Int>
}
