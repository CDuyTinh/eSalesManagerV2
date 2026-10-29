package com.tinhcd.myesalesmanager.data.remote.service

import com.tinhcd.myesalesmanager.data.remote.dto.ManagerBootstrapDto
import retrofit2.Response
import retrofit2.http.GET

/**
 * Edge Functions owned by this app are prefixed `mgr-` so they sit beside the
 * SFA ones in the shared Supabase project without colliding. They live in
 * MyeSalesSFA/supabase/functions, the one place migrations and functions for the
 * shared database are kept.
 */
interface BootstrapService {

    /**
     * Profile, team and settings in one response. [Response] rather than the body
     * so a failure keeps the message the function raised.
     */
    @GET("mgr-bootstrap")
    suspend fun bootstrap(): Response<ManagerBootstrapDto>
}
