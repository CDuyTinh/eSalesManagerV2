package com.tinhcd.myesalesmanager.feature.route

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.tinhcd.myesalesmanager.core.ui.LoadingBox
import com.tinhcd.myesalesmanager.core.ui.StatusPill
import com.tinhcd.myesalesmanager.core.ui.SubScreen
import com.tinhcd.myesalesmanager.domain.DataResult
import com.tinhcd.myesalesmanager.domain.model.SalesRoute
import com.tinhcd.myesalesmanager.domain.model.WEEKDAY_LABELS
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
)

@HiltViewModel
class RoutesViewModel @Inject constructor(
    private val repository: RouteRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(RoutesState())
    val state: StateFlow<RoutesState> = _state.asStateFlow()

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

    SubScreen(title = "Lập tuyến & phân tuyến", subtitle = "Tuyến của nhóm", onBack = onBack) { padding ->
        val routes = state.routes
        when {
            routes == null && state.error != null -> ErrorBox(state.error!!, onRetry = viewModel::refresh, modifier = Modifier.padding(padding))
            routes == null -> LoadingBox(Modifier.padding(padding))
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
    }
}

@Composable
private fun RouteCard(route: SalesRoute, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
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
