package com.phicdy.redraft.shared.data.history

import com.phicdy.redraft.shared.data.history.db.AggregationRows
import com.phicdy.redraft.shared.data.history.db.FindingCountRow
import com.phicdy.redraft.shared.data.history.db.FindingKind
import com.phicdy.redraft.shared.data.history.db.HistoryEntryEntity
import com.phicdy.redraft.shared.data.history.db.HistoryFindingEntity
import com.phicdy.redraft.shared.data.history.db.HistorySummaryRow
import com.phicdy.redraft.shared.model.ErrorCategory
import com.phicdy.redraft.shared.model.InputMode
import com.phicdy.redraft.shared.model.ProofreadResult
import com.phicdy.redraft.shared.model.Severity
import com.phicdy.redraft.shared.model.TranslationResult
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlin.enums.EnumEntries
import kotlin.time.Instant

/**
 * The only place that converts between domain types and database rows, including the stable
 * strings stored for enums. Values that this version does not know (for example written by a
 * newer version on another device) are skipped instead of failing the whole read.
 */
internal object HistoryMapper {
    /** Version of the JSON in `result_json`. Bump it when the result types change incompatibly. */
    const val RESULT_SCHEMA_VERSION: Int = 1

    private val json = Json { ignoreUnknownKeys = true }

    private val modes: Map<InputMode, String> = mapOf(
        InputMode.Translate to "translate",
        InputMode.Proofread to "proofread",
    )

    // The model enums' serial names are already stable (they are the LLM JSON schema), so they are reused.
    private val categories = serialNames(ErrorCategory.serializer(), ErrorCategory.entries)
    private val severities = serialNames(Severity.serializer(), Severity.entries)
    private val expressionTypes =
        serialNames(TranslationResult.ExpressionType.serializer(), TranslationResult.ExpressionType.entries)

    fun modeValue(mode: InputMode): String = modes.getValue(mode)

    fun categoryValue(category: ErrorCategory): String = categories.getValue(category)

    fun severityValue(severity: Severity): String = severities.getValue(severity)

    fun toEntity(entry: HistoryEntry): HistoryEntryEntity = HistoryEntryEntity(
        id = entry.id,
        deviceId = entry.deviceId,
        createdAt = entry.createdAt.toEpochMilliseconds(),
        updatedAt = entry.updatedAt.toEpochMilliseconds(),
        deletedAt = null,
        mode = modeValue(entry.result.mode),
        inputText = entry.input,
        resultText = entry.result.outputText(),
        purpose = entry.context.purpose.storageValue,
        politeness = entry.context.politeness.storageValue,
        model = entry.model,
        inputTokens = entry.usage.inputTokens,
        outputTokens = entry.usage.outputTokens,
        resultSchemaVersion = RESULT_SCHEMA_VERSION,
        resultJson = when (val result = entry.result) {
            is HistoryResult.Translation -> json.encodeToString(TranslationResult.serializer(), result.result)
            is HistoryResult.Proofread -> json.encodeToString(ProofreadResult.serializer(), result.result)
        },
    )

    /** One row per issue, grammar point and expression of [entry], with ids from [newId]. */
    fun toFindings(entry: HistoryEntry, newId: () -> String): List<HistoryFindingEntity> {
        val createdAt = entry.createdAt.toEpochMilliseconds()
        fun finding(
            kind: String,
            position: Int,
            text: String,
            category: ErrorCategory? = null,
            severity: Severity? = null,
            expressionType: TranslationResult.ExpressionType? = null,
            detail: String? = null,
        ): HistoryFindingEntity = HistoryFindingEntity(
            id = newId(),
            entryId = entry.id,
            kind = kind,
            position = position,
            category = category?.let(categories::getValue),
            severity = severity?.let(severities::getValue),
            expressionType = expressionType?.let(expressionTypes::getValue),
            text = text,
            detail = detail,
            createdAt = createdAt,
        )
        return when (val result = entry.result) {
            is HistoryResult.Proofread -> result.result.issues.mapIndexed { index, issue ->
                finding(
                    kind = FindingKind.ISSUE,
                    position = index,
                    text = issue.original,
                    category = issue.category,
                    severity = issue.severity,
                    detail = issue.suggestion,
                )
            }

            is HistoryResult.Translation -> {
                val grammarPoints = result.result.grammarPoints.mapIndexed { index, point ->
                    finding(kind = FindingKind.GRAMMAR_POINT, position = index, text = point.title, category = point.category)
                }
                val expressions = result.result.expressions.mapIndexed { index, expression ->
                    finding(
                        kind = FindingKind.EXPRESSION,
                        position = index,
                        text = expression.expression,
                        expressionType = expression.type,
                        detail = expression.meaning,
                    )
                }
                grammarPoints + expressions
            }
        }
    }

