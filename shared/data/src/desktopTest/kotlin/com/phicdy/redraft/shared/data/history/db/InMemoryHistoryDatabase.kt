package com.phicdy.redraft.shared.data.history.db

import androidx.room.Room
import kotlinx.coroutines.Dispatchers

/** A fresh, empty database that lives only as long as the returned object; close it after the test. */
internal fun inMemoryHistoryDatabase(): HistoryDatabase =
    Room.inMemoryDatabaseBuilder<HistoryDatabase>().buildHistoryDatabase(queryContext = Dispatchers.IO)
