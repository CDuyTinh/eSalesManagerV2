package com.tinhcd.myesalesmanager.feature.approval

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.tinhcd.myesalesmanager.core.ui.ErrorBox
import com.tinhcd.myesalesmanager.core.ui.LoadingBox
import com.tinhcd.myesalesmanager.core.ui.SubScreen

/**
 * Header, status chips, list and the decision dialog, wired to an
 * [ApprovalListViewModel]. Screens supply only the rows.
 */
@Composable
fun <T> ApprovalListScaffold(
    title: String,
    state: ApprovalListState<T>,
    viewModel: ApprovalListViewModel<T>,
    onBack: () -> Unit,
    header: (LazyListScope.() -> Unit)? = null,
    rows: LazyListScope.(List<T>) -> Unit,
) {
    val snackbar = remember { SnackbarHostState() }

    // Reload on every return to the screen: a detail screen may have ruled on
    // an item, and the list must not keep showing it as pending.
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.messageShown()
        }
    }

    SubScreen(title = title, onBack = onBack, snackbar = snackbar) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            StatusChips(selected = state.status, counts = state.counts, onSelect = viewModel::selectStatus)
            when {
                state.loading && state.items.isEmpty() -> LoadingBox()
                state.error != null && state.items.isEmpty() ->
                    ErrorBox(message = state.error, onRetry = viewModel::refresh)
                else -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    header?.invoke(this)
                    if (state.items.isEmpty()) {
                        item {
                            Text(
                                "Không có mục nào ở trạng thái \"${state.status.label}\"",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 32.dp).fillMaxSize(),
                            )
                        }
                    } else {
                        rows(state.items)
                    }
                }
            }
        }
    }

    state.pending?.let { pending ->
        DecisionDialog(
            pending = pending,
            busy = state.deciding,
            onConfirm = viewModel::confirm,
            onDismiss = viewModel::dismiss,
        )
    }
}
