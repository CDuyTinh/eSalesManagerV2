package com.tinhcd.myesalesmanager.feature.approval

import androidx.compose.foundation.layout.Row
import com.tinhcd.myesalesmanager.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.tinhcd.myesalesmanager.domain.model.DisplayResult
import com.tinhcd.myesalesmanager.domain.repository.ApprovalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DisplayResultApprovalViewModel @Inject constructor(
    private val repository: ApprovalRepository,
) : ApprovalListViewModel<DisplayResult>() {

    override suspend fun load(status: ApprovalStatus): DataResult<ApprovalPage<DisplayResult>> =
        repository.displayResults(status)

    override suspend fun decide(pending: PendingDecision, note: String, qty: Int?) =
        repository.decideDisplayResult(pending.id, Decision(approve = pending.approve, note = note))
}

/**
 * Kết quả trưng bày: approved display registrations, with what the audits
 * showed. Approving confirms the reward, which the rep then pays on the
 * outlet's next orders (Trả thưởng trưng bày); refusing needs a reason. The
 * audits are the evidence, the decision is the manager's — as the legacy's.
 */
@Composable
fun DisplayResultApprovalScreen(
    onBack: () -> Unit,
    viewModel: DisplayResultApprovalViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ApprovalListScaffold(
        title = stringResource(R.string.dr_title),
        state = state,
        viewModel = viewModel,
        onBack = onBack,
    ) { list ->
        items(list, key = { it.id }) { r ->
            val title = "${r.customerName} · ${r.programCode}"
            ApprovalCard(
                status = r.status,
                decisionNote = r.decisionNote,
                onApprove = { viewModel.ask(PendingDecision(r.id, true, title)) },
                onReject = { viewModel.ask(PendingDecision(r.id, false, title)) },
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "${r.customerName} (${r.customerCode})",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    StatusBadge(r.status)
                }
                LabeledLine(stringResource(R.string.lbl_program), "${r.programCode} – ${r.programName}")
                LabeledLine(stringResource(R.string.lbl_level), r.levelName)
                LabeledLine(stringResource(R.string.lbl_audit_result), stringResource(R.string.dr_achieved, r.achieved, r.audits))
                LabeledLine(stringResource(R.string.lbl_last_audit), r.lastAudit?.let(::formatIsoDate))
                LabeledLine(stringResource(R.string.lbl_reward), r.reward)
                LabeledLine(stringResource(R.string.lbl_program_end), r.programTo?.let(::formatIsoDate))
                LabeledLine(stringResource(R.string.lbl_staff), r.salesperson?.let { "${it.name} (${it.code})" })
            }
        }
    }
}
