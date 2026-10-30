package com.tinhcd.myesalesmanager.feature.route

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.tinhcd.myesalesmanager.core.ui.ErrorBox
import com.tinhcd.myesalesmanager.core.ui.LoadingBox
import com.tinhcd.myesalesmanager.core.ui.SearchBox
import com.tinhcd.myesalesmanager.core.ui.SubScreen
import com.tinhcd.myesalesmanager.core.ui.formatDateTime
import com.tinhcd.myesalesmanager.core.ui.formatIsoDate
import com.tinhcd.myesalesmanager.domain.DataResult
import com.tinhcd.myesalesmanager.domain.model.RouteCandidate
import com.tinhcd.myesalesmanager.domain.model.RouteDetail
import com.tinhcd.myesalesmanager.domain.model.RouteOutlet
import com.tinhcd.myesalesmanager.domain.model.TeamMember
import com.tinhcd.myesalesmanager.domain.model.WEEKDAY_LABELS
import com.tinhcd.myesalesmanager.domain.model.formatSchedule
import com.tinhcd.myesalesmanager.domain.repository.AuthRepository
import com.tinhcd.myesalesmanager.domain.repository.RouteRepository
import com.tinhcd.myesalesmanager.feature.approval.toMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

const val ARG_ROUTE_ID = "routeId"

/** The outlet whose days are being set: one on the route, or a candidate to add. */
data class ScheduleEdit(
    val customerId: String,
    val title: String,
    val weekdays: Set<Int>,
    val weeks: Set<Int>,
    val adding: Boolean,
)

data class RouteDetailState(
    val route: RouteDetail? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val busy: Boolean = false,
    val message: String? = null,
    val edit: ScheduleEdit? = null,
    val removing: RouteOutlet? = null,
    val assigning: Boolean = false,
    /** Non-null while the add-outlet picker is open. */
    val candidates: List<RouteCandidate>? = null,
    val candidateQuery: String = "",
)

@HiltViewModel
class RouteDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repository: RouteRepository,
    auth: AuthRepository,
) : ViewModel() {

    private val routeId: String = checkNotNull(savedState[ARG_ROUTE_ID])

    private val _state = MutableStateFlow(RouteDetailState())
    val state: StateFlow<RouteDetailState> = _state.asStateFlow()

    val team: StateFlow<List<TeamMember>> = auth.team.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private var search: Job? = null

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (val r = repository.detail(routeId)) {
                is DataResult.Success -> _state.update { it.copy(loading = false, route = r.data) }
                is DataResult.Failure -> _state.update { it.copy(loading = false, error = r.error.toMessage()) }
            }
        }
    }

    fun editOutlet(o: RouteOutlet) = _state.update {
        it.copy(edit = ScheduleEdit(o.customerId, "${o.code} – ${o.name}", o.weekdays.toSet(), o.weeks.toSet(), adding = false))
    }

    fun pickCandidate(c: RouteCandidate) = _state.update {
        it.copy(candidates = null, edit = ScheduleEdit(c.customerId, "${c.code} – ${c.name}", emptySet(), emptySet(), adding = true))
    }

    fun toggleDay(d: Int) = _state.update { s ->
        s.copy(edit = s.edit?.let { e -> e.copy(weekdays = if (d in e.weekdays) e.weekdays - d else e.weekdays + d) })
    }

    fun toggleWeek(w: Int) = _state.update { s ->
        s.copy(edit = s.edit?.let { e -> e.copy(weeks = if (w in e.weeks) e.weeks - w else e.weeks + w) })
    }

    fun saveSchedule() {
        val e = _state.value.edit ?: return
        run(if (e.adding) "Đã thêm điểm bán vào tuyến" else "Đã cập nhật lịch ghé") {
            repository.setOutlet(routeId, e.customerId, e.weekdays.sorted(), e.weeks.sorted())
        }
    }

    fun askRemove(o: RouteOutlet) = _state.update { it.copy(removing = o) }

    fun remove() {
        val o = _state.value.removing ?: return
        run("Đã bỏ ${o.code} khỏi tuyến") { repository.removeOutlet(routeId, o.customerId) }
    }

    fun askAssign() = _state.update { it.copy(assigning = true) }

    fun assign(salespersonId: String, note: String) {
        run("Đã giao tuyến") { repository.assign(routeId, salespersonId, note) }
    }

    fun openCandidates() {
        _state.update { it.copy(candidates = emptyList(), candidateQuery = "") }
        loadCandidates("")
    }

    fun queryCandidates(q: String) {
        _state.update { it.copy(candidateQuery = q) }
        search?.cancel()
        search = viewModelScope.launch {
            delay(300)
            loadCandidates(q)
        }
    }

    private fun loadCandidates(q: String) {
        viewModelScope.launch {
            when (val r = repository.candidates(routeId, q)) {
                is DataResult.Success -> _state.update { if (it.candidates == null) it else it.copy(candidates = r.data) }
                is DataResult.Failure -> _state.update { it.copy(message = r.error.toMessage()) }
            }
        }
    }

    fun dismiss() = _state.update {
        if (it.busy) it else it.copy(edit = null, removing = null, assigning = false, candidates = null)
    }

    fun messageShown() = _state.update { it.copy(message = null) }

    /** Runs a change, closes whatever dialog asked for it, and reloads the route. */
    private fun run(done: String, block: suspend () -> DataResult<Unit>) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            when (val r = block()) {
                is DataResult.Success -> {
                    _state.update {
                        it.copy(busy = false, edit = null, removing = null, assigning = false, message = done)
                    }
                    refresh()
                }
                // The dialog stays open so the supervisor can fix what the server refused.
                is DataResult.Failure -> _state.update { it.copy(busy = false, message = r.error.toMessage()) }
            }
        }
    }
}

