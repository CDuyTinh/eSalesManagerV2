package com.tinhcd.myesalesmanager.feature.approval

import androidx.compose.foundation.layout.Arrangement
import com.tinhcd.myesalesmanager.R
import androidx.compose.ui.res.stringResource
import com.tinhcd.myesalesmanager.domain.model.TradeKind
import com.tinhcd.myesalesmanager.core.ui.theme.status
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tinhcd.myesalesmanager.domain.AppError
import com.tinhcd.myesalesmanager.domain.model.ApprovalStatus

/** Chips for the three statuses, each with its own count whichever is selected. */
@Composable
fun StatusChips(
    selected: ApprovalStatus,
    counts: Map<ApprovalStatus, Int>,
    onSelect: (ApprovalStatus) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    ) {
        items(ApprovalStatus.entries) { status ->
            FilterChip(
                selected = status == selected,
                onClick = { onSelect(status) },
                label = { Text("${status.label()} (${counts[status] ?: 0})") },
            )
        }
    }
}

@Composable
fun StatusBadge(status: ApprovalStatus) {
    val (bg, fg) = when (status) {
        ApprovalStatus.PENDING -> MaterialTheme.status.warningContainer to MaterialTheme.status.onWarningContainer
        ApprovalStatus.APPROVED -> MaterialTheme.status.successContainer to MaterialTheme.status.onSuccessContainer
        ApprovalStatus.REJECTED -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    }
    Surface(color = bg, shape = RoundedCornerShape(8.dp)) {
        Text(
            status.label(),
            color = fg,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

/**
 * The card every approval list renders: [content] describes the item, and a
 * pending item gets the two buttons underneath.
 */
@Composable
fun ApprovalCard(
    status: ApprovalStatus,
    decisionNote: String?,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val body: @Composable () -> Unit = {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            content()
            if (!decisionNote.isNullOrBlank() && status != ApprovalStatus.PENDING) {
                Text(
                    stringResource(R.string.appr_decision_note, decisionNote),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (status == ApprovalStatus.PENDING) {
                DecisionButtons(onApprove = onApprove, onReject = onReject)
            }
        }
    }
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) { body() }
    } else {
        Card(modifier = modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) { body() }
    }
}

@Composable
fun DecisionButtons(onApprove: () -> Unit, onReject: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(
            onClick = onReject,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) { Text(stringResource(R.string.appr_reject)) }
        Button(onClick = onApprove, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.appr_approve)) }
    }
}

/** Which ruling is being confirmed, and on what. */
data class PendingDecision(
    val id: String,
    val approve: Boolean,
    val title: String,
    /** POSM: the quantity asked for, so the dialog can offer to grant less. */
    val maxQty: Int? = null,
    /** Overrides "Duyệt"/"Từ chối" for moves that are neither, e.g. calling a task off. */
    val heading: String? = null,
    /** Overrides the reason field's label to match [heading]. */
    val reasonLabel: String? = null,
)

/** The ready-made refusal reasons from mgr-bootstrap, provided at the app root. */
val LocalRejectReasons = compositionLocalOf<List<String>> { emptyList() }

/**
 * Confirms a ruling. Refusing needs a reason — the rep sees it — so the confirm
 * button stays disabled until there is one; approving takes an optional note.
 */
@Composable
fun DecisionDialog(
    pending: PendingDecision,
    busy: Boolean,
    onConfirm: (note: String, qty: Int?) -> Unit,
    onDismiss: () -> Unit,
) {
    var note by remember(pending) { mutableStateOf("") }
    var qty by remember(pending) { mutableStateOf(pending.maxQty?.toString().orEmpty()) }
    val qtyValue = qty.toIntOrNull()
    val qtyValid = pending.maxQty == null || !pending.approve ||
        (qtyValue != null && qtyValue in 1..pending.maxQty)
    val canConfirm = !busy && qtyValid && (pending.approve || note.isNotBlank())

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(pending.heading ?: stringResource(if (pending.approve) R.string.appr_approve else R.string.appr_reject)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(pending.title, style = MaterialTheme.typography.bodyLarge)
                if (pending.approve && pending.maxQty != null) {
                    OutlinedTextField(
                        value = qty,
                        onValueChange = { qty = it.filter(Char::isDigit).take(5) },
                        label = { Text(stringResource(R.string.appr_qty_label, pending.maxQty)) },
                        singleLine = true,
                        isError = !qtyValid,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                // One tap for the usual reasons; the text stays editable for the rest.
                val reasons = LocalRejectReasons.current
                if (!pending.approve && reasons.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        reasons.forEach { reason ->
                            FilterChip(selected = note == reason, onClick = { note = reason }, label = { Text(reason) })
                        }
                    }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = {
                        Text(
                            pending.reasonLabel
                                ?: stringResource(if (pending.approve) R.string.appr_note_optional else R.string.appr_reject_reason),
                        )
                    },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(note, if (pending.approve) qtyValue else null) },
                enabled = canConfirm,
            ) { Text(stringResource(if (busy) R.string.action_saving else R.string.action_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
fun LabeledLine(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Row(verticalAlignment = Alignment.Top) {
        Text("$label: ", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

fun AppError.toMessage(): String = when (this) {
    is AppError.Rule -> message ?: when (key) {
        "reject_needs_note" -> "Vui lòng nhập lý do từ chối"
        else -> "Dữ liệu không hợp lệ"
    }
    is AppError.Network -> "Không có kết nối mạng"
    is AppError.Auth -> "Phiên đăng nhập đã hết hạn"
    is AppError.Server -> message?.takeIf { it.isNotBlank() } ?: "Lỗi máy chủ${code?.let { " ($it)" }.orEmpty()}"
    is AppError.Unknown -> message?.takeIf { it.isNotBlank() } ?: "Đã có lỗi xảy ra"
}

/** The status's name, in the app's language. */
@Composable
fun ApprovalStatus.label(): String = stringResource(
    when (this) {
        ApprovalStatus.PENDING -> R.string.status_pending
        ApprovalStatus.APPROVED -> R.string.status_approved
        ApprovalStatus.REJECTED -> R.string.status_rejected
    },
)

/** The registration kind's name, in the app's language, as it reads mid-sentence. */
@Composable
fun TradeKind.label(): String = stringResource(
    when (this) {
        TradeKind.DISPLAY -> R.string.trade_display
        TradeKind.LOYALTY -> R.string.trade_loyalty
        TradeKind.POSM -> R.string.trade_posm
    },
)
