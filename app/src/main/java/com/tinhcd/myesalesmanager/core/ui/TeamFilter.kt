package com.tinhcd.myesalesmanager.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tinhcd.myesalesmanager.domain.model.PlacePick
import com.tinhcd.myesalesmanager.domain.model.TeamMember
import com.tinhcd.myesalesmanager.domain.model.branchIds

/**
 * Who a figure is about, the legacy Filter's levels top down: region, area and
 * distributor (each row only when the team spans more than one), then one
 * supervisor's reps (AM / RM only), then one rep. Each row narrows the rows
 * below it.
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
    place: PlacePick? = null,
    onPlace: (PlacePick?) -> Unit = {},
) {
    val everyone = supervisors + reps
    val branches = everyone.mapNotNull { it.branch }.distinctBy { it.id }
    val inPlace = place.branchIds(everyone)?.toSet()

    val shownSupervisors = if (inPlace == null) supervisors else supervisors.filter { it.branch?.id in inPlace }
    val placedReps = if (inPlace == null) reps else reps.filter { it.branch?.id in inPlace }
    val shownReps = if (supervisorId == null) placedReps else placedReps.filter { it.supervisorId == supervisorId }

    // What the rows above have narrowed to, so a lower row offers only what fits.
    val regionId = when (place) {
        is PlacePick.Region -> place.id
        is PlacePick.Area -> branches.firstOrNull { it.areaId == place.id }?.regionId
        is PlacePick.Branch -> branches.firstOrNull { it.id == place.id }?.regionId
        null -> null
    }
    val areaId = when (place) {
        is PlacePick.Area -> place.id
        is PlacePick.Branch -> branches.firstOrNull { it.id == place.id }?.areaId
        else -> null
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        val regions = branches.filter { it.regionId != null }.distinctBy { it.regionId }
        if (regions.size > 1) {
            ChipRow(
                all = "Mọi vùng", allSelected = regionId == null, onAll = { onPlace(null) },
                options = regions.map { it.regionId!! to it.regionName.orEmpty() },
                selected = regionId, onPick = { onPlace(PlacePick.Region(it)) },
            )
        }
        val areas = branches.filter { it.areaId != null && (regionId == null || it.regionId == regionId) }.distinctBy { it.areaId }
        if (areas.size > 1) {
            ChipRow(
                all = "Mọi khu vực", allSelected = areaId == null,
                onAll = { onPlace(regionId?.let { PlacePick.Region(it) }) },
                options = areas.map { it.areaId!! to it.areaName.orEmpty() },
                selected = areaId, onPick = { onPlace(PlacePick.Area(it)) },
            )
        }
        val shownBranches = branches.filter { (regionId == null || it.regionId == regionId) && (areaId == null || it.areaId == areaId) }
        if (shownBranches.size > 1) {
            val branchId = (place as? PlacePick.Branch)?.id
            ChipRow(
                all = "Mọi NPP", allSelected = branchId == null,
                onAll = { onPlace(areaId?.let { PlacePick.Area(it) } ?: regionId?.let { PlacePick.Region(it) }) },
                options = shownBranches.map { it.id to it.name },
                selected = branchId, onPick = { onPlace(PlacePick.Branch(it)) },
            )
        }
        if (shownSupervisors.isNotEmpty()) {
            ChipRow(
                all = "Mọi giám sát", allSelected = supervisorId == null, onAll = { onSupervisor(null) },
                options = shownSupervisors.map { it.id to "GS ${it.fullName}" },
                selected = supervisorId, onPick = onSupervisor,
            )
        }
        ChipRow(
            all = if (supervisorId == null) "Cả nhóm (${shownReps.size})" else "Cả tổ (${shownReps.size})",
            allSelected = salespersonId == null, onAll = { onRep(null) },
            options = shownReps.map { it.id to it.fullName },
            selected = salespersonId, onPick = onRep,
        )
    }
}

@Composable
private fun ChipRow(
    all: String,
    allSelected: Boolean,
    onAll: () -> Unit,
    options: List<Pair<String, String>>,
    selected: String?,
    onPick: (String) -> Unit,
) {
    // A new scroll position whenever the choices change: the row is first drawn
    // before the team has loaded, and the old position left it part-scrolled,
    // its first chip cut off at the edge.
    val scroll = remember(options.map { it.first }) { LazyListState() }
    LazyRow(state = scroll, horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 16.dp)) {
        item(key = "all") { FilterChip(selected = allSelected, onClick = onAll, label = { Text(all) }) }
        items(options, key = { it.first }) { (id, label) ->
            FilterChip(selected = selected == id, onClick = { onPick(id) }, label = { Text(label) })
        }
    }
}
