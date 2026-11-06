package com.tinhcd.myesalesmanager.feature.approval

import androidx.compose.foundation.layout.Row
import com.tinhcd.myesalesmanager.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tinhcd.myesalesmanager.core.ui.formatIsoDate
import com.tinhcd.myesalesmanager.domain.DataResult
import com.tinhcd.myesalesmanager.domain.model.ApprovalPage
import com.tinhcd.myesalesmanager.domain.model.ApprovalStatus
import com.tinhcd.myesalesmanager.domain.model.Decision
import com.tinhcd.myesalesmanager.domain.model.LeaveApproval
import com.tinhcd.myesalesmanager.domain.repository.ApprovalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LeaveApprovalViewModel @Inject constructor(
    private val repository: ApprovalRepository,
) : ApprovalListViewModel<LeaveApproval>() {

    override suspend fun load(status: ApprovalStatus): DataResult<ApprovalPage<LeaveApproval>> =
        repository.leaves(status)

    override suspend fun decide(pending: PendingDecision, note: String, qty: Int?) =
        repository.decideLeave(pending.id, Decision(approve = pending.approve, note = note))
}

/** Duyệt nghỉ phép. Legacy approve_leave_application. */
@Composable
fun LeaveApprovalScreen(
    onBack: () -> Unit,
    onOpenCalendar: () -> Unit = {},
    viewModel: LeaveApprovalViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ApprovalListScaffold(
        title = stringResource(R.string.leave_title),
        state = state,
        viewModel = viewModel,
        onBack = onBack,
        header = { item { TextButton(onClick = onOpenCalendar) { Text(stringResource(R.string.leave_calendar_link)) } } },
    ) { list ->
        items(list, key = { it.id }) { leave ->
            val who = leave.salesperson?.let { "${it.name} (${it.code})" }.orEmpty()
            val period = if (leave.fromDate == leave.toDate) {
                formatIsoDate(leave.fromDate)
            } else {
                "${formatIsoDate(leave.fromDate)} – ${formatIsoDate(leave.toDate)}"
            }
            val decisionTitle = stringResource(R.string.leave_decision, who, period)
            ApprovalCard(
                status = leave.status,
                decisionNote = leave.decisionNote,
                onApprove = {
                    viewModel.ask(PendingDecision(leave.id, true, decisionTitle))
                },
                onReject = {
                    viewModel.ask(PendingDecision(leave.id, false, decisionTitle))
                },
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        who,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    StatusBadge(leave.status)
                }
                LabeledLine(stringResource(R.string.lbl_type), leave.typeName + stringResource(if (leave.isPaid) R.string.leave_paid else R.string.leave_unpaid))
                LabeledLine(stringResource(R.string.lbl_period), stringResource(R.string.leave_period_days, period, leave.days))
                LabeledLine(stringResource(R.string.lbl_reason), leave.reason)
            }
        }
    }
}
