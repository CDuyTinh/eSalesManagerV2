package com.tinhcd.myesalesmanager.data.remote.service

import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.HTTP
import retrofit2.http.POST

@Serializable
data class DeviceTokenDto(
    val token: String,
    /** "manager": the server sends this app only what is meant for a manager. */
    val app: String? = null,
)

@Serializable
data class DeviceTokenAckDto(val ok: Boolean = true)

/** This phone's FCM token, filed against whoever is signed in (`device-token`). */
interface DeviceTokenService {

    @POST("device-token")
    suspend fun register(@Body body: DeviceTokenDto): Response<DeviceTokenAckDto>

    /** A DELETE with a body: the token is what is being removed, not a path segment. */
    @HTTP(method = "DELETE", path = "device-token", hasBody = true)
    suspend fun unregister(@Body body: DeviceTokenDto): Response<DeviceTokenAckDto>
}
