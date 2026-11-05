package com.tinhcd.myesalesmanager.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tinhcd.myesalesmanager.core.ui.SubScreen

/** Cài đặt: how the app looks on this phone. Kept on the phone, not the account. */
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    SubScreen(title = "Cài đặt", onBack = onBack) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Giao diện", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            AppearanceCard()
        }
    }
}
