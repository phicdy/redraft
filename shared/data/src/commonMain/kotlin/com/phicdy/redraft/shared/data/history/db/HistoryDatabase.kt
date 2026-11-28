package com.phicdy.redraft.shared.data.history.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlin.coroutines.CoroutineContext

/**
 * Local history of translations and proofreading. The schema of each version is exported to
 * `shared/data/schemas`; changing an entity requires a new version and a migration.
 */
@Database(
    entities = [HistoryEntryEntity::class, HistoryFindingEntity::class],
    version = 1,
    exportSchema = true,
)
@ConstructedBy(HistoryDatabaseConstructor::class)
abstract class HistoryDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
}

// The Room compiler generates the `actual` implementations for each target.
@Suppress("KotlinNoActualForExpect")
expect object HistoryDatabaseConstructor : RoomDatabaseConstructor<HistoryDatabase> {
    override fun initialize(): HistoryDatabase
}

/** File name of the history database. */
internal const val HISTORY_DATABASE_FILE_NAME: String = "history.db"

/**
 * Finishes a platform builder. The bundled SQLite driver gives every platform the same SQLite
 * version and features, instead of whatever the OS ships.
 */
internal fun RoomDatabase.Builder<HistoryDatabase>.buildHistoryDatabase(
    queryContext: CoroutineContext,
): HistoryDatabase = setDriver(BundledSQLiteDriver())
    .setQueryCoroutineContext(queryContext)
    .build()
