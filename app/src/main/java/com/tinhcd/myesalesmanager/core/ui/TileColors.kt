package com.tinhcd.myesalesmanager.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Icon colours for a list of menu entries, as SFA's menu sheets have them: one
 * per entry, taken in turn, so neighbours never match.
 */
val TileColors = listOf(
    Color(0xFF2D6CDF), // blue
    Color(0xFFF57C00), // orange
    Color(0xFF04A489), // green
    Color(0xFF7B3FE4), // violet
    Color(0xFFE53935), // red
    Color(0xFF00A3BF), // teal
    Color(0xFFD81B60), // pink
    Color(0xFFF5A202), // amber
)

/** The [TileColors] entry for the entry at [index]. */
fun tileColor(index: Int): Color = TileColors[index.mod(TileColors.size)]

/** A menu entry's icon: drawn in [colour], on a rounded square in a pale wash of it. */
@Composable
fun MenuIcon(icon: ImageVector, colour: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(40.dp)
            .background(colour.copy(alpha = 0.14f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = colour, modifier = Modifier.size(22.dp))
    }
}