/** One route: its outlets and their days (MCP), who runs it, and what changed. */
@Composable
fun RouteDetailScreen(
    onBack: () -> Unit,
    viewModel: RouteDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val team by viewModel.team.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.message) {
        state.message?.let { snackbar.showSnackbar(it); viewModel.messageShown() }
    }

    val route = state.route
    SubScreen(
        title = route?.let { "${it.code} – ${it.name}" } ?: "Tuyến",
        subtitle = route?.let { r -> r.rep?.let { "NV: ${it.name}" } ?: "Chưa giao nhân viên" },
        onBack = onBack,
        snackbar = snackbar,
    ) { padding ->
        when {
            route == null && state.error != null -> ErrorBox(state.error!!, onRetry = viewModel::refresh, modifier = Modifier.padding(padding))
            route == null -> LoadingBox(Modifier.padding(padding))
            else -> RouteBody(route, Modifier.padding(padding), viewModel)
        }
    }

    state.edit?.let { ScheduleDialog(it, state.busy, viewModel) }
    state.removing?.let { o ->
        AlertDialog(
            onDismissRequest = viewModel::dismiss,
            title = { Text("Bỏ khỏi tuyến") },
            text = { Text("Bỏ ${o.code} – ${o.name} khỏi tuyến? Nhân viên sẽ không còn ghé điểm này theo lịch.") },
            confirmButton = { TextButton(onClick = viewModel::remove, enabled = !state.busy) { Text("Bỏ") } },
            dismissButton = { TextButton(onClick = viewModel::dismiss, enabled = !state.busy) { Text("Huỷ") } },
        )
    }
    if (state.assigning && route != null) AssignDialog(route, team, state.busy, viewModel)
    state.candidates?.let { CandidatesDialog(it, state.candidateQuery, viewModel) }
}

