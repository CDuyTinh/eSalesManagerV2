package com.tinhcd.myesalesmanager.feature.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.IconButton
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.tinhcd.myesalesmanager.feature.personal.PersonalTarget
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tinhcd.myesalesmanager.core.ui.ErrorBox
import com.tinhcd.myesalesmanager.core.ui.LoadingBox
import com.tinhcd.myesalesmanager.core.ui.theme.brand
import com.tinhcd.myesalesmanager.domain.model.Manager
import com.tinhcd.myesalesmanager.domain.model.TeamMember
import com.tinhcd.myesalesmanager.domain.model.title
import com.tinhcd.myesalesmanager.feature.approval.ApprovalMenu
import com.tinhcd.myesalesmanager.feature.approval.ApprovalTarget
import com.tinhcd.myesalesmanager.feature.dashboard.DashboardTab
import com.tinhcd.myesalesmanager.domain.model.ReportKind
import com.tinhcd.myesalesmanager.feature.report.ReportsMenu
import com.tinhcd.myesalesmanager.feature.workplan.TaskTarget
import com.tinhcd.myesalesmanager.feature.workplan.TasksMenu

/**
 * The legacy manager app's five bottom tabs, in its order. Công việc and Khác
 * opened sheets there; here they are tabs until their entries exist.
 */
private enum class Tab(val label: String, val icon: ImageVector) {
    OVERVIEW("Tổng quan", Icons.Default.Dashboard),
    TASKS("Công việc", Icons.AutoMirrored.Filled.Assignment),
    REPORTS("Báo cáo", Icons.Default.BarChart),
    APPROVALS("Phê duyệt", Icons.AutoMirrored.Filled.FactCheck),
    OTHER("Khác", Icons.Default.MoreHoriz),
}

@Composable
fun MainShell(
    onSignedOut: () -> Unit,
    onOpenApproval: (ApprovalTarget) -> Unit,
    onOpenTask: (TaskTarget) -> Unit,
    onOpenReport: (ReportKind) -> Unit,
    onOpenPersonal: (PersonalTarget) -> Unit,
    viewModel: ShellViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val unread by viewModel.unread.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(state.signedOut) { if (state.signedOut) onSignedOut() }
    LifecycleResumeEffect(Unit) {
        viewModel.refreshUnread()
        onPauseOrDispose { }
    }

    Scaffold(
        topBar = { Header(state.manager, unread) { onOpenPersonal(PersonalTarget.NOTIFICATIONS) } },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEachIndexed { index, t ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { tab = index },
                        icon = { Icon(t.icon, contentDescription = null) },
                        label = { Text(t.label) },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                state.loading -> LoadingBox()
                state.manager == null -> ErrorBox(
                    message = "Đã đăng nhập nhưng chưa tải được thông tin quản lý.",
                    onRetry = viewModel::retryProfile,
                )

                else -> when (Tab.entries[tab]) {
                    Tab.OVERVIEW -> DashboardTab()
                    Tab.TASKS -> TasksMenu(onOpen = onOpenTask)
                    Tab.REPORTS -> ReportsMenu(onOpen = onOpenReport)
                    Tab.APPROVALS -> ApprovalMenu(onOpen = onOpenApproval)
                    Tab.OTHER -> OtherTab(state.team, onOpen = onOpenPersonal, onSignOut = viewModel::signOut)
                }
            }
        }
    }
}

@Composable
private fun Header(manager: Manager?, unread: Int, onBell: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.brand.header)
            .statusBarsPadding()
            .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                manager?.fullName ?: "eSales Manager",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.brand.onHeader,
            )
            if (manager != null) {
                Text(
                    "${manager.code} · ${manager.position.title} · ${manager.branchName}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.brand.onHeader,
                )
            }
        }
        IconButton(onClick = onBell) {
            BadgedBox(badge = { if (unread > 0) Badge { Text(if (unread > 99) "99+" else "$unread") } }) {
                Icon(Icons.Default.Notifications, contentDescription = "Thông báo", tint = MaterialTheme.brand.onHeader)
            }
        }
    }
}

@Composable
private fun OtherTab(team: List<TeamMember>, onOpen: (PersonalTarget) -> Unit, onSignOut: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize()) {
        items(PersonalTarget.entries) { t ->
            ListItem(
                headlineContent = { Text(t.title) },
                supportingContent = { Text(t.subtitle) },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                modifier = Modifier.clickable { onOpen(t) },
            )
            HorizontalDivider()
        }
        item {
            Text(
                "Nhân viên trong nhóm (${team.size})",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp),
            )
        }
        items(team, key = { it.id }) { member ->
            ListItem(
                headlineContent = { Text(member.fullName) },
                supportingContent = { Text(listOfNotNull(member.code, member.phone).joinToString(" · ")) },
            )
            HorizontalDivider()
        }
        item {
            OutlinedButton(
                onClick = onSignOut,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) { Text("Đăng xuất") }
        }
    }
}

