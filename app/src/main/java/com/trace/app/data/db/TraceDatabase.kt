package com.trace.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.trace.app.data.db.dao.MockRuleDao
import com.trace.app.data.db.dao.SessionDao
import com.trace.app.data.db.dao.TrafficDao
import com.trace.app.data.db.entity.CapturedTrafficEntity
import com.trace.app.data.db.entity.CaptureSessionEntity
import com.trace.app.data.db.entity.MockRuleEntity

@Database(
    entities = [
        CapturedTrafficEntity::class,
        MockRuleEntity::class,
        CaptureSessionEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class TraceDatabase : RoomDatabase() {
    abstract fun trafficDao(): TrafficDao
    abstract fun mockRuleDao(): MockRuleDao
    abstract fun sessionDao(): SessionDao
}
