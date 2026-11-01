package com.tinhcd.myesalesmanager.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tinhcd.myesalesmanager.core.ui.theme.brand

/** One tile on a [MenuGrid]. A [badge] above zero is drawn in the tile's corner. */
data class GridEntry(
    val title: String,
    val icon: ImageVector,
    val enabled: Boolean = true,
    val badge: Int = 0,
)

/**
 * A tab's destinations as two columns of tiles, as SFA's report menu lays its
 * reports out: the header's colour carried down in a band with rounded lower
 * corners, and the tiles starting over its edge.
 *
 * @param caption a line on the band above the tiles (in the header's colours).
 * @param bandHeight how far the band runs; enough that the first row overlaps it.
 */
@Composable
fun MenuGrid(
    entries: List<GridEntry>,
    onClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    caption: String? = null,
    bandHeight: Dp = if (caption == null) 56.dp else 92.dp,
) {
    Box(modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(bandHeight)
                .background(MaterialTheme.brand.header, HeaderShape),
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            if (caption != null) {
                item(span = { GridItemSpan(2) }) {
                    Text(
                        caption,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.brand.onHeader,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                    )
                }
            }
            itemsIndexed(entries) { index, entry ->
                GridTile(entry, tileColor(index)) { onClick(index) }
            }
        }
    }
}

@Composable
private fun GridTile(entry: GridEntry, colour: Color, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        enabled = entry.enabled,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.2f)
            .alpha(if (entry.enabled) 1f else 0.5f),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            disabledContainerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Box(Modifier.fillMaxSize()) {
            Column(
                Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                MenuIcon(entry.icon, colour, size = 56.dp)
                Text(
                    entry.title,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
            if (entry.badge > 0) {
                Surface(
                    color = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                    shape = CircleShape,
                    modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
                ) {
                    Text(
                        if (entry.badge > 99) "99+" else entry.badge.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }
}
