package com.tinhcd.myesalesmanager.feature.personal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.tinhcd.myesalesmanager.core.ui.SubScreen
import com.tinhcd.myesalesmanager.core.ui.formatIsoDate
import com.tinhcd.myesalesmanager.domain.DataResult
import com.tinhcd.myesalesmanager.domain.model.AppNotification
import com.tinhcd.myesalesmanager.domain.model.NotificationFeed
import com.tinhcd.myesalesmanager.domain.repository.PersonalRepository
import com.tinhcd.myesalesmanager.feature.approval.toMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationsState(val feed: NotificationFeed? = null, val error: String? = null)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val repository: PersonalRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationsState())
    val state: StateFlow<NotificationsState> = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            when (val r = repository.notifications()) {
                is DataResult.Success -> _state.update { it.copy(feed = r.data, error = null) }
                is DataResult.Failure -> _state.update { it.copy(error = r.error.toMessage()) }
            }
        }
    }

    /** Marks it read on the way to wherever it leads. */
    fun open(n: AppNotification) {
        if (n.isRead) return
        _state.update { s ->
            s.copy(feed = s.feed?.let { f ->
                f.copy(
                    items = f.items.map { if (it.sourceId == n.sourceId && it.kindCode == n.kindCode) it.copy(isRead = true) else it },
                    unread = (f.unread - 1).coerceAtLeast(0),
                )
            })
        }
        viewModelScope.launch { repository.markRead(n.kindCode, n.sourceId) }
    }

    fun markAll() {
        viewModelScope.launch {
            if (repository.markAllRead() is DataResult.Success) refresh()
        }
    }
}

/**
 * Thông báo (legacy API_GetPPC_Notify_Manager): what the team sent up for a
 * decision, plus the manager's own results and the branch's programmes.
 */
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onOpen: (AppNotification) -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    val feed = state.feed
    SubScreen(
        title = "Thông báo",
        subtitle = feed?.let { if (it.unread > 0) "${it.unread} chưa đọc" else "Đã đọc hết" },
        onBack = onBack,
    ) { padding ->
        when {
            feed == null && state.error != null -> ErrorBox(state.error!!, onRetry = viewModel::refresh, modifier = Modifier.padding(padding))
            feed == null -> LoadingBox(Modifier.padding(padding))
            else -> LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (feed.unread > 0) {
                    item {
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            TextButton(onClick = viewModel::markAll) { Text("Đánh dấu đã đọc tất cả") }
                        }
                    }
                }
                if (feed.items.isEmpty()) {
                    item { Text("Chưa có thông báo", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                items(feed.items, key = { it.kindCode + it.sourceId }) { n ->
                    Card(onClick = { viewModel.open(n); onOpen(n) }, modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                            Box(
                                Modifier
                                    .padding(top = 6.dp)
                                    .size(8.dp)
                                    .background(if (n.isRead) MaterialTheme.colorScheme.surface else Kpi.Orange.first(), CircleShape),
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    n.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (n.isRead) FontWeight.Normal else FontWeight.SemiBold,
                                )
                                if (n.body.isNotBlank()) Text(n.body, style = MaterialTheme.typography.labelSmall)
                                n.fromDate?.let {
                                    Text(formatIsoDate(it), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
