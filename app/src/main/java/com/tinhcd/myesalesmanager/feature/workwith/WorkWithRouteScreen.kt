package com.tinhcd.myesalesmanager.feature.workwith

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.tinhcd.myesalesmanager.core.location.LocationProvider
import com.tinhcd.myesalesmanager.core.ui.ErrorBox
import com.tinhcd.myesalesmanager.core.ui.Kpi
import com.tinhcd.myesalesmanager.core.ui.LoadingBox
import com.tinhcd.myesalesmanager.core.ui.StatusPill
import com.tinhcd.myesalesmanager.core.ui.SubScreen
import com.tinhcd.myesalesmanager.core.ui.compactMoney
import com.tinhcd.myesalesmanager.core.ui.formatDateTime
import com.tinhcd.myesalesmanager.core.ui.formatDay
import com.tinhcd.myesalesmanager.core.ui.todayVn
import com.tinhcd.myesalesmanager.domain.DataResult
import com.tinhcd.myesalesmanager.domain.model.TeamMember
import com.tinhcd.myesalesmanager.domain.model.VisitStatus
import com.tinhcd.myesalesmanager.domain.model.WorkWithRoute
import com.tinhcd.myesalesmanager.domain.model.WorkWithStop
import com.tinhcd.myesalesmanager.domain.repository.AuthRepository
import com.tinhcd.myesalesmanager.domain.repository.WorkWithRepository
import com.tinhcd.myesalesmanager.feature.approval.toMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WorkWithRouteState(
    val team: List<TeamMember> = emptyList(),
    val repId: String? = null,
    val route: WorkWithRoute? = null,
    val loading: Boolean = false,
    val error: String? = null,
    /** The stop the supervisor tapped to check in at, awaiting confirmation. */
    val confirm: WorkWithStop? = null,
    val checkingIn: Boolean = false,
    val message: String? = null,
    /** Set once a call is open, so the screen can navigate into it. */
    val openVisit: String? = null,
)

@HiltViewModel
class WorkWithRouteViewModel @Inject constructor(
    private val repository: WorkWithRepository,
    private val location: LocationProvider,
    auth: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(WorkWithRouteState())
    val state: StateFlow<WorkWithRouteState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val team = auth.team.first { it.isNotEmpty() }
            _state.update { it.copy(team = team, repId = it.repId ?: team.first().id) }
            refresh()
        }
    }

    fun refresh() {
        val rep = _state.value.repId ?: return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (val r = repository.route(rep, todayVn().toString())) {
                is DataResult.Success -> _state.update { it.copy(loading = false, route = r.data) }
                is DataResult.Failure -> _state.update { it.copy(loading = false, error = r.error.toMessage()) }
            }
        }
    }

    fun selectRep(id: String) {
        _state.update { it.copy(repId = id, route = null) }
        refresh()
    }

    /**
     * A stop with a call of the supervisor's already open goes straight in; any
     * other asks to check in — unless a call is open elsewhere, which has to be
     * closed first (one at a time, as on the rep's app).
     */
    fun onStop(stop: WorkWithStop) {
        val route = _state.value.route ?: return
        when {
            stop.myVisitId != null -> _state.update { it.copy(openVisit = stop.myVisitId) }
            route.openVisitId != null -> _state.update {
                it.copy(message = "Đang có lượt viếng thăm chưa check-out. Mở lượt đó để kết thúc trước.")
            }
            else -> _state.update { it.copy(confirm = stop) }
        }
    }

    fun dismiss() = _state.update { it.copy(confirm = null) }

    fun checkIn() {
        val s = _state.value
        val stop = s.confirm ?: return
        val rep = s.repId ?: return
        viewModelScope.launch {
            _state.update { it.copy(checkingIn = true) }
            // No fix is not a reason to refuse: the distance is recorded when
            // there is one, and the supervisor is not policed like a rep.
            val here = runCatching { location.currentLocation() }.getOrNull()
            when (val r = repository.checkIn(rep, stop.customerId, here)) {
                is DataResult.Success -> _state.update {
                    it.copy(checkingIn = false, confirm = null, openVisit = r.data)
                }
                is DataResult.Failure -> _state.update {
                    it.copy(checkingIn = false, confirm = null, message = r.error.toMessage())
                }
            }
        }
    }

    fun openHandled() = _state.update { it.copy(openVisit = null) }
    fun messageShown() = _state.update { it.copy(message = null) }
}

