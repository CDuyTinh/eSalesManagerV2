package com.tinhcd.myesalesmanager.data.di

import com.tinhcd.myesalesmanager.BuildConfig
import com.tinhcd.myesalesmanager.data.remote.http.ClockSkewRetryInterceptor
import com.tinhcd.myesalesmanager.data.remote.http.SessionTokens
import com.tinhcd.myesalesmanager.data.remote.http.SupabaseApiKey
import com.tinhcd.myesalesmanager.data.remote.http.SupabaseAuthInterceptor
import com.tinhcd.myesalesmanager.data.remote.http.SupabaseSessionTokens
import com.tinhcd.myesalesmanager.data.remote.http.SupabaseTokenAuthenticator
import com.tinhcd.myesalesmanager.data.remote.service.ApprovalService
import com.tinhcd.myesalesmanager.data.remote.service.BootstrapService
import com.tinhcd.myesalesmanager.data.remote.service.DashboardService
import com.tinhcd.myesalesmanager.data.remote.service.ReportService
import com.tinhcd.myesalesmanager.data.remote.service.WorkWithService
import com.tinhcd.myesalesmanager.data.remote.service.RouteService
import com.tinhcd.myesalesmanager.data.remote.service.WorkPlanService
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Retrofit against the shared project's Edge Functions — the same stack as
 * MyeSalesSFA. The functions build their database client from the manager's JWT,
 * so RLS decides which reps' data comes back.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkBindings {
    @Binds
    @Singleton
    abstract fun bindSessionTokens(impl: SupabaseSessionTokens): SessionTokens
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideApiKey(): SupabaseApiKey {
        check(BuildConfig.SUPABASE_PUBLISHABLE_KEY.isNotBlank()) {
            "supabase.publishableKey is missing from local.properties"
        }
        return SupabaseApiKey(BuildConfig.SUPABASE_PUBLISHABLE_KEY)
    }

    /** Lenient about unknown keys: the server may grow a column before this build. */
    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideOkHttp(
        authInterceptor: SupabaseAuthInterceptor,
        clockSkewRetry: ClockSkewRetryInterceptor,
        authenticator: SupabaseTokenAuthenticator,
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        // After the auth interceptor, so the retry carries the same token.
        .addInterceptor(clockSkewRetry)
        .authenticator(authenticator)
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(
                    HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC },
                )
            }
        }
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit {
        check(BuildConfig.SUPABASE_URL.isNotBlank()) {
            "supabase.url is missing from local.properties"
        }
        return Retrofit.Builder()
            .baseUrl(BuildConfig.SUPABASE_URL.trimEnd('/') + "/functions/v1/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun provideBootstrapService(retrofit: Retrofit): BootstrapService =
        retrofit.create(BootstrapService::class.java)

    @Provides
    @Singleton
    fun provideApprovalService(retrofit: Retrofit): ApprovalService =
        retrofit.create(ApprovalService::class.java)

    @Provides
    @Singleton
    fun provideWorkPlanService(retrofit: Retrofit): WorkPlanService =
        retrofit.create(WorkPlanService::class.java)

    @Provides
    @Singleton
    fun provideDashboardService(retrofit: Retrofit): DashboardService =
        retrofit.create(DashboardService::class.java)

    @Provides
    @Singleton
    fun provideReportService(retrofit: Retrofit): ReportService =
        retrofit.create(ReportService::class.java)

    @Provides
    @Singleton
    fun provideWorkWithService(retrofit: Retrofit): WorkWithService =
        retrofit.create(WorkWithService::class.java)

    @Provides
    @Singleton
    fun provideRouteService(retrofit: Retrofit): RouteService =
        retrofit.create(RouteService::class.java)
}
