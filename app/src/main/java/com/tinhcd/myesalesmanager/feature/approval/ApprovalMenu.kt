package com.tinhcd.myesalesmanager.feature.approval

import com.tinhcd.myesalesmanager.core.ui.GridEntry
import com.tinhcd.myesalesmanager.core.ui.MenuGrid
import com.tinhcd.myesalesmanager.core.ui.tileColor
import com.tinhcd.myesalesmanager.core.ui.MenuIcon
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Loyalty
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.automirrored.filled.ViewQuilt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.tinhcd.myesalesmanager.domain.DataResult
import com.tinhcd.myesalesmanager.domain.model.ApprovalCounts
import com.tinhcd.myesalesmanager.domain.model.TradeKind
import com.tinhcd.myesalesmanager.domain.repository.ApprovalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Where each menu entry leads. */
sealed interface ApprovalTarget {
    data object Leave : ApprovalTarget
    data object Customer : ApprovalTarget
    data class Trade(val kind: TradeKind) : ApprovalTarget

    /** Plans reps sent up — the team plan list, opened on "Chờ duyệt". Legacy TaskApprove. */
    data object WorkPlan : ApprovalTarget
}

@HiltViewModel
class ApprovalMenuViewModel @Inject constructor(
    private val repository: ApprovalRepository,
) : ViewModel() {

    private val _counts = MutableStateFlow<ApprovalCounts?>(null)
    val counts: StateFlow<ApprovalCounts?> = _counts.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            // A failed count leaves the previous numbers up rather than blanking
            // the menu: the entries still work without a badge.
            (repository.counts() as? DataResult.Success)?.let { _counts.value = it.data }
        }
    }
}

private data class MenuEntry(
    val target: ApprovalTarget,
    val title: String,
    val icon: ImageVector,
    val count: (ApprovalCounts) -> Int,
)

private val entries = listOf(
    MenuEntry(ApprovalTarget.WorkPlan, "Kế hoạch làm việc", Icons.AutoMirrored.Filled.Assignment) { it.workPlan },
    MenuEntry(ApprovalTarget.Leave, "Nghỉ phép", Icons.Default.EventBusy) { it.leave },
    MenuEntry(ApprovalTarget.Customer, "Khách hàng mới", Icons.Default.Storefront) { it.customer },
    MenuEntry(ApprovalTarget.Trade(TradeKind.DISPLAY), "Đăng ký trưng bày", Icons.AutoMirrored.Filled.ViewQuilt) { it.display },
    MenuEntry(ApprovalTarget.Trade(TradeKind.LOYALTY), "Đăng ký tích lũy", Icons.Default.Loyalty) { it.loyalty },
    MenuEntry(ApprovalTarget.Trade(TradeKind.POSM), "Đăng ký POSM", Icons.Default.Inventory2) { it.posm },
)

/** Tab "Phê duyệt". Legacy ApprovalMenuScreen. */
@Composable
fun ApprovalMenu(
    onOpen: (ApprovalTarget) -> Unit,
    viewModel: ApprovalMenuViewModel = hiltViewModel(),
) {
    val counts by viewModel.counts.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    MenuGrid(
        entries = entries.map { GridEntry(it.title, it.icon, badge = counts?.let(it.count) ?: 0) },
        onClick = { onOpen(entries[it].target) },
        caption = counts?.let { "Đang chờ duyệt: ${it.total}" } ?: "Đang tải…",
    )
}
