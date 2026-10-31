package com.tinhcd.myesalesmanager.feature.personal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.tinhcd.myesalesmanager.core.ui.SubScreen
import com.tinhcd.myesalesmanager.domain.AppError
import com.tinhcd.myesalesmanager.domain.DataResult
import com.tinhcd.myesalesmanager.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The rules the SFA app applies, so one account is held to one standard. */
private enum class PasswordRule(val label: String, val met: (String) -> Boolean) {
    LENGTH("Ít nhất 8 ký tự", { it.length >= 8 }),
    LOWERCASE("Có chữ thường", { p -> p.any { it.isLowerCase() } }),
    UPPERCASE("Có chữ hoa", { p -> p.any { it.isUpperCase() } }),
    DIGIT("Có chữ số", { p -> p.any { it.isDigit() } }),
    SPECIAL("Có ký tự đặc biệt", { Regex("[^\\p{L}\\p{N}\\s]").containsMatchIn(it) }),
}

data class PasswordState(
    val current: String = "",
    val new: String = "",
    val confirm: String = "",
    val saving: Boolean = false,
    val error: String? = null,
    /** Stays on screen rather than closing, so the change is seen to have happened. */
    val changed: Boolean = false,
) {
    val confirmMismatch: Boolean get() = confirm.isNotEmpty() && confirm != new
    val sameAsCurrent: Boolean get() = new.isNotEmpty() && new == current
    val canSubmit: Boolean
        get() = current.isNotEmpty() && PasswordRule.entries.all { it.met(new) } &&
            confirm == new && !sameAsCurrent
}

@HiltViewModel
class PasswordViewModel @Inject constructor(
    private val auth: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(PasswordState())
    val state: StateFlow<PasswordState> = _state.asStateFlow()

    fun edit(change: (PasswordState) -> PasswordState) =
        _state.update { change(it).copy(error = null, changed = false) }

    fun save() {
        val s = _state.value
        if (s.saving || !s.canSubmit) return
        _state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            when (val r = auth.changePassword(s.current, s.new)) {
                // Cleared, so no password is left sitting in a field.
                is DataResult.Success -> _state.value = PasswordState(changed = true)
                is DataResult.Failure -> _state.update { it.copy(saving = false, error = r.error.passwordMessage()) }
            }
        }
    }
}

/** A wrong current password is the one failure the manager can act on, so it is named. */
private fun AppError.passwordMessage(): String = when (this) {
    is AppError.Auth -> if (message == "invalid_credentials") "Mật khẩu hiện tại không đúng" else "Phiên đăng nhập đã hết hạn, đăng nhập lại"
    is AppError.Network -> "Không có kết nối mạng"
    else -> "Chưa đổi được mật khẩu, thử lại"
}

/** Đổi mật khẩu: the current one first, so an unlocked phone cannot lock its owner out. */
@Composable
fun PasswordScreen(onBack: () -> Unit, viewModel: PasswordViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    SubScreen(title = "Đổi mật khẩu", onBack = onBack) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PasswordField("Mật khẩu hiện tại", state.current) { v -> viewModel.edit { it.copy(current = v) } }
            PasswordField(
                "Mật khẩu mới", state.new,
                error = if (state.sameAsCurrent) "Mật khẩu mới phải khác mật khẩu hiện tại" else null,
            ) { v -> viewModel.edit { it.copy(new = v) } }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                PasswordRule.entries.forEach { rule ->
                    val met = rule.met(state.new)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (met) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (met) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(16.dp),
                        )
                        Text("  ${rule.label}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            PasswordField(
                "Nhập lại mật khẩu mới", state.confirm,
                error = if (state.confirmMismatch) "Không khớp với mật khẩu mới" else null,
            ) { v -> viewModel.edit { it.copy(confirm = v) } }

            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (state.changed) Text("Đã đổi mật khẩu", color = Color(0xFF2E7D32))

            Button(
                onClick = viewModel::save,
                enabled = state.canSubmit && !state.saving,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (state.saving) "Đang lưu…" else "Đổi mật khẩu") }
        }
    }
}

@Composable
private fun PasswordField(label: String, value: String, error: String? = null, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth(),
    )
}
