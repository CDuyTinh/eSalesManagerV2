package com.tinhcd.myesalesmanager.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.unit.dp
import com.tinhcd.myesalesmanager.core.ui.theme.brand

/**
 * A pushed screen: brand header with a back arrow, then content. Every screen
 * opened from a tab uses it, so the header reads the same everywhere.
 */
@Composable
fun SubScreen(
    title: String,
    onBack: () -> Unit,
    snackbar: SnackbarHostState? = null,
    subtitle: String? = null,
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.brand.header)
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
                Column {
                    Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.brand.onHeader)
                    if (subtitle != null) {
                        Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.brand.onHeader)
                    }
                }
            }
        },
        snackbarHost = { if (snackbar != null) SnackbarHost(snackbar) },
        bottomBar = bottomBar,
    ) { padding ->
        Box(Modifier.fillMaxSize()) { content(padding) }
    }
}

/** "2026-11-05" or "2026-11-05T15:03:02+00:00" → "05/11/2026". */
fun formatIsoDate(value: String?): String {
    if (value == null || value.length < 10) return value.orEmpty()
    val (y, m, d) = value.substring(0, 10).split('-').takeIf { it.size == 3 } ?: return value
    return "$d/$m/$y"
}