@Composable
private fun RouteBody(route: RouteDetail, modifier: Modifier, viewModel: RouteDetailViewModel) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = viewModel::openCandidates, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text(" Thêm điểm")
                }
                OutlinedButton(onClick = viewModel::askAssign, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = null)
                    Text(" Giao tuyến")
                }
            }
        }
        item {
            Text(
                "${route.outlets.size} điểm bán trên tuyến",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (route.outlets.isEmpty()) {
            item { Text("Tuyến chưa có điểm bán nào", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(route.outlets, key = { it.customerId }) { o ->
            OutletCard(o, onEdit = { viewModel.editOutlet(o) }, onRemove = { viewModel.askRemove(o) })
        }
        if (route.history.isNotEmpty()) {
            item {
                Text(
                    "Lịch sử thay đổi",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            items(route.history) { h ->
                Column {
                    Text(h.text, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        listOfNotNull(formatDateTime(h.at), h.actor).joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    HorizontalDivider(Modifier.padding(top = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun OutletCard(o: RouteOutlet, onEdit: () -> Unit, onRemove: () -> Unit) {
    Card(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${o.code} – ${o.name}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                o.address?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Text(
                    formatSchedule(o.weekdays, o.weeks),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "Ghé gần nhất: ${o.lastVisit?.let(::formatIsoDate) ?: "—"} · Đơn gần nhất: ${o.lastOrder?.let(::formatIsoDate) ?: "—"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Sửa lịch ghé") }
            IconButton(onClick = onRemove) { Icon(Icons.Default.Delete, contentDescription = "Bỏ khỏi tuyến") }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ScheduleDialog(edit: ScheduleEdit, busy: Boolean, viewModel: RouteDetailViewModel) {
    AlertDialog(
        onDismissRequest = viewModel::dismiss,
        title = { Text(if (edit.adding) "Thêm vào tuyến" else "Lịch ghé") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(edit.title, fontWeight = FontWeight.SemiBold)
                Text("Ngày ghé trong tuần", style = MaterialTheme.typography.labelSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    WEEKDAY_LABELS.forEachIndexed { i, label ->
                        FilterChip(selected = (i + 1) in edit.weekdays, onClick = { viewModel.toggleDay(i + 1) }, label = { Text(label) })
                    }
                }
                Text("Tuần trong tháng (bỏ trống = mọi tuần)", style = MaterialTheme.typography.labelSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..4).forEach { w ->
                        FilterChip(selected = w in edit.weeks, onClick = { viewModel.toggleWeek(w) }, label = { Text("Tuần $w") })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = viewModel::saveSchedule, enabled = !busy && edit.weekdays.isNotEmpty()) {
                Text(if (busy) "Đang lưu…" else "Lưu")
            }
        },
        dismissButton = { TextButton(onClick = viewModel::dismiss, enabled = !busy) { Text("Huỷ") } },
    )
}

@Composable
private fun AssignDialog(route: RouteDetail, team: List<TeamMember>, busy: Boolean, viewModel: RouteDetailViewModel) {
    var picked by remember { mutableStateOf(route.rep?.id) }
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = viewModel::dismiss,
        title = { Text("Giao tuyến ${route.code}") },
        text = {
            Column {
                team.forEach { m ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { picked = m.id },
                    ) {
                        RadioButton(selected = picked == m.id, onClick = { picked = m.id })
                        Text("${m.fullName} (${m.code})")
                    }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Ghi chú (không bắt buộc)") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { picked?.let { viewModel.assign(it, note) } },
                enabled = !busy && picked != null && picked != route.rep?.id,
            ) { Text(if (busy) "Đang lưu…" else "Giao") }
        },
        dismissButton = { TextButton(onClick = viewModel::dismiss, enabled = !busy) { Text("Huỷ") } },
    )
}

@Composable
private fun CandidatesDialog(candidates: List<RouteCandidate>, query: String, viewModel: RouteDetailViewModel) {
    AlertDialog(
        onDismissRequest = viewModel::dismiss,
        title = { Text("Thêm điểm bán") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SearchBox(query, viewModel::queryCandidates, placeholder = "Mã, tên, địa chỉ")
                LazyColumn(Modifier.heightIn(max = 380.dp)) {
                    if (candidates.isEmpty()) {
                        item { Text("Không có điểm bán phù hợp", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    items(candidates, key = { it.customerId }) { c ->
                        Column(
                            Modifier.fillMaxWidth().clickable { viewModel.pickCandidate(c) }.padding(vertical = 8.dp),
                        ) {
                            Text("${c.code} – ${c.name}", fontWeight = FontWeight.SemiBold)
                            Text(
                                listOfNotNull(c.address, c.onRoutes?.let { "Đang ở tuyến $it" }).joinToString(" · "),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = viewModel::dismiss) { Text("Đóng") } },
    )
}
