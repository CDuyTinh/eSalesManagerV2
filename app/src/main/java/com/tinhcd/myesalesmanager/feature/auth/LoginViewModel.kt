package com.tinhcd.myesalesmanager.feature.auth

import androidx.lifecycle.ViewModel
import com.tinhcd.myesalesmanager.R
import com.tinhcd.myesalesmanager.core.ui.AppStrings
import androidx.lifecycle.viewModelScope
import com.tinhcd.myesalesmanager.domain.AppError
import com.tinhcd.myesalesmanager.domain.DataResult
import com.tinhcd.myesalesmanager.domain.usecase.SignInUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val signedIn: Boolean = false,
) {
    val canSubmit: Boolean get() = username.isNotBlank() && password.isNotBlank() && !loading
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val strings: AppStrings,
    private val signIn: SignInUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onUsernameChange(value: String) = _state.update { it.copy(username = value, error = null) }

    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, error = null) }

    fun submit() {
        val current = _state.value
        if (!current.canSubmit) return

        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val result = signIn(current.username, current.password)) {
                is DataResult.Success ->
                    _state.update { it.copy(loading = false, signedIn = true) }

                is DataResult.Failure ->
                    _state.update { it.copy(loading = false, error = result.error.toMessage(strings)) }
            }
        }
    }
}

private fun AppError.toMessage(strings: AppStrings): String = when (this) {
    is AppError.Auth -> when (message) {
        "invalid_credentials" -> strings(R.string.mlg_1)
        "account_not_provisioned" -> strings(R.string.mlg_2)
        "not_a_manager" -> strings(R.string.mlg_3)
        "profile_unavailable" -> strings(R.string.mlg_4)
        else -> strings(R.string.mlg_5)
    }

    is AppError.Network -> strings(R.string.error_network)
    is AppError.Rule -> when (key) {
        "login_empty_fields" -> strings(R.string.mlg_6)
        else -> strings(R.string.error_invalid)
    }

    is AppError.Server -> code?.let { strings(R.string.error_server_code, it) } ?: strings(R.string.error_server)
    is AppError.Unknown -> message?.takeIf { it.isNotBlank() } ?: strings(R.string.error_unknown)
}
