package com.tinhcd.myesalesmanager.domain.usecase

import com.tinhcd.myesalesmanager.domain.AppError
import com.tinhcd.myesalesmanager.domain.DataResult
import com.tinhcd.myesalesmanager.domain.model.Manager
import com.tinhcd.myesalesmanager.domain.repository.AuthRepository
import javax.inject.Inject

class SignInUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(username: String, password: String): DataResult<Manager> {
        val user = username.trim()
        if (user.isEmpty() || password.isEmpty()) {
            return DataResult.Failure(AppError.Rule("login_empty_fields"))
        }
        return authRepository.signIn(user, password)
    }
}