/** Đi tuyến cùng nhân viên: pick a rep, see their day, check in alongside. */
@Composable
fun WorkWithRouteScreen(
    onBack: () -> Unit,
    onOpenVisit: (String) -> Unit,
    viewModel: WorkWithRouteViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }
    LaunchedEffect(Unit) {
        permissions.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    LaunchedEffect(state.openVisit) {
        state.openVisit?.let { onOpenVisit(it); viewModel.openHandled() }
    }
    LaunchedEffect(state.message) {
        state.message?.let { snackbar.showSnackbar(it); viewModel.messageShown() }
    }

    SubScreen(
        title = "Đi tuyến cùng nhân viên",
        subtitle = "Hôm nay · ${formatDay(todayVn())}",
        onBack = onBack,
        snackbar = snackbar,
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(16.dp, 8.dp)) {
                items(state.team, key = { it.id }) { m ->
                    FilterChip(selected = state.repId == m.id, onClick = { viewModel.selectRep(m.id) }, label = { Text(m.fullName) })
                }
            }
            val route = state.route
            when {
                state.loading && route == null -> LoadingBox()
                state.error != null && route == null -> ErrorBox(state.error!!, onRetry = viewModel::refresh)
                route != null -> {
                    val done = route.stops.count { it.repStatus != null }
                    Text(
                        "${route.stops.size} điểm · nhân viên đã ghé $done · ${route.stops.count { it.myStatus != null }} điểm có giám sát",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (route.stops.isEmpty()) {
                            item { Text("Hôm nay nhân viên không có điểm nào trên tuyến", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                        items(route.stops, key = { it.customerId }) { stop -> StopCard(stop) { viewModel.onStop(stop) } }
                    }
                }
            }
        }
    }

    state.confirm?.let { stop ->
        AlertDialog(
            onDismissRequest = { if (!state.checkingIn) viewModel.dismiss() },
            title = { Text("Check-in") },
            text = { Text("Bắt đầu viếng thăm ${stop.name} cùng nhân viên? Vị trí hiện tại sẽ được ghi lại.") },
            confirmButton = {
                TextButton(onClick = viewModel::checkIn, enabled = !state.checkingIn) {
                    Text(if (state.checkingIn) "Đang check-in…" else "Check-in")
                }
            },
            dismissButton = { TextButton(onClick = viewModel::dismiss, enabled = !state.checkingIn) { Text("Huỷ") } },
        )
    }
}

@Composable
private fun StopCard(stop: WorkWithStop, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${stop.code} – ${stop.name}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                if (!stop.planned) StatusPill("Ngoài tuyến", Kpi.Orange.first())
            }
            stop.address?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                val rep = stop.repStatus
                StatusPill("NV: ${rep?.label ?: "Chưa ghé"}", if (rep == null) Kpi.Slate.first() else Kpi.Blue.first())
                stop.myStatus?.let {
                    StatusPill(
                        if (it == VisitStatus.IN_PROGRESS) "Bạn: đang ghé" else "Bạn: đã ghé",
                        if (it == VisitStatus.IN_PROGRESS) Kpi.Orange.first() else Kpi.Green.first(),
                    )
                }
            }
            if (stop.repCheckIn != null || stop.repOrderAmount > 0) {
                Text(
                    listOfNotNull(
                        stop.repCheckIn?.let { "Vào lúc ${formatDateTime(it).takeLast(5)}" },
                        if (stop.repOrderAmount > 0) "Đơn ${compactMoney(stop.repOrderAmount)}" else null,
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
