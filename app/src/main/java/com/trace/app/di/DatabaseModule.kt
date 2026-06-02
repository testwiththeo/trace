package com.trace.app.di

import android.content.Context
import androidx.room.Room
import com.trace.app.data.db.TraceDatabase
import com.trace.app.data.db.dao.MockRuleDao
import com.trace.app.data.db.dao.SessionDao
import com.trace.app.data.db.dao.TrafficDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): TraceDatabase {
        return Room.databaseBuilder(
            context,
            TraceDatabase::class.java,
            "trace_database"
        ).build()
    }

    @Provides
    fun provideTrafficDao(database: TraceDatabase): TrafficDao {
        return database.trafficDao()
    }

    @Provides
    fun provideMockRuleDao(database: TraceDatabase): MockRuleDao {
        return database.mockRuleDao()
    }

    @Provides
    fun provideSessionDao(database: TraceDatabase): SessionDao {
        return database.sessionDao()
    }
}
