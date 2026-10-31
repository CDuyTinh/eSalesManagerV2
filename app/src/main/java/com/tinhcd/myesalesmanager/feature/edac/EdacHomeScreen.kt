package com.tinhcd.myesalesmanager.feature.edac

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
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
import com.tinhcd.myesalesmanager.core.ui.ErrorBox
import com.tinhcd.myesalesmanager.core.ui.Kpi
import com.tinhcd.myesalesmanager.core.ui.LoadingBox
import com.tinhcd.myesalesmanager.core.ui.StatusPill
import com.tinhcd.myesalesmanager.core.ui.SubScreen
import com.tinhcd.myesalesmanager.core.ui.formatIsoDate
import com.tinhcd.myesalesmanager.core.ui.formatWindow
import com.tinhcd.myesalesmanager.core.ui.todayVn
import com.tinhcd.myesalesmanager.domain.DataResult
import com.tinhcd.myesalesmanager.domain.model.EdacHistoryItem
import com.tinhcd.myesalesmanager.domain.model.EdacSession
import com.tinhcd.myesalesmanager.domain.model.EdacStep
import com.tinhcd.myesalesmanager.domain.model.TeamMember
import com.tinhcd.myesalesmanager.domain.repository.AuthRepository
import com.tinhcd.myesalesmanager.domain.repository.EdacRepository
import com.tinhcd.myesalesmanager.feature.approval.toMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EdacHomeState(
    val sessions: List<EdacSession>? = null,
    val history: List<EdacHistoryItem> = emptyList(),
    val error: String? = null,
    val picking: Boolean = false,
    val starting: Boolean = false,
    val message: String? = null,
    /** A training to navigate into, once started. */
    val open: String? = null,
)

@HiltViewModel
class EdacHomeViewModel @Inject constructor(
    private val repository: EdacRepository,
    auth: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(EdacHomeState())
    val state: StateFlow<EdacHomeState> = _state.asStateFlow()

    val team: StateFlow<List<TeamMember>> = auth.team.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun refresh() {
        viewModelScope.launch {
            val today = todayVn()
            _state.update { it.copy(error = null) }
            when (val r = repository.sessions(today.toString())) {
                is DataResult.Success -> _state.update { it.copy(sessions = r.data) }
                is DataResult.Failure -> _state.update { it.copy(error = r.error.toMessage()) }
            }
            val h = repository.history(today.minusDays(60).toString(), today.toString())
            if (h is DataResult.Success) _state.update { it.copy(history = h.data) }
        }
    }

    fun pickRep() = _state.update { it.copy(picking = true) }
    fun dismiss() = _state.update { if (it.starting) it else it.copy(picking = false) }

    /** A plan already begun is reopened; one not yet begun is started from the plan. */
    fun open(session: EdacSession) {
        if (session.trainingId != null) _state.update { it.copy(open = session.trainingId) } else start(null, session.planId)
    }

    fun start(repId: String?, planId: String?) {
        viewModelScope.launch {
            _state.update { it.copy(starting = true) }
            when (val r = repository.start(repId, planId)) {
                is DataResult.Success -> _state.update { it.copy(starting = false, picking = false, open = r.data) }
                is DataResult.Failure -> _state.update { it.copy(starting = false, message = r.error.toMessage()) }
            }
        }
    }

    fun opened() = _state.update { it.copy(open = null) }
    fun messageShown() = _state.update { it.copy(message = null) }
}

/** Huấn luyện EDAC: today's coaching plans, a way to begin one, past days. */
@Composable
fun EdacHomeScreen(
    onBack: () -> Unit,
    onOpenTraining: (String) -> Unit,
    onOpenResult: (String) -> Unit,
    onOpenCriteria: () -> Unit = {},
    viewModel: EdacHomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val team by viewModel.team.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    LaunchedEffect(state.open) { state.open?.let { onOpenTraining(it); viewModel.opened() } }
    LaunchedEffect(state.message) { state.message?.let { snackbar.showSnackbar(it); viewModel.messageShown() } }

    SubScreen(title = "Huấn luyện EDAC", subtitle = "Kèm cặp nhân viên tại điểm bán", onBack = onBack, snackbar = snackbar) { padding ->
        val sessions = state.sessions
        when {
            sessions == null && state.error != null -> ErrorBox(state.error!!, onRetry = viewModel::refresh, modifier = Modifier.padding(padding))
            sessions == null -> LoadingBox(Modifier.padding(padding))
            else -> LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    TextButton(onClick = onOpenCriteria) { Text("Bộ tiêu chí EDAC ›") }
                }
                item { SectionTitle("Hôm nay") }
                if (sessions.isEmpty()) {
                    item {
                        Text(
                            "Chưa có buổi huấn luyện nào hôm nay. Bấm \"Bắt đầu huấn luyện\" để chọn nhân viên.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(sessions, key = { it.planId }) { s -> SessionCard(s) { viewModel.open(s) } }

                val past = state.history.filter { it.id !in sessions.mapNotNull { s -> s.trainingId } }
                if (past.isNotEmpty()) {
                    item { SectionTitle("Lịch sử huấn luyện") }
                    items(past, key = { it.id }) { h ->
                        HistoryCard(h) { if (h.completed) onOpenResult(h.id) else onOpenTraining(h.id) }
                    }
                }
            }
        }
        if (state.sessions != null) {
            Box(Modifier.fillMaxSize().padding(padding).padding(16.dp), contentAlignment = Alignment.BottomEnd) {
                ExtendedFloatingActionButton(
                    onClick = viewModel::pickRep,
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                    text = { Text("Bắt đầu huấn luyện") },
                )
            }
        }
    }

    if (state.picking) {
        AlertDialog(
            onDismissRequest = viewModel::dismiss,
            title = { Text("Huấn luyện nhân viên") },
            text = {
                Column {
                    team.forEach { m ->
                        Text(
                            "${m.fullName} (${m.code})",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !state.starting) { viewModel.start(m.id, null) }
                                .padding(vertical = 12.dp),
                        )
                    }
                    if (state.starting) Text("Đang bắt đầu…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = viewModel::dismiss, enabled = !state.starting) { Text("Huỷ") } },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
}

@Composable
private fun SessionCard(s: EdacSession, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(s.rep.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                ProgressPill(s.trainingId != null, s.completed, s.currentStep)
            }
            Text(s.content, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatWindow(s.fromAt, s.toAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun HistoryCard(h: EdacHistoryItem, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${formatIsoDate(h.trainingDate)} · ${h.rep.name}",
                    style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f),
                )
                ProgressPill(true, h.completed, h.currentStep)
            }
            Text("${h.outlets} điểm bán", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            h.result?.let { Text(it, style = MaterialTheme.typography.labelSmall, maxLines = 2) }
        }
    }
}

@Composable
private fun ProgressPill(started: Boolean, completed: Boolean, currentStep: Int) = when {
    completed -> StatusPill("Hoàn tất", Kpi.Green.first())
    started -> StatusPill("Bước ${currentStep + 1}/8 · ${EdacStep.of(currentStep).title}", Kpi.Orange.first())
    else -> StatusPill("Chưa bắt đầu", Kpi.Slate.first())
}
