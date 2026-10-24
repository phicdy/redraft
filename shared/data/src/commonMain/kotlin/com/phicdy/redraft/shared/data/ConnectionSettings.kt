package com.phicdy.redraft.shared.data

import kotlinx.coroutines.flow.Flow

/**
 * The non-secret, user-editable part of a provider connection.
 *
 * A null field means "not set by the user"; the LLM layer's defaults apply then.
 * The data layer does not know those defaults because it must not depend on `shared/llm`.
 */
data class ConnectionSettings(
    val baseUrl: String? = null,
    val model: String? = null,
)

interface ConnectionSettingsRepository {
    /** The current settings, re-emitted whenever they change. */
    val settings: Flow<ConnectionSettings>

    suspend fun save(settings: ConnectionSettings)
}
