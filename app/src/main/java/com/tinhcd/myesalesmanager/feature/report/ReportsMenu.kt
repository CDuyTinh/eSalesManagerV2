package com.tinhcd.myesalesmanager.feature.report

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
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.ViewCarousel
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
import com.tinhcd.myesalesmanager.domain.model.ReportKind

private fun ReportKind.icon(): ImageVector = when (this) {
    ReportKind.ACTIVITIES -> Icons.Default.Today
    ReportKind.SALES -> Icons.Default.BarChart
    ReportKind.ORDERS -> Icons.Default.Receipt
    ReportKind.SKU -> Icons.Default.Inventory2
    ReportKind.TRADE -> Icons.Default.ViewCarousel
    ReportKind.WORK -> Icons.AutoMirrored.Filled.Assignment
    ReportKind.TRAINING -> Icons.Default.School
    ReportKind.DAILY_TRACKING -> Icons.Default.Timeline
    ReportKind.MAP -> Icons.Default.Map
}

private fun ReportKind.subtitle(): String = when (this) {
    ReportKind.TRAINING -> "Các buổi huấn luyện EDAC"
    ReportKind.DAILY_TRACKING -> "Ai đang làm gì, ngay lúc này"
    ReportKind.MAP -> "Đường đi trong ngày làm việc"
    else -> "Cả nhóm hoặc từng nhân viên"
}

/** Tab "Báo cáo". Legacy ReportMenuScreen, in its order. */
@Composable
fun ReportsMenu(onOpen: (ReportKind) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(ReportKind.entries) { r ->
            Card(
                onClick = { onOpen(r) },
                enabled = r.ready,
                modifier = Modifier.fillMaxWidth().alpha(if (r.ready) 1f else 0.5f),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
                    Icon(r.icon(), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(r.title, style = MaterialTheme.typography.bodyLarge)
                        Text(r.subtitle(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (r.ready) Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                }
            }
        }
    }
}
