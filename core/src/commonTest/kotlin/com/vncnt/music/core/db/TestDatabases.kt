package com.vncnt.music.core.db

/**
 * Platform-appropriate throwaway database for tests.
 * - JVM: in-memory database (fast, proven).
 * - iOS simulator: temp-file database (in-memory bootstrap is unreliable
 *   on Native; production iOS is file-based too).
 */
expect fun createTestDatabase(): AppDatabase
