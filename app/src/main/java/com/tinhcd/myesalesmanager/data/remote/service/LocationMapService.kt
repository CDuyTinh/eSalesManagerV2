package com.tinhcd.myesalesmanager.data.remote.service

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import com.tinhcd.myesalesmanager.data.remote.dto.StaffBriefDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

@Serializable
data class MapVisitDto(
    @SerialName("customer_code") val customerCode: String,
    @SerialName("customer_name") val customerName: String,
    val lat: Double? = null,
    val lng: Double? = null,
    @SerialName("check_in_at") val checkInAt: String? = null,
    @SerialName("check_out_at") val checkOutAt: String? = null,
    val status: String? = null,
    @SerialName("distance_m") val distanceM: Double? = null,
)

@Serializable
data class RepDayDto(
    val rep: StaffBriefDto,
    @SerialName("clock_in") val clockIn: String? = null,
    @SerialName("clock_out") val clockOut: String? = null,
    /** [lat, lng, recorded_at] triples, compact on purpose: a day is hundreds of them. */
    val path: List<JsonArray> = emptyList(),
    val visits: List<MapVisitDto> = emptyList(),
)

@Serializable
data class LocationMapDto(val date: String, val reps: List<RepDayDto> = emptyList())

/** `mgr-location-map`: the team's day on a map. */
interface LocationMapService {
    @GET("mgr-location-map")
    suspend fun map(
        @Query("date") date: String,
        @Query("salespersonId") salespersonId: String?,
    ): Response<LocationMapDto>
}
