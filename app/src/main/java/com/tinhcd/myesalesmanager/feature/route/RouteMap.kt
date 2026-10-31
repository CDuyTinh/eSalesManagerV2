package com.tinhcd.myesalesmanager.feature.route

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.tinhcd.myesalesmanager.core.ui.Kpi
import com.tinhcd.myesalesmanager.domain.model.RouteOutlet
import com.tinhcd.myesalesmanager.domain.model.formatSchedule
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

/**
 * The route's outlets on OpenStreetMap, numbered and joined in call order (the
 * legacy mcp_map). Outlets without a position are left off.
 */
@Composable
fun RouteMap(outlets: List<RouteOutlet>) {
    val context = LocalContext.current
    val map = remember {
        Configuration.getInstance().userAgentValue = context.packageName
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(14.0)
        }
    }
    DisposableEffect(Unit) {
        map.onResume()
        onDispose { map.onPause(); map.onDetach() }
    }
    AndroidView(factory = { map }, modifier = Modifier.fillMaxSize()) { view ->
        view.overlays.clear()
        val placed = outlets.filter { it.lat != null && it.lng != null }
        val points = placed.map { GeoPoint(it.lat!!, it.lng!!) }
        if (points.size >= 2) {
            view.overlays.add(Polyline(view).apply {
                setPoints(points)
                outlinePaint.color = Kpi.Blue.first().toArgb()
                outlinePaint.strokeWidth = 5f
            })
        }
        placed.forEachIndexed { i, o ->
            view.overlays.add(Marker(view).apply {
                position = points[i]
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = "${i + 1}. ${o.code} – ${o.name}"
                snippet = listOfNotNull(o.address, formatSchedule(o.weekdays, o.weeks)).joinToString(" · ")
            })
        }
        if (points.isNotEmpty()) {
            view.post {
                if (points.size == 1) view.controller.setCenter(points.first())
                else view.zoomToBoundingBox(BoundingBox.fromGeoPointsSafe(points).increaseByScale(1.3f), false)
            }
        }
        view.invalidate()
    }
}
