package com.vncnt.music.core.db

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUUID

/**
 * File-based test database: same bundled driver as JVM, but pointed at a
 * unique temp file because the in-memory bootstrap throws on Native.
 * Runner filesystems are ephemeral, so no cleanup is needed.
 */
@OptIn(ExperimentalForeignApi::class)
actual fun createTestDatabase(): AppDatabase {
    val path = NSTemporaryDirectory() + "testdb_" + NSUUID.UUID().UUIDString + ".db"
    return Room.databaseBuilder<AppDatabase>(path)
        .setDriver(BundledSQLiteDriver())
        .build()
}
