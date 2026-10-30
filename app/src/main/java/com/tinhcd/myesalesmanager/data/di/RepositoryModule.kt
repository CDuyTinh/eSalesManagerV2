package com.tinhcd.myesalesmanager.data.di

import com.tinhcd.myesalesmanager.data.repository.ApprovalRepositoryImpl
import com.tinhcd.myesalesmanager.data.repository.AuthRepositoryImpl
import com.tinhcd.myesalesmanager.data.repository.DashboardRepositoryImpl
import com.tinhcd.myesalesmanager.data.repository.ReportRepositoryImpl
import com.tinhcd.myesalesmanager.data.repository.WorkWithRepositoryImpl
import com.tinhcd.myesalesmanager.domain.repository.WorkWithRepository
import com.tinhcd.myesalesmanager.domain.repository.ReportRepository
import com.tinhcd.myesalesmanager.domain.repository.DashboardRepository
import com.tinhcd.myesalesmanager.data.repository.WorkPlanRepositoryImpl
import com.tinhcd.myesalesmanager.domain.repository.ApprovalRepository
import com.tinhcd.myesalesmanager.domain.repository.AuthRepository
import com.tinhcd.myesalesmanager.domain.repository.WorkPlanRepository
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

    @Binds
    @Singleton
    abstract fun bindWorkPlanRepository(impl: WorkPlanRepositoryImpl): WorkPlanRepository

    @Binds
    @Singleton
    abstract fun bindDashboardRepository(impl: DashboardRepositoryImpl): DashboardRepository

    @Binds
    @Singleton
    abstract fun bindReportRepository(impl: ReportRepositoryImpl): ReportRepository

    @Binds
    @Singleton
    abstract fun bindWorkWithRepository(impl: WorkWithRepositoryImpl): WorkWithRepository
}