    /** Null when the mode or the stored result cannot be read by this version. */
    fun toEntry(entity: HistoryEntryEntity): HistoryEntry? {
        val mode = modes.keyFor(entity.mode) ?: return null
        val result = try {
            when (mode) {
                InputMode.Translate ->
                    HistoryResult.Translation(json.decodeFromString(TranslationResult.serializer(), entity.resultJson))

                InputMode.Proofread ->
                    HistoryResult.Proofread(json.decodeFromString(ProofreadResult.serializer(), entity.resultJson))
            }
        } catch (e: IllegalArgumentException) {
            // SerializationException is an IllegalArgumentException.
            return null
        }
        return HistoryEntry(
            id = entity.id,
            deviceId = entity.deviceId,
            createdAt = Instant.fromEpochMilliseconds(entity.createdAt),
            updatedAt = Instant.fromEpochMilliseconds(entity.updatedAt),
            input = entity.inputText,
            context = HistoryContext(
                // Unknown values fall back to the defaults of the main screen.
                purpose = HistoryPurpose.entries.firstOrNull { it.storageValue == entity.purpose } ?: HistoryPurpose.Chat,
                politeness = HistoryPoliteness.entries.firstOrNull { it.storageValue == entity.politeness }
                    ?: HistoryPoliteness.Neutral,
            ),
            model = entity.model,
            usage = HistoryUsage(inputTokens = entity.inputTokens, outputTokens = entity.outputTokens),
            result = result,
        )
    }

    fun toSummary(row: HistorySummaryRow): HistoryEntrySummary? = modes.keyFor(row.mode)?.let { mode ->
        HistoryEntrySummary(
            id = row.id,
            createdAt = Instant.fromEpochMilliseconds(row.createdAt),
            mode = mode,
            input = row.inputText,
            output = row.resultText,
            errorCount = row.errorCount,
            improvementCount = row.improvementCount,
            grammarPointCount = row.grammarPointCount,
            expressionCount = row.expressionCount,
        )
    }

    fun toAggregation(rows: AggregationRows): HistoryAggregation {
        val modeCounts = rows.modeCounts.associate { it.mode to it.count }
        return HistoryAggregation(
            entryCounts = InputMode.entries.associateWith { mode -> modeCounts[modeValue(mode)] ?: 0 },
            issueCounts = rows.issueCounts
                .mapNotNull { row ->
                    val category = row.category?.let { categories.keyFor(it) } ?: return@mapNotNull null
                    val severity = row.severity?.let { severities.keyFor(it) } ?: return@mapNotNull null
                    IssueCount(category = category, severity = severity, count = row.count)
                }
                .sortedWith(compareByDescending<IssueCount> { it.count }.thenBy { it.category }.thenBy { it.severity }),
            grammarPointCounts = rows.grammarPointCounts
                .mapNotNull { row -> row.categoryOrNull()?.let { GrammarPointCount(category = it, count = row.count) } }
                .sortedWith(compareByDescending<GrammarPointCount> { it.count }.thenBy { it.category }),
            expressions = rows.expressions.mapNotNull { row ->
                val type = row.expressionType?.let { expressionTypes.keyFor(it) } ?: return@mapNotNull null
                LearnedExpression(
                    entryId = row.entryId,
                    createdAt = Instant.fromEpochMilliseconds(row.createdAt),
                    expression = row.text,
                    type = type,
                    meaning = row.detail.orEmpty(),
                )
            },
        )
    }

    /**
     * A LIKE pattern (escape character `\`) matching [query] anywhere, or null for a blank query.
     * `%` and `_` in the query are matched literally.
     */
    fun containsPattern(query: String?): String? {
        val trimmed = query?.trim()
        if (trimmed.isNullOrEmpty()) return null
        val escaped = buildString {
            for (char in trimmed) {
                if (char == '\\' || char == '%' || char == '_') append('\\')
                append(char)
            }
        }
        return "%$escaped%"
    }

    private fun FindingCountRow.categoryOrNull(): ErrorCategory? = category?.let { categories.keyFor(it) }

    private fun HistoryResult.outputText(): String = when (this) {
        is HistoryResult.Translation -> result.translation
        is HistoryResult.Proofread -> result.correctedText
    }

    private fun <K> Map<K, String>.keyFor(value: String): K? = entries.firstOrNull { it.value == value }?.key

    private fun <E : Enum<E>> serialNames(serializer: KSerializer<E>, entries: EnumEntries<E>): Map<E, String> =
        entries.associateWith { serializer.descriptor.getElementName(it.ordinal) }
}
