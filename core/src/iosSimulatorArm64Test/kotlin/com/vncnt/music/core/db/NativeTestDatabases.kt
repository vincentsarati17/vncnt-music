package com.vncnt.music.core.db

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUUID

/**
 * File-based test database: same bundled driver as JVM, pointed at a unique
 * temp file (runner filesystems are ephemeral, so no cleanup is needed).
 *
 * The factory is passed explicitly: on Native, Room's default reflective
 * constructor lookup (findDatabaseConstructorAndInitDatabaseImpl) cannot work,
 * so the generated constructor object is handed over directly instead.
 */
@OptIn(ExperimentalForeignApi::class)
actual fun createTestDatabase(): AppDatabase {
    val path = NSTemporaryDirectory() + "testdb_" + NSUUID.UUID().UUIDString + ".db"
    return Room.databaseBuilder<AppDatabase>(path) { AppDatabaseConstructor.initialize() }
        .setDriver(BundledSQLiteDriver())
        .build()
}
