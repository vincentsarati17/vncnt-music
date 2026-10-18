package com.vncnt.music.data.db

// Compatibility shim — canonical @Database lives in :core
// (com.vncnt.music.core.db.AppDatabase). The Android singleton factory is a
// companion extension in :core androidMain, so existing
// AppDatabase.getDatabase(context) call sites keep working unchanged.
// TODO(KMP): migrate call sites to com.vncnt.music.core.db, then delete this file.
typealias AppDatabase = com.vncnt.music.core.db.AppDatabase
