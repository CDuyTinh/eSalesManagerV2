package com.tinhcd.myesalesmanager.feature.approval

import androidx.compose.foundation.layout.Row
import com.tinhcd.myesalesmanager.core.ui.AppStrings
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
import com.tinhcd.myesalesmanager.domain.model.CustomerApproval
import com.tinhcd.myesalesmanager.domain.model.Decision
import com.tinhcd.myesalesmanager.domain.repository.ApprovalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CustomerApprovalViewModel @Inject constructor(
    override val strings: AppStrings,
    private val repository: ApprovalRepository,
) : ApprovalListViewModel<CustomerApproval>() {

    override suspend fun load(status: ApprovalStatus): DataResult<ApprovalPage<CustomerApproval>> =
        repository.customers(status, null)

    /** Quick reject from the list; approving goes through the detail screen. */
    override suspend fun decide(pending: PendingDecision, note: String, qty: Int?) =
        repository.decideCustomer(pending.id, Decision(approve = pending.approve, note = note))
}

/**
 * Duyệt khách hàng mới. Approving opens the dossier first — photos and the
 * route to put the outlet on are what the decision is actually made from —
 * while a refusal can be given straight from the list.
 */
@Composable
fun CustomerApprovalScreen(
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    viewModel: CustomerApprovalViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ApprovalListScaffold(
        title = stringResource(R.string.cust_approval_title),
        state = state,
        viewModel = viewModel,
        onBack = onBack,
    ) { list ->
        items(list, key = { it.id }) { c ->
            ApprovalCard(
                status = c.status,
                decisionNote = c.decisionNote,
                onApprove = { onOpen(c.id) },
                onReject = { viewModel.ask(PendingDecision(c.id, false, "${c.name} (${c.code})")) },
                onClick = { onOpen(c.id) },
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        c.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    StatusBadge(c.status)
                }
                LabeledLine(stringResource(R.string.lbl_code), c.code)
                LabeledLine(stringResource(R.string.lbl_address), c.address)
                LabeledLine(stringResource(R.string.lbl_phone), c.phone)
                LabeledLine(stringResource(R.string.lbl_staff), c.salesperson?.let { "${it.name} (${it.code})" })
                LabeledLine(stringResource(R.string.lbl_registered), formatIsoDate(c.createdAt))
                LabeledLine(stringResource(R.string.lbl_staff_note), c.note)
                if (c.photoCount > 0) LabeledLine(stringResource(R.string.lbl_photo), stringResource(R.string.photo_count, c.photoCount))
            }
        }
    }
}
