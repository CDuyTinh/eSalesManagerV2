package com.tinhcd.myesalesmanager.feature.auth

import androidx.lifecycle.ViewModel
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
                    _state.update { it.copy(loading = false, error = result.error.toMessage()) }
            }
        }
    }
}

private fun AppError.toMessage(): String = when (this) {
    is AppError.Auth -> when (message) {
        "invalid_credentials" -> "Sai tên đăng nhập hoặc mật khẩu"
        "account_not_provisioned" -> "Tài khoản chưa được gắn nhân viên"
        "not_a_manager" -> "Tài khoản nhân viên bán hàng không dùng được ứng dụng quản lý. Vui lòng dùng eSales SFA."
        "profile_unavailable" -> "Đã đăng nhập nhưng chưa tải được thông tin. Thử lại."
        else -> "Đăng nhập thất bại"
    }

    is AppError.Network -> "Không có kết nối mạng"
    is AppError.Rule -> when (key) {
        "login_empty_fields" -> "Vui lòng nhập đầy đủ thông tin"
        else -> "Dữ liệu không hợp lệ"
    }

    is AppError.Server -> "Lỗi máy chủ${code?.let { " ($it)" }.orEmpty()}"
    is AppError.Unknown -> message?.takeIf { it.isNotBlank() } ?: "Đã có lỗi xảy ra"
}
