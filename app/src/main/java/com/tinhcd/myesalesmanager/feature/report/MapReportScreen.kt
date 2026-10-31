package com.tinhcd.myesalesmanager.feature.report

import android.content.Context
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.tinhcd.myesalesmanager.core.ui.ErrorBox
import com.tinhcd.myesalesmanager.core.ui.Kpi
import com.tinhcd.myesalesmanager.core.ui.LoadingBox
import com.tinhcd.myesalesmanager.core.ui.SubScreen
import com.tinhcd.myesalesmanager.core.ui.TeamFilter
import com.tinhcd.myesalesmanager.core.ui.formatDateTime
import com.tinhcd.myesalesmanager.core.ui.formatDay
import com.tinhcd.myesalesmanager.core.ui.todayVn
import com.tinhcd.myesalesmanager.data.repository.LocationMapRepository
import com.tinhcd.myesalesmanager.domain.DataResult
import com.tinhcd.myesalesmanager.domain.model.RepDay
import com.tinhcd.myesalesmanager.domain.model.TeamMember
import com.tinhcd.myesalesmanager.domain.repository.AuthRepository
import com.tinhcd.myesalesmanager.feature.approval.toMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.time.LocalDate
import javax.inject.Inject

data class MapReportState(
    val date: LocalDate = todayVn(),
    val team: List<TeamMember> = emptyList(),
    val supervisors: List<TeamMember> = emptyList(),
    val supervisorId: String? = null,
    val salespersonId: String? = null,
    val days: List<RepDay>? = null,
    val error: String? = null,
)

@HiltViewModel
class MapReportViewModel @Inject constructor(
    private val repository: LocationMapRepository,
    auth: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(MapReportState())
    val state: StateFlow<MapReportState> = _state.asStateFlow()

    init {
        viewModelScope.launch { auth.team.collect { t -> _state.update { it.copy(team = t) } } }
        viewModelScope.launch { auth.supervisors.collect { s -> _state.update { it.copy(supervisors = s) } } }
        refresh()
    }

    fun refresh() {
        val s = _state.value
        viewModelScope.launch {
            _state.update { it.copy(error = null) }
            when (val r = repository.day(s.date.toString(), s.salespersonId)) {
                is DataResult.Success -> _state.update { it.copy(days = r.data) }
                is DataResult.Failure -> _state.update { it.copy(error = r.error.toMessage()) }
            }
        }
    }

    fun shiftDay(days: Long) {
        val next = _state.value.date.plusDays(days)
        if (next.isAfter(todayVn())) return
        _state.update { it.copy(date = next) }
        refresh()
    }

    fun selectRep(id: String?) {
        _state.update { it.copy(salespersonId = id) }
        refresh()
    }

    /** Only narrows what is drawn: the server already returns the whole team. */
    fun selectSupervisor(id: String?) = _state.update { it.copy(supervisorId = id, salespersonId = null) }
}

/** One colour per rep, stable across reloads of the same day. */
private val palette = listOf(Kpi.Blue, Kpi.Orange, Kpi.Green, Kpi.Violet, Kpi.Slate).map { it.first() }

/**
 * Bản đồ vị trí nhân viên (legacy OM30400): each rep's path through the day as
 * the tracking service recorded it, the outlets they checked in at, and where
 * they were last seen. OpenStreetMap tiles, so no map key is needed.
 */
