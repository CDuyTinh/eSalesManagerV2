package com.tinhcd.myesalesmanager.feature.workplan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/** Where an entry on the tasks tab leads; null for one not built yet. */
enum class TaskTarget { TEAM_PLANS, ASSIGN, WORK_WITH, ROUTES, EDAC, NOTES, STOCK, SALES_FOCUS, EDIT_CUSTOMERS }

private data class TaskEntry(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val target: TaskTarget?,
)

/**
 * The legacy app's "Công việc" sheet, as a tab. Entries for the later phases are
 * listed but disabled, so the supervisor sees where things will live.
 */
private val entries = listOf(
    TaskEntry("Công việc của nhóm", "Kế hoạch, việc đã giao, duyệt kế hoạch", Icons.AutoMirrored.Filled.Assignment, TaskTarget.TEAM_PLANS),
    TaskEntry("Giao việc mới", "Giao công việc cho nhân viên", Icons.Default.AddTask, TaskTarget.ASSIGN),
    TaskEntry("Đi tuyến cùng nhân viên", "Check-in cùng nhân viên, chấm trưng bày", Icons.AutoMirrored.Filled.DirectionsWalk, TaskTarget.WORK_WITH),
    TaskEntry("Lập tuyến (MCP) & phân tuyến", "Điểm bán, lịch ghé, giao tuyến cho nhân viên", Icons.Default.Route, TaskTarget.ROUTES),
    TaskEntry("Huấn luyện EDAC", "Kèm cặp nhân viên 8 bước tại điểm bán", Icons.Default.School, TaskTarget.EDAC),
    TaskEntry("Ghi chú công việc", "Việc cần làm của tôi", Icons.AutoMirrored.Filled.StickyNote2, TaskTarget.NOTES),
    TaskEntry("Trọng tâm bán hàng", "Sản phẩm trọng tâm, chương trình đang chạy", Icons.Default.Star, TaskTarget.SALES_FOCUS),
    TaskEntry("Tồn kho", "Tồn kho tại kho nhà phân phối", Icons.Default.Warehouse, TaskTarget.STOCK),
    TaskEntry("Sửa khách hàng", "Sửa thông tin điểm bán theo nhân viên", Icons.Default.Storefront, TaskTarget.EDIT_CUSTOMERS),
)

@Composable
fun TasksMenu(onOpen: (TaskTarget) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(entries) { e ->
            val enabled = e.target != null
            Card(
                onClick = { e.target?.let(onOpen) },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth().alpha(if (enabled) 1f else 0.5f),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
                    Icon(e.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(e.title, style = MaterialTheme.typography.bodyLarge)
                        Text(e.subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (enabled) Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                }
            }
        }
    }
}
