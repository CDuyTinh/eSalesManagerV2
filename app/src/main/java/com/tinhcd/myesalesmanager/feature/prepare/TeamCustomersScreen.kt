package com.tinhcd.myesalesmanager.feature.prepare

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.tinhcd.myesalesmanager.core.ui.ErrorBox
import com.tinhcd.myesalesmanager.core.ui.LoadingBox
import com.tinhcd.myesalesmanager.core.ui.SearchBox
import com.tinhcd.myesalesmanager.core.ui.SubScreen
import com.tinhcd.myesalesmanager.core.ui.formatDateTime
import com.tinhcd.myesalesmanager.domain.DataResult
import com.tinhcd.myesalesmanager.domain.model.TeamCustomer
import com.tinhcd.myesalesmanager.domain.model.TeamMember
import com.tinhcd.myesalesmanager.domain.repository.AuthRepository
import com.tinhcd.myesalesmanager.domain.repository.PrepareRepository
import com.tinhcd.myesalesmanager.feature.approval.toMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TeamCustomersState(
    val team: List<TeamMember> = emptyList(),
    val repId: String? = null,
    val query: String = "",
    val customers: List<TeamCustomer>? = null,
    val error: String? = null,
)

@HiltViewModel
class TeamCustomersViewModel @Inject constructor(
    private val repository: PrepareRepository,
    auth: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(TeamCustomersState())
    val state: StateFlow<TeamCustomersState> = _state.asStateFlow()

    private var search: Job? = null

    init {
        viewModelScope.launch {
            val team = auth.team.first { it.isNotEmpty() }
            _state.update { it.copy(team = team, repId = it.repId ?: team.first().id) }
            refresh()
        }
    }

    fun refresh() {
        val s = _state.value
        val rep = s.repId ?: return
        search?.cancel()
        search = viewModelScope.launch {
            when (val r = repository.customers(rep, s.query)) {
                is DataResult.Success -> _state.update { it.copy(customers = r.data, error = null) }
                is DataResult.Failure -> _state.update { it.copy(error = r.error.toMessage()) }
            }
        }
    }

    fun selectRep(id: String) {
        _state.update { it.copy(repId = id, customers = null) }
        refresh()
    }

    fun query(q: String) {
        _state.update { it.copy(query = q) }
        search?.cancel()
        search = viewModelScope.launch {
            delay(300)
            refresh()
        }
    }
}

/** Sửa khách hàng theo nhân viên: pick a rep, then one of their outlets. */
@Composable
fun TeamCustomersScreen(
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    viewModel: TeamCustomersViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    SubScreen(title = "Sửa khách hàng", subtitle = "Điểm bán trên tuyến của nhân viên", onBack = onBack) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(16.dp, 8.dp)) {
                items(state.team, key = { it.id }) { m ->
                    FilterChip(selected = state.repId == m.id, onClick = { viewModel.selectRep(m.id) }, label = { Text(m.fullName) })
                }
            }
            SearchBox(state.query, viewModel::query, placeholder = "Mã, tên, địa chỉ, SĐT", modifier = Modifier.padding(horizontal = 16.dp))
            val list = state.customers
            when {
                list == null && state.error != null -> ErrorBox(state.error!!, onRetry = viewModel::refresh)
                list == null -> LoadingBox()
                else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (list.isEmpty()) item { Text("Không có điểm bán", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    items(list, key = { it.id }) { c ->
                        Card(onClick = { onOpen(c.id) }, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("${c.code} – ${c.name}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                c.address?.let { Text(it, style = MaterialTheme.typography.labelSmall) }
                                Text(
                                    listOfNotNull(
                                        c.contactName, c.phone, c.routes?.let { "Tuyến $it" },
                                        c.editedAt?.let { "sửa ${formatDateTime(it)}" },
                                    ).joinToString(" · "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
