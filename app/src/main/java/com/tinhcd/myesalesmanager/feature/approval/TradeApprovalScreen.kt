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
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tinhcd.myesalesmanager.core.ui.formatIsoDate
import com.tinhcd.myesalesmanager.domain.DataResult
import com.tinhcd.myesalesmanager.domain.model.ApprovalPage
import com.tinhcd.myesalesmanager.domain.model.ApprovalStatus
import com.tinhcd.myesalesmanager.domain.model.Decision
import com.tinhcd.myesalesmanager.domain.model.TradeApproval
import com.tinhcd.myesalesmanager.domain.model.TradeKind
import com.tinhcd.myesalesmanager.domain.repository.ApprovalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** Nav argument naming which family of sign-ups the screen shows. */
const val ARG_TRADE_KIND = "kind"

@HiltViewModel
class TradeApprovalViewModel @Inject constructor(
    override val strings: AppStrings,
    savedState: SavedStateHandle,
    private val repository: ApprovalRepository,
) : ApprovalListViewModel<TradeApproval>() {

    val kind: TradeKind = TradeKind.of(savedState.get<String>(ARG_TRADE_KIND)) ?: TradeKind.DISPLAY

    override suspend fun load(status: ApprovalStatus): DataResult<ApprovalPage<TradeApproval>> =
        repository.trades(kind, status)

    override suspend fun decide(pending: PendingDecision, note: String, qty: Int?) =
        repository.decideTrade(
            kind,
            pending.id,
            Decision(approve = pending.approve, note = note, approvedQty = qty),
        )
}

/**
 * Duyệt đăng ký chương trình. One screen per family, as the legacy approval
 * menu opened ApproveNewTradeProgramScreen with a different type each time.
 */
@Composable
fun TradeApprovalScreen(
    onBack: () -> Unit,
    viewModel: TradeApprovalViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val kind = viewModel.kind

    ApprovalListScaffold(
        title = stringResource(R.string.trade_title, kind.label()),
        state = state,
        viewModel = viewModel,
        onBack = onBack,
    ) { list ->
        items(list, key = { it.id }) { reg ->
            val title = "${reg.customerName} · ${reg.programCode}"
            ApprovalCard(
                status = reg.status,
                decisionNote = reg.decisionNote,
                onApprove = {
                    viewModel.ask(
                        PendingDecision(
                            reg.id, true, title,
                            maxQty = reg.qty.takeIf { kind == TradeKind.POSM },
                        ),
                    )
                },
                onReject = { viewModel.ask(PendingDecision(reg.id, false, title)) },
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "${reg.customerName} (${reg.customerCode})",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    StatusBadge(reg.status)
                }
                LabeledLine(stringResource(R.string.lbl_address), reg.customerAddress)
                LabeledLine(stringResource(R.string.lbl_program), "${reg.programCode} – ${reg.programName}")
                LabeledLine(stringResource(if (kind == TradeKind.POSM) R.string.lbl_item else R.string.lbl_level), reg.detailName)
                if (kind == TradeKind.POSM) {
                    LabeledLine(
                        stringResource(R.string.lbl_qty),
                        "${reg.qty}" + (reg.approvedQty?.takeIf { reg.status == ApprovalStatus.APPROVED }
                            ?.let { stringResource(R.string.trade_approved_qty, it) } ?: ""),
                    )
                } else {
                    LabeledLine(stringResource(R.string.lbl_lots), reg.qty.toString())
                }
                LabeledLine(stringResource(R.string.lbl_reason), reg.reason)
                LabeledLine(
                    stringResource(R.string.lbl_staff),
                    reg.salesperson?.let { "${it.name} (${it.code})" },
                )
                LabeledLine(stringResource(R.string.lbl_registered), formatIsoDate(reg.registeredAt))
            }
        }
    }
}
