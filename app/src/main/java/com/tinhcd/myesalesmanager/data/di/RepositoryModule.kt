package com.tinhcd.myesalesmanager.data.di

import com.tinhcd.myesalesmanager.data.repository.ApprovalRepositoryImpl
import com.tinhcd.myesalesmanager.data.repository.AuthRepositoryImpl
import com.tinhcd.myesalesmanager.domain.repository.ApprovalRepository
import com.tinhcd.myesalesmanager.domain.repository.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindApprovalRepository(impl: ApprovalRepositoryImpl): ApprovalRepository
}
