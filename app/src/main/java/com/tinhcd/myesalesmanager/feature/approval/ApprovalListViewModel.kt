package com.tinhcd.myesalesmanager.feature.approval

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tinhcd.myesalesmanager.domain.DataResult
import com.tinhcd.myesalesmanager.domain.model.ApprovalPage
import com.tinhcd.myesalesmanager.domain.model.ApprovalStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ApprovalListState<T>(
    val status: ApprovalStatus = ApprovalStatus.PENDING,
    val counts: Map<ApprovalStatus, Int> = emptyMap(),
    val items: List<T> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
    /** The ruling being confirmed in the dialog, if any. */
    val pending: PendingDecision? = null,
    val deciding: Boolean = false,
    /** One-off message for the snackbar. */
    val message: String? = null,
)

/**
 * The behaviour every approval list shares — filter by status, ask, confirm,
 * reload — so each screen only says how to load its rows and how to rule on one.
 */
abstract class ApprovalListViewModel<T> : ViewModel() {

    private val _state = MutableStateFlow(ApprovalListState<T>())
    val state: StateFlow<ApprovalListState<T>> = _state.asStateFlow()

    private var loadJob: Job? = null

    protected abstract suspend fun load(status: ApprovalStatus): DataResult<ApprovalPage<T>>

    protected abstract suspend fun decide(
        pending: PendingDecision,
        note: String,
        qty: Int?,
    ): DataResult<Unit>

    fun refresh() {
        val status = _state.value.status
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (val result = load(status)) {
                is DataResult.Success -> _state.update {
                    it.copy(loading = false, counts = result.data.counts, items = result.data.items)
                }
                is DataResult.Failure -> _state.update {
                    it.copy(loading = false, error = result.error.toMessage())
                }
            }
        }
    }

    fun selectStatus(status: ApprovalStatus) {
        if (status == _state.value.status) return
        _state.update { it.copy(status = status, items = emptyList()) }
        refresh()
    }

    fun ask(pending: PendingDecision) = _state.update { it.copy(pending = pending) }

    fun dismiss() = _state.update { it.copy(pending = null) }

    fun confirm(note: String, qty: Int?) {
        val pending = _state.value.pending ?: return
        viewModelScope.launch {
            _state.update { it.copy(deciding = true) }
            when (val result = decide(pending, note, qty)) {
                is DataResult.Success -> {
                    _state.update {
                        it.copy(
                            deciding = false,
                            pending = null,
                            message = if (pending.approve) "Đã duyệt" else "Đã từ chối",
                        )
                    }
                    refresh()
                }
                is DataResult.Failure -> _state.update {
                    it.copy(deciding = false, message = result.error.toMessage())
                }
            }
        }
    }

    fun messageShown() = _state.update { it.copy(message = null) }
}
