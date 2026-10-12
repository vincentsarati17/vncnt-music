package com.vncnt.music.core.db

import android.content.Context
import androidx.room.Room

// NOTE: no hand-written actual object here — Room's KSP processor generates
// the AppDatabaseConstructor actual per platform from the expect declaration.
// (A handwritten actual makes @ConstructedBy resolve to a concrete object,
// which Room rejects: "@ConstructedBy definition must be an expect declaration".)
// The app keeps calling AppDatabase.getDatabase(context) unchanged (via the
// typealias shim).
@Volatile
private var instance: AppDatabase? = null

/** Android singleton factory. Same file name, version, and migration policy as before. */
fun AppDatabase.Companion.getDatabase(context: Context): AppDatabase {
    return instance ?: synchronized(AppDatabase::class) {
        instance ?: Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            AppDatabase.FILE_NAME
        )
            .addMigrations(AppDatabase.MIGRATION_4_5)
            .fallbackToDestructiveMigrationOnDowngrade(true)
            .build()
            .also { instance = it }
    }
}
