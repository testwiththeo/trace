package com.trace.app.di

import com.trace.app.data.repository.MockRuleRepositoryImpl
import com.trace.app.data.repository.SessionRepositoryImpl
import com.trace.app.data.repository.TrafficRepositoryImpl
import com.trace.app.domain.repository.MockRuleRepository
import com.trace.app.domain.repository.SessionRepository
import com.trace.app.domain.repository.TrafficRepository
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
    abstract fun bindTrafficRepository(
        impl: TrafficRepositoryImpl
    ): TrafficRepository

    @Binds
    @Singleton
    abstract fun bindMockRuleRepository(
        impl: MockRuleRepositoryImpl
    ): MockRuleRepository

    @Binds
    @Singleton
    abstract fun bindSessionRepository(
        impl: SessionRepositoryImpl
    ): SessionRepository
}
