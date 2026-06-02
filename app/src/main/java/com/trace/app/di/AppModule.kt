package com.trace.app.di

import android.content.Context
import com.trace.app.proxy.TlsInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideTlsInterceptor(
        @ApplicationContext context: Context
    ): TlsInterceptor = TlsInterceptor(context)
}