@Composable
fun MapReportScreen(onBack: () -> Unit, viewModel: MapReportViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val shownReps = state.supervisorId?.let { sv -> state.team.filter { it.supervisorId == sv }.map { it.id }.toSet() }
    val days = state.days?.filter { shownReps == null || it.rep.id in shownReps }

    SubScreen(title = "Bản đồ vị trí nhân viên", onBack = onBack) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                IconButton(onClick = { viewModel.shiftDay(-1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Ngày trước") }
                Text(formatDay(state.date), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                IconButton(onClick = { viewModel.shiftDay(1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Ngày sau") }
            }
            TeamFilter(
                supervisors = state.supervisors,
                supervisorId = state.supervisorId,
                onSupervisor = viewModel::selectSupervisor,
                reps = state.team,
                salespersonId = state.salespersonId,
                onRep = viewModel::selectRep,
            )
            Spacer(Modifier.height(8.dp))
            when {
                days == null && state.error != null -> ErrorBox(state.error!!, onRetry = viewModel::refresh)
                days == null -> LoadingBox()
                else -> {
                    val colours = days.mapIndexed { i, d -> d.rep.id to palette[i % palette.size] }.toMap()
                    Box(Modifier.fillMaxWidth().height(340.dp)) { OsmMap(days, colours) }
                    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (days.all { it.path.isEmpty() && it.visits.isEmpty() }) {
                            item { Text("Không có dữ liệu vị trí ngày này", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                        items(days, key = { it.rep.id }) { d -> RepLegend(d, colours.getValue(d.rep.id)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun RepLegend(d: RepDay, colour: Color) {
    Row(verticalAlignment = Alignment.Top) {
        Box(Modifier.padding(top = 5.dp).size(12.dp).background(colour, CircleShape))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(d.rep.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(
                listOfNotNull(
                    d.clockIn?.let { "vào ${formatDateTime(it).takeLast(5)}" },
                    d.clockOut?.let { "ra ${formatDateTime(it).takeLast(5)}" },
                    "${d.visits.size} điểm ghé",
                    "${d.path.size} điểm GPS",
                    d.path.lastOrNull()?.let { "thấy lần cuối ${formatDateTime(it.at).takeLast(5)}" },
                ).joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** osmdroid's MapView inside Compose: paths, visit pins, the view fitted to all of it. */
@Composable
private fun OsmMap(days: List<RepDay>, colours: Map<String, Color>) {
    val context = LocalContext.current
    val map = remember { newMapView(context) }
    DisposableEffect(Unit) {
        map.onResume()
        onDispose { map.onPause(); map.onDetach() }
    }
    AndroidView(factory = { map }, modifier = Modifier.fillMaxSize()) { view ->
        view.overlays.clear()
        val all = mutableListOf<GeoPoint>()
        days.forEach { d ->
            val colour = colours.getValue(d.rep.id).toArgb()
            if (d.path.size >= 2) {
                view.overlays.add(Polyline(view).apply {
                    setPoints(d.path.map { GeoPoint(it.lat, it.lng) })
                    outlinePaint.color = colour
                    outlinePaint.strokeWidth = 6f
                    title = d.rep.name
                })
            }
            d.visits.forEachIndexed { i, v ->
                if (v.lat == null || v.lng == null) return@forEachIndexed
                val at = GeoPoint(v.lat, v.lng)
                view.overlays.add(Marker(view).apply {
                    position = at
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    title = "${i + 1}. ${v.customerCode} – ${v.customerName}"
                    snippet = listOfNotNull(
                        d.rep.name,
                        v.checkInAt?.let { "vào ${formatDateTime(it).takeLast(5)}" },
                        v.checkOutAt?.let { "ra ${formatDateTime(it).takeLast(5)}" },
                    ).joinToString(" · ")
                })
                all += at
            }
            d.lastSeen?.let { last ->
                view.overlays.add(Marker(view).apply {
                    position = GeoPoint(last.lat, last.lng)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    icon = android.graphics.drawable.ShapeDrawable(android.graphics.drawable.shapes.OvalShape()).apply {
                        intrinsicWidth = 36; intrinsicHeight = 36; paint.color = colour
                    }
                    title = "${d.rep.name} — vị trí cuối"
                })
            }
            all += d.path.map { GeoPoint(it.lat, it.lng) }
        }
        if (all.isNotEmpty()) {
            view.post {
                if (all.size == 1) {
                    view.controller.setZoom(16.0)
                    view.controller.setCenter(all.first())
                } else {
                    view.zoomToBoundingBox(BoundingBox.fromGeoPointsSafe(all).increaseByScale(1.3f), false)
                }
            }
        }
        view.invalidate()
    }
}

private fun newMapView(context: Context): MapView {
    // OSM's tile servers ask every client to identify itself.
    Configuration.getInstance().userAgentValue = context.packageName
    return MapView(context).apply {
        setTileSource(TileSourceFactory.MAPNIK)
        setMultiTouchControls(true)
        controller.setZoom(13.0)
        controller.setCenter(GeoPoint(10.98, 106.65))
        setBackgroundColor(AndroidColor.LTGRAY)
    }
}
