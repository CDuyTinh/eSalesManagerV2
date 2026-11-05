package com.tinhcd.myesalesmanager.feature.settings

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.tinhcd.myesalesmanager.core.ui.theme.AppLanguage
import com.tinhcd.myesalesmanager.core.ui.theme.LanguageSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LanguageViewModel @Inject constructor(
    private val settings: LanguageSettings,
) : ViewModel() {
    val language: AppLanguage get() = settings.language

    fun select(language: AppLanguage) = settings.set(language)
}

/** Ngôn ngữ / Language: Vietnamese or English, each written in itself. Redraws the app at once. */
@Composable
fun LanguageCard(viewModel: LanguageViewModel = hiltViewModel()) {
    val context = LocalContext.current
    var language by remember { mutableStateOf(viewModel.language) }
    LanguageCard(language = language) { picked ->
        if (picked != language) {
            language = picked
            viewModel.select(picked)
            context.findActivity()?.recreate()
        }
    }
}

@Composable
fun LanguageCard(language: AppLanguage, onSelect: (AppLanguage) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp)) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                AppLanguage.entries.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = option == language,
                        onClick = { onSelect(option) },
                        shape = SegmentedButtonDefaults.itemShape(index, AppLanguage.entries.size),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                        label = { Text(option.nativeName) },
                    )
                }
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
