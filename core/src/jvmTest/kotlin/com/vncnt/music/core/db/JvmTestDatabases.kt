package com.vncnt.music.core.db

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

actual fun createTestDatabase(): AppDatabase =
    Room.inMemoryDatabaseBuilder<AppDatabase>()
        .setDriver(BundledSQLiteDriver())
        .build()
