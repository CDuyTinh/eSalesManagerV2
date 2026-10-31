package com.tinhcd.myesalesmanager.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tinhcd.myesalesmanager.domain.model.TeamMember

/**
 * Who a figure is about: the whole team, one supervisor's reps (AM / RM only,
 * the legacy Filter's SS level), or one rep. The rep row narrows to the chosen
 * supervisor's reps.
 */
@Composable
fun TeamFilter(
    supervisors: List<TeamMember>,
    supervisorId: String?,
    onSupervisor: (String?) -> Unit,
    reps: List<TeamMember>,
    salespersonId: String?,
    onRep: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shownReps = if (supervisorId == null) reps else reps.filter { it.supervisorId == supervisorId }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (supervisors.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 16.dp)) {
                item {
                    FilterChip(selected = supervisorId == null, onClick = { onSupervisor(null) }, label = { Text("Mọi giám sát") })
                }
                items(supervisors, key = { it.id }) { s ->
                    FilterChip(selected = supervisorId == s.id, onClick = { onSupervisor(s.id) }, label = { Text("GS ${s.fullName}") })
                }
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 16.dp)) {
            item {
                FilterChip(
                    selected = salespersonId == null,
                    onClick = { onRep(null) },
                    label = { Text(if (supervisorId == null) "Cả nhóm (${shownReps.size})" else "Cả tổ (${shownReps.size})") },
                )
            }
            items(shownReps, key = { it.id }) { m ->
                FilterChip(selected = salespersonId == m.id, onClick = { onRep(m.id) }, label = { Text(m.fullName) })
            }
        }
    }
}
