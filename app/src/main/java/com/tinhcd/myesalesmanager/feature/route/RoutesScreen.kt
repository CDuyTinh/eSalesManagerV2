package com.tinhcd.myesalesmanager.feature.route

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.tinhcd.myesalesmanager.core.ui.ErrorBox
import com.tinhcd.myesalesmanager.core.ui.Kpi
import com.tinhcd.myesalesmanager.core.ui.SkeletonList
import com.tinhcd.myesalesmanager.core.ui.StatusPill
import com.tinhcd.myesalesmanager.core.ui.SubScreen
import com.tinhcd.myesalesmanager.domain.DataResult
import com.tinhcd.myesalesmanager.domain.model.SalesRoute
import com.tinhcd.myesalesmanager.domain.model.TeamMember
import com.tinhcd.myesalesmanager.domain.model.WEEKDAY_LABELS
import com.tinhcd.myesalesmanager.domain.repository.AuthRepository
import com.tinhcd.myesalesmanager.domain.repository.RouteRepository
import com.tinhcd.myesalesmanager.feature.approval.toMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RoutesState(
    val routes: List<SalesRoute>? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val reps: List<TeamMember> = emptyList(),
    /** The new-route form, while open. */
    val draft: NewRouteDraft? = null,
    val saving: Boolean = false,
    /** A route just made, to open. */
    val created: String? = null,
)

data class NewRouteDraft(val repId: String? = null, val name: String = "", val code: String = "") {
    val canSave: Boolean get() = repId != null && name.isNotBlank()
}

@HiltViewModel
class RoutesViewModel @Inject constructor(
    private val repository: RouteRepository,
    auth: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(RoutesState())
    val state: StateFlow<RoutesState> = _state.asStateFlow()

    init {
        viewModelScope.launch { auth.team.collect { t -> _state.update { it.copy(reps = t) } } }
    }

    fun newRoute() = _state.update { it.copy(draft = NewRouteDraft(repId = it.reps.singleOrNull()?.id)) }
    fun edit(change: (NewRouteDraft) -> NewRouteDraft) = _state.update { s -> s.copy(draft = s.draft?.let(change)) }
    fun dismiss() = _state.update { if (it.saving) it else it.copy(draft = null) }

    fun create() {
        val d = _state.value.draft ?: return
        if (!d.canSave) return
        viewModelScope.launch {
            _state.update { it.copy(saving = true) }
            when (val r = repository.create(d.repId!!, d.name, d.code)) {
                is DataResult.Success -> _state.update { it.copy(saving = false, draft = null, created = r.data) }
                is DataResult.Failure -> _state.update { it.copy(saving = false, error = r.error.toMessage()) }
            }
        }
    }

    fun opened() = _state.update { it.copy(created = null) }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (val r = repository.routes()) {
                is DataResult.Success -> _state.update { it.copy(loading = false, routes = r.data) }
                is DataResult.Failure -> _state.update { it.copy(loading = false, error = r.error.toMessage()) }
            }
        }
    }
}

/** Lập tuyến (MCP) & phân tuyến: the routes the team runs. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RoutesScreen(
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    viewModel: RoutesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Reloaded on return, so counts reflect what was changed inside a route.
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    LaunchedEffect(state.created) { state.created?.let { onOpen(it); viewModel.opened() } }

    SubScreen(title = "Lập tuyến & phân tuyến", subtitle = "Tuyến của nhóm", onBack = onBack) { padding ->
        val routes = state.routes
        when {
            routes == null && state.error != null -> ErrorBox(state.error!!, onRetry = viewModel::refresh, modifier = Modifier.padding(padding))
            routes == null -> SkeletonList(Modifier.padding(padding), count = 4)
            else -> LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (routes.isEmpty()) {
                    item { Text("Nhóm chưa có tuyến nào", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                items(routes, key = { it.id }) { r -> RouteCard(r) { onOpen(r.id) } }
            }
        }
        if (state.routes != null) {
            Box(Modifier.fillMaxSize().padding(padding).padding(16.dp), contentAlignment = Alignment.BottomEnd) {
                ExtendedFloatingActionButton(
                    onClick = viewModel::newRoute,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Tạo tuyến") },
                )
            }
        }
    }

    state.draft?.let { d ->
        AlertDialog(
            onDismissRequest = viewModel::dismiss,
            title = { Text("Tạo tuyến mới") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Nhân viên phụ trách", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.reps.forEach { m ->
                            FilterChip(
                                selected = d.repId == m.id,
                                onClick = { viewModel.edit { it.copy(repId = m.id) } },
                                label = { Text(m.fullName) },
                            )
                        }
                    }
                    OutlinedTextField(
                        d.name, { v -> viewModel.edit { it.copy(name = v) } },
                        label = { Text("Tên tuyến *") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        d.code, { v -> viewModel.edit { it.copy(code = v.uppercase()) } },
                        label = { Text("Mã tuyến (để trống: tự cấp)") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    )
                    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall) }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::create, enabled = d.canSave && !state.saving) {
                    Text(if (state.saving) "Đang tạo…" else "Tạo")
                }
            },
            dismissButton = { TextButton(onClick = viewModel::dismiss, enabled = !state.saving) { Text("Huỷ") } },
        )
    }
}

@Composable
private fun RouteCard(route: SalesRoute, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${route.code} – ${route.name}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        route.rep?.let { "NV: ${it.name} (${it.code})" } ?: "Chưa giao nhân viên",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                StatusPill("${route.customerCount} điểm", Kpi.Blue.first())
            }
            WeekStrip(route.perWeekday)
        }
    }
}

/** Outlets per weekday as a row of small cells: the shape of the route's week. */
@Composable
private fun WeekStrip(counts: List<Int>) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
        WEEKDAY_LABELS.forEachIndexed { i, label ->
            val n = counts.getOrElse(i) { 0 }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    n.toString(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (n > 0) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (n > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
