package com.vncnt.music.core.db

import androidx.room.Room
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUUID

/**
 * Test-only actual for the simulator test compilation. Safe by construction:
 * Room's "@ConstructedBy must be expect" validation runs only inside KSP-
 * processed (main) compilations, which never see test sources — so this can
 * never collide with Room's own generated actuals on JVM/Android.
 */
actual object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase = AppDatabase::class.instantiateImpl()
}

/**
 * File-based test database: same bundled driver as JVM, pointed at a unique
 * temp file (runner filesystems are ephemeral, so no cleanup is needed).
 *
 * The factory is passed explicitly: on Native, Room's default reflective
 * constructor lookup (findDatabaseConstructorAndInitDatabaseImpl) cannot work,
 * so the test-local actual above is handed over directly instead.
 */
@OptIn(ExperimentalForeignApi::class)
actual fun createTestDatabase(): AppDatabase {
    val path = NSTemporaryDirectory() + "testdb_" + NSUUID.UUID().UUIDString + ".db"
    return Room.databaseBuilder<AppDatabase>(path) { AppDatabaseConstructor.initialize() }
        .setDriver(BundledSQLiteDriver())
        .build()
}
