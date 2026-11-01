package com.tinhcd.myesalesmanager.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.tinhcd.myesalesmanager.core.ui.theme.brand

/**
 * A pushed screen: brand header with a back arrow, then content. Every screen
 * opened from a tab uses it, so the header reads the same everywhere.
 *
 * @param actions icons at the header's right end.
 * @param band carries the header's colour a little way down with rounded
 *   corners, behind the content, so a first card can sit over its edge.
 */
@Composable
fun SubScreen(
    title: String,
    onBack: () -> Unit,
    snackbar: SnackbarHostState? = null,
    subtitle: String? = null,
    bottomBar: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    band: Boolean = false,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.brand.header, if (band) RectangleShape else HeaderShape)
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Quay lại",
                        tint = MaterialTheme.brand.onHeader,
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.brand.onHeader)
                    if (subtitle != null) {
                        Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.brand.onHeader)
                    }
                }
                actions()
            }
        },
        snackbarHost = { if (snackbar != null) SnackbarHost(snackbar) },
        bottomBar = bottomBar,
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            if (band) {
                Box(
                    Modifier
                        .padding(top = padding.calculateTopPadding())
                        .fillMaxWidth()
                        .height(HeaderBandDepth)
                        .background(MaterialTheme.brand.header, HeaderShape),
                )
            }
            content(padding)
        }
    }
}

/**
 * The header's lower edge: both corners rounded, as SFA's are. A screen with a
 * band keeps the header square and rounds the band instead, so the two read as one.
 */
val HeaderShape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)

/** How far a [SubScreen] band runs below the header; a 16dp list padding leaves the card over the rest. */
private val HeaderBandDepth = 44.dp

/** "2026-11-05" or "2026-11-05T15:03:02+00:00" → "05/11/2026". */
fun formatIsoDate(value: String?): String {
    if (value == null || value.length < 10) return value.orEmpty()
    val (y, m, d) = value.substring(0, 10).split('-').takeIf { it.size == 3 } ?: return value
    return "$d/$m/$y"
}
